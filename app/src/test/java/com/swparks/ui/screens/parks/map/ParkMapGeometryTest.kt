package com.swparks.ui.screens.parks.map

import com.swparks.data.model.Park
import com.swparks.ui.state.MapCameraPosition
import com.swparks.ui.state.UiCoordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.maplibre.android.geometry.LatLng

class ParkMapGeometryTest {
    @Test
    fun isValidCoordinates_whenLatitudeOutOfRange_thenReturnsFalse() {
        assertFalse(isValidCoordinates(latitude = 91.0, longitude = 37.0))
    }

    @Test
    fun isValidCoordinates_stringOverload_whenValid_thenReturnsTrue() {
        assertTrue(isValidCoordinates(latitude = "55.75", longitude = "37.61"))
    }

    @Test
    fun isValidCoordinates_stringOverload_whenNull_thenReturnsFalse() {
        assertFalse(isValidCoordinates(latitude = null, longitude = "37.61"))
        assertFalse(isValidCoordinates(latitude = "55.75", longitude = null))
        assertFalse(isValidCoordinates(latitude = null as String?, longitude = null as String?))
    }

    @Test
    fun isValidCoordinates_stringOverload_whenNonNumeric_thenReturnsFalse() {
        assertFalse(isValidCoordinates(latitude = "abc", longitude = "37.61"))
    }

    @Test
    fun isValidCoordinates_stringOverload_whenOutOfRange_thenReturnsFalse() {
        assertFalse(isValidCoordinates(latitude = "91.0", longitude = "37.0"))
    }

    @Test
    fun toValidParkPoints_whenContainsInvalidCoordinates_thenFiltersInvalidParks() {
        val parks =
            listOf(
                createPark(id = 1, latitude = "55.75", longitude = "37.61"),
                createPark(id = 2, latitude = "100.0", longitude = "37.61"),
                createPark(id = 3, latitude = "abc", longitude = "37.61")
            )

        val validPoints = parks.toValidParkPoints()

        assertEquals(1, validPoints.size)
        assertEquals(1L, validPoints.first().park.id)
    }

    @Test
    fun uniqueCoordinateCount_whenDuplicateCoordinates_thenCountsOnce() {
        val coordinates =
            listOf(
                LatLng(55.75, 37.61),
                LatLng(55.75, 37.61),
                LatLng(55.76, 37.62)
            )

        assertEquals(2, coordinates.uniqueCoordinateCount())
    }

    @Test
    fun selectedCityBoundsCameraUpdate_whenDefaultCityCameraAndUniqueCoordinates_thenReturnsBounds() {
        val cityCenter = UiCoordinates(latitude = 55.75, longitude = 37.61)
        val requestedCamera = MapCameraPosition(target = cityCenter, zoom = 11.0)
        val parks =
            listOf(
                LatLng(55.75, 37.61),
                LatLng(55.76, 37.62)
            )

        val result =
            selectedCityBoundsCameraUpdate(
                parks = parks,
                selectedCityCenter = cityCenter,
                requestedCamera = requestedCamera
            )

        assertNotNull(result)
        assertEquals(2, result?.uniqueCoordinateCount)
    }

    @Test
    fun selectedCityBoundsCameraUpdate_whenCameraIsNotDefaultCityRequest_thenReturnsNull() {
        val cityCenter = UiCoordinates(latitude = 55.75, longitude = 37.61)
        val requestedCamera =
            MapCameraPosition(
                target = UiCoordinates(latitude = 55.80, longitude = 37.70),
                zoom = 12.0
            )
        val parks =
            listOf(
                LatLng(55.75, 37.61),
                LatLng(55.76, 37.62)
            )

        val result =
            selectedCityBoundsCameraUpdate(
                parks = parks,
                selectedCityCenter = cityCenter,
                requestedCamera = requestedCamera
            )

        assertNull(result)
    }

    @Test
    fun isDefaultCityRequestFor_whenTargetAndZoomMatch_thenReturnsTrue() {
        val cityCenter = UiCoordinates(latitude = 55.75, longitude = 37.61)
        val requestedCamera = MapCameraPosition(target = cityCenter, zoom = 11.0)

        assertTrue(requestedCamera.isDefaultCityRequestFor(cityCenter))
    }

    private fun createPark(
        id: Long,
        latitude: String,
        longitude: String
    ) = Park(
        id = id,
        name = "Park$id",
        sizeID = 1,
        typeID = 1,
        longitude = longitude,
        latitude = latitude,
        address = "Address$id",
        cityID = 1,
        countryID = 1,
        preview = ""
    )
}
