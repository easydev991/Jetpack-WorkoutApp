package com.swparks.model

/**
 * Модель социальных обновлений пользователя
 * Используется для получения информации о пользователе, друзьях, заявках и черном списке
 */
data class SocialUpdates(
    val user: User,
    val friends: List<User>,
    val friendRequests: List<User>,
    val blacklist: List<User>
)
