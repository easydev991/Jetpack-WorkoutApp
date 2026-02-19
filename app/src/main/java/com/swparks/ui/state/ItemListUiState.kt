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
    val items: List<String>, // Уже отфильтрованный список
    val selectedItem: String?,
    val searchQuery: String = "",
    val isEmpty: Boolean = false // Вычисляется в Composable
)
