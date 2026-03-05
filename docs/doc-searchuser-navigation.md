# Навигация SearchUserScreen - сохранение активной вкладки

## Описание функционала

Экран поиска пользователей `SearchUserScreen` может быть открыт из двух разных разделов приложения:
- Из вкладки **Profile** (через иконку поиска в TopAppBar)
- Из вкладки **Messages** (через кнопку в EmptyState)

Для обеспечения корректной работы навигации реализована система динамического определения активной вкладки на основе источника перехода.

## Принцип работы

### Динамическое определение вкладки

При открытии `SearchUserScreen` система определяет, из какой вкладки произошел переход, и сохраняет эту вкладку активной. Это обеспечивает корректное отображение BottomNavigationBar и правильную работу навигации.

**Параметр `source`:**
- `"profile"` — переход из вкладки Profile
- `"messages"` — переход из вкладки Messages (значение по умолчанию)

### Маршрутизация

Маршрут экрана использует query-параметр для передачи источника:

```
user_search?source={source}
```

**Примеры маршрутов:**
- `user_search?source=profile` — поиск пользователей из Profile
- `user_search?source=messages` — поиск пользователей из Messages

## Реализация

### 1. Модель навигации (Destinations.kt)

Объект `UserSearch` содержит:
- Маршрут с query-параметром: `user_search?source={source}`
- Метод `createRoute(source: String)` для формирования маршрута
- Метод `findParentTab(arguments: Bundle?)` для динамического определения родительской вкладки

```kotlin
object UserSearch : Screen("user_search?source={source}", parentTab = Messages) {
    fun createRoute(source: String = "messages") = "user_search?source=$source"

    fun findParentTab(arguments: Bundle?): Screen {
        val source = arguments?.getString("source") ?: "messages"
        return getScreenBySource(source, default = Messages)
    }
}
```

### 2. AppState (AppState.kt)

Метод `onDestinationChanged()` автоматически определяет родительскую вкладку для всех дочерних экранов, включая `SearchUserScreen`:

```kotlin
fun onDestinationChanged(route: String?, arguments: android.os.Bundle? = null) {
    // Проверяем прямое совпадение с корневой вкладкой
    val matchingTab = topLevelDestinations.find { it.route == route }
    if (matchingTab != null) {
        currentTopLevelDestination = matchingTab
        return
    }

    // Для дочерних экранов определяем parentTab через Screen.findParentTab()
    val parentTab = Screen.findParentTab(route ?: "", arguments)
    if (parentTab != null) {
        val parentTopLevelDestination = topLevelDestinations.find { 
            it.route == parentTab.route 
        }
        if (parentTopLevelDestination != null) {
            currentTopLevelDestination = parentTopLevelDestination
            return
        }
    }
}
```

### 3. Точки вызова навигации

**ProfileRootScreen.kt:**

```kotlin
onSearchUsersClick = {
    appState.navController.navigate(Screen.UserSearch.createRoute("profile"))
}
```

**MessagesRootScreen.kt:**

```kotlin
onNavigateToSearchUsers = {
    appState.navController.navigate(Screen.UserSearch.createRoute("messages"))
}
```

### 4. NavHost (RootScreen.kt)

Настройка composable для обработки query-параметра:

```kotlin
composable(
    route = Screen.UserSearch.route,
    arguments = listOf(
        androidx.navigation.navArgument("source") {
            defaultValue = "messages"
        }
    )
) { navBackStackEntry ->
    val source = navBackStackEntry.arguments?.getString("source") ?: "messages"
    SearchUserScreen(
        viewModel = viewModel,
        onUserClick = { userId ->
            appState.navController.navigate(
                Screen.OtherUserProfile.createRoute(userId, source)
            )
        }
    )
}
```

## Поведение пользователя

### Сценарий 1: Переход из Profile

1. Пользователь находится во вкладке **Profile**
2. Нажимает на иконку поиска в TopAppBar
3. Открывается `SearchUserScreen`
4. **BottomNavigationBar** показывает активную вкладку **Profile**
5. При нажатии на любую вкладку происходит корректный переход

### Сценарий 2: Переход из Messages

1. Пользователь находится во вкладке **Messages**
2. Нажимает на кнопку поиска пользователей (в EmptyState)
3. Открывается `SearchUserScreen`
4. **BottomNavigationBar** показывает активную вкладку **Messages**
5. При нажатии на любую вкладку происходит корректный переход

## Наследование source параметра

При переходе из `SearchUserScreen` на другие экраны (например, `OtherUserProfile`) параметр `source` передается дальше. Это обеспечивает корректную работу навигации на всех последующих экранах.

**Пример цепочки навигации:**

```
Profile → SearchUserScreen (source=profile) → OtherUserProfile (source=profile)
Messages → SearchUserScreen (source=messages) → OtherUserProfile (source=messages)
```

## Обратная совместимость

Реализация обеспечивает обратную совместимость:
- Если параметр `source` не указан, используется значение по умолчанию: `"messages"`
- Это гарантирует корректную работу существующего кода, который может использовать старый формат маршрута

## Логирование

Для отладки навигации используется логирование в теге **"Navigation"**:

```
OnDestinationChangedListener: destination=user_search?source=profile, arguments=[source]
  -> дочерний экран с parentTab=profile, source=profile, обновляем currentTopLevelDestination
```

## Файлы реализации

1. `app/src/main/java/com/swparks/navigation/Destinations.kt` — модель навигации
2. `app/src/main/java/com/swparks/navigation/AppState.kt` — логика определения вкладки
3. `app/src/main/java/com/swparks/ui/screens/RootScreen.kt` — настройка NavHost
4. `app/src/main/java/com/swparks/ui/screens/profile/ProfileRootScreen.kt` — вызов из Profile
5. `app/src/main/java/com/swparks/ui/screens/messages/MessagesRootScreen.kt` — вызов из Messages
