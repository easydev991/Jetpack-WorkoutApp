package com.swparks.ui.viewmodel

import com.swparks.ui.model.LoginCredentials
import com.swparks.ui.state.LoginEvent
import com.swparks.ui.state.LoginUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake-реализация LoginViewModel для UI тестов.
 *
 * Предоставляет простую реализацию интерфейса с возможностью установки состояния.
 * Используется в Compose UI тестах для проверки разных состояний экрана.
 */
class FakeLoginViewModel(
    override val uiState: StateFlow<LoginUiState> = MutableStateFlow(LoginUiState.Idle),
    override val loginErrorState: StateFlow<String?> = MutableStateFlow(null),
    override val resetErrorState: StateFlow<String?> = MutableStateFlow(null),
    initialLogin: String = "",
    initialPassword: String = ""
) : ILoginViewModel {
    // Поток событий для тестирования
    private val eventsFlow = MutableSharedFlow<LoginEvent>()
    override val loginEvents: SharedFlow<LoginEvent> = eventsFlow.asSharedFlow()

    // Состояние учетных данных
    private val _credentialsState =
        MutableStateFlow(LoginCredentials(login = initialLogin, password = initialPassword))
    override val credentialsState: StateFlow<LoginCredentials> = _credentialsState.asStateFlow()

    /**
     * Обновляет логин пользователя.
     * В тестах состояние устанавливается напрямую через конструктор.
     */
    override fun onLoginChange(value: String) {
        _credentialsState.value = _credentialsState.value.copy(login = value)
    }

    /**
     * Обновляет пароль пользователя.
     * В тестах состояние устанавливается напрямую через конструктор.
     */
    override fun onPasswordChange(value: String) {
        _credentialsState.value = _credentialsState.value.copy(password = value)
    }

    /**
     * Функция-заглушка для выполнения входа.
     * В тестах можно проверить, был ли вызван этот метод.
     */
    override fun login() {
        // Заглушка - не делает ничего в тестах
    }

    /**
     * Функция-заглушка для восстановления пароля.
     * В тестах можно проверить, был ли вызван этот метод.
     */
    override fun resetPassword() {
        // Заглушка - не делает ничего в тестах
    }

    /**
     * Функция-заглушка для очистки ошибок.
     * В тестах можно проверить, был ли вызван этот метод.
     */
    override fun clearErrors() {
        // Заглушка - не делает ничего в тестах
    }

    /**
     * Функция-заглушка для сброса состояния новой сессии.
     * В тестах можно проверить, был ли вызван этот метод.
     */
    override fun resetForNewSession() {
        // Заглушка - не делает ничего в тестах
    }
}
