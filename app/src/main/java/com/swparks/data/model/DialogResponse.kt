package com.swparks.data.model

import com.swparks.data.database.entity.DialogEntity
import com.swparks.data.datetime.FlexibleDateDeserializer
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
)

/**
 * Преобразовать DialogResponse в DialogEntity для сохранения в Room
 */
fun DialogResponse.toEntity(): DialogEntity = DialogEntity(
    id = id,
    anotherUserId = anotherUserId,
    name = name,
    image = image,
    lastMessageText = lastMessageText,
    lastMessageDate = lastMessageDate,
    unreadCount = count
)
