package com.swparks.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель в ответе на успешный запрос авторизации
 */
@Serializable
data class LoginSuccess(
    @SerialName("user_id")
    val userId: Long
)
