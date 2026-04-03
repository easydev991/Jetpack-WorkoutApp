package com.swparks.ui.state

import com.swparks.data.model.User

/**
 * Состояния экрана поиска пользователей.
 */
sealed class SearchUserUiState {
    /**
     * Начальное состояние - пустой экран до первого поиска.
     */
    data object Initial : SearchUserUiState()

    /**
     * Состояние загрузки - отображается LoadingOverlayView поверх предыдущего контента.
     */
    data object Loading : SearchUserUiState()

    /**
     * Успешный результат поиска - отображается список пользователей.
     */
    data class Success(
        val users: List<User>
    ) : SearchUserUiState()

    /**
     * Пользователи не найдены - отображается сообщение.
     */
    data object Empty : SearchUserUiState()

    /**
     * Ошибка сети при поиске - отображается локализованное сообщение.
     * Локализация выполняется в UI слое через stringResource().
     */
    data object NetworkError : SearchUserUiState()
}
