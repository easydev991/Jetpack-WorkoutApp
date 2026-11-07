package com.swparks.data.database.entity

import com.swparks.domain.model.DialogResponse

/**
 * Преобразовать DialogResponse в DialogEntity для сохранения в Room
 */
fun DialogResponse.toEntity(): DialogEntity =
    DialogEntity(
        id = id,
        anotherUserId = anotherUserId,
        name = name,
        image = image,
        lastMessageText = lastMessageText,
        lastMessageDate = lastMessageDate,
        unreadCount = count
    )
