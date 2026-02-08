package com.swparks.domain.model

import com.swparks.data.model.JournalEntryResponse

/**
 * Доменная модель записи в дневнике
 *
 * Используется в Domain Layer и UI Layer для работы с записями в дневниках пользователей
 *
 * @property id Идентификатор записи
 * @property journalId Идентификатор дневника
 * @property authorId Идентификатор автора записи
 * @property authorName Имя автора записи
 * @property message Текст записи
 * @property createDate Дата создания записи (в формате ISO)
 * @property modifyDate Дата последнего изменения записи (в формате ISO)
 * @property authorImage URL изображения автора
 */
data class JournalEntry(
    val id: Long,
    val journalId: Long?,
    val authorId: Long?,
    val authorName: String?,
    val message: String?,
    val createDate: String?,
    val modifyDate: String?,
    val authorImage: String?
)

/**
 * Маппер для преобразования API-модели [JournalEntryResponse] в доменную модель [JournalEntry]
 *
 * Важные замечания:
 * - Даты передаются в формате ISO (например, `2023-10-10T12:00:00`) без форматирования
 * - Форматирование выполняется в UI при отображении через [DateFormatter]
 * - [journalId] и [authorId] приводятся из [Int?] в [Long?] для консистентности с другими ID в проекте
 *
 * @return Доменная модель [JournalEntry]
 */
fun JournalEntryResponse.toDomain(): JournalEntry = JournalEntry(
    id = id,
    journalId = journalId?.toLong(),
    authorId = authorId?.toLong(),
    authorName = name,
    message = message,
    createDate = createDate,
    modifyDate = modifyDate,
    authorImage = image
)
