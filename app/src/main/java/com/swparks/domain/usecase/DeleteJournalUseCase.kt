package com.swparks.domain.usecase

import com.swparks.data.repository.JournalsRepositoryImpl

/**
 * Use case для удаления дневника.
 *
 * Делегирует удаление дневника репозиторию, который выполняет
 * запрос к API и обновляет локальный кэш в БД.
 *
 * @property journalsRepository Репозиторий для работы с дневниками
 */
class DeleteJournalUseCase(
    private val journalsRepository: JournalsRepositoryImpl
) {
    suspend operator fun invoke(
        userId: Long,
        journalId: Long
    ): Result<Unit> = journalsRepository.deleteJournal(journalId, userId)
}
