package com.swparks.ui.state

import com.swparks.domain.model.AppIcon
import com.swparks.domain.model.AppTheme

/**
 * UI State для экрана Theme and Icon Screen.
 *
 * @property theme Текущая выбранная тема приложения
 * @property useDynamicColors Использовать динамические цвета (true по умолчанию)
 * @property icon Текущая выбранная иконка приложения
 * @property isLoading Индикатор загрузки настроек
 */
data class ThemeIconUiState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val useDynamicColors: Boolean = true,
    val icon: AppIcon = AppIcon.DEFAULT,
    val isLoading: Boolean = false,
)
