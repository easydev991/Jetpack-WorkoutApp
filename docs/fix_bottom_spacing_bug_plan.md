# План исправления бага с пустым пространством между LazyColumn и BottomNavigationBar

## Описание проблемы

На экранах `ParksRootScreen.kt` и `EventsScreen.kt` между LazyColumn и BottomNavigationBar появляется пустое пространство.

## Корневая причина

Двойная вложенность Scaffold:
- **RootScreen.kt** имеет внешний `Scaffold` с `bottomBar` (BottomNavigationBar) и `contentWindowInsets = WindowInsets(0, 0, 0, 0)`
- **ParksRootScreen.kt** и **EventsScreen.kt** имеют свой внутренний `Scaffold` с `topBar`
- Внутренний Scaffold получает `paddingValues` только от своего TopAppBar, но не учитывает BottomNavigationBar из родительского Scaffold

## Решение

Передача TopAppBar как параметра в родительский Scaffold:
- Обновить `ParksRootScreen` и `EventsScreen` для приема `topBar` как параметра вместо создания собственного Scaffold
- Удалить внутренний Scaffold из этих экранов
- Обновить `RootScreen.kt` для передачи соответствующего TopAppBar через параметр `topBar` внешнего Scaffold
- Убедиться, что LazyColumn использует `paddingValues` от родительского Scaffold

## Этапы реализации

### Этап 1: Изменение ParksRootScreen.kt

- [ ] Изменить сигнатуру для приема `@Composable () -> Unit topBar` или использовать Box вместо Scaffold
- [ ] Удалить внутренний Scaffold (строки 35-56)
- [ ] Добавить TopAppBar напрямую в UI без Scaffold или передать его как параметр

### Этап 2: Изменение EventsScreen.kt и PastEventsScreen.kt

- [ ] Аналогично изменить `EventsScreen.kt` и `PastEventsScreen.kt`
- [ ] Убрать внутренние Scaffold

### Этап 3: Обновление RootScreen.kt

- [ ] Обновить `RootScreen.kt` для передачи TopAppBar в каждый экран через параметры
- [ ] Настроить передачу разных TopAppBar для разных вкладок (Parks, Events)

### Этап 4: Проверка корректности

- [ ] Убедиться, что LazyColumn не перекрывает BottomNavigationBar
- [ ] Проверить, что контент прокручивается до самого низа без лишних отступов
- [ ] Убедиться, что TopAppBar отображается корректно
- [ ] Проверить на разных размерах экранов
- [ ] Протестировать на экранах без контента (пустой список)
- [ ] Проверить корректность прокрутки

### Этап 5: Актуализация документации

- [ ] Добавить раздел в архитектурную документацию о правильной работе с Scaffold и windowInsets
- [ ] Обновить правила разработки экранов для предотвращения повторения этой ошибки

## Критерии завершения

- ✅ Нет пустого пространства между LazyColumn и BottomNavigationBar
- ✅ LazyColumn прокручивается до конца контента
- ✅ TopAppBar отображается корректно на всех экранах
- ✅ Контент не перекрывается BottomNavigationBar
- ✅ Отступы корректны на всех размерах экранов
- ✅ Документация обновлена

## Примечание

`contentPadding.bottom` не влияет на верстку, потому что добавляет отступ внутри прокручиваемого контента, который "уходит вверх" при прокрутке вниз. Проблема специфична для вложенных Scaffold — каждый Scaffold имеет свои paddingValues.

---

## Проблема авторизации вызывает удаление пользователя

### Описание проблемы

При авторизации пользователя через `LoginSheetHost` происходит следующее:

1. Пользователь вводит учетные данные и нажимает "Войти"
2. `LoginViewModel.login()` выполняет авторизацию через `loginUseCase(credentials)` (строка 96)
3. При успешной авторизации `LoginViewModel.loginAndLoadUserData()` загружает данные пользователя (строки 124-154)
4. При навигации пользователя на экран профиля (или закрытии LoginSheet) корутина отменяется
5. Отмена корутины вызывает исключение, которое обрабатывается как ошибка авторизации
6. `AuthInterceptor` срабатывает и удаляет токен пользователя

**Результат:** пользователь теряет авторизацию сразу после успешного входа.

### Корневая причина

