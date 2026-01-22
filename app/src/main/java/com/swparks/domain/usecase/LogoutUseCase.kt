package com.swparks.domain.usecase

import com.swparks.data.SecureTokenRepository
import com.swparks.data.repository.SWRepository

/**
 * Use case для выхода из учётной записи.
 *
 * Очищает токен авторизации в SecureTokenRepository и сбрасывает флаг isAuthorized
 * через SWRepository.forceLogout().
 *
 * @param secureTokenRepository Репозиторий для безопасного хранения токена
 * @param swRepository Репозиторий для работы с API
 */
class LogoutUseCase(
    private val secureTokenRepository: SecureTokenRepository,
    private val swRepository: SWRepository
) : ILogoutUseCase {
    /**
     * Выполняет выход из учётной записи.
     *
     * Очищает токен авторизации и сбрасывает флаг isAuthorized.
     */
    override suspend operator fun invoke() {
        // Очищаем токен авторизации
        secureTokenRepository.saveAuthToken(null)

        // Сбрасываем флаг isAuthorized
        swRepository.forceLogout()
    }
}
