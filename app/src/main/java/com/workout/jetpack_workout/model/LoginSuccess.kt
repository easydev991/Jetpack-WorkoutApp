package com.workout.jetpack_workout.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель в ответе на успешный запрос авторизации
 */
@Serializable
data class LoginSuccess(
    @SerialName("user_id")
    val userID: Int
)
