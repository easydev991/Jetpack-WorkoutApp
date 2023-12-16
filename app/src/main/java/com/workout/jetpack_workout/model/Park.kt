package com.workout.jetpack_workout.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.w3c.dom.Comment

/**
 * Модель площадки для воркаута
 */
@Serializable
data class Park (
    val id: Long,
    val name: String,

    @SerialName("class_id")
    val classID: Long,

    @SerialName("type_id")
    val typeID: Long,

    val longitude: String,
    val latitude: String,
    val address: String,

    @SerialName("city_id")
    val cityID: Long,

    @SerialName("country_id")
    val countryID: Long,

    @SerialName("comments_count")
    val commentsCount: Long,

    val preview: String,
    val trainings: Long,

    @SerialName("create_date")
    val createDate: String,

    @SerialName("modify_date")
    val modifyDate: String,

    val author: User,
    val photos: List<Photo>,
    val comments: List<Comment>,

    @SerialName("train_here")
    val trainHere: Boolean,

    @SerialName("equipment_ids")
    val equipmentIDS: List<Long>,

    val mine: Boolean,

    @SerialName("can_edit")
    val canEdit: Boolean,

    @SerialName("users_train_here")
    val usersTrainHere: List<User>
)
