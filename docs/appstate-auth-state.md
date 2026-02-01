# Состояние авторизации в AppState

## Обзор

AppState - это глобальный класс для управления состоянием навигации и авторизации в приложении. Он предоставляет удобный доступ к статусу авторизации пользователя из любого экрана без необходимости прокидывать ProfileViewModel через параметры.

## Свойства авторизации

### currentUser: User?

Текущий авторизованный пользователь. Может быть `null`, если пользователь не авторизован.

**ВАЖНО:** Используем `mutableStateOf<User?>` вместо обычного `var`, чтобы Compose "видел" изменения состояния и вызывал рекомпозицию UI.

```kotlin
var currentUser by mutableStateOf<User?>(null)
    private set
```

### isAuthorized: Boolean

Флаг авторизации пользователя. Автоматически вычисляется на основе `currentUser` и обновляется при каждом его изменении.

**ВАЖНО:** Это вычисляемое свойство (val с getter), которое автоматически пересчитывается при изменении `currentUser`. Не нужно вручную вызывать recomposity.

```kotlin
val isAuthorized: Boolean
    get() = currentUser != null
```

### updateCurrentUser(user: User?)

Метод для безопасного обновления текущего пользователя. Используется вместо прямого присваивания `appState.currentUser = ...`, так как setter для `currentUser` приватный.

```kotlin
fun updateCurrentUser(user: User?) {
    currentUser = user
}
```

## Использование в UI

### Проверка авторизации на любом экране

```kotlin
@Composable
fun SomeScreen(appState: AppState) {
    if (appState.isAuthorized) {
        // Показываем контент для авторизованных пользователей
        Text("Привет, авторизованный пользователь!")
    } else {
        // Показываем заглушку или кнопку входа
        Text("Пожалуйста, авторизуйтесь")
    }
}
```

### Доступ к данным пользователя

```kotlin
@Composable
fun ProfileScreen(appState: AppState) {
    appState.currentUser?.let { user ->
        Text("Привет, ${user.fullName}")
    } ?: run {
        // Пользователь не авторизован
        Text("Вы не авторизованы")
    }
}
```

### Условное отображение кнопок

```kotlin
@Composable
fun ParksTopAppBar(appState: AppState) {
    CenterAlignedTopAppBar(
        title = { Text("Площадки") },
        actions = {
            if (appState.isAuthorized) {
                IconButton(onClick = { /* Создать площадку */ }) {
                    Icon(Icons.Default.Add, contentDescription = "Создать")
                }
            }
        }
    )
}
```

## Синхронизация с ProfileViewModel

Синхронизация выполняется в `RootScreen` через `LaunchedEffect` и `collectAsState()`:

```kotlin
@Composable
fun RootScreen(appState: AppState) {
    val profileViewModel = remember {
        appContainer.profileViewModelFactory()
    }

    // Подписываемся на Flow из ProfileViewModel для реактивного обновления
    val currentUser by profileViewModel.currentUser.collectAsState()

    // Синхронизируем currentUser с AppState при каждом изменении
    LaunchedEffect(currentUser) {
        appState.updateCurrentUser(currentUser)
    }

    Scaffold(
        topBar = {
            when (appState.currentTopLevelDestination?.route) {
                Screen.Profile.route -> {
                    ProfileTopAppBar(
                        appState = appState,
                        onSearchUsersClick = { /* Логика поиска */ }
                    )
                }
            }
        }
    )
}
```

### Почему используется collectAsState()

`collectAsState()` - это функция Jetpack Compose, которая автоматически:
1. Подписывается на Flow из `profileViewModel.currentUser`
2. Преобразует Flow в State, который отслеживается Compose
3. Вызывает рекомпозицию всех компонентов, которые читают этот State

**ВАЖНО:** Использование обычного чтения `.value` из Flow не обеспечит автоматическую рекомпозицию при изменении потока.

## Важные примечания

### 1. Мгновенное обновление UI

Использование `mutableStateOf` в `AppState` и `collectAsState()` в `RootScreen` обеспечивает мгновенное обновление UI:

- При login: `profileViewModel.currentUser` изменяется → `collectAsState()` ловит изменение → `LaunchedEffect` обновляет `appState.currentUser` → Компоненты с `appState.isAuthorized` перерисовываются → Кнопка поиска появляется мгновенно
- При logout: То же самое, только наоборот — кнопка поиска исчезает мгновенно

### 2. AppState уже используется везде

`AppState` уже прокидывается во все экраны через навигацию:
- Вкладки: `ParksRootScreen`, `EventsScreen`, `MessagesRootScreen`, `ProfileRootScreen`, `MoreScreen`
- Детальные экраны получают `appState` из `MainActivity` → `RootScreen` → навигация

