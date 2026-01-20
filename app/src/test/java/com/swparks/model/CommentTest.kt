package com.swparks.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CommentTest {
    // Вспомогательные методы для создания тестовых данных
    private fun createTestUser(id: Long = 1L) = User(
        id = id,
        name = "testuser",
        image = "https://example.com/image.jpg",
        lang = "ru"
    )

    private fun createTestComment(
        id: Long = 1L,
        body: String? = null,
        date: String? = null,
        user: User? = null
    ) = Comment(
        id = id,
        body = body,
        date = date,
        user = user
    )

    @Test
    fun id_whenValueIs123_thenReturns123() {
        // Given
        val comment = createTestComment(id = 123L)

        // When & Then
        assertEquals(123L, comment.id)
    }

    @Test
    fun body_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val comment = createTestComment(body = "Test comment")

        // When & Then
        assertEquals("Test comment", comment.body)
    }

    @Test
    fun body_whenValueIsNull_thenReturnsNull() {
        // Given
        val comment = createTestComment(body = null)

        // When & Then
        assertNull(comment.body)
    }

    @Test
    fun date_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val comment = createTestComment(date = "2024-01-01")

        // When & Then
        assertEquals("2024-01-01", comment.date)
    }

    @Test
    fun date_whenValueIsNull_thenReturnsNull() {
        // Given
        val comment = createTestComment(date = null)

        // When & Then
        assertNull(comment.date)
    }

    @Test
    fun user_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val user = createTestUser(id = 123L)
        val comment = createTestComment(user = user)

        // When & Then
        assertEquals(123L, comment.user?.id)
    }

    @Test
    fun user_whenValueIsNull_thenReturnsNull() {
        // Given
        val comment = createTestComment(user = null)

        // When & Then
        assertNull(comment.user)
    }

    @Test
    fun comment_whenAllFieldsArePresent_thenCreatesCorrectly() {
        // Given
        val user = createTestUser(id = 2L)
        val comment = createTestComment(
            id = 1L,
            body = "Test comment body",
            date = "2024-01-01",
            user = user
        )

        // When & Then
        assertEquals(1L, comment.id)
        assertEquals("Test comment body", comment.body)
        assertEquals("2024-01-01", comment.date)
        assertEquals(2L, comment.user?.id)
    }
}
