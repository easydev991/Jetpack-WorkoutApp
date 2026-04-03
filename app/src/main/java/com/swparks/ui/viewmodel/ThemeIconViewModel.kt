package com.swparks.ui.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.swparks.data.preferences.AppSettingsDataStore
import com.swparks.domain.model.AppIcon
import com.swparks.domain.model.AppTheme
import com.swparks.domain.usecase.IconManager
import com.swparks.ui.state.ThemeIconUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel для экрана Theme and Icon Screen. Управляет настройками темы и иконки приложения.
 *
 * Настройки загружаются из DataStore при инициализации, чтобы при каждом запуске приложения выбор
 * пользователя не сбрасывался.
 *
 * @property dataStore DataStore для сохранения настроек
 * @property iconManager Use Case для смены иконки приложения
 */
class ThemeIconViewModel(
    private val dataStore: AppSettingsDataStore,
    private val iconManager: IconManager
) : ViewModel(),
    IThemeIconViewModel {
    companion object {
        private const val STATE_TIMEOUT_MS = 5000L
        private const val TAG = "ThemeIconViewModel"

        /**
         * Factory для создания ThemeIconViewModel. Используется для ручного DI вместо Hilt.
         *
         * @param dataStore DataStore для настроек приложения
         * @param application Application контекст для создания IconManager
         * @return Factory для создания ViewModel
         */
        fun factory(
            dataStore: AppSettingsDataStore,
            application: Application
        ): ViewModelProvider.Factory =
            viewModelFactory {
                val iconManager = IconManager(application)
                initializer { ThemeIconViewModel(dataStore, iconManager) }
            }
    }

    /** UI State экрана. Объединяет поток настроек из DataStore. */
    override val uiState: StateFlow<ThemeIconUiState> =
        combine(
            dataStore.theme,
            dataStore.useDynamicColors,
            dataStore.icon
        ) { theme, useDynamicColors, icon ->
            ThemeIconUiState(
                theme = theme,
                useDynamicColors = useDynamicColors,
                icon = icon,
                isLoading = false
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MS),
            initialValue = ThemeIconUiState()
        )

    /**
     * Обновляет тему приложения. Сохраняет выбор в DataStore.
     *
     * @param theme Новая тема приложения
     */
    override fun updateTheme(theme: AppTheme) {
        viewModelScope.launch { dataStore.setTheme(theme) }
    }

    /**
     * Обновляет настройку использования динамических цветов. Сохраняет выбор в DataStore.
     *
     * @param useDynamicColors Использовать динамические цвета
     */
    override fun updateDynamicColors(useDynamicColors: Boolean) {
        viewModelScope.launch {
            dataStore.setUseDynamicColors(useDynamicColors)
            Log.d(TAG, "Динамические цвета: $useDynamicColors")
        }
    }

    /**
     * Обновляет иконку приложения. Сохраняет выбор в DataStore и применяет через PackageManager.
     *
     * @param icon Новая иконка приложения
     */
    override fun updateIcon(icon: AppIcon) {
        viewModelScope.launch {
            try {
                // Сохраняем выбор в DataStore
                dataStore.setIcon(icon)

                // Применяем иконку через PackageManager
                iconManager.changeIcon(icon)

                Log.d(TAG, "Иконка успешно изменена на ${icon.name}")
            } catch (e: SecurityException) {
                Log.e(TAG, "Ошибка безопасности при смене иконки", e)
                // Продолжаем работу, даже если смена иконки не удалась
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e(TAG, "Компонент иконки не найден", e)
                // Продолжаем работу, даже если смена иконки не удалась
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Неверный аргумент при смене иконки", e)
                // Продолжаем работу, даже если смена иконки не удалась
            }
        }
    }
}
