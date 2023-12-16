package com.workout.jetpack_workout.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель мероприятия
 */
@Serializable
data class Event(
    val id: Long,
    val title: String,
    val description: String,
    @SerialName("begin_date")
    val beginDate: String,
    @SerialName("country_id")
    val countryID: Int,
    @SerialName("city_id")
    val cityID: Int,
    @SerialName("comment_count")
    val commentsCount: Int,
    val preview: String,
    @SerialName("area_id")
    val parkID: Long? = null,
    val latitude: String,
    val longitude: String,
    @SerialName("user_count")
    val userCount: Int,
    /**
     * `true` - предстоящее мероприятие, `false` - прошедшее
     */
    @SerialName("is_current")
    val isCurrent: Boolean,
    val address: String? = null,
    val photos: List<Photo>,
    @SerialName("training_users")
    val trainingUsers: List<User>? = null,
    val author: User,
    val name: String,
    val comments: List<Comment>? = null,
    @SerialName("is_organizer")
    val isOrganizer: Boolean? = null,
    @SerialName("can_edit")
    val canEdit: Boolean? = null,
    @SerialName("train_here")
    val trainHere: Boolean? = null
) {
    /**
     * Есть ли описание
     */
    val hasDescription = description.isNotBlank()

    /**
     * Есть ли комментарии
     */
    val hasComments = commentsCount > 0

    /**
     * Есть ли фотографии
     */
    val hasPhotos = photos.isNotEmpty()

    /**
     * Есть ли участники
     */
    val hasParticipants = !trainingUsers.isNullOrEmpty()

    /**
     * Ссылка на мероприятие, которой можно поделиться
     */
    val shareLinkStringURL = "https://workout.su/trainings/$id"
}