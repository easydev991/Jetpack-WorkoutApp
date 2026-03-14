package com.swparks.ui.viewmodel

import com.swparks.ui.state.BlacklistAction
import com.swparks.ui.state.BlacklistUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс для BlacklistViewModel.
 *
 * Создан для возможности тестирования UI компонентов Compose без бизнес-логики.
 * Позволяет создавать mock/fake реализации для UI тестов.
 */
interface IBlacklistViewModel {
    /**
     * Состояние UI экрана.
     */
    val uiState: StateFlow<BlacklistUiState>

    /**
     * Обработка действий пользователя.
     *
     * @param action Действие пользователя
     */
    fun onAction(action: BlacklistAction)
}
