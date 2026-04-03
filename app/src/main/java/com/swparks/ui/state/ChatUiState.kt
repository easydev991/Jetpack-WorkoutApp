package com.swparks.ui.state

import com.swparks.data.model.MessageResponse

/**
 * UI State для экрана чата (диалога).
 */
sealed class ChatUiState {
    /**
     * Первичная загрузка (показывается полный экран загрузки).
     */
    data object Loading : ChatUiState()

    /**
     * Контент с списком сообщений.
     *
     * @param messages Список сообщений
     */
    data class Success(
        val messages: List<MessageResponse>
    ) : ChatUiState()

    /**
     * Ошибка загрузки с возможностью повтора.
     *
     * @param message Сообщение об ошибке для отображения пользователю
     */
    data class Error(
        val message: String
    ) : ChatUiState()
}
