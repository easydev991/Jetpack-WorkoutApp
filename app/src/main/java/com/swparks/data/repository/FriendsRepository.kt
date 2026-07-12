package com.swparks.data.repository

import com.swparks.data.database.dao.UserDao
import com.swparks.data.database.entity.toDomain
import com.swparks.data.model.ApiBlacklistOption
import com.swparks.data.model.ApiFriendAction
import com.swparks.data.model.User
import com.swparks.network.SWApi
import com.swparks.util.CrashReporter
import com.swparks.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException

open class FriendsRepository(
    private val swApi: SWApi,
    private val userDao: UserDao,
    logger: Logger,
    crashReporter: CrashReporter
) : BaseRepository(logger, crashReporter) {
    companion object {
        private const val TAG = "FriendsRepository"
    }

    open fun getFriendsFlow(): Flow<List<User>> =
        userDao
            .getFriendsFlow()
            .map { users -> users.map { it.toDomain() } }

    open fun getFriendRequestsFlow(): Flow<List<User>> =
        userDao
            .getFriendRequestsFlow()
            .map { users -> users.map { it.toDomain() } }

    open fun getBlacklistFlow(): Flow<List<User>> =
        userDao
            .getBlacklistFlow()
            .map { users -> users.map { it.toDomain() } }

    open fun getFriendsCountFlow(): Flow<Int> = userDao.getFriendsCountFlow()

    open suspend fun getFriendsForUser(userId: Long): Result<List<User>> =
        try {
            val friends = swApi.getFriendsForUser(userId)
            Result.success(friends)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "загрузке друзей"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "загрузке друзей"))
        }

    open suspend fun getFriendRequests(): Result<List<User>> =
        try {
            val requests = swApi.getFriendRequests()
            Result.success(requests)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "загрузке заявок в друзья"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "загрузке заявок в друзья"))
        }

    open suspend fun respondToFriendRequest(
        userId: Long,
        accept: Boolean
    ): Result<Unit> =
        try {
            val response =
                if (accept) {
                    swApi.acceptFriendRequest(userId)
                } else {
                    swApi.declineFriendRequest(userId)
                }
            if (response.isSuccessful) {
                if (accept) {
                    userDao.markAsFriend(userId)
                    userDao.incrementFriendsCount()
                }
                userDao.removeFriendRequest(userId)
                userDao.decrementFriendRequestCount()
                Result.success(Unit)
            } else {
                Result.failure(handleResponseError(response, TAG, "обработке заявки в друзья"))
            }
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "обработке заявки в друзья"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "обработке заявки в друзья"))
        }

    open suspend fun friendAction(
        userId: Long,
        action: ApiFriendAction
    ): Result<Unit> =
        try {
            val response =
                when (action) {
                    ApiFriendAction.ADD -> swApi.sendFriendRequest(userId)
                    ApiFriendAction.REMOVE -> swApi.deleteFriend(userId)
                }
            if (response.isSuccessful) {
                when (action) {
                    ApiFriendAction.ADD -> { // noop - заявка отправлена, ждём подтверждения
                    }

                    ApiFriendAction.REMOVE -> {
                        userDao.removeFriend(userId)
                        userDao.decrementFriendsCount()
                    }
                }
                Result.success(Unit)
            } else {
                Result.failure(handleResponseError(response, TAG, "действии с другом"))
            }
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "действии с другом"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "действии с другом"))
        }

    open suspend fun blacklistAction(
        user: User,
        option: ApiBlacklistOption
    ): Result<Unit> =
        try {
            val response =
                when (option) {
                    ApiBlacklistOption.ADD -> swApi.addToBlacklist(user.id)
                    ApiBlacklistOption.REMOVE -> swApi.deleteFromBlacklist(user.id)
                }
            if (response.isSuccessful) {
                when (option) {
                    ApiBlacklistOption.ADD -> userDao.addToBlacklist(user.id)
                    ApiBlacklistOption.REMOVE -> userDao.removeFromBlacklist(user.id)
                }
                Result.success(Unit)
            } else {
                Result.failure(handleResponseError(response, TAG, "действии с черным списком"))
            }
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "действии с черным списком"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "действии с черным списком"))
        }

    open suspend fun getBlacklist(): Result<List<User>> =
        try {
            val blacklist = swApi.getBlacklist()
            Result.success(blacklist)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "загрузке черного списка"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "загрузке черного списка"))
        }
}
