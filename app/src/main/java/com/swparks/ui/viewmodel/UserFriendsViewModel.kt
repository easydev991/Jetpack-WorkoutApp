package com.swparks.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.data.model.User
import com.swparks.data.repository.SWRepository
import com.swparks.util.AppError
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI State для экрана друзей пользователя
 */
sealed interface UserFriendsUiState {
    data object Loading : UserFriendsUiState

    data class Success(
        val friends: List<User>
    ) : UserFriendsUiState

    data class Error(
        val message: String
    ) : UserFriendsUiState
}

/**
 * ViewModel для экрана просмотра друзей другого пользователя
 *
 * @param userId ID пользователя, чей список друзей нужно показать
 * @param swRepository Репозиторий для работы с API
 * @param logger Логгер
 * @param userNotifier Обработчик ошибок
 */
class UserFriendsViewModel(
    private val userId: Long,
    private val swRepository: SWRepository,
    private val logger: Logger,
    private val userNotifier: UserNotifier
) : ViewModel() {
    private companion object {
        private const val TAG = "UserFriendsViewModel"
    }

    private val _uiState = MutableStateFlow<UserFriendsUiState>(UserFriendsUiState.Loading)
    val uiState: StateFlow<UserFriendsUiState> = _uiState.asStateFlow()

    init {
        loadFriends()
    }

    private fun loadFriends() {
        viewModelScope.launch {
            logger.i(TAG, "Загрузка друзей пользователя: userId=$userId")
            _uiState.value = UserFriendsUiState.Loading

            swRepository
                .getFriendsForUser(userId)
                .onSuccess { friends ->
                    logger.i(TAG, "Друзья загружены: ${friends.size}")
                    _uiState.value = UserFriendsUiState.Success(friends)
                }.onFailure { error ->
                    logger.e(TAG, "Ошибка загрузки друзей: ${error.message}")
                    userNotifier.handleError(
                        AppError.Network(
                            message = "Не удалось загрузить список друзей",
                            throwable = error
                        )
                    )
                    _uiState.value =
                        UserFriendsUiState.Error(
                            message = error.message ?: "Неизвестная ошибка"
                        )
                }
        }
    }

    /**
     * Повторная загрузка друзей
     */
    fun refresh() {
        loadFriends()
    }
}
