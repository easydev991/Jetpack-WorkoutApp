package com.swparks.domain.usecase

import com.swparks.data.repository.JournalEntriesRepositoryImpl

/**
 * Use case для удаления записи из дневника.
 *
 * Делегирует удаление записи репозиторию, который выполняет
 * запрос к API и обновляет локальный кэш в БД.
 *
 * @property repository Репозиторий для работы с записями в дневнике
 */
class DeleteJournalEntryUseCase(
    private val repository: JournalEntriesRepositoryImpl
) {
    suspend operator fun invoke(
        userId: Long,
        journalId: Long,
        entryId: Long
    ): Result<Unit> = repository.deleteJournalEntry(userId, journalId, entryId)
}
