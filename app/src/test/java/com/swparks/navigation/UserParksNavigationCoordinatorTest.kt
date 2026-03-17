package com.swparks.navigation

import com.swparks.data.model.Park
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserParksNavigationCoordinatorTest {

    @Test
    fun buildUserAddedParksNavigationData_whenSmallPayload_thenKeepsSeedAndRoute() {
        val parks = listOf(createPark(1L))

        val data = buildUserAddedParksNavigationData(
            userId = 123L,
            source = "messages",
            addedParks = parks
        )

        assertEquals("user_parks/123?source=messages", data.route)
        assertFalse(data.seedPayload.requiresFetch)
        assertTrue(data.seedPayload.parksJson?.isNotBlank() == true)
    }

    @Test
    fun buildUserAddedParksNavigationData_whenLargePayload_thenDropsSeedAndRequiresFetch() {
        val parks = (1L..200L).map { createPark(it, longText = "x".repeat(3000)) }

        val data = buildUserAddedParksNavigationData(
            userId = 777L,
            source = "profile",
            addedParks = parks
        )

        assertEquals("user_parks/777?source=profile", data.route)
        assertTrue(data.seedPayload.requiresFetch)
        assertNull(data.seedPayload.parksJson)
    }

    private fun createPark(id: Long, longText: String = "short"): Park = Park(
        id = id,
        name = "Park $id",
        sizeID = 1,
        typeID = 1,
        longitude = "37.6173",
        latitude = "55.7558",
        address = "Address $id $longText",
        cityID = 1,
        countryID = 1,
        preview = "https://example.com/$id.jpg"
    )
}
