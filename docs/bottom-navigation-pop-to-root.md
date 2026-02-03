# Анализ и реализация навигации: Jetpack-WorkoutApp

## Обзор

Документ содержит описание реализации навигации в Jetpack-WorkoutApp, сравнение с Now in Android (NIA) и описание реализованного функционала возврата к родительской вкладке при повторном нажатии на вкладку bottom navigation.

---

## Текущая реализация в Jetpack-WorkoutApp

### Компоненты навигации

1. **AppState** (`com.swparks.navigation.AppState`)
   - Использует стандартный `NavController` из Navigation Component
   - Управляет состоянием авторизации пользователя
   - Определяет верхнеуровневые назначения (вкладки)
   - Определяет принадлежность экрана к вкладке (через `parentTab`)

2. **BottomNavigationBar** (`com.swparks.navigation.BottomNavigationBar`)
   - Простая реализация с `NavigationBar` и `NavigationBarItem`
   - При клике вызывает `appState.navigateToTopLevelDestination(destination)`

3. **Destinations.kt** (`com.swparks.navigation.Screen`)
   - Содержит все экраны приложения
   - Каждому экрану назначен `parentTab` (родительская вкладка)
   - Верхнеуровневые экраны (вкладки) не имеют `parentTab`

---

## Реализация в Now in Android

### Архитектура навигации

NIA использует **Navigation 3** (экспериментальная версия) с кастомной реализацией.

### Основные компоненты

1. **Navigator** (`com.google.samples.apps.nowinandroid.core.navigation.Navigator`)
   - Центральный компонент для управления навигацией
   - Обрабатывает все навигационные события

2. **NavigationState** (`com.google.samples.apps.nowinandroid.core.navigation.NavigationState`)
   - Управляет состоянием навигации
   - Хранит два уровня стека навигации:
     - `topLevelStack` - стек верхнеуровневых назначений (вкладки)
     - `subStacks` - под-стеки для каждой вкладки (хранятся в `Map<NavKey, NavBackStack<NavKey>>`)

3. **NavKey**
   - Типобезопасный ключ навигации
   - Все ключи сериализуемы через `@Serializable`

### Ключевая логика сброса навигационного стека

```kotlin
// Navigator.kt
fun navigate(key: NavKey) {
    when (key) {
        state.currentTopLevelKey -> clearSubStack()  // ← ВАЖНО: очистка при повторном нажатии
        in state.topLevelKeys -> goToTopLevel(key)
        else -> goToKey(key)
    }
}

private fun clearSubStack() {
    state.currentSubStack.run {
        if (size > 1) subList(1, size).clear()  // Оставляем только корневой экран
    }
}
```

**Объяснение логики:**
- При навигации на ключ `NavKey`:
  1. Если это текущий ключ (повторное нажатие на вкладку) → вызываем `clearSubStack()`
  2. Если это другой верхнеуровневый ключ → переходим на него
  3. Иначе → добавляем в под-стек

- `clearSubStack()`:
  - Оставляет только первый элемент в под-стеке (корневой экран текущей вкладки)
  - Удаляет все остальные элементы под-стека

### Структура стека навигации в NIA

```
topLevelStack (главный стек вкладок)
  ↓
ForYouNavKey → BookmarksNavKey → InterestsNavKey
  ↓
subStacks (под-стеки для каждой вкладки)
  ForYouNavKey: [ForYouNavKey]
  BookmarksNavKey: [BookmarksNavKey, BookmarkDetailNavKey, ...]
  InterestsNavKey: [InterestsNavKey, TopicNavKey(topicId), ...]
```

---

## Сравнение реализаций

| Характеристика | Jetpack-WorkoutApp (наше) | Now in Android |
|---------------|---------------------------|----------------|
| **Навигационный компонент** | Navigation Component (v2) | Navigation 3 (экспериментальный) |
| **Управление стеком** | NavController (один стек) | Navigator + NavigationState (два уровня стека) |
| **Сброс под-стека при reselect** | ✅ Реализовано через `parentTab` и `isCurrentRouteTopLevel` | ✅ Реализовано через `clearSubStack()` |
| **Сохранение состояния вкладок** | ✅ Через `saveState`/`restoreState` | ✅ Через отдельные под-стеки |
| **Типобезопасность** | ⚠️ Partial (строковые маршруты) | ✅ Полная (NavKey + @Serializable) |
| **Адаптивная навигация** | ❌ Только BottomBar | ✅ NavigationSuiteScaffold (Bar/Rail/Drawer) |

