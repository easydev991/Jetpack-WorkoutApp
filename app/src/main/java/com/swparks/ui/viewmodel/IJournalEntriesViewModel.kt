package com.swparks.ui.viewmodel

import com.swparks.domain.model.Journal
import com.swparks.domain.model.JournalEntry
import com.swparks.ui.state.JournalEntriesUiState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс для JournalEntriesViewModel.
 *
 * Создан для возможности тестирования UI компонентов Compose без бизнес-логики.
 * Позволяет создавать mock/fake реализации для UI тестов.
 * Расширяет IJournalSettingsViewModel для поддержки редактирования настроек дневника.
 */
interface IJournalEntriesViewModel : IJournalSettingsViewModel {
    /**
     * Состояние UI экрана.
     */
    val uiState: StateFlow<JournalEntriesUiState>

    /**
     * Индикатор обновления (pull-to-refresh).
     */
    val isRefreshing: StateFlow<Boolean>

    /**
     * Индикатор удаления записи.
     */
    val isDeleting: StateFlow<Boolean>

    /**
     * Поток событий UI (например, для Snackbar).
     */
    val events: SharedFlow<JournalEntriesEvent>

    /**
     * Загрузить записи с сервера.
     */
    fun loadEntries()

    /**
     * Повторить загрузку при ошибке.
     */
    fun retry()

    /**
     * Удалить запись из дневника.
     *
     * @param entryId Идентификатор записи
     */
    fun deleteEntry(entryId: Long)

    /**
     * Проверить, можно ли удалить запись.
     *
     * Первую запись (с минимальным id) нельзя удалить.
     *
     * @param entryId Идентификатор записи
     * @return true если удаление разрешено, false если это первая запись
     */
    suspend fun canDeleteEntry(entryId: Long): Boolean

    /**
     * Признак возможности создания записей (для отображения FAB).
     * Вычисляется на основе прав доступа к дневнику.
     */
    val canCreateEntry: StateFlow<Boolean>

    /**
     * Обновить список записей (public метод для refresh).
     */
    fun refresh()

    /**
     * Проверить, можно ли редактировать запись.
     *
     * Редактировать записи может только автор записи.
     *
     * @param entry Запись в дневнике
     * @return true если редактирование разрешено (автор записи)
     */
    fun canEditEntry(entry: JournalEntry): Boolean
}

/**
 * События UI для экрана списка записей в дневнике.
 */
sealed interface JournalEntriesEvent {
    /**
     * Настройки дневника успешно сохранены.
     *
     * @property journal Обновленный дневник
     */
    data class JournalSettingsSaved(val journal: Journal) : JournalEntriesEvent
}
