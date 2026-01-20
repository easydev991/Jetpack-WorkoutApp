package com.swparks.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель ответа для социальных обновлений пользователя
 */
@Serializable
data class SocialUpdatesResponse(
    @SerialName("user")
    val user: User,
    @SerialName("friends")
    val friends: List<User>,
    @SerialName("friend_requests")
    val friendRequests: List<User>,
    @SerialName("blacklist")
    val blacklist: List<User>
)
