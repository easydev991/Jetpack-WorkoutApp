# Анализ и план реализации универсального механизма обработки ошибок

## Обзор

### Проблема

В текущей архитектуре каждая ViewModel должна отдельно реализовывать логику для:
1. Логирования ошибок
2. Отправки ошибок в UI через Channel/SharedFlow
3. Обработки ошибок в UI компонентах для отображения пользователю

Это приводит к:
- Дублированию кода в разных ViewModels
- Рискам пропустить обработку ошибок на каком-то экране
- Разным UX для ошибок на разных экранах

### Цель

Создать единый механизм для отображения ошибок пользователям, который:
- Централизованно обрабатывает ошибки из всех ViewModels
- Показывает уведомления (Snackbar/Toast/Alert) поверх текущего экрана
- Обеспечивает консистентный UX для всех ошибок приложения
- Легко интегрируется в существующую архитектуру через DI
- Не нарушает существующие паттерны создания ViewModels

---

## Анализ текущей архитектуры

### Существующие паттерны

#### 1. AppState - глобальное состояние приложения

**Файл:** `app/src/main/java/com/swparks/navigation/AppState.kt`

**Текущая функциональность:**
- Управление навигацией (`navController`)
- Состояние авторизации (`currentUser`, `isAuthorized`)
- Метод `updateCurrentUser()` для реактивного обновления UI

**ВАЖНО:** AppState - это Composable-создаваемое состояние (через `rememberAppState()`), ориентированное на UI-слой. Добавление в него шины ошибок создаст сильную связность между ViewModel и UI-слоем, что нарушит текущую архитектуру.

#### 2. Channel/SharedFlow для одноразовых событий

**Пример из LoginViewModel:**

```kotlin
private val _loginEvents = Channel<LoginEvent>(Channel.BUFFERED)
val loginEvents = _loginEvents.receiveAsFlow()

// Использование в UI
LaunchedEffect(Unit) {
    viewModel.loginEvents.collect { event ->
        when (event) {
            is LoginEvent.Success -> { /* ... */ }
        }
    }
}
```

**Проблема:** Этот паттерн требует копирования на каждый экран.

#### 3. Логирование без UI

**Пример из FriendsListViewModel:**

```kotlin
swRepository.respondToFriendRequest(userId, accept = true)
    .onSuccess {
        logger.i(TAG, "Заявка успешно принята: userId=$userId")
    }
    .onFailure { error ->
        logger.e(TAG, "Ошибка принятия заявки: ${error.message}")
        // ❌ Ошибка только логируется, но не показывается пользователю
    }
```

#### 4. DI контейнер для ViewModels

**Файл:** `app/src/main/java/com/swparks/data/AppContainer.kt`

**Текущая архитектура:**

```kotlin
interface AppContainer {
    fun profileViewModelFactory(): ProfileViewModel
    fun friendsListViewModelFactory(): FriendsListViewModel
    // ...
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val logger: Logger = AndroidLogger()

    override fun profileViewModelFactory() = ProfileViewModel(
        countriesRepository = countriesRepository,
        swRepository = swRepository,
        logger = logger
    )

    override fun friendsListViewModelFactory() = FriendsListViewModel(
        userDao = userDao,
        swRepository = swRepository,
        logger = logger
    )
}
```

**Преимущество:** Легко добавить новый параметр (ErrorReporter) во все ViewModels через factory методы.

---

## Рекомендуемый подход: ErrorReporter + SharedFlow

### Архитектура

```
┌─────────────────────────────────────────────────────────┐
│                     ViewModels                          │
│  (FriendsListViewModel, LoginViewModel, etc.)          │
│                                                         │
│  swRepository.method()                                  │
│      .onFailure { error ->                              │
│          errorReporter.handleError(AppError.Network(...)) │
│      }                                                  │
└───────────────────────┬─────────────────────────────────┘
                        │
                        ↓ handleError()
┌─────────────────────────────────────────────────────────┐
│                 ErrorHandler : ErrorReporter              │
│                                                         │
│  - logger: Logger                                       │
│  + errorFlow: SharedFlow<AppError>                      │
│  + handleError(error: AppError)  // tryEmit без launch  │
└───────────────────────┬─────────────────────────────────┘
                        │
                        ↓ emit(error)
┌─────────────────────────────────────────────────────────┐
│                      RootScreen                         │
│                                                         │
│  LaunchedEffect(Unit) {                                │
│      errorReporter.errorFlow.collect { error ->         │
│          val message = error.toMessage()               │
│          snackbarHostState.showSnackbar(message)       │
│      }                                                  │
│  }                                                      │
└───────────────────────┬─────────────────────────────────┘
                        │
                        ↓ showSnackbar()
┌─────────────────────────────────────────────────────────┐
│                 SnackbarHostState                       │
│                                                         │
│  Показывает Snackbar поверх текущего экрана            │
└─────────────────────────────────────────────────────────┘
```

### Преимущества подхода

1. **Чистое разделение ответственности**
   - AppState остается только для навигации и авторизации
   - ErrorReporter - отдельный интерфейс для обработки ошибок
   - ErrorHandler - реализация интерфейса с логикой
   - Нет смешения UI-state и business-logic concerns

2. **Легкая интеграция в текущую архитектуру**
   - ErrorReporter размещается рядом с Logger и другими сервисами
   - Передается в ViewModels через существующие factory методы в AppContainer
   - Не требует изменений в AppState

3. **Простота использования в ViewModels**
   - Использует `tryEmit` без необходимости в `launch` и scope
   - `MutableSharedFlow` с `extraBufferCapacity` обеспечивает надежную отправку
   - ViewModel может вызывать `handleError` напрямую из корутины

4. **Расширяемость**
   - Логирование ошибок в одном месте
   - Легко добавить аналитику (Firebase, Crashlytics)
   - Можно добавить историю ошибок для отладки

5. **Тестируемость**
   - Легко мокать ErrorReporter в тестах ViewModels (интерфейс)
   - Отдельные unit-тесты для ErrorHandler
   - Нет зависимости от AppState и Compose

### Реализация ErrorReporter и ErrorHandler

```kotlin
// ErrorReporter.kt
/**
 * Интерфейс для обработки и отправки ошибок в UI-слой.
 *
 * Используется во всех ViewModels для централизованной обработки ошибок.
 * Реализация (ErrorHandler) логирует ошибки и отправляет их в SharedFlow.
 */
interface ErrorReporter {
    /**
     * Поток ошибок для подписки из UI-слоя.
     */
    val errorFlow: SharedFlow<AppError>

    /**
     * Обрабатывает ошибку: логирует и отправляет в поток.
     *
     * @param error Ошибка для обработки
     * @return true если ошибка успешно отправлена, false если буфер переполнен
     */
    fun handleError(error: AppError): Boolean
}

// ErrorHandler.kt
/**
 * Реализация интерфейса ErrorReporter.
 *
 * Логирует все ошибки и отправляет их в SharedFlow для отображения в UI.
 */
class ErrorHandler(
    private val logger: Logger,
) : ErrorReporter {
    private companion object {
        private const val TAG = "ErrorHandler"
        private const val ERROR_BUFFER_CAPACITY = 10
    }

    /**
     * Поток ошибок для подписки из UI-слоя.
     * Использует SharedFlow для поддержки множества подписчиков.
     */
    private val _errorFlow = MutableSharedFlow<AppError>(
        extraBufferCapacity = ERROR_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * Публичный поток для подписки из UI-слоя.
     */
    override val errorFlow: SharedFlow<AppError> = _errorFlow.asSharedFlow()

    /**
     * Обрабатывает ошибку: логирует и отправляет в поток.
     *
     * Использует tryEmit для неблокирующей отправки без необходимости в launch.
     * Если буфер переполнен, старые ошибки отбрасываются.
     *
     * @param error Ошибка для обработки
     * @return true если ошибка успешно отправлена, false если буфер переполнен
     */
    override fun handleError(error: AppError): Boolean {
        // Логируем ошибку
        when (error) {
            is AppError.Network -> {
                logger.e(TAG, "Сетевая ошибка: ${error.message}", error.throwable)
            }
            is AppError.Validation -> {
                logger.w(TAG, "Ошибка валидации: ${error.message}")
            }
            is AppError.Server -> {
                logger.e(TAG, "Ошибка сервера (${error.code}): ${error.message}")
            }
            is AppError.Generic -> {
                logger.e(TAG, "Общая ошибка: ${error.message}", error.throwable)
            }
        }

        // Отправляем в поток (неблокирующая операция)
        return _errorFlow.tryEmit(error)
    }
}

// AppError.kt
/**
 * Модель ошибки приложения.
 *
 * Разные типы ошибок для различной обработки в UI и логировании.
 */
sealed class AppError {
    /**
     * Сетевая ошибка (отсутствие подключения, таймаут и т.д.)
     */
    data class Network(
        val message: String,
        val throwable: Throwable? = null
    ) : AppError()

    /**
     * Ошибка валидации пользовательского ввода
     */
    data class Validation(
        val message: String,
        val field: String? = null
    ) : AppError()

    /**
     * Ошибка сервера (4xx, 5xx статусы)
     */
    data class Server(
        val message: String,
        val code: Int? = null
    ) : AppError()

    /**
     * Общая ошибка (непредвиденная)
     */
    data class Generic(
        val message: String,
        val throwable: Throwable? = null
    ) : AppError()
}
```

### Использование в ViewModel

```kotlin
// FriendsListViewModel.kt
class FriendsListViewModel(
    private val userDao: UserDao,
    private val swRepository: SWRepository,
    private val logger: Logger,
    private val errorReporter: ErrorReporter,  // ✅ Интерфейс через DI
) : ViewModel() {

    fun onAcceptFriendRequest(userId: Long) {
        viewModelScope.launch {
            _uiState.value = FriendsListUiState.Busy

            logger.i(TAG, "Принятие заявки на добавление в друга: userId=$userId")

            swRepository.respondToFriendRequest(userId, accept = true)
                .onSuccess {
                    logger.i(TAG, "Заявка успешно принята: userId=$userId")
                }
                .onFailure { error ->
                    // ✅ Отправляем ошибку через ErrorReporter
                    errorReporter.handleError(
                        AppError.Network(
                            message = "Не удалось принять заявку. Проверьте подключение к интернету.",
                            throwable = error
                        )
                    )
                }

            _uiState.value = lastSuccessState.value ?: FriendsListUiState.Success()
        }
    }
}
```

### Интеграция в AppContainer

```kotlin
// AppContainer.kt (реальный путь: app/src/main/java/com/swparks/data/AppContainer.kt)
interface AppContainer {
    // Сервисы
    val logger: Logger
    val errorReporter: ErrorReporter  // ✅ Интерфейс

    // Factory методы для ViewModels
    fun profileViewModelFactory(): ProfileViewModel
    fun friendsListViewModelFactory(): FriendsListViewModel
    // ... другие ViewModels
}

// DefaultAppContainer.kt
class DefaultAppContainer(context: Context) : AppContainer {
    // Логгер
    private val logger: Logger = AndroidLogger()

    // ErrorHandler (реализует ErrorReporter)
    override val errorReporter: ErrorReporter = ErrorHandler(logger)

    override fun profileViewModelFactory() = ProfileViewModel(
        countriesRepository = countriesRepository,
        swRepository = swRepository,
        logger = logger,
        errorReporter = errorReporter  // ✅ Передаем через DI
    )

    override fun friendsListViewModelFactory() = FriendsListViewModel(
        userDao = userDao,
        swRepository = swRepository,
        logger = logger,
        errorReporter = errorReporter  // ✅ Передаем через DI
    )
}
```

### Отображение ошибок в RootScreen

```kotlin
// RootScreen.kt
@Composable
fun RootScreen(
    appState: AppState,
    appContainer: AppContainer
) {
    // Состояние для Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // Собираем ошибки из ErrorReporter
    LaunchedEffect(Unit) {
        appContainer.errorReporter.errorFlow.collect { error ->
            // Преобразуем ошибку в сообщение для пользователя
            val message = when (error) {
                is AppError.Network -> error.message
                is AppError.Validation -> error.message
                is AppError.Server -> error.message
                is AppError.Generic -> error.message
            }

            // Показываем Snackbar
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
        }
    }

    Scaffold(
        topBar = {
            // ... TopAppBar логика ...
        },
        bottomBar = {
            BottomNavigationBar(appState = appState)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },  // ✅ Добавляем SnackbarHost
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->
        NavHost(
            navController = appState.navController,
            startDestination = Screen.Parks.route,
            modifier = Modifier.fillMaxSize(),
        ) {
            // ... экраны ...
        }
    }
}
```

---

## План реализации

### Этап 1: Создание модели ошибок (AppError)

**Задачи:**
1. Создать sealed class `AppError` в `app/src/main/java/com/swparks/model/`
2. Определить типы ошибок: Network, Validation, Server, Generic
3. Добавить свойства для каждого типа ошибок
4. Написать unit-тесты для AppError

**Файлы:**
- `app/src/main/java/com/swparks/model/AppError.kt` (создать)
- `app/src/test/java/com/swparks/model/AppErrorTest.kt` (создать)

**Unit-тесты:**

```kotlin
class AppErrorTest {
    @Test
    fun testNetworkError() {
        val error = AppError.Network("Нет подключения")
        assertTrue(error is AppError.Network)
        assertEquals("Нет подключения", error.message)
        assertNull(error.throwable)
    }

    @Test
    fun testValidationErrorWithField() {
        val error = AppError.Validation("Обязательное поле", field = "email")
        assertEquals("Обязательное поле", error.message)
        assertEquals("email", error.field)
    }

    @Test
    fun testServerErrorWithCode() {
        val error = AppError.Server("Внутренняя ошибка сервера", code = 500)
        assertEquals(500, error.code)
    }
}
```

---

### Этап 2: Создание интерфейса ErrorReporter и реализация ErrorHandler

**Задачи:**
1. Создать интерфейс `ErrorReporter` в `app/src/main/java/com/swparks/util/`
2. Создать класс `ErrorHandler` с реализацией `ErrorReporter`
3. Использовать `MutableSharedFlow` с `extraBufferCapacity`
4. Реализовать метод `handleError()` с использованием `tryEmit`
5. Логировать все ошибки через Logger
6. Написать unit-тесты для ErrorHandler

**Файлы:**
- `app/src/main/java/com/swparks/util/ErrorReporter.kt` (создать)
- `app/src/main/java/com/swparks/util/ErrorHandler.kt` (создать)
- `app/src/test/java/com/swparks/util/ErrorHandlerTest.kt` (создать)

**Unit-тесты:**

