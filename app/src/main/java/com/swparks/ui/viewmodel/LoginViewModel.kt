package com.swparks.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.swparks.JetpackWorkoutApplication
import com.swparks.analytics.AnalyticsEvent
import com.swparks.analytics.AnalyticsService
import com.swparks.analytics.AppErrorOperation
import com.swparks.analytics.UserActionType
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
    private val resetPasswordUseCase: IResetPasswordUseCase,
    private val analyticsService: AnalyticsService
) : ViewModel(),
    ILoginViewModel {
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    override val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _loginErrorState = MutableStateFlow<String?>(null)
    override val loginErrorState: StateFlow<String?> = _loginErrorState.asStateFlow()

    private val _resetErrorState = MutableStateFlow<String?>(null)
    override val resetErrorState: StateFlow<String?> = _resetErrorState.asStateFlow()

    // Добавляем канал для одноразовых событий
    private val loginEventsChannel = Channel<LoginEvent>(Channel.BUFFERED)
    override val loginEvents = loginEventsChannel.receiveAsFlow()

    private val _credentialsState = MutableStateFlow(LoginCredentials())
    override val credentialsState: StateFlow<LoginCredentials> = _credentialsState.asStateFlow()

    /**
     * Обновляет логин пользователя.
     * При изменении логина очищаются все ошибки.
     *
     * @param value Новый логин пользователя
     */
    override fun onLoginChange(value: String) {
        _credentialsState.value = _credentialsState.value.copy(login = value)
        clearErrors()
    }

    /**
     * Обновляет пароль пользователя.
     * При изменении пароля очищаются все ошибки.
     *
     * @param value Новый пароль пользователя
     */
    override fun onPasswordChange(value: String) {
        _credentialsState.value = _credentialsState.value.copy(password = value)
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
        analyticsService.log(AnalyticsEvent.UserAction(UserActionType.LOGIN))

        val credentials = _credentialsState.value

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            loginUseCase(credentials)
                .onSuccess { result ->
                    // ОТПРАВЛЯЕМ СОБЫТИЕ и сбрасываем UI в Idle
                    loginEventsChannel.send(LoginEvent.Success(userId = result.userId))

                    _uiState.value = LoginUiState.Idle
                    _loginErrorState.value = null
                }.onFailure { exception ->
                    val errorMessage = exception.message ?: "Неизвестная ошибка авторизации"
                    analyticsService.log(
                        AnalyticsEvent.AppError(AppErrorOperation.LOGIN_FAILED, exception)
                    )
                    _uiState.value = LoginUiState.LoginError(errorMessage, exception)
                    _loginErrorState.value = errorMessage

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
        val credentials = _credentialsState.value

        if (!credentials.canRestorePassword) {
            return
        }

        analyticsService.log(AnalyticsEvent.UserAction(UserActionType.RESET_PASSWORD))

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            resetPasswordUseCase(credentials.login)
                .onSuccess {
                    // ОТПРАВЛЯЕМ СОБЫТИЕ и сбрасываем UI в Idle
                    loginEventsChannel.send(LoginEvent.ResetSuccess(email = credentials.login))

                    _uiState.value = LoginUiState.Idle
                    _resetErrorState.value = null
                }.onFailure { exception ->
                    val errorMessage =
                        exception.message ?: "Неизвестная ошибка восстановления пароля"
                    analyticsService.log(
                        AnalyticsEvent.AppError(AppErrorOperation.PASSWORD_RESET_FAILED, exception)
                    )
                    _uiState.value = LoginUiState.ResetError(errorMessage, exception)
                    _resetErrorState.value = errorMessage

                    // Не отправляем в userNotifier - ошибка отображается под полем логина
                }
        }
    }

    /**
     * Очищает все ошибки (loginError и resetError).
     * Возвращает состояние UI в Idle.
     */
    override fun clearErrors() {
        _loginErrorState.value = null
        _resetErrorState.value = null
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
        _credentialsState.value = LoginCredentials()

        // Сбросить состояние UI в Idle
        _uiState.value = LoginUiState.Idle

        // Очистить ошибки
        clearErrors()
    }

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val application =
                        this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as
                            JetpackWorkoutApplication
                    LoginViewModel(
                        logger = application.container.logger,
                        loginUseCase = application.container.loginUseCase,
                        resetPasswordUseCase = application.container.resetPasswordUseCase,
                        analyticsService = application.container.analyticsService
                    )
                }
            }
    }
}
