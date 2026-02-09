package com.swparks.ui.viewmodel

import com.swparks.data.model.User
import com.swparks.ui.state.BlacklistUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fake-реализация BlacklistViewModel для UI тестов.
 *
 * Предоставляет простую реализацию интерфейса с возможностью установки состояния.
 * Используется в Compose UI тестах для проверки разных состояний экрана.
 */
class FakeBlacklistViewModel(
    override val uiState: StateFlow<BlacklistUiState> = MutableStateFlow(BlacklistUiState.Loading),
    override val itemToRemove: StateFlow<User?> = MutableStateFlow(null),
    override val showRemoveDialog: StateFlow<Boolean> = MutableStateFlow(false),
    override val isRemoving: StateFlow<Boolean> = MutableStateFlow(false),
    override val showSuccessAlert: StateFlow<Boolean> = MutableStateFlow(false),
    override val unblockedUserName: StateFlow<String?> = MutableStateFlow(null)
) : IBlacklistViewModel {

    /**
     * Функция-заглушка для показа диалога удаления.
     * В тестах можно проверить, был ли вызван этот метод.
     */
    override fun showRemoveDialog(user: User) {
        // Заглушка - не делает ничего в тестах
    }

    /**
     * Функция-заглушка для удаления пользователя из черного списка.
     * В тестах можно проверить, был ли вызван этот метод.
     */
    override fun removeFromBlacklist(user: User) {
        // Заглушка - не делает ничего в тестах
    }

    /**
     * Функция-заглушка для отмены удаления.
     * В тестах можно проверить, был ли вызван этот метод.
     */
    override fun cancelRemove() {
        // Заглушка - не делает ничего в тестах
    }

    /**
     * Функция-заглушка для закрытия алерта успешной разблокировки.
     * В тестах можно проверить, был ли вызван этот метод.
     */
    override fun dismissSuccessAlert() {
        // Заглушка - не делает ничего в тестах
    }
}