```kotlin
class ErrorHandlerTest {
    private lateinit var mockLogger: Logger
    private lateinit var errorHandler: ErrorHandler

    @Before
    fun setUp() {
        mockLogger = mockk()
        errorHandler = ErrorHandler(mockLogger)
    }

    @Test
    fun `handleError logs network error`() = runTest {
        val error = AppError.Network("Нет подключения")

        errorHandler.handleError(error)

        verify { mockLogger.e(any(), any(), any()) }
    }

    @Test
    fun `handleError emits error to flow`() = runTest {
        val error = AppError.Generic("Ошибка")

        errorHandler.handleError(error)

        val emittedError = errorHandler.errorFlow.first()
        assertEquals(error, emittedError)
    }

    @Test
    fun `handleError returns true when emitted successfully`() {
        val error = AppError.Generic("Ошибка")

        val result = errorHandler.handleError(error)

        assertTrue(result)
    }
}
```

---

### Этап 3: Интеграция ErrorReporter в AppContainer

**Задачи:**
1. Обновить интерфейс `AppContainer` с добавлением `errorReporter: ErrorReporter`
2. Создать экземпляр `ErrorHandler` в `DefaultAppContainer`
3. Обновить все factory методы ViewModels для передачи `errorReporter`
4. Обновить тесты AppContainer для проверки интеграции

**Файлы:**
- `app/src/main/java/com/swparks/data/AppContainer.kt` (обновить интерфейс и реализацию)
- `app/src/test/java/com/swparks/data/DefaultAppContainerTest.kt` (обновить)

**Пример обновления factory метода:**

```kotlin
// В DefaultAppContainer
override val logger: Logger = AndroidLogger()
override val errorReporter: ErrorReporter = ErrorHandler(logger)

override fun friendsListViewModelFactory() = FriendsListViewModel(
    userDao = userDao,
    swRepository = swRepository,
    logger = logger,
    errorReporter = errorReporter  // ✅ Добавлено
)

override fun profileViewModelFactory() = ProfileViewModel(
    countriesRepository = countriesRepository,
    swRepository = swRepository,
    logger = logger,
    errorReporter = errorReporter  // ✅ Добавлено
)
```

---

### Этап 4: Обновление FriendsListViewModel

**Задачи:**
1. Добавить `errorReporter: ErrorReporter` в конструктор `FriendsListViewModel`
2. Заменить логирование ошибок на вызов `errorReporter.handleError()`
3. Обновить методы `onAcceptFriendRequest()` и `onDeclineFriendRequest()`
4. Обновить unit-тесты с mock ErrorReporter

**Файлы:**
- `app/src/main/java/com/swparks/viewmodel/FriendsListViewModel.kt` (обновить)
- `app/src/test/java/com/swparks/viewmodel/FriendsListViewModelTest.kt` (обновить)

**Пример изменений:**

```kotlin
class FriendsListViewModel(
    private val userDao: UserDao,
    private val swRepository: SWRepository,
    private val logger: Logger,
    private val errorReporter: ErrorReporter,  // ✅ Интерфейс через DI
) : ViewModel() {

    fun onAcceptFriendRequest(userId: Long) {
        viewModelScope.launch {
            _uiState.value = FriendsListUiState.Busy

            logger.i(TAG, "Принятие заявки на добавление в друга: userId=$userId")

            swRepository.respondToFriendRequest(userId, accept = true)
                .onSuccess {
                    logger.i(TAG, "Заявка успешно принята: userId=$userId")
                }
                .onFailure { error ->
                    // ✅ Обработка через ErrorReporter
                    errorReporter.handleError(
                        AppError.Network(
                            message = "Не удалось принять заявку. Проверьте подключение к интернету.",
                            throwable = error
                        )
                    )
                }

            _uiState.value = lastSuccessState.value ?: FriendsListUiState.Success()
        }
    }

    fun onDeclineFriendRequest(userId: Long) {
        viewModelScope.launch {
            _uiState.value = FriendsListUiState.Busy

            logger.i(TAG, "Отклонение заявки на добавление в друга: userId=$userId")

            swRepository.respondToFriendRequest(userId, accept = false)
                .onSuccess {
                    logger.i(TAG, "Заявка успешно отклонена: userId=$userId")
                }
                .onFailure { error ->
                    // ✅ Обработка через ErrorReporter
                    errorReporter.handleError(
                        AppError.Network(
                            message = "Не удалось отклонить заявку. Проверьте подключение к интернету.",
                            throwable = error
                        )
                    )
                }

            _uiState.value = lastSuccessState.value ?: FriendsListUiState.Success()
        }
    }
}
```

**Пример обновления unit-тестов:**

```kotlin
class FriendsListViewModelTest {
    private lateinit var mockLogger: Logger
    private lateinit var mockErrorReporter: ErrorReporter

    private lateinit var viewModel: FriendsListViewModel

    @Before
    fun setUp() {
        mockLogger = mockk()
        mockErrorReporter = mockk(relaxed = true)  // ✅ Мок интерфейса

        viewModel = FriendsListViewModel(
            userDao = mockk(),
            swRepository = mockk(),
            logger = mockLogger,
            errorReporter = mockErrorReporter  // ✅ Параметр назван правильно
        )
    }

    @Test
    fun `onAcceptFriendRequest emits error on failure`() = runTest {
        // Given
        val testUserId = 123L
        val testError = IOException("Нет подключения")
        coEvery { mockSwRepository.respondToFriendRequest(testUserId, true) }
            .returns(Result.failure(testError))

        // When
        viewModel.onAcceptFriendRequest(testUserId)

        // Then
        verify {
            mockErrorReporter.handleError(
                match<AppError> {
                    it is AppError.Network && it.message.contains("Не удалось принять заявку")
                }
            )
        }
    }
}
```

