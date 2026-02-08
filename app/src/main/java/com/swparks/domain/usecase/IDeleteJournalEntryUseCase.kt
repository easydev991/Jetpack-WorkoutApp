package com.swparks.domain.usecase

/**
 * Интерфейс для use case удаления записи из дневника.
 * Создан для удобства тестирования ViewModels.
 */
interface IDeleteJournalEntryUseCase {
    /**
     * Удалить запись из дневника.
     *
     * @param userId Идентификатор пользователя
     * @param journalId Идентификатор дневника
     * @param entryId Идентификатор записи
     * @return Result успеха или ошибки операции
     */
    suspend operator fun invoke(
        userId: Long,
        journalId: Long,
        entryId: Long
    ): Result<Unit>
}
