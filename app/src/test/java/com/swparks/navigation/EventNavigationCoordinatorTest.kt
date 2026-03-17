package com.swparks.navigation

import com.swparks.data.model.Event
import com.swparks.data.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventNavigationCoordinatorTest {

    @Test
    fun buildEventParticipantsNavigationData_whenUsersProvided_thenKeepsUsersJsonAndRoute() {
        val users = listOf(createUser(1L), createUser(2L))

        val data = buildEventParticipantsNavigationData(
            eventId = 42L,
            source = "events",
            users = users
        )

        assertEquals("event_participants/42?source=events", data.route)
        assertTrue(data.usersJson.isNotBlank())
        assertTrue(data.usersJson.contains("\"id\":1"))
        assertTrue(data.usersJson.contains("\"id\":2"))
    }

    @Test
    fun buildEventParticipantsNavigationData_whenEmptyUsers_thenEmptyJsonArray() {
        val data = buildEventParticipantsNavigationData(
            eventId = 100L,
            source = "profile",
            users = emptyList()
        )

        assertEquals("event_participants/100?source=profile", data.route)
        assertEquals("[]", data.usersJson)
    }

    @Test
    fun buildEditEventNavigationData_whenEventProvided_thenKeepsEventJsonAndRoute() {
        val event = createEvent(77L, "Test Event")

        val data = buildEditEventNavigationData(
            eventId = 77L,
            source = "events",
            event = event
        )

        assertEquals("edit_event/77?source=events", data.route)
        assertTrue(data.eventJson.isNotBlank())
        assertTrue(data.eventJson.contains("\"id\":77"))
        assertTrue(data.eventJson.contains("Test Event"))
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
}
