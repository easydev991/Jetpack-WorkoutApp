package com.swparks.ui.viewmodel

import android.Manifest
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.swparks.JetpackWorkoutApplication
import com.swparks.data.model.City
import com.swparks.data.model.NewParkDraft
import com.swparks.data.model.Park
import com.swparks.data.model.ParkFilter
import com.swparks.data.model.ParkSize
import com.swparks.data.model.ParkType
import com.swparks.data.preferences.ParksFilterDataStore
import com.swparks.data.repository.SWRepository
import com.swparks.domain.provider.LocationService
import com.swparks.domain.provider.LocationSettingsCheckResult
import com.swparks.domain.repository.CountriesRepository
import com.swparks.domain.usecase.ICreateParkLocationHandler
import com.swparks.domain.usecase.IFilterParksUseCase
import com.swparks.domain.usecase.IInitializeParksUseCase
import com.swparks.domain.usecase.SyncParksUseCase
import com.swparks.ui.model.ParksTab
import com.swparks.ui.screens.parks.map.isValidCoordinates
import com.swparks.ui.state.MapCameraPosition
import com.swparks.ui.state.MapEvent
import com.swparks.ui.state.MapUiState
import com.swparks.ui.state.UiCoordinates
import com.swparks.util.AppError
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.pow

@Suppress("LongParameterList")
class ParksRootViewModel(
    private val createParkLocationHandler: ICreateParkLocationHandler,
    private val logger: Logger,
    private val filterParksUseCase: IFilterParksUseCase,
    private val parksFilterDataStore: ParksFilterDataStore,
    private val countriesRepository: CountriesRepository,
    private val swRepository: SWRepository,
    private val initializeParksUseCase: IInitializeParksUseCase,
    private val userNotifier: UserNotifier,
    private val locationService: LocationService,
    private val syncParksUseCase: SyncParksUseCase
) : ViewModel(), IParksRootViewModel {

    override val parksFilter: StateFlow<ParkFilter> = parksFilterDataStore.filter
        .stateIn(viewModelScope, SharingStarted.Eagerly, ParkFilter())

    private val _uiState = MutableStateFlow(ParksRootUiState(localFilter = parksFilter.value))
    override val uiState: StateFlow<ParksRootUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ParksRootEvent>()
    override val events: SharedFlow<ParksRootEvent> = _events.asSharedFlow()

    override var permissionLauncher: ((Map<String, Boolean>) -> Unit)? = null
    override var openSettingsLauncher: ((Intent) -> Unit)? = null
    override var resolveLocationSettingsLauncher: ((android.content.IntentSender) -> Unit)? = null

    override val selectedTab: MutableStateFlow<ParksTab> = MutableStateFlow(ParksTab.LIST)

    private var allParks: List<Park> = emptyList()

    init {
        viewModelScope.launch {
            parksFilterDataStore.filter.collect { filter ->
                logger.d(TAG, "parksFilterDataStore.filter emitted: $filter")
                _uiState.value = _uiState.value.copy(
                    localFilter = filter,
                    isLoadingFilter = false
                )
                recalculateFilteredParks()
                restoreSelectedCity(filter.selectedCityId)
            }
        }
        observeParks()
        loadCities()
        viewModelScope.launch {
            initializeParks()
            syncParks()
        }
    }

    private fun observeParks() {
        viewModelScope.launch {
            swRepository.getParksFlow().collect { parks ->
                updateParks(parks)
            }
        }
    }

    private fun loadCities() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingCities = true)
            try {
                val citiesList = countriesRepository.getAllCities()
                _uiState.value = _uiState.value.copy(
                    cities = citiesList,
                    isLoadingCities = false
                )
                logger.d(TAG, "Загружено городов: ${citiesList.size}")
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                logger.e(TAG, "Ошибка загрузки городов", e)
                _uiState.value = _uiState.value.copy(isLoadingCities = false)
            }
        }
    }

    private suspend fun syncParks(force: Boolean = false) {
        if (force) {
            logger.d(TAG, "Принудительное обновление площадок")
        } else {
            logger.d(TAG, "Проверка необходимости обновления площадок")
        }
        val result = syncParksUseCase(force = force)
        result.onFailure { error ->
            val message = if (force) {
                "Ошибка принудительного обновления площадок"
            } else {
                "Ошибка обновления площадок"
            }
            logger.e(TAG, message, error)
        }
    }

    private suspend fun initializeParks() {
        logger.d(TAG, "Инициализация seed площадок")
        initializeParksUseCase()
    }

    override fun refresh() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            try {
                syncParks(force = true)
            } finally {
                _uiState.value = _uiState.value.copy(isRefreshing = false)
            }
        }
    }

    private fun restoreSelectedCity(cityId: Int?) {
        if (cityId == null) {
            _uiState.value = _uiState.value.copy(selectedCity = null)
            recalculateFilteredParks()
            return
        }
        viewModelScope.launch {
            val city = countriesRepository.getCityById(cityId.toString())
            _uiState.value = _uiState.value.copy(selectedCity = city)
            if (city != null) {
                logger.d(TAG, "Восстановлен выбранный город: ${city.name}")
            } else {
                logger.d(TAG, "Не удалось восстановить город по id=$cityId")
            }
            recalculateFilteredParks()
        }
    }

    private fun recalculateFilteredParks() {
        if (allParks.isEmpty()) return
        val baseFiltered = filterParksUseCase(allParks, _uiState.value.localFilter)
        val filtered = normalizeSelectedCityParks(
            parks = baseFiltered,
            selectedCity = _uiState.value.selectedCity
        )
        val currentMapState = _uiState.value.mapState
        val selectedParkId = currentMapState.selectedParkId
        val updatedMapState =
            if (selectedParkId != null && filtered.none { it.id == selectedParkId }) {
                logger.d(
                    TAG,
                    "Выбранный парк $selectedParkId больше не в filteredParks, сбрасываем выбор"
                )
                currentMapState.copy(selectedParkId = null)
            } else {
                currentMapState
            }
        val withValidCoords = filtered.filter { park ->
            isValidCoordinates(park.latitude, park.longitude)
        }
        _uiState.value = _uiState.value.copy(
            filteredParks = withValidCoords,
            mapState = updatedMapState
        )
    }

    companion object {
        private const val TAG = "ParksRootViewModel"
        private const val CITY_CAMERA_ZOOM = 11.0
        private const val USER_LOCATION_CAMERA_ZOOM = 15.0
        private const val SUSPICIOUS_CITY_RADIUS_KM = 250.0
        private const val NORMALIZED_CITY_RADIUS_KM = 75.0
        private const val EARTH_RADIUS_KM = 6371.0

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as JetpackWorkoutApplication
                val container = application.container
                ParksRootViewModel(
                    createParkLocationHandler = container.createParkLocationHandler,
                    logger = container.logger,
                    filterParksUseCase = container.filterParksUseCase,
                    parksFilterDataStore = container.parksFilterDataStore,
                    countriesRepository = container.countriesRepository,
                    swRepository = container.swRepository,
                    initializeParksUseCase = container.initializeParksUseCase,
                    userNotifier = container.userNotifier,
                    locationService = container.locationService,
                    syncParksUseCase = container.syncParksUseCase
                )
            }
        }
    }

    private fun cameraPositionForCity(city: City): MapCameraPosition? {
        val parsedCoordinates = parseCityCoordinates(city)
        return if (parsedCoordinates != null) {
            MapCameraPosition(
                target = parsedCoordinates,
                zoom = CITY_CAMERA_ZOOM
            )
        } else {
            logger.d(
                TAG,
                "Не удалось построить камеру для города ${city.name}: координаты не парсятся"
            )
            null
        }
    }

    private fun normalizeSelectedCityParks(
        parks: List<Park>,
        selectedCity: City?
    ): List<Park> {
        val cityCoordinates = selectedCity?.let(::parseCityCoordinates)
        val parksWithDistance = if (cityCoordinates != null && parks.isNotEmpty()) {
            parks.mapNotNull { park ->
                val latitude = park.latitude.toDoubleOrNull() ?: return@mapNotNull null
                val longitude = park.longitude.toDoubleOrNull() ?: return@mapNotNull null
                if (!isValidCoordinates(latitude, longitude)) {
                    return@mapNotNull null
                }

                park to haversineDistanceKm(
                    startLatitude = cityCoordinates.latitude,
                    startLongitude = cityCoordinates.longitude,
                    endLatitude = latitude,
                    endLongitude = longitude
                )
            }
        } else {
            emptyList()
        }

        if (parks.isNotEmpty() && cityCoordinates != null) {
            val validCount = parksWithDistance.size
            val totalCount = parks.size
            val invalidCoordCount = totalCount - validCount
            logger.d(
                TAG,
                "Координаты площадок (${selectedCity?.name}): " +
                    "всего=$totalCount, валидные=$validCount, невалидные=$invalidCoordCount"
            )
        }

        return when {
            selectedCity == null || parks.isEmpty() -> parks
            cityCoordinates == null -> {
                logger.d(
                    TAG,
                    "Нормализация города ${selectedCity.name} пропущена: координаты не парсятся"
                )
                parks
            }

            parksWithDistance.isEmpty() -> parks
            parksWithDistance.maxOf { it.second } <= SUSPICIOUS_CITY_RADIUS_KM -> {
                logger.d(
                    TAG,
                    "Фильтр города ${selectedCity.name} выглядит консистентно: " +
                        "${parksWithDistance.size} parks, " +
                        "farthestDistanceKm=${parksWithDistance.maxOf { it.second }}"
                )
                parks
            }

            else -> normalizeWideCityFilter(
                parks = parks,
                selectedCity = selectedCity,
                parksWithDistance = parksWithDistance
            )
        }
    }

    private fun normalizeWideCityFilter(
        parks: List<Park>,
        selectedCity: City,
        parksWithDistance: List<Pair<Park, Double>>
    ): List<Park> {
        val farthestDistance = parksWithDistance.maxOf { it.second }
        val normalized = parksWithDistance
            .filter { (_, distanceKm) -> distanceKm <= NORMALIZED_CITY_RADIUS_KM }
            .map { it.first }

        logger.d(
            TAG,
            "Нормализуем parks для города ${selectedCity.name}: base=${parks.size}, " +
                "valid=${parksWithDistance.size}, normalized=${normalized.size}, " +
                "farthestDistanceKm=$farthestDistance, radiusKm=$NORMALIZED_CITY_RADIUS_KM"
        )

        return normalized.ifEmpty { parks }
    }

    private fun haversineDistanceKm(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double
    ): Double {
        val latitudeDelta = Math.toRadians(endLatitude - startLatitude)
        val longitudeDelta = Math.toRadians(endLongitude - startLongitude)
        val startLatitudeRad = Math.toRadians(startLatitude)
        val endLatitudeRad = Math.toRadians(endLatitude)

        val haversine = kotlin.math.sin(latitudeDelta / 2).pow(2.0) +
            kotlin.math.cos(startLatitudeRad) * kotlin.math.cos(endLatitudeRad) *
            kotlin.math.sin(longitudeDelta / 2).pow(2.0)
        val angularDistance = 2 * kotlin.math.atan2(
            kotlin.math.sqrt(haversine),
            kotlin.math.sqrt(1 - haversine)
        )
        return EARTH_RADIUS_KM * angularDistance
    }

    private fun parseCityCoordinates(city: City): UiCoordinates? {
        val latitude = city.lat.toDoubleOrNull() ?: return null
        val longitude = city.lon.toDoubleOrNull() ?: return null
        return if (isValidCoordinates(latitude, longitude)) {
            UiCoordinates(latitude = latitude, longitude = longitude)
        } else {
            null
        }
    }

    override fun updateParks(parks: List<Park>) {
        allParks = parks
        _uiState.value = _uiState.value.copy(hasParks = parks.isNotEmpty())
        recalculateFilteredParks()
    }

    override val cityNames: List<String>
        get() = _uiState.value.cities
            .filter { it.name.contains(_uiState.value.citySearchQuery, ignoreCase = true) }
            .map { it.name }

    override fun onPermissionGranted() {
        logger.d(TAG, "Разрешение получено")
        handlePermissionGranted()
    }

    override fun onPermissionDenied(shouldShowRationale: Boolean) {
        logger.d(TAG, "Разрешение отклонено, shouldShowRationale=$shouldShowRationale")
        if (shouldShowRationale) {
            _uiState.value = ParksRootUiState(
                showPermissionDialog = true,
                permissionDialogCause = PermissionDialogCause.DENIED
            )
        } else {
            requestPermission()
        }
    }

    override fun onPermissionResult(permissions: Map<String, Boolean>) {
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val isGranted = fineLocationGranted || coarseLocationGranted

        logger.d(
            TAG,
            "Результат запроса разрешений: fine=$fineLocationGranted, coarse=$coarseLocationGranted"
        )
        if (isGranted) {
            handlePermissionGranted()
        } else {
            _uiState.value = ParksRootUiState(
                showPermissionDialog = true,
                permissionDialogCause = PermissionDialogCause.FOREVER_DENIED
            )
        }
    }

    override fun onLocationSettingsResolutionResult(succeeded: Boolean) {
        logger.d(TAG, "Результат resolution dialog геолокации: $succeeded")
        if (!succeeded) {
            _uiState.value = _uiState.value.copy(
                mapState = _uiState.value.mapState.copy(isLoadingLocation = false)
            )
            userNotifier.showInfo("Включение геолокации отменено")
            return
        }

        if (!_uiState.value.mapState.locationPermissionGranted) {
            _uiState.value = _uiState.value.copy(
                mapState = _uiState.value.mapState.copy(isLoadingLocation = false)
            )
            userNotifier.handleError(
                AppError.LocationFailed(
                    message = "Нет разрешения на геолокацию",
                    cause = SecurityException("Location permission is not granted")
                )
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            mapState = _uiState.value.mapState.copy(isLoadingLocation = true)
        )
        getCurrentLocationAndUpdate()
    }

    override fun onDismissDialog() {
        logger.d(TAG, "Диалог разрешения закрыт")
        _uiState.value = _uiState.value.copy(
            showPermissionDialog = false,
            permissionDialogCause = null
        )
    }

    override fun onConfirmDialog() {
        logger.d(TAG, "Подтверждение в диалоге, запрашиваем разрешение")
        _uiState.value = _uiState.value.copy(
            showPermissionDialog = false,
            permissionDialogCause = null
        )
        requestPermission()
    }

    override fun onOpenSettings(intent: Intent) {
        logger.d(TAG, "Открываем настройки приложения")
        _uiState.value = _uiState.value.copy(
            showPermissionDialog = false,
            permissionDialogCause = null
        )
        openSettingsLauncher?.invoke(intent)
        viewModelScope.launch {
            _events.emit(ParksRootEvent.OpenSettings)
        }
    }

    override fun onLocalFilterChange(filter: ParkFilter) {
        logger.d(TAG, "onLocalFilterChange: $filter")
        _uiState.value = _uiState.value.copy(localFilter = filter)
    }

    override fun onFilterToggleSize(size: ParkSize) {
        logger.d(TAG, "onFilterToggleSize: $size")
        val current = _uiState.value.localFilter
        val newFilter = if (current.sizes.contains(size)) {
            if (current.sizes.size > 1) {
                current.copy(sizes = current.sizes - size)
            } else current
        } else {
            current.copy(sizes = current.sizes + size)
        }
        logger.d(TAG, "onFilterToggleSize: newFilter = $newFilter")
        _uiState.value = _uiState.value.copy(localFilter = newFilter)
    }

    override fun onFilterToggleType(type: ParkType) {
        logger.d(TAG, "onFilterToggleType: $type")
        val current = _uiState.value.localFilter
        val newFilter = if (current.types.contains(type)) {
            if (current.types.size > 1) {
                current.copy(types = current.types - type)
            } else current
        } else {
            current.copy(types = current.types + type)
        }
        logger.d(TAG, "onFilterToggleType: newFilter = $newFilter")
        _uiState.value = _uiState.value.copy(localFilter = newFilter)
    }

    override fun onFilterReset() {
        logger.d(TAG, "onFilterReset")
        _uiState.value = _uiState.value.copy(localFilter = ParkFilter())
    }

    override fun onFilterApply() {
        val sizeTypeFilter = _uiState.value.localFilter
        val cityId = _uiState.value.selectedCity?.id?.toIntOrNull()
        val finalFilter = sizeTypeFilter.copy(selectedCityId = cityId)
        logger.d(TAG, "onFilterApply: saving $finalFilter")
        viewModelScope.launch {
            parksFilterDataStore.saveFilter(finalFilter)
        }
        _uiState.value = _uiState.value.copy(showFilterDialog = false)
    }

    override fun onShowFilterDialog() {
        logger.d(TAG, "onShowFilterDialog: parksFilter=${parksFilter.value}")
        _uiState.value = _uiState.value.copy(
            showFilterDialog = true,
            localFilter = parksFilter.value
        )
    }

    override fun onDismissFilterDialog() {
        logger.d(TAG, "onDismissFilterDialog")
        _uiState.value = _uiState.value.copy(showFilterDialog = false)
    }

    override fun onSelectCityClick() {
        logger.d(TAG, "onSelectCityClick")
    }

    override fun onCitySearchQueryChange(query: String) {
        logger.d(TAG, "onCitySearchQueryChange: $query")
        _uiState.value = _uiState.value.copy(citySearchQuery = query)
    }

    override fun onCitySelected(cityName: String) {
        logger.d(TAG, "onCitySelected: $cityName")
        val city = _uiState.value.cities.find { it.name == cityName }
        val cityId = city?.id?.toIntOrNull()
        if (city != null && cityId != null) {
            val newFilter = _uiState.value.localFilter.copy(selectedCityId = cityId)
            val cityCameraPosition = cameraPositionForCity(city)
            _uiState.value = _uiState.value.copy(
                localFilter = newFilter,
                selectedCity = city,
                mapState = _uiState.value.mapState.copy(
                    selectedParkId = null,
                    cameraPosition = cityCameraPosition
                )
            )
            viewModelScope.launch {
                parksFilterDataStore.saveFilter(newFilter)
            }
            recalculateFilteredParks()
        }
    }

    override fun onClearCityFilter() {
        logger.d(TAG, "onClearCityFilter")
        val newFilter = _uiState.value.localFilter.copy(selectedCityId = null)
        _uiState.value = _uiState.value.copy(
            localFilter = newFilter,
            selectedCity = null,
            mapState = _uiState.value.mapState.copy(
                selectedParkId = null
            )
        )
        viewModelScope.launch {
            parksFilterDataStore.saveFilter(newFilter)
        }
        recalculateFilteredParks()
    }

    override fun onTabSelected(tab: ParksTab) {
        logger.d(TAG, "onTabSelected: $tab")
        selectedTab.value = tab
    }

    override fun onMapEvent(event: MapEvent) {
        if (shouldIgnoreMapEvent(event)) return

        logger.d(TAG, "onMapEvent: $event")
        when (event) {
            is MapEvent.SelectPark -> updateMapState { it.copy(selectedParkId = event.parkId) }
            is MapEvent.ClearSelection -> updateMapState { it.copy(selectedParkId = null) }
            is MapEvent.ClusterClick -> handleClusterClick(event)
            is MapEvent.CenterOnUser -> handleCenterOnUser()
            is MapEvent.OnLocationPermissionResult -> handleLocationPermissionResult(event)
            is MapEvent.OnCameraIdle -> handleCameraIdle(event)
            is MapEvent.OnMapLoadFailed -> handleMapLoadFailed(event)
            is MapEvent.OnMapReady -> handleMapReady()
        }
    }

    private fun shouldIgnoreMapEvent(event: MapEvent): Boolean {
        return event is MapEvent.OnLocationPermissionResult &&
            _uiState.value.mapState.locationPermissionGranted == event.granted
    }

    private fun updateMapState(transform: (MapUiState) -> MapUiState) {
        _uiState.value = _uiState.value.copy(mapState = transform(_uiState.value.mapState))
    }

    private fun handleClusterClick(event: MapEvent.ClusterClick) {
        updateMapState {
            it.copy(
                cameraPosition = MapCameraPosition(
                    target = event.target,
                    zoom = event.expansionZoom.toDouble()
                )
            )
        }
    }

    private fun handleCenterOnUser() {
        when {
            _uiState.value.mapState.isLoadingLocation -> {
                logger.d(TAG, "Повторный запрос геолокации игнорируется: уже идёт загрузка")
            }

            !_uiState.value.mapState.locationPermissionGranted -> {
                userNotifier.handleError(
                    AppError.LocationFailed(
                        message = "Нет разрешения на геолокацию",
                        cause = SecurityException("Location permission is not granted")
                    )
                )
            }

            else -> startCenterOnUserFlow()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun startCenterOnUserFlow() {
        updateMapState { it.copy(isLoadingLocation = true) }
        viewModelScope.launch {
            try {
                val settingsResult = locationService.checkLocationSettings()
                settingsResult.fold(
                    onSuccess = ::handleLocationSettingsResult,
                    onFailure = { error ->
                        logger.e(TAG, "Ошибка проверки location settings", error)
                        getCurrentLocationAndUpdate()
                    }
                )
            } catch (error: Exception) {
                logger.e(TAG, "Неожиданная ошибка в flow геолокации", error)
                userNotifier.handleError(
                    AppError.LocationFailed(
                        message = error.message ?: "Не удалось получить геолокацию",
                        cause = error
                    )
                )
                updateMapState { it.copy(isLoadingLocation = false) }
            }
        }
    }

    private fun handleLocationSettingsResult(checkResult: LocationSettingsCheckResult) {
        when (checkResult) {
            is LocationSettingsCheckResult.SettingsOk -> getCurrentLocationAndUpdate()
            is LocationSettingsCheckResult.NeedsResolution -> {
                logger.d(TAG, "Location settings need resolution")
                updateMapState { it.copy(isLoadingLocation = false) }
                viewModelScope.launch {
                    _events.emit(ParksRootEvent.ResolveLocationSettings(checkResult.intentSender))
                }
            }

            is LocationSettingsCheckResult.SettingsDisabled -> {
                logger.d(TAG, "Location settings disabled")
                userNotifier.handleError(
                    AppError.LocationDisabled(message = "Геолокация устройства отключена")
                )
                updateMapState { it.copy(isLoadingLocation = false) }
            }
        }
    }

    private fun handleLocationPermissionResult(event: MapEvent.OnLocationPermissionResult) {
        updateMapState { it.copy(locationPermissionGranted = event.granted) }
    }

    private fun handleCameraIdle(event: MapEvent.OnCameraIdle) {
        updateMapState { it.copy(cameraPosition = event.position) }
    }

    private fun handleMapLoadFailed(event: MapEvent.OnMapLoadFailed) {
        logger.e(TAG, "Ошибка загрузки карты: ${event.message}")
        userNotifier.handleError(AppError.Generic(message = event.message))
    }

    private fun handleMapReady() {
        updateMapState { it.copy(isMapReady = true) }
    }

    private fun getCurrentLocationAndUpdate() {
        viewModelScope.launch {
            locationService.getCurrentLocation().fold(
                onSuccess = { coordinates ->
                    _uiState.value = _uiState.value.copy(
                        mapState = _uiState.value.mapState.copy(
                            userLocation = UiCoordinates(
                                latitude = coordinates.latitude,
                                longitude = coordinates.longitude
                            ),
                            cameraPosition = MapCameraPosition(
                                target = UiCoordinates(
                                    latitude = coordinates.latitude,
                                    longitude = coordinates.longitude
                                ),
                                zoom = USER_LOCATION_CAMERA_ZOOM
                            ),
                            isLoadingLocation = false,
                            isFollowingUser = false
                        )
                    )
                },
                onFailure = { error ->
                    logger.e(TAG, "Ошибка получения геолокации", error)
                    userNotifier.handleError(
                        AppError.LocationFailed(
                            message = error.message ?: "Не удалось получить геолокацию",
                            cause = error
                        )
                    )
                    _uiState.value = _uiState.value.copy(
                        mapState = _uiState.value.mapState.copy(isLoadingLocation = false)
                    )
                }
            )
        }
    }

    private fun requestPermission() {
        logger.d(TAG, "Запрос разрешений на местоположение")
        permissionLauncher?.invoke(
            mapOf(
                Manifest.permission.ACCESS_FINE_LOCATION to true,
                Manifest.permission.ACCESS_COARSE_LOCATION to true
            )
        )
    }

    private fun handlePermissionGranted() {
        logger.d(TAG, "Обработка полученного разрешения")
        _uiState.value = _uiState.value.copy(isGettingLocation = true)
        viewModelScope.launch {
            try {
                logger.d(TAG, "Вызов createParkLocationHandler")
                val result = createParkLocationHandler()
                logger.d(TAG, "Получен результат от createParkLocationHandler")
                result.fold(
                    onSuccess = { draft ->
                        logger.d(TAG, "Создание черновика площадки: $draft")
                        _events.emit(ParksRootEvent.NavigateToCreatePark(draft))
                    },
                    onFailure = {
                        logger.d(
                            TAG,
                            "Ошибка createParkLocationHandler, используем пустой черновик"
                        )
                        _events.emit(ParksRootEvent.NavigateToCreatePark(NewParkDraft.EMPTY))
                    }
                )
            } finally {
                _uiState.value = _uiState.value.copy(isGettingLocation = false)
            }
        }
    }
}
