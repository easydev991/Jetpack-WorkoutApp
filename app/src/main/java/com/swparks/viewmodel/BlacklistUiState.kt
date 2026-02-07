package com.swparks.viewmodel

import com.swparks.model.User

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
