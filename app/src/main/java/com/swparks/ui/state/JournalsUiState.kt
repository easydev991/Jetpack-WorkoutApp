package com.swparks.ui.state

import com.swparks.domain.model.Journal

/**
 * UI State для экрана списка дневников.
 */
sealed class JournalsUiState {
    /**
     * Первичная загрузка (показывается полный экран загрузки).
     */
    data object InitialLoading : JournalsUiState()

    /**
     * Контент с списком дневников.
     *
     * @param journals Список дневников
     * @param isRefreshing Признак обновления данных (pull-to-refresh)
     * @param isSavingJournalSettings Признак сохранения настроек дневника
     */
    data class Content(
        val journals: List<Journal>,
        val isRefreshing: Boolean = false,
        val isSavingJournalSettings: Boolean = false
    ) : JournalsUiState()

    /**
     * Ошибка загрузки с возможностью повтора.
     *
     * @param message Сообщение об ошибке для отображения пользователю
     */
    data class Error(val message: String) : JournalsUiState()
}
