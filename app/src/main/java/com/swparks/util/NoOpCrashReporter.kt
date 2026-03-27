package com.swparks.util

/**
 * Пустая реализация CrashReporter для тестов.
 * Не выполняет никаких действий при вызове методов.
 */
class NoOpCrashReporter : CrashReporter {
    override fun logException(
        exception: Throwable,
        message: String?,
    ) {
        // Ничего не делаем в тестах
    }

    override fun setUserId(userId: String?) {
        // Ничего не делаем в тестах
    }

    override fun setCustomKey(
        key: String,
        value: Any,
    ) {
        // Ничего не делаем в тестах
    }

    override fun log(message: String) {
        // Ничего не делаем в тестах
    }
}