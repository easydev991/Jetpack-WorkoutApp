package com.swparks.ui.viewmodel

import com.swparks.ui.state.DialogsUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс для DialogsViewModel.
 *
 * Создан для возможности тестирования UI компонентов Compose без бизнес-логики.
 * Позволяет создавать mock/fake реализации для UI тестов.
 */
interface IDialogsViewModel {
    /**
     * Состояние UI экрана.
     */
    val uiState: StateFlow<DialogsUiState>

    /**
     * Индикатор обновления (pull-to-refresh).
     */
    val isRefreshing: StateFlow<Boolean>

    /**
     * Индикатор загрузки диалогов после авторизации.
     * True когда идет первая загрузка данных после авторизации.
     */
    val isLoadingDialogs: StateFlow<Boolean>

    /**
     * Ошибка синхронизации для отображения в Snackbar.
     */
    val syncError: StateFlow<String?>

    /**
     * Обновить диалоги (pull-to-refresh).
     */
    fun refresh()

    /**
     * Загрузить диалоги после успешной авторизации.
     * Всегда показывает LoadingOverlayView (isLoadingDialogs = true).
     * Вызывается из RootScreen.onLoginSuccess.
     */
    fun loadDialogsAfterAuth()

    /**
     * Обработать нажатие на диалог.
     *
     * @param dialogId Идентификатор диалога
     * @param userId Идентификатор собеседника (может быть null)
     */
    fun onDialogClick(dialogId: Long, userId: Int?)

    /**
     * Скрыть ошибку синхронизации.
     */
    fun dismissSyncError()

    /**
     * Индикатор удаления диалога.
     */
    val isDeleting: StateFlow<Boolean>

    /**
     * Индикатор отметки диалога как прочитанного.
     */
    val isMarkingAsRead: StateFlow<Boolean>

    /**
     * Общий индикатор любой операции обновления (удаление или отметка прочитанным).
     */
    val isUpdating: StateFlow<Boolean>

    /**
     * Удалить диалог.
     *
     * @param dialogId Идентификатор диалога для удаления
     */
    fun deleteDialog(dialogId: Long)

    /**
     * Отметить диалог как прочитанный.
     *
     * @param dialogId Идентификатор диалога
     * @param userId Идентификатор собеседника
     */
    fun markDialogAsRead(dialogId: Long, userId: Int)
}
