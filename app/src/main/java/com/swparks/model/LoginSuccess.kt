package com.swparks.model

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
