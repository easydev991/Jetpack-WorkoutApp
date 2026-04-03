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
    fun canCreateEntry_with_ALL_and_authorized_user_returns_true() {
        val result =
            JournalAccess.ALL.canCreateEntry(
                journalOwnerId = journalOwnerId,
                mainUserId = currentUserId,
                mainUserFriendsIds = emptyList()
            )

        assertTrue(result)
    }

    @Test
    fun canCreateEntry_with_ALL_and_unauthorized_user_returns_false() {
        val result =
            JournalAccess.ALL.canCreateEntry(
                journalOwnerId = journalOwnerId,
                mainUserId = null,
                mainUserFriendsIds = emptyList()
            )

        assertFalse(result)
    }

    @Test
    fun canCreateEntry_with_FRIENDS_and_friend_returns_true() {
        val result =
            JournalAccess.FRIENDS.canCreateEntry(
                journalOwnerId = friendId,
                mainUserId = currentUserId,
                mainUserFriendsIds = listOf(friendId)
            )

        assertTrue(result)
    }

    @Test
    fun canCreateEntry_with_FRIENDS_and_non_friend_returns_false() {
        val result =
            JournalAccess.FRIENDS.canCreateEntry(
                journalOwnerId = journalOwnerId,
                mainUserId = currentUserId,
                mainUserFriendsIds = emptyList()
            )

        assertFalse(result)
    }

    @Test
    fun canCreateEntry_with_FRIENDS_and_owner_returns_false() {
        val result =
            JournalAccess.FRIENDS.canCreateEntry(
                journalOwnerId = journalOwnerId,
                mainUserId = journalOwnerId,
                mainUserFriendsIds = emptyList()
            )

        assertFalse(result)
    }

    @Test
    fun canCreateEntry_with_NOBODY_and_owner_returns_true() {
        val result =
            JournalAccess.NOBODY.canCreateEntry(
                journalOwnerId = journalOwnerId,
                mainUserId = journalOwnerId,
                mainUserFriendsIds = emptyList()
            )

        assertTrue(result)
    }

    @Test
    fun canCreateEntry_with_NOBODY_and_non_owner_returns_false() {
        val result =
            JournalAccess.NOBODY.canCreateEntry(
                journalOwnerId = journalOwnerId,
                mainUserId = currentUserId,
                mainUserFriendsIds = emptyList()
            )

        assertFalse(result)
    }
}
