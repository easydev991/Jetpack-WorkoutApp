package com.swparks.domain.usecase

/** Интерфейс для use case синхронизации записей дневника. Создан для удобства тестирования ViewModels. */
interface ISyncJournalEntriesUseCase {
    suspend operator fun invoke(
        userId: Long,
        journalId: Long
    ): Result<Unit>
}
