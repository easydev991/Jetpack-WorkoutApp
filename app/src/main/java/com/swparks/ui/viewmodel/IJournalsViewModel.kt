package com.swparks.ui.viewmodel

import com.swparks.ui.state.JournalsUiState
import kotlinx.coroutines.flow.SharedFlow
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
     * Индикатор удаления дневника.
     */
    val isDeleting: StateFlow<Boolean>

    /**
     * Поток событий UI (например, для Snackbar).
     */
    val events: SharedFlow<JournalsEvent>

    /**
     * Повторить загрузку при ошибке.
     */
    fun retry()

    /**
     * Загрузить дневники с сервера.
     */
    fun loadJournals()

    /**
     * Удалить дневник.
     *
     * @param journalId Идентификатор дневника
     */
    fun deleteJournal(journalId: Long)
}

/**
 * События UI для экрана списка дневников.
 */
sealed interface JournalsEvent {
    /**
     * Показать Snackbar с сообщением.
     *
     * @property message Текст сообщения
     */
    data class ShowSnackbar(val message: String) : JournalsEvent
}
