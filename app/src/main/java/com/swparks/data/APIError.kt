package com.swparks.data

/**
 * Типы ошибок API
 *
 * Аналог enum APIError из iOS проекта (SWNetwork).
 * Используется для обработки ошибок HTTP с локализованными сообщениями.
 */
@Suppress("MagicNumber")
enum class APIError(val errorMessage: String, val statusCode: Int? = null) {
    NO_DATA("Нет данных"),
    UNKNOWN("Неизвестная ошибка"),
    BAD_REQUEST("Неверные данные формы", 400),
    INVALID_CREDENTIALS("Необходима авторизация", 401),
    NOT_FOUND("Ресурс не найден", 404),
    PAYLOAD_TOO_LARGE("Слишком большой размер данных", 413),
    SERVER_ERROR("Ошибка сервера", 500),
    INVALID_USER_ID("Неверный идентификатор пользователя"),
    TOO_MANY_REQUESTS("Слишком много запросов", 429),
    SERVICE_UNAVAILABLE("Сервер недоступен", 503),
    FORBIDDEN("Нет доступа к ресурсу", 403);

    companion object {
        /**
         * Создает ошибку на основе кода статуса
         */
        @Suppress("MagicNumber")
        fun fromStatusCode(code: Int): APIError {
            return when {
                code == 401 -> INVALID_CREDENTIALS
                code == 403 -> FORBIDDEN
                code == 404 -> NOT_FOUND
                code == 413 -> PAYLOAD_TOO_LARGE
                code == 429 -> TOO_MANY_REQUESTS
                code in 500..599 -> SERVER_ERROR
                code in listOf(502, 503, 504) -> SERVICE_UNAVAILABLE
                code in listOf(400, 402) || code in 405..412 || code in 414..428 -> BAD_REQUEST
                else -> UNKNOWN
            }
        }
    }
}
