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
 * Экран выбора страны для регистрации.
 *
 * Управляет локальным состоянием поиска и передает данные в ItemListScreen.
 *
 * @param viewModel ViewModel с данными регистрации
 * @param onBackClick Колбэк при нажатии назад
 */
@Composable
fun RegisterSelectCountryScreen(
    viewModel: IRegisterViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val countries by viewModel.countries.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }

    val allCountries = remember(countries) {
        countries.map { it.name }
    }

    val filteredItems = remember(searchQuery, allCountries) {
        if (searchQuery.isEmpty()) allCountries
        else allCountries.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    val isEmpty = filteredItems.isEmpty() && searchQuery.isNotEmpty()

    ItemListScreen(
        state = ItemListUiState(
            mode = ItemListMode.COUNTRY,
            items = filteredItems,
            selectedItem = selectedCountry?.name,
            searchQuery = searchQuery,
            isEmpty = isEmpty
        ),
        onSearchQueryChange = { searchQuery = it },
        onItemSelected = { countryName ->
            viewModel.onCountrySelectedByName(countryName)
            onBackClick()
        },
        onContactUs = {
            val feedback = LocationFeedback.createCountry(context)
            sendLocationFeedback(context, feedback)
        },
        onBackClick = onBackClick
    )
}
