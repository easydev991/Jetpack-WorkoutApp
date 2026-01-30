package com.swparks.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.swparks.JetpackWorkoutApplication
import com.swparks.domain.usecase.ILoginUseCase
import com.swparks.domain.usecase.IResetPasswordUseCase
import com.swparks.model.LoginCredentials
import com.swparks.ui.state.LoginUiState
import com.swparks.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel для управления экраном авторизации.
 *
 * Управляет состоянием UI экрана входа в систему, включая учетные данные,
 * обработку ошибок авторизации и восстановления пароля.
 *
 * ВАЖНО: Эта ViewModel выполняет ТОЛЬКО авторизацию.
 * Загрузка данных пользователя выполняется в ProfileViewModel при открытии профиля.
 *
 * @param logger Логгер для записи сообщений
 * @param loginUseCase Use case для входа в систему
 * @param resetPasswordUseCase Use case для восстановления пароля
 */
class LoginViewModel(
    private val logger: Logger,
    private val loginUseCase: ILoginUseCase,
    private val resetPasswordUseCase: IResetPasswordUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginErrorState: StateFlow<String?> = _loginError.asStateFlow()


    private val _resetError = MutableStateFlow<String?>(null)
    val resetErrorState: StateFlow<String?> = _resetError.asStateFlow()

    private val _login = mutableStateOf("")
    private val _password = mutableStateOf("")

    val credentials: LoginCredentials
        get() = LoginCredentials(
            login = _login.value,
            password = _password.value
        )

    /**
     * Обновляет логин пользователя.
     * При изменении логина очищаются все ошибки.
     *
     * @param value Новый логин пользователя
     */
    fun onLoginChange(value: String) {
        _login.value = value
        clearErrors()
    }

    /**
     * Обновляет пароль пользователя.
     * При изменении пароля очищаются все ошибки.
     *
     * @param value Новый пароль пользователя
     */
    fun onPasswordChange(value: String) {
        _password.value = value
        clearErrors()
    }

    /**
     * Выполняет вход в систему.
     *
     * Если учетные данные валидны, вызывает loginUseCase.
     * При успешном входе обновляет состояние на LoginSuccess.
     * При ошибке авторизации сохраняет ошибку в loginError для отображения под полем пароля.
     *
     * ВАЖНО: Этот метод выполняет ТОЛЬКО авторизацию и сохраняет токен.
     * Загрузка данных пользователя выполняется в ProfileViewModel при открытии профиля.
     */
    fun login() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            loginUseCase(credentials)
                .onSuccess { result ->
                    _uiState.value = LoginUiState.LoginSuccess(
                        userId = result.userId,
                        socialUpdates = null // Загрузка выполняется в ProfileViewModel
                    )
                    _loginError.value = null
                }
                .onFailure { exception ->
                    val errorMessage = exception.message ?: "Неизвестная ошибка авторизации"
                    _uiState.value = LoginUiState.LoginError(errorMessage)
                    _loginError.value = errorMessage
                }
        }
    }

    /**
     * Выполняет восстановление пароля.
     *
     * Если логин пустой, не выполняет запрос (UI покажет алерт).
     * Если логин указан, вызывает resetPasswordUseCase.
     * При успешном восстановлении обновляет состояние на ResetSuccess.
     * При ошибке восстановления сохраняет ошибку в resetError для отображения под полем логина.
     */
    fun resetPassword() {
        if (!credentials.canRestorePassword) {
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            resetPasswordUseCase(credentials.login)
                .onSuccess {
                    _uiState.value = LoginUiState.ResetSuccess
                    _resetError.value = null
                }
                .onFailure { exception ->
                    val errorMessage =
                        exception.message ?: "Неизвестная ошибка восстановления пароля"
                    _uiState.value = LoginUiState.ResetError(errorMessage)
                    _resetError.value = errorMessage
                }
        }
    }

    /**
     * Очищает все ошибки (loginError и resetError).
     * Возвращает состояние UI в Idle.
     */
    fun clearErrors() {
        _loginError.value = null
        _resetError.value = null
    }

    /**
     * Сбрасывает состояние ViewModel для новой сессии авторизации.
     *
     * Вызывается при каждом открытии LoginSheet, чтобы очистить:
     * - учетные данные (логин и пароль)
     * - состояние UI (в Idle)
     * - ошибки авторизации и восстановления пароля
     *
     * Это предотвращает повторное использование старых данных и авто-логин при повторном открытии.
     */
    fun resetForNewSession() {
        // Сбросить учетные данные
        _login.value = ""
        _password.value = ""

        // Сбросить состояние UI в Idle
        _uiState.value = LoginUiState.Idle

        // Очистить ошибки
        clearErrors()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as
                            JetpackWorkoutApplication
                LoginViewModel(
                    logger = application.logger,
                    loginUseCase = application.container.loginUseCase,
                    resetPasswordUseCase = application.container.resetPasswordUseCase
                )
            }
        }
    }
}
