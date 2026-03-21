package com.swparks.navigation

import com.swparks.data.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParkNavigationCoordinatorTest {

    @Test
    fun buildParkTraineesNavigationData_whenUsersProvided_thenKeepsUsersJsonAndRoute() {
        val users = listOf(createUser(1L), createUser(2L))

        val data = buildParkTraineesNavigationData(
            parkId = 42L,
            source = "parks",
            users = users
        )

        assertEquals("park_trainees/42?source=parks", data.route)
        assertTrue(data.usersJson.isNotBlank())
        assertTrue(data.usersJson.contains("\"id\":1"))
        assertTrue(data.usersJson.contains("\"id\":2"))
    }

    @Test
    fun buildParkTraineesNavigationData_whenEmptyUsers_thenEmptyJsonArray() {
        val data = buildParkTraineesNavigationData(
            parkId = 100L,
            source = "profile",
            users = emptyList()
        )

        assertEquals("park_trainees/100?source=profile", data.route)
        assertEquals("[]", data.usersJson)
    }

    private fun createUser(id: Long) = User(
        id = id,
        name = "User $id",
        image = "https://example.com/user$id.jpg"
    )
}
