package com.swparks.ui.screens.parks

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.swparks.data.model.Park
import com.swparks.ui.screens.parks.map.clusterTextSize
import com.swparks.ui.screens.parks.map.isValidCoordinates
import com.swparks.ui.screens.parks.map.selectedCityBoundsCameraUpdate
import com.swparks.ui.screens.parks.map.toFeatureCollection
import com.swparks.ui.screens.parks.map.toValidLatLngs
import com.swparks.ui.screens.parks.map.uniqueCoordinateCount
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
private const val INVALID_PARK_LOG_SAMPLE_COUNT = 5
private const val TAG = "ParkMapView"
private val CLUSTER_COLOR = "#FF6B00".toColorInt()
private val UNCLUSTERED_COLOR = "#00A86B".toColorInt()
private val SELECTED_COLOR = "#FF2D55".toColorInt()
private val MARKER_STROKE_COLOR = "#FFFFFF".toColorInt()
private val DEFAULT_TARGET = UiCoordinates(latitude = 64.0, longitude = 94.0)
private val loggedInvalidParkSummaries = mutableSetOf<String>()
private val clusterBitmapCache = mutableMapOf<String, Bitmap>()
private var cachedParkBitmap: Bitmap? = null
private var cachedSelectedParkBitmap: Bitmap? = null

@Suppress("LongMethod", "CyclomaticComplexMethod")
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
    val appContext = remember(context) { context.applicationContext }
    val mapView = remember(context, appContext) {
        MapLibre.getInstance(appContext)
        MapView(context).apply {
            onCreate(null)
        }
    }
    val latestParks by rememberUpdatedState(parks)
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var isStyleLoaded by remember { mutableStateOf(false) }
    var hasResolvedInitialCamera by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(mapView, lifecycleOwner, appContext) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
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

        @Suppress("EmptyFunctionBlock")
        val componentCallbacks = object : android.content.ComponentCallbacks2 {
            @Deprecated("Deprecated in Java")
            override fun onLowMemory() = mapView.onLowMemory()
            override fun onTrimMemory(level: Int) {}
            override fun onConfigurationChanged(config: android.content.res.Configuration) {}
        }
        appContext.registerComponentCallbacks(componentCallbacks)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            appContext.unregisterComponentCallbacks(componentCallbacks)
            mapLibreMap = null
        }
    }

    AndroidView(
        factory = {
            mapView.getMapAsync { map ->
                mapLibreMap = map
                map.setStyle(STYLE_URI) { style ->
                    isStyleLoaded = true
                    setupMapLayers(style, parks, selectedParkId)
                    onMapEvent(MapEvent.OnMapReady)
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
                            onMapEvent(
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
                                onMapEvent(MapEvent.SelectPark(parkId))
                            } else {
                                Log.w(TAG, "Тап по feature без id, очищаем выделение")
                                onMapEvent(MapEvent.ClearSelection)
                            }
                        }

                        else -> onMapEvent(MapEvent.ClearSelection)
                    }

                    true
                }

                map.addOnCameraIdleListener {
                    if (!hasResolvedInitialCamera) return@addOnCameraIdleListener
                    val camPos = map.cameraPosition
                    val target = camPos.target ?: return@addOnCameraIdleListener
                    logRenderedLayerDiagnostics(map, mapView, latestParks)
                    onMapEvent(
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
            if (isStyleLoaded) {
                val map = mapLibreMap ?: return@AndroidView
                val style = map.style ?: return@AndroidView
                updateParksSource(style, parks)
                updateSelectedPark(style, selectedParkId)
            }
        }
    )

    LaunchedEffect(mapLibreMap, isStyleLoaded, parks, selectedCityCenter, cameraPosition) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (!isStyleLoaded) return@LaunchedEffect
        val validCoordinates = parks.toValidLatLngs()
        logCoordinateDiagnostics(parks, validCoordinates, selectedCityCenter)
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
                    Log.d(
                        TAG,
                        "Применяем fit bounds для выбранного города: " +
                            "${cityBounds.uniqueCoordinateCount} уникальных координат"
                    )
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
                if (cameraPosition != null) {
                    Log.d(
                        TAG,
                        "Игнорируем сохранённую камеру с слишком маленьким zoom=${cameraPosition.zoom}"
                    )
                }
                applyInitialCamera(map, parks, selectedCityCenter)
                hasResolvedInitialCamera = true
            }
        }
    }
}

private fun setupMapLayers(style: Style, parks: List<Park>, selectedParkId: Long?) {
    ensureMarkerImages(style, parks.size)

    val source = GeoJsonSource(
        SOURCE_ID,
        parks.toFeatureCollection(),
        GeoJsonOptions()
            .withCluster(true)
            .withClusterMaxZoom(CLUSTER_MAX_ZOOM)
            .withClusterRadius(CLUSTER_RADIUS)
    )

    style.addSource(source)

    val clustersLayer = SymbolLayer(CLUSTERS_LAYER_ID, SOURCE_ID).apply {
        setProperties(
            PropertyFactory.iconImage(clusterIconExpression()),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true),
            PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER)
        )
        setFilter(Expression.has("point_count"))
    }

    val unclusteredLayer = SymbolLayer(UNCLUSTERED_LAYER_ID, SOURCE_ID).apply {
        setProperties(
            PropertyFactory.iconImage(PARK_ICON_ID),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true),
            PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER)
        )
        setFilter(Expression.not(Expression.has("point_count")))
    }

    val selectedLayer = SymbolLayer(SELECTED_UNCLUSTERED_LAYER_ID, SOURCE_ID).apply {
        setProperties(
            PropertyFactory.iconImage(SELECTED_PARK_ICON_ID),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true),
            PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER)
        )
    }

    style.addLayer(clustersLayer)
    style.addLayer(unclusteredLayer)
    style.addLayer(selectedLayer)
    updateSelectedPark(style, selectedParkId)
    logStyleDiagnostics(style)
}

