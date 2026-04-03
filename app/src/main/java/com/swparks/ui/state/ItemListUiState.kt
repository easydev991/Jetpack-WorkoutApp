package com.swparks.ui.state

import com.swparks.ui.screens.settings.ItemListMode

/**
 * Состояние экрана ItemListScreen.
 *
 * Note: filteredItems и isEmpty вычисляются в Composable, а не в data class,
 * чтобы следовать принципам State Hoisting и разделения ответственности.
 */
data class ItemListUiState(
    val mode: ItemListMode,
    // Уже отфильтрованный список
    val items: List<String>,
    val selectedItem: String?,
    val searchQuery: String = "",
    // Вычисляется в Composable
    val isEmpty: Boolean = false
)
