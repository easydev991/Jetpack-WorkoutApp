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
import com.swparks.ui.model.LoginCredentials
import com.swparks.ui.state.LoginEvent
import com.swparks.ui.state.LoginUiState
import com.swparks.util.Logger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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
 * @param userNotifier Интерфейс для обработки и отправки ошибок в UI-слой
 */
class LoginViewModel(
    private val logger: Logger,
    private val loginUseCase: ILoginUseCase,
    private val resetPasswordUseCase: IResetPasswordUseCase
) : ViewModel(), ILoginViewModel {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    override val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    override val loginErrorState: StateFlow<String?> = _loginError.asStateFlow()


    private val _resetError = MutableStateFlow<String?>(null)
    override val resetErrorState: StateFlow<String?> = _resetError.asStateFlow()

    // Добавляем канал для одноразовых событий
    private val _loginEvents = Channel<LoginEvent>(Channel.BUFFERED)
    override val loginEvents = _loginEvents.receiveAsFlow()

    private val _login = mutableStateOf("")
    private val _password = mutableStateOf("")

    override val credentials: LoginCredentials
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
    override fun onLoginChange(value: String) {
        _login.value = value
        clearErrors()
    }

    /**
     * Обновляет пароль пользователя.
     * При изменении пароля очищаются все ошибки.
     *
     * @param value Новый пароль пользователя
     */
    override fun onPasswordChange(value: String) {
        _password.value = value
        clearErrors()
    }

    /**
     * Выполняет вход в систему.
     *
     * Если учетные данные валидны, вызывает loginUseCase.
     * При успешном входе отправляет событие LoginEvent.Success через канал.
     * При ошибке авторизации сохраняет ошибку в loginError для отображения под полем пароля.
     *
     * ВАЖНО: Этот метод выполняет ТОЛЬКО авторизацию и сохраняет токен.
     * Загрузка данных пользователя выполняется в ProfileViewModel при открытии профиля.
     */
    override fun login() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            loginUseCase(credentials)
                .onSuccess { result ->
                    // ОТПРАВЛЯЕМ СОБЫТИЕ и сбрасываем UI в Idle
                    _loginEvents.send(LoginEvent.Success(userId = result.userId))

                    _uiState.value = LoginUiState.Idle
                    _loginError.value = null
                }
                .onFailure { exception ->
                    val errorMessage = exception.message ?: "Неизвестная ошибка авторизации"
                    _uiState.value = LoginUiState.LoginError(errorMessage, exception)
                    _loginError.value = errorMessage

                    // Не отправляем в userNotifier - ошибка отображается под полем пароля
                }
        }
    }

    /**
     * Выполняет восстановление пароля.
     *
     * Если логин пустой, не выполняет запрос (UI покажет алерт).
     * Если логин указан, вызывает resetPasswordUseCase.
     * При успешном восстановлении отправляет событие LoginEvent.ResetSuccess через канал.
     * При ошибке восстановления сохраняет ошибку в resetError для отображения под полем логина.
     */
    override fun resetPassword() {
        if (!credentials.canRestorePassword) {
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            resetPasswordUseCase(credentials.login)
                .onSuccess {
                    // ОТПРАВЛЯЕМ СОБЫТИЕ и сбрасываем UI в Idle
                    _loginEvents.send(LoginEvent.ResetSuccess(email = credentials.login))

                    _uiState.value = LoginUiState.Idle
                    _resetError.value = null
                }
                .onFailure { exception ->
                    val errorMessage =
                        exception.message ?: "Неизвестная ошибка восстановления пароля"
                    _uiState.value = LoginUiState.ResetError(errorMessage, exception)
                    _resetError.value = errorMessage

                    // Не отправляем в userNotifier - ошибка отображается под полем логина
                }
        }
    }

    /**
     * Очищает все ошибки (loginError и resetError).
     * Возвращает состояние UI в Idle.
     */
    override fun clearErrors() {
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
    override fun resetForNewSession() {
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
                    logger = application.container.logger,
                    loginUseCase = application.container.loginUseCase,
                    resetPasswordUseCase = application.container.resetPasswordUseCase
                )
            }
        }
    }
}