---

## Реализованное решение

### Проблема

В текущей реализации Jetpack-WorkoutApp при повторном нажатии на вкладку bottom navigation не происходил возврат на корневой экран этой вкладки, если пользователь находился на дочернем экране.

### Решение

Мы реализовали подход на основе `parentTab` для определения принадлежности экрана к вкладке и корректной обработки навигации.

### Архитектурные решения

#### Основная идея

1. **Добавление поля `parentTab`** в класс `Screen`:
   - Каждому дочернему экрану назначается родительская вкладка
   - Верхнеуровневые экраны (вкладки) не имеют `parentTab`

2. **Определение типа маршрута** в `AppState`:
   - `isCurrentRouteTopLevel` - проверяет, является ли текущий экран корневым (верхнеуровневым) или дочерним
   - `currentTopLevelDestination` - находит родительскую вкладку для текущего маршрута (учитывает подэкраны)

3. **Управление отображением UI в `RootScreen`**:
   - `topBar` показывается только для корневых экранов (`if (appState.isCurrentRouteTopLevel)`)
   - `bottomBar` показывается всегда (и на корневых, и на дочерних экранах)

4. **Логика навигации** в `navigateToTopLevelDestination()`:
   - При повторном нажатии на текущую вкладку → сбрасываем стек до корня этой вкладки
   - При переходе на другую вкладку → стандартная логика с сохранением/восстановлением состояния

---

## Детальная реализация

### Шаг 1: Добавление поля parentTab в Screen

```kotlin
// Destinations.kt
sealed class Screen(
    val route: String,
    val parentTab: Screen? = null  // <-- Родительская вкладка
) {
    // Верхнеуровневые вкладки (без parentTab)
    object Parks : Screen("parks")
    object Events : Screen("events")
    object Messages : Screen("messages")
    object Profile : Screen("profile")
    object More : Screen("more")

    // Дочерние экраны Profile (с parentTab)
    object UserParks : Screen("user_parks/{userId}", parentTab = Profile)
    object UserTrainingParks : Screen("user_training_parks/{userId}", parentTab = Profile)
    object EditProfile : Screen("edit_profile", parentTab = Profile)
    // ... и т.д.

    companion object {
        val allScreens by lazy {
            listOf(
                Parks, Events, Messages, Profile, More,
                UserParks, UserTrainingParks, EditProfile,
                // ... все экраны
            )
        }

        /**
         * Находит родительскую вкладку для маршрута
         */
        fun findParentTab(route: String): Screen? {
            val baseRoute = route.substringBefore("/")
            return allScreens.find {
                it.route.substringBefore("/") == baseRoute
            }?.parentTab
        }
    }
}
```

### Шаг 2: Обновление AppState

```kotlin
// AppState.kt
class AppState(
    val navController: NavHostController,
) {
    // ... существующий код ...

    /**
     * Текущее верхнеуровневое назначение (вкладка)
     * Учитывает подэкраны — если мы на UserParks, вернет PROFILE
     */
    val currentTopLevelDestination: TopLevelDestination?
        @Composable get() {
            val route = currentDestination?.route ?: return null
            return findTopLevelDestinationForRoute(route)
        }

    /**
     * Находит TopLevelDestination для маршрута (учитывает parentTab)
     */
    private fun findTopLevelDestinationForRoute(route: String): TopLevelDestination? {
        val baseRoute = route.substringBefore("/")

        // Сначала ищем точное совпадение с вкладкой
        topLevelDestinations.find {
            it.route.substringBefore("/") == baseRoute
        }?.let { return it }

        // Если не нашли, ищем через parentTab (для подэкранов)
        val parentTabRoute = Screen.findParentTab(route)?.route?.substringBefore("/")
        return topLevelDestinations.find {
            it.route.substringBefore("/") == parentTabRoute
        }
    }

    /**
     * Проверяет, является ли текущий маршрут корневым (верхнеуровневым) экраном
     * или дочерним (подэкраном внутри вкладки)
     * Возвращает true только для корневых экранов без parentTab
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

    /**
     * UI логика для навигации к верхнеуровневому назначению.
     * При повторном нажатии на текущую вкладку сбрасывает стек на корень.
     */
    fun navigateToTopLevelDestination(topLevelDestination: TopLevelDestination) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        val currentTab = currentRoute?.let { findTopLevelDestinationForRoute(it) }
        val isReselect = currentTab?.route == topLevelDestination.route

        if (isReselect) {
            // Повторное нажатие на текущую вкладку — сбрасываем стек до корня
            navController.navigate(topLevelDestination.route) {
                // Удаляем весь стек выше корня вкладки (включительно),
                // затем создаем заново
                popUpTo(topLevelDestination.route) {
                    inclusive = true  // Удаляем и сам корень вкладки
                }
                launchSingleTop = true
                // restoreState = false — не восстанавливаем, а создаем заново
            }
        } else {
            // Переход на другую вкладку — стандартная логика
            navController.navigate(topLevelDestination.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
}
```

