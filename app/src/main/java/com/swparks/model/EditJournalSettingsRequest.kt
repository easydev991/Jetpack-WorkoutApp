package com.swparks.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель запроса настроек дневника
 */
@Serializable
data class EditJournalSettingsRequest(
    val title: String,
    @SerialName("view_access")
    val viewAccess: String,
    @SerialName("comment_access")
    val commentAccess: String
) {
    companion object {
        fun create(
            title: String,
            viewAccess: JournalAccess,
            commentAccess: JournalAccess
        ): EditJournalSettingsRequest {
            return EditJournalSettingsRequest(
                title = title,
                viewAccess = viewAccess.rawValue.toString(),
                commentAccess = commentAccess.rawValue.toString()
            )
        }
    }
}
