package com.swparks.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель формы для создания/редактирования площадки
 */
@Serializable
data class ParkForm(
    val address: String,
    val latitude: String,
    val longitude: String,
    @SerialName("city_id")
    val cityId: Int?,
    @SerialName("type_id")
    val typeId: Int,
    @SerialName("class_id")
    val sizeId: Int
)
