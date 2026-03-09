# Навигация: возврат к родительской вкладке

## Обзор

Реализация навигации в Jetpack-WorkoutApp с поддержкой:
- Возврата к корневому экрану вкладки при повторном нажатии на вкладку bottom navigation
- Сохранения активной вкладки на всю глубину стека навигации (Full Source Propagation)

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

### Основные механизмы

1. **Поле `parentTab`** в `Screen` — определяет UI-поведение (какой TopAppBar показывать)
2. **Параметр `source`** в маршрутах — определяет физический стек навигации
3. **"Липкое" состояние `currentTopLevelDestination`** — запоминает вкладку при навигации вглубь
4. **Слушатель `onDestinationChanged`** с `arguments` — обновляет активную вкладку с учетом `source`
5. **Функция `getScreenBySource()`** — маппинг source → Screen

### Разделение логики и физики навигации

| Концепция                     | Определяет                    | Источник данных            |
|-------------------------------|-------------------------------|----------------------------|
| **Физика** (фактический стек) | Из какой вкладки открыт экран | Параметр `source`          |
| **Логика** (parentTab)        | UI-поведение (TopAppBar)      | Статичное поле `parentTab` |

Пример: `Chat` открыт из вкладки `Parks`:
- Физика: стек `Parks`, активная вкладка — `Parks` (из `source=parks`)
- Логика: `parentTab = Messages` → показываем TopAppBar с кнопкой "Назад"
- Результат: возврат назад вернёт в `Parks`

---

## Full Source Propagation

### Проблема

Экраны профилей пользователей (`UserFriends`, `UserParks`, `JournalsList` и др.) всегда возвращали `parentTab = Profile`, независимо от начальной вкладки навигации.

### Решение

Реализована **Full Source Propagation** — явная передача `source` параметра через всю цепочку навигации. Каждый экран наследует `source` от родителя и передает дочерним экранам.

### Принцип работы

```
Messages (вкладка Messages) ✓
  ↓
UserSearch (source=messages) ✓
  ↓
OtherUserProfile (source=messages) ✓
  ↓
UserFriends (source=messages) ✓
  ↓
ParkDetail (source=messages) ✓
```

### Экраны с source параметром

22 экрана поддерживают `source` параметр:

**Экраны пользователей:**
- `UserSearch`, `OtherUserProfile`, `UserFriends`, `UserParks`, `UserTrainingParks`
- `JournalsList`, `JournalEntries`

**Экраны площадок:**
- `ParkDetail`, `EditPark`, `CreateEventForPark`, `ParkRoute`
- `AddParkComment`, `ParkTrainees`, `ParkGallery`

**Экраны мероприятий:**
- `EventDetail`, `EditEvent`, `EventParticipants`, `EventGallery`, `AddEventComment`

**Экраны сообщений:**
- `Chat`

---

## Детальная реализация

### 1. Screen (Destinations.kt)

```kotlin
sealed class Screen(
    val route: String,
    val parentTab: Screen? = null  // Влияет только на UI
) {
    object Parks : Screen("parks")
    object Profile : Screen("profile")
    object Messages : Screen("messages")

    // Экран с source параметром
    object UserFriends : Screen("user_friends/{userId}?source={source}", parentTab = Profile) {
        fun createRoute(userId: Long, source: String = "profile") =
            "user_friends/$userId?source=$source"

        fun findParentTab(arguments: Bundle?): Screen {
            val source = arguments?.getString("source") ?: "profile"
            return getScreenBySource(source, default = Profile)
        }
    }

    companion object {
        fun findParentTab(route: String, arguments: Bundle? = null): Screen? {
            val baseRoute = route.substringBefore("/").substringBefore("?")
            return when (baseRoute) {
                "user_friends" -> UserFriends.findParentTab(arguments)
                "user_parks" -> UserParks.findParentTab(arguments)
                // ... и т.д. для всех экранов с source
                else -> allScreens.find { ... }?.parentTab
            }
        }
    }
}

fun getScreenBySource(source: String, default: Screen): Screen {
    return when (source) {
        "parks" -> Screen.Parks
        "events" -> Screen.Events
        "messages" -> Screen.Messages
        "profile" -> Screen.Profile
        "more" -> Screen.More
        else -> default
    }
}
```

### 2. AppState — "липкое" состояние вкладки

