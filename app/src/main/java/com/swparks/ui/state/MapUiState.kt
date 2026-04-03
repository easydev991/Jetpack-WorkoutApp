package com.swparks.ui.state

data class UiCoordinates(
    val latitude: Double,
    val longitude: Double
)

data class MapUiState(
    val selectedParkId: Long? = null,
    val userLocation: UiCoordinates? = null,
    val isFollowingUser: Boolean = false,
    val isLoadingLocation: Boolean = false,
    val locationPermissionGranted: Boolean = false,
    val isMapReady: Boolean = false,
    val cameraPosition: MapCameraPosition? = null
)

data class MapCameraPosition(
    val target: UiCoordinates,
    val zoom: Double,
    val bearing: Double = 0.0,
    val tilt: Double = 0.0
)

sealed interface MapEvent {
    data class SelectPark(
        val parkId: Long
    ) : MapEvent

    data object ClearSelection : MapEvent

    data class ClusterClick(
        val target: UiCoordinates,
        val expansionZoom: Int
    ) : MapEvent

    data object CenterOnUser : MapEvent

    data class OnLocationPermissionResult(
        val granted: Boolean
    ) : MapEvent

    data class OnCameraIdle(
        val position: MapCameraPosition
    ) : MapEvent

    data class OnMapLoadFailed(
        val message: String
    ) : MapEvent

    data object OnMapReady : MapEvent
}
