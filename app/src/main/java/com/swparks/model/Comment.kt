package com.swparks.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.swparks.data.datetime.FlexibleDateDeserializer

/**
 * Модель комментария
 */
@Serializable
data class Comment(
    @SerialName("comment_id")
    val id: Long,
    val body: String?,
    @Serializable(with = FlexibleDateDeserializer::class)
    val date: String?,
    val user: User?
)
