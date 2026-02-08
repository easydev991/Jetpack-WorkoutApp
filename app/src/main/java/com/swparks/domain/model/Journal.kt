package com.swparks.domain.model

import com.swparks.ui.model.JournalAccess

/**
 * Доменная модель дневника
 *
 * Используется в Domain Layer и UI Layer для работы с дневниками пользователей
 *
 * @property id Идентификатор дневника
 * @property title Название дневника
 * @property lastMessageImage URL изображения последнего сообщения
 * @property createDate Дата создания дневника
 * @property modifyDate Дата последнего изменения дневника
 * @property lastMessageDate Дата последнего сообщения
 * @property lastMessageText Текст последнего сообщения
 * @property entriesCount Количество записей в дневнике
 * @property ownerId Идентификатор владельца дневника
 * @property viewAccess Уровень доступа для просмотра
 * @property commentAccess Уровень доступа для комментариев
 */
data class Journal(
    val id: Long,
    val title: String?,
    val lastMessageImage: String?,
    val createDate: String?,
    val modifyDate: String?,
    val lastMessageDate: String?,
    val lastMessageText: String?,
    val entriesCount: Int?,
    val ownerId: Long?,
    val viewAccess: JournalAccess?,
    val commentAccess: JournalAccess?
)
