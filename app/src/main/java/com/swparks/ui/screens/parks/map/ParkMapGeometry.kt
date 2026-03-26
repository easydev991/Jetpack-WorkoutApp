package com.swparks.ui.screens.parks.map

import com.swparks.data.model.Park
import com.swparks.ui.state.MapCameraPosition
import com.swparks.ui.state.UiCoordinates
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import java.util.Locale

private const val MIN_LATITUDE = -90.0
private const val MAX_LATITUDE = 90.0
private const val MIN_LONGITUDE = -180.0
private const val MAX_LONGITUDE = 180.0
private const val CAMERA_EPSILON = 0.0001
private const val ZOOM_EPSILON = 0.01
private const val INITIAL_CITY_ZOOM = 11.0

internal data class ValidParkPoint(
    val park: Park,
    val latitude: Double,
    val longitude: Double
)

internal data class BoundsCameraUpdate(
    val bounds: LatLngBounds,
    val uniqueCoordinateCount: Int
)

internal fun List<Park>.toFeatureCollection(): FeatureCollection {
    val features = toValidParkPoints().map { parkPoint ->
        Feature.fromGeometry(
            Point.fromLngLat(parkPoint.longitude, parkPoint.latitude)
        ).apply {
            addNumberProperty("id", parkPoint.park.id.toDouble())
            addStringProperty("name", parkPoint.park.name)
        }
    }

    return FeatureCollection.fromFeatures(features)
}

internal fun List<Park>.toValidParkPoints(): List<ValidParkPoint> = mapNotNull { park ->
    val latitude = park.latitude.toDoubleOrNull() ?: return@mapNotNull null
    val longitude = park.longitude.toDoubleOrNull() ?: return@mapNotNull null
    if (!isValidCoordinates(latitude, longitude)) {
        return@mapNotNull null
    }

    ValidParkPoint(
        park = park,
        latitude = latitude,
        longitude = longitude
    )
}

internal fun List<Park>.toValidLatLngs(): List<LatLng> = toValidParkPoints().map { parkPoint ->
    LatLng(parkPoint.latitude, parkPoint.longitude)
}

internal fun selectedCityBoundsCameraUpdate(
    parks: List<LatLng>,
    selectedCityCenter: UiCoordinates,
    requestedCamera: MapCameraPosition
): BoundsCameraUpdate? {
    val uniqueCoordinateCount = parks.uniqueCoordinateCount()
    val shouldBuildBounds = parks.size >= 2 &&
        requestedCamera.isDefaultCityRequestFor(selectedCityCenter) &&
        uniqueCoordinateCount >= 2

    return if (shouldBuildBounds) {
        val boundsBuilder = LatLngBounds.Builder()
        parks.forEach(boundsBuilder::include)
        BoundsCameraUpdate(
            bounds = boundsBuilder.build(),
            uniqueCoordinateCount = uniqueCoordinateCount
        )
    } else {
        null
    }
}

internal fun MapCameraPosition.isDefaultCityRequestFor(selectedCityCenter: UiCoordinates): Boolean {
    return kotlin.math.abs(target.latitude - selectedCityCenter.latitude) < CAMERA_EPSILON &&
        kotlin.math.abs(target.longitude - selectedCityCenter.longitude) < CAMERA_EPSILON &&
        kotlin.math.abs(zoom - INITIAL_CITY_ZOOM) < ZOOM_EPSILON
}

internal fun isValidCoordinates(latitude: Double, longitude: Double): Boolean {
    return latitude in MIN_LATITUDE..MAX_LATITUDE &&
        longitude in MIN_LONGITUDE..MAX_LONGITUDE
}

internal fun List<LatLng>.uniqueCoordinateCount(): Int {
    return map { "${it.latitude.formatCoordinateKey()}:${it.longitude.formatCoordinateKey()}" }
        .toSet()
        .size
}

internal fun Double.formatCoordinateKey(): String = String.format(Locale.US, "%.6f", this)
