package com.swparks.ui.state

import com.swparks.data.database.entity.DialogEntity

/**
 * UI State для экрана списка диалогов.
 */
sealed class DialogsUiState {
    /**
     * Первичная загрузка (показывается полный экран загрузки).
     */
    data object Loading : DialogsUiState()

    /**
     * Контент с списком диалогов.
     *
     * @param dialogs Список диалогов
     */
    data class Success(
        val dialogs: List<DialogEntity>
    ) : DialogsUiState()

    /**
     * Ошибка загрузки с возможностью повтора.
     *
     * @param message Сообщение об ошибке для отображения пользователю
     */
    data class Error(
        val message: String
    ) : DialogsUiState()
}
