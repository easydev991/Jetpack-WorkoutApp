package com.swparks.data.database.entity

import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.swparks.domain.model.Journal
import com.swparks.ui.model.JournalAccess
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Entity для хранения дневника в Room
 *
 * @property id Идентификатор дневника (Primary Key)
 * @property title Название дневника
 * @property lastMessageImage URL изображения последнего сообщения
 * @property createDate Дата создания дневника (ISO формат)
 * @property modifyDate Дата последнего изменения дневника (timestamp для сортировки)
 * @property lastMessageDate Дата последнего сообщения (ISO формат)
 * @property lastMessageText Текст последнего сообщения
 * @property entriesCount Количество записей в дневнике
 * @property ownerId Идентификатор владельца дневника
 * @property viewAccess Уровень доступа для просмотра (0 - все, 1 - друзья, 2 - никто)
 * @property commentAccess Уровень доступа для комментариев (0 - все, 1 - друзья, 2 - никто)
 */
@Entity(tableName = "journals")
data class JournalEntity(
    @PrimaryKey
    val id: Long,

    val title: String?,
    val lastMessageImage: String?,
    val createDate: String?,
    val modifyDate: Long,
    val lastMessageDate: String?,
    val lastMessageText: String?,
    val entriesCount: Int?,
    val ownerId: Long?,
    val viewAccess: Int?,
    val commentAccess: Int?
)

/**
 * Маппер для преобразования доменной модели [Journal] в [JournalEntity]
 *
 * Конвертирует даты из String формата ISO8601 в Long timestamp
 * для корректной сортировки в базе данных
 */
fun Journal.toEntity(): JournalEntity = JournalEntity(
    id = id,
    title = title,
    lastMessageImage = lastMessageImage,
    createDate = createDate,
    modifyDate = parseDateToTimestamp(modifyDate),
    lastMessageDate = lastMessageDate,
    lastMessageText = lastMessageText,
    entriesCount = entriesCount,
    ownerId = ownerId,
    viewAccess = viewAccess?.rawValue,
    commentAccess = commentAccess?.rawValue
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
        Log.e("JournalMapper", "Не удалось распарсить дату: $dateString")
        0L
    }
}

/**
 * Маппер для преобразования [JournalEntity] в доменную модель [Journal]
 *
 * Конвертирует даты из Long timestamp обратно в String (ISO формат)
 * и мапит уровни доступа из Int в JournalAccess
 */
fun JournalEntity.toDomain(): Journal = Journal(
    id = id,
    title = title,
    lastMessageImage = lastMessageImage,
    createDate = createDate,
    modifyDate = parseTimestampToDate(modifyDate),
    lastMessageDate = lastMessageDate,
    lastMessageText = lastMessageText,
    entriesCount = entriesCount,
    ownerId = ownerId,
    viewAccess = viewAccess?.let { JournalAccess.from(it) },
    commentAccess = commentAccess?.let { JournalAccess.from(it) }
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
        Log.e("JournalMapper", "Не удалось конвертировать timestamp в дату: $timestamp")
        null
    }
}
