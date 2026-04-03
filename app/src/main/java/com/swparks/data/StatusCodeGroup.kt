package com.swparks.data

/**
 * Группа кодов статуса HTTP
 *
 * Аналог enum StatusCodeGroup из iOS проекта (SWNetwork).
 * Используется для классификации HTTP кодов статуса.
 */
@Suppress("MagicNumber")
enum class StatusCodeGroup {
    INFO,
    SUCCESS,
    REDIRECT,
    CLIENT_ERROR,
    SERVER_ERROR,
    UNKNOWN;

    companion object {
        /**
         * Создает группу на основе кода статуса
         */
        @Suppress("MagicNumber")
        fun fromCode(code: Int): StatusCodeGroup =
            when (code) {
                in 100..199 -> INFO
                in 200..299 -> SUCCESS
                in 300..399 -> REDIRECT
                in 400..499 -> CLIENT_ERROR
                in 500..599 -> SERVER_ERROR
                else -> UNKNOWN
            }
    }

    /**
     * Является ли код ошибкой
     */
    val isError: Boolean
        get() = this == CLIENT_ERROR || this == SERVER_ERROR

    /**
     * Является ли код успешным
     */
    val isSuccess: Boolean
        get() = this == SUCCESS
}
