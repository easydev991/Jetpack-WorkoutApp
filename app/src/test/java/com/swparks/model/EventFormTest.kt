package com.swparks.model

import com.swparks.ui.model.EventForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventFormTest {
    // Вспомогательные методы для создания тестовых данных
    private fun createTestForm(
        title: String = "Test Event",
        description: String = "Test description",
        date: String = "2024-01-01",
        parkId: Long = 1L,
        parkName: String = "Test Park"
    ) = EventForm(
        title = title,
        description = description,
        date = date,
        parkId = parkId,
        parkName = parkName
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
        val form =
            createTestForm(
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
        val form =
            createTestForm(
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

    // ============= isReadyToCreate tests =============

    @Test
    fun isReadyToCreate_whenAllFieldsValid_thenReturnsTrue() {
        val form = createTestForm()

        assertTrue(form.isReadyToCreate)
    }

    @Test
    fun isReadyToCreate_whenTitleIsBlank_thenReturnsFalse() {
        val form = createTestForm(title = "")

        assertFalse(form.isReadyToCreate)
    }

    @Test
    fun isReadyToCreate_whenTitleIsWhitespace_thenReturnsFalse() {
        val form = createTestForm(title = "   ")

        assertFalse(form.isReadyToCreate)
    }

    @Test
    fun isReadyToCreate_whenParkIdIsZero_thenReturnsFalse() {
        val form = createTestForm(parkId = 0L)

        assertFalse(form.isReadyToCreate)
    }

    @Test
    fun isReadyToCreate_whenDateIsBlank_thenReturnsFalse() {
        val form = createTestForm(date = "")

        assertFalse(form.isReadyToCreate)
    }

    @Test
    fun isReadyToCreate_whenDescriptionIsBlank_thenReturnsTrue() {
        val form = createTestForm(description = "")

        assertTrue(form.isReadyToCreate)
    }

    // ============= isReadyToUpdate tests =============

    @Test
    fun isReadyToUpdate_whenNoChanges_thenReturnsFalse() {
        val form = createTestForm()
        val oldForm = createTestForm()

        assertFalse(form.isReadyToUpdate(oldForm))
    }

    @Test
    fun isReadyToUpdate_whenTitleChanged_thenReturnsTrue() {
        val form = createTestForm(title = "New Title")
        val oldForm = createTestForm(title = "Old Title")

        assertTrue(form.isReadyToUpdate(oldForm))
    }

    @Test
    fun isReadyToUpdate_whenDescriptionChanged_thenReturnsTrue() {
        val form = createTestForm(description = "New Description")
        val oldForm = createTestForm(description = "Old Description")

        assertTrue(form.isReadyToUpdate(oldForm))
    }

    @Test
    fun isReadyToUpdate_whenDateChanged_thenReturnsTrue() {
        val form = createTestForm(date = "2024-12-31")
        val oldForm = createTestForm(date = "2024-01-01")

        assertTrue(form.isReadyToUpdate(oldForm))
    }

    @Test
    fun isReadyToUpdate_whenParkIdChanged_thenReturnsTrue() {
        val form = createTestForm(parkId = 2L)
        val oldForm = createTestForm(parkId = 1L)

        assertTrue(form.isReadyToUpdate(oldForm))
    }

    @Test
    fun isReadyToUpdate_whenParkNameChanged_thenReturnsFalse() {
        val form = createTestForm(parkName = "New Park")
        val oldForm = createTestForm(parkName = "Old Park")

        assertFalse(form.isReadyToUpdate(oldForm))
    }

    @Test
    fun isReadyToUpdate_whenFormIsInvalid_thenReturnsFalse() {
        val form = createTestForm(title = "")
        val oldForm = createTestForm(title = "Old Title")

        assertFalse(form.isReadyToUpdate(oldForm))
    }

    // ============= Default values tests =============

    @Test
    fun form_whenCreatedWithDefaults_thenHasEmptyValues() {
        val form = EventForm()

        assertEquals("", form.title)
        assertEquals("", form.description)
        assertEquals("", form.date)
        assertEquals(0L, form.parkId)
        assertEquals("", form.parkName)
    }

    @Test
    fun form_whenCreatedWithDefaults_thenIsNotReadyToCreate() {
        val form = EventForm()

        assertFalse(form.isReadyToCreate)
    }
}
