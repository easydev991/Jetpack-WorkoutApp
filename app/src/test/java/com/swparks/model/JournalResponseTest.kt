package com.swparks.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JournalResponseTest {
    // Вспомогательные методы для создания тестовых данных
    @Suppress("LongParameterList")
    private fun createTestJournal(
        id: Long = 1L,
        title: String? = null,
        lastMessageImage: String? = null,
        createDate: String? = null,
        modifyDate: String? = null,
        lastMessageDate: String? = null,
        lastMessageText: String? = null,
        count: Int? = null,
        ownerId: Int? = null,
        viewAccess: Int? = null,
        commentAccess: Int? = null
    ) = JournalResponse(
        id = id,
        title = title,
        lastMessageImage = lastMessageImage,
        createDate = createDate,
        modifyDate = modifyDate,
        lastMessageDate = lastMessageDate,
        lastMessageText = lastMessageText,
        count = count,
        ownerId = ownerId,
        viewAccess = viewAccess,
        commentAccess = commentAccess
    )

    @Test
    fun id_whenValueIs123_thenReturns123() {
        // Given
        val journal = createTestJournal(id = 123L)

        // When & Then
        assertEquals(123L, journal.id)
    }

    @Test
    fun title_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val journal = createTestJournal(title = "Test Journal")

        // When & Then
        assertEquals("Test Journal", journal.title)
    }

    @Test
    fun title_whenValueIsNull_thenReturnsNull() {
        // Given
        val journal = createTestJournal(title = null)

        // When & Then
        assertNull(journal.title)
    }

    @Test
    fun journalAccessOption_whenViewAccessIs0_thenReturnsAll() {
        // Given
        val journal = createTestJournal(viewAccess = 0)

        // When & Then
        assertEquals(JournalAccess.ALL, journal.journalAccessOption)
    }

    @Test
    fun journalAccessOption_whenViewAccessIs1_thenReturnsFriends() {
        // Given
        val journal = createTestJournal(viewAccess = 1)

        // When & Then
        assertEquals(JournalAccess.FRIENDS, journal.journalAccessOption)
    }

    @Test
    fun journalAccessOption_whenViewAccessIs2_thenReturnsNobody() {
        // Given
        val journal = createTestJournal(viewAccess = 2)

        // When & Then
        assertEquals(JournalAccess.NOBODY, journal.journalAccessOption)
    }

    @Test
    fun journalAccessOption_whenViewAccessIsNull_thenReturnsNull() {
        // Given
        val journal = createTestJournal(viewAccess = null)

        // When & Then
        assertNull(journal.journalAccessOption)
    }

    @Test
    fun commentAccessOption_whenCommentAccessIs0_thenReturnsAll() {
        // Given
        val journal = createTestJournal(commentAccess = 0)

        // When & Then
        assertEquals(JournalAccess.ALL, journal.commentAccessOption)
    }

    @Test
    fun commentAccessOption_whenCommentAccessIs1_thenReturnsFriends() {
        // Given
        val journal = createTestJournal(commentAccess = 1)

        // When & Then
        assertEquals(JournalAccess.FRIENDS, journal.commentAccessOption)
    }

    @Test
    fun commentAccessOption_whenCommentAccessIs2_thenReturnsNobody() {
        // Given
        val journal = createTestJournal(commentAccess = 2)

        // When & Then
        assertEquals(JournalAccess.NOBODY, journal.commentAccessOption)
    }

    @Test
    fun commentAccessOption_whenCommentAccessIsNull_thenReturnsNull() {
        // Given
        val journal = createTestJournal(commentAccess = null)

        // When & Then
        assertNull(journal.commentAccessOption)
    }

    @Test
    fun lastMessageImage_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val journal = createTestJournal(lastMessageImage = "https://example.com/image.jpg")

        // When & Then
        assertEquals("https://example.com/image.jpg", journal.lastMessageImage)
    }

    @Test
    fun lastMessageImage_whenValueIsNull_thenReturnsNull() {
        // Given
        val journal = createTestJournal(lastMessageImage = null)

        // When & Then
        assertNull(journal.lastMessageImage)
    }

    @Test
    fun count_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val journal = createTestJournal(count = 5)

        // When & Then
        assertEquals(5, journal.count)
    }

    @Test
    fun count_whenValueIsNull_thenReturnsNull() {
        // Given
        val journal = createTestJournal(count = null)

        // When & Then
        assertNull(journal.count)
    }

    @Test
    fun ownerId_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val journal = createTestJournal(ownerId = 123)

        // When & Then
        assertEquals(123, journal.ownerId)
    }

    @Test
    fun ownerId_whenValueIsNull_thenReturnsNull() {
        // Given
        val journal = createTestJournal(ownerId = null)

        // When & Then
        assertNull(journal.ownerId)
    }

    @Test
    fun journal_whenAllFieldsArePresent_thenCreatesCorrectly() {
        // Given
        val journal = createTestJournal(
            id = 1L,
            title = "My Journal",
            lastMessageImage = "https://example.com/image.jpg",
            createDate = "2024-01-01",
            modifyDate = "2024-01-02",
            lastMessageDate = "2024-01-03",
            lastMessageText = "Last message",
            count = 10,
            ownerId = 2,
            viewAccess = 1,
            commentAccess = 1
        )

        // When & Then
        assertEquals(1L, journal.id)
        assertEquals("My Journal", journal.title)
        assertEquals("https://example.com/image.jpg", journal.lastMessageImage)
        assertEquals("2024-01-01", journal.createDate)
        assertEquals("2024-01-02", journal.modifyDate)
        assertEquals("2024-01-03", journal.lastMessageDate)
        assertEquals("Last message", journal.lastMessageText)
        assertEquals(10, journal.count)
        assertEquals(2, journal.ownerId)
        assertEquals(JournalAccess.FRIENDS, journal.journalAccessOption)
        assertEquals(JournalAccess.FRIENDS, journal.commentAccessOption)
    }
}
