package com.swparks.domain.model

/**
 * Параметры для регистрации пользователя.
 *
 * @property name Логин пользователя
 * @property fullName Полное имя
 * @property email Email
 * @property password Пароль
 * @property birthDate Дата рождения в формате ISO (YYYY-MM-DD)
 * @property genderCode Код пола (0 - мужской, 1 - женский)
 * @property countryId ID страны (опционально)
 * @property cityId ID города (опционально)
 */
data class RegistrationParams(
    val name: String,
    val fullName: String,
    val email: String,
    val password: String,
    val birthDate: String,
    val genderCode: Int,
    val countryId: Int?,
    val cityId: Int?
)
