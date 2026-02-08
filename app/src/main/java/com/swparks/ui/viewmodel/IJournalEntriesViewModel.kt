package com.swparks.ui.viewmodel

import com.swparks.ui.state.JournalEntriesUiState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс для JournalEntriesViewModel.
 *
 * Создан для возможности тестирования UI компонентов Compose без бизнес-логики.
 * Позволяет создавать mock/fake реализации для UI тестов.
 */
interface IJournalEntriesViewModel {
    /**
     * Состояние UI экрана.
     */
    val uiState: StateFlow<JournalEntriesUiState>

    /**
     * Индикатор обновления (pull-to-refresh).
     */
    val isRefreshing: StateFlow<Boolean>

    /**
     * Индикатор удаления записи.
     */
    val isDeleting: StateFlow<Boolean>

    /**
     * Поток событий UI (например, для Snackbar).
     */
    val events: SharedFlow<JournalEntriesEvent>

    /**
     * Загрузить записи с сервера.
     */
    fun loadEntries()

    /**
     * Повторить загрузку при ошибке.
     */
    fun retry()

    /**
     * Удалить запись из дневника.
     *
     * @param entryId Идентификатор записи
     */
    fun deleteEntry(entryId: Long)

    /**
     * Проверить, можно ли удалить запись.
     *
     * Первую запись (с минимальным id) нельзя удалить.
     *
     * @param entryId Идентификатор записи
     * @return true если удаление разрешено, false если это первая запись
     */
    suspend fun canDeleteEntry(entryId: Long): Boolean
}

/**
 * События UI для экрана списка записей в дневнике.
 */
sealed interface JournalEntriesEvent {
    /**
     * Показать Snackbar с сообщением.
     *
     * @property message Текст сообщения
     */
    data class ShowSnackbar(val message: String) : JournalEntriesEvent
}
