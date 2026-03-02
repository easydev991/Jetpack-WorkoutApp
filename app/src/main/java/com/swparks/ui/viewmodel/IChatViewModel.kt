package com.swparks.ui.viewmodel

import androidx.compose.runtime.MutableState
import com.swparks.ui.state.ChatEvent
import com.swparks.ui.state.ChatUiState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс для ChatViewModel.
 *
 * Создан для возможности тестирования UI компонентов Compose без бизнес-логики.
 * Позволяет создавать mock/fake реализации для UI тестов.
 */
interface IChatViewModel {
    /**
     * Состояние UI экрана.
     */
    val uiState: StateFlow<ChatUiState>

    /**
     * Индикатор отправки сообщения.
     */
    val isLoading: StateFlow<Boolean>

    /**
     * События чата для коммуникации с родительскими экранами.
     */
    val events: SharedFlow<ChatEvent>

    /**
     * Текст сообщения для отправки.
     */
    val messageText: MutableState<String>

    /**
     * Загрузка сообщений при открытии экрана.
     *
     * @param dialogId Идентификатор диалога
     */
    fun loadMessages(dialogId: Long)

    /**
     * Обновить список сообщений с сервера.
     * Вызывается после успешной отправки сообщения или по кнопке обновления.
     */
    fun refreshMessages()

    /**
     * Отправить сообщение.
     *
     * @param userId Идентификатор собеседника
     */
    fun sendMessage(userId: Int)

    /**
     * Пометить диалог как прочитанный.
     * Вызывается при появлении последнего сообщения на экране.
     *
     * @param userId Идентификатор собеседника
     */
    fun markAsRead(userId: Int)
}
