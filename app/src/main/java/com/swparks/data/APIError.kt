package com.swparks.data

/**
 * Типы ошибок API
 *
 * Аналог enum APIError из iOS проекта (SWNetwork).
 * Используется для обработки ошибок HTTP с локализованными сообщениями.
 */
@Suppress("MagicNumber")
enum class APIError(
    val errorMessage: String,
    val statusCode: Int? = null
) {
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
        fun fromStatusCode(code: Int): APIError =
            when (code) {
                401 -> INVALID_CREDENTIALS
                403 -> FORBIDDEN
                404 -> NOT_FOUND
                413 -> PAYLOAD_TOO_LARGE
                429 -> TOO_MANY_REQUESTS
                in 500..599 -> SERVER_ERROR
                in listOf(502, 503, 504) -> SERVICE_UNAVAILABLE
                in listOf(400, 402), in 405..412, in 414..428 -> BAD_REQUEST
                else -> UNKNOWN
            }
    }
}
