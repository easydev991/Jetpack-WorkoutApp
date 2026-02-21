package com.swparks.ui.screens.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.swparks.ui.screens.more.sendLocationFeedback
import com.swparks.ui.screens.settings.ItemListMode
import com.swparks.ui.screens.settings.ItemListScreen
import com.swparks.ui.state.ItemListUiState
import com.swparks.ui.viewmodel.IRegisterViewModel
import com.swparks.util.LocationFeedback

/**
 * Экран выбора города для регистрации.
 *
 * Управляет локальным состоянием поиска и передает данные в ItemListScreen.
 * Если страна не выбрана - показывает все города из всех стран.
 * Если страна выбрана - показывает города только этой страны.
 *
 * @param viewModel ViewModel с данными регистрации
 * @param onBackClick Колбэк при нажатии назад
 */
@Composable
fun RegisterSelectCityScreen(
    viewModel: IRegisterViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val selectedCity by viewModel.selectedCity.collectAsState()
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val cities by viewModel.cities.collectAsState()
    val allCities by viewModel.allCities.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Если страна выбрана - показываем города этой страны, иначе - все города
    val displayCities = remember(selectedCountry, cities, allCities) {
        if (selectedCountry != null) {
            cities.map { it.name }
        } else {
            allCities.map { it.name }
        }
    }

    val filteredItems = remember(searchQuery, displayCities) {
        if (searchQuery.isEmpty()) displayCities
        else displayCities.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    val isEmpty = filteredItems.isEmpty() && searchQuery.isNotEmpty()

    ItemListScreen(
        state = ItemListUiState(
            mode = ItemListMode.CITY,
            items = filteredItems,
            selectedItem = selectedCity?.name,
            searchQuery = searchQuery,
            isEmpty = isEmpty
        ),
        onSearchQueryChange = { searchQuery = it },
        onItemSelected = { cityName ->
            viewModel.onCitySelectedByName(cityName)
            onBackClick()
        },
        onContactUs = {
            val feedback = LocationFeedback.createCity(context)
            sendLocationFeedback(context, feedback)
        },
        onBackClick = onBackClick
    )
}
