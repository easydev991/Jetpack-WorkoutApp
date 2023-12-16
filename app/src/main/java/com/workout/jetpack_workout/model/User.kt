package com.workout.jetpack_workout.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель пользователя
 */
@Serializable
data class User (
    val id: Long,
    val name: String,
    val image: String,

    @SerialName("city_id")
    val cityID: Long? = null,

    @SerialName("country_id")
    val countryID: Long? = null,

    /**
     * Пример: "1990-11-25"
     */
    @SerialName("birth_date")
    val birthDate: String? = null,
    /**
     * Пример: "2013-01-16T03:35:54+04:00"
     */
    @SerialName("create_date")
    val createDate: String? = null,

    val email: String? = null,

    @SerialName("fullname")
    val fullName: String? = null,
    val gender: Long? = null,

    /**
     * Пример: "0"
     */
    @SerialName("friend_request_count")
    val friendRequestCount: String? = null,

    @SerialName("friend_count")
    val friendCount: Long? = null,
    /**
     * Пример: "0"
     */
    @SerialName("area_count")
    val areaCount: String? = null,

    @SerialName("added_areas")
    val addedAreas: List<Park>? = null,

    @SerialName("journal_count")
    val journalCount: Long? = null,

    val lang: String
)
