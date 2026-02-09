package com.swparks.ui.viewmodel

import com.swparks.domain.model.AppIcon
import com.swparks.domain.model.AppTheme
import com.swparks.ui.state.ThemeIconUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс для ThemeIconViewModel.
 *
 * Создан для возможности тестирования UI компонентов Compose без бизнес-логики.
 * Позволяет создавать mock/fake реализации для UI тестов.
 */
interface IThemeIconViewModel {
    /**
     * Состояние UI экрана (тема, динамические цвета, иконка).
     */
    val uiState: StateFlow<ThemeIconUiState>

    /**
     * Обновляет тему приложения.
     *
     * @param theme Новая тема приложения
     */
    fun updateTheme(theme: AppTheme)

    /**
     * Обновляет настройку использования динамических цветов.
     *
     * @param useDynamicColors Использовать динамические цвета
     */
    fun updateDynamicColors(useDynamicColors: Boolean)

    /**
     * Обновляет иконку приложения.
     *
     * @param icon Новая иконка приложения
     */
    fun updateIcon(icon: AppIcon)
}
