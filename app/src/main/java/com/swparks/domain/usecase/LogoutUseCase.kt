package com.swparks.domain.usecase

import android.util.Log
import com.swparks.data.SecureTokenRepository
import com.swparks.data.repository.AuthRepository
import com.swparks.util.CrashReporter

/**
 * Use case для выхода из учётной записи.
 *
 * Очищает токен авторизации в SecureTokenRepository, сбрасывает флаг isAuthorized
 * через AuthRepository.forceLogout() и очищает userId в UserPreferencesRepository.
 * Очищает все данные пользователя из локального хранилища (через AuthRepository.clearUserData()).
 *
 * @param secureTokenRepository Репозиторий для безопасного хранения токена
 * @param authRepository Репозиторий для работы с API авторизации
 */
class LogoutUseCase(
    private val secureTokenRepository: SecureTokenRepository,
    private val authRepository: AuthRepository,
    private val crashReporter: CrashReporter
) {
    private companion object {
        const val TAG = "LogoutUseCase"
    }

    /**
     * Выполняет выход из учётной записи.
     *
     * Очищает токен авторизации, сбрасывает флаг isAuthorized.
     * Очищает все данные пользователя из локального хранилища.
     */
    suspend operator fun invoke() {
        secureTokenRepository.saveAuthToken(null)

        authRepository.clearUserData()

        authRepository.forceLogout()

        crashReporter.setUserId(null)

        Log.i(TAG, "Текущий пользователь очищен")
    }
}
