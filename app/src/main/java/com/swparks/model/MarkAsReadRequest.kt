package com.swparks.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель запроса отметки сообщений прочитанными
 */
@Serializable
data class MarkAsReadRequest(
    @SerialName("from_user_id")
    val fromUserId: Long
)
