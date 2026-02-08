package com.swparks.data

import com.swparks.ui.model.LoginCredentials
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Класс для генерации токена авторизации из учетных данных пользователя.
 *
 * Токен формируется как Base64-encoded строка "login:password".
 * Использует java.util.Base64 (кроссплатформенное решение).
 */
class TokenEncoder {

    /**
     * Генерирует токен авторизации из учетных данных.
     *
     * @param credentials Учетные данные пользователя (login и password)
     * @return Base64-encoded токен или null, если логин или пароль пустые
     */
    fun encode(credentials: LoginCredentials): String? {
        val trimmedLogin = credentials.login.trim()
        val trimmedPassword = credentials.password.trim()

        if (trimmedLogin.isEmpty() || trimmedPassword.isEmpty()) {
            return null
        }

        val credentialsString = "$trimmedLogin:$trimmedPassword"
        return Base64.getEncoder()
            .encodeToString(credentialsString.toByteArray(StandardCharsets.UTF_8))
    }
}
