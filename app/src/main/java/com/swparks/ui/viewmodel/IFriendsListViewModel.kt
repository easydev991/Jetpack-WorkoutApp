package com.swparks.ui.viewmodel

import com.swparks.data.database.entity.UserEntity
import com.swparks.ui.state.FriendsListUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс для FriendsListViewModel.
 *
 * Создан для возможности тестирования UI компонентов Compose без бизнес-логики.
 * Позволяет создавать mock/fake реализации для UI тестов.
 */
interface IFriendsListViewModel {
    /**
     * Flow для заявок на добавление в друзья.
     */
    val friendRequests: Flow<List<UserEntity>>

    /**
     * Flow для списка друзей.
     */
    val friends: Flow<List<UserEntity>>

    /**
     * Состояние UI экрана.
     */
    val uiState: StateFlow<FriendsListUiState>

    /**
     * Принятие заявки на добавление в друзья.
     *
     * @param userId ID пользователя
     */
    fun onAcceptFriendRequest(userId: Long)

    /**
     * Отклонение заявки на добавление в друзья.
     *
     * @param userId ID пользователя
     */
    fun onDeclineFriendRequest(userId: Long)

    /**
     * Нажатие на друга в списке.
     *
     * @param userId ID пользователя
     */
    fun onFriendClick(userId: Long)
}
