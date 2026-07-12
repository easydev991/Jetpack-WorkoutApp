package com.swparks.domain.usecase

import com.swparks.data.SecureTokenRepository
import com.swparks.data.TokenEncoder
import com.swparks.data.repository.AuthRepository
import com.swparks.ui.model.LoginCredentials
import kotlinx.coroutines.flow.first

class ChangePasswordUseCase(
    private val authRepository: AuthRepository,
    private val secureTokenRepository: SecureTokenRepository,
    private val tokenEncoder: TokenEncoder
) {
    suspend operator fun invoke(
        current: String,
        new: String
    ): Result<Unit> {
        val result = authRepository.changePassword(current, new)

        result.onSuccess {
            val currentUser = authRepository.getCurrentUserFlow().first()
            if (currentUser != null) {
                val newToken = tokenEncoder.encode(LoginCredentials(currentUser.name, new))
                if (newToken != null) {
                    secureTokenRepository.saveAuthToken(newToken)
                }
            }
        }

        return result
    }
}
