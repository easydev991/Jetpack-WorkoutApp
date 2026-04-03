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

        val data =
            buildUserAddedParksNavigationData(
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

        val data =
            buildUserAddedParksNavigationData(
                userId = 777L,
                source = "profile",
                addedParks = parks
            )

        assertEquals("user_parks/777?source=profile", data.route)
        assertTrue(data.seedPayload.requiresFetch)
        assertNull(data.seedPayload.parksJson)
    }

    @Test
    fun buildUserAddedParksNavigationData_whenSourceIsParks_thenRouteContainsParksNotPark() {
        val data =
            buildUserAddedParksNavigationData(
                userId = 1L,
                source = "parks",
                addedParks = emptyList()
            )

        assertEquals("user_parks/1?source=parks", data.route)
        assertTrue("Route должен содержать 'source=parks'", data.route.contains("source=parks"))
        assertFalse(
            "Route не должен содержать legacy 'source=park&'",
            data.route.contains("source=park&")
        )
        assertFalse(
            "Route не должен содержать legacy 'source=park\$'",
            data.route.endsWith("source=park")
        )
    }

    @Test
    fun buildUserAddedParksNavigationData_whenAllSources_thenRoutePreservesSourceExactly() {
        val sources = listOf("parks", "events", "messages", "profile", "more")

        sources.forEach { source ->
            val data =
                buildUserAddedParksNavigationData(
                    userId = 1L,
                    source = source,
                    addedParks = emptyList()
                )
            assertEquals(
                "Route должен содержать source=$source без изменений",
                "user_parks/1?source=$source",
                data.route
            )
        }
    }

    private fun createPark(
        id: Long,
        longText: String = "short"
    ): Park =
        Park(
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
