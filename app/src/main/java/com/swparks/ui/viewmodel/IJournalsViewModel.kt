package com.swparks.ui.viewmodel

import com.swparks.ui.state.JournalsUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс для JournalsViewModel.
 *
 * Создан для возможности тестирования UI компонентов Compose без бизнес-логики.
 * Позволяет создавать mock/fake реализации для UI тестов.
 */
interface IJournalsViewModel {
    /**
     * Состояние UI экрана.
     */
    val uiState: StateFlow<JournalsUiState>

    /**
     * Индикатор обновления (pull-to-refresh).
     */
    val isRefreshing: StateFlow<Boolean>

    /**
     * Повторить загрузку при ошибке.
     */
    fun retry()

    /**
     * Загрузить дневники с сервера.
     */
    fun loadJournals()
}
