package com.swparks.ui.viewmodel

import com.swparks.ui.model.JournalAccess
import com.swparks.ui.state.JournalsUiState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс для JournalsViewModel.
 *
 * Создан для возможности тестирования UI компонентов Compose без бизнес-логики.
 * Позволяет создавать mock/fake реализации для UI тестов.
 */
interface IJournalsViewModel {
    /**
     * Состояние UI экрана.
     */
    val uiState: StateFlow<JournalsUiState>

    /**
     * Индикатор обновления (pull-to-refresh).
     */
    val isRefreshing: StateFlow<Boolean>

    /**
     * Индикатор удаления дневника.
     */
    val isDeleting: StateFlow<Boolean>

    /**
     * Поток событий UI (например, для Snackbar).
     */
    val events: SharedFlow<JournalsEvent>

    /**
     * Повторить загрузку при ошибке.
     */
    fun retry()

    /**
     * Загрузить дневники с сервера.
     */
    fun loadJournals()

    /**
     * Удалить дневник.
     *
     * @param journalId Идентификатор дневника
     */
    fun deleteJournal(journalId: Long)

    /**
     * Редактировать настройки дневника.
     *
     * После успешного обновления эмитится событие [JournalsEvent.JournalSettingsSaved].
     * При ошибке эмитится событие [JournalsEvent.ShowSnackbar].
     *
     * @param journalId Идентификатор дневника
     * @param title Новое название дневника
     * @param viewAccess Новый уровень доступа для просмотра
     * @param commentAccess Новый уровень доступа для комментариев
     */
    fun editJournalSettings(
        journalId: Long,
        title: String,
        viewAccess: JournalAccess,
        commentAccess: JournalAccess
    )
}

/**
 * События UI для экрана списка дневников.
 */
sealed interface JournalsEvent {
    /**
     * Показать Snackbar с сообщением.
     *
     * @property message Текст сообщения
     */
    data class ShowSnackbar(val message: String) : JournalsEvent

    /**
     * Настройки дневника успешно сохранены.
     *
     * @property journal Обновленный дневник
     */
    data class JournalSettingsSaved(val journal: com.swparks.domain.model.Journal) : JournalsEvent
}