**1. Неправильный scope для сетевых операций:**

- `loginAndLoadUserData()` запускается из `LaunchedEffect(uiState)` в `LoginSheetHost` (строка 420)
- Это создает корутину в **Compose scope**, который привязан к жизненному циклу экрана
- При навигации или закрытии экрана корутина отменяется
- Отмена корутины вызывает `IOException` в `swRepository.getSocialUpdates(userId)`

**2. RetryInterceptor ретраит отмененные запросы:**

- В `RetryInterceptor` нет проверки на `CancellationException` (строки 64-76)
- При отмене корутины выбрасывается `IOException`, который ретраится 3 раза
- Это неправильно — отмена означает "пользователь ушел", а не "ошибка сети"

**3. AuthInterceptor срабатывает на ложные триггеры:**

- `AuthInterceptor` срабатывает на любой ошибке (строки 31-43)
- При отмене корутины и retry — может сработать `forceLogout()`
- Нет проверки на реальные HTTP коды ошибок

**4. Отсутствие защиты от гонок:**

- Нет защиты от множественных вызовов `forceLogout()`
- Несколько параллельных запросов могут одновременно вызвать логаут

### Анализ nowinandroid

**Ключевые отличия в nowinandroid:**

1. **ViewModels используют только viewModelScope:**
   - Все ViewModels (например, `ForYouViewModel`) используют только `viewModelScope.launch`
   - Нет использования `LaunchedEffect` для сетевых вызовов
   - Никакие сетевые операции не запускаются из Compose scope

2. **Отсутствие retry interceptor:**
   - В `NetworkModule` используется только `HttpLoggingInterceptor`
   - Нет `RetryInterceptor` — временные ошибки сервера обрабатываются иначе
   - Отмена корутин не вызывает retry

3. **Отсутствие AuthInterceptor для 401:**
   - В nowinandroid нет интерсептора для обработки 401
   - Логауты не вызываются автоматически из интерсепторов
   - Логауты контролируются только из UI层

### Решение

Вариант решения основан на лучших практиках из **nowinandroid** (Google sample app):

### Ключевые принципы из nowinandroid

1. **ViewModels используют ТОЛЬКО viewModelScope:**
   - В `ForYouViewModel` все сетевые операции используют `viewModelScope.launch`
   - Никакие сетевые операции не запускаются из Compose scope
   - Никакие сетевые операции не запускаются из `LaunchedEffect` для сетевых вызовов

2. **Отсутствие retry interceptor:**
   - В `NetworkModule` используется только `HttpLoggingInterceptor`
   - Нет `RetryInterceptor` — временные ошибки обрабатываются иначе
   - Отмена корутин не вызывает retry автоматически

3. **Отсутствие автоматического логаута из интерсепторов:**
   - В nowinandroid нет интерсептора для обработки 401
   - Логауты контролируются только из UI layer
   - Нет автоматического удаления данных на основе HTTP ошибок

### 1. Перенести логин из Composable в ViewModel

**Проблема:** `loginAndLoadUserData()` запускается из `LaunchedEffect(uiState)` в Compose scope

**Решение:** Все сетевые операции должны быть в `viewModelScope`:

```kotlin
// LoginViewModel.kt - УБРАТЬ loginAndLoadUserData()!
// Использовать только viewModelScope.launch { ... }
```

### 2. Разделить "логин" и "загрузка профиля"

**Проблема:** `loginAndLoadUserData()` делает две разные операции

**Решение:** Разделить на отдельные операции:

- `LoginViewModel.login()` — только авторизация, сохраняет токен
- `ProfileViewModel` — загружает данные пользователя при открытии профиля
- Загрузка профиля начинается в стабильном lifecycle (ProfileScreen), не в LoginSheet

### 3. Исправить RetryInterceptor: не ретраить отмену

**Проблема:** Отмененные корутины ретраятся 3 раза

**Решение:** Добавить проверку на `CancellationException`:

```kotlin
// RetryInterceptor.kt
override fun intercept(chain: Interceptor.Chain): Response {
    try {
        // ... существующий код ...
    } catch (e: IOException) {
        // Проверяем на отмену корутины
        if (e is CancellationException) {
            throw e // НЕ ретраить, пробрасываем дальше
        }
        // ... существующий код retry ...
    }
}
```

