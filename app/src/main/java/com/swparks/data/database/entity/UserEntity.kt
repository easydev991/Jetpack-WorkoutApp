package com.swparks.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.swparks.data.database.UserConverters
import com.swparks.model.Park

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
 * @property addedParks Добавленные пользователем площадки
 * @property journalCount Количество дневников
 * @property lang Язык
 * @property isFriend Флаг - является ли пользователем другом
 * @property isFriendRequest Флаг - есть ли заявка в друзья
 * @property isBlacklisted Флаг - добавлен ли в черный список
 * @property isCurrentUser Флаг - является ли текущим авторизованным пользователем
 */
@Entity(tableName = "users")
@TypeConverters(UserConverters::class)
data class UserEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val image: String? = null,
    val cityId: Int? = null,
    val countryId: Int? = null,
    val birthDate: String? = null,
    val email: String? = null,
    val fullName: String? = null,
    val genderCode: Int? = null,
    val friendRequestCount: String? = null,
    val friendsCount: Int? = null,
    val parksCount: String? = null,
    val addedParks: List<Park>? = null,
    val journalCount: Int? = null,
    // Флаги для категоризации пользователей
    val isFriend: Boolean = false,
    val isFriendRequest: Boolean = false,
    val isBlacklisted: Boolean = false,
    val isCurrentUser: Boolean = false
)