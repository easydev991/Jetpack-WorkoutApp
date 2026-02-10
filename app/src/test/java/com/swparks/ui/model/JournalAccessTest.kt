package com.swparks.ui.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты для extension функции [JournalAccess.canCreateEntry]
 */
class JournalAccessTest {

    private val journalOwnerId = 100L
    private val currentUserId = 200L
    private val friendId = 100L

    @Test
    fun `canCreateEntry with ALL and authorized user returns true`() {
        val result = JournalAccess.ALL.canCreateEntry(
            journalOwnerId = journalOwnerId,
            mainUserId = currentUserId,
            mainUserFriendsIds = emptyList()
        )

        assertTrue(result)
    }

    @Test
    fun `canCreateEntry with ALL and unauthorized user returns false`() {
        val result = JournalAccess.ALL.canCreateEntry(
            journalOwnerId = journalOwnerId,
            mainUserId = null,
            mainUserFriendsIds = emptyList()
        )

        assertFalse(result)
    }

    @Test
    fun `canCreateEntry with FRIENDS and friend returns true`() {
        val result = JournalAccess.FRIENDS.canCreateEntry(
            journalOwnerId = friendId,
            mainUserId = currentUserId,
            mainUserFriendsIds = listOf(friendId)
        )

        assertTrue(result)
    }

    @Test
    fun `canCreateEntry with FRIENDS and non-friend returns false`() {
        val result = JournalAccess.FRIENDS.canCreateEntry(
            journalOwnerId = journalOwnerId,
            mainUserId = currentUserId,
            mainUserFriendsIds = emptyList()
        )

        assertFalse(result)
    }

    @Test
    fun `canCreateEntry with FRIENDS and owner returns true`() {
        val result = JournalAccess.FRIENDS.canCreateEntry(
            journalOwnerId = journalOwnerId,
            mainUserId = journalOwnerId,
            mainUserFriendsIds = emptyList()
        )

        assertTrue(result)
    }

    @Test
    fun `canCreateEntry with NOBODY and owner returns true`() {
        val result = JournalAccess.NOBODY.canCreateEntry(
            journalOwnerId = journalOwnerId,
            mainUserId = journalOwnerId,
            mainUserFriendsIds = emptyList()
        )

        assertTrue(result)
    }

    @Test
    fun `canCreateEntry with NOBODY and non-owner returns false`() {
        val result = JournalAccess.NOBODY.canCreateEntry(
            journalOwnerId = journalOwnerId,
            mainUserId = currentUserId,
            mainUserFriendsIds = emptyList()
        )

        assertFalse(result)
    }
}
