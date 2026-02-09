package com.swparks.ui.viewmodel

import com.swparks.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fake-реализация ProfileViewModel для UI тестов.
 *
 * Предоставляет простую реализацию интерфейса с возможностью установки состояния.
 * Используется в Compose UI тестах для проверки разных состояний экрана.
 */
class FakeProfileViewModel(
    override val currentUser: StateFlow<User?> = MutableStateFlow(null),
    override val uiState: StateFlow<ProfileUiState> = MutableStateFlow(ProfileUiState.Loading),
    override val isRefreshing: StateFlow<Boolean> = MutableStateFlow(false),
    override val blacklist: StateFlow<List<User>> = MutableStateFlow(emptyList())
) : IProfileViewModel {

    /**
     * Функция-заглушка для загрузки профиля с сервера.
     * В тестах состояние устанавливается напрямую через конструктор.
     */
    override fun loadProfileFromServer(userId: Long) {
        // Заглушка - не делает ничего в тестах
    }

    /**
     * Функция-заглушка для обновления профиля (pull-to-refresh).
     * В тестах состояние устанавливается напрямую через конструктор.
     */
    override fun refreshProfile() {
        // Заглушка - не делает ничего в тестах
    }
}
