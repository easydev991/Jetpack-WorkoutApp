package com.swparks.domain.usecase

import com.swparks.data.repository.SWRepository

/**
 * Use case для удаления дневника.
 *
 * Делегирует удаление дневника репозиторию, который выполняет
 * запрос к API и обновляет локальный кэш в БД.
 *
 * @property swRepository Репозиторий для работы с дневниками
 */
class DeleteJournalUseCase(
    private val swRepository: SWRepository
) : IDeleteJournalUseCase {
    override suspend operator fun invoke(
        userId: Long,
        journalId: Long
    ): Result<Unit> = swRepository.deleteJournal(journalId, userId)
}
