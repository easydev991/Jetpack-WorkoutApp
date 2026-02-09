package com.swparks.ui.viewmodel

import com.swparks.data.model.User
import com.swparks.ui.state.BlacklistUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс для BlacklistViewModel.
 *
 * Создан для возможности тестирования UI компонентов Compose без бизнес-логики.
 * Позволяет создавать mock/fake реализации для UI тестов.
 */
interface IBlacklistViewModel {
    /**
     * Состояние UI экрана.
     */
    val uiState: StateFlow<BlacklistUiState>

    /**
     * Пользователь для удаления из черного списка.
     */
    val itemToRemove: StateFlow<User?>

    /**
     * Состояние диалога подтверждения удаления.
     */
    val showRemoveDialog: StateFlow<Boolean>

    /**
     * Индикатор удаления из черного списка.
     */
    val isRemoving: StateFlow<Boolean>

    /**
     * Состояние алерта об успешной разблокировке.
     */
    val showSuccessAlert: StateFlow<Boolean>

    /**
     * Имя разблокированного пользователя для алерта.
     */
    val unblockedUserName: StateFlow<String?>

    /**
     * Показать диалог подтверждения удаления пользователя из черного списка.
     *
     * @param user Пользователь для удаления
     */
    fun showRemoveDialog(user: User)

    /**
     * Удаление пользователя из черного списка.
     *
     * @param user Пользователь для удаления
     */
    fun removeFromBlacklist(user: User)

    /**
     * Отмена удаления пользователя из черного списка.
     */
    fun cancelRemove()

    /**
     * Закрытие алерта об успешной разблокировке.
     */
    fun dismissSuccessAlert()
}
