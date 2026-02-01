---
name: AppState авторизация
overview: Добавить состояние авторизации в AppState для мгновенного обновления UI при login/logout и удобного доступа к статусу авторизации из любого экрана
todos: []
isProject: false
---

# План: Добавление состояния авторизации в AppState

## Критически важные изменения (исправлены)

**ВАЖНО:** Используем `mutableStateOf` вместо обычных `var` для обеспечения рекомпозиции в Compose.

## Этап 1: Модификация AppState

**Файл:** `app/src/main/java/com/swparks/navigation/AppState.kt`

Добавить свойства авторизации:

- `currentUser: User?` с использованием `mutableStateOf` (композиционно отслеживаемое)
- `isAuthorized: Boolean` - вычисляемое свойство через getter
- `updateCurrentUser(user: User?)` - метод для безопасного обновления

```kotlin
class AppState(val navController: NavHostController) {
    var currentUser by mutableStateOf<User?>(null)
        private set
    
    val isAuthorized: Boolean
        get() = currentUser != null
    
    fun updateCurrentUser(user: User?) {
        currentUser = user
    }
}
```

## Этап 2: Синхронизация в RootScreen

**Файл:** `app/src/main/java/com/swparks/ui/screens/RootScreen.kt`

- Подписаться на `profileViewModel.currentUser` через `collectAsState()`
- Синхронизировать через `appState.updateCurrentUser()` в `LaunchedEffect`

```kotlin
val currentUser by profileViewModel.currentUser.collectAsState()

LaunchedEffect(currentUser) {
    appState.updateCurrentUser(currentUser)
}
```

## Этап 3: Обновление ProfileTopAppBar

**Файл:** `app/src/main/java/com/swparks/ui/screens/profile/ProfileRootScreen.kt`

- Добавить параметр `appState: AppState`
- Удалить параметр `showSearchButton: Boolean`
- Использовать `appState.isAuthorized` для условного отображения кнопки поиска

**Файл:** `app/src/main/java/com/swparks/ui/screens/RootScreen.kt`

- Передать `appState` в `ProfileTopAppBar`

## Этап 4: Примеры использования

**Файл:** `app/src/main/java/com/swparks/ui/screens/parks/ParksRootScreen.kt` (опционально)

- Добавить параметр `appState: AppState` в `ParksTopAppBar`
- Показать пример условной кнопки создания площадки

## Этап 5: Обновление документации

**Файлы:**

- `docs/profile-buttons-enhancement-plan.md`
- `docs/appstate-auth-state.md` (новый)

Обновить планы и создать документацию по AppState с примерами использования.

## Этап 6: Тестирование

Проверить функциональность:

- Кнопка поиска появляется мгновенно при login
- Кнопка поиска исчезает мгновенно при logout
- Состояние доступно на всех экранах через `appState.isAuthorized`

**Все изменения подробно описаны в:** `docs/auth-state-in-appstate-plan.md`
