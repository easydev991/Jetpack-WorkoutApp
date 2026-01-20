package com.swparks.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель запроса настроек дневника
 */
@Serializable
data class EditJournalSettingsRequest(
    @SerialName("journal_id")
    val journalId: Long,
    val title: String,
    @SerialName("user_id")
    val userId: Int,
    @SerialName("view_access")
    val viewAccess: String,
    @SerialName("comment_access")
    val commentAccess: String
) {
    companion object {
        fun create(
            journalId: Long,
            title: String,
            userId: Int?,
            viewAccess: JournalAccess,
            commentAccess: JournalAccess
        ): EditJournalSettingsRequest {
            return EditJournalSettingsRequest(
                journalId = journalId,
                title = title,
                userId = userId ?: 0,
                viewAccess = viewAccess.rawValue.toString(),
                commentAccess = commentAccess.rawValue.toString()
            )
        }
    }
}
