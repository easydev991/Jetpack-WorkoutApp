package com.swparks.data.repository

import com.swparks.data.NetworkUtils
import com.swparks.data.UserPreferencesRepository
import com.swparks.data.database.dao.UserDao
import com.swparks.data.database.entity.toDomain
import com.swparks.data.database.entity.toEntity
import com.swparks.data.model.SocialUpdates
import com.swparks.data.model.User
import com.swparks.network.SWApi
import com.swparks.ui.model.MainUserForm
import com.swparks.util.CrashReporter
import com.swparks.util.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.supervisorScope
import retrofit2.HttpException
import java.io.IOException

open class UserProfileRepository(
    private val swApi: SWApi,
    private val preferencesRepository: UserPreferencesRepository,
    private val userDao: UserDao,
    logger: Logger,
    crashReporter: CrashReporter
) : BaseRepository(logger, crashReporter) {
    companion object {
        private const val TAG = "UserProfileRepository"
    }

    open suspend fun getUser(userId: Long): Result<User> =
        try {
            // 1. Загружаем с сервера
            val remoteUser = swApi.getUser(userId)

            // 2. Сохраняем в кэш, сохраняя существующие флаги отношений
            val currentUserId = preferencesRepository.getCurrentUserIdSync()
            val existingUser = userDao.getUserByIdFlow(userId).first()
            userDao.insert(
                remoteUser.toEntity(
                    isCurrentUser = (userId == currentUserId),
                    isFriend = existingUser?.isFriend ?: false,
                    isFriendRequest = existingUser?.isFriendRequest ?: false,
                    isBlacklisted = existingUser?.isBlacklisted ?: false
                )
            )

            Result.success(remoteUser)
        } catch (e: IOException) {
            // 3. Ошибка сети - берем из кэша
            val cachedUser = userDao.getUserByIdFlow(userId).first()
            if (cachedUser != null) {
                logger.i(TAG, "Профиль загружен из кэша")
                Result.success(cachedUser.toDomain())
            } else {
                Result.failure(handleIOException(e, TAG, "загрузке пользователя"))
            }
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "загрузке пользователя"))
        }

    open suspend fun editUser(
        userId: Long,
        form: MainUserForm,
        image: ByteArray?
    ): Result<User> =
        try {
            val user =
                swApi.editUser(
                    userId = userId,
                    name = NetworkUtils.createPartWithName("name", form.name),
                    fullName = NetworkUtils.createPartWithName("fullname", form.fullname),
                    email = NetworkUtils.createPartWithName("email", form.email),
                    birthDate =
                        NetworkUtils.createOptionalPartWithName(
                            "birth_date",
                            form.birthDate
                        ),
                    gender =
                        NetworkUtils.createOptionalPartWithName(
                            "gender",
                            form.genderCode.toString()
                        ),
                    countryId =
                        NetworkUtils.createOptionalPartWithName(
                            "country_id",
                            form.countryId?.toString()
                        ),
                    cityId =
                        NetworkUtils.createOptionalPartWithName(
                            "city_id",
                            form.cityId?.toString()
                        ),
                    image = NetworkUtils.createOptionalImagePart(image, "image")
                )
            // Сохраняем обновленного пользователя в локальный кэш для автообновления UI
            userDao.insert(user.toEntity(isCurrentUser = true))
            Result.success(user)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "редактировании пользователя"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "редактировании пользователя"))
        }

    open suspend fun deleteUser(): Result<Unit> =
        try {
            val response = swApi.deleteUser()
            if (response.isSuccessful) {
                preferencesRepository.clearCurrentUserId()
                Result.success(Unit)
            } else {
                Result.failure(handleResponseError(response, TAG, "удалении пользователя"))
            }
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "удалении пользователя"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "удалении пользователя"))
        }

    open suspend fun getSocialUpdates(userId: Long): Result<SocialUpdates> =
        supervisorScope {
            try {
                // Параллельные запросы к серверу
                val userDeferred = async { swApi.getUser(userId) }
                val friendsDeferred = async { swApi.getFriendsForUser(userId) }
                val requestsDeferred = async { swApi.getFriendRequests() }
                val blacklistDeferred = async { swApi.getBlacklist() }

                // Ожидание всех результатов
                val user = userDeferred.await()
                val friends = friendsDeferred.await()
                val requests = requestsDeferred.await()
                val blacklist = blacklistDeferred.await()

                // Сбрасываем все флаги перед обновлением
                // Это критически важно: если сервер возвращает пустой список,
                // старые записи должны быть очищены
                userDao.clearAllFriendFlags()
                userDao.clearAllFriendRequestFlags()
                userDao.clearAllBlacklistFlags()

                // Сохраняем в кэш
                userDao.insert(user.toEntity(isCurrentUser = true))
                userDao.insertAll(friends.map { it.toEntity(isFriend = true) })
                userDao.insertAll(requests.map { it.toEntity(isFriendRequest = true) })
                userDao.insertAll(blacklist.map { it.toEntity(isBlacklisted = true) })

                val socialUpdates =
                    SocialUpdates(
                        user = user,
                        friends = friends,
                        friendRequests = requests,
                        blacklist = blacklist
                    )
                Result.success(socialUpdates)
            } catch (e: IOException) {
                // Ошибка сети - возвращаем кэшированные данные
                val cachedUser = userDao.getCurrentUserFlow().first()?.toDomain()
                val cachedFriends = userDao.getFriendsFlow().first().map { it.toDomain() }
                val cachedRequests = userDao.getFriendRequestsFlow().first().map { it.toDomain() }
                val cachedBlacklist = userDao.getBlacklistFlow().first().map { it.toDomain() }

                if (cachedUser != null) {
                    logger.i(TAG, "Социальные обновления загружены из кэша")
                    Result.success(
                        SocialUpdates(
                            user = cachedUser,
                            friends = cachedFriends,
                            friendRequests = cachedRequests,
                            blacklist = cachedBlacklist
                        )
                    )
                } else {
                    Result.failure(handleIOException(e, TAG, "загрузке социальных обновлений"))
                }
            } catch (e: HttpException) {
                Result.failure(handleHttpException(e, TAG, "загрузке социальных обновлений"))
            }
        }

    open suspend fun findUsers(name: String): Result<List<User>> =
        try {
            val users = swApi.findUsers(name)
            Result.success(users)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "поиске пользователей"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "поиске пользователей"))
        }
}
