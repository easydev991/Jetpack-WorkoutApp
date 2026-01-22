package com.swparks.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Запрос на сброс пароля
 */
@Serializable
data class ResetPasswordRequest(
    @SerialName("username_or_email")
    val usernameOrEmail: String
)
