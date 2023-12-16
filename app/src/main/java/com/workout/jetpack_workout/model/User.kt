package com.workout.jetpack_workout.model

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
 * @property createDate Дата создания, например: "2013-01-16T03:35:54+04:00"
 * @property email Электронный адрес
 * @property fullName Настоящее имя
 * @property gender Пол, 0 - мужчина, 1 - женщина
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
    val cityID: Long? = null,
    @SerialName("country_id")
    val countryID: Long? = null,
    @SerialName("birth_date")
    val birthDate: String? = null,
    @SerialName("create_date")
    val createDate: String? = null,
    val email: String? = null,
    @SerialName("fullname")
    val fullName: String? = null,
    val gender: Long? = null,
    @SerialName("friend_request_count")
    val friendRequestCount: String? = null,
    @SerialName("friend_count")
    val friendsCount: Long? = null,
    @SerialName("area_count")
    val parksCount: String? = null,
    @SerialName("added_areas")
    val addedParks: List<Park>? = null,
    @SerialName("journal_count")
    val journalCount: Long? = null,
    val lang: String
)
