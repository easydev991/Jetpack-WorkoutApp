package com.swparks.model

import com.swparks.data.model.JournalEntryResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JournalEntryResponseTest {
    // Вспомогательные методы для создания тестовых данных
    @Suppress("LongParameterList")
    private fun createTestJournalEntry(
        id: Long = 1L,
        journalId: Int? = null,
        authorId: Int? = null,
        name: String? = null,
        message: String? = null,
        createDate: String? = null,
        modifyDate: String? = null,
        image: String? = null
    ) = JournalEntryResponse(
        id = id,
        journalId = journalId,
        authorId = authorId,
        name = name,
        message = message,
        createDate = createDate,
        modifyDate = modifyDate,
        image = image
    )

    @Test
    fun id_whenValueIs123_thenReturns123() {
        // Given
        val entry = createTestJournalEntry(id = 123L)

        // When & Then
        assertEquals(123L, entry.id)
    }

    @Test
    fun journalId_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val entry = createTestJournalEntry(journalId = 456)

        // When & Then
        assertEquals(456, entry.journalId)
    }

    @Test
    fun journalId_whenValueIsNull_thenReturnsNull() {
        // Given
        val entry = createTestJournalEntry(journalId = null)

        // When & Then
        assertNull(entry.journalId)
    }

    @Test
    fun authorId_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val entry = createTestJournalEntry(authorId = 789)

        // When & Then
        assertEquals(789, entry.authorId)
    }

    @Test
    fun authorId_whenValueIsNull_thenReturnsNull() {
        // Given
        val entry = createTestJournalEntry(authorId = null)

        // When & Then
        assertNull(entry.authorId)
    }

    @Test
    fun name_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val entry = createTestJournalEntry(name = "Test Entry")

        // When & Then
        assertEquals("Test Entry", entry.name)
    }

    @Test
    fun name_whenValueIsNull_thenReturnsNull() {
        // Given
        val entry = createTestJournalEntry(name = null)

        // When & Then
        assertNull(entry.name)
    }

    @Test
    fun message_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val entry = createTestJournalEntry(message = "Test message content")

        // When & Then
        assertEquals("Test message content", entry.message)
    }

    @Test
    fun message_whenValueIsNull_thenReturnsNull() {
        // Given
        val entry = createTestJournalEntry(message = null)

        // When & Then
        assertNull(entry.message)
    }

    @Test
    fun createDate_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val entry = createTestJournalEntry(createDate = "2024-01-01")

        // When & Then
        assertEquals("2024-01-01", entry.createDate)
    }

    @Test
    fun createDate_whenValueIsNull_thenReturnsNull() {
        // Given
        val entry = createTestJournalEntry(createDate = null)

        // When & Then
        assertNull(entry.createDate)
    }

    @Test
    fun modifyDate_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val entry = createTestJournalEntry(modifyDate = "2024-01-02")

        // When & Then
        assertEquals("2024-01-02", entry.modifyDate)
    }

    @Test
    fun modifyDate_whenValueIsNull_thenReturnsNull() {
        // Given
        val entry = createTestJournalEntry(modifyDate = null)

        // When & Then
        assertNull(entry.modifyDate)
    }

    @Test
    fun image_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val entry = createTestJournalEntry(image = "https://example.com/image.jpg")

        // When & Then
        assertEquals("https://example.com/image.jpg", entry.image)
    }

    @Test
    fun image_whenValueIsNull_thenReturnsNull() {
        // Given
        val entry = createTestJournalEntry(image = null)

        // When & Then
        assertNull(entry.image)
    }

    @Test
    fun journalEntry_whenAllFieldsArePresent_thenCreatesCorrectly() {
        // Given
        val entry = createTestJournalEntry(
            id = 1L,
            journalId = 2,
            authorId = 3,
            name = "Test Entry",
            message = "Test message",
            createDate = "2024-01-01",
            modifyDate = "2024-01-02",
            image = "https://example.com/image.jpg"
        )

        // When & Then
        assertEquals(1L, entry.id)
        assertEquals(2, entry.journalId)
        assertEquals(3, entry.authorId)
        assertEquals("Test Entry", entry.name)
        assertEquals("Test message", entry.message)
        assertEquals("2024-01-01", entry.createDate)
        assertEquals("2024-01-02", entry.modifyDate)
        assertEquals("https://example.com/image.jpg", entry.image)
    }
}