### Шаг 3: Обновление RootScreen

```kotlin
// RootScreen.kt
@Composable
fun RootScreen(appState: AppState) {
    // ... существующий код ...

    Scaffold(
        topBar = {
            // Показываем TopAppBar только для корневых экранов вкладок
            // Для дочерних экранов (parentTab != null) TopAppBar показывается внутри самого экрана
            if (appState.isCurrentRouteTopLevel) {
                when (appState.currentTopLevelDestination?.route) {
                    Screen.Parks.route -> {
                        ParksTopAppBar(
                            appState = appState,
                            parksCount = parks.size
                        )
                    }
                    Screen.Events.route -> {
                        EventsTopAppBar()
                    }
                    Screen.Messages.route -> {
                        MessagesTopAppBar()
                    }
                    Screen.Profile.route -> {
                        ProfileTopAppBar(
                            appState = appState,
                            onSearchUsersClick = {
                                Log.i("RootScreen", "Нажата кнопка: Поиск пользователей")
                            }
                        )
                    }
                    Screen.More.route -> {
                        MoreTopAppBar()
                    }
                    null -> {}
                    else -> {}
                }
            }
        },
        bottomBar = {
            // BottomNavigationBar показываем всегда (и на корневых, и на дочерних экранах)
            BottomNavigationBar(appState = appState)
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->
        NavHost(
            navController = appState.navController,
            startDestination = Screen.Parks.route,
            modifier = Modifier.fillMaxSize(),
        ) {
            // ... composable для всех экранов ...

            // Вкладка "Профиль"
            composable(route = Screen.Profile.route) {
                ProfileRootScreen(
                    appContainer = appContainer,
                    viewModel = profileViewModel,
                    appState = appState,
                    onShowLoginSheet = { showLoginSheet = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)  // Учет TopAppBar и BottomNavigationBar
                )
            }

            // Дочерний экран (UserParks)
            composable(route = Screen.UserParks.route) {
                val currentUser by profileViewModel.currentUser.collectAsState()
                val addedParks = currentUser?.addedParks ?: emptyList()
                ParksAddedByUserScreen(
                    modifier = Modifier.fillMaxSize(),
                    parks = addedParks,
                    onBackClick = { appState.navController.popBackStack() },
                    onParkClick = { park ->
                        Log.d("RootScreen", "Нажата площадка: ${park.name}")
                    },
                    parentPaddingValues = paddingValues  // Учет BottomNavigationBar
                )
            }
        }
    }
}
```

### Шаг 4: Обновление дочерних экранов

Дочерние экраны имеют свой `Scaffold` с `TopAppBar` и используют `parentPaddingValues` для учета отступов от `BottomNavigationBar`:

```kotlin
// ParksAddedByUserScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParksAddedByUserScreen(
    modifier: Modifier = Modifier,
    parks: List<Park>,
    onBackClick: () -> Unit,
    onParkClick: (Park) -> Unit = { park ->
        Log.d("ParksAddedByUserScreen", "Нажата площадка: ${park.name}")
    },
    parentPaddingValues: PaddingValues  // <-- Получаем от RootScreen
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = stringResource(R.string.added_parks))
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        ParksListView(
            modifier = modifier
                .padding(parentPaddingValues)  // <-- Учет BottomNavigationBar
                .padding(innerPadding),          // <-- Учет собственного TopAppBar
            parks = parks,
            onParkClick = onParkClick
        )
    }
}
```

---

## Как это работает

### Определение типа маршрута

1. **Корневой экран (вкладка):**
   - Маршрут: `profile`
   - `parentTab`: `null`
   - `isCurrentRouteTopLevel`: `true`
   - `currentTopLevelDestination`: `PROFILE`

