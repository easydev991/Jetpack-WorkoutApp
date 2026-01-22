package com.swparks.model

import com.swparks.data.datetime.FlexibleDateDeserializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель ответа дневника
 */
@Serializable
data class JournalResponse(
    @SerialName("journal_id")
    val id: Long,
    val title: String?,
    @SerialName("last_message_image")
    val lastMessageImage: String?,
    @Serializable(with = FlexibleDateDeserializer::class)
    @SerialName("create_date")
    val createDate: String?,
    @Serializable(with = FlexibleDateDeserializer::class)
    @SerialName("modify_date")
    val modifyDate: String?,
    @Serializable(with = FlexibleDateDeserializer::class)
    @SerialName("last_message_date")
    val lastMessageDate: String?,
    @SerialName("last_message_text")
    val lastMessageText: String?,
    val count: Int?,
    @SerialName("user_id")
    val ownerId: Int?,
    @SerialName("view_access")
    val viewAccess: Int?,
    @SerialName("comment_access")
    val commentAccess: Int?
) {
    val journalAccessOption: JournalAccess? = viewAccess?.let { JournalAccess.from(it) }
    val commentAccessOption: JournalAccess? = commentAccess?.let { JournalAccess.from(it) }
}
