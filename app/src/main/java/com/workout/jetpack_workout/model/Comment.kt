package com.workout.jetpack_workout.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель комментария
 */
@Serializable
data class Comment(
    @SerialName("comment_id")
    val commentID: Long,
    val body: String,
    val date: String,
    val user: User
)
