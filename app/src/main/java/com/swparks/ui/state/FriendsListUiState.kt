package com.swparks.ui.state

import com.swparks.data.model.User

/** UI State для экрана списка друзей */
sealed class FriendsListUiState {
    /** Состояние загрузки */
    data object Loading : FriendsListUiState()

    /** Успешная загрузка с данными */
    data class Success(
        val friendRequests: List<User> = emptyList(),
        val friends: List<User> = emptyList(),
        val isProcessing: Boolean = false
    ) : FriendsListUiState()

    /** Состояние ошибки с сообщением */
    data class Error(val message: String) : FriendsListUiState()
}