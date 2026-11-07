package com.swparks.domain.model

import com.swparks.util.parseHtmlOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель ответа диалога
 */
@Serializable
data class DialogResponse(
    @SerialName("dialog_id")
    val id: Long,
    @SerialName("user_id")
    val anotherUserId: Int?,
    val name: String?,
    val image: String?,
    @SerialName("last_message_text")
    val lastMessageText: String?,
    @Serializable(with = FlexibleDateDeserializer::class)
    @SerialName("last_message_date")
    val lastMessageDate: String?,
    val count: Int?
) {
    /**
     * Текст последнего сообщения без HTML-тегов (compact mode - для превью в списке)
     */
    val parsedLastMessageText: String? = lastMessageText.parseHtmlOrNull(compactMode = true)
}
