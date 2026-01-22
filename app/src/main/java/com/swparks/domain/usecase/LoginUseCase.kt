package com.swparks.domain.usecase

import com.swparks.data.SecureTokenRepository
import com.swparks.data.repository.SWRepository
import com.swparks.model.LoginSuccess

/**
 * Интерфейс для use case авторизации.
 * Создан для удобства тестирования ViewModels.
 */
interface ILoginUseCase {
    suspend operator fun invoke(token: String?): Result<LoginSuccess>
}

/**
 * Use case для авторизации пользователя.
 *
 * Сохраняет токен в SecureTokenRepository, затем вызывает login в SWRepository.
 * Токен автоматически добавляется в заголовок Authorization через TokenInterceptor.
 *
 * @param secureTokenRepository Репозиторий для безопасного хранения токена
 * @param swRepository Репозиторий для работы с API
 */
class LoginUseCase(
    private val secureTokenRepository: SecureTokenRepository,
    private val swRepository: SWRepository
) : ILoginUseCase {
    /**
     * Выполняет авторизацию пользователя.
     *
     * @param token Токен авторизации или null для очистки
     * @return Result<LoginSuccess> с userId или ошибкой
     */
    override suspend operator fun invoke(token: String?): Result<LoginSuccess> {
        // Сначала сохраняем токен в SecureTokenRepository
        secureTokenRepository.saveAuthToken(token)

        // Затем вызываем login в SWRepository
        // Токен будет автоматически добавлен в заголовок Authorization через TokenInterceptor
        return swRepository.login(null)
    }
}