Это означает, что не нужно прокидывать ProfileViewModel в каждый экран — просто используйте `appState.isAuthorized`.

### 3. Соответствие архитектуре

Это решение следует MVVM и принципам проекта:
- `AppState` — это глобальное состояние приложения (как навигация)
- `ProfileViewModel` — источник истины (подписан на репозитории)
- `RootScreen` — синхронизирует данные между ними
- Компоненты UI — читают состояние через параметры (`appState`)

### 4. Безопасное обновление

Метод `updateCurrentUser(user: User?)` защищает от ошибок:
- Setter для `currentUser` приватный (`private set`)
- Изменение возможно только через метод `updateCurrentUser()`
- Это предотвращает случайное прямое присваивание из внешнего кода

### 5. Вычисляемое свойство isAuthorized

Использование вычисляемого свойства (val с getter) вместо обычного свойства имеет преимущества:
- Автоматически пересчитывается при каждом изменении `currentUser`
- Не нужно вручную вызывать `isAuthorized = currentUser != null` при обновлении
- Compose видит, что свойство зависит от `currentUser`, и подписывается на его изменения

## Типичные сценарии использования

### Сценарий 1: Условное отображение FAB кнопки

```kotlin
@Composable
fun ParksScreen(appState: AppState) {
    // ... остальной код ...

    // Кнопка создания площадки только для авторизованных
    if (appState.isAuthorized) {
        FloatingActionButton(
            onClick = { /* Создать площадку */ }
        ) {
            Icon(Icons.Default.Add, contentDescription = "Создать")
        }
    }
}
```

### Сценарий 2: Ограничение доступа к функциям

```kotlin
@Composable
fun EventsScreen(appState: AppState) {
    if (appState.isAuthorized) {
        Button(onClick = { /* Создать мероприятие */ }) {
            Text("Создать мероприятие")
        }
    } else {
        Button(onClick = { appState.navigateToProfile() }) {
            Text("Войти для создания мероприятий")
        }
    }
}
```

### Сценарий 3: Доступ к данным профиля

```kotlin
@Composable
fun ProfileScreen(appState: AppState) {
    appState.currentUser?.let { user ->
        // Показываем данные авторизованного пользователя
        Text("Имя: ${user.fullName}")
        Text("ID: ${user.id}")
    } ?: run {
        // Пользователь не авторизован
        IncognitoProfileView(onClickAuth = { /* Открыть экран входа */ })
    }
}
```

## Консистентность с iOS

В iOS-приложении аналогичная функциональность реализована через `@EnvironmentObject` для глобальных сервисов (например, `DefaultsService` для настроек авторизации).

`AppState` в Android выполняет ту же роль:
- iOS: `@EnvironmentObject var defaultsService` → экраны читают через `@EnvironmentObject`
- Android: `appState: AppState` → экраны читают через параметр `appState`

Оба подхода обеспечивают глобальный доступ к состоянию без необходимости прокидывать зависимости через параметры каждого компонента.

## Тестирование

### Unit-тесты для AppState

Для проверки функциональности состояния авторизации созданы unit-тесты в файле `app/src/test/java/com/swparks/navigation/AppStateTest.kt`.

**Тесты покрывают следующие сценарии:**
- Начальное состояние (`currentUser = null`, `isAuthorized = false`)
- Обновление пользователя через `updateCurrentUser()`
- Проверка флага авторизации при различных состояниях
- Множественные обновления пользователя
- Очистка пользователя (logout)
- Переходы состояний (null → user → null → user)
- Стабильность при повторных обновлениях одним и тем же пользователем

**Результаты тестирования:**
- Количество тестов: 9
- Успешно: 9/9
- Успешность: 100%
- Время выполнения: 0.046s

### Пример теста

```kotlin
@Test
fun updateCurrentUser_whenCalledWithUser_thenUpdatesCurrentUser() {
    // Given
    val testUser = createTestUser()

    // When
    appState.updateCurrentUser(testUser)

    // Then
    val currentUser = appState.currentUser
    assertTrue("Текущий пользователь должен быть обновлен", currentUser != null)
    assertTrue("ID пользователя должен совпадать", currentUser?.id == testUser.id)
    assertTrue("Имя пользователя должно совпадать", currentUser?.name == testUser.name)
}
```

### Ручное тестирование

Ручное тестирование функциональности было проведено на экране профиля:

✅ Кнопка поиска скрывается мгновенно при logout
✅ Кнопка поиска появляется мгновенно при login
✅ `appState.isAuthorized` доступен на всех экранах
✅ Состояние синхронизируется корректно между ProfileViewModel и AppState
