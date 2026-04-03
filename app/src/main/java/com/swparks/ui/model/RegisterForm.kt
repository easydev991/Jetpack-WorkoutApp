package com.swparks.ui.model

import java.time.LocalDate

/**
 * Модель формы регистрации пользователя.
 *
 * @property login Логин пользователя
 * @property email Email пользователя
 * @property password Пароль
 * @property fullName Полное имя
 * @property genderCode Код пола (0 - мужской, 1 - женский)
 * @property birthDate Дата рождения
 * @property countryId ID выбранной страны (String из справочника)
 * @property cityId ID выбранного города (String из справочника)
 * @property isPolicyAccepted Флаг принятия пользовательского соглашения
 */
data class RegisterForm(
    val login: String = "",
    val email: String = "",
    val password: String = "",
    val fullName: String = "",
    val genderCode: Int? = null,
    val birthDate: LocalDate? = null,
    val countryId: String? = null,
    val cityId: String? = null,
    val isPolicyAccepted: Boolean = false
) {
    /**
     * Проверяет, что форма валидна для отправки.
     * Условия:
     * - login не пустой
     * - email валидный
     * - password не пустой (минимум 6 символов)
     * - fullName не пустое
     * - genderCode выбран
     * - birthDate указана и не в будущем
     * - countryId выбран
     * - cityId выбран
     * - isPolicyAccepted == true
     */
    val isValid: Boolean
        get() =
            login.isNotBlank() &&
                email.isValidEmail() &&
                password.trueCount >= MIN_PASSWORD_LENGTH &&
                fullName.isNotBlank() &&
                genderCode != null &&
                birthDate != null &&
                !birthDate.isInFuture() &&
                countryId != null &&
                cityId != null &&
                isPolicyAccepted

    companion object {
        const val MIN_PASSWORD_LENGTH = 6
    }
}

/**
 * Проверяет валидность email адреса.
 */
private fun String.isValidEmail(): Boolean {
    if (isBlank()) return false
    val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    return emailRegex.matches(this)
}

/**
 * Подсчет непробельных символов в строке.
 */
private val String.trueCount: Int
    get() = count { !it.isWhitespace() }

/**
 * Проверяет, что дата не в будущем.
 */
private fun LocalDate.isInFuture(): Boolean = this > LocalDate.now()
