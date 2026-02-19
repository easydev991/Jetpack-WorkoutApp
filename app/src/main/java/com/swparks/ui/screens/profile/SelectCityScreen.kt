package com.swparks.ui.screens.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.swparks.ui.screens.settings.ItemListMode
import com.swparks.ui.screens.settings.ItemListScreen
import com.swparks.ui.state.ItemListUiState
import com.swparks.ui.viewmodel.IEditProfileViewModel

/**
 * Экран выбора города (Stateful wrapper).
 *
 * Управляет локальным состоянием поиска и передает данные в ItemListScreen.
 *
 * @param viewModel ViewModel с данными (Single Source of Truth)
 * @param onBackClick Колбэк при нажатии назад
 */
@Composable
fun SelectCityScreen(
    viewModel: IEditProfileViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    // Локальное состояние для поиска
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Города берутся из state.cities (уже отфильтрованы по выбранной стране в ViewModel)
    val allCities = remember(state.cities) {
        state.cities.map { it.name }
    }

    // Фильтрация
    val filteredItems = remember(searchQuery, allCities) {
        if (searchQuery.isEmpty()) allCities
        else allCities.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    val isEmpty = filteredItems.isEmpty() && searchQuery.isNotEmpty()

    ItemListScreen(
        state = ItemListUiState(
            mode = ItemListMode.CITY,
            items = filteredItems,
            selectedItem = state.selectedCity?.name,
            searchQuery = searchQuery,
            isEmpty = isEmpty
        ),
        onSearchQueryChange = { searchQuery = it },
        onItemSelected = { cityName ->
            viewModel.onCitySelected(cityName)
            onBackClick()
        },
        onContactUs = {
            // Note: отправка feedback будет реализована позже
        },
        onBackClick = onBackClick
    )
}