---

### Этап 5: Обновление LoginViewModel

**Задачи:**
1. Добавить `errorReporter: ErrorReporter` в конструктор `LoginViewModel`
2. Заменить ошибки валидации и сетевые ошибки на `errorReporter.handleError()`
3. Обновить методы `login()` и `resetPassword()`
4. Обновить unit-тесты с mock ErrorReporter

**Файлы:**
- `app/src/main/java/com/swparks/ui/viewmodel/LoginViewModel.kt` (обновить)
- `app/src/test/java/com/swparks/ui/viewmodel/LoginViewModelTest.kt` (обновить)

**Пример изменений:**

```kotlin
class LoginViewModel(
    private val logger: Logger,
    private val loginUseCase: ILoginUseCase,
    private val resetPasswordUseCase: IResetPasswordUseCase,
    private val errorReporter: ErrorReporter,  // ✅ Интерфейс через DI
) : ViewModel() {

    fun login() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            loginUseCase(credentials)
                .onSuccess { result ->
                    _loginEvents.send(LoginEvent.Success(userId = result.userId))
                    _uiState.value = LoginUiState.Idle
                    _loginError.value = null
                }
                .onFailure { exception ->
                    val errorMessage = exception.message ?: "Неизвестная ошибка авторизации"
                    _uiState.value = LoginUiState.LoginError(errorMessage, exception)
                    _loginError.value = errorMessage

                    // ✅ Дополнительная обработка через ErrorReporter
                    errorReporter.handleError(
                        AppError.Network(
                            message = "Не удалось войти. Проверьте подключение к интернету.",
                            throwable = exception
                        )
                    )
                }
        }
    }

    fun resetPassword() {
        if (!credentials.canRestorePassword) {
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            resetPasswordUseCase(credentials.login)
                .onSuccess {
                    _loginEvents.send(LoginEvent.ResetSuccess(email = credentials.login))
                    _uiState.value = LoginUiState.Idle
                    _resetError.value = null
                }
                .onFailure { exception ->
                    val errorMessage =
                        exception.message ?: "Неизвестная ошибка восстановления пароля"
                    _uiState.value = LoginUiState.ResetError(errorMessage, exception)
                    _resetError.value = errorMessage

                    // ✅ Дополнительная обработка через ErrorReporter
                    errorReporter.handleError(
                        AppError.Network(
                            message = "Не удалось восстановить пароль. Проверьте подключение к интернету.",
                            throwable = exception
                        )
                    )
                }
        }
    }
}
```

---

### Этап 6: Реализация Snackbar в RootScreen

**Задачи:**
1. Добавить `SnackbarHostState` в RootScreen
2. Собирать ошибки из `appContainer.errorReporter.errorFlow` через `LaunchedEffect(Unit)`
3. Показывать Snackbar при получении ошибки
4. Добавить логирование для отладки (опционально)
5. Написать инструментальные тесты для Snackbar

**Файлы:**
- `app/src/main/java/com/swparks/ui/screens/RootScreen.kt` (обновить)
- `app/src/androidTest/java/com/swparks/ui/screens/RootScreenTest.kt` (создать)

**Реализация:**

```kotlin
@Composable
fun RootScreen(
    appState: AppState,
    appContainer: AppContainer
) {
    // Состояние для Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // Собираем ошибки из ErrorReporter
    LaunchedEffect(Unit) {
        appContainer.errorReporter.errorFlow.collect { error ->
            // Преобразуем ошибку в сообщение для пользователя
            val message = when (error) {
                is AppError.Network -> error.message
                is AppError.Validation -> error.message
                is AppError.Server -> error.message
                is AppError.Generic -> error.message
            }

            // Показываем Snackbar
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
        }
    }

    Scaffold(
        topBar = {
            // ... TopAppBar логика ...
        },
        bottomBar = {
            BottomNavigationBar(appState = appState)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },  // ✅ Добавляем SnackbarHost
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->
        NavHost(
            navController = appState.navController,
            startDestination = Screen.Parks.route,
            modifier = Modifier.fillMaxSize(),
        ) {
            // ... экраны ...
        }
    }
}
```

**Инструментальные тесты:**

```kotlin
@RunWith(AndroidJUnit4::class)
class RootScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun snackbarShowsWhenErrorOccurs() {
        var errorReporter: ErrorReporter? = null
        val container = mockk<AppContainer>(relaxed = true)

        composeTestRule.setContent {
            errorReporter = container.errorReporter
            RootScreen(
                appState = rememberAppState(),
                appContainer = container
            )
        }

        // Отправляем ошибку
        errorReporter?.handleError(
            AppError.Network("Нет подключения к интернету")
        )

        // Проверяем, что Snackbar отображается
        composeTestRule.onNodeWithText("Нет подключения к интернету").assertIsDisplayed()
    }
}
```

