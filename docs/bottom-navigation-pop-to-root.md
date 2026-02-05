# Навигация: возврат к родительской вкладке

## Обзор

Реализация навигации в Jetpack-WorkoutApp с поддержкой возврата к корневому экрану вкладки при повторном нажатии на вкладку bottom navigation.

---

## Реализация в Now in Android

NIA использует **Navigation 3** с кастомным Navigator и двухуровневой структурой стека:

- `topLevelStack` - стек вкладок
- `subStacks` - под-стеки для каждой вкладки

Ключевая логика:

```kotlin
fun navigate(key: NavKey) {
    when (key) {
        state.currentTopLevelKey -> clearSubStack()  // Сброс при повторном нажатии
        in state.topLevelKeys -> goToTopLevel(key)
        else -> goToKey(key)
    }
}
```

---

## Решение для Jetpack-WorkoutApp

### Основная идея

1. **Поле `parentTab`** в `Screen` для определения принадлежности экрана к вкладке
2. **Метод `isCurrentRouteTopLevel`** в `AppState` для определения типа экрана
3. **Метод `currentTopLevelDestination`** для нахождения родительской вкладки
4. **Условное отображение `topBar`** в `RootScreen`
5. **Логика `navigateToTopLevelDestination()`** для сброса стека при reselect

---

## Детальная реализация

### 1. Screen (Destinations.kt)

```kotlin
sealed class Screen(
    val route: String,
    val parentTab: Screen? = null
) {
    object Parks : Screen("parks")
    object Profile : Screen("profile")
    object UserParks : Screen("user_parks/{userId}", parentTab = Profile)

    companion object {
        fun findParentTab(route: String): Screen? {
            val baseRoute = route.substringBefore("/")
            return allScreens.find {
                it.route.substringBefore("/") == baseRoute
            }?.parentTab
        }
    }
}
```

### 2. AppState

```kotlin
class AppState(val navController: NavHostController) {
    val currentTopLevelDestination: TopLevelDestination?
        @Composable get() {
            val route = currentDestination?.route ?: return null
            return findTopLevelDestinationForRoute(route)
        }

    private fun findTopLevelDestinationForRoute(route: String): TopLevelDestination? {
        val baseRoute = route.substringBefore("/")
        topLevelDestinations.find {
            it.route.substringBefore("/") == baseRoute
        }?.let { return it }

        val parentTabRoute = Screen.findParentTab(route)?.route?.substringBefore("/")
        return topLevelDestinations.find {
            it.route.substringBefore("/") == parentTabRoute
        }
    }

    val isCurrentRouteTopLevel: Boolean
        @Composable get() {
            val route = currentDestination?.route ?: return false
            val baseRoute = route.substringBefore("/")
            return Screen.allScreens.find {
                it.route.substringBefore("/") == baseRoute
            }?.parentTab == null
        }

    fun navigateToTopLevelDestination(topLevelDestination: TopLevelDestination) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        val currentTab = currentRoute?.let { findTopLevelDestinationForRoute(it) }
        val isReselect = currentTab?.route == topLevelDestination.route

        if (isReselect) {
            navController.navigate(topLevelDestination.route) {
                popUpTo(topLevelDestination.route) { inclusive = true }
                launchSingleTop = true
            }
        } else {
            navController.navigate(topLevelDestination.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
}
```

### 3. RootScreen

```kotlin
Scaffold(
    topBar = {
        if (appState.isCurrentRouteTopLevel) {
            when (appState.currentTopLevelDestination?.route) {
                Screen.Profile.route -> ProfileTopAppBar(appState = appState, ...)
                // ...
            }
        }
    },
    bottomBar = { BottomNavigationBar(appState = appState) },
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
) { paddingValues ->
    NavHost(...) {
        composable(route = Screen.Profile.route) {
            ProfileRootScreen(modifier = Modifier.padding(paddingValues), ...)
        }
        composable(route = Screen.UserParks.route) {
            ParksAddedByUserScreen(
                parentPaddingValues = paddingValues,
                ...
            )
        }
    }
}
```

### 4. Дочерний экран (UserParks)

```kotlin
Scaffold(
    topBar = { CenterAlignedTopAppBar(...) },
    contentWindowInsets = WindowInsets(0, 0, 0, 0)
) { innerPadding ->
    ParksListView(
        modifier = modifier
            .padding(parentPaddingValues)  // BottomNavigationBar
            .padding(innerPadding),          // TopAppBar
        ...
    )
}
```

---

## Статус реализации

### ✅ Выполнено

1. ✅ Добавлено поле `parentTab` в класс `Screen`
2. ✅ Реализован метод `findParentTab()` для поиска родительской вкладки
3. ✅ Реализован метод `isCurrentRouteTopLevel()` в `AppState`
4. ✅ Реализован метод `findTopLevelDestinationForRoute()` в `AppState`
5. ✅ Обновлена логика `navigateToTopLevelDestination()` для обработки подэкранов
6. ✅ Обновлен `RootScreen` для условного отображения `topBar`
7. ✅ Обновлены дочерние экраны для использования `parentPaddingValues`
8. ✅ Проект успешно собран
9. ✅ Код отформатирован
10. ✅ Нет ошибок линтера

### 📋 Сценарии проверки

- [x] Повторное нажатие на вкладку с подэкраном → возврат на корневой экран
- [x] Повторное нажатие на вкладку Profile из ParksAddedByUserScreen → возврат на корень
- [x] Переход между вкладками → корректная навигация с сохранением состояния
- [x] Повторное нажатие на вкладку без подэкранов → ничего не происходит
- [x] Кнопка Back из подэкрана → возврат на корневой экран вкладки
- [x] Сохранение состояния вкладок при переключении → состояние сохранено

---

## Безопасные зоны

Используется `contentWindowInsets = WindowInsets(0, 0, 0, 0)` в Scaffold и добавление отступов через `paddingValues` (RootScreen) и `innerPadding` (дочерние экраны).

---

## Дополнительные улучшения (в будущем)

1. Переход на Navigation 3 (когда станет стабильным)
2. Добавить типобезопасные NavKeys для навигации
3. Адаптивная навигация (NavigationRail/Drawer для планшетов)
4. Анимации переходов между вкладками
5. Улучшить обработку deeplinks
