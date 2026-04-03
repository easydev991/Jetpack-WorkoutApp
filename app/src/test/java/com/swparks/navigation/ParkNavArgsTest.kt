package com.swparks.navigation

import android.os.Build
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.swparks.data.model.User
import com.swparks.util.WorkoutAppJson
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
class ParkNavArgsTest {
    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setUp() {
        savedStateHandle = SavedStateHandle()
    }

    @Test
    fun consumeParkTraineesArgs_whenValidData_thenReturnsParsedArgs() {
        val users = listOf(createUser(1L), createUser(2L))
        val usersJson = WorkoutAppJson.encodeToString(users)
        savedStateHandle[PARK_TRAINEES_USERS_JSON_KEY] = usersJson

        val bundle =
            Bundle().apply {
                putLong("parkId", 42L)
                putString("source", "parks")
            }
        val navBackStackEntry = createNavBackStackEntry(bundle, savedStateHandle)

        val args = navBackStackEntry.consumeParkTraineesArgs()

        assertNotNull(args)
        assertEquals(42L, args!!.parkId)
        assertEquals("parks", args.source)
        assertEquals(2, args.users.size)
        assertEquals(1L, args.users[0].id)
        assertEquals(2L, args.users[1].id)
    }

    @Test
    fun consumeParkTraineesArgs_whenNoUsersJson_thenReturnsEmptyList() {
        val bundle =
            Bundle().apply {
                putLong("parkId", 100L)
                putString("source", "profile")
            }
        val navBackStackEntry = createNavBackStackEntry(bundle, savedStateHandle)

        val args = navBackStackEntry.consumeParkTraineesArgs()

        assertNotNull(args)
        assertEquals(100L, args!!.parkId)
        assertEquals("profile", args.source)
        assertEquals(0, args.users.size)
    }

    @Test
    fun consumeParkTraineesArgs_whenMalformedJson_thenReturnsEmptyList() {
        savedStateHandle[PARK_TRAINEES_USERS_JSON_KEY] = "{invalid json"
        val bundle =
            Bundle().apply {
                putLong("parkId", 1L)
                putString("source", "parks")
            }
        val navBackStackEntry = createNavBackStackEntry(bundle, savedStateHandle)

        val args = navBackStackEntry.consumeParkTraineesArgs()

        assertNotNull(args)
        assertEquals(0, args!!.users.size)
    }

    @Test
    fun consumeParkTraineesArgs_whenConsumed_thenKeyRemovedFromSavedStateHandle() {
        savedStateHandle[PARK_TRAINEES_USERS_JSON_KEY] = "[]"
        val bundle =
            Bundle().apply {
                putLong("parkId", 1L)
                putString("source", "parks")
            }
        val navBackStackEntry = createNavBackStackEntry(bundle, savedStateHandle)

        navBackStackEntry.consumeParkTraineesArgs()

        assertNull(savedStateHandle.get<String>(PARK_TRAINEES_USERS_JSON_KEY))
    }

    private fun createUser(id: Long) =
        User(
            id = id,
            name = "User $id",
            image = "https://example.com/user$id.jpg"
        )

    private fun createNavBackStackEntry(
        arguments: Bundle,
        savedStateHandle: SavedStateHandle
    ): androidx.navigation.NavBackStackEntry =
        mockk {
            every { this@mockk.arguments } returns arguments
            every { this@mockk.savedStateHandle } returns savedStateHandle
        }
}
