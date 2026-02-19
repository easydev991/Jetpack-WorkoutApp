package com.swparks.ui.screens.settings

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.swparks.R
import com.swparks.ui.ds.ButtonConfig
import com.swparks.ui.ds.CheckmarkRowView
import com.swparks.ui.ds.FormCardContainer
import com.swparks.ui.ds.SWButton
import com.swparks.ui.ds.SWButtonMode
import com.swparks.ui.ds.SWButtonSize
import com.swparks.ui.state.ItemListUiState
import com.swparks.ui.theme.JetpackWorkoutAppTheme

/**
 * Stateless экран для выбора элемента из списка.
 *
 * @param state Полное состояние экрана (items уже отфильтрованы)
 * @param onSearchQueryChange Колбэк для изменения поискового запроса
 * @param onItemSelected Колбэк при выборе элемента
 * @param onContactUs Колбэк при нажатии "Связаться с нами"
 * @param onBackClick Колбэк при нажатии назад
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemListScreen(
    state: ItemListUiState,
    onSearchQueryChange: (String) -> Unit,
    onItemSelected: (String) -> Unit,
    onContactUs: () -> Unit,
    onBackClick: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            ItemListTopAppBar(
                titleResId = state.mode.titleResId,
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        ItemListContent(
            state = state,
            paddingValues = paddingValues,
            onSearchQueryChange = onSearchQueryChange,
            onItemSelected = { item ->
                keyboardController?.hide()
                onItemSelected(item)
            },
            onContactUs = onContactUs
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemListTopAppBar(
    titleResId: Int,
    onBackClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(text = stringResource(titleResId))
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        }
    )
}

@Composable
private fun ItemListContent(
    state: ItemListUiState,
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    onSearchQueryChange: (String) -> Unit,
    onItemSelected: (String) -> Unit,
    onContactUs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = dimensionResource(R.dimen.spacing_regular))
    ) {
        SearchBar(
            query = state.searchQuery,
            onQueryChange = onSearchQueryChange
        )

        Spacer(modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_small)))

        FormCardContainer {
            if (state.isEmpty) {
                EmptyStateView(
                    mode = state.mode,
                    onContactUs = onContactUs
                )
            } else {
                ItemsList(
                    items = state.items,
                    selectedItem = state.selectedItem,
                    onItemSelected = onItemSelected
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.search)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null
            )
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        singleLine = true
    )
}

@Composable
private fun ItemsList(
    items: List<String>,
    selectedItem: String?,
    onItemSelected: (String) -> Unit
) {
    LazyColumn {
        itemsIndexed(items, key = { _, item -> item }) { index, item ->
            val isSelected = item == selectedItem

            CheckmarkRowView(
                text = item,
                isChecked = isSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(R.dimen.spacing_small),
                        vertical = dimensionResource(R.dimen.spacing_xsmall)
                    )
                    .then(
                        if (isSelected) {
                            Modifier
                        } else {
                            Modifier.clickable { onItemSelected(item) }
                        }
                    )
            )

            if (index != items.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}

/**
 * Empty state view для отображения, когда поиск не дал результатов.
 */
@Composable
private fun EmptyStateView(
    mode: ItemListMode,
    onContactUs: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.spacing_small)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(mode.helpMessageResId),
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SWButton(
            config = ButtonConfig(
                size = SWButtonSize.SMALL,
                mode = SWButtonMode.TINTED,
                text = stringResource(R.string.contact_us),
                onClick = onContactUs
            )
        )
    }
}

// MARK: - Previews

@Preview(showBackground = true, locale = "ru")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
fun ItemListScreenCountryPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            ItemListScreen(
                state = ItemListUiState(
                    mode = ItemListMode.COUNTRY,
                    items = listOf("Россия", "США", "Франция", "Германия"),
                    selectedItem = "Россия",
                    searchQuery = "",
                    isEmpty = false
                ),
                onSearchQueryChange = {},
                onItemSelected = {},
                onContactUs = {},
                onBackClick = {}
            )
        }
    }
}

@Preview(showBackground = true, locale = "ru")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
fun ItemListScreenCityPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            ItemListScreen(
                state = ItemListUiState(
                    mode = ItemListMode.CITY,
                    items = listOf("Москва", "Санкт-Петербург", "Казань"),
                    selectedItem = "Москва",
                    searchQuery = "",
                    isEmpty = false
                ),
                onSearchQueryChange = {},
                onItemSelected = {},
                onContactUs = {},
                onBackClick = {}
            )
        }
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun ItemListScreenEmptyPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            ItemListScreen(
                state = ItemListUiState(
                    mode = ItemListMode.COUNTRY,
                    items = emptyList(),
                    selectedItem = null,
                    searchQuery = "Несуществующая страна",
                    isEmpty = true
                ),
                onSearchQueryChange = {},
                onItemSelected = {},
                onContactUs = {},
                onBackClick = {}
            )
        }
    }
}
