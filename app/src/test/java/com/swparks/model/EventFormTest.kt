package com.swparks.model

import org.junit.Assert.assertEquals
import org.junit.Test

class EventFormTest {
    // Вспомогательные методы для создания тестовых данных
    private fun createTestForm(
        title: String = "Test Event",
        description: String = "Test description",
        date: String = "2024-01-01",
        parkId: Long = 1L
    ) = EventForm(
        title = title,
        description = description,
        date = date,
        parkId = parkId
    )

    @Test
    fun title_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val form = createTestForm(title = "My Event")

        // When & Then
        assertEquals("My Event", form.title)
    }

    @Test
    fun description_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val form = createTestForm(description = "Event description")

        // When & Then
        assertEquals("Event description", form.description)
    }

    @Test
    fun date_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val form = createTestForm(date = "2024-12-31")

        // When & Then
        assertEquals("2024-12-31", form.date)
    }

    @Test
    fun parkId_whenValueIs123_thenReturns123() {
        // Given
        val form = createTestForm(parkId = 123L)

        // When & Then
        assertEquals(123L, form.parkId)
    }

    @Test
    fun parkId_whenValueIs0_thenReturns0() {
        // Given
        val form = createTestForm(parkId = 0L)

        // When & Then
        assertEquals(0L, form.parkId)
    }

    @Test
    fun form_whenAllFieldsArePresent_thenCreatesCorrectly() {
        // Given
        val form = createTestForm(
            title = "Workout Competition",
            description = "Annual workout competition",
            date = "2024-06-15",
            parkId = 456L
        )

        // When & Then
        assertEquals("Workout Competition", form.title)
        assertEquals("Annual workout competition", form.description)
        assertEquals("2024-06-15", form.date)
        assertEquals(456L, form.parkId)
    }

    @Test
    fun form_whenTitleIsRussian_thenCreatesCorrectly() {
        // Given
        val form = createTestForm(
            title = "Соревнования по воркауту",
            description = "Ежегодные соревнования",
            date = "2024-07-01",
            parkId = 789L
        )

        // When & Then
        assertEquals("Соревнования по воркауту", form.title)
        assertEquals("Ежегодные соревнования", form.description)
        assertEquals("2024-07-01", form.date)
        assertEquals(789L, form.parkId)
    }
}
