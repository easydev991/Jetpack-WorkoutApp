package com.swparks.domain.usecase

import com.swparks.data.repository.JournalsRepositoryImpl
import com.swparks.ui.model.JournalAccess

/**
 * Use case для редактирования настроек дневника
 */
class EditJournalSettingsUseCase(
    private val journalsRepository: JournalsRepositoryImpl
) {
    suspend operator fun invoke(
        journalId: Long,
        title: String,
        userId: Long?,
        viewAccess: JournalAccess,
        commentAccess: JournalAccess
    ): Result<Unit> =
        journalsRepository.editJournalSettings(
            journalId = journalId,
            title = title,
            userId = userId,
            viewAccess = viewAccess,
            commentAccess = commentAccess
        )
}
