package com.swparks.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.data.database.dao.UserDao
import com.swparks.data.database.entity.UserEntity
import com.swparks.data.database.entity.toDomain
import com.swparks.data.repository.SWRepository
import com.swparks.ui.state.FriendsListUiState
import com.swparks.util.AppError
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel для экрана списка друзей
 *
 * Управляет отображением списка друзей и заявок на добавление в друзья
 *
 * @param userDao DAO для работы с пользователями в локальной БД
 * @param swRepository Репозиторий для работы с API и загрузки социальных данных
 * @param logger Логгер для записи сообщений
 * @param userNotifier Обработчик ошибок для отправки ошибок в UI
 */
class FriendsListViewModel(
    private val userDao: UserDao,
    private val swRepository: SWRepository,
    private val logger: Logger,
    private val userNotifier: UserNotifier,
) : ViewModel(), IFriendsListViewModel {

    private companion object {
        private const val TAG = "FriendsListViewModel"
    }

    // Flow для заявок на добавление в друзья
    override val friendRequests: Flow<List<UserEntity>> =
        userDao.getFriendRequestsFlow()

    // Flow для списка друзей
    override val friends: Flow<List<UserEntity>> = userDao.getFriendsFlow()

    // UI State
    private val _uiState = MutableStateFlow<FriendsListUiState>(FriendsListUiState.Loading)
    override val uiState: StateFlow<FriendsListUiState> = _uiState.asStateFlow()

    // Индикатор загрузки при выполнении запросов к серверу (принятие/отклонение заявки)
    private val _isProcessing = MutableStateFlow(false)
    override val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    init {
        viewModelScope.launch {
            // Подписываемся на потоки из UserDao
            combine(friendRequests, friends) { requests, friendList ->
                // Преобразуем UserEntity в User
                val requestsUsers = requests.map { it.toDomain() }
                val friendsUsers = friendList.map { it.toDomain() }
                FriendsListUiState.Success(
                    friendRequests = requestsUsers,
                    friends = friendsUsers
                )
            }
                .collect { state -> _uiState.value = state }
        }

        viewModelScope.launch {
            // Проверяем, есть ли данные в кэше (fallback-механизм)
            val currentUser = userDao.getCurrentUserFlow().first()
            if (currentUser != null) {
                // Получаем текущее состояние
                val currentState = uiState.value
                if (currentState is FriendsListUiState.Success &&
                    currentState.friendRequests.isEmpty() &&
                    currentState.friends.isEmpty()
                ) {
                    // Если списки пустые, загружаем данные с сервера
                    logger.i(
                        TAG,
                        "Загрузка социальных данных для экрана друзей: ${currentUser.toDomain().id}"
                    )
                    swRepository
                        .getSocialUpdates(currentUser.toDomain().id)
                        .onSuccess {
                            logger.i(TAG, "Социальные данные успешно загружены из кэша")
                        }
                        .onFailure { error ->
                            logger.e(TAG, "Ошибка загрузки социальных данных: ${error.message}")
                        }
                } else {
                    logger.i(TAG, "Социальные данные уже загружены в кэше из ProfileViewModel")
                }
            }
        }
    }

    /**
     * Принятие заявки на добавление в друзья
     *
     * @param userId ID пользователя
     */
    override fun onAcceptFriendRequest(userId: Long) {
        viewModelScope.launch {
            _isProcessing.value = true

            logger.i(TAG, "Принятие заявки на добавление в друга: userId=$userId")
            swRepository.respondToFriendRequest(userId, accept = true)
                .onSuccess {
                    logger.i(TAG, "Заявка успешно принята: userId=$userId")
                }
                .onFailure { error ->
                    userNotifier.handleError(
                        AppError.Network(
                            message = "Не удалось принять заявку. Проверьте подключение к интернету.",
                            throwable = error
                        )
                    )
                }
            _isProcessing.value = false
        }
    }

    /**
     * Отклонение заявки на добавление в друзья
     *
     * @param userId ID пользователя
     */
    override fun onDeclineFriendRequest(userId: Long) {
        viewModelScope.launch {
            _isProcessing.value = true

            logger.i(TAG, "Отклонение заявки на добавление в друга: userId=$userId")
            swRepository.respondToFriendRequest(userId, accept = false)
                .onSuccess {
                    logger.i(TAG, "Заявка успешно отклонена: userId=$userId")
                }
                .onFailure { error ->
                    userNotifier.handleError(
                        AppError.Network(
                            message = "Не удалось отклонить заявку. Проверьте подключение к интернету.",
                            throwable = error
                        )
                    )
                }
            _isProcessing.value = false
        }
    }

    /**
     * Нажатие на друга в списке
     *
     * @param userId ID пользователя
     */
    override fun onFriendClick(userId: Long) {
        logger.i(TAG, "Нажатие на друга: userId=$userId")
    }
}
