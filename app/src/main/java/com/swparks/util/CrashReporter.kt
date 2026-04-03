package com.swparks.util

/**
 * Интерфейс для отправки crash-отчётов в Crashlytics.
 *
 * Позволяет мокировать crash-отчёты в тестах и использовать реальную
 * отправку отчётов в продакшене через Firebase Crashlytics.
 */
interface CrashReporter {
    /**
     * Записать исключение в Crashlytics.
     *
     * @param exception Исключение для записи
     * @param message Дополнительное сообщение (опционально)
     */
    fun logException(
        exception: Throwable,
        message: String? = null
    )

    /**
     * Установить идентификатор пользователя для Crashlytics.
     *
     * @param userId Идентификатор пользователя (null для сброса)
     */
    fun setUserId(userId: String?)

    /**
     * Установить пользовательский ключ для Crashlytics.
     *
     * @param key Ключ
     * @param value Значение (поддерживает String, Int, Boolean, Long, Double, Float)
     */
    fun setCustomKey(
        key: String,
        value: Any
    )

    /**
     * Записать лог-сообщение в Crashlytics.
     *
     * @param message Сообщение для логирования
     */
    fun log(message: String)
}
