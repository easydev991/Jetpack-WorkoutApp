package com.swparks.domain.usecase

/**
 * Интерфейс для use case удаления дневника.
 * Создан для удобства тестирования ViewModels.
 */
interface IDeleteJournalUseCase {
    /**
     * Удалить дневник.
     *
     * @param userId Идентификатор пользователя
     * @param journalId Идентификатор дневника
     * @return Result успеха или ошибки операции
     */
    suspend operator fun invoke(
        userId: Long,
        journalId: Long
    ): Result<Unit>
}
