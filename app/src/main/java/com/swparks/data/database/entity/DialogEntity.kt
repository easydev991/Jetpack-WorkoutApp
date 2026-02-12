package com.swparks.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity для хранения данных диалога в Room
 *
 * @property id Идентификатор диалога
 * @property anotherUserId Идентификатор собеседника
 * @property name Имя собеседника
 * @property image URL аватара собеседника
 * @property lastMessageText Текст последнего сообщения
 * @property lastMessageDate Дата последнего сообщения
 * @property unreadCount Количество непрочитанных сообщений
 */
@Entity(tableName = "dialogs")
data class DialogEntity(
    @PrimaryKey val id: Long,
    val anotherUserId: Int?,
    val name: String?,
    val image: String?,
    val lastMessageText: String?,
    val lastMessageDate: String?,
    val unreadCount: Int?
)
