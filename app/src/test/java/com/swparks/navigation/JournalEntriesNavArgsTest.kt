package com.swparks.navigation

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class JournalEntriesNavArgsTest {
    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setUp() {
        savedStateHandle = SavedStateHandle()
    }

    @Test
    fun consumeJournalEntriesArgs_whenValidData_thenReturnsParsedArgs() {
        val bundle =
            Bundle().apply {
                putString("journalId", "10")
                putString("userId", "42")
                putString("journalTitle", Uri.encode("Мой дневник"))
                putString("viewAccess", "ALL")
                putString("commentAccess", "FRIENDS")
                putString("source", "events")
            }
        val navBackStackEntry = createNavBackStackEntry(bundle, savedStateHandle)

        val args = navBackStackEntry.consumeJournalEntriesArgs()

        assertNotNull(args)
        assertEquals(10L, args!!.journalId)
        assertEquals(42L, args.journalOwnerId)
        assertEquals("Мой дневник", args.journalTitle)
        assertEquals("ALL", args.viewAccess)
        assertEquals("FRIENDS", args.commentAccess)
        assertEquals("events", args.source)
    }

    @Test
    fun consumeJournalEntriesArgs_whenAccessAndSourceMissing_thenDefaultsApplied() {
        val bundle =
            Bundle().apply {
                putString("journalId", "10")
                putString("userId", "42")
                putString("journalTitle", Uri.encode("Title"))
            }
        val navBackStackEntry = createNavBackStackEntry(bundle, savedStateHandle)

        val args = navBackStackEntry.consumeJournalEntriesArgs()

        assertNotNull(args)
        assertEquals("NOBODY", args!!.viewAccess)
        assertEquals("NOBODY", args.commentAccess)
        assertEquals("profile", args.source)
    }

    @Test
    fun consumeJournalEntriesArgs_whenAccessMalformed_thenFallsBackToNobody() {
        val bundle =
            Bundle().apply {
                putString("journalId", "10")
                putString("userId", "42")
                putString("viewAccess", "UNKNOWN")
                putString("commentAccess", "INVALID")
            }
        val navBackStackEntry = createNavBackStackEntry(bundle, savedStateHandle)

        val args = navBackStackEntry.consumeJournalEntriesArgs()

        assertNotNull(args)
        assertEquals("NOBODY", args!!.viewAccess)
        assertEquals("NOBODY", args.commentAccess)
    }

    @Test
    fun consumeJournalEntriesArgs_whenJournalIdInvalid_thenReturnsNull() {
        val bundle =
            Bundle().apply {
                putString("journalId", "invalid")
                putString("userId", "42")
            }
        val navBackStackEntry = createNavBackStackEntry(bundle, savedStateHandle)

        val args = navBackStackEntry.consumeJournalEntriesArgs()

        assertNull(args)
    }

    @Test
    fun consumeJournalEntriesArgs_whenOwnerIdInvalid_thenReturnsNull() {
        val bundle =
            Bundle().apply {
                putString("journalId", "10")
                putString("userId", "invalid")
            }
        val navBackStackEntry = createNavBackStackEntry(bundle, savedStateHandle)

        val args = navBackStackEntry.consumeJournalEntriesArgs()

        assertNull(args)
    }

    @Test
    fun consumeJournalEntriesArgs_whenArgumentsNull_thenReturnsNull() {
        val navBackStackEntry = createNavBackStackEntry(null, savedStateHandle)

        val args = navBackStackEntry.consumeJournalEntriesArgs()

        assertNull(args)
    }

    private fun createNavBackStackEntry(
        arguments: Bundle?,
        savedStateHandle: SavedStateHandle
    ): androidx.navigation.NavBackStackEntry =
        mockk {
            every { this@mockk.arguments } returns arguments
            every { this@mockk.savedStateHandle } returns savedStateHandle
        }
}
