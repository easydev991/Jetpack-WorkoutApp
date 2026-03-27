package com.swparks.domain.usecase

import android.util.Log
import com.swparks.data.SecureTokenRepository
import com.swparks.data.repository.SWRepository
import com.swparks.util.CrashReporter

/**
 * Use case для выхода из учётной записи.
 *
 * Очищает токен авторизации в SecureTokenRepository, сбрасывает флаг isAuthorized
 * через SWRepository.forceLogout() и очищает userId в UserPreferencesRepository.
 * Очищает все данные пользователя из локального хранилища (через SWRepository.clearUserData()).
 *
 * @param secureTokenRepository Репозиторий для безопасного хранения токена
 * @param swRepository Репозиторий для работы с API
 */
class LogoutUseCase(
    private val secureTokenRepository: SecureTokenRepository,
    private val swRepository: SWRepository,
    private val crashReporter: CrashReporter
) : ILogoutUseCase {
    private companion object {
        const val TAG = "LogoutUseCase"
    }

    /**
     * Выполняет выход из учётной записи.
     *
     * Очищает токен авторизации, сбрасывает флаг isAuthorized.
     * Очищает все данные пользователя из локального хранилища.
     */
    override suspend operator fun invoke() {
        // Очищаем токен авторизации
        secureTokenRepository.saveAuthToken(null)

        // Очищаем все данные пользователя (профиль, друзья, заявки, черный список)
        swRepository.clearUserData()

        // Сбрасываем флаг isAuthorized
        swRepository.forceLogout()

        // Сбрасываем userId в Crashlytics
        crashReporter.setUserId(null)

        Log.i(TAG, "Текущий пользователь очищен")
    }
}
