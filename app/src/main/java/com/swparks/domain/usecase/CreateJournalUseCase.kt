package com.swparks.domain.usecase

import com.swparks.data.repository.SWRepository

/**
 * Use case для создания дневника
 */
class CreateJournalUseCase(
    private val repository: SWRepository
) : ICreateJournalUseCase {
    override suspend fun invoke(userId: Long, title: String): Result<Unit> =
        repository.createJournal(title, userId)
}
