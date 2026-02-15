package com.swparks.ui.viewmodel

import com.swparks.ui.state.SearchUserUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс для SearchUserViewModel.
 *
 * Создан для возможности тестирования UI компонентов Compose без бизнес-логики.
 */
interface ISearchUserViewModel {
    /**
     * Состояние UI экрана.
     * Loading состояние используется для отображения LoadingOverlayView.
     */
    val uiState: StateFlow<SearchUserUiState>

    /**
     * Текст поискового запроса.
     */
    val searchQuery: MutableStateFlow<String>

    /**
     * Выполнить поиск пользователей.
     *
     * Валидирует запрос (минимум 2 символа) и вызывает API.
     * Игнорирует запрос если:
     * - Текущее состояние Loading
     * - Запрос совпадает с последним успешным поиском
     */
    fun onSearch()

    /**
     * Обработать нажатие на пользователя.
     *
     * @param userId ID пользователя
     */
    fun onUserClick(userId: Long)
}