---

### Этап 7: Миграция других ViewModels

**Задачи:**
1. Обновить ProfileViewModel для использования errorReporter
2. Обновить EventsViewModel для использования errorReporter
3. Обновить другие ViewModels по необходимости
4. Обновить unit-тесты для всех ViewModels

**Файлы:**
- `app/src/main/java/com/swparks/viewmodel/ProfileViewModel.kt` (обновить)
- `app/src/main/java/com/swparks/ui/screens/events/EventsViewModel.kt` (обновить)
- Unit-тесты для всех ViewModels (обновить)

---

### Этап 8: Локализация сообщений об ошибках

**Задачи:**
1. Создать string ресурсы для стандартных сообщений об ошибках
2. Создать расширение `AppError.toUiText()` для маппинга на локализованные строки
3. Добавить поддержку параметризованных сообщений (с данными)
4. Обновить RootScreen для использования локализованных сообщений

**Файлы:**
- `app/src/main/res/values/strings.xml` (добавить error_*)
- `app/src/main/res/values/ru/strings.xml` (добавить переводы)
- `app/src/main/java/com/swparks/model/AppErrorExt.kt` (создать)

**Маппинг ошибок на строки (extension-функция):**

```kotlin
// AppErrorExt.kt - extension-функция для AppError
/**
 * Расширение для преобразования AppError в локализованное сообщение.
 *
 * Использует @StringRes для получения локализованных строк.
 */
fun AppError.toUiText(context: Context): String {
    return when (this) {
        is AppError.Network -> {
            when {
                throwable is IOException -> {
                    context.getString(R.string.error_network_io)
                }
                else -> {
                    context.getString(R.string.error_network_general)
                }
            }
        }
        is AppError.Validation -> {
            if (field != null) {
                context.getString(R.string.error_validation_field, field)
            } else {
                message
            }
        }
        is AppError.Server -> {
            when (code) {
                401 -> context.getString(R.string.error_server_unauthorized)
                403 -> context.getString(R.string.error_server_forbidden)
                404 -> context.getString(R.string.error_server_not_found)
                500 -> context.getString(R.string.error_server_internal)
                else -> context.getString(R.string.error_server_general)
            }
        }
        is AppError.Generic -> message
    }
}
```

**Использование в RootScreen:**

```kotlin
@Composable
fun RootScreen(
    appState: AppState,
    appContainer: AppContainer
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        appContainer.errorReporter.errorFlow.collect { error ->
            // ✅ Используем локализованное сообщение (extension-функция)
            val message = error.toUiText(context)

            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
        }
    }

    // ... остальное ...
}
```

**Строковые ресурсы:**

```xml
<!-- strings.xml -->
<resources>
    <!-- Network errors -->
    <string name="error_network_io">Ошибка сети. Проверьте подключение к интернету.</string>
    <string name="error_network_general">Произошла сетевая ошибка. Попробуйте позже.</string>

    <!-- Validation errors -->
    <string name="error_validation_field">Поле %s обязательно для заполнения.</string>
    <string name="error_validation_email">Некорректный email адрес.</string>
    <string name="error_validation_password">Пароль должен содержать минимум 6 символов.</string>

    <!-- Server errors -->
    <string name="error_server_unauthorized">Требуется авторизация.</string>
    <string name="error_server_forbidden">Нет доступа к ресурсу.</string>
    <string name="error_server_not_found">Ресурс не найден.</string>
    <string name="error_server_internal">Ошибка сервера. Попробуйте позже.</string>
    <string name="error_server_general">Произошла ошибка на сервере.</string>

    <!-- Generic errors -->
    <string name="error_unknown">Произошла неизвестная ошибка.</string>
</resources>
```

---

### Этап 9: Тестирование на устройстве

**Задачи:**
1. Собрать проект (`./gradlew assembleDebug`)
2. Запустить приложение на устройстве/эмуляторе
3. Протестировать сценарии с ошибками сети:
   - Принятие/отклонение заявки без интернета
   - Загрузка данных без интернета
   - Ошибки сервера
   - Ошибки валидации
4. Проверить, что ошибки корректно отображаются через Snackbar
5. Проверить, что UI не крашится при ошибках
6. Проверить, что несколько ошибок корректно отображаются по очереди
7. Проверить, что буфер ошибок работает корректно (DROP_OLDEST)
8. Проверить локализацию сообщений (переключение языка)

**Чек-лист тестирования:**
- [ ] Отключить интернет, попытаться принять заявку → Snackbar с ошибкой сети
- [ ] Отключить интернет, попытаться войти → Snackbar с ошибкой сети
- [ ] Войти с неверными данными → Ошибка валидации под полем + Snackbar
- [ ] Имитировать ошибку сервера (Mock API) → Snackbar с ошибкой сервера
- [ ] Отправить несколько ошибок подряд → Проверить, что все отображаются
- [ ] Проверить, что приложение не крашится при ошибках
- [ ] Проверить локализацию сообщений (английский/русский)

