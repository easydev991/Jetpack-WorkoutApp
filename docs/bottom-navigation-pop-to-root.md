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

1. **Поле `parentTab`** в `Screen` для определения принадлежности экрана к вкладке (влияет только на UI)
2. **"Липкое" состояние `currentTopLevelDestination`** — запоминает вкладку при входе, сохраняет при навигации вглубь
3. **Слушатель `onDestinationChanged`** — обновляет активную вкладку только при попадании на корневой экран
4. **Метод `isCurrentRouteTopLevel`** — определяет тип экрана для условного отображения TopAppBar
5. **Логика `navigateToTopLevelDestination()`** — сброс стека при reselect на основе "липкого" состояния

### Важный архитектурный принцип

**Разделение логики и физики навигации:**

- **Физика** (фактический стек): определяется тем, из какой вкладки мы открыли экран
- **Логика** (parentTab): определяет только UI-поведение (какой TopAppBar показывать)

Пример: если открыть `Chat` из вкладки `Parks`:
- Физика: мы в стеке `Parks`, активная вкладка — `Parks`
- Логика: `Chat` имеет `parentTab = Messages`, поэтому показываем его TopAppBar с кнопкой "Назад"
- Результат: возврат назад вернёт в `Parks`, а не в `Messages`

---

## Детальная реализация

### 1. Screen (Destinations.kt)

```kotlin
sealed class Screen(
    val route: String,
    val parentTab: Screen? = null  // Влияет только на UI, не на переключение вкладок
) {
    object Parks : Screen("parks")
    object Profile : Screen("profile")
    object Messages : Screen("messages")
    object Chat : Screen("chat/{dialogId}", parentTab = Messages)  // Логически в Messages

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

### 2. AppState — "липкое" состояние вкладки

```kotlin
class AppState(val navController: NavHostController) {

    /**
     * Текущее верхнеуровневое назначение (вкладка).
     * Состояние "липкое": обновляется только когда мы попадаем на корневой экран вкладки.
     * Если мы уходим вглубь вкладки, это поле хранит ссылку на "родительскую" вкладку.
     * Это позволяет корректно определять физический стек навигации.
     */
    var currentTopLevelDestination by mutableStateOf<TopLevelDestination?>(null)
        private set

    /**
     * Обновляет активную вкладку, если маршрут соответствует одной из корневых вкладок.
     */
    fun onDestinationChanged(route: String?) {
        val matchingTab = topLevelDestinations.find { it.route == route }
        if (matchingTab != null) {
            currentTopLevelDestination = matchingTab
        }
    }

    /**
     * Проверяет, является ли текущий маршрут корневым экраном.
     * Используется для условного отображения главного TopAppBar в RootScreen.
     */
    val isCurrentRouteTopLevel: Boolean
        @Composable get() {
            val route = currentDestination?.route ?: return false
            val baseRoute = route.substringBefore("/")
            val screen = Screen.allScreens.find {
                it.route.substringBefore("/") == baseRoute
            }
            return screen?.parentTab == null
        }

    fun navigateToTopLevelDestination(topLevelDestination: TopLevelDestination) {
        // Используем "липкое" состояние — помнит вкладку даже на дочерних экранах
        val isReselect = currentTopLevelDestination?.route == topLevelDestination.route

        if (isReselect) {
            // Повторное нажатие на текущую вкладку — сбрасываем стек до корня
            navController.navigate(topLevelDestination.route) {
                popUpTo(topLevelDestination.route) { inclusive = true }
                launchSingleTop = true
            }
        } else {
            // Переход на другую вкладку — стандартная логика
            navController.navigate(topLevelDestination.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
}
```

### 3. rememberAppState — подписка на изменения навигации

```kotlin
@Composable
fun rememberAppState(
    navController: NavHostController = rememberNavController(),
): AppState {
    val appState = remember(navController) {
        AppState(navController)
    }

    // Слушаем изменения навигации для обновления активной вкладки
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            appState.onDestinationChanged(destination.route)
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    return appState
}
```

### 4. RootScreen

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

### 5. Дочерний экран (пример)

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

## Роль parentTab

**Важно:** Поле `parentTab` в `Destinations.kt` влияет **только на UI**, а не на переключение вкладок.

### Что делает parentTab:

1. **Определяет显示TopAppBar** через `isCurrentRouteTopLevel`
   - Если у экрана есть `parentTab` → это не корневой экран → показываем его собственный TopAppBar
   - Если `parentTab == null` → корневой экран → показываем главный TopAppBar из `RootScreen`

### Что НЕ делает parentTab:

- ❌ НЕ определяет активную вкладку в BottomNavigationBar
- ❌ НЕ влияет на поведение при нажатии на вкладку
- ❌ НЕ определяет стек навигации

### Пример:

```kotlin
object Chat : Screen("chat/{dialogId}", parentTab = Messages)
```

Если открыть `Chat` из вкладки `Parks`:
- Активная вкладка: `Parks` (физический стек)
- TopAppBar: показываем собственный TopAppBar экрана `Chat` (потому что есть `parentTab`)
- Кнопка "Назад": вернёт в `Parks` (физический стек)

---

## Статус реализации

### ✅ Выполнено

1. ✅ Добавлено поле `parentTab` в класс `Screen`
2. ✅ Реализован метод `findParentTab()` для поиска родительской вкладки
3. ✅ Реализовано "липкое" состояние `currentTopLevelDestination` в `AppState`
4. ✅ Реализован слушатель `onDestinationChanged()` через `DisposableEffect`
5. ✅ Реализован метод `isCurrentRouteTopLevel()` в `AppState`
6. ✅ Обновлена логика `navigateToTopLevelDestination()` для использования "липкого" состояния
7. ✅ Обновлен `RootScreen` для условного отображения `topBar`
8. ✅ Обновлены дочерние экраны для использования `parentPaddingValues`
9. ✅ Проект успешно собран
10. ✅ Код отформатирован
11. ✅ Нет ошибок линтера

### 📋 Сценарии проверки

- [x] Повторное нажатие на вкладку с подэкраном → возврат на корневой экран
- [x] Повторное нажатие на вкладку Profile из ParksAddedByUserScreen → возврат на корень
- [x] Переход между вкладками → корректная навигация с сохранением состояния
- [x] Повторное нажатие на вкладку без подэкранов → ничего не происходит
- [x] Кнопка Back из подэкрана → возврат на корневой экран вкладки
- [x] Сохранение состояния вкладок при переключении → состояние сохранено
- [x] Кросс-навигация (открытие Chat из Parks) → активная вкладка остаётся Parks

---

## Безопасные зоны

Используется `contentWindowInsets = WindowInsets(0, 0, 0, 0)` в Scaffold и добавление отступов через `paddingValues` (RootScreen) и `innerPadding` (дочерние экраны).

---

## Добавление новых экранов

При добавлении нового экрана в `Destinations.kt`:

```kotlin
object NewScreen : Screen("new_screen", parentTab = Screen.Profile) // или другая вкладка
```

Это будет работать корректно:
1. Если открыть его из Профиля — это будет обычный переход вглубь
2. Если открыть его из Сообщений — он откроется поверх стека Сообщений, вкладка "Сообщения" останется активной
3. Кнопка "Назад" вернёт на предыдущий экран в физическом стеке

---

## Дополнительные улучшения (в будущем)

1. Переход на Navigation 3 (когда станет стабильным)
2. Добавить типобезопасные NavKeys для навигации
3. Адаптивная навигация (NavigationRail/Drawer для планшетов)
4. Анимации переходов между вкладками
5. Улучшить обработку deeplinks
