package com.workout.jetpack_workout.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.w3c.dom.Comment

/**
 * Модель площадки для воркаута
 */
@Serializable
data class Park(
    val id: Long,
    val name: String,
    @SerialName("class_id")
    /**
     * Размер площадки, модель [ParkSize]
     */
    val sizeID: Int,
    /**
     * Тип площадки, модель [ParkType]
     */
    @SerialName("type_id")
    val typeID: Int,
    val longitude: String,
    val latitude: String,
    val address: String,
    @SerialName("city_id")
    val cityID: Int,
    @SerialName("country_id")
    val countryID: Int,
    @SerialName("comments_count")
    val commentsCount: Int,
    val preview: String,
    val trainings: Int,
    @SerialName("create_date")
    val createDate: String,
    @SerialName("modify_date")
    val modifyDate: String,
    val author: User,
    val photos: List<Photo>,
    val comments: List<Comment>? = null,
    @SerialName("train_here")
    val trainHere: Boolean? = null,
    @SerialName("equipment_ids")
    val equipmentIDS: List<Int>,
    val mine: Boolean? = null,
    @SerialName("can_edit")
    val canEdit: Boolean? = null,
    @SerialName("users_train_here")
    val trainingUsers: List<User>
) {
    val size: ParkSize? = ParkSize.values().firstOrNull {
        it.model.rawValue == sizeID
    }
    val type: ParkType? = ParkType.values().firstOrNull {
        it.model.rawValue == typeID
    }

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
    val hasParticipants = trainingUsers.isNotEmpty()

    /**
     * Ссылка на мероприятие, которой можно поделиться
     */
    val shareLinkStringURL = "https://workout.su/areas/$id"
}