---

## Дополнительные улучшения (будущие итерации)

### Улучшение 1: AlertDialog для критических ошибок

**Идея:** Разные типы UI для разных типов ошибок

- **Snackbar** для информационных ошибок (Network, Validation)
- **AlertDialog** для критических ошибок (Server 500, Auth)

**Реализация:**

```kotlin
// RootScreen.kt
val showErrorDialog by remember { mutableStateOf<AppError?>(null) }

LaunchedEffect(Unit) {
    appContainer.errorReporter.errorFlow.collect { error ->
        when (error) {
            is AppError.Server, is AppError.Generic -> {
                showErrorDialog = error
            }
            else -> {
                // Snackbar для остальных ошибок
                val message = error.toUiText(context)
                snackbarHostState.showSnackbar(message)
            }
        }
    }
}

// AlertDialog для критических ошибок
showErrorDialog?.let { error ->
    AlertDialog(
        onDismissRequest = { showErrorDialog = null },
        title = { Text("Ошибка") },
        text = { Text(error.toUiText(context)) },
        confirmButton = {
            Button(onClick = { showErrorDialog = null }) {
                Text("OK")
            }
        }
    )
}
```

### Улучшение 2: Кнопка "Повторить" в Snackbar

**Идея:** Добавить действие для повторения операции

> **ПРИМЕЧАНИЕ:** Это улучшение для будущей итерации. В текущей реализации (Этап 1) поле `retryAction` отсутствует в `AppError.Network`.

```kotlin
// AppError.kt (будущая версия)
sealed class AppError {
    data class Network(
        val message: String,
        val throwable: Throwable? = null,
        val retryAction: (() -> Unit)? = null  // ✅ Добавлено в будущей итерации
    ) : AppError()
    // ...
}

// RootScreen.kt
LaunchedEffect(Unit) {
    appContainer.errorReporter.errorFlow.collect { error ->
        val message = error.toUiText(context)
        val actionLabel = if (error is AppError.Network && error.retryAction != null) {
            "Повторить"
        } else {
            null
        }

        val snackbarResult = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Long
        )

        if (snackbarResult == SnackbarResult.ActionPerformed) {
            (error as? AppError.Network)?.retryAction?.invoke()
        }
    }
}
```

### Улучшение 3: Кэш истории ошибок

**Идея:** Хранить историю ошибок для аналитики и отладки

```kotlin
// ErrorHandler.kt
class ErrorHandler(
    private val logger: Logger,
) : ErrorReporter {
    private val errorHistory = mutableListOf<AppError>()

    override fun handleError(error: AppError): Boolean {
        // Сохраняем в историю
        errorHistory.add(error)

        // Ограничиваем размер истории
        if (errorHistory.size > MAX_HISTORY_SIZE) {
            errorHistory.removeAt(0)
        }

        // Логируем и отправляем
        logger.e(TAG, error.message)
        return _errorFlow.tryEmit(error)
    }

    fun getErrorHistory(): List<AppError> = errorHistory.toList()

    fun clearErrorHistory() {
        errorHistory.clear()
    }

    private companion object {
        const val MAX_HISTORY_SIZE = 100
    }
}
```

### Улучшение 4: Аналитика ошибок

**Идея:** Отправлять ошибки в аналитику (Firebase Analytics, Crashlytics)

```kotlin
// AnalyticsHelper.kt
class AnalyticsHelper {
    fun logError(error: AppError) {
        when (error) {
            is AppError.Network -> {
                Firebase.analytics.logEvent("network_error") {
                    param("message", error.message)
                }
            }
            is AppError.Server -> {
                Firebase.crashlytics.recordException(
                    RuntimeException(error.message)
                )
            }
            // ...
        }
    }
}

// ErrorHandler.kt
class ErrorHandler(
    private val logger: Logger,
    private val analyticsHelper: AnalyticsHelper,  // ✅ Добавлено
) : ErrorReporter {
    override fun handleError(error: AppError): Boolean {
        logger.e(TAG, error.message)
        analyticsHelper.logError(error)  // ✅ Логируем в аналитику
        return _errorFlow.tryEmit(error)
    }
}
```

---

## Примеры использования

### Пример 1: Ошибка сети в FriendsListViewModel

**До:**

```kotlin
swRepository.respondToFriendRequest(userId, accept = true)
    .onSuccess {
        logger.i(TAG, "Заявка успешно принята: userId=$userId")
    }
    .onFailure { error ->
        logger.e(TAG, "Ошибка принятия заявки: ${error.message}")
        // ❌ Ошибка только логируется
    }
```

**После:**

```kotlin
swRepository.respondToFriendRequest(userId, accept = true)
    .onSuccess {
        logger.i(TAG, "Заявка успешно принята: userId=$userId")
    }
    .onFailure { error ->
        // ✅ Ошибка отправляется через ErrorReporter
        errorReporter.handleError(
            AppError.Network(
                message = "Не удалось принять заявку. Проверьте подключение к интернету.",
                throwable = error
            )
        )
    }
```

