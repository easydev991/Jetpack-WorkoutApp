package com.swparks.ui.viewmodel

import android.content.Intent
import com.swparks.data.model.City
import com.swparks.data.model.NewParkDraft
import com.swparks.data.model.Park
import com.swparks.data.model.ParkFilter
import com.swparks.data.model.ParkSize
import com.swparks.data.model.ParkType
import com.swparks.ui.screens.settings.ItemListMode
import com.swparks.ui.state.ItemListUiState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface IParksRootViewModel {
    val uiState: StateFlow<ParksRootUiState>
    val events: SharedFlow<ParksRootEvent>

    var permissionLauncher: ((Map<String, Boolean>) -> Unit)?
    var openSettingsLauncher: ((Intent) -> Unit)?

    fun onPermissionGranted()
    fun onPermissionDenied(shouldShowRationale: Boolean)
    fun onPermissionResult(permissions: Map<String, Boolean>)
    fun onDismissDialog()
    fun onConfirmDialog()
    fun onOpenSettings(intent: Intent)

    val parksFilter: StateFlow<ParkFilter>
    fun onLocalFilterChange(filter: ParkFilter)
    fun onFilterToggleSize(size: ParkSize)
    fun onFilterToggleType(type: ParkType)
    fun onFilterReset()
    fun onFilterApply()
    fun onShowFilterDialog()
    fun onDismissFilterDialog()

    fun onSelectCityClick()
    fun onCitySearchQueryChange(query: String)
    fun onCitySelected(cityName: String)
    fun onClearCityFilter()

    val cityNames: List<String>
}

data class ParksRootUiState(
    val showPermissionDialog: Boolean = false,
    val permissionDialogCause: PermissionDialogCause? = null,
    val isGettingLocation: Boolean = false,
    val showFilterDialog: Boolean = false,
    val localFilter: ParkFilter = ParkFilter(),
    val isLoadingFilter: Boolean = true,
    val selectedCity: City? = null,
    val cities: List<City> = emptyList(),
    val isLoadingCities: Boolean = false,
    val citySearchQuery: String = "",
    val filteredParks: List<Park> = emptyList()
)

sealed class ParksRootEvent {
    data class NavigateToCreatePark(val draft: NewParkDraft) : ParksRootEvent()
    data object OpenSettings : ParksRootEvent()
}

enum class PermissionDialogCause {
    DENIED,
    FOREVER_DENIED
}

fun ParksRootUiState.toItemListUiState(): ItemListUiState = ItemListUiState(
    mode = ItemListMode.CITY,
    items = cities
        .filter { it.name.contains(citySearchQuery, ignoreCase = true) }
        .map { it.name },
    selectedItem = null,
    searchQuery = citySearchQuery,
    isEmpty = false
)