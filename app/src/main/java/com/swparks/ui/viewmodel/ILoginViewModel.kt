package com.swparks.ui.viewmodel

import com.swparks.ui.model.LoginCredentials
import com.swparks.ui.state.LoginEvent
import com.swparks.ui.state.LoginUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс для LoginViewModel.
 *
 * Создан для возможности тестирования UI компонентов Compose без бизнес-логики.
 * Позволяет создавать mock/fake реализации для UI тестов.
 */
interface ILoginViewModel {
    /**
     * Состояние UI экрана.
     */
    val uiState: StateFlow<LoginUiState>

    /**
     * Ошибка авторизации для отображения под полем пароля.
     */
    val loginErrorState: StateFlow<String?>

    /**
     * Ошибка восстановления пароля для отображения под полем логина.
     */
    val resetErrorState: StateFlow<String?>

    /**
     * Одноразовые события авторизации (Success, ResetSuccess).
     */
    val loginEvents: Flow<LoginEvent>

    /**
     * Текущие учетные данные пользователя (логин и пароль) в виде StateFlow.
     * Использовать для подписки в UI через collectAsStateWithLifecycle().
     */
    val credentialsState: StateFlow<LoginCredentials>

    /**
     * Обновляет логин пользователя.
     * При изменении логина очищаются все ошибки.
     *
     * @param value Новый логин пользователя
     */
    fun onLoginChange(value: String)

    /**
     * Обновляет пароль пользователя.
     * При изменении пароля очищаются все ошибки.
     *
     * @param value Новый пароль пользователя
     */
    fun onPasswordChange(value: String)

    /**
     * Выполняет вход в систему.
     */
    fun login()

    /**
     * Выполняет восстановление пароля.
     */
    fun resetPassword()

    /**
     * Очищает все ошибки (loginError и resetError).
     */
    fun clearErrors()

    /**
     * Сбрасывает состояние ViewModel для новой сессии авторизации.
     */
    fun resetForNewSession()
}
