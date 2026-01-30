package com.swparks.domain.usecase

import android.util.Log
import com.swparks.data.SecureTokenRepository
import com.swparks.data.TokenEncoder
import com.swparks.data.UserPreferencesRepository
import com.swparks.data.repository.SWRepository
import com.swparks.model.LoginCredentials
import com.swparks.model.LoginSuccess

/**
 * Интерфейс для use case авторизации.
 * Создан для удобства тестирования ViewModels.
 */
interface ILoginUseCase {
    suspend operator fun invoke(credentials: LoginCredentials): Result<LoginSuccess>
}

/**
 * Use case для авторизации пользователя.
 *
 * Сохраняет токен в SecureTokenRepository, затем вызывает login в SWRepository.
 * Токен автоматически добавляется в заголовок Authorization через TokenInterceptor.
 * После успешной авторизации сохраняет userId в UserPreferencesRepository для использования в кэше.
 *
 * @param tokenEncoder Кодировщик токена для генерации токена из учетных данных
 * @param secureTokenRepository Репозиторий для безопасного хранения токена
 * @param swRepository Репозиторий для работы с API
 * @param preferencesRepository Репозиторий для хранения настроек и userId
 */
class LoginUseCase(
    private val tokenEncoder: TokenEncoder,
    private val secureTokenRepository: SecureTokenRepository,
    private val swRepository: SWRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ILoginUseCase {
    /**
     * Выполняет авторизацию пользователя.
     *
     * @param credentials Учетные данные пользователя (login и password)
     * @return Result<LoginSuccess> с userId или ошибкой
     */
    override suspend operator fun invoke(credentials: LoginCredentials): Result<LoginSuccess> {
        // Сначала сохраняем токен в SecureTokenRepository
        // Токен генерируется из credentials.login и credentials.password через TokenEncoder
        val token = tokenEncoder.encode(credentials)
        secureTokenRepository.saveAuthToken(token)

        // Затем вызываем login в SWRepository и передаем токен для сохранения флага авторизации
        // Токен будет автоматически добавлен в заголовок Authorization через TokenInterceptor
        val result = swRepository.login(token)

        // Сохраняем userId после успешной авторизации
        result.onSuccess { loginSuccess ->
            preferencesRepository.saveCurrentUserId(loginSuccess.userId)
            Log.i("LoginUseCase", "Пользователь сохранён: ${loginSuccess.userId}")
        }

        return result
    }
}