### 4. Ужесточить AuthInterceptor: только на реальный 401

**Проблема:** `AuthInterceptor` срабатывает на любой ошибке

**Решение:** Добавить проверки:

```kotlin
// AuthInterceptor.kt
override fun intercept(chain: Interceptor.Chain): Response {
    val response = chain.proceed(chain.request())

    // ТОЛЬКО на реальный HTTP 401
    if (response.code == UNAUTHORIZED_STATUS_CODE) {
        Log.e(TAG, "Ошибка авторизации (401): токен недействителен или истек")
        clearToken()
    }

    return response
}
```

### 5. Защититься от гонок при логауте

**Проблема:** Параллельные запросы могут одновременно вызвать логаут

**Решение:** Добавить флаг или Mutex:

```kotlin
// AuthInterceptor.kt
private var isLoggingOut = false
private val logoutMutex = Mutex()

private suspend fun clearToken() {
    // Защита от гонок
    logoutMutex.withLock {
        if (isLoggingOut) return
        isLoggingOut = true

        try {
            preferencesRepository.clearToken()
            Log.i(TAG, "Токен авторизации очищен")
        } finally {
            isLoggingOut = false
        }
    }
}
```

### 5.1. Добавить исключение эндпоинтов для логаута

**Проблема:** `AuthInterceptor` срабатывает на 401 для любых эндпоинтов, включая логин/регистрацию

**Решение:** Не выполнять `forceLogout()` для эндпоинтов авторизации:

```kotlin
// AuthInterceptor.kt
override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val response = chain.proceed(request)

    if (response.code == UNAUTHORIZED_STATUS_CODE) {
        // Проверяем путь запроса - НЕ логаут для эндпоинтов авторизации
        val path = request.url.encodedPath
        val isAuthEndpoint = path.contains("/login") ||
                            path.contains("/register") ||
                            path.contains("/reset-password")

        if (!isAuthEndpoint) {
            Log.e(TAG, "Ошибка авторизации (401): токен недействителен или истек")
            clearToken()
        }
    }

    return response
}
```

## Этапы реализации

### Этап 1: Исправление RetryInterceptor ✅

- [x] Добавить импорт `kotlin.coroutines.CancellationException`
- [x] Добавить проверку: если ошибка `CancellationException` — НЕ ретраить
- [x] Протестировать, что отмена корутины не вызывает retry

### Этап 2: Ужесточение AuthInterceptor ✅

- [x] Добавить защиту от гонок через Mutex
- [x] Убедиться, что `forceLogout()` вызывается только на HTTP 401
- [x] Добавить логирование причины логаута
- [x] Добавить исключение эндпоинтов для логаута
- [x] Протестировать на разных ошибках (401, 404, 500, отмена корутины)

### Этап 3: Убрать loginAndLoadUserData из ViewModel ✅

- [x] Удалить метод `loginAndLoadUserData()` из `LoginViewModel`
- [x] Оставить только `login()` с базовой авторизацией
- [x] Переместить загрузку профиля в `ProfileViewModel` (уже загружается автоматически при старте)

### Этап 4: Обновить навигацию и UI ✅

- [x] Убрать вызов `loginAndLoadUserData()` из `LaunchedEffect` в `LoginSheetHost`
- [x] Обновить `LoginSheetHost.onLoginSuccess` — только закрытие sheet
- [x] Упростить callback `LoginScreen.onLoginSuccess` — убрать передачу Result<SocialUpdates>
- [x] ProfileViewModel загружает данные пользователя при старте (через `currentUser.collect`)

### Этап 5: Актуализация документации ✅

- [x] Добавить раздел в архитектурную документацию о правильной работе с корутинами в ViewModels
- [x] Обновить правила разработки интерсепторов
- [x] Добавить примеры правильного и неправильного кода

## Критерии завершения

- ✅ Авторизация не вызывает удаление пользователя
- ✅ Отмена корутины не вызывает retry
- ✅ `AuthInterceptor` срабатывает только на реальный HTTP 401
- ✅ Нет гонок при логауте
- ✅ Данные пользователя загружаются корректно после авторизации
- ✅ Навигация работает корректно после входа
- ✅ Документация обновлена
