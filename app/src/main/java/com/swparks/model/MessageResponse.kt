package com.swparks.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.swparks.data.datetime.FlexibleDateDeserializer

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
)
