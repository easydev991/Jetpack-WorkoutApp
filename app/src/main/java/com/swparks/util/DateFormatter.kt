package com.swparks.util

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

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
            val localDate = date.toLocalDate()
            val locale = context.resources.configuration.locales.get(0)
            val yesterdayString = context.getString(com.swparks.R.string.yesterday)

            when {
                localDate.isToday() -> {
                    // Сегодня: показываем только время
                    val timeFormatter = SimpleDateFormat(MEDIUM_TIME, locale)
                    timeFormatter.format(date)
                }

                localDate.isYesterday() -> {
                    // Вчера: показываем локализованную строку и время
                    val timeFormatter = SimpleDateFormat(MEDIUM_TIME, locale)
                    "$yesterdayString, ${timeFormatter.format(date)}"
                }

                localDate.isThisYear() -> {
                    // В этом году: показываем дату и (опционально) время
                    if (showTimeInThisYear) {
                        val dateTimeFormatter = SimpleDateFormat(DAY_MONTH_MEDIUM_TIME, locale)
                        dateTimeFormatter.format(date)
                    } else {
                        val dateOnlyFormatter = SimpleDateFormat(DAY_MONTH, locale)
                        dateOnlyFormatter.format(date)
                    }
                }

                else -> {
                    // Другой год: показываем полную дату
                    val formatter = SimpleDateFormat(DAY_MONTH_YEAR, locale)
                    formatter.format(date)
                }
            }
        } catch (e: Exception) {
            Log.w("DateFormatter", "Failed to format date: ${e.message}")
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
        // Пробуем разные форматы ISO8601
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.forLanguageTag("en-US")).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.forLanguageTag("en-US")).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.forLanguageTag("en-US")).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.forLanguageTag("en-US")).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.forLanguageTag("en-US")).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            },
            SimpleDateFormat("yyyy-MM-dd", Locale.forLanguageTag("en-US")).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
        )

        for (format in formats) {
            try {
                format.isLenient = false
                val parsedDate = format.parse(dateString)
                if (parsedDate != null) {
                    return parsedDate
                }
            } catch (_: Exception) {
                // Игнорируем исключение и пробуем следующий формат
            }
        }

        throw IllegalArgumentException("Не удалось распарсить дату: $dateString")
    }
}

/**
 * Расширения для работы с датами
 */
private fun Date.toLocalDate(): LocalDate {
    return this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}

private fun LocalDate.isToday(): Boolean {
    return this == LocalDate.now()
}

private fun LocalDate.isYesterday(): Boolean {
    return this == LocalDate.now().minusDays(1)
}

private fun LocalDate.isThisYear(): Boolean {
    return this.year == LocalDate.now().year
}
