package com.swparks.ui.state

import com.swparks.data.model.User

/** UI State для экрана черного списка */
sealed class BlacklistUiState {
    /** Состояние загрузки */
    data object Loading : BlacklistUiState()

    /** Успешная загрузка с данными */
    data class Success(
        val blacklist: List<User> = emptyList(),
        val isLoading: Boolean = false,
        val itemToRemove: User? = null,
        val showRemoveDialog: Boolean = false,
        val isRemoving: Boolean = false,
        val showSuccessAlert: Boolean = false,
        val unblockedUserName: String? = null
    ) : BlacklistUiState()

    /** Состояние ошибки с сообщением */
    data class Error(
        val message: String
    ) : BlacklistUiState()
}

/** Actions для экрана черного списка */
sealed class BlacklistAction {
    object Back : BlacklistAction()

    data class ShowRemoveDialog(
        val user: User
    ) : BlacklistAction()

    data class Remove(
        val user: User
    ) : BlacklistAction()

    object CancelRemove : BlacklistAction()

    object DismissSuccessAlert : BlacklistAction()
}
