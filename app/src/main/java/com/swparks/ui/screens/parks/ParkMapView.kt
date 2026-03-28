package com.swparks.ui.screens.parks

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.swparks.BuildConfig
import com.swparks.data.model.Park
import com.swparks.ui.screens.parks.map.clusterTextSize
import com.swparks.ui.screens.parks.map.isValidCoordinates
import com.swparks.ui.screens.parks.map.selectedCityBoundsCameraUpdate
import com.swparks.ui.screens.parks.map.toFeatureCollection
import com.swparks.ui.screens.parks.map.toValidLatLngs
import com.swparks.ui.state.MapCameraPosition
import com.swparks.ui.state.MapEvent
import com.swparks.ui.state.UiCoordinates
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Point

private const val SOURCE_ID = "parks_source"
private const val CLUSTERS_LAYER_ID = "clusters"
private const val UNCLUSTERED_LAYER_ID = "unclustered_point"
private const val SELECTED_UNCLUSTERED_LAYER_ID = "selected_unclustered_point"
private const val STYLE_URI = "https://tiles.openfreemap.org/styles/liberty"

private const val DEFAULT_ZOOM = 15.0
private const val CLUSTER_MAX_ZOOM = 14
private const val CLUSTER_RADIUS = 50
private const val MAP_PADDING_PX = 120
private const val INITIAL_COUNTRY_ZOOM = 3.5
private const val MIN_RESTORABLE_ZOOM = 2.0

private const val PARK_MARKER_DIAMETER_PX = 56
private const val SELECTED_MARKER_DIAMETER_PX = 68
private const val CLUSTER_MARKER_DIAMETER_PX = 100
private const val MAX_EXACT_CLUSTER_ICON_COUNT = 2000
private const val CLUSTER_ICON_PREFIX = "cluster-icon-"
private const val PARK_ICON_ID = "park-icon"
private const val SELECTED_PARK_ICON_ID = "selected-park-icon"
private const val CLUSTER_ICON_OVERFLOW_ID = "cluster-icon-overflow"

private const val CAMERA_EPSILON = 0.0001
private const val ZOOM_EPSILON = 0.01
private const val CIRCLE_STROKE_WIDTH_DIVISOR = 12f
private const val PARK_SIGNATURE_ENTRY_SIZE = 24

private const val TAG = "ParkMapView"

private val CLUSTER_COLOR = "#FF6B00".toColorInt()
private val UNCLUSTERED_COLOR = "#00A86B".toColorInt()
private val SELECTED_COLOR = "#FF2D55".toColorInt()
private val MARKER_STROKE_COLOR = "#FFFFFF".toColorInt()

private val DEFAULT_TARGET = UiCoordinates(latitude = 64.0, longitude = 94.0)

private val clusterBitmapCache = mutableMapOf<String, Bitmap>()
private var cachedParkBitmap: Bitmap? = null
private var cachedSelectedParkBitmap: Bitmap? = null

