package com.swparks.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.data.model.LoginSuccess
import com.swparks.domain.usecase.ILoginUseCase
import com.swparks.domain.usecase.ILogoutUseCase
import com.swparks.ui.model.LoginCredentials
import com.swparks.util.AppError
import com.swparks.util.UserNotifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Состояние авторизации
 */
sealed class AuthUiState {
    data object Idle : AuthUiState()

    data object Loading : AuthUiState()

    data class Success(
        val loginSuccess: LoginSuccess
    ) : AuthUiState()

    data class Error(
        val message: String
    ) : AuthUiState()
}

/**
 * ViewModel для управления авторизацией пользователей.
 *
 * Использует LoginUseCase и LogoutUseCase для авторизации и выхода из системы.
 * Управляет состоянием UI авторизации. Обрабатывает ошибки через UserNotifier.
 *
 * @param loginUseCase Use case для входа в систему
 * @param logoutUseCase Use case для выхода из системы
 * @param userNotifier Интерфейс для обработки и отправки ошибок в UI-слой
 */
class AuthViewModel(
    private val loginUseCase: ILoginUseCase,
    private val logoutUseCase: ILogoutUseCase,
    private val userNotifier: UserNotifier
) : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Выполняет вход в систему.
     *
     * @param credentials Учетные данные пользователя
     */
    fun login(credentials: LoginCredentials) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            loginUseCase(credentials)
                .onSuccess { loginSuccess ->
                    _uiState.value = AuthUiState.Success(loginSuccess)
                }.onFailure { exception ->
                    val errorMessage = exception.message ?: "Неизвестная ошибка авторизации"
                    _uiState.value = AuthUiState.Error(message = errorMessage)

                    // Отправляем ошибку через UserNotifier
                    userNotifier.handleError(
                        AppError.Network(
                            message = "Не удалось войти. Проверьте подключение к интернету.",
                            throwable = exception
                        )
                    )
                }
        }
    }

    /**
     * Выполняет выход из системы.
     */
    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _uiState.value = AuthUiState.Idle
        }
    }

    /**
     * Сбрасывает состояние UI.
     */
    fun clearError() {
        _uiState.value = AuthUiState.Idle
    }
}
