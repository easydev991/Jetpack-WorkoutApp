package com.swparks.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity для хранения данных пользователя в Room
 *
 * @property id Идентификатор пользователя
 * @property name Логин на английском
 * @property image Ссылка на фотографию
 * @property cityId Идентификатор города
 * @property countryId Идентификатор страны
 * @property birthDate Дата рождения
 * @property email Электронный адрес
 * @property fullName Настоящее имя
 * @property genderCode Пол (0 - мужчина, 1 - женщина)
 * @property friendRequestCount Количество заявок на добавление в друзья
 * @property friendsCount Количество друзей
 * @property parksCount Количество площадок, где тренируется
 * @property journalCount Количество дневников
 * @property lang Язык
 * @property isFriend Флаг - является ли пользователем другом
 * @property isFriendRequest Флаг - есть ли заявка в друзья
 * @property isBlacklisted Флаг - добавлен ли в черный список
 * @property isCurrentUser Флаг - является ли текущим авторизованным пользователем
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val image: String,
    val cityId: Int? = null,
    val countryId: Int? = null,
    val birthDate: String? = null,
    val email: String? = null,
    val fullName: String? = null,
    val genderCode: Int? = null,
    val friendRequestCount: String? = null,
    val friendsCount: Int? = null,
    val parksCount: String? = null,
    val journalCount: Int? = null,
    val lang: String,
    // Флаги для категоризации пользователей
    val isFriend: Boolean = false,
    val isFriendRequest: Boolean = false,
    val isBlacklisted: Boolean = false,
    val isCurrentUser: Boolean = false
)
