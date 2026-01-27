package com.swparks.util

/**
 * Пустая реализация Logger для тестов.
 * Не выполняет никаких действий при вызове методов логирования.
 */
class NoOpLogger : Logger {
    override fun d(
        tag: String,
        message: String,
    ) {
        // Ничего не делаем в тестах
    }

    override fun w(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        // Ничего не делаем в тестах
    }

    override fun e(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        // Ничего не делаем в тестах
    }

    override fun i(
        tag: String,
        message: String,
    ) {
        // Ничего не делаем в тестах
    }
}