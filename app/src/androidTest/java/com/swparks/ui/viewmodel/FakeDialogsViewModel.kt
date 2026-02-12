package com.swparks.ui.viewmodel

import com.swparks.ui.state.DialogsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fake-реализация DialogsViewModel для UI тестов.
 *
 * Предоставляет простую реализацию интерфейса с возможностью установки состояния.
 * Используется в Compose UI тестах для проверки разных состояний экрана.
 */
class FakeDialogsViewModel(
    override val uiState: StateFlow<DialogsUiState>,
    override val isRefreshing: StateFlow<Boolean> = MutableStateFlow(false),
    override val syncError: StateFlow<String?> = MutableStateFlow(null),
    override val isLoadingDialogs: StateFlow<Boolean> = MutableStateFlow(false)
) : IDialogsViewModel {

    /**
     * Функция-заглушка для обновления.
     * В тестах состояние устанавливается напрямую через конструктор.
     */
    override fun refresh() {
        // Заглушка - не делает ничего в тестах
    }

    /**
     * Функция-заглушка для загрузки диалогов после авторизации.
     */
    override fun loadDialogsAfterAuth() {
        // Заглушка - не делает ничего в тестах
    }

    /**
     * Функция-заглушка для обработки нажатия на диалог.
     * В тестах можно проверить, был ли вызван этот метод.
     */
    override fun onDialogClick(dialogId: Long, userId: Int?) {
        // Заглушка - не делает ничего в тестах
    }

    /**
     * Функция-заглушка для скрытия ошибки синхронизации.
     */
    override fun dismissSyncError() {
        // Заглушка - не делает ничего в тестах
    }
}