@Composable
fun ParkMapView(
    parks: List<Park>,
    selectedParkId: Long?,
    selectedCityCenter: UiCoordinates?,
    cameraPosition: MapCameraPosition?,
    onMapEvent: (MapEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val appContext = context.applicationContext

    val latestParks by rememberUpdatedState(parks)
    val latestSelectedParkId by rememberUpdatedState(selectedParkId)
    val latestOnMapEvent by rememberUpdatedState(onMapEvent)

    val mapViewBundle = rememberSaveable { Bundle() }

    val mapView = remember {
        MapLibre.getInstance(appContext)
        MapView(context).apply {
            onCreate(mapViewBundle)
        }
    }

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var isStyleLoaded by remember { mutableStateOf(false) }
    var hasResolvedInitialCamera by remember { mutableStateOf(false) }

    var lastAppliedParksSignature by remember { mutableStateOf<String?>(null) }
    var lastAppliedSelectedParkId by remember { mutableStateOf<Long?>(null) }
    var lastAppliedCameraSignature by remember { mutableStateOf<String?>(null) }

    DisposableEffect(mapView, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> {
                    mapView.onSaveInstanceState(mapViewBundle)
                    mapView.onDestroy()
                }

                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        when {
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) -> {
                mapView.onStart()
                mapView.onResume()
            }

            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) -> {
                mapView.onStart()
            }
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onSaveInstanceState(mapViewBundle)
            mapLibreMap = null
        }
    }

    AndroidView(
        factory = {
            mapView.getMapAsync { map ->
                mapLibreMap = map

                map.setStyle(STYLE_URI) { style ->
                    isStyleLoaded = true
                    setupMap(style, latestParks, latestSelectedParkId)
                    lastAppliedParksSignature = parksSignature(latestParks)
                    lastAppliedSelectedParkId = latestSelectedParkId
                    latestOnMapEvent(MapEvent.OnMapReady)
                }

                map.addOnMapClickListener { latLng ->
                    val screenPoint = map.projection.toScreenLocation(latLng)
                    val features = map.queryRenderedFeatures(
                        screenPoint,
                        CLUSTERS_LAYER_ID,
                        UNCLUSTERED_LAYER_ID
                    )

                    val clusterFeature = features.firstOrNull { it.hasProperty("point_count") }

                    when {
                        clusterFeature != null -> {
                            val source = map.style?.getSourceAs<GeoJsonSource>(SOURCE_ID)
                            val expansionZoom = source?.getClusterExpansionZoom(clusterFeature) ?: 0
                            val point = clusterFeature.geometry() as Point

                            latestOnMapEvent(
                                MapEvent.ClusterClick(
                                    target = UiCoordinates(
                                        latitude = point.latitude(),
                                        longitude = point.longitude()
                                    ),
                                    expansionZoom = expansionZoom
                                )
                            )
                        }

                        features.isNotEmpty() -> {
                            val parkId = features.firstOrNull()?.getNumberProperty("id")?.toLong()
                            if (parkId != null) {
                                latestOnMapEvent(MapEvent.SelectPark(parkId))
                            } else {
                                latestOnMapEvent(MapEvent.ClearSelection)
                            }
                        }

                        else -> latestOnMapEvent(MapEvent.ClearSelection)
                    }

                    true
                }

                map.addOnCameraIdleListener {
                    if (!hasResolvedInitialCamera) return@addOnCameraIdleListener

                    val camPos = map.cameraPosition
                    val target = camPos.target ?: return@addOnCameraIdleListener

                    latestOnMapEvent(
                        MapEvent.OnCameraIdle(
                            MapCameraPosition(
                                target = UiCoordinates(
                                    latitude = target.latitude,
                                    longitude = target.longitude
                                ),
                                zoom = camPos.zoom,
                                bearing = camPos.bearing,
                                tilt = camPos.tilt
                            )
                        )
                    )
                }
            }

            mapView
        },
        modifier = modifier.testTag("park_map"),
        update = {
            val map = mapLibreMap ?: return@AndroidView
            if (!isStyleLoaded) return@AndroidView
            val style = map.style ?: return@AndroidView

            val newParksSignature = parksSignature(parks)
            if (lastAppliedParksSignature != newParksSignature) {
                updateParksSource(style, parks)
                lastAppliedParksSignature = newParksSignature
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "parks source updated: parks=${parks.size}")
                }
            }

            if (lastAppliedSelectedParkId != selectedParkId) {
                updateSelectedPark(style, selectedParkId)
                lastAppliedSelectedParkId = selectedParkId
            }
        }
    )

    LaunchedEffect(mapLibreMap, isStyleLoaded, selectedCityCenter, cameraPosition, parks) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (!isStyleLoaded) return@LaunchedEffect

        val newCameraSignature = cameraSignature(
            parks = parks,
            selectedCityCenter = selectedCityCenter,
            cameraPosition = cameraPosition,
            hasResolvedInitialCamera = hasResolvedInitialCamera
        )

        if (lastAppliedCameraSignature == newCameraSignature) return@LaunchedEffect

        val validCoordinates = parks.toValidLatLngs()
        val restorableCameraPosition = cameraPosition?.takeIf { it.zoom >= MIN_RESTORABLE_ZOOM }

        when {
            restorableCameraPosition != null -> {
                val cityBounds = selectedCityCenter?.let { cityCenter ->
                    selectedCityBoundsCameraUpdate(
                        parks = validCoordinates,
                        selectedCityCenter = cityCenter,
                        requestedCamera = restorableCameraPosition
                    )
                }

                if (cityBounds != null) {
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(
                            cityBounds.bounds,
                            MAP_PADDING_PX
                        )
                    )
                } else {
                    val target = LatLng(
                        restorableCameraPosition.target.latitude,
                        restorableCameraPosition.target.longitude
                    )
                    if (!cameraMatches(map, target, restorableCameraPosition.zoom)) {
                        map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                target,
                                restorableCameraPosition.zoom
                            )
                        )
                    }
                }
                hasResolvedInitialCamera = true
            }

            !hasResolvedInitialCamera -> {
                applyInitialCamera(map, parks, selectedCityCenter)
                hasResolvedInitialCamera = true
            }
        }

        lastAppliedCameraSignature = newCameraSignature
    }
}

private fun setupMap(
    style: Style,
    parks: List<Park>,
    selectedParkId: Long?
) {
    ensureMarkerImages(style, parks.size)

    if (style.getSource(SOURCE_ID) == null) {
        val source = GeoJsonSource(
            SOURCE_ID,
            parks.toFeatureCollection(),
            GeoJsonOptions()
                .withCluster(true)
                .withClusterMaxZoom(CLUSTER_MAX_ZOOM)
                .withClusterRadius(CLUSTER_RADIUS)
        )
        style.addSource(source)
    }

    if (style.getLayer(CLUSTERS_LAYER_ID) == null) {
        val clustersLayer = SymbolLayer(CLUSTERS_LAYER_ID, SOURCE_ID).apply {
            setProperties(
                PropertyFactory.iconImage(clusterIconExpression()),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER)
            )
            setFilter(Expression.has("point_count"))
        }
        style.addLayer(clustersLayer)
    }

    if (style.getLayer(UNCLUSTERED_LAYER_ID) == null) {
        val unclusteredLayer = SymbolLayer(UNCLUSTERED_LAYER_ID, SOURCE_ID).apply {
            setProperties(
                PropertyFactory.iconImage(PARK_ICON_ID),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER)
            )
            setFilter(Expression.not(Expression.has("point_count")))
        }
        style.addLayer(unclusteredLayer)
    }

    if (style.getLayer(SELECTED_UNCLUSTERED_LAYER_ID) == null) {
        val selectedLayer = SymbolLayer(SELECTED_UNCLUSTERED_LAYER_ID, SOURCE_ID).apply {
            setProperties(
                PropertyFactory.iconImage(SELECTED_PARK_ICON_ID),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER)
            )
        }
        style.addLayer(selectedLayer)
    }

    updateSelectedPark(style, selectedParkId)
}

private fun updateParksSource(style: Style, parks: List<Park>) {
    val source = style.getSourceAs<GeoJsonSource>(SOURCE_ID) ?: return
    source.setGeoJson(parks.toFeatureCollection())
}

private fun updateSelectedPark(style: Style, selectedParkId: Long?) {
    val selectedLayer = style.getLayer(SELECTED_UNCLUSTERED_LAYER_ID) as? SymbolLayer ?: return

    val filter = if (selectedParkId != null) {
        Expression.all(
            Expression.not(Expression.has("point_count")),
            Expression.eq(Expression.get("id"), Expression.literal(selectedParkId.toDouble()))
        )
    } else {
        Expression.eq(Expression.get("id"), Expression.literal(-1))
    }

    selectedLayer.setFilter(filter)
}

private fun applyInitialCamera(
    map: MapLibreMap,
    parks: List<Park>,
    selectedCityCenter: UiCoordinates?
) {
    val coordinates = parks.toValidLatLngs()
    val cityTarget = selectedCityCenter?.toLatLngOrNull()

    when {
        cityTarget == null && coordinates.isNotEmpty() -> {
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(DEFAULT_TARGET.latitude, DEFAULT_TARGET.longitude),
                    INITIAL_COUNTRY_ZOOM
                )
            )
        }

        coordinates.isEmpty() -> {
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(DEFAULT_TARGET.latitude, DEFAULT_TARGET.longitude),
                    INITIAL_COUNTRY_ZOOM
                )
            )
        }

        coordinates.size == 1 -> {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates.first(), DEFAULT_ZOOM))
        }

        else -> {
            val boundsBuilder = LatLngBounds.Builder()
            coordinates.forEach(boundsBuilder::include)
            map.moveCamera(
                CameraUpdateFactory.newLatLngBounds(
                    boundsBuilder.build(),
                    MAP_PADDING_PX
                )
            )
        }
    }
}