### Пример 2: Ошибка валидации в LoginViewModel

**До:**

```kotlin
if (credentials.login.isEmpty() || credentials.password.isEmpty()) {
    _loginError.value = "Введите логин и пароль"
    return
}
```

**После:**

```kotlin
if (credentials.login.isEmpty()) {
    errorReporter.handleError(
        AppError.Validation(
            message = "Введите логин",
            field = "login"
        )
    )
    return
}
if (credentials.password.isEmpty()) {
    errorReporter.handleError(
        AppError.Validation(
            message = "Введите пароль",
            field = "password"
        )
    )
    return
}
```

### Пример 3: Ошибка сервера в ProfileViewModel

**До:**

```kotlin
repository.updateProfile(userData)
    .onSuccess {
        logger.i(TAG, "Профиль успешно обновлен")
    }
    .onFailure { error ->
        logger.e(TAG, "Ошибка обновления профиля: ${error.message}")
        // ❌ Ошибка только логируется
    }
```

**После:**

```kotlin
repository.updateProfile(userData)
    .onSuccess {
        logger.i(TAG, "Профиль успешно обновлен")
    }
    .onFailure { error ->
        logger.e(TAG, "Ошибка обновления профиля: ${error.message}")

        val httpCode = (error as? HttpException)?.code()
        errorReporter.handleError(
            AppError.Server(
                message = "Не удалось обновить профиль. Попробуйте позже.",
                code = httpCode
            )
        )
    }
```

### Пример 4: Ошибка с retry action

**ПРИМЕЧАНИЕ:** Этот пример относится к будущей итерации. В текущей реализации (Этап 1) поле `retryAction` отсутствует в `AppError.Network`. См. раздел "Улучшение 2: Кнопка 'Повторить' в Snackbar" для деталей.

**Будущая возможность:**

```kotlin
class FriendsListViewModel(
    // ...
    private val errorReporter: ErrorReporter,
) : ViewModel() {

    fun onAcceptFriendRequest(userId: Long) {
        viewModelScope.launch {
            _uiState.value = FriendsListUiState.Busy

            swRepository.respondToFriendRequest(userId, accept = true)
                .onSuccess { /* ... */ }
                .onFailure { error ->
                    errorReporter.handleError(
                        AppError.Network(
                            message = "Не удалось принять заявку. Проверьте подключение к интернету.",
                            throwable = error,
                            retryAction = {  // ✅ Будущее: возможность повторения
                                onAcceptFriendRequest(userId)
                            }
                        )
                    )
                }

            _uiState.value = lastSuccessState.value ?: FriendsListUiState.Success()
        }
    }
}
```

---

## Критерии приемки

- [ ] **Этап 1**: AppError создан и покрыт unit-тестами
- [ ] **Этап 2**: ErrorReporter интерфейс и ErrorHandler реализация созданы
- [ ] **Этап 3**: AppContainer обновлен с errorReporter и всеми factory методами
- [ ] **Этап 4**: FriendsListViewModel обновлен с errorReporter
- [ ] **Этап 5**: LoginViewModel обновлен с errorReporter
- [ ] **Этап 6**: RootScreen показывает Snackbar при ошибках
- [ ] **Этап 7**: Все существующие ViewModels обновлены
- [ ] **Этап 8**: Сообщения об ошибках локализованы с AppError.toUiText()
- [ ] **Этап 9**: Функциональность протестирована на устройстве
- [ ] Проект собирается без ошибок (`./gradlew assembleDebug`)
- [ ] Все unit-тесты проходят (`./gradlew test`)
- [ ] Все инструментальные тесты проходят (`./gradlew connectedAndroidTest`)
- [ ] `make format` выполнен (ktlint, detekt)

---

## Заключение

Реализация универсального механизма обработки ошибок через **ErrorReporter (интерфейс) + ErrorHandler (реализация) + SharedFlow** позволит:

1. **Устранить дублирование кода** в ViewModels
2. **Обеспечить консистентный UX** для всех ошибок приложения
3. **Упростить разработку новых экранов** - нет необходимости реализовывать обработку ошибок с нуля
4. **Улучшить тестируемость** - единый механизм для всех ошибок, интерфейс для моков
5. **Сохранить архитектурную чистоту** - минимальные изменения в существующем коде
6. **Сохранить чистоту AppState** - AppState остается только для навигации и авторизации
7. **Легкая интеграция** через существующий DI контейнер AppContainer
8. **Чистое разделение ответственности** - интерфейс для DI, реализация для логики

Этот подход идеально подходит для текущей архитектуры приложения и соответствует принципам MVVM и Clean Architecture. Разделение ответственности между AppState (UI-состояние) и ErrorReporter (обработка ошибок) обеспечивает чистую архитектуру и легкую поддержку кода. Использование интерфейса ErrorReporter в зависимостях улучшает тестируемость и соответствует принципу Dependency Inversion.
