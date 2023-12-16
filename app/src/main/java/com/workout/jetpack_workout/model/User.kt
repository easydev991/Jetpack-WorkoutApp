package com.workout.jetpack_workout.model

import android.content.res.Resources
import com.workout.jetpack_workout.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель пользователя
 * @property id Идентификатор
 * @property name Логин на английском
 * @property image Ссылка на фотографию
 * @property cityID Идентификатор города
 * @property countryID Идентификатор страны
 * @property birthDate Дата рождения, например: "1990-11-25"
 * @property email Электронный адрес
 * @property fullName Настоящее имя
 * @property genderCode Пол, 0 - мужчина, 1 - женщина, модель [Gender]
 * @property friendRequestCount Количество заявок на добавление в друзья, например: "1"
 * @property friendsCount Количество друзей
 * @property parksCount Количество площадок, где тренируется, например: "2"
 * @property addedParks Добавленные площадки
 * @property journalCount Количество дневников
 * @property lang Язык
 */
@Serializable
data class User(
    val id: Long,
    val name: String,
    val image: String,
    @SerialName("city_id")
    val cityID: Int? = null,
    @SerialName("country_id")
    val countryID: Int? = null,
    @SerialName("birth_date")
    val birthDate: String? = null,
    val email: String? = null,
    @SerialName("fullname")
    val fullName: String? = null,
    @SerialName("gender")
    val genderCode: Int? = null,
    @SerialName("friend_request_count")
    val friendRequestCount: String? = null,
    @SerialName("friend_count")
    val friendsCount: Int? = null,
    @SerialName("area_count")
    val parksCount: String? = null,
    @SerialName("added_areas")
    val addedParks: List<Park>? = null,
    @SerialName("journal_count")
    val journalCount: Int? = null,
    val lang: String
) {
    val genderOption: Gender? = if (genderCode != null) {
        Gender.values().firstOrNull { it.model.rawValue == genderCode }
    } else {
        null
    }

    /**
     * Есть ли дневники
     */
    val hasJournals = if (journalCount != null) {
        journalCount > 0
    } else {
        false
    }

    /**
     * Есть ли друзья
     */
    val hasFriends = if (friendsCount != null) {
        friendsCount > 0
    } else {
        false
    }

    /**
     * Тренируется ли на каких-нибудь площадках
     */
    val hasUsedParks = if (parksCount?.toIntOrNull() != null) {
        parksCount.toInt() > 0
    } else {
        false
    }

    /**
     * Добавил ли какие-нибудь площадки
     */
    val hasAddedParks = addedParks?.isNotEmpty() ?: false

    /**
     * Сообщение для `name`
     */
    val messageFor = Resources.getSystem().getString(
        R.string.message_for_user,
        name
    )
}
