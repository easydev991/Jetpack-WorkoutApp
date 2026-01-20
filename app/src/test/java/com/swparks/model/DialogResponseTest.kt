package com.swparks.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DialogResponseTest {
    // Вспомогательные методы для создания тестовых данных
    @Suppress("LongParameterList")
    private fun createTestDialog(
        id: Long = 1L,
        anotherUserId: Int? = null,
        name: String? = null,
        image: String? = null,
        lastMessageText: String? = null,
        lastMessageDate: String? = null,
        count: Int? = null
    ) = DialogResponse(
        id = id,
        anotherUserId = anotherUserId,
        name = name,
        image = image,
        lastMessageText = lastMessageText,
        lastMessageDate = lastMessageDate,
        count = count
    )

    @Test
    fun id_whenValueIs123_thenReturns123() {
        // Given
        val dialog = createTestDialog(id = 123L)

        // When & Then
        assertEquals(123L, dialog.id)
    }

    @Test
    fun anotherUserId_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val dialog = createTestDialog(anotherUserId = 456)

        // When & Then
        assertEquals(456, dialog.anotherUserId)
    }

    @Test
    fun anotherUserId_whenValueIsNull_thenReturnsNull() {
        // Given
        val dialog = createTestDialog(anotherUserId = null)

        // When & Then
        assertNull(dialog.anotherUserId)
    }

    @Test
    fun name_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val dialog = createTestDialog(name = "Test Name")

        // When & Then
        assertEquals("Test Name", dialog.name)
    }

    @Test
    fun name_whenValueIsNull_thenReturnsNull() {
        // Given
        val dialog = createTestDialog(name = null)

        // When & Then
        assertNull(dialog.name)
    }

    @Test
    fun image_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val dialog = createTestDialog(image = "https://example.com/image.jpg")

        // When & Then
        assertEquals("https://example.com/image.jpg", dialog.image)
    }

    @Test
    fun image_whenValueIsNull_thenReturnsNull() {
        // Given
        val dialog = createTestDialog(image = null)

        // When & Then
        assertNull(dialog.image)
    }

    @Test
    fun lastMessageText_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val dialog = createTestDialog(lastMessageText = "Last message")

        // When & Then
        assertEquals("Last message", dialog.lastMessageText)
    }

    @Test
    fun lastMessageText_whenValueIsNull_thenReturnsNull() {
        // Given
        val dialog = createTestDialog(lastMessageText = null)

        // When & Then
        assertNull(dialog.lastMessageText)
    }

    @Test
    fun lastMessageDate_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val dialog = createTestDialog(lastMessageDate = "2024-01-01")

        // When & Then
        assertEquals("2024-01-01", dialog.lastMessageDate)
    }

    @Test
    fun lastMessageDate_whenValueIsNull_thenReturnsNull() {
        // Given
        val dialog = createTestDialog(lastMessageDate = null)

        // When & Then
        assertNull(dialog.lastMessageDate)
    }

    @Test
    fun count_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val dialog = createTestDialog(count = 5)

        // When & Then
        assertEquals(5, dialog.count)
    }

    @Test
    fun count_whenValueIsNull_thenReturnsNull() {
        // Given
        val dialog = createTestDialog(count = null)

        // When & Then
        assertNull(dialog.count)
    }

    @Test
    fun dialog_whenAllFieldsArePresent_thenCreatesCorrectly() {
        // Given
        val dialog = createTestDialog(
            id = 1L,
            anotherUserId = 2,
            name = "Test Dialog",
            image = "https://example.com/image.jpg",
            lastMessageText = "Hello",
            lastMessageDate = "2024-01-01",
            count = 3
        )

        // When & Then
        assertEquals(1L, dialog.id)
        assertEquals(2, dialog.anotherUserId)
        assertEquals("Test Dialog", dialog.name)
        assertEquals("https://example.com/image.jpg", dialog.image)
        assertEquals("Hello", dialog.lastMessageText)
        assertEquals("2024-01-01", dialog.lastMessageDate)
        assertEquals(3, dialog.count)
    }
}
