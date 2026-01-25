package com.swparks.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.swparks.JetpackWorkoutApplication
import com.swparks.data.SecureTokenRepository
import com.swparks.data.repository.SWRepository
import com.swparks.domain.exception.NetworkException
import com.swparks.domain.usecase.ILoginUseCase
import com.swparks.domain.usecase.IResetPasswordUseCase
import com.swparks.model.LoginCredentials
import com.swparks.model.SocialUpdates
import com.swparks.ui.state.LoginUiState
import com.swparks.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * ViewModel для управления экраном авторизации.
 *
 * Управляет состоянием UI экрана входа в систему, включая учетные данные,
 * обработку ошибок авторизации и восстановления пароля.
 *
 * @param logger Логгер для записи сообщений
 * @param loginUseCase Use case для входа в систему
 * @param resetPasswordUseCase Use case для восстановления пароля
 * @param secureTokenRepository Репозиторий для безопасного хранения токена
 * @param swRepository Репозиторий для работы с API сервера
 */
class LoginViewModel(
    private val logger: Logger,
    private val loginUseCase: ILoginUseCase,
    private val resetPasswordUseCase: IResetPasswordUseCase,
    private val secureTokenRepository: SecureTokenRepository,
    private val swRepository: SWRepository,
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
     */
    fun login() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            loginUseCase(credentials)
                .onSuccess { result ->
                    _uiState.value = LoginUiState.LoginSuccess(
                        socialUpdates = null // SocialUpdates будут загружены отдельно
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
     * Выполняет авторизацию и загружает данные пользователя.
     *
     * Аналогично iOS-реализации (LoginScreen.swift:111-117):
     * 1. Авторизуется через loginUseCase
     * 2. Сохраняет токен через secureTokenRepository
     * 3. Получает данные пользователя через swRepository.getSocialUpdates(userId) с retry логикой
     * 4. Возвращает Result<SocialUpdates> для передачи в ProfileScreen
     *
     * Retry логика добавлена для обработки временных ошибок сервера (502, 503, 504),
     * которые могут возникать при повторной авторизации после выхода.
     *
     * @return Result<SocialUpdates> с данными пользователя или ошибкой
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun loginAndLoadUserData(): Result<SocialUpdates> {
        return try {
            // 1. Авторизация через loginUseCase
            val authResult = loginUseCase(credentials).getOrThrow()
            val userId = authResult.userId

            logger.i("LoginViewModel", "Авторизация успешна, userId: $userId")

            // Токен уже сохранен в loginUseCase (через SecureTokenRepository)

            // 3. Загрузка данных пользователя с retry логикой
            val socialUpdates = loadUserDataWithRetry(userId)
            socialUpdates.onSuccess {
                logger.i("LoginViewModel", "Данные пользователя успешно загружены")
            }

            // 4. Вернуть результат для передачи в ProfileScreen
            socialUpdates
        } catch (e: IOException) {
            logger.e(
                "LoginViewModel",
                "Ошибка сети при загрузке данных пользователя: ${e.message}",
                e
            )
            Result.failure(
                NetworkException(
                    "Ошибка сети при загрузке данных пользователя: ${e.message}",
                    e
                )
            )
        } catch (e: Exception) {
            // Общий catch для обработки всех неожиданных ошибок
            logger.e("LoginViewModel", "Ошибка при загрузке данных пользователя: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Загружает данные пользователя с retry логикой.
     *
     * Повторяет запрос до 3 раз при ошибках сервера (502, 503, 504)
     * или ошибках сети. Это помогает обработать временные проблемы
     * при повторной авторизации после выхода.
     *
     * @param userId Идентификатор пользователя
     * @return Result<SocialUpdates> с данными пользователя или ошибкой
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadUserDataWithRetry(userId: Long): Result<SocialUpdates> {
        var lastError: Exception? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                val socialUpdates = swRepository.getSocialUpdates(userId).getOrThrow()
                logger.i("LoginViewModel", "Данные пользователя загружены с попытки ${attempt + 1}")
                return Result.success(socialUpdates)
            } catch (e: retrofit2.HttpException) {
                val statusCode = e.code()
                if (statusCode == HTTP_BAD_GATEWAY ||
                    statusCode == HTTP_SERVICE_UNAVAILABLE ||
                    statusCode == HTTP_GATEWAY_TIMEOUT
                ) {
                    logger.w(
                        "LoginViewModel",
                        "Ошибка сервера $statusCode при загрузке данных пользователя, " +
                                "попытка ${attempt + 1}/$MAX_RETRIES"
                    )
                    lastError = e
                    if (attempt < MAX_RETRIES - 1) {
                        kotlinx.coroutines.delay(RETRY_DELAY_MS)
                    }
                } else {
                    // Для других кодов ошибок HTTP выкидываем исключение сразу
                    throw e
                }
            } catch (e: IOException) {
                logger.w(
                    "LoginViewModel",
                    "Ошибка сети при загрузке данных пользователя, " +
                            "попытка ${attempt + 1}/$MAX_RETRIES: ${e.message}"
                )
                lastError = e
                if (attempt < MAX_RETRIES - 1) {
                    kotlinx.coroutines.delay(RETRY_DELAY_MS)
                }
            }
        }

        // Если все попытки неудачны, возвращаем последнюю ошибку
        val errorMessage = when (lastError) {
            is retrofit2.HttpException -> "Не удалось загрузить данные пользователя " +
                    "после $MAX_RETRIES попыток. Ошибка сервера ${lastError.code()}"

            is IOException -> "Не удалось загрузить данные пользователя. " +
                    "Проверьте интернет-соединение."

            else -> "Не удалось загрузить данные пользователя после $MAX_RETRIES попыток"
        }
        logger.e("LoginViewModel", errorMessage, lastError)
        return Result.failure(NetworkException(errorMessage, lastError))
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

    companion object {
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
        private const val HTTP_BAD_GATEWAY = 502
        private const val HTTP_SERVICE_UNAVAILABLE = 503
        private const val HTTP_GATEWAY_TIMEOUT = 504

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as
                            JetpackWorkoutApplication
                LoginViewModel(
                    logger = application.logger,
                    loginUseCase = application.container.loginUseCase,
                    resetPasswordUseCase = application.container.resetPasswordUseCase,
                    secureTokenRepository = application.container.secureTokenRepository,
                    swRepository = application.container.swRepository
                )
            }
        }
    }
}
