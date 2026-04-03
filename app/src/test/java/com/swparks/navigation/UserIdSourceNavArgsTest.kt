package com.swparks.navigation

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
class UserIdSourceNavArgsTest {
    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setUp() {
        savedStateHandle = SavedStateHandle()
    }

    @Test
    fun consumeUserIdSourceArgs_whenValidData_thenReturnsParsedArgs() {
        val bundle =
            Bundle().apply {
                putString("userId", "42")
                putString("source", "events")
            }
        val navBackStackEntry = createNavBackStackEntry(bundle, savedStateHandle)

        val args = navBackStackEntry.consumeUserIdSourceArgs()

        assertNotNull(args)
        assertEquals(42L, args!!.userId)
        assertEquals("events", args.source)
    }

    @Test
    fun consumeUserIdSourceArgs_whenNoSource_thenReturnsDefaultSource() {
        val bundle =
            Bundle().apply {
                putString("userId", "100")
            }
        val navBackStackEntry = createNavBackStackEntry(bundle, savedStateHandle)

        val args = navBackStackEntry.consumeUserIdSourceArgs()

        assertNotNull(args)
        assertEquals(100L, args!!.userId)
        assertEquals("profile", args.source)
    }

    @Test
    fun consumeUserIdSourceArgs_whenCustomDefaultSource_thenReturnsCustomDefault() {
        val bundle =
            Bundle().apply {
                putString("userId", "200")
            }
        val navBackStackEntry = createNavBackStackEntry(bundle, savedStateHandle)

        val args = navBackStackEntry.consumeUserIdSourceArgs(defaultSource = "messages")

        assertNotNull(args)
        assertEquals(200L, args!!.userId)
        assertEquals("messages", args.source)
    }

    @Test
    fun consumeUserIdSourceArgs_whenNullUserId_thenReturnsNull() {
        val bundle =
            Bundle().apply {
                putString("source", "events")
            }
        val navBackStackEntry = createNavBackStackEntry(bundle, savedStateHandle)

        val args = navBackStackEntry.consumeUserIdSourceArgs()

        assertNull(args)
    }

    @Test
    fun consumeUserIdSourceArgs_whenInvalidUserId_thenReturnsNull() {
        val bundle =
            Bundle().apply {
                putString("userId", "invalid")
                putString("source", "events")
            }
        val navBackStackEntry = createNavBackStackEntry(bundle, savedStateHandle)

        val args = navBackStackEntry.consumeUserIdSourceArgs()

        assertNull(args)
    }

    @Test
    fun consumeUserIdSourceArgs_whenNullArguments_thenReturnsNull() {
        val navBackStackEntry = createNavBackStackEntry(null, savedStateHandle)

        val args = navBackStackEntry.consumeUserIdSourceArgs()

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
