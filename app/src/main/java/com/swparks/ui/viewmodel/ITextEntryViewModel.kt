package com.swparks.ui.viewmodel

import com.swparks.ui.state.TextEntryEvent
import com.swparks.ui.state.TextEntryUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс для TextEntryViewModel.
 *
 * Создан для возможности тестирования UI компонентов Compose без бизнес-логики.
 * Позволяет создавать mock/fake реализации для UI тестов.
 */
interface ITextEntryViewModel {
    /**
     * Состояние UI экрана.
     */
    val uiState: StateFlow<TextEntryUiState>

    /**
     * Одноразовые события (Success, Error).
     */
    val events: Flow<TextEntryEvent>

    /**
     * Обновляет текст в текстовом поле.
     *
     * @param text Новый текст
     */
    fun onTextChanged(text: String)

    /**
     * Отправляет текст на сервер (создает новый комментарий/запись или редактирует существующий).
     */
    fun onSend()

    /**
     * Сбрасывает состояние ошибки.
     */
    fun onDismissError()

    /**
     * Сбрасывает состояние ViewModel для новой сессии (при открытии sheet).
     */
    fun resetState()
}
