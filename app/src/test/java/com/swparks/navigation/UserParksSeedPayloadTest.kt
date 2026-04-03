package com.swparks.navigation

import com.swparks.data.model.Park
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserParksSeedPayloadTest {
    @Test
    fun fromParks_whenPayloadIsSmall_thenReturnsSeedJson() {
        val payload = UserParksSeedPayload.fromParks(listOf(createPark(1L)))

        assertFalse(payload.requiresFetch)
        assertNotNull(payload.parksJson)
    }

    @Test
    fun fromParks_whenPayloadIsLarge_thenReturnsRequiresFetch() {
        val parks = (1L..200L).map { createPark(it, longText = "x".repeat(3000)) }

        val payload = UserParksSeedPayload.fromParks(parks)

        assertTrue(payload.requiresFetch)
        assertNull(payload.parksJson)
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
