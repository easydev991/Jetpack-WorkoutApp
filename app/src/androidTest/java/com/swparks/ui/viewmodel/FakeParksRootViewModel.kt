package com.swparks.ui.viewmodel

import android.Manifest
import android.content.Intent
import com.swparks.data.model.City
import com.swparks.data.model.NewParkDraft
import com.swparks.data.model.Park
import com.swparks.data.model.ParkFilter
import com.swparks.data.model.ParkSize
import com.swparks.data.model.ParkType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

class FakeParksRootViewModel : IParksRootViewModel {

    private val _uiState = MutableStateFlow(ParksRootUiState())
    override val uiState: StateFlow<ParksRootUiState> = _uiState

    private val _events = MutableSharedFlow<ParksRootEvent>()
    override val events: SharedFlow<ParksRootEvent> = _events.asSharedFlow()

    private val _parksFilter = MutableStateFlow(ParkFilter())
    override val parksFilter: StateFlow<ParkFilter> = _parksFilter

    override var permissionLauncher: ((Map<String, Boolean>) -> Unit)? = null
    override var openSettingsLauncher: ((Intent) -> Unit)? = null

    var nextDraft: NewParkDraft = NewParkDraft.EMPTY
    private val _capturedEvents = mutableListOf<ParksRootEvent>()
    val capturedEvents: List<ParksRootEvent> = _capturedEvents

    private var shouldBypassPermission = false
    private var shouldShowDialogOnPermissionDenied = false
    private var dialogCauseToShow: PermissionDialogCause? = null

    fun reset() {
        _uiState.value = ParksRootUiState()
        _capturedEvents.clear()
        nextDraft = NewParkDraft.EMPTY
        shouldBypassPermission = false
        shouldShowDialogOnPermissionDenied = false
        dialogCauseToShow = null
    }

    fun setBypassPermission(bypass: Boolean) {
        shouldBypassPermission = bypass
    }

    fun setShowDialogOnPermissionDenied(cause: PermissionDialogCause) {
        shouldShowDialogOnPermissionDenied = true
        dialogCauseToShow = cause
    }

    override fun onPermissionGranted() {
        _capturedEvents.add(ParksRootEvent.NavigateToCreatePark(nextDraft))
    }

    override fun onPermissionDenied(shouldShowRationale: Boolean) {
        if (shouldShowDialogOnPermissionDenied) {
            _uiState.value = ParksRootUiState(
                showPermissionDialog = true,
                permissionDialogCause = dialogCauseToShow
            )
        } else if (!shouldShowRationale) {
            // If no dialog should be shown and no rationale needed, simulate requesting permission
            permissionLauncher?.invoke(
                mapOf(
                    Manifest.permission.ACCESS_FINE_LOCATION to true,
                    Manifest.permission.ACCESS_COARSE_LOCATION to true
                )
            )
        }
    }

    override fun onPermissionResult(permissions: Map<String, Boolean>) {
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val isGranted = fineLocationGranted || coarseLocationGranted

        if (isGranted) {
            _capturedEvents.add(ParksRootEvent.NavigateToCreatePark(nextDraft))
        } else {
            _uiState.value = ParksRootUiState(
                showPermissionDialog = true,
                permissionDialogCause = PermissionDialogCause.FOREVER_DENIED
            )
        }
    }

    override fun onDismissDialog() {
        _uiState.value = _uiState.value.copy(
            showPermissionDialog = false,
            permissionDialogCause = null
        )
    }

    override fun onConfirmDialog() {
        _uiState.value = _uiState.value.copy(
            showPermissionDialog = false,
            permissionDialogCause = null
        )
        permissionLauncher?.invoke(
            mapOf(
                Manifest.permission.ACCESS_FINE_LOCATION to true,
                Manifest.permission.ACCESS_COARSE_LOCATION to true
            )
        )
    }

    override fun onOpenSettings(intent: Intent) {
        _uiState.value = _uiState.value.copy(
            showPermissionDialog = false,
            permissionDialogCause = null
        )
    }

    override fun onLocalFilterChange(filter: ParkFilter) {
        _uiState.value = _uiState.value.copy(localFilter = filter)
    }

    override fun onFilterToggleSize(size: ParkSize) {
        val current = _uiState.value.localFilter
        val newFilter = if (current.sizes.contains(size)) {
            if (current.sizes.size > 1) {
                current.copy(sizes = current.sizes - size)
            } else current
        } else {
            current.copy(sizes = current.sizes + size)
        }
        _uiState.value = _uiState.value.copy(localFilter = newFilter)
    }

    override fun onFilterToggleType(type: ParkType) {
        val current = _uiState.value.localFilter
        val newFilter = if (current.types.contains(type)) {
            if (current.types.size > 1) {
                current.copy(types = current.types - type)
            } else current
        } else {
            current.copy(types = current.types + type)
        }
        _uiState.value = _uiState.value.copy(localFilter = newFilter)
    }

    override fun onFilterReset() {
        _uiState.value = _uiState.value.copy(localFilter = ParkFilter())
    }

    override fun onFilterApply() {
        _parksFilter.value = _uiState.value.localFilter
    }

    override fun onShowFilterDialog() {
        _uiState.value = _uiState.value.copy(
            showFilterDialog = true,
            localFilter = _parksFilter.value
        )
    }

    override fun onDismissFilterDialog() {
        _uiState.value = _uiState.value.copy(showFilterDialog = false)
    }

    override fun onSelectCityClick() {
    }

    override fun onCitySearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(citySearchQuery = query)
    }

    override fun onCitySelected(cityName: String) {
        val city = _uiState.value.cities.find { it.name == cityName }
        val cityId = city?.id?.toIntOrNull()
        if (city != null && cityId != null) {
            _uiState.value = _uiState.value.copy(
                localFilter = _uiState.value.localFilter.copy(selectedCityId = cityId),
                selectedCity = city
            )
        }
    }

    override fun onClearCityFilter() {
        _uiState.value = _uiState.value.copy(
            localFilter = _uiState.value.localFilter.copy(selectedCityId = null),
            selectedCity = null
        )
    }

    override fun updateParks(parks: List<Park>) {
        _uiState.value = _uiState.value.copy(
            filteredParks = parks,
            hasParks = parks.isNotEmpty()
        )
    }

    fun setParksState(hasParks: Boolean, filteredParks: List<Park>) {
        _uiState.value = _uiState.value.copy(
            hasParks = hasParks,
            filteredParks = filteredParks
        )
    }

    fun setSelectedCity(city: City?) {
        _uiState.value = _uiState.value.copy(selectedCity = city)
    }

    override val cityNames: List<String>
        get() = _uiState.value.cities
            .filter { it.name.contains(_uiState.value.citySearchQuery, ignoreCase = true) }
            .map { it.name }
}
