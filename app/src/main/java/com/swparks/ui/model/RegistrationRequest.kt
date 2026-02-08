package com.swparks.ui.model

import com.swparks.data.datetime.FlexibleDateDeserializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель запроса регистрации нового пользователя
 */
@Serializable
data class RegistrationRequest(
    val name: String,
    @SerialName("fullname")
    val fullName: String,
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
