package com.swparks.domain.usecase

import com.swparks.domain.repository.JournalEntriesRepository

/**
 * Use case для проверки возможности удаления записи из дневника.
 *
 * Первую запись в дневнике (с минимальным id) нельзя удалить,
 * так как сервер не позволяет удалить самую первую запись.
 *
 * @property repository Репозиторий для работы с записями в дневнике
 */
class CanDeleteJournalEntryUseCase(
    private val repository: JournalEntriesRepository
) : ICanDeleteJournalEntryUseCase {

    override suspend operator fun invoke(entryId: Long, journalId: Long): Boolean {
        return repository.canDeleteEntry(entryId, journalId)
    }
}
