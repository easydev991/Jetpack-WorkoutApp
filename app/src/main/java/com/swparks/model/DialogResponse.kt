package com.swparks.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.swparks.data.datetime.FlexibleDateDeserializer

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
)
