package com.swparks.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Верхнеуровневое назначение (вкладка)
 *
 * @param route Маршрут навигации
 * @param selectedIcon Иконка, которая отображается, когда вкладка выбрана
 * @param unselectedIcon Иконка, которая отображается, когда вкладка не выбрана
 * @param iconTextId Текст для иконки (подпись под иконкой)
 * @param titleTextId Заголовок для TopAppBar
 */
data class TopLevelDestination(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @param:StringRes val iconTextId: Int,
    @param:StringRes val titleTextId: Int
)
