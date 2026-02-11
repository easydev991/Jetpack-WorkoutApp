package com.swparks.ui.viewmodel

import com.swparks.ui.model.JournalAccess
import com.swparks.ui.state.JournalsUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Fake-реализация JournalsViewModel для UI тестов.
 *
 * Предоставляет простую реализацию интерфейса с возможность установки состояния.
 * Используется в Compose UI тестах для проверки разных состояний экрана.
 */
class FakeJournalsViewModel(
    override val uiState: StateFlow<JournalsUiState>,
    override val isRefreshing: StateFlow<Boolean>,
    override val isDeleting: StateFlow<Boolean> = MutableStateFlow(false)
) : IJournalsViewModel {

    // Поток событий для тестирования
    private val _events = MutableSharedFlow<JournalsEvent>()
    override val events: SharedFlow<JournalsEvent> = _events.asSharedFlow()

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
    override fun loadJournals() {
        // Заглушка - не делает ничего в тестах
    }

    /**
     * Функция-заглушка для удаления дневника.
     * В тестах можно проверить, был ли вызван этот метод.
     */
    override fun deleteJournal(journalId: Long) {
        // Заглушка - не делает ничего в тестах
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

    /**
     * Эмитировать событие успешного сохранения настроек дневника.
     * Используется в тестах для проверки закрытия диалога.
     */
    suspend fun emitJournalSettingsSaved(journal: com.swparks.domain.model.Journal) {
        _events.emit(JournalsEvent.JournalSettingsSaved(journal))
    }
}
