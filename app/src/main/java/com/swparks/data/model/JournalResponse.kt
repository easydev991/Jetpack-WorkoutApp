package com.swparks.data.model

import com.swparks.data.database.entity.toEntity
import com.swparks.data.datetime.FlexibleDateDeserializer
import com.swparks.domain.model.Journal
import com.swparks.ui.model.JournalAccess
import com.swparks.util.parseHtmlOrNull
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
    /**
     * Текст последнего сообщения без HTML-тегов (compact mode - для превью в списке)
     */
    val parsedLastMessageText: String? = lastMessageText.parseHtmlOrNull(compactMode = true)

    val journalAccessOption: JournalAccess? = viewAccess?.let { JournalAccess.from(it) }
    val commentAccessOption: JournalAccess? =
        commentAccess?.let { JournalAccess.from(it) }
}

/**
 * Маппер для преобразования [JournalResponse] в доменную модель [Journal]
 */
fun JournalResponse.toDomain(): Journal = Journal(
    id = id,
    title = title,
    lastMessageImage = lastMessageImage,
    createDate = createDate,
    modifyDate = modifyDate,
    lastMessageDate = lastMessageDate,
    lastMessageText = lastMessageText,
    entriesCount = count,
    ownerId = ownerId?.toLong(),
    viewAccess = journalAccessOption,
    commentAccess = commentAccessOption
)

/**
 * Маппер для преобразования [JournalResponse] в [JournalEntity]
 */
fun JournalResponse.toEntity(): com.swparks.data.database.entity.JournalEntity {
    val domain = toDomain()
    return domain.toEntity()
}
