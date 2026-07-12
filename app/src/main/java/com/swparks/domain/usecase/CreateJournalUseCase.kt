package com.swparks.domain.usecase

import com.swparks.data.repository.JournalsRepositoryImpl

/**
 * Use case для создания дневника
 */
class CreateJournalUseCase(
    private val journalsRepository: JournalsRepositoryImpl
) {
    suspend operator fun invoke(
        userId: Long,
        title: String
    ): Result<Unit> = journalsRepository.createJournal(title, userId)
}
