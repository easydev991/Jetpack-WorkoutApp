package com.swparks.ui.viewmodel

import com.swparks.domain.model.JournalEntry
import com.swparks.ui.model.JournalAccess
import com.swparks.ui.state.JournalEntriesUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Fake-реализация JournalEntriesViewModel для UI тестов.
 *
 * Предоставляет простую реализацию интерфейса с возможностью установки состояния.
 * Используется в Compose UI тестах для проверки разных состояний экрана.
 */
class FakeJournalEntriesViewModel(
    override val uiState: StateFlow<JournalEntriesUiState>,
    override val isRefreshing: StateFlow<Boolean>,
    override val isDeleting: StateFlow<Boolean> = MutableStateFlow(false),
    override val canCreateEntry: StateFlow<Boolean> = MutableStateFlow(false),
    override val isSavingSettings: StateFlow<Boolean> = MutableStateFlow(false)
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

    /**
     * Функция-заглушка для обновления списка.
     * В тестах состояние устанавливается напрямую через конструктор.
     */
    override fun refresh() {
        // Заглушка - не делает ничего в тестах
    }

    /**
     * Функция-заглушка для проверки возможности редактирования записи.
     * В тестах возвращает true если у записи есть автор.
     */
    override fun canEditEntry(entry: JournalEntry): Boolean {
        // Заглушка - проверяет, что у записи есть автор
        return entry.authorId != null
    }

    /**
     * Функция-заглушка для проверки возможности удаления записи.
     * В тестах возвращает true если у записи есть автор.
     */
    override fun canDeleteEntry(entry: JournalEntry): Boolean {
        // Заглушка - проверяет, что у записи есть автор
        return entry.authorId != null
    }

    /**
     * Функция-заглушка для редактирования настроек дневника.
     * В тестах можно проверить, был ли вызван этот метод.
     */
    override fun editJournalSettings(
        journalId: Long,
        title: String,
        viewAccess: JournalAccess,
        commentAccess: JournalAccess
    ) {
        // Заглушка - не делает ничего в тестах
    }
}
