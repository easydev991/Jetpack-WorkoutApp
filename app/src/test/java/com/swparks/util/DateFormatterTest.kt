package com.swparks.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты для DateFormatter
 *
 * Тестирует форматирование дат из формата ISO8601 в читаемый формат
 *
 * @see DateFormatter
 */
class DateFormatterTest {

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
    fun parseIsoDateToMillis_whenIso8601WithSeconds_thenReturnsMillis() {
        // Given
        val dateString = "2023-01-21T10:05:35+00:00"

        // When
        val result = DateFormatter.parseIsoDateToMillis(dateString)

        // Then
        assertTrue(result != null && result > 0)
    }

    @Test
    fun parseIsoDateToMillis_whenIso8601WithFractionalSeconds_thenReturnsMillis() {
        // Given
        val dateString = "2023-01-21T10:05:35.123+00:00"

        // When
        val result = DateFormatter.parseIsoDateToMillis(dateString)

        // Then
        assertTrue(result != null && result > 0)
    }

    @Test
    fun parseIsoDateToMillis_whenIso8601WithZ_thenReturnsMillis() {
        // Given
        val dateString = "2023-01-21T10:05:35Z"

        // When
        val result = DateFormatter.parseIsoDateToMillis(dateString)

        // Then
        assertTrue(result != null && result > 0)
    }

    @Test
    fun parseIsoDateToMillis_whenIsoShortDate_thenReturnsMillis() {
        // Given
        val dateString = "1992-08-12"

        // When
        val result = DateFormatter.parseIsoDateToMillis(dateString)

        // Then
        assertTrue(result != null && result > 0)
    }

    @Test
    fun parseIsoDateToMillis_whenServerDateTimeWithoutTimezone_thenReturnsMillis() {
        // Given
        val dateString = "2023-01-21T10:05:35"

        // When
        val result = DateFormatter.parseIsoDateToMillis(dateString)

        // Then
        assertTrue(result != null && result > 0)
    }

    @Test
    fun parseIsoDateToMillis_whenInvalidString_thenReturnsNull() {
        // Given
        val dateString = "invalid-date"

        // When
        val result = DateFormatter.parseIsoDateToMillis(dateString)

        // Then
        assertNull(result)
    }

    @Test
    fun parseIsoDateToMillis_whenEmptyString_thenReturnsNull() {
        // Given
        val dateString = ""

        // When
        val result = DateFormatter.parseIsoDateToMillis(dateString)

        // Then
        assertNull(result)
    }

    @Test
    fun parseIsoDateToMillis_whenConsistentResults_thenReturnSameValue() {
        // Given
        val dateString = "2024-03-09T15:30:00Z"

        // When
        val result1 = DateFormatter.parseIsoDateToMillis(dateString)
        val result2 = DateFormatter.parseIsoDate(dateString).time

        // Then
        assertEquals(result1, result2)
    }

    @Test
    fun parseIsoDate_whenUtcTimezone_thenConvertsToLocalTime() {
        // Given - дата в UTC: 2026-03-15T20:17:09+00:00
        val dateString = "2026-03-15T20:17:09+00:00"

        // When
        val date = DateFormatter.parseIsoDate(dateString)

        // Then - проверяем что timestamp соответствует UTC времени
        // 2026-03-15T20:17:09 UTC = 1773605829000 ms
        val expectedMillis = 1773605829000L
        assertEquals(expectedMillis, date.time)
    }

    @Test
    fun parseIsoDate_whenUtcPlusOffset_thenCorrectlyParses() {
        // Given - дата с UTC+offset: 2026-03-15T23:17:09+03:00
        val dateString = "2026-03-15T23:17:09+03:00"

        // When
        val date = DateFormatter.parseIsoDate(dateString)

        // Then - это то же самое время что и 2026-03-15T20:17:09+00:00
        // 2026-03-15T23:17:09+03:00 = 2026-03-15T20:17:09 UTC = 1773605829000 ms
        val expectedMillis = 1773605829000L
        assertEquals(expectedMillis, date.time)
    }
}