2. **Дочерний экран:**
   - Маршрут: `user_parks/123`
   - `parentTab`: `Profile`
   - `isCurrentRouteTopLevel`: `false`
   - `currentTopLevelDestination`: `PROFILE` (находит через `parentTab`)

### Отображение UI

1. **Корневой экран (Profile):**
   - `topBar`: Показывается `ProfileTopAppBar` из корневого `Scaffold`
   - `bottomBar`: Показывается `BottomNavigationBar`
   - Контент: Использует `paddingValues` от корневого `Scaffold`

2. **Дочерний экран (UserParks):**
   - `topBar`: **НЕ показывается** из корневого `Scaffold` (избегаем дублирования)
   - `bottomBar`: Показывается `BottomNavigationBar` (всегда)
   - `TopAppBar`: Показывается собственный `TopAppBar` внутри `ParksAddedByUserScreen`
   - Контент: Использует `parentPaddingValues` (от корневого `Scaffold`) + `innerPadding` (от собственного `Scaffold`)

### Логика навигации

1. **Повторное нажатие на текущую вкладку:**

   ```
   Пользователь на: Profile → UserParks
   Нажимает на: Profile (вкладка)
   Логика:
     - isReselect = true (текущая вкладка = Profile)
     - popUpTo(Profile.route, inclusive = true)
     - navigate(Profile.route)
   Результат:
     - Стек очищен до корня вкладки Profile
     - Пользователь на корневом экране Profile
   ```

2. **Переход на другую вкладку:**

   ```
   Пользователь на: Profile → UserParks
   Нажимает на: Parks (вкладка)
   Логика:
     - isReselect = false (текущая вкладка != Parks)
     - popUpTo(startDestination, saveState = true)
     - navigate(Parks.route, restoreState = true)
   Результат:
     - Стек очищен до корня всего графа навигации
     - Состояние вкладки Profile сохранено
     - Состояние вкладки Parks восстановлено
     - Пользователь на корневом экране Parks
   ```

---

## Преимущества реализованного решения

### По сравнению с NIA

| Характеристика | NIA | Jetpack-WorkoutApp (наша реализация) |
|---------------|------|--------------------------------------|
| **Сложность реализации** | Высокая (собственный Navigator) | Низкая (использует Navigation Component) |
| **Типобезопасность** | Полная (NavKey) | Частичная (строковые маршруты) |
| **Прозрачность логики** | Высокая (явная логика стеков) | Средняя (логика Navigation Component) |
| **Сохранение состояния** | Явное (отдельные под-стеки) | Автоматическое (saveState/restoreState) |
| **Управление UI** | Прямое | Через isCurrentRouteTopLevel |

### Преимущества нашего подхода

1. **Минимальные изменения:**
   - Не требует переписывания всей архитектуры навигации
   - Использует существующий Navigation Component

2. **Понятная логика:**
   - `parentTab` явно определяет принадлежность экрана к вкладке
   - `isCurrentRouteTopLevel` явно указывает тип экрана

3. **Гибкость:**
   - Легко добавить новые экраны
   - Легко изменить логику навигации

4. **Сохранение состояния:**
   - Автоматическое сохранение/восстановление состояния вкладок
   - Не требует ручного управления состоянием

---

## Безопасные зоны

### Реализация в Jetpack-WorkoutApp

В RootScreen используется:

```kotlin
Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),  // Scaffold не применяет insets
) { paddingValues ->
    NavHost(
        navController = appState.navController,
        startDestination = Screen.Parks.route,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(route = Screen.Parks.route) {
            ParksRootScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),  // padding от Scaffold
                // ...
            )
        }
    }
}
```

В дочерних экранах (например, ParksAddedByUserScreen):

```kotlin
Scaffold(
    topBar = { ... },
    contentWindowInsets = WindowInsets(0, 0, 0, 0)
) { innerPadding ->
    ParksListView(
        modifier = modifier
            .padding(parentPaddingValues)  // padding от RootScreen (BottomNavigationBar)
            .padding(innerPadding),          // padding от собственного Scaffold (TopAppBar)
        parks = parks,
        onParkClick = onParkClick
    )
}
```

Реализация безопасных зон в Jetpack-WorkoutApp использует `contentWindowInsets = WindowInsets(0, 0, 0, 0)` в Scaffold и добавляет отступы для контента через `paddingValues` и `innerPadding`.

