package com.swparks.ui.viewmodel

import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс для UserTrainingParksViewModel.
 *
 * Создан для возможности тестирования UI компонентов Compose без бизнес-логики.
 * Позволяет создавать mock/fake реализации для UI тестов.
 */
interface IUserTrainingParksViewModel {
    /**
     * Состояние UI экрана.
     */
    val uiState: StateFlow<UserTrainingParksUiState>

    /**
     * Индикатор обновления (pull-to-refresh).
     */
    val isRefreshing: StateFlow<Boolean>

    /**
     * Обновляет список площадок для pull-to-refresh.
     */
    fun refreshParks()
}
