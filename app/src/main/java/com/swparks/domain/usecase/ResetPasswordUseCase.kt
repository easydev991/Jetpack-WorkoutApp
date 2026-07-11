package com.swparks.domain.usecase

import com.swparks.data.repository.SWRepository

/**
 * Use case для восстановления пароля пользователя.
 *
 * @param swRepository Репозиторий для работы с API
 */
class ResetPasswordUseCase(
    private val swRepository: SWRepository
) {
    /**
     * Выполняет восстановление пароля пользователя.
     *
     * @param login Логин или email пользователя
     * @return Result<Unit> успешность операции или ошибка
     */
    suspend operator fun invoke(login: String): Result<Unit> = swRepository.resetPassword(login)
}
