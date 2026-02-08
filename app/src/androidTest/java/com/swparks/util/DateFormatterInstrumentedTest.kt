package com.swparks.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Инструментальные тесты для DateFormatter
 *
 * Тестируют форматирование дат с контекстом Android для проверки локализации
 *
 * @see DateFormatter
 */
@RunWith(AndroidJUnit4::class)
class DateFormatterInstrumentedTest {

    private lateinit var context: android.content.Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun formatDate_withContext_whenYesterday_thenShowsLocalizedYesterday() {
        // Given
        val yesterday = LocalDate.now().minusDays(1)
        val dateString = "${yesterday.year}-${String.format("%02d", yesterday.monthValue)}-${String.format("%02d", yesterday.dayOfMonth)}T12:30:00+00:00"

        // When
        val result = DateFormatter.formatDate(context, dateString)

        // Then - Должен начинаться с локализованной строки "Вчера,"
        val expectedPrefix = context.getString(com.swparks.R.string.yesterday)
        assertTrue(result.startsWith("$expectedPrefix, "))
        assertTrue(result.contains(":")) // Содержит время
    }

    @Test
    fun formatDate_withContext_whenToday_thenShowsTimeOnly() {
        // Given
        val today = LocalDate.now()
        val dateString = "${today.year}-${String.format("%02d", today.monthValue)}-${String.format("%02d", today.dayOfMonth)}T10:30:00+00:00"

        // When
        val result = DateFormatter.formatDate(context, dateString)

        // Then - Должен показывать только время
        val expectedTime = "10:30"
        assertEquals(expectedTime, result)
    }

    @Test
    fun formatDate_withContext_whenDifferentYear_thenShowsFullDate() {
        // Given
        val dateString = "1992-08-12T10:30:00+00:00"

        // When
        val result = DateFormatter.formatDate(context, dateString)

        // Then - Должна показываться полная дата
        val expectedDate = "12 авг. 1992"
        assertEquals(expectedDate, result)
    }

    @Test
    fun formatDate_withContext_whenThisYear_thenShowsDayMonthWithTime() {
        // Given
        val currentYear = LocalDate.now().year
        val dateString = "${currentYear}-06-15T14:45:00+00:00"

        // When
        val result = DateFormatter.formatDate(context, dateString)

        // Then - Должна показываться дата и время
        assertTrue(result.startsWith("15 июн."))
        assertTrue(result.contains(":")) // Содержит время
    }

    @Test
    fun formatDate_withContext_whenEmptyString_thenReturnsEmpty() {
        // Given
        val dateString = ""

        // When
        val result = DateFormatter.formatDate(context, dateString)

        // Then
        assertEquals("", result)
    }

    @Test
    fun formatDate_withContext_whenNullString_thenReturnsEmpty() {
        // Given
        val dateString: String? = null

        // When
        val result = DateFormatter.formatDate(context, dateString)

        // Then
        assertEquals("", result)
    }

    @Test
    fun formatDate_withContext_whenInvalidString_thenReturnsEmpty() {
        // Given
        val dateString = "invalid-date-string"

        // When
        val result = DateFormatter.formatDate(context, dateString)

        // Then
        assertEquals("", result)
    }
}
