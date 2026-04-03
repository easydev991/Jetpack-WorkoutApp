package com.swparks.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.swparks.data.model.Comment
import com.swparks.data.model.Event
import com.swparks.data.model.Photo
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
    val authorImage: String?,
    @ColumnInfo(name = "address")
    val address: String? = null,
    @ColumnInfo(name = "photos")
    val photos: List<Photo>? = null,
    @ColumnInfo(name = "training_users")
    val trainingUsers: List<User>? = null,
    @ColumnInfo(name = "comments")
    val comments: List<Comment>? = null,
    @ColumnInfo(name = "author")
    val author: User? = null,
    @ColumnInfo(name = "comments_count")
    val commentsCount: Int? = null,
    @ColumnInfo(name = "training_users_count")
    val trainingUsersCount: Int? = null,
    @ColumnInfo(name = "park_id")
    val parkId: Long? = null,
    @ColumnInfo(name = "is_current")
    val isCurrent: Boolean = false,
    @ColumnInfo(name = "is_organizer")
    val isOrganizer: Boolean? = null,
    @ColumnInfo(name = "can_edit")
    val canEdit: Boolean? = null,
    @ColumnInfo(name = "train_here")
    val trainHere: Boolean? = null,
    @ColumnInfo(name = "is_full")
    val isFull: Boolean = false
)

/**
 * Маппер для преобразования модели [Event] в [EventEntity]
 *
 * Для списка past events сохраняется только частичный snapshot.
 */
fun Event.toPartialEntity(): EventEntity =
    EventEntity(
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
        authorImage = author.image,
        isFull = false
    )

/**
 * Полный snapshot past event для cache-first details.
 */
fun Event.toFullEntity(): EventEntity =
    EventEntity(
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
        authorImage = author.image,
        address = address,
        photos = photos,
        trainingUsers = trainingUsers,
        comments = comments,
        author = author,
        commentsCount = commentsCount,
        trainingUsersCount = trainingUsersCount,
        parkId = parkID,
        isCurrent = isCurrent,
        isOrganizer = isOrganizer,
        canEdit = canEdit,
        trainHere = trainHere,
        isFull = true
    )

/**
 * Маппер для преобразования [EventEntity] в модель [Event]
 *
 * Для partial cache восстанавливаются только поля списка.
 * Для full cache подставляются сохранённые details-данные.
 */
fun EventEntity.toEvent(): Event =
    Event(
        id = id,
        title = title,
        description = description,
        beginDate = beginDate,
        countryID = countryId,
        cityID = cityId,
        commentsCount = commentsCount,
        preview = preview,
        parkID = parkId,
        latitude = latitude,
        longitude = longitude,
        trainingUsersCount = trainingUsersCount,
        isCurrent = isCurrent,
        address = address,
        photos = photos.orEmpty(),
        trainingUsers = trainingUsers,
        author =
            author
                ?: User(
                    id = authorId ?: 0L,
                    name = authorName ?: "",
                    image = authorImage
                ),
        name = title,
        comments = comments,
        isOrganizer = isOrganizer,
        canEdit = canEdit,
        trainHere = trainHere
    )
