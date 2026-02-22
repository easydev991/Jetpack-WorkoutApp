package com.swparks.data.model

import com.swparks.data.datetime.FlexibleDateDeserializer
import com.swparks.ui.model.Gender
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeParseException

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
 */
@Serializable
data class User(
    val id: Long,
    val name: String,
    val image: String?,
    @SerialName("city_id")
    val cityID: Int? = null,
    @SerialName("country_id")
    val countryID: Int? = null,
    @Serializable(with = FlexibleDateDeserializer::class)
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
    val journalCount: Int? = null
) {
    val genderOption: Gender? = if (genderCode != null) {
        Gender.entries.firstOrNull { it.rawValue == genderCode }
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
     * Возраст пользователя
     * Вычисляется на основе даты рождения
     * Возвращает 0 если дата отсутствует, неверна или в будущем
     */
    val age: Int
        get() {
            if (birthDate.isNullOrBlank()) {
                return 0
            }
            return try {
                val birthLocalDate = LocalDate.parse(birthDate)
                val currentDate = LocalDate.now()
                val years = Period.between(birthLocalDate, currentDate).years
                // Если дата рождения в будущем - возвращаем 0
                if (years < 0) 0 else years
            } catch (e: DateTimeParseException) {
                0
            } catch (e: IllegalArgumentException) {
                0
            }
        }
}