private fun ensureMarkerImages(style: Style, parksCount: Int) {
    style.addImage(
        PARK_ICON_ID,
        cachedParkBitmap ?: createCircleBitmap(
            diameterPx = PARK_MARKER_DIAMETER_PX,
            fillColor = UNCLUSTERED_COLOR
        ).also { cachedParkBitmap = it }
    )

    style.addImage(
        SELECTED_PARK_ICON_ID,
        cachedSelectedParkBitmap ?: createCircleBitmap(
            diameterPx = SELECTED_MARKER_DIAMETER_PX,
            fillColor = SELECTED_COLOR
        ).also { cachedSelectedParkBitmap = it }
    )

    val exactClusterCountLimit = minOf(parksCount, MAX_EXACT_CLUSTER_ICON_COUNT)
    for (count in 2..exactClusterCountLimit) {
        style.addImage(
            "$CLUSTER_ICON_PREFIX$count",
            clusterBitmapCache.getOrPut(count.toString()) {
                createClusterBitmap(count.toString())
            }
        )
    }

    style.addImage(
        CLUSTER_ICON_OVERFLOW_ID,
        clusterBitmapCache.getOrPut(CLUSTER_ICON_OVERFLOW_ID) {
            createClusterBitmap("${MAX_EXACT_CLUSTER_ICON_COUNT}+")
        }
    )
}

private fun clusterIconExpression(): Expression {
    return Expression.switchCase(
        Expression.lte(
            Expression.get("point_count"),
            Expression.literal(MAX_EXACT_CLUSTER_ICON_COUNT)
        ),
        Expression.concat(
            Expression.literal(CLUSTER_ICON_PREFIX),
            Expression.toString(Expression.get("point_count"))
        ),
        Expression.literal(CLUSTER_ICON_OVERFLOW_ID)
    )
}

private fun createCircleBitmap(
    diameterPx: Int,
    fillColor: Int
): Bitmap {
    val bitmap = createBitmap(diameterPx, diameterPx)
    val canvas = Canvas(bitmap)
    val radius = diameterPx / 2f
    val strokeWidth = diameterPx / CIRCLE_STROKE_WIDTH_DIVISOR

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MARKER_STROKE_COLOR
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
    }

    canvas.drawCircle(radius, radius, radius - strokeWidth, fillPaint)
    canvas.drawCircle(radius, radius, radius - strokeWidth, strokePaint)
    return bitmap
}

private fun createClusterBitmap(label: String): Bitmap {
    val bitmap = createCircleBitmap(
        diameterPx = CLUSTER_MARKER_DIAMETER_PX,
        fillColor = CLUSTER_COLOR
    )

    val canvas = Canvas(bitmap)
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = clusterTextSize(label)
    }

    val x = bitmap.width / 2f
    val y = bitmap.height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(label, x, y, textPaint)
    return bitmap
}

private fun UiCoordinates.toLatLngOrNull(): LatLng? {
    if (!isValidCoordinates(latitude, longitude)) {
        Log.w(
            TAG,
            "Пропускаем невалидный target камеры: latitude=$latitude, longitude=$longitude"
        )
        return null
    }
    return LatLng(latitude, longitude)
}

private fun cameraMatches(
    map: MapLibreMap,
    target: LatLng,
    zoom: Double
): Boolean {
    val current = map.cameraPosition
    val currentTarget = current.target ?: return false
    return kotlin.math.abs(currentTarget.latitude - target.latitude) < CAMERA_EPSILON &&
        kotlin.math.abs(currentTarget.longitude - target.longitude) < CAMERA_EPSILON &&
        kotlin.math.abs(current.zoom - zoom) < ZOOM_EPSILON
}

private fun parksSignature(parks: List<Park>): String {
    return buildString(parks.size * PARK_SIGNATURE_ENTRY_SIZE) {
        parks.forEach { park ->
            append(park.id)
            append(':')
            append(park.latitude)
            append(':')
            append(park.longitude)
            append(';')
        }
    }
}

private fun cameraSignature(
    parks: List<Park>,
    selectedCityCenter: UiCoordinates?,
    cameraPosition: MapCameraPosition?,
    hasResolvedInitialCamera: Boolean
): String {
    return buildString {
        append("resolved=")
        append(hasResolvedInitialCamera)
        append("|parks=")
        append(parks.size)
        append("|city=")
        append(selectedCityCenter?.latitude)
        append(':')
        append(selectedCityCenter?.longitude)
        append("|camera=")
        append(cameraPosition?.target?.latitude)
        append(':')
        append(cameraPosition?.target?.longitude)
        append(':')
        append(cameraPosition?.zoom)
        append(':')
        append(cameraPosition?.bearing)
        append(':')
        append(cameraPosition?.tilt)
    }
}
