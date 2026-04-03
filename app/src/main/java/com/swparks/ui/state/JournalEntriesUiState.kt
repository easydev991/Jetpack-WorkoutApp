package com.swparks.ui.state

import com.swparks.domain.model.Journal
import com.swparks.domain.model.JournalEntry

/**
 * UI State для экрана списка записей в дневнике.
 */
sealed class JournalEntriesUiState {
    /**
     * Первичная загрузка (показывается полный экран загрузки).
     */
    data object InitialLoading : JournalEntriesUiState()

    /**
     * Контент с списком записей в дневнике.
     *
     * @param entries Список записей в дневнике
     * @param isRefreshing Признак обновления данных (pull-to-refresh)
     * @param firstEntryId Id первой записи (с минимальным id), которую нельзя удалить
     * @param canCreateEntry Признак возможности создания записей (для отображения FAB)
     * @param journal Текущий дневник для диалога настроек (null во время загрузки)
     * @param isSavingJournalSettings Флаг сохранения настроек дневника
     */
    data class Content(
        val entries: List<JournalEntry>,
        val isRefreshing: Boolean = false,
        val firstEntryId: Long? = null,
        val canCreateEntry: Boolean = false,
        val journal: Journal? = null,
        val isSavingJournalSettings: Boolean = false
    ) : JournalEntriesUiState()

    /**
     * Ошибка загрузки с возможностью повтора.
     *
     * @param message Сообщение об ошибке для отображения пользователю
     */
    data class Error(
        val message: String
    ) : JournalEntriesUiState()
}