```kotlin
class AppState(val navController: NavHostController) {

    var currentTopLevelDestination by mutableStateOf<TopLevelDestination?>(null)
        private set

    fun onDestinationChanged(route: String?, arguments: Bundle? = null) {
        // 1. Прямое совпадение с корневой вкладкой
        val matchingTab = topLevelDestinations.find { it.route == route }
        if (matchingTab != null) {
            currentTopLevelDestination = matchingTab
            return
        }

        // 2. Определение parentTab с учетом source из аргументов
        val parentTab = Screen.findParentTab(route ?: "", arguments)
        if (parentTab != null) {
            val parentTopLevelDestination = topLevelDestinations.find { it.route == parentTab.route }
            if (parentTopLevelDestination != null) {
                currentTopLevelDestination = parentTopLevelDestination
                return
            }
        }
    }

    fun navigateToTopLevelDestination(topLevelDestination: TopLevelDestination) {
        val isReselect = currentTopLevelDestination?.route == topLevelDestination.route

        if (isReselect) {
            // Повторное нажатие — сбрасываем стек до корня
            navController.navigate(topLevelDestination.route) {
                popUpTo(topLevelDestination.route) { inclusive = true }
                launchSingleTop = true
            }
        } else {
            // Переход на другую вкладку
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
fun rememberAppState(navController: NavHostController = rememberNavController()): AppState {
    val appState = remember(navController) { AppState(navController) }

    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, arguments ->
            appState.onDestinationChanged(destination.route, arguments)
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    return appState
}
```

### 4. RootScreen — извлечение source из arguments

```kotlin
composable(
    route = Screen.OtherUserProfile.route,
    arguments = listOf(
        navArgument("userId") { type = NavType.LongType },
        navArgument("source") { type = NavType.StringType; defaultValue = "profile" }
    )
) { backStackEntry ->
    val source = backStackEntry.arguments?.getString("source") ?: "profile"
    OtherUserProfileScreen(
        source = source,
        onNavigateToFriends = { userId ->
            navController.navigate(Screen.UserFriends.createRoute(userId, source))
        },
        // ...
    )
}
```

---

## Роль parentTab

Поле `parentTab` используется для:

### 1. Определения TopAppBar через `isCurrentRouteTopLevel`

- `parentTab != null` → дочерний экран → показываем его собственный TopAppBar
- `parentTab == null` → корневой экран → показываем главный TopAppBar из `RootScreen`

### 2. Определения активной вкладки (fallback)

Если у экрана нет `source` параметра, используется статичный `parentTab`.

### Что НЕ делает parentTab

- ❌ НЕ определяет физический стек навигации (используется `source`)
- ❌ НЕ влияет на поведение при нажатии на вкладку (используется `currentTopLevelDestination`)

---

## Статус реализации

### ✅ Выполнено

1. ✅ Добавлено поле `parentTab` в класс `Screen`
2. ✅ Реализован метод `findParentTab()` с поддержкой `arguments`
3. ✅ Реализована функция `getScreenBySource()`
4. ✅ Реализовано "липкое" состояние `currentTopLevelDestination` в `AppState`
5. ✅ Реализован слушатель `onDestinationChanged()` с `arguments`
6. ✅ Реализован метод `isCurrentRouteTopLevel()` в `AppState`
7. ✅ Обновлена логика `navigateToTopLevelDestination()`
8. ✅ Добавлен `source` параметр для 22 экранов
9. ✅ Обновлены экраны UI для передачи `source` в дочерние экраны
10. ✅ Unit-тесты: `DestinationsTest.kt` — 42 теста для `getScreenBySource()` и `findParentTab()`
11. ✅ Проект собирается, тесты проходят (1076 тестов)

### 📋 Сценарии проверки

- [x] Повторное нажатие на вкладку с подэкраном → возврат на корневой экран
- [x] Переход между вкладками → корректная навигация с сохранением состояния
- [x] Кнопка Back из подэкрана → возврат на предыдущий экран
- [x] Восстановление стека с дочерним экраном → вкладка отображается корректно
- [x] Кросс-навигация (Chat из Parks) → активная вкладка остаётся Parks
- [x] Full Source Propagation — сохранение вкладки на всю глубину стека

---

## Нереализованные экраны (TODO в RootScreen.kt)

Следующие экраны имеют заглушки и требуют реализации:

- [ ] `ParkDetailScreen` - детальный просмотр площадки
- [ ] `CreateParkScreen` - создание площадки
- [ ] `EditParkScreen` - редактирование площадки
- [ ] `EventDetailScreen` - детальный просмотр мероприятия
- [ ] `CreateEventScreen` - создание мероприятия
- [ ] `EditEventScreen` - редактирование мероприятия
- [ ] `ChatScreen` - экран чата

Эти экраны уже настроены для приема `source` параметра в `Destinations.kt`.

---

## Безопасные зоны

Используется `contentWindowInsets = WindowInsets(0, 0, 0, 0)` в Scaffold и добавление отступов через `paddingValues` (RootScreen) и `innerPadding` (дочерние экраны).

---

## Добавление новых экранов

### Экран с фиксированной вкладкой

```kotlin
object NewScreen : Screen("new_screen", parentTab = Screen.Profile)
```

### Экран с динамической вкладкой (source)

```kotlin
object NewScreen : Screen("new_screen/{itemId}?source={source}", parentTab = Profile) {
    fun createRoute(itemId: Long, source: String = "profile") =
        "new_screen/$itemId?source=$source"

    fun findParentTab(arguments: Bundle?): Screen {
        val source = arguments?.getString("source") ?: "profile"
        return getScreenBySource(source, default = Profile)
    }
}

// В Screen.findParentTab():
"new_screen" -> NewScreen.findParentTab(arguments)
```

