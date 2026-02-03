package com.swparks.viewmodel

import com.swparks.model.User

/** UI State для экрана списка друзей */
sealed class FriendsListUiState {
    /** Состояние загрузки */
    data object Loading : FriendsListUiState()

    /** Успешная загрузка с данными */
    data class Success(
        val friendRequests: List<User> = emptyList(),
        val friends: List<User> = emptyList()
    ) : FriendsListUiState()

    /** Состояние индикатора загрузки (выполнение запроса к серверу) */
    data object Busy : FriendsListUiState()

    /** Состояние ошибки с сообщением */
    data class Error(val message: String) : FriendsListUiState()
}
