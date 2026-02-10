package com.swparks.ui.viewmodel

import com.swparks.ui.state.TextEntryEvent
import com.swparks.ui.state.TextEntryUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Fake-реализация TextEntryViewModel для UI тестов.
 *
 * Предоставляет простую реализацию интерфейса с возможностью установки состояния.
 * Используется в Compose UI тестах для проверки разных состояний экрана.
 */
class FakeTextEntryViewModel(
    override val uiState: StateFlow<TextEntryUiState>
) : ITextEntryViewModel {

    // Поток событий для тестирования
    private val _events = MutableSharedFlow<TextEntryEvent>()
    override val events: SharedFlow<TextEntryEvent> = _events.asSharedFlow()

    /**
     * Функция-заглушка для обновления текста.
     * В тестах состояние устанавливается напрямую через конструктор.
     */
    override fun onTextChanged(text: String) {
        // Заглушка - не делает ничего в тестах
    }

    /**
     * Функция-заглушка для отправки текста.
     * В тестах можно проверить, был ли вызван этот метод.
     */
    override fun onSend() {
        // Заглушка - не делает ничего в тестах
    }

    /**
     * Функция-заглушка для очистки ошибок.
     * В тестах можно проверить, был ли вызван этот метод.
     */
    override fun onDismissError() {
        // Заглушка - не делает ничего в тестах
    }

    /**
     * Функция-заглушка для сброса состояния.
     * В тестах можно проверить, был ли вызван этот метод.
     */
    override fun resetState() {
        // Заглушка - не делает ничего в тестах
    }
}