---

## Исправленные баги

### Баг: двойное выделение вкладок после авторизации ✅

**Симптомы до исправления:**
- После логина на экране `Profile` визуально подсвечивались две вкладки: `Parks` и `Profile`
- В логах навигации активной вкладкой при этом уже был `profile`
- Баг воспроизводился чаще в момент ожидания ответа сервера после авторизации, до завершения загрузки профиля

**Фактическая причина:**
- `NavController` и `currentTopLevelDestination` были корректны, проблема находилась в transient visual state `NavigationBarItem`
- В auth-flow есть короткое окно между `onLoginSuccess` и фактическим обновлением `currentUser` из `ProfileViewModel`
- В это окно нативный `NavigationBarItem` мог кратковременно показывать устаревший highlight первой вкладки, хотя route уже корректный

**Ключевое наблюдение из логов:**

Сразу после `currentUser` обновление было корректным, но визуальный артефакт ещё мог кратко появляться:

```text
RootScreen: currentUser изменился: 10367, isAuthorized=true
BottomNavigationBar: рекомпозиция, currentDestination=profile, isAuthorized=true
```

Это подтверждает, что проблема была не в state маршрута, а в визуальном состоянии `NavigationBarItem` в переходный момент.

**Исправление:**
1. Сохранён нативный `NavigationBar` + `NavigationBarItem` (без кастомной вёрстки)
2. `BottomNavigationBar()` явно читает `appState.isAuthorized`
3. В `AppState` добавлен `bottomNavVisualEpoch` и метод `bumpBottomNavVisualEpoch()`
4. В `RootScreen` epoch увеличивается сразу в `onLoginSuccess` и `onRegisterSuccess`, до `loadProfileFromServer(...)`
5. В `BottomNavigationBar` `key(...)` и `interactionSource` завязаны на `route + isAuthorized + epoch`, чтобы гарантированно сбрасывать transient visual state при auth transition
6. В `LoginSheetHost` и `RegisterSheetHost` перед закрытием sheet очищаются focus и keyboard

**Актуальное ожидаемое поведение:**
- После logout на `Profile` нижняя навигация остаётся на `Profile`
- После повторного login на `Profile` происходит рекомпозиция с новым `epoch`:

```text
BottomNavigationBar: рекомпозиция, currentDestination=profile, isAuthorized=true, epoch=<N>
```

- Визуально подсвечивается только одна вкладка: `Profile`

**Минимальные диагностические логи, которые нужно сохранять:**
- `RootScreen`: изменение `currentUser`
- `Navigation`: `OnDestinationChangedListener`, `onDestinationChanged()`, `navigateToTopLevelDestination()`
- `BottomNavigation`: рекомпозиция бара с `currentDestination`, `isAuthorized`, `epoch`, плюс лог клика по вкладке

Этого достаточно, чтобы в будущем отличить:
- проблему навигационного state
- проблему отсутствия рекомпозиции
- проблему чисто визуального состояния нижнего бара

**Обязательное правило для будущей разработки:**

Если экран может менять авторизацию пользователя без смены текущего top-level route
(например: login sheet, register sheet, re-auth flow, refresh session, восстановление сессии при старте),
то UI нижней навигации должен иметь явный механизм сброса transient visual state в auth transition
(в текущей реализации это `bottomNavVisualEpoch`).

Иначе возможен такой класс багов:
- маршрут остаётся `profile`
- `AppState` корректен
- но `NavigationBarItem` кратковременно показывает устаревший highlight

**Практическое правило для новых экранов и flow:**
1. Не полагаться только на обновление `currentUser` для корректного визуального состояния `NavigationBarItem`
2. В новых auth-flow (login/register/re-auth) при необходимости увеличивать `bottomNavVisualEpoch` в точке успеха авторизации
3. Сохранять нативный `NavigationBarItem`, но контролировать его transient состояние через ключи композиции
4. Для любых новых auth-related flow после реализации прогонять сценарий:
   - сложная навигация по вкладкам
   - переход в дочерние экраны
   - logout
   - login на `Profile`
   - проверка единственной активной вкладки

**Контрольный лог успешного сценария:**

```text
RootScreen: currentUser изменился: 10367, isAuthorized=true
BottomNavigationBar: рекомпозиция, currentDestination=profile, isAuthorized=true, epoch=<N>
```

Если после успешной авторизации `epoch` не меняется и снова появляется временный highlight первой вкладки, значит проблема вернулась в auth transition.

---

## Дополнительные улучшения (в будущем)

1. Переход на Navigation 3 (когда станет стабильным)
2. Добавить типобезопасные NavKeys для навигации
3. Адаптивная навигация (NavigationRail/Drawer для планшетов)
4. Анимации переходов между вкладками
5. Улучшить обработку deeplinks
