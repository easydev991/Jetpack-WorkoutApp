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
 * Расширяет IJournalSettingsViewModel для поддержки редактирования настроек дневника.
 */
interface IJournalsViewModel : IJournalSettingsViewModel {
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
     * Информационные сообщения и ошибки отправляются через [com.swparks.util.UserNotifier].
     *
     * @param journalId Идентификатор дневника
     * @param title Новое название дневника
     * @param viewAccess Новый уровень доступа для просмотра
     * @param commentAccess Новый уровень доступа для комментариев
     */
    override fun editJournalSettings(
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
     * Настройки дневника успешно сохранены.
     *
     * @property journal Обновленный дневник
     */
    data class JournalSettingsSaved(val journal: com.swparks.domain.model.Journal) : JournalsEvent
}
