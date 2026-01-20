package com.swparks.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель данных для десериализации ответов сервера с ошибками
 *
 * iOS-аналог: `ErrorResponse.swift` с полями `errors`, `message`, `name`, `code`, `status`
 */
@Serializable
data class ErrorResponse(
    @SerialName("errors")
    val errors: List<String> = emptyList(),
    @SerialName("name")
    val name: String? = null,
    @SerialName("message")
    val message: String? = null,
    @SerialName("code")
    val code: Int = 0,
    @SerialName("status")
    val status: Int = 0
) {
    /**
     * Возвращает реальное сообщение об ошибке от сервера
     *
     * Приоритет:
     * 1. Поле `message`, если оно заполнено
     * 2. Список `errors`, если он не пуст (объединенный через запятую)
     * 3. `null`, если ни одно поле не заполнено
     */
    val realMessage: String?
        get() = message ?: errors.takeIf { it.isNotEmpty() }?.joinToString(", ")

    /**
     * Возвращает реальный код ошибки
     *
     * Приоритет:
     * 1. Поле `code`, если оно не равно 0
     * 2. Поле `status`, если оно не равно 0
     * 3. `statusCode` из HTTP ответа, если передан
     * 4. 0, если ничего не подходит
     */
    fun makeRealCode(statusCode: Int?): Int {
        val realCode = if (code != 0) code else status
        return if (realCode != 0) realCode else (statusCode ?: 0)
    }
}
