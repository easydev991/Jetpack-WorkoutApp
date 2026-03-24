package com.swparks.ui.viewmodel

import android.Manifest
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.swparks.JetpackWorkoutApplication
import com.swparks.data.model.NewParkDraft
import com.swparks.data.model.Park
import com.swparks.data.model.ParkFilter
import com.swparks.data.model.ParkSize
import com.swparks.data.model.ParkType
import com.swparks.data.preferences.ParksFilterDataStore
import com.swparks.domain.repository.CountriesRepository
import com.swparks.domain.usecase.ICreateParkLocationHandler
import com.swparks.domain.usecase.IFilterParksUseCase
import com.swparks.util.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ParksRootViewModel(
    private val createParkLocationHandler: ICreateParkLocationHandler,
    private val logger: Logger,
    private val filterParksUseCase: IFilterParksUseCase,
    private val parksFilterDataStore: ParksFilterDataStore,
    private val countriesRepository: CountriesRepository
) : ViewModel(), IParksRootViewModel {

    override val parksFilter: StateFlow<ParkFilter> = parksFilterDataStore.filter
        .stateIn(viewModelScope, SharingStarted.Eagerly, ParkFilter())

    private val _uiState = MutableStateFlow(ParksRootUiState(localFilter = parksFilter.value))
    override val uiState: StateFlow<ParksRootUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ParksRootEvent>()
    override val events: SharedFlow<ParksRootEvent> = _events.asSharedFlow()

    override var permissionLauncher: ((Map<String, Boolean>) -> Unit)? = null
    override var openSettingsLauncher: ((Intent) -> Unit)? = null

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
        loadCities()
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
            } catch (e: Exception) {
                logger.e(TAG, "Ошибка загрузки городов", e)
                _uiState.value = _uiState.value.copy(isLoadingCities = false)
            }
        }
    }

    private fun restoreSelectedCity(cityId: Int?) {
        if (cityId == null) return
        viewModelScope.launch {
            val city = countriesRepository.getCityById(cityId.toString())
            if (city != null) {
                _uiState.value = _uiState.value.copy(selectedCity = city)
                logger.d(TAG, "Восстановлен выбранный город: ${city.name}")
            }
        }
    }

    private fun recalculateFilteredParks() {
        if (allParks.isEmpty()) return
        val filtered = filterParksUseCase(allParks, _uiState.value.localFilter)
        _uiState.value = _uiState.value.copy(filteredParks = filtered)
    }

    companion object {
        private const val TAG = "ParksRootViewModel"

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
                    countriesRepository = container.countriesRepository
                )
            }
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
            _uiState.value = _uiState.value.copy(
                localFilter = newFilter,
                selectedCity = city
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
            selectedCity = null
        )
        viewModelScope.launch {
            parksFilterDataStore.saveFilter(newFilter)
        }
        recalculateFilteredParks()
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
