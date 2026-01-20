package com.swparks.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель запроса смены пароля
 */
@Serializable
data class ChangePasswordRequest(
    val password: String,
    @SerialName("new_password")
    val newPassword: String
)
