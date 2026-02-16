package com.swparks.data.model

import com.swparks.data.datetime.FlexibleDateDeserializer
import com.swparks.util.parseHtmlOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель ответа записи дневника
 */
@Serializable
data class JournalEntryResponse(
    val id: Long,
    @SerialName("journal_id")
    val journalId: Int?,
    @SerialName("user_id")
    val authorId: Int?,
    val name: String?,
    val message: String?,
    @Serializable(with = FlexibleDateDeserializer::class)
    @SerialName("create_date")
    val createDate: String?,
    @Serializable(with = FlexibleDateDeserializer::class)
    @SerialName("modify_date")
    val modifyDate: String?,
    val image: String?
) {
    /**
     * Текст записи без HTML-тегов (detail mode - сохраняет переносы)
     */
    val parsedMessage: String? = message.parseHtmlOrNull(compactMode = false)
}
