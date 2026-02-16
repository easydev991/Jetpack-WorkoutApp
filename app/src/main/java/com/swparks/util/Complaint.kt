package com.swparks.util

/**
 * Модель для обратной связи с жалобой на контент
 *
 * Аналог iOS Complaint.swift
 */
sealed class Complaint {
    abstract val subject: String
    abstract val body: String

    /**
     * Жалоба на фото к мероприятию
     */
    data class EventPhoto(val eventTitle: String) : Complaint() {
        override val subject: String = "$APP_NAME: Жалоба на фото к мероприятию"
        override val body: String = "Наименование мероприятия: $eventTitle"
    }

    /**
     * Жалоба на комментарий к мероприятию
     */
    data class EventComment(
        val eventTitle: String,
        val author: String,
        val commentText: String
    ) : Complaint() {
        override val subject: String = "$APP_NAME: Жалоба на комментарий к мероприятию"
        override val body: String = """
            |- Наименование мероприятия: $eventTitle
            |- Автор комментария: $author
            |- Текст комментария: $commentText
        """.trimMargin()
    }

    /**
     * Жалоба на фото к площадке
     */
    data class ParkPhoto(val parkTitle: String) : Complaint() {
        override val subject: String = "$APP_NAME: Жалоба на фото к площадке"
        override val body: String = "Наименование площадки: $parkTitle"
    }

    /**
     * Жалоба на комментарий к площадке
     */
    data class ParkComment(
        val parkTitle: String,
        val author: String,
        val commentText: String
    ) : Complaint() {
        override val subject: String = "$APP_NAME: Жалоба на комментарий к площадке"
        override val body: String = """
            |- Наименование площадки: $parkTitle
            |- Автор комментария: $author
            |- Текст комментария: $commentText
        """.trimMargin()
    }

    /**
     * Жалоба на запись в дневнике
     */
    data class JournalEntry(
        val author: String,
        val entryText: String
    ) : Complaint() {
        override val subject: String = "$APP_NAME: Жалоба на запись в дневнике"
        override val body: String = """
            |Автор записи: $author
            |Текст записи: $entryText
        """.trimMargin()
    }

    companion object {
        private const val APP_NAME = "Jetpack WorkoutApp"
    }
}
