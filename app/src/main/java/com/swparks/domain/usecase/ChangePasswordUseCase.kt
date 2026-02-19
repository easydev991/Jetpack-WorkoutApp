package com.swparks.domain.usecase

import com.swparks.data.SecureTokenRepository
import com.swparks.data.TokenEncoder
import com.swparks.data.repository.SWRepository
import com.swparks.ui.model.LoginCredentials
import kotlinx.coroutines.flow.first

interface IChangePasswordUseCase {
    suspend operator fun invoke(current: String, new: String): Result<Unit>
}

class ChangePasswordUseCase(
    private val swRepository: SWRepository,
    private val secureTokenRepository: SecureTokenRepository,
    private val tokenEncoder: TokenEncoder
) : IChangePasswordUseCase {
    override suspend operator fun invoke(current: String, new: String): Result<Unit> {
        val result = swRepository.changePassword(current, new)

        result.onSuccess {
            // Получаем логин текущего пользователя
            val currentUser = swRepository.getCurrentUserFlow().first()
            if (currentUser != null) {
                // Генерируем и сохраняем новый токен с новым паролем
                val newToken = tokenEncoder.encode(LoginCredentials(currentUser.name, new))
                if (newToken != null) {
                    secureTokenRepository.saveAuthToken(newToken)
                }
            }
        }

        return result
    }
}
