package com.swparks.ui.viewmodel

import com.swparks.domain.model.AppIcon
import com.swparks.domain.model.AppTheme
import com.swparks.ui.state.ThemeIconUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fake-реализация ThemeIconViewModel для UI тестов.
 *
 * Предоставляет простую реализацию интерфейса с возможностью установки состояния.
 * Используется в Compose UI тестах для проверки разных состояний экрана.
 */
class FakeThemeIconViewModel(
    override val uiState: StateFlow<ThemeIconUiState> = MutableStateFlow(ThemeIconUiState())
) : IThemeIconViewModel {

    /**
     * Функция-заглушка для обновления темы.
     * В тестах можно проверить, был ли вызван этот метод.
     */
    override fun updateTheme(theme: AppTheme) {
        // Заглушка - не делает ничего в тестах
    }

    /**
     * Функция-заглушка для обновления динамических цветов.
     * В тестах можно проверить, был ли вызван этот метод.
     */
    override fun updateDynamicColors(useDynamicColors: Boolean) {
        // Заглушка - не делает ничего в тестах
    }

    /**
     * Функция-заглушка для обновления иконки.
     * В тестах можно проверить, был ли вызван этот метод.
     */
    override fun updateIcon(icon: AppIcon) {
        // Заглушка - не делает ничего в тестах
    }
}
