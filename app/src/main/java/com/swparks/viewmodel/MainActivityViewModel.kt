package com.swparks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.swparks.data.preferences.AppSettingsDataStore
import com.swparks.domain.model.AppTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STATE_TIMEOUT_MS = 5000L

/**
 * ViewModel для управления состоянием MainActivity (тема и динамические цвета).
 *
 * Читает настройки темы и динамических цветов из AppSettingsDataStore.
 *
 * @property dataStore DataStore для настроек приложения
 */
class MainActivityViewModel(
    private val dataStore: AppSettingsDataStore,
) : ViewModel() {
    companion object {
        /**
         * Factory для создания MainActivityViewModel. Используется для ручного DI вместо Hilt.
         *
         * @param dataStore DataStore для настроек приложения
         * @return Factory для создания ViewModel
         */
        fun factory(dataStore: AppSettingsDataStore): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { MainActivityViewModel(dataStore) }
            }
    }

    /** Тема приложения из DataStore */
    val theme: StateFlow<AppTheme> = dataStore.theme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MS),
            initialValue = AppTheme.SYSTEM,
        )

    /** Использование динамических цветов из DataStore */
    val useDynamicColors: StateFlow<Boolean> = dataStore.useDynamicColors
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MS),
            initialValue = false,
        )

    /**
     * Обновляет тему приложения.
     *
     * @param theme Новая тема приложения
     */
    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            dataStore.setTheme(theme)
        }
    }

    /**
     * Обновляет настройку использования динамических цветов.
     *
     * @param useDynamicColors Использовать динамические цвета
     */
    fun updateDynamicColors(useDynamicColors: Boolean) {
        viewModelScope.launch {
            dataStore.setUseDynamicColors(useDynamicColors)
        }
    }
}
