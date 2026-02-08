package com.swparks.model

import com.swparks.data.model.MessageResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MessageResponseTest {
    // Вспомогательные методы для создания тестовых данных
    private fun createTestMessage(
        id: Long = 1L,
        userId: Int? = null,
        message: String? = null,
        name: String? = null,
        created: String? = null
    ) = MessageResponse(
        id = id,
        userId = userId,
        message = message,
        name = name,
        created = created
    )

    @Test
    fun id_whenValueIs123_thenReturns123() {
        // Given
        val message = createTestMessage(id = 123L)

        // When & Then
        assertEquals(123L, message.id)
    }

    @Test
    fun userId_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val message = createTestMessage(userId = 456)

        // When & Then
        assertEquals(456, message.userId)
    }

    @Test
    fun userId_whenValueIsNull_thenReturnsNull() {
        // Given
        val message = createTestMessage(userId = null)

        // When & Then
        assertNull(message.userId)
    }

    @Test
    fun message_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val message = createTestMessage(message = "Test message")

        // When & Then
        assertEquals("Test message", message.message)
    }

    @Test
    fun message_whenValueIsNull_thenReturnsNull() {
        // Given
        val message = createTestMessage(message = null)

        // When & Then
        assertNull(message.message)
    }

    @Test
    fun name_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val message = createTestMessage(name = "Test Name")

        // When & Then
        assertEquals("Test Name", message.name)
    }

    @Test
    fun name_whenValueIsNull_thenReturnsNull() {
        // Given
        val message = createTestMessage(name = null)

        // When & Then
        assertNull(message.name)
    }

    @Test
    fun created_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val message = createTestMessage(created = "2024-01-01")

        // When & Then
        assertEquals("2024-01-01", message.created)
    }

    @Test
    fun created_whenValueIsNull_thenReturnsNull() {
        // Given
        val message = createTestMessage(created = null)

        // When & Then
        assertNull(message.created)
    }

    @Test
    fun message_whenAllFieldsArePresent_thenCreatesCorrectly() {
        // Given
        val message = createTestMessage(
            id = 1L,
            userId = 2,
            message = "Hello world",
            name = "User",
            created = "2024-01-01"
        )

        // When & Then
        assertEquals(1L, message.id)
        assertEquals(2, message.userId)
        assertEquals("Hello world", message.message)
        assertEquals("User", message.name)
        assertEquals("2024-01-01", message.created)
    }
}
