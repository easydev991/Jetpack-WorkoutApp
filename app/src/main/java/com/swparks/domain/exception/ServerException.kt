package com.swparks.domain.exception

/**
 * Исключение для ошибок сервера
 *
 * Используется для обработки ошибок HTTP, которые возвращаются сервером
 * в формате ErrorResponse с текстом ошибки от сервера.
 *
 * @property message Текст ошибки от сервера (поле `realMessage` из ErrorResponse)
 * @property cause Оригинальное исключение (например, HttpException или IOException)
 * @property code Уточняющий код ошибки (опционально): `404` — настоящий API-404
 *  (ресурс удалён на сервере), `503` — инфраструктурный 404 (HTML/чужой JSON от
 *  прокси, «Сервер недоступен», деструктивные действия запрещены); `null` —
 *  прочие серверные ошибки
 */
class ServerException(
    message: String,
    cause: Throwable? = null,
    val code: Int? = null
) : Exception(message, cause)
