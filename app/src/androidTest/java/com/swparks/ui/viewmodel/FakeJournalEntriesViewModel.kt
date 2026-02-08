package com.swparks.ui.viewmodel

import com.swparks.ui.state.JournalEntriesUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Fake-реализация JournalEntriesViewModel для UI тестов.
 *
 * Предоставляет простую реализацию интерфейса с возможность установки состояния.
 * Используется в Compose UI тестах для проверки разных состояний экрана.
 */
class FakeJournalEntriesViewModel(
    override val uiState: StateFlow<JournalEntriesUiState>,
    override val isRefreshing: StateFlow<Boolean>,
    override val isDeleting: StateFlow<Boolean> = MutableStateFlow(false)
) : IJournalEntriesViewModel {

    // Поток событий для тестирования
    private val _events = MutableSharedFlow<JournalEntriesEvent>()
    override val events: SharedFlow<JournalEntriesEvent> = _events.asSharedFlow()

    /**
     * Функция-заглушка для повторной загрузки.
     * В тестах состояние устанавливается напрямую через конструктор.
     */
    override fun retry() {
        // Заглушка - не делает ничего в тестах
    }

    /**
     * Функция-заглушка для загрузки.
     * В тестах состояние устанавливается напрямую через конструктор.
     */
    override fun loadEntries() {
        // Заглушка - не делает ничего в тестах
    }

    /**
     * Функция-заглушка для удаления записи.
     * В тестах можно проверить, был ли вызван этот метод.
     */
    override fun deleteEntry(entryId: Long) {
        // Заглушка - не делает ничего в тестах
    }

    /**
     * Функция-заглушка для проверки возможности удаления записи.
     * В тестах состояние устанавливается напрямую через конструктор.
     */
    override suspend fun canDeleteEntry(entryId: Long): Boolean {
        // Заглушка - всегда возвращает true в тестах
        return true
    }
}