private fun updateParksSource(style: Style, parks: List<Park>) {
    val source = style.getSourceAs<GeoJsonSource>(SOURCE_ID) ?: return
    val featureCollection = parks.toFeatureCollection()
    Log.d(TAG, "Обновляем source парков: ${featureCollection.features()?.size ?: 0} features")
    source.setGeoJson(featureCollection)
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
            Log.d(
                TAG,
                "Стартовая камера без выбранного города: используем безопасный " +
                    "регион вместо fit-all для ${coordinates.size} парков"
            )
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(DEFAULT_TARGET.latitude, DEFAULT_TARGET.longitude),
                    INITIAL_COUNTRY_ZOOM
                )
            )
        }

        coordinates.isEmpty() -> {
            Log.d(TAG, "Стартовая камера: нет валидных парков, используем регион по умолчанию")
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(DEFAULT_TARGET.latitude, DEFAULT_TARGET.longitude),
                    INITIAL_COUNTRY_ZOOM
                )
            )
        }

        coordinates.size == 1 -> {
            Log.d(TAG, "Стартовая камера: один парк, фокусируемся на нём")
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates.first(), DEFAULT_ZOOM))
        }

        else -> {
            val boundsBuilder = LatLngBounds.Builder()
            coordinates.forEach(boundsBuilder::include)
            Log.d(
                TAG,
                "Стартовая камера: выбран город, вписываем ${coordinates.size} парков в bounds"
            )
            map.moveCamera(
                CameraUpdateFactory.newLatLngBounds(
                    boundsBuilder.build(),
                    MAP_PADDING_PX
                )
            )
        }
    }
}

private fun logCoordinateDiagnostics(
    parks: List<Park>,
    validCoordinates: List<LatLng>,
    selectedCityCenter: UiCoordinates?
) {
    logInvalidCoordinateSummary(parks)
    if (parks.isEmpty()) {
        Log.d(TAG, "Диагностика координат: parks пуст")
        return
    }
    if (validCoordinates.isEmpty()) {
        Log.d(TAG, "Диагностика координат: нет валидных координат для ${parks.size} parks")
        return
    }

    val latitudes = validCoordinates.map { it.latitude }
    val longitudes = validCoordinates.map { it.longitude }
    val uniqueCoordinateCount = validCoordinates.uniqueCoordinateCount()
    val cityDistanceSummary = selectedCityCenter?.let { cityCenter ->
        val farthest = validCoordinates.maxOf {
            kotlin.math.abs(it.latitude - cityCenter.latitude) +
                kotlin.math.abs(it.longitude - cityCenter.longitude)
        }
        ", selectedCityCenter=(${cityCenter.latitude}, ${cityCenter.longitude}), maxDistance=$farthest"
    }.orEmpty()

    Log.d(
        TAG,
        "Диагностика координат: total=${parks.size}, valid=${validCoordinates.size}, unique=$uniqueCoordinateCount, " +
            "lat=[${latitudes.minOrNull()}, ${latitudes.maxOrNull()}], " +
            "lon=[${longitudes.minOrNull()}, ${longitudes.maxOrNull()}]$cityDistanceSummary"
    )
}

private fun logInvalidCoordinateSummary(parks: List<Park>) {
    val invalidParkIds = parks.mapNotNull { park ->
        val latitude = park.latitude.toDoubleOrNull()
        val longitude = park.longitude.toDoubleOrNull()
        if (latitude == null || longitude == null || !isValidCoordinates(latitude, longitude)) {
            park.id
        } else {
            null
        }
    }
    if (invalidParkIds.isEmpty()) return

    val sampleIds = invalidParkIds.take(INVALID_PARK_LOG_SAMPLE_COUNT)
    val summaryKey = "${invalidParkIds.size}:${sampleIds.joinToString(",")}"
    if (!loggedInvalidParkSummaries.add(summaryKey)) return

    Log.w(
        TAG,
        "Не добавляем parks с невалидными координатами: count=${invalidParkIds.size}, sampleIds=$sampleIds"
    )
}

private fun logStyleDiagnostics(style: Style) {
    Log.d(
        TAG,
        "Style diagnostics: source=${style.getSource(SOURCE_ID) != null}, " +
            "clustersLayer=${style.getLayer(CLUSTERS_LAYER_ID) != null}, " +
            "unclusteredLayer=${style.getLayer(UNCLUSTERED_LAYER_ID) != null}, " +
            "selectedLayer=${style.getLayer(SELECTED_UNCLUSTERED_LAYER_ID) != null}"
    )
}

private fun logRenderedLayerDiagnostics(
    map: MapLibreMap,
    mapView: MapView,
    parks: List<Park>
) {
    if (mapView.width <= 0 || mapView.height <= 0) return

    val validCoordinates = parks.toValidLatLngs()
    val visibleParks = validCoordinates.count { coordinate ->
        map.projection.visibleRegion.latLngBounds.contains(coordinate)
    }

    Log.d(
        TAG,
        "Rendered diagnostics: visibleParks=$visibleParks, viewport=${mapView.width}x${mapView.height}, " +
            "zoom=${map.cameraPosition.zoom}"
    )
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
