package com.swparks.model

/**
 * Модель для хранения и валидации учетных данных пользователя на экране авторизации.
 *
 * @property login Логин или email пользователя
 * @property password Пароль пользователя
 * @property minPasswordSize Минимальная длина пароля (по умолчанию 6)
 */
data class LoginCredentials(
    val login: String = "",
    val password: String = "",
    val minPasswordSize: Int = 6
) {
    /**
     * Проверяет, что учетные данные готовы для авторизации.
     * Условия:
     * - login не пустой
     * - password содержит непробельных символов >= minPasswordSize
     */
    val isReady: Boolean
        get() = login.isNotEmpty() && password.trueCount >= minPasswordSize

    /**
     * Проверяет, что можно выполнить восстановление пароля.
     * Условие: login не пустой.
     */
    val canRestorePassword: Boolean
        get() = login.isNotEmpty()

    /**
     * Проверяет, что можно выполнить вход в систему.
     * Условия:
     * - данные готовы (isReady)
     * - нет ошибки авторизации (isError == false)
     *
     * @param isError Флаг наличия ошибки авторизации
     */
    fun canLogIn(isError: Boolean): Boolean = isReady && !isError
}

/**
 * Extension свойство для подсчета непробельных символов в строке.
 * Аналогично trueCount в Swift для паролей с пробелами.
 *
 * @return Количество непробельных символов
 */
private val String.trueCount: Int
    get() = count { !it.isWhitespace() }
