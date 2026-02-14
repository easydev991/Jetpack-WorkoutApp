package com.swparks.ui.viewmodel

import com.swparks.ui.model.JournalAccess
import kotlinx.coroutines.flow.StateFlow

/**
 * Общий интерфейс для ViewModel, поддерживающих редактирование настроек дневника.
 *
 * Используется JournalSettingsDialog для работы с разными ViewModel
 * (IJournalsViewModel и IJournalEntriesViewModel).
 */
interface IJournalSettingsViewModel {
    /**
     * Индикатор сохранения настроек дневника.
     */
    val isSavingSettings: StateFlow<Boolean>

    /**
     * Редактировать настройки дневника.
     *
     * После успешного обновления эмитится событие [JournalSettingsSaved].
     * При ошибке эмитится событие [ShowSnackbar].
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
