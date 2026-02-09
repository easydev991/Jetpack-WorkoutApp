package com.swparks.ui.viewmodel

import com.swparks.data.database.entity.UserEntity
import com.swparks.ui.state.FriendsListUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fake-реализация FriendsListViewModel для UI тестов.
 *
 * Предоставляет простую реализацию интерфейса с возможностью установки состояния.
 * Используется в Compose UI тестах для проверки разных состояний экрана.
 */
class FakeFriendsListViewModel(
    override val friendRequests: kotlinx.coroutines.flow.Flow<List<UserEntity>> = MutableStateFlow(
        emptyList()
    ),
    override val friends: kotlinx.coroutines.flow.Flow<List<UserEntity>> = MutableStateFlow(
        emptyList()
    ),
    override val uiState: StateFlow<FriendsListUiState> = MutableStateFlow(FriendsListUiState.Loading),
    override val isProcessing: StateFlow<Boolean> = MutableStateFlow(false)
) : IFriendsListViewModel {

    /**
     * Функция-заглушка для принятия заявки на добавление в друзья.
     * В тестах можно проверить, был ли вызван этот метод.
     */
    override fun onAcceptFriendRequest(userId: Long) {
        // Заглушка - не делает ничего в тестах
    }

    /**
     * Функция-заглушка для отклонения заявки на добавление в друзья.
     * В тестах можно проверить, был ли вызван этот метод.
     */
    override fun onDeclineFriendRequest(userId: Long) {
        // Заглушка - не делает ничего в тестах
    }

    /**
     * Функция-заглушка для нажатия на друга.
     * В тестах можно проверить, был ли вызван этот метод.
     */
    override fun onFriendClick(userId: Long) {
        // Заглушка - не делает ничего в тестах
    }
}
