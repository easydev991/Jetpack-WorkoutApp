package com.swparks.domain.usecase

import com.swparks.data.SecureTokenRepository
import com.swparks.data.TokenEncoder
import com.swparks.data.UserPreferencesRepository
import com.swparks.data.model.LoginSuccess
import com.swparks.data.repository.AuthRepository
import com.swparks.ui.model.LoginCredentials
import com.swparks.util.CrashReporter

/**
 * Use case для авторизации пользователя.
 *
 * Сохраняет токен в SecureTokenRepository, затем вызывает login в AuthRepository.
 * Токен автоматически добавляется в заголовок Authorization через TokenInterceptor.
 * После успешной авторизации сохраняет userId в UserPreferencesRepository для использования в кэше.
 *
 * @param tokenEncoder Кодировщик токена для генерации токена из учетных данных
 * @param secureTokenRepository Репозиторий для безопасного хранения токена
 * @param authRepository Репозиторий для работы с API авторизации
 * @param preferencesRepository Репозиторий для хранения настроек и userId
 */
class LoginUseCase(
    private val tokenEncoder: TokenEncoder,
    private val secureTokenRepository: SecureTokenRepository,
    private val authRepository: AuthRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val crashReporter: CrashReporter
) {
    /**
     * Выполняет авторизацию пользователя.
     *
     * @param credentials Учетные данные пользователя (login и password)
     * @return Result<LoginSuccess> с userId или ошибкой
     */
    suspend operator fun invoke(credentials: LoginCredentials): Result<LoginSuccess> {
        val token = tokenEncoder.encode(credentials)
        secureTokenRepository.saveAuthToken(token)

        // Вызываем login в AuthRepository и передаем токен для сохранения флага авторизации
        val result = authRepository.login(token)

        result.onSuccess { loginSuccess ->
            preferencesRepository.saveCurrentUserId(loginSuccess.userId)
            crashReporter.setUserId(loginSuccess.userId.toString())
        }

        return result
    }
}
