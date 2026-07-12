package com.swparks.data.repository

import com.swparks.data.UserPreferencesRepository
import com.swparks.data.database.dao.DialogDao
import com.swparks.data.database.dao.UserDao
import com.swparks.data.database.entity.toDomain
import com.swparks.data.database.entity.toEntity
import com.swparks.data.model.LoginSuccess
import com.swparks.data.model.User
import com.swparks.domain.model.RegistrationParams
import com.swparks.network.SWApi
import com.swparks.util.CrashReporter
import com.swparks.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException

@Suppress("TooGenericExceptionCaught")
open class AuthRepository(
    private val swApi: SWApi,
    private val preferencesRepository: UserPreferencesRepository,
    private val userDao: UserDao,
    private val dialogDao: DialogDao,
    logger: Logger,
    crashReporter: CrashReporter
) : BaseRepository(logger, crashReporter) {
    companion object {
        private const val TAG = "AuthRepository"
    }

    open val isAuthorized: Flow<Boolean>
        get() = preferencesRepository.isAuthorized

    @OptIn(ExperimentalCoroutinesApi::class)
    open fun getCurrentUserFlow(): Flow<User?> =
        preferencesRepository.currentUserId
            .flatMapLatest { userId ->
                if (userId != null) {
                    logger.d(TAG, "Текущий пользователь изменился: $userId")
                    userDao.getUserByIdFlow(userId).map { entity -> entity?.toDomain() }
                } else {
                    logger.d(TAG, "Текущий пользователь отсутствует")
                    flowOf(null)
                }
            }.flowOn(Dispatchers.IO)

    open suspend fun register(params: RegistrationParams): Result<User> =
        try {
            val user =
                swApi.register(
                    name = params.name,
                    fullName = params.fullName,
                    email = params.email,
                    password = params.password,
                    birthDate = params.birthDate,
                    genderCode = params.genderCode,
                    countryId = params.countryId,
                    cityId = params.cityId
                )
            // Сохраняем пользователя в локальный кэш для отображения профиля без запроса
            userDao.insert(user.toEntity(isCurrentUser = true))
            Result.success(user)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "регистрации"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "регистрации"))
        }

    open suspend fun login(token: String?): Result<LoginSuccess> =
        try {
            val response = swApi.login()
            Result.success(response)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "авторизации"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "авторизации"))
        } catch (e: Exception) {
            crashReporter.logException(e, "Ошибка при авторизации")
            Result.failure(e)
        }

    open suspend fun resetPassword(login: String): Result<Unit> =
        try {
            swApi.resetPassword(login)
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "восстановлении пароля"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "восстановлении пароля"))
        } catch (e: Exception) {
            crashReporter.logException(e, "Ошибка при восстановлении пароля")
            Result.failure(e)
        }

    open suspend fun changePassword(
        current: String,
        new: String
    ): Result<Unit> =
        try {
            val response = swApi.changePassword(current, new)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val exception = HttpException(response)
                Result.failure(handleHttpException(exception, TAG, "смене пароля"))
            }
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "смене пароля"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "смене пароля"))
        }

    open suspend fun forceLogout() {
        preferencesRepository.clearCurrentUserId()
        logger.i(TAG, "Принудительный логаут выполнен")
    }

    open suspend fun clearUserData() {
        // Удаляем все данные пользователя (профиль, друзья, заявки, черный список)
        userDao.clearAll()
        // Удаляем все диалоги пользователя
        dialogDao.deleteAll()
        logger.i(TAG, "Все данные пользователя удалены")
        // Очищаем ID текущего пользователя
        preferencesRepository.clearCurrentUserId()
    }
}
