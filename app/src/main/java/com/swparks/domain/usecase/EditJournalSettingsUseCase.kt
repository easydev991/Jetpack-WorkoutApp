package com.swparks.domain.usecase

import com.swparks.data.repository.SWRepository
import com.swparks.ui.model.JournalAccess

/**
 * Use case для редактирования настроек дневника
 */
class EditJournalSettingsUseCase(
    private val repository: SWRepository
) : IEditJournalSettingsUseCase {
    override suspend operator fun invoke(
        journalId: Long,
        title: String,
        userId: Long?,
        viewAccess: JournalAccess,
        commentAccess: JournalAccess
    ): Result<Unit> =
        repository.editJournalSettings(
            journalId = journalId,
            title = title,
            userId = userId,
            viewAccess = viewAccess,
            commentAccess = commentAccess
        )
}
