package com.swparks.ui.state

import com.swparks.data.model.User

/** UI State для экрана черного списка */
sealed class BlacklistUiState {
    /** Состояние загрузки */
    data object Loading : BlacklistUiState()

    /** Успешная загрузка с данными */
    data class Success(
        val blacklist: List<User> = emptyList(),
        val isLoading: Boolean = false
    ) : BlacklistUiState()

    /** Состояние ошибки с сообщением */
    data class Error(val message: String) : BlacklistUiState()
}