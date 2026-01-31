# Фикс бага повторной авторизации (Final Plan)

Этот документ объединяет анализ проблемы и пошаговый план реализации с использованием `Channel` для обработки событий авторизации.

## 1. Анализ проблемы

**Симптомы:** После logout и быстрого нажатия на "Авторизация" (без ввода данных) срабатывает `onLoginSuccess`, вызывая запрос профиля с невалидным токеном (401).

**Причина:** `LoginUiState.Success` — это *состояние* (`StateFlow`), которое сохраняется при пересоздании View. При повторном открытии `LoginSheetHost` UI успевает прочитать старое состояние "Успех" до того, как сработает сброс сессии.

**Решение:** Заменить обработку успеха авторизации и сброса пароля на **одноразовые события** (`Channel` / `Flow`).

---

## 2. План реализации

### Шаг 1: Создать LoginEvent

Создайте новый файл `app/src/main/java/com/swparks/ui/state/LoginEvent.kt`:

```kotlin
package com.swparks.ui.state

/**
 * Одноразовые события для экрана авторизации.
 */
sealed class LoginEvent {
    data class Success(val userId: Long) : LoginEvent()
    data class ResetSuccess(val email: String) : LoginEvent()
}
```

### Шаг 2: Обновить LoginViewModel

В файле `app/src/main/java/com/swparks/ui/viewmodel/LoginViewModel.kt`:

1. Добавьте `Channel` и `Flow` для событий.
2. Обновите методы `login()` и `resetPassword()` для отправки событий.
3. Метод `resetForNewSession()` не требует изменений касательно канала (события не хранятся).

```kotlin
import com.swparks.ui.state.LoginEvent // Импорт нового класса
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class LoginViewModel(...) : ViewModel() {

    // ... существующие поля ...

    // Добавляем канал для событий
    private val _loginEvents = Channel<LoginEvent>(Channel.BUFFERED)
    val loginEvents = _loginEvents.receiveAsFlow()

    fun login() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            loginUseCase(credentials)
                .onSuccess { result ->
                    // ОТПРАВЛЯЕМ СОБЫТИЕ и сбрасываем UI в Idle
                    _loginEvents.send(LoginEvent.Success(result.userId))
                    
                    _uiState.value = LoginUiState.Idle 
                    _loginError.value = null
                }
                .onFailure { exception ->
                    val errorMessage = exception.message ?: "Неизвестная ошибка авторизации"
                    _uiState.value = LoginUiState.LoginError(errorMessage, exception)
                    _loginError.value = errorMessage
                }
        }
    }

    fun resetPassword() {
        if (!credentials.canRestorePassword) return

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            resetPasswordUseCase(credentials.login)
                .onSuccess {
                    // ОТПРАВЛЯЕМ СОБЫТИЕ и сбрасываем UI в Idle
                    _loginEvents.send(LoginEvent.ResetSuccess(credentials.login))
                    
                    _uiState.value = LoginUiState.Idle
                    _resetError.value = null
                }
                .onFailure { exception ->
                    val errorMessage = exception.message ?: "Неизвестная ошибка восстановления пароля"
                    _uiState.value = LoginUiState.ResetError(errorMessage, exception)
                    _resetError.value = errorMessage
                }
        }
    }
    
    // ... остальные методы (resetForNewSession и др.) остаются
}
```

### Шаг 3: Обновить LoginSheetHost

В `app/src/main/java/com/swparks/ui/screens/auth/LoginSheetHost.kt`:

Добавьте параметр `onResetSuccess` и передайте его в `LoginScreen`.

```kotlin
@Composable
fun LoginSheetHost(
    show: Boolean,
    onDismissed: () -> Unit,
    onLoginSuccess: (userId: Long) -> Unit,
    onResetSuccess: (String) -> Unit = {} // Новый параметр (опционально)
) {
    // ... (код инициализации ViewModel и LaunchedEffect)

    if (show) {
        ModalBottomSheet(...) {
            LoginScreen(
                viewModel = loginViewModel,
                onDismiss = {
                    if (uiState.isBusy) return@LoginScreen
                    scope.launch {
                        allowHide = true
                        sheetState.hide()
                        allowHide = false
                        onDismissed()
                    }
                },
                onLoginSuccess = { userId ->
                    scope.launch {
                        allowHide = true
                        sheetState.hide()
                        allowHide = false
                        onLoginSuccess(userId)
                    }
                },
                // Передаем обработчик (если нужен проброс наверх)
                onResetSuccess = onResetSuccess, 
                modifier = Modifier.disableAllGestures()
            )
        }
    }
}
```

### Шаг 4: Обновить LoginScreen

В `app/src/main/java/com/swparks/ui/screens/auth/LoginScreen.kt`:

1. Добавьте параметр `onResetSuccess`.
2. Замените использование `HandleLoginUiState` (или удалите его логику успеха) на `LaunchedEffect` с подпиской на события.

```kotlin
import com.swparks.ui.state.LoginEvent

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel,
    onDismiss: () -> Unit = {},
    onLoginSuccess: (userId: Long) -> Unit = {},
    onResetSuccess: (String) -> Unit = {} // Новый параметр
) {
    val uiState by viewModel.uiState.collectAsState()
    val loginError by viewModel.loginErrorState.collectAsState()
    val resetError by viewModel.resetErrorState.collectAsState()

    val screenState = rememberLoginScreenState()

    LaunchedEffect(Unit) { screenState.focusRequester.requestFocus() }

    // ... Scaffold и LoginContent ...

    // НОВАЯ ОБРАБОТКА СОБЫТИЙ (вместо uiState.Success)
    LaunchedEffect(Unit) {
        viewModel.loginEvents.collect { event ->
            when (event) {
                is LoginEvent.Success -> {
                    onLoginSuccess(event.userId)
                }
                is LoginEvent.ResetSuccess -> {
                    // Показываем локальный алерт
                    screenState.setShowResetSuccessAlert(true)
                    // И уведомляем родителя (если нужно)
                    onResetSuccess(event.email)
                }
            }
        }
    }

    // Обработка ОШИБОК (остается на uiState)
    HandleLoginErrorsOnly(uiState, screenState, viewModel) 
    // ^ Либо оставьте старый HandleLoginUiState, но удалите из него ветки Success/ResetSuccess
    
    // ... Алерты ...
}

// Вспомогательная функция (или модифицированный HandleLoginUiState)
@Composable
private fun HandleLoginErrorsOnly(
    uiState: LoginUiState, 
    screenState: LoginScreenState,
    viewModel: LoginViewModel
) {
    LaunchedEffect(uiState) {
        when (uiState) {
            is LoginUiState.LoginError -> {
                if (uiState.exception is NetworkException) screenState.setShowNoInternetAlert(true)
            }
            is LoginUiState.ResetError -> {
                if (uiState.exception is NetworkException) screenState.setShowNoInternetAlert(true)
                viewModel.clearErrors()
            }
            else -> {} // Success и Loading здесь игнорируем
        }
    }
}
```

### Шаг 5: Очистка (Удаление isLoggingOut)

Удалите ненужную логику состояния логаута из `ProfileRootScreen` и `RootScreen`. Теперь логаут — это простое действие без внешнего флага состояния.

#### В `app/src/main/java/com/swparks/ui/screens/profile/ProfileRootScreen.kt`

1. **Удалите параметры:** `isLoggingOut`, `onLogout`, `onLogoutComplete`.
2. **Добавьте:** `val scope = rememberCoroutineScope()` в начало функции.
3. **Удалите:** `LaunchedEffect(isLoggingOut) { ... }` в конце функции.
4. **Обновите `LogoutButton`:** вызывайте `logoutUseCase` напрямую внутри `onClick` и удалите параметр `enabled = !isLoggingOut`.

```kotlin
// ... Импорты: добавить import androidx.compose.runtime.rememberCoroutineScope ...
import kotlinx.coroutines.launch // не забудьте

@Composable
fun ProfileRootScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel,
    appContainer: com.swparks.data.AppContainer? = null,
    // УДАЛИТЬ: isLoggingOut, onLogout, onLogoutComplete
    onShowLoginSheet: () -> Unit = {}
) {
    // ДОБАВИТЬ scope
    val scope = rememberCoroutineScope()
    
    // ... (currentUser, uiState, проверка user == null) ...

    } else {
        // ... (Column, when(state)) ...

        // ОБНОВИТЬ LogoutButton
        LogoutButton(
            onClick = {
                scope.launch {
                    // Вызываем usecase напрямую
                    appContainer?.logoutUseCase?.invoke()
                }
            },
            // УДАЛИТЬ: enabled = !isLoggingOut (теперь кнопка всегда активна, или можно добавить локальный state)
        )

        // УДАЛИТЬ: LaunchedEffect(isLoggingOut)
    }
}
```

#### В `app/src/main/java/com/swparks/ui/screens/RootScreen.kt`

1. **Удалите:** `val isLoggingOut = remember { mutableStateOf(false) }`.
2. **Обновите вызов `ProfileRootScreen`:** удалите передачу удаленных параметров.

```kotlin
@Composable
fun RootScreen(appState: AppState) {
    // ...
    // УДАЛИТЬ: val isLoggingOut = remember { mutableStateOf(false) }
    // ...

    NavHost(...) {
        // ...
        composable(route = Screen.Profile.route) {
            ProfileRootScreen(
                appContainer = appContainer,
                viewModel = profileViewModel,
                // УДАЛИТЬ: isLoggingOut = ..., onLogout = ..., onLogoutComplete = ...
                onShowLoginSheet = { showLoginSheet = true },
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            )
        }
        // ...
    }
}
```

## Итог

Мы вынесли события успеха в отдельный канал и упростили логику логаута, убрав лишний стейт. Теперь при повторном открытии экрана старые события не воспроизводятся, что полностью устраняет баг с ложным вызовом `onLoginSuccess`.

---

## Статус реализации

✅ **Все шаги плана выполнены**

- ✅ Шаг 1: Создан `LoginEvent.kt` с sealed class для одноразовых событий
- ✅ Шаг 2: `LoginViewModel` обновлен с использованием Channel для событий
- ✅ Шаг 3: `LoginSheetHost` обновлен с передачей `onResetSuccess`
- ✅ Шаг 4: `LoginScreen` обновлен с подпиской на события через LaunchedEffect
- ✅ Шаг 5: Очистка завершена - удален `isLoggingOut` из UI

**Баг с повторной авторизации исправлен.** События успеха теперь являются одноразовыми (через Channel) и не сохраняются при пересоздании View, что предотвращает ложные вызовы `onLoginSuccess`.

---

## Анализ проблемы с многократным логом logout

### Симптомы

При logout в консоли дважды выводится лог "Очищен текущий userId":

```
2026-01-31 15:19:53.531  UserPrefer...Repository  I  Очищен текущий userId  ← первый вызов
2026-01-31 15:19:53.545  UserPrefer...Repository  I  Очищен текущий userId  ← второй вызов
```

### Анализ последовательности вызовов

В `LogoutUseCase.invoke()` (строки 30-43):

```kotlin
override suspend operator fun invoke() {
    // 1. Очищаем токен авторизации
    secureTokenRepository.saveAuthToken(null)

    // 2. Очищаем все данные пользователя
    swRepository.clearUserData()
    // Внутри вызывается preferencesRepository.clearCurrentUserId() ← ЛОГ 1

    // 3. Сбрасываем флаг isAuthorized
    swRepository.forceLogout()

    // 4. Очищаем userId (ИЗБЫТОЧНО!)
    preferencesRepository.clearCurrentUserId()  ← ЛОГ 2
    Log.i("LogoutUseCase", "Текущий пользователь очищен")
}
```

В `SWRepository.clearUserData()` (строки 974-980):

```kotlin
override suspend fun clearUserData() {
    // Удаляем все данные пользователя из БД
    userDao.clearAll()
    // Очищаем ID текущего пользователя ← ЛОГ 1
    preferencesRepository.clearCurrentUserId()
    Log.i("SWRepository", "Все данные пользователя удалены")
}
```

### Причина проблемы

**Дублирование очистки userId**:
- `LogoutUseCase` вызывает `swRepository.clearUserData()`, который уже очищает userId
- После этого `LogoutUseCase` снова вызывает `preferencesRepository.clearCurrentUserId()` — избыточно

Комментарий в коде даже указывает на это избыточность:
```kotlin
// Очищаем userId (избыточно, так как clearUserData уже сделал это)
preferencesRepository.clearCurrentUserId()
```

### Влияние на работу приложения

- **Функциональность**: Не нарушается — userId очищается корректно
- **Логи**: Дублирование создает путаницу при отладке
- **Качество кода**: Избыточная операция, затрудняет понимание потока выполнения

---

## План исправления проблемы с logout

### Шаг 1: Удалить избыточный вызов clearCurrentUserId из LogoutUseCase

**Статус: ✅ ВЫПОЛНЕНО**

В файле `app/src/main/java/com/swparks/domain/usecase/LogoutUseCase.kt`:

**Удалено:**
- Строку 40: `preferencesRepository.clearCurrentUserId()`
- Комментарий на строке 39

**Результат:**

```kotlin
override suspend operator fun invoke() {
    // Очищаем токен авторизации
    secureTokenRepository.saveAuthToken(null)

    // Очищаем все данные пользователя (профиль, друзья, заявки, черный список)
    swRepository.clearUserData()

    // Сбрасываем флаг isAuthorized
    swRepository.forceLogout()

    Log.i("LogoutUseCase", "Текущий пользователь очищен")
}
```

### Шаг 2: Обновить тесты

**Статус: ✅ ВЫПОЛНЕНО**

В файле `app/src/test/java/com/swparks/domain/usecase/LogoutUseCaseTest.kt`:

**Изменено:**
- Убрана проверка `coVerify(exactly = 1) { preferencesRepository.clearCurrentUserId() }` из тестов
- Оставлена только проверка вызова `swRepository.clearUserData()`

**Результат:**

```kotlin
@Test
fun invoke_whenLogout_thenClearsTokenAndCallsForceLogout() = runTest {
    // When
    logoutUseCase()

    // Then
    coVerify(exactly = 1) { secureTokenRepository.saveAuthToken(null) }
    coVerify(exactly = 1) { swRepository.clearUserData() }
    coVerify(exactly = 1) { swRepository.forceLogout() }
}
```

### Ожидаемый результат

После исправления в логах будет выводиться только один раз:
```
UserPreferencesRepository  I  Очищен текущий userId
```

Последовательность вызовов:
1. `secureTokenRepository.saveAuthToken(null)` — очищаем токен
2. `swRepository.clearUserData()` → `userDao.clearAll()` + `preferencesRepository.clearCurrentUserId()` — очищаем данные и userId
3. `swRepository.forceLogout()` → `preferencesRepository.savePreference(false)` — сбрасываем флаг авторизации

---

## Статус реализации

✅ **Исправление проблемы с logout завершено**

### Выполненные изменения

1. ✅ **Шаг 1**: Удален избыточный вызов `preferencesRepository.clearCurrentUserId()` из `LogoutUseCase.kt`
   - Убрана строка с прямым вызовом `preferencesRepository.clearCurrentUserId()`
   - Удален комментарий о избыточности
   - Теперь userId очищается только через `swRepository.clearUserData()`

2. ✅ **Шаг 2**: Обновлены тесты в `LogoutUseCaseTest.kt`
   - Убрана проверка `coVerify(exactly = 1) { preferencesRepository.clearCurrentUserId() }`
   - Тест теперь проверяет только корректные вызовы:
     - `secureTokenRepository.saveAuthToken(null)`
     - `swRepository.clearUserData()`
     - `swRepository.forceLogout()`

### Результат

- ✅ Убрано дублирование лога "Очищен текущий userId"
- ✅ Сохранена корректность логики logout
- ✅ Все тесты обновлены в соответствии с новой логикой

Теперь логи при logout будут выглядеть так:

```
UserPreferencesRepository  I  Очищен текущий userId        ← только 1 раз
SWRepository            I  Все данные пользователя удалены
SWRepository            I  Принудительный логаут выполнен
LogoutUseCase           I  Текущий пользователь очищен
```

---

## Дополнительное исправление: Дублирование лога "Текущий пользователь отсутствует"

### Симптомы

При logout в логах дважды выводится сообщение "Текущий пользователь отсутствует":

```
2026-01-31 15:39:39.290  UserPrefer...Repository  I  Очищен текущий userId
2026-01-31 15:39:39.290  SWRepositoryImp         I  Принудительный логаут выполнен
2026-01-31 15:39:39.292  SWRepositoryImp         D  Текущий пользователь отсутствует  ← первый раз
2026-01-31 15:39:39.292  SWRepositoryImp         D  Текущий пользователь отсутствует  ← второй раз
```

### Анализ проблемы

**Причина:** `UserPreferencesRepository.currentUserId` основан на `dataStore.data`. DataStore эмитит событие при **любом** изменении в файле настроек.

При логауте происходят два изменения последовательно:
1. `clearUserData()` удаляет `current_user_id` → DataStore обновляется → `currentUserId` Flow получает `null`
2. `forceLogout()` меняет `is_authorized` → DataStore обновляется → `currentUserId` Flow снова получает `null` (так как ID всё ещё нет)

Так как в потоке `currentUserId` нет фильтрации повторов, `SWRepository` получает сигнал `null` дважды и дважды выполняет логику "пользователь отсутствует".

### Решение

Добавить оператор `distinctUntilChanged()` в `UserPreferencesRepository.currentUserId`. Это заставит поток игнорировать повторные `null`, если значение не изменилось.

### Выполненные изменения

**Статус: ✅ ВЫПОЛНЕНО**

В файле `app/src/main/java/com/swparks/data/UserPreferencesRepository.kt`:

1. ✅ Добавлен импорт: `import kotlinx.coroutines.flow.distinctUntilChanged`

2. ✅ Добавлен оператор `distinctUntilChanged()` в поле `currentUserId`:

```kotlin
val currentUserId: Flow<Long?> = dataStore.data
    .catch { ... }
    .map { it[current_user_id] }
    .distinctUntilChanged()  // ← предотвращает повторные эмиссии одного значения
```

### Почему это поможет

`distinctUntilChanged()` пропускает значение дальше только если оно отличается от предыдущего:
- При удалении ID: было `123`, стало `null` → пропускаем (лог выводится 1 раз)
- При смене флага авторизации: было `null`, стало `null` → **игнорируем** (лог не выводится)

Это сделает поток данных чище и избавит от лишних срабатываний логики в репозитории и ViewModel.

### Ожидаемый результат

После исправления логи при logout будут выглядеть так:

```
UserPreferencesRepository  I  Очищен текущий userId
SWRepository            I  Все данные пользователя удалены
SWRepository            D  Текущий пользователь отсутствует        ← только 1 раз
SWRepository            I  Принудительный логаут выполнен
LogoutUseCase           I  Текущий пользователь очищен
```
