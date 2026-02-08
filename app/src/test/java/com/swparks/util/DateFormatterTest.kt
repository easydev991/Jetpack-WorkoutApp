package com.swparks.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Тесты для DateFormatter
 *
 * Тестирует форматирование дат из формата ISO8601 в читаемый формат
 *
 * @see DateFormatter
 */
class DateFormatterTest {

    @Test
    fun formatDate_withIso8601StringAndLocale_thenReturnsFormattedDate() {
        // Given
        val dateString = "2022-10-30T09:00:00+00:00"
        val date = DateFormatter.parseIsoDate(dateString)
        val expectedDate = "30 окт. 2022"

        // When
        val result = DateFormatter.formatDateWithYesterdayString(date, Locale.forLanguageTag("ru"), showTimeInThisYear = true, null)

        // Then
        assertEquals(expectedDate, result)
    }

    @Test
    fun formatDate_withDateObjectAndLocale_thisYear_thenReturnsDayMonthWithTime() {
        // Given
        val currentYear = java.time.LocalDate.now().year
        val dateString = "${currentYear}-06-15T14:45:00+00:00"
        val date = DateFormatter.parseIsoDate(dateString)
        val expectedPrefix = "15 июн."

        // When
        val result = DateFormatter.formatDateWithYesterdayString(date, Locale.forLanguageTag("ru"), showTimeInThisYear = true, null)

        // Then
        assertTrue(result.startsWith(expectedPrefix))
        assertTrue(result.contains(":")) // Содержит время
    }

    @Test
    fun formatDate_withDateObjectAndLocale_thisYearAndShowTimeFalse_thenReturnsDayMonthOnly() {
        // Given
        val currentYear = java.time.LocalDate.now().year
        val dateString = "${currentYear}-06-15T14:45:00+00:00"
        val date = DateFormatter.parseIsoDate(dateString)
        val expectedPrefix = "15 июн."

        // When
        val result = DateFormatter.formatDateWithYesterdayString(date, Locale.forLanguageTag("ru"), showTimeInThisYear = false, null)

        // Then
        assertEquals(expectedPrefix, result)
    }

    @Test
    fun formatDate_withDateObjectAndLocale_differentYear_thenReturnsFullDate() {
        // Given
        val dateString = "1992-08-12T10:30:00+00:00"
        val date = DateFormatter.parseIsoDate(dateString)
        val expectedDate = "12 авг. 1992"

        // When
        val result = DateFormatter.formatDateWithYesterdayString(date, Locale.forLanguageTag("ru"), showTimeInThisYear = true, null)

        // Then
        assertEquals(expectedDate, result)
    }

    @Test
    fun formatDate_withDateObjectAndLocale_yesterday_thenReturnsYesterdayWithTime() {
        // Given
        val yesterday = java.time.LocalDate.now().minusDays(1)
        val dateString = "${yesterday.year}-${String.format("%02d", yesterday.monthValue)}-${String.format("%02d", yesterday.dayOfMonth)}T12:30:00+00:00"
        val date = DateFormatter.parseIsoDate(dateString)
        val customYesterdayString = "Вчера"

        // When
        val result = DateFormatter.formatDateWithYesterdayString(date, Locale.forLanguageTag("ru"), showTimeInThisYear = true, customYesterdayString)

        // Then - Должен начинаться с кастомной строки "Вчера, "
        assertTrue(result.startsWith("$customYesterdayString, "))
        assertTrue(result.contains(":")) // Содержит время
    }

    @Test
    fun formatDate_withDateObjectAndLocale_today_thenReturnsTimeOnly() {
        // Given
        val today = java.time.LocalDate.now()
        val dateString = "${today.year}-${String.format("%02d", today.monthValue)}-${String.format("%02d", today.dayOfMonth)}T10:30:00+00:00"
        val date = DateFormatter.parseIsoDate(dateString)

        // When
        val result = DateFormatter.formatDateWithYesterdayString(date, Locale.forLanguageTag("ru"), showTimeInThisYear = true, null)

        // Then - Должен показывать только время (формат HH:mm)
        // Не проверяем точное время из-за разницы часовых поясов при форматировании
        assertTrue("Результат должен быть в формате времени HH:mm, но был: $result", result.matches(Regex("\\d{2}:\\d{2}")))
    }

    @Test
    fun parseIsoDate_whenIso8601WithSeconds_thenReturnsDate() {
        // Given
        val dateString = "2023-01-21T10:05:35+00:00"

        // When
        val result = DateFormatter.parseIsoDate(dateString)

        // Then - Парсинг должен пройти успешно без исключения
        assertTrue(result.time > 0)
    }

    @Test
    fun parseIsoDate_whenIso8601WithFractionalSeconds_thenReturnsDate() {
        // Given
        val dateString = "2023-01-21T10:05:35.123+00:00"

        // When
        val result = DateFormatter.parseIsoDate(dateString)

        // Then - Парсинг должен пройти успешно без исключения
        assertTrue(result.time > 0)
    }

    @Test
    fun parseIsoDate_whenIso8601WithZ_thenReturnsDate() {
        // Given
        val dateString = "2023-01-21T10:05:35Z"

        // When
        val result = DateFormatter.parseIsoDate(dateString)

        // Then - Парсинг должен пройти успешно без исключения
        assertTrue(result.time > 0)
    }

    @Test
    fun parseIsoDate_whenIsoShortDate_thenReturnsDate() {
        // Given
        val dateString = "1992-08-12"

        // When
        val result = DateFormatter.parseIsoDate(dateString)

        // Then - Парсинг должен пройти успешно без исключения
        assertTrue(result.time > 0)
    }

    @Test
    fun parseIsoDate_whenServerDateTimeWithoutTimezone_thenReturnsDate() {
        // Given
        val dateString = "2023-01-21T10:05:35"

        // When
        val result = DateFormatter.parseIsoDate(dateString)

        // Then - Парсинг должен пройти успешно без исключения
        assertTrue(result.time > 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseIsoDate_whenInvalidString_thenThrowsException() {
        // Given
        val dateString = "invalid-date"

        // When
        DateFormatter.parseIsoDate(dateString)

        // Then - Ожидается IllegalArgumentException
    }

    @Test
    fun formatDate_withEnglishLocale_thenUsesEnglishMonthNames() {
        // Given
        val dateString = "2023-11-21T10:30:00+00:00"
        val date = DateFormatter.parseIsoDate(dateString)
        val expectedPrefix = "21 Nov"

        // When
        val result = DateFormatter.formatDateWithYesterdayString(date, Locale.forLanguageTag("en"), showTimeInThisYear = true, null)

        // Then
        assertTrue(result.startsWith(expectedPrefix))
    }

    @Test
    fun formatDate_withRussianLocale_thenUsesRussianMonthNames() {
        // Given
        val dateString = "2023-11-21T10:30:00+00:00"
        val date = DateFormatter.parseIsoDate(dateString)
        val expectedPrefix = "21 нояб."

        // When
        val result = DateFormatter.formatDateWithYesterdayString(date, Locale.forLanguageTag("ru"), showTimeInThisYear = true, null)

        // Then
        assertTrue(result.startsWith(expectedPrefix))
    }
}
