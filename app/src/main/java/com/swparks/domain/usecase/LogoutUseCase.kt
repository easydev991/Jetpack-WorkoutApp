package com.swparks.domain.usecase

import android.util.Log
import com.swparks.data.SecureTokenRepository
import com.swparks.data.UserPreferencesRepository
import com.swparks.data.repository.SWRepository

/**
 * Use case для выхода из учётной записи.
 *
 * Очищает токен авторизации в SecureTokenRepository, сбрасывает флаг isAuthorized
 * через SWRepository.forceLogout() и очищает userId в UserPreferencesRepository.
 * Очищает все данные пользователя из локального хранилища (через SWRepository.clearUserData()).
 *
 * @param secureTokenRepository Репозиторий для безопасного хранения токена
 * @param preferencesRepository Репозиторий для хранения настроек и userId
 * @param swRepository Репозиторий для работы с API
 */
class LogoutUseCase(
    private val secureTokenRepository: SecureTokenRepository,
    private val swRepository: SWRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ILogoutUseCase {
    /**
     * Выполняет выход из учётной записи.
     *
     * Очищает токен авторизации, сбрасывает флаг isAuthorized и userId.
     * Очищает все данные пользователя из локального хранилища.
     */
    override suspend operator fun invoke() {
        // Очищаем токен авторизации
        secureTokenRepository.saveAuthToken(null)

        // Очищаем все данные пользователя (профиль, друзья, заявки, черный список)
        swRepository.clearUserData()

        // Сбрасываем флаг isAuthorized
        swRepository.forceLogout()

        // Очищаем userId (избыточно, так как clearUserData уже сделал это)
        preferencesRepository.clearCurrentUserId()
        Log.i("LogoutUseCase", "Текущий пользователь очищен")
    }
}
