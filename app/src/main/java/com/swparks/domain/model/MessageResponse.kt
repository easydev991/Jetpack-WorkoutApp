package com.swparks.domain.model

import com.swparks.util.parseHtmlOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель ответа сообщения
 */
@Serializable
data class MessageResponse(
    val id: Long,
    @SerialName("user_id")
    val userId: Int?,
    val message: String?,
    val name: String?,
    @Serializable(with = FlexibleDateDeserializer::class)
    val created: String?
) {
    /**
     * Текст сообщения без HTML-тегов (detail mode - сохраняет переносы)
     */
    val parsedMessage: String? = message.parseHtmlOrNull(compactMode = false)
}
