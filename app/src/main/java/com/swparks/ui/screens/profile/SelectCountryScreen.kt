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
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }

    val allCountries =
        remember(state.countries) {
            state.countries.map { it.name }
        }

    val filteredItems =
        remember(searchQuery, allCountries) {
            if (searchQuery.isEmpty()) {
                allCountries
            } else {
                allCountries.filter { it.contains(searchQuery, ignoreCase = true) }
            }
        }

    val isEmpty = filteredItems.isEmpty() && searchQuery.isNotEmpty()

    ItemListScreen(
        state =
            ItemListUiState(
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
            val feedback = LocationFeedback.createCountry(context)
            sendLocationFeedback(context, feedback)
        },
        onBackClick = onBackClick
    )
}