---

## Резюме

### Ключевые реализации

1. **Определение родительской вкладки:**
   - Добавлено поле `parentTab` в класс `Screen`
   - Реализован метод `findParentTab()` для поиска родительской вкладки

2. **Определение типа маршрута:**
   - Реализован метод `isCurrentRouteTopLevel()` для проверки типа экрана
   - Реализован метод `findTopLevelDestinationForRoute()` для нахождения родительской вкладки

3. **Управление отображением UI:**
   - `topBar` показывается только для корневых экранов
   - `bottomBar` показывается всегда

4. **Логика навигации:**
   - Повторное нажатие на вкладку → сброс до корня
   - Переход на другую вкладку → сохранение/восстановление состояния

---

## Статус реализации

### ✅ Выполнено

1. ✅ Добавлено поле `parentTab` в класс `Screen`
2. ✅ Реализован метод `findParentTab()` для нахождения родительской вкладки
3. ✅ Реализован метод `isCurrentRouteTopLevel()` в `AppState`
4. ✅ Реализован метод `findTopLevelDestinationForRoute()` в `AppState`
5. ✅ Обновлена логика `navigateToTopLevelDestination()` для правильной обработки подэкранов
6. ✅ Обновлен `RootScreen` для условного отображения `topBar`
7. ✅ Обновлены дочерние экраны для использования `parentPaddingValues`
8. ✅ Проект успешно собран (`./gradlew assembleDebug`)
9. ✅ Код успешно отформатирован (`./gradlew ktlintFormat`)
10. ✅ Нет ошибок линтера

### 📋 Сценарии проверки функционала

Для полной проверки функционала необходимо протестировать следующие сценарии:

- [x] **Сценарий 1: Повторное нажатие на вкладку с подэкраном**
  1. Открыть вкладку Profile
  2. Перейти на подэкран (например, UserParks через Edit Profile → User Parks)
  3. Нажать на вкладку Profile в bottom navigation
  4. ✅ Ожидаемый результат: возврат на корневой экран Profile

- [x] **Сценарий 2: Повторное нажатие на вкладку профиля при открытом ParksAddedByUserScreen**
  1. Открыть вкладку Profile
  2. Перейти на экран ParksAddedByUserScreen (маршрут `user_parks/{userId}`)
  3. Нажать на вкладку Profile в bottom navigation
  4. ✅ Ожидаемый результат: возврат на корневой экран Profile

- [x] **Сценарий 3: Переход между вкладками**
  1. Открыть вкладку Parks
  2. Перейти на вкладку Events
  3. Перейти на вкладку Profile
  4. ✅ Ожидаемый результат: корректная навигация между вкладками с сохранением состояния

- [x] **Сценарий 4: Повторное нажатие на вкладку без подэкранов**
  1. Открыть вкладку Events
  2. Нажать на вкладку Events в bottom navigation
  3. ✅ Ожидаемый результат: ничего не происходит (уже на корневом экране)

- [x] **Сценарий 5: Проверка кнопки Back**
  1. Открыть вкладку Profile
  2. Перейти на подэкран (например, UserParks)
  3. Нажать кнопку Back
  4. ✅ Ожидаемый результат: возврат на корневой экран Profile

- [x] **Сценарий 6: Проверка сохранения состояния вкладок**
  1. Открыть вкладку Events
  2. Перейти на вкладку Profile
  3. Вернуться на вкладку Events
  4. ✅ Ожидаемый результат: состояние вкладки Events сохранено

---

## Дополнительные улучшения (в будущем)

1. Рассмотреть переход на Navigation 3 (когда станет стабильным)
2. Добавить типобезопасные NavKeys для навигации
3. Рассмотреть адаптивную навигацию (NavigationRail/Drawer для планшетов)
4. Добавить анимации переходов между вкладками
5. Улучшить обработку deeplinks

---

## Вывод

Реализованное решение обеспечивает корректную навигацию в приложении с поддержкой:

1. **Сброс навигационного стека при повторном нажатии на вкладку**
2. **Правильное отображение UI** (TopAppBar и BottomNavigationBar)
3. **Сохранение состояния вкладок**
4. **Гибкость** для добавления новых экранов

Решение использует стандартный Navigation Component, что делает его надежным и поддерживаемым в долгосрочной перспективе.
