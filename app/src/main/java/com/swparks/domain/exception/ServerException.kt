package com.swparks.domain.exception

/**
 * Исключение для ошибок сервера
 *
 * Используется для обработки ошибок HTTP, которые возвращаются сервером
 * в формате ErrorResponse с текстом ошибки от сервера.
 *
 * @property message Текст ошибки от сервера (поле `realMessage` из ErrorResponse)
 * @property cause Оригинальное исключение (например, HttpException или IOException)
 */
class ServerException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
