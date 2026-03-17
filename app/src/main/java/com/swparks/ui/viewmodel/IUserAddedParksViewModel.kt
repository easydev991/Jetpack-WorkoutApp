package com.swparks.ui.viewmodel

import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс для UserAddedParksViewModel.
 *
 * Нужен для тестирования Compose UI без бизнес-логики.
 */
interface IUserAddedParksViewModel {
    /**
     * Состояние UI экрана добавленных площадок.
     */
    val uiState: StateFlow<UserAddedParksUiState>

    /**
     * Индикатор обновления (pull-to-refresh).
     */
    val isRefreshing: StateFlow<Boolean>

    /**
     * Принудительное обновление списка добавленных площадок.
     */
    fun refresh()
}
