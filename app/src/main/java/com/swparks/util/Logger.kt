package com.swparks.util

/**
 * Интерфейс для логирования.
 * Позволяет мокировать логирование в тестах и использовать реальные логи в продакшене.
 */
interface Logger {
    /**
     * Логирование уровня DEBUG.
     *
     * @param tag Тег для идентификации источника лога
     * @param message Сообщение для логирования
     */
    fun d(
        tag: String,
        message: String,
    )

    /**
     * Логирование уровня WARNING.
     *
     * @param tag Тег для идентификации источника лога
     * @param message Сообщение для логирования
     * @param throwable Опциональное исключение для логирования
     */
    fun w(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )

    /**
     * Логирование уровня ERROR.
     *
     * @param tag Тег для идентификации источника лога
     * @param message Сообщение для логирования
     * @param throwable Опциональное исключение для логирования
     */
    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )

    /**
     * Логирование уровня INFO.
     *
     * @param tag Тег для идентификации источника лога
     * @param message Сообщение для логирования
     */
    fun i(
        tag: String,
        message: String,
    )
}
