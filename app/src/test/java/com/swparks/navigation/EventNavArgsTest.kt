package com.swparks.navigation

import android.os.Build
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.swparks.data.model.Event
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
class EventNavArgsTest {

    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setUp() {
        savedStateHandle = SavedStateHandle()
    }

    @Test
    fun consumeEventParticipantsArgs_whenValidData_thenReturnsParsedArgs() {
        val users = listOf(createUser(1L), createUser(2L))
        val usersJson = WorkoutAppJson.encodeToString(users)
        savedStateHandle[EVENT_PARTICIPANTS_USERS_JSON_KEY] = usersJson

        val bundle = Bundle().apply {
            putLong("eventId", 42L)
            putString("source", "events")
        }
        val navBackStackEntry = createNavBackStackEntry(bundle, savedStateHandle)

        val args = navBackStackEntry.consumeEventParticipantsArgs()

        assertNotNull(args)
        assertEquals(42L, args!!.eventId)
        assertEquals("events", args.source)
        assertEquals(2, args.users.size)
        assertEquals(1L, args.users[0].id)
        assertEquals(2L, args.users[1].id)
    }

    @Test
    fun consumeEventParticipantsArgs_whenNoUsersJson_thenReturnsEmptyList() {
        val bundle = Bundle().apply {
            putLong("eventId", 100L)
            putString("source", "profile")
        }
        val navBackStackEntry = createNavBackStackEntry(bundle, savedStateHandle)

        val args = navBackStackEntry.consumeEventParticipantsArgs()

        assertNotNull(args)
        assertEquals(100L, args!!.eventId)
        assertEquals("profile", args.source)
        assertEquals(0, args.users.size)
    }

    @Test
    fun consumeEventParticipantsArgs_whenMalformedJson_thenReturnsEmptyList() {
        savedStateHandle[EVENT_PARTICIPANTS_USERS_JSON_KEY] = "{invalid json"
        val bundle = Bundle().apply {
            putLong("eventId", 1L)
            putString("source", "events")
        }
        val navBackStackEntry = createNavBackStackEntry(bundle, savedStateHandle)

        val args = navBackStackEntry.consumeEventParticipantsArgs()

        assertNotNull(args)
        assertEquals(0, args!!.users.size)
    }

    @Test
    fun consumeEventParticipantsArgs_whenConsumed_thenKeyRemovedFromSavedStateHandle() {
        savedStateHandle[EVENT_PARTICIPANTS_USERS_JSON_KEY] = "[]"
        val bundle = Bundle().apply {
            putLong("eventId", 1L)
            putString("source", "events")
        }
        val navBackStackEntry = createNavBackStackEntry(bundle, savedStateHandle)

        navBackStackEntry.consumeEventParticipantsArgs()

        assertNull(savedStateHandle.get<String>(EVENT_PARTICIPANTS_USERS_JSON_KEY))
    }

    @Test
    fun consumeEditEventArgs_whenValidData_thenReturnsParsedArgs() {
        val event = createEvent(77L, "Test Event")
        val eventJson = WorkoutAppJson.encodeToString(event)
        savedStateHandle[EDIT_EVENT_JSON_KEY] = eventJson

        val bundle = Bundle().apply {
            putLong("eventId", 77L)
            putString("source", "events")
        }
        val navBackStackEntry = createNavBackStackEntry(bundle, savedStateHandle)

        val args = navBackStackEntry.consumeEditEventArgs()

        assertNotNull(args)
        assertEquals(77L, args!!.eventId)
        assertEquals("events", args.source)
        assertNotNull(args.event)
        assertEquals(77L, args.event!!.id)
        assertEquals("Test Event", args.event!!.title)
    }

    @Test
    fun consumeEditEventArgs_whenNoEventJson_thenReturnsNullEvent() {
        val bundle = Bundle().apply {
            putLong("eventId", 100L)
            putString("source", "profile")
        }
        val navBackStackEntry = createNavBackStackEntry(bundle, savedStateHandle)

        val args = navBackStackEntry.consumeEditEventArgs()

        assertNotNull(args)
        assertEquals(100L, args!!.eventId)
        assertNull(args.event)
    }

    @Test
    fun consumeEditEventArgs_whenMalformedJson_thenReturnsNullEvent() {
        savedStateHandle[EDIT_EVENT_JSON_KEY] = "{invalid json"
        val bundle = Bundle().apply {
            putLong("eventId", 1L)
            putString("source", "events")
        }
        val navBackStackEntry = createNavBackStackEntry(bundle, savedStateHandle)

        val args = navBackStackEntry.consumeEditEventArgs()

        assertNotNull(args)
        assertNull(args!!.event)
    }

    @Test
    fun consumeEditEventArgs_whenConsumed_thenKeyRemovedFromSavedStateHandle() {
        savedStateHandle[EDIT_EVENT_JSON_KEY] = "{}"
        val bundle = Bundle().apply {
            putLong("eventId", 1L)
            putString("source", "events")
        }
        val navBackStackEntry = createNavBackStackEntry(bundle, savedStateHandle)

        navBackStackEntry.consumeEditEventArgs()

        assertNull(savedStateHandle.get<String>(EDIT_EVENT_JSON_KEY))
    }

    private fun createUser(id: Long) = User(
        id = id,
        name = "User $id",
        image = "https://example.com/user$id.jpg"
    )

    private fun createEvent(id: Long, title: String) = Event(
        id = id,
        title = title,
        description = "Test description",
        beginDate = "2024-01-15",
        countryID = 1,
        cityID = 1,
        preview = "https://example.com/preview.jpg",
        parkID = 1L,
        latitude = "55.7558",
        longitude = "37.6173",
        isCurrent = true,
        photos = emptyList(),
        author = User(id = 1L, name = "Author", image = null),
        name = title
    )

    private fun createNavBackStackEntry(
        arguments: Bundle,
        savedStateHandle: SavedStateHandle
    ): androidx.navigation.NavBackStackEntry {
        return mockk {
            every { this@mockk.arguments } returns arguments
            every { this@mockk.savedStateHandle } returns savedStateHandle
        }
    }
}
