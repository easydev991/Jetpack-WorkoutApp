package com.swparks.model

import com.swparks.data.datetime.FlexibleDateDeserializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель формы для создания/редактирования мероприятия
 */
@Serializable
data class EventForm(
    val title: String,
    val description: String,
    @Serializable(with = FlexibleDateDeserializer::class)
    val date: String,
    @SerialName("area_id")
    val parkId: Long
)
