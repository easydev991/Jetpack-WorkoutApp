package com.swparks.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.domain.usecase.ILoginUseCase
import com.swparks.domain.usecase.ILogoutUseCase
import com.swparks.model.LoginSuccess
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
    data class Success(val loginSuccess: LoginSuccess) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

/**
 * ViewModel для управления авторизацией пользователей.
 *
 * Использует LoginUseCase и LogoutUseCase для авторизации и выхода из системы.
 * Управляет состоянием UI авторизации.
 *
 * @param loginUseCase Use case для входа в систему
 * @param logoutUseCase Use case для выхода из системы
 */
class AuthViewModel(
    private val loginUseCase: ILoginUseCase,
    private val logoutUseCase: ILogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Выполняет вход в систему.
     *
     * @param token Токен авторизации или null для очистки
     */
    fun login(token: String?) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            loginUseCase(token)
                .onSuccess { loginSuccess ->
                    _uiState.value = AuthUiState.Success(loginSuccess)
                }
                .onFailure { exception ->
                    _uiState.value = AuthUiState.Error(
                        message = exception.message ?: "Неизвестная ошибка авторизации"
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
