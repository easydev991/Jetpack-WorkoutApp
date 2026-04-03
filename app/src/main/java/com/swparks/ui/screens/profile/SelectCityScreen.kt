package com.swparks.ui.screens.profile

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
import com.swparks.ui.viewmodel.IEditProfileViewModel
import com.swparks.util.LocationFeedback

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
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }

    val allCities =
        remember(state.cities) {
            state.cities.map { it.name }
        }

    val filteredItems =
        remember(searchQuery, allCities) {
            if (searchQuery.isEmpty()) {
                allCities
            } else {
                allCities.filter { it.contains(searchQuery, ignoreCase = true) }
            }
        }

    val isEmpty = filteredItems.isEmpty() && searchQuery.isNotEmpty()

    ItemListScreen(
        state =
            ItemListUiState(
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
            val feedback = LocationFeedback.createCity(context)
            sendLocationFeedback(context, feedback)
        },
        onBackClick = onBackClick
    )
}
