package com.swparks.domain.usecase

import android.util.Log
import com.swparks.data.SecureTokenRepository
import com.swparks.data.repository.AuthRepository
import com.swparks.data.repository.UserProfileRepository

/**
 * Use case для удаления аккаунта пользователя.
 *
 * Выполняет следующие действия:
 * 1. Вызывает API для удаления профиля на сервере
 * 2. Очищает токен авторизации в SecureTokenRepository
 * 3. Очищает все локальные данные пользователя через AuthRepository.clearUserData()
 * 4. Сбрасывает флаг isAuthorized через AuthRepository.forceLogout()
 *
 * @param secureTokenRepository Репозиторий для безопасного хранения токена
 * @param userProfileRepository Репозиторий для работы с профилем пользователя
 * @param authRepository Репозиторий для работы с API авторизации
 */
class DeleteUserUseCase(
    private val secureTokenRepository: SecureTokenRepository,
    private val userProfileRepository: UserProfileRepository,
    private val authRepository: AuthRepository
) {
    private companion object {
        const val TAG = "DeleteUserUseCase"
    }

    /**
     * Удаляет аккаунт пользователя.
     *
     * Сначала отправляет запрос на сервер для удаления профиля,
     * затем очищает все локальные данные.
     *
     * @return Result.success если удаление прошло успешно,
     *         Result.failure с ошибкой в противном случае
     */
    suspend operator fun invoke(): Result<Unit> {
        Log.i(TAG, "Начало удаления аккаунта")

        val apiResult = userProfileRepository.deleteUser()

        return apiResult.fold(
            onSuccess = {
                secureTokenRepository.saveAuthToken(null)

                authRepository.clearUserData()

                authRepository.forceLogout()

                Log.i(TAG, "Аккаунт успешно удален")
                Result.success(Unit)
            },
            onFailure = { error ->
                Log.e(TAG, "Ошибка удаления аккаунта: ${error.message}")
                Result.failure(error)
            }
        )
    }
}
