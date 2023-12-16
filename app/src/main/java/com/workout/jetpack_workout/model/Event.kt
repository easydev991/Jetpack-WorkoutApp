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
    @SerialName("create_date")
    val createDate: String,
    @SerialName("modify_date")
    val modifyDate: String,
    @SerialName("begin_date")
    val beginDate: String,
    @SerialName("country_id")
    val countryID: Long,
    @SerialName("city_id")
    val cityID: Long,
    @SerialName("comment_count")
    val commentCount: Long,
    val preview: String,
    @SerialName("area_id")
    val parkID: Long? = null,
    val latitude: String,
    val longitude: String,
    @SerialName("user_count")
    val userCount: Long,
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
)