package com.swparks.ui.state

import com.swparks.data.model.Park
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapUiStateTest {

    @Test
    fun defaultMapUiState_hasNullSelectedParkId() {
        val state = MapUiState()
        assertNull(state.selectedParkId)
    }

    @Test
    fun defaultMapUiState_hasNullUserLocation() {
        val state = MapUiState()
        assertNull(state.userLocation)
    }

    @Test
    fun defaultMapUiState_hasDefaultFollowingUserFalse() {
        val state = MapUiState()
        assertEquals(false, state.isFollowingUser)
    }

    @Test
    fun defaultMapUiState_hasDefaultLoadingLocationFalse() {
        val state = MapUiState()
        assertEquals(false, state.isLoadingLocation)
    }

    @Test
    fun defaultMapUiState_hasDefaultPermissionFalse() {
        val state = MapUiState()
        assertEquals(false, state.locationPermissionGranted)
    }

    @Test
    fun defaultMapUiState_hasDefaultMapNotReady() {
        val state = MapUiState()
        assertEquals(false, state.isMapReady)
    }

    @Test
    fun defaultMapUiState_hasNullCameraPosition() {
        val state = MapUiState()
        assertNull(state.cameraPosition)
    }

    @Test
    fun uiCoordinates_holdsLatitudeAndLongitude() {
        val coords = UiCoordinates(latitude = 55.7558, longitude = 37.6173)
        assertEquals(55.7558, coords.latitude, 0.0)
        assertEquals(37.6173, coords.longitude, 0.0)
    }

    @Test
    fun mapCameraPosition_holdsTargetZoomBearingTilt() {
        val target = UiCoordinates(55.7558, 37.6173)
        val position = MapCameraPosition(target = target, zoom = 10.0, bearing = 45.0, tilt = 30.0)
        assertEquals(target, position.target)
        assertEquals(10.0, position.zoom, 0.0)
        assertEquals(45.0, position.bearing, 0.0)
        assertEquals(30.0, position.tilt, 0.0)
    }

    @Test
    fun mapCameraPosition_hasDefaultBearingAndTiltZero() {
        val target = UiCoordinates(55.7558, 37.6173)
        val position = MapCameraPosition(target = target, zoom = 10.0)
        assertEquals(0.0, position.bearing, 0.0)
        assertEquals(0.0, position.tilt, 0.0)
    }

    @Test
    fun park_toDoubleOrNull_convertsValidCoordinates() {
        val park = Park(
            id = 1L,
            name = "Test Park",
            sizeID = 1,
            typeID = 1,
            longitude = "37.6173",
            latitude = "55.7558",
            address = "Test Address",
            cityID = 1,
            countryID = 1,
            preview = ""
        )
        val latitude = park.latitude.toDoubleOrNull()
        val longitude = park.longitude.toDoubleOrNull()
        assertEquals(55.7558, latitude!!, 0.0001)
        assertEquals(37.6173, longitude!!, 0.0001)
    }

    @Test
    fun park_toDoubleOrNull_returnsNullForInvalidLatitude() {
        val park = Park(
            id = 1L,
            name = "Test Park",
            sizeID = 1,
            typeID = 1,
            longitude = "37.6173",
            latitude = "invalid",
            address = "Test Address",
            cityID = 1,
            countryID = 1,
            preview = ""
        )
        val latitude = park.latitude.toDoubleOrNull()
        assertNull(latitude)
    }

    @Test
    fun park_toDoubleOrNull_returnsNullForInvalidLongitude() {
        val park = Park(
            id = 1L,
            name = "Test Park",
            sizeID = 1,
            typeID = 1,
            longitude = "invalid",
            latitude = "55.7558",
            address = "Test Address",
            cityID = 1,
            countryID = 1,
            preview = ""
        )
        val longitude = park.longitude.toDoubleOrNull()
        assertNull(longitude)
    }

    @Test
    fun park_toDoubleOrNull_returnsNullForEmptyStrings() {
        val park = Park(
            id = 1L,
            name = "Test Park",
            sizeID = 1,
            typeID = 1,
            longitude = "",
            latitude = "",
            address = "Test Address",
            cityID = 1,
            countryID = 1,
            preview = ""
        )
        assertNull(park.latitude.toDoubleOrNull())
        assertNull(park.longitude.toDoubleOrNull())
    }

    @Test
    fun park_toDoubleOrNull_returnsNullForOutOfRangeValues() {
        val park = Park(
            id = 1L,
            name = "Test Park",
            sizeID = 1,
            typeID = 1,
            longitude = "200.0",
            latitude = "100.0",
            address = "Test Address",
            cityID = 1,
            countryID = 1,
            preview = ""
        )
        val latitude = park.latitude.toDoubleOrNull()
        val longitude = park.longitude.toDoubleOrNull()
        assertEquals(100.0, latitude!!, 0.0)
        assertEquals(200.0, longitude!!, 0.0)
    }

    @Test
    fun selectParkEvent_containsParkId() {
        val event = MapEvent.SelectPark(123L)
        assertEquals(123L, event.parkId)
    }

    @Test
    fun clearSelectionEvent_isSingleton() {
        val event1 = MapEvent.ClearSelection
        val event2 = MapEvent.ClearSelection
        assertTrue(event1 === event2)
    }

    @Test
    fun clusterClickEvent_containsTargetAndZoom() {
        val target = UiCoordinates(55.7558, 37.6173)
        val event = MapEvent.ClusterClick(target, 14)
        assertEquals(target, event.target)
        assertEquals(14, event.expansionZoom)
    }

    @Test
    fun centerOnUserEvent_isSingleton() {
        val event1 = MapEvent.CenterOnUser
        val event2 = MapEvent.CenterOnUser
        assertTrue(event1 === event2)
    }

    @Test
    fun onLocationPermissionResultEvent_containsGranted() {
        val event = MapEvent.OnLocationPermissionResult(true)
        assertEquals(true, event.granted)
    }

    @Test
    fun onCameraIdleEvent_containsPosition() {
        val target = UiCoordinates(55.7558, 37.6173)
        val position = MapCameraPosition(target = target, zoom = 10.0)
        val event = MapEvent.OnCameraIdle(position)
        assertEquals(position, event.position)
    }

    @Test
    fun onMapLoadFailedEvent_containsMessage() {
        val event = MapEvent.OnMapLoadFailed("Style load failed")
        assertEquals("Style load failed", event.message)
    }

    @Test
    fun onMapReadyEvent_isSingleton() {
        val event1 = MapEvent.OnMapReady
        val event2 = MapEvent.OnMapReady
        assertTrue(event1 === event2)
    }
}
