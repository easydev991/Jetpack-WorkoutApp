package com.swparks.domain.usecase

import com.swparks.data.repository.SWRepository

/**
 * Интерфейс для use case восстановления пароля.
 * Создан для удобства тестирования ViewModels.
 */
interface IResetPasswordUseCase {
    /**
     * Выполняет восстановление пароля пользователя.
     *
     * @param login Логин или email пользователя
     * @return Result<Unit> успешность операции или ошибка
     */
    suspend operator fun invoke(login: String): Result<Unit>
}

/**
 * Use case для восстановления пароля пользователя.
 *
 * @param swRepository Репозиторий для работы с API
 */
class ResetPasswordUseCase(
    private val swRepository: SWRepository
) : IResetPasswordUseCase {
    /**
     * Выполняет восстановление пароля пользователя.
     *
     * @param login Логин или email пользователя
     * @return Result<Unit> успешность операции или ошибка
     */
    override suspend operator fun invoke(login: String): Result<Unit> {
        return swRepository.resetPassword(login)
    }
}
