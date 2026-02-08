package com.swparks.data.database.entity

import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.swparks.domain.model.JournalEntry
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Entity для хранения записи дневника в Room
 *
 * @property id Идентификатор записи (Primary Key)
 * @property journalId Идентификатор дневника
 * @property authorId Идентификатор автора записи
 * @property authorName Имя автора записи
 * @property message Текст записи
 * @property createDate Дата создания записи (ISO формат)
 * @property modifyDate Дата последнего изменения записи (timestamp для сортировки)
 * @property authorImage URL изображения автора
 */
@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey
    val id: Long,

    val journalId: Long?,
    val authorId: Long?,
    val authorName: String?,
    val message: String?,
    val createDate: String?,
    val modifyDate: Long,
    val authorImage: String?
)

/**
 * Маппер для преобразования доменной модели [JournalEntry] в [JournalEntryEntity]
 *
 * Конвертирует дату из String формата ISO8601 в Long timestamp
 * для корректной сортировки в базе данных
 */
fun JournalEntry.toEntity(): JournalEntryEntity = JournalEntryEntity(
    id = id,
    journalId = journalId,
    authorId = authorId,
    authorName = authorName,
    message = message,
    createDate = createDate,
    modifyDate = parseDateToTimestamp(modifyDate),
    authorImage = authorImage
)

/**
 * Вспомогательная функция для конвертации строки даты в timestamp
 *
 * @param dateString Строка даты в ISO формате
 * @return Timestamp (Long) или 0 если дата пустая
 */
private fun parseDateToTimestamp(dateString: String?): Long {
    if (dateString.isNullOrBlank()) {
        return 0L
    }

    return try {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        format.isLenient = false
        val date = format.parse(dateString)
        date?.time ?: 0L
    } catch (_: Exception) {
        // Если не удалось распарсить дату, возвращаем 0
        Log.e("JournalEntryMapper", "Не удалось распарсить дату: $dateString")
        0L
    }
}

/**
 * Маппер для преобразования [JournalEntryEntity] в доменную модель [JournalEntry]
 *
 * Конвертирует дату из Long timestamp обратно в String (ISO формат)
 */
fun JournalEntryEntity.toDomain(): JournalEntry = JournalEntry(
    id = id,
    journalId = journalId,
    authorId = authorId,
    authorName = authorName,
    message = message,
    createDate = createDate,
    modifyDate = parseTimestampToDate(modifyDate),
    authorImage = authorImage
)

/**
 * Вспомогательная функция для конвертации timestamp в строку даты
 *
 * @param timestamp Timestamp (Long)
 * @return Строка даты в ISO формате или null если timestamp равен 0
 */
private fun parseTimestampToDate(timestamp: Long): String? {
    if (timestamp == 0L) {
        return null
    }

    return try {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        format.format(java.util.Date(timestamp))
    } catch (_: Exception) {
        // Если не удалось конвертировать timestamp, возвращаем null
        Log.e("JournalEntryMapper", "Не удалось конвертировать timestamp в дату: $timestamp")
        null
    }
}
