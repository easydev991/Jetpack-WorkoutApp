package com.swparks.domain.usecase

import com.swparks.data.repository.AuthRepository

/**
 * Use case для восстановления пароля пользователя.
 *
 * @param authRepository Репозиторий для работы с API авторизации
 */
class ResetPasswordUseCase(
    private val authRepository: AuthRepository
) {
    /**
     * Выполняет восстановление пароля пользователя.
     *
     * @param login Логин или email пользователя
     * @return Result<Unit> успешность операции или ошибка
     */
    suspend operator fun invoke(login: String): Result<Unit> = authRepository.resetPassword(login)
}
