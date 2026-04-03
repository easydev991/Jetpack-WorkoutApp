package com.swparks.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class NewParkDraftTest {
    private fun createDraft(
        latitude: Double = 55.7558,
        longitude: Double = 37.6173,
        lastLocationRequestDate: Long? = null,
        address: String = "Moscow, Red Square",
        cityId: Int? = 1
    ) = NewParkDraft(
        latitude = latitude,
        longitude = longitude,
        lastLocationRequestDate = lastLocationRequestDate,
        address = address,
        cityId = cityId
    )

    @Test
    fun isEmpty_whenLatitudeZero_thenReturnsTrue() {
        val draft = createDraft(latitude = 0.0)
        assertTrue(draft.isEmpty)
    }

    @Test
    fun isEmpty_whenLongitudeZero_thenReturnsTrue() {
        val draft = createDraft(longitude = 0.0)
        assertTrue(draft.isEmpty)
    }

    @Test
    fun isEmpty_whenBothZero_thenReturnsTrue() {
        val draft = createDraft(latitude = 0.0, longitude = 0.0)
        assertTrue(draft.isEmpty)
    }

    @Test
    fun isEmpty_whenCoordinatesValid_thenReturnsFalse() {
        val draft = createDraft()
        assertFalse(draft.isEmpty)
    }

    @Test
    fun shouldRequestLocation_whenEmpty_thenReturnsTrue() {
        val draft = createDraft(latitude = 0.0, longitude = 0.0)
        assertTrue(draft.shouldRequestLocation)
    }

    @Test
    fun shouldRequestLocation_whenNoLastDate_thenReturnsTrue() {
        val draft = createDraft(lastLocationRequestDate = null)
        assertTrue(draft.shouldRequestLocation)
    }

    @Test
    fun shouldRequestLocation_whenLastDateRecent_thenReturnsFalse() {
        val recentDate = System.currentTimeMillis()
        val draft = createDraft(lastLocationRequestDate = recentDate)
        assertFalse(draft.shouldRequestLocation)
    }

    @Test
    fun shouldRequestLocation_whenLastDateOld_thenReturnsTrue() {
        val oldDate = System.currentTimeMillis() - 15_000L
        val draft = createDraft(lastLocationRequestDate = oldDate)
        assertTrue(draft.shouldRequestLocation)
    }

    @Test
    fun shouldPerformGeocode_whenAddressEmpty_thenReturnsTrue() {
        val draft = createDraft(address = "")
        assertTrue(draft.shouldPerformGeocode)
    }

    @Test
    fun shouldPerformGeocode_whenCityIdNull_thenReturnsTrue() {
        val draft = createDraft(cityId = null)
        assertTrue(draft.shouldPerformGeocode)
    }

    @Test
    fun shouldPerformGeocode_whenCityIdZero_thenReturnsTrue() {
        val draft = createDraft(cityId = 0)
        assertTrue(draft.shouldPerformGeocode)
    }

    @Test
    fun shouldPerformGeocode_whenAllDataValid_thenReturnsFalse() {
        val draft = createDraft()
        assertFalse(draft.shouldPerformGeocode)
    }

    @Test
    fun empty_createsInstanceWithDefaultValues() {
        val empty = NewParkDraft.EMPTY
        assertEquals(0.0, empty.latitude, 0.0)
        assertEquals(0.0, empty.longitude, 0.0)
        assertEquals(null, empty.lastLocationRequestDate)
        assertEquals("", empty.address)
        assertEquals(null, empty.cityId)
        assertTrue(empty.isEmpty)
    }

    @Test
    fun withCoordinates_createsCopyWithNewCoordsAndCurrentDate() {
        val original =
            createDraft(
                latitude = 55.0,
                longitude = 37.0,
                lastLocationRequestDate = 1000L,
                address = "Old Address",
                cityId = 5
            )
        val updated = original.withCoordinates(59.9343, 30.3351)

        assertEquals(59.9343, updated.latitude, 0.0)
        assertEquals(30.3351, updated.longitude, 0.0)
        assertNotSame(original.lastLocationRequestDate, updated.lastLocationRequestDate)
        assertTrue(updated.lastLocationRequestDate!! > original.lastLocationRequestDate!!)
        assertEquals("Old Address", updated.address)
        assertEquals(5, updated.cityId)
    }

    @Test
    fun withGeocodingData_createsCopyWithNewAddressAndCityId() {
        val original =
            createDraft(
                latitude = 55.7558,
                longitude = 37.6173,
                address = "",
                cityId = null
            )
        val updated = original.withGeocodingData("New Address", 3)

        assertEquals(55.7558, updated.latitude, 0.0)
        assertEquals(37.6173, updated.longitude, 0.0)
        assertEquals("New Address", updated.address)
        assertEquals(3, updated.cityId)
    }

    @Test
    fun withoutAddress_createsCopyWithEmptyAddress() {
        val original =
            createDraft(
                latitude = 55.7558,
                longitude = 37.6173,
                address = "Some Address",
                cityId = 5
            )
        val updated = original.withoutAddress()

        assertEquals(55.7558, updated.latitude, 0.0)
        assertEquals(37.6173, updated.longitude, 0.0)
        assertEquals("", updated.address)
        assertEquals(5, updated.cityId)
    }

    @Test
    fun updatingLastLocationRequestDate_createsCopyWithCurrentDate() {
        val original = createDraft(lastLocationRequestDate = 1000L)
        val updated = original.updatingLastLocationRequestDate()

        assertNotSame(original.lastLocationRequestDate, updated.lastLocationRequestDate)
        assertTrue(updated.lastLocationRequestDate!! > original.lastLocationRequestDate!!)
    }

    @Test
    fun copy_preservesAllFieldsWhenNotSpecified() {
        val original =
            createDraft(
                latitude = 55.7558,
                longitude = 37.6173,
                lastLocationRequestDate = 1000L,
                address = "Test Address",
                cityId = 42
            )
        val copy = original.copy()

        assertEquals(original.latitude, copy.latitude, 0.0)
        assertEquals(original.longitude, copy.longitude, 0.0)
        assertEquals(original.lastLocationRequestDate, copy.lastLocationRequestDate)
        assertEquals(original.address, copy.address)
        assertEquals(original.cityId, copy.cityId)
    }
}
