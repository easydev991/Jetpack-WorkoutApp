package com.swparks.util

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.swparks.util.DateFormatter.parseIsoDate
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Date

/**
 * Утилита для форматирования дат
 *
 * Преобразует даты из формата ISO8601 в локализованный формат для отображения пользователю
 *
 * @see DateFormatterTests для примеров форматирования
 */
object DateFormatter {
    private const val DAY_MONTH_YEAR = "d MMM yyyy"
    private const val DAY_MONTH_MEDIUM_TIME = "d MMM, HH:mm"
    private const val MEDIUM_TIME = "HH:mm"
    private const val DAY_MONTH = "d MMM"

    /**
     * Форматирует дату из ISO8601 строки в читаемый формат
     *
     * Правила форматирования:
     * - Если дата сегодня: формат "HH:mm" (например, "10:30")
     * - Если дата вчера: формат "Вчера, HH:mm" (например, "Вчера, 10:30")
     * - Если дата в этом году: формат "d MMM, HH:mm" (например, "21 нояб, 10:30")
     * - Если дата в другом году: формат "d MMM yyyy" (например, "21 нояб 2023")
     *
     * @param context Контекст приложения для получения локали и локализованных строк
     * @param dateString Дата в формате ISO8601
     * @param showTimeInThisYear Показывать время для дат в этом году (по умолчанию - true)
     * @return Отформатированная строка даты или пустая строка при ошибке
     */
    @Suppress("TooGenericExceptionCaught")
    fun formatDate(
        context: Context,
        dateString: String?,
        showTimeInThisYear: Boolean = true
    ): String {
        if (dateString.isNullOrEmpty()) {
            return ""
        }

        return try {
            val date = parseIsoDate(dateString)
            val localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val locale =
                context.resources.configuration.locales
                    .get(0)
            val yesterdayString = context.getString(com.swparks.R.string.yesterday)

            when {
                localDate.isToday() -> {
                    val formatter = DateTimeFormatter.ofPattern(MEDIUM_TIME, locale)
                    date.toInstant().atZone(ZoneId.systemDefault()).format(formatter)
                }

                localDate.isYesterday() -> {
                    val formatter = DateTimeFormatter.ofPattern(MEDIUM_TIME, locale)
                    "$yesterdayString, ${date.toInstant().atZone(ZoneId.systemDefault()).format(formatter)}"
                }

                localDate.isThisYear() -> {
                    if (showTimeInThisYear) {
                        val formatter = DateTimeFormatter.ofPattern(DAY_MONTH_MEDIUM_TIME, locale)
                        date.toInstant().atZone(ZoneId.systemDefault()).format(formatter)
                    } else {
                        val formatter = DateTimeFormatter.ofPattern(DAY_MONTH, locale)
                        date.toInstant().atZone(ZoneId.systemDefault()).format(formatter)
                    }
                }

                else -> {
                    val formatter = DateTimeFormatter.ofPattern(DAY_MONTH_YEAR, locale)
                    date.toInstant().atZone(ZoneId.systemDefault()).format(formatter)
                }
            }
        } catch (e: Exception) {
            Log.w("DateFormatter", "Не удалось отформатировать дату: ${e.message}")
            ""
        }
    }

    /**
     * Парсит дату из формата ISO8601
     *
     * Поддерживаемые форматы:
     * - 2024-01-15T10:30:00Z
     * - 2024-01-15T10:30:00.123Z
     * - 2024-01-15T10:30:00+00:00
     * - 2024-01-15T10:30:00-05:00
     * - 2024-01-15T10:30:00.123+03:00
     * - 2024-01-15T10:30:00
     * - 2024-01-15
     *
     * @param dateString Дата в формате ISO8601
     * @return Объект Date
     * @throws IllegalArgumentException Если дату невозможно распарсить
     */
    @VisibleForTesting
    internal fun parseIsoDate(dateString: String): Date {
        val instant =
            try {
                Instant.parse(dateString)
            } catch (_: DateTimeParseException) {
                try {
                    LocalDateTime.parse(dateString).toInstant(ZoneOffset.UTC)
                } catch (_: DateTimeParseException) {
                    try {
                        LocalDate.parse(dateString).atStartOfDay(ZoneOffset.UTC).toInstant()
                    } catch (_: DateTimeParseException) {
                        throw IllegalArgumentException("Не удалось распарсить дату: $dateString")
                    }
                }
            }

        return Date.from(instant)
    }

    /**
     * Парсит дату из формата ISO8601 и возвращает время в миллисекундах
     *
     * Поддерживаемые форматы такие же, как в [parseIsoDate]
     *
     * @param dateString Дата в формате ISO8601
     * @return Время в миллисекундах или null, если дату невозможно распарсить
     */
    fun parseIsoDateToMillis(dateString: String): Long? =
        try {
            parseIsoDate(dateString).time
        } catch (_: Exception) {
            null
        }
}

/**
 * Расширения для работы с датами
 */
private fun LocalDate.isToday(): Boolean = this == LocalDate.now()

private fun LocalDate.isYesterday(): Boolean = this == LocalDate.now().minusDays(1)

private fun LocalDate.isThisYear(): Boolean = this.year == LocalDate.now().year
