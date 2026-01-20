package com.swparks.domain.exception

/**
 * Исключение для ошибок сети
 *
 * Используется для обработки ошибок сетевого подключения,
 * когда нет доступа к серверу или таймаут соединения.
 *
 * @property message Сообщение об ошибке сети для пользователя
 * @property cause Оригинальное исключение (IOException или другое сетевое исключение)
 */
class NetworkException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
