package com.swparks.ui.model

import com.swparks.data.datetime.FlexibleDateDeserializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель формы для создания/редактирования пользователя
 */
@Serializable
data class MainUserForm(
    val name: String,
    val fullname: String,
    val email: String,
    val password: String,
    @Serializable(with = FlexibleDateDeserializer::class)
    @SerialName("birth_date")
    val birthDate: String,
    @SerialName("gender")
    val genderCode: Int,
    @SerialName("country_id")
    val countryId: Int? = null,
    @SerialName("city_id")
    val cityId: Int? = null
)
