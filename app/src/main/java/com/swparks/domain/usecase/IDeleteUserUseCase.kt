package com.swparks.domain.usecase

/**
 * Use case для удаления аккаунта пользователя.
 *
 * Вызывает API для удаления профиля на сервере, затем очищает
 * токен авторизации и все локальные данные пользователя.
 */
interface IDeleteUserUseCase {
    /**
     * Удаляет аккаунт пользователя.
     *
     * @return Result.success если удаление прошло успешно,
     *         Result.failure с ошибкой в противном случае
     */
    suspend operator fun invoke(): Result<Unit>
}
