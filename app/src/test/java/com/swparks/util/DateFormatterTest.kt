package com.swparks.util

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
}
