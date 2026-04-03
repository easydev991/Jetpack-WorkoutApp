package com.swparks.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.data.repository.SWRepository
import com.swparks.ui.state.SearchUserUiState
import com.swparks.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel для управления экраном поиска пользователей.
 *
 * Управляет состоянием UI экрана поиска, включая поисковый запрос,
 * выполнение поиска и отображение результатов.
 *
 * @param swRepository Репозиторий для вызова API поиска пользователей
 * @param logger Логгер для записи сообщений
 */
class SearchUserViewModel(
    private val swRepository: SWRepository,
    private val logger: Logger
) : ViewModel(),
    ISearchUserViewModel {
    private val _uiState = MutableStateFlow<SearchUserUiState>(SearchUserUiState.Initial)
    override val uiState: StateFlow<SearchUserUiState> = _uiState.asStateFlow()

    override val searchQuery = MutableStateFlow("")

    private var lastSearchedQuery: String? = null

    override fun onSearch() {
        val query = searchQuery.value.trim()

        // Проверяем условия для игнорирования поиска
        val shouldIgnore =
            when {
                query.length < MIN_SEARCH_LENGTH -> {
                    logger.d(TAG, "Search query too short: '$query' (${query.length} chars)")
                    true
                }

                uiState.value is SearchUserUiState.Loading -> {
                    logger.d(TAG, "Search ignored: already loading")
                    true
                }

                query == lastSearchedQuery && uiState.value is SearchUserUiState.Success -> {
                    logger.d(TAG, "Search ignored: same query with existing results")
                    true
                }

                else -> false
            }

        if (shouldIgnore) return

        viewModelScope.launch {
            _uiState.value = SearchUserUiState.Loading

            swRepository
                .findUsers(query)
                .onSuccess { users ->
                    // Устанавливаем последний запрос ТОЛЬКО после успешного завершения
                    // Это предотвращает race condition при быстром переключении запросов
                    lastSearchedQuery = query

                    _uiState.value =
                        if (users.isEmpty()) {
                            SearchUserUiState.Empty
                        } else {
                            SearchUserUiState.Success(users)
                        }
                    logger.i(TAG, "Search completed: found ${users.size} users for '$query'")
                }.onFailure { error ->
                    _uiState.value = SearchUserUiState.NetworkError
                    logger.e(TAG, "Search failed for '$query': ${error.message}", error)
                }
        }
    }

    override fun onUserClick(userId: Long) {
        logger.i(TAG, "User clicked: userId=$userId")
    }

    private companion object {
        private const val MIN_SEARCH_LENGTH = 2
        private const val TAG = "SearchUserViewModel"
    }
}
