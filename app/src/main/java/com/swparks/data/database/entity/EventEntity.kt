package com.swparks.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.swparks.data.model.Event
import com.swparks.data.model.User

/**
 * Entity для хранения прошедшего мероприятия в Room
 *
 * Хранит только прошедшие мероприятия (past events).
 * Поля соответствуют ответу сервера.
 *
 * @property id Идентификатор мероприятия (Primary Key)
 * @property title Название мероприятия
 * @property description Описание
 * @property beginDate Дата начала (ISO формат)
 * @property countryId Идентификатор страны
 * @property cityId Идентификатор города
 * @property preview URL превью изображения
 * @property latitude Широта
 * @property longitude Долгота
 * @property authorId Идентификатор автора
 * @property authorName Имя автора
 * @property authorImage URL изображения автора
 */
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey
    val id: Long,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "begin_date")
    val beginDate: String,

    @ColumnInfo(name = "country_id")
    val countryId: Int,

    @ColumnInfo(name = "city_id")
    val cityId: Int,

    @ColumnInfo(name = "preview")
    val preview: String,

    @ColumnInfo(name = "latitude")
    val latitude: String,

    @ColumnInfo(name = "longitude")
    val longitude: String,

    @ColumnInfo(name = "author_id")
    val authorId: Long?,

    @ColumnInfo(name = "author_name")
    val authorName: String?,

    @ColumnInfo(name = "author_image")
    val authorImage: String?
)

/**
 * Маппер для преобразования модели [Event] в [EventEntity]
 *
 * Автор сохраняется с 3 полями (id, name, image) для офлайн доступа к деталям мероприятия.
 */
fun Event.toEntity(): EventEntity = EventEntity(
    id = id,
    title = title,
    description = description,
    beginDate = beginDate,
    countryId = countryID,
    cityId = cityID,
    preview = preview,
    latitude = latitude,
    longitude = longitude,
    authorId = author.id,
    authorName = author.name,
    authorImage = author.image
)

/**
 * Маппер для преобразования [EventEntity] в модель [Event]
 *
 * Все события в БД - прошедшие (isCurrent = false).
 * Автор восстанавливается из сохраненных полей.
 * Если данные отсутствуют, используются дефолтные значения (id=0, name="").
 */
fun EventEntity.toEvent(): Event = Event(
    id = id,
    title = title,
    description = description,
    beginDate = beginDate,
    countryID = countryId,
    cityID = cityId,
    preview = preview,
    latitude = latitude,
    longitude = longitude,
    isCurrent = false,
    photos = emptyList(),
    author = User(
        id = authorId ?: 0L,
        name = authorName ?: "",
        image = authorImage
    ),
    name = title
)
