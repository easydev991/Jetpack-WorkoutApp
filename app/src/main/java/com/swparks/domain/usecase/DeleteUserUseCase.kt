package com.swparks.domain.usecase

import android.util.Log
import com.swparks.data.SecureTokenRepository
import com.swparks.data.repository.SWRepository

/**
 * Use case для удаления аккаунта пользователя.
 *
 * Выполняет следующие действия:
 * 1. Вызывает API для удаления профиля на сервере
 * 2. Очищает токен авторизации в SecureTokenRepository
 * 3. Очищает все локальные данные пользователя через SWRepository.clearUserData()
 * 4. Сбрасывает флаг isAuthorized через SWRepository.forceLogout()
 *
 * @param secureTokenRepository Репозиторий для безопасного хранения токена
 * @param swRepository Репозиторий для работы с API и локальными данными
 */
class DeleteUserUseCase(
    private val secureTokenRepository: SecureTokenRepository,
    private val swRepository: SWRepository
) : IDeleteUserUseCase {
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
    override suspend operator fun invoke(): Result<Unit> {
        Log.i(TAG, "Начало удаления аккаунта")

        // Отправляем запрос на сервер для удаления профиля
        val apiResult = swRepository.deleteUser()

        return apiResult.fold(
            onSuccess = {
                // Очищаем токен авторизации
                secureTokenRepository.saveAuthToken(null)

                // Очищаем все данные пользователя (профиль, друзья, заявки, черный список, диалоги)
                swRepository.clearUserData()

                // Сбрасываем флаг isAuthorized
                swRepository.forceLogout()

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
