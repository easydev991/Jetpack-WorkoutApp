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
 * Экран выбора страны (Stateful wrapper).
 *
 * Управляет локальным состоянием поиска и передает данные в ItemListScreen.
 *
 * @param viewModel ViewModel с данными (Single Source of Truth)
 * @param onBackClick Колбэк при нажатии назад
 */
@Composable
fun SelectCountryScreen(
    viewModel: IEditProfileViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    // Локальное состояние для поиска (не загрязняет EditProfileViewModel)
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Преобразуем страны в строки один раз при изменении списка
    val allCountries = remember(state.countries) {
        state.countries.map { it.name }
    }

    // Фильтрация происходит здесь (State Hoisting)
    val filteredItems = remember(searchQuery, allCountries) {
        if (searchQuery.isEmpty()) allCountries
        else allCountries.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    // Empty state только когда есть запрос, но нет результатов
    val isEmpty = filteredItems.isEmpty() && searchQuery.isNotEmpty()

    ItemListScreen(
        state = ItemListUiState(
            mode = ItemListMode.COUNTRY,
            items = filteredItems,
            selectedItem = state.selectedCountry?.name,
            searchQuery = searchQuery,
            isEmpty = isEmpty
        ),
        onSearchQueryChange = { searchQuery = it },
        onItemSelected = { countryName ->
            viewModel.onCountrySelected(countryName)
            onBackClick()
        },
        onContactUs = {
            // Note: отправка feedback будет реализована позже
        },
        onBackClick = onBackClick
    )
}
