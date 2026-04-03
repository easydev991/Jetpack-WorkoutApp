package com.swparks.data.model

import com.swparks.data.datetime.FlexibleDateDeserializer
import com.swparks.data.serialization.IntStringSerializer
import com.swparks.data.serialization.LongStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель площадки для воркаута
 */
@Serializable
data class Park(
    @Serializable(with = LongStringSerializer::class)
    val id: Long,
    val name: String,
    @SerialName("class_id")
    /**
     * Размер площадки, модель [ParkSize]
     */
    @Serializable(with = IntStringSerializer::class)
    val sizeID: Int,
    /**
     * Тип площадки, модель [ParkType]
     */
    @SerialName("type_id")
    @Serializable(with = IntStringSerializer::class)
    val typeID: Int,
    val longitude: String,
    val latitude: String,
    val address: String,
    @SerialName("city_id")
    @Serializable(with = IntStringSerializer::class)
    val cityID: Int,
    @SerialName("country_id")
    val countryID: Int,
    @SerialName("comments_count")
    val commentsCount: Int? = null,
    val preview: String,
    @SerialName("trainings")
    @Serializable(with = IntStringSerializer::class)
    val trainingUsersCount: Int? = null,
    @Serializable(with = FlexibleDateDeserializer::class)
    @SerialName("create_date")
    val createDate: String? = null,
    @Serializable(with = FlexibleDateDeserializer::class)
    @SerialName("modify_date")
    val modifyDate: String? = null,
    val author: User? = null,
    val photos: List<Photo>? = null,
    val comments: List<Comment>? = null,
    @SerialName("train_here")
    val trainHere: Boolean? = null,
    @SerialName("equipment_ids")
    val equipmentIDS: List<Int>? = null,
    val mine: Boolean? = null,
    @SerialName("can_edit")
    val canEdit: Boolean? = null,
    @SerialName("users_train_here")
    val trainingUsers: List<User>? = null
) {
    val size: ParkSize? =
        ParkSize.entries.firstOrNull {
            it.rawValue == sizeID
        }
    val type: ParkType? =
        ParkType.entries.firstOrNull {
            it.rawValue == typeID
        }

    /**
     * Есть ли комментарии
     */
    val hasComments = !comments.isNullOrEmpty()
    private val needUpdateComments =
        commentsCount?.let {
            it > 0 && comments.isNullOrEmpty()
        } ?: false

    /**
     * Есть ли фотографии
     */
    val hasPhotos = !photos.isNullOrEmpty()

    /**
     * Есть ли участники
     */
    val hasParticipants = !trainingUsers.isNullOrEmpty()
    private val needUpdateParticipants =
        trainingUsersCount?.let {
            it > 0 && trainingUsers.isNullOrEmpty()
        } ?: false

    /**
     * Ссылка на мероприятие, которой можно поделиться
     */
    val shareLinkStringURL = "https://workout.su/areas/$id"

    /**
     * `true` - сервер прислал всю информацию о площадке, `false` - не всю
     */
    val isFull = (
        createDate != null &&
            author != null &&
            hasPhotos &&
            !needUpdateParticipants &&
            !needUpdateComments
    )
}
