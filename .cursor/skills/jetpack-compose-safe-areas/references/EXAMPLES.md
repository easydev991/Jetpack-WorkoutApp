# Примеры работы с безопасными зонами

## Корневой Scaffold (RootScreen)

### Пример реализации из проекта swparks

```kotlin
Scaffold(
    bottomBar = {
        BottomNavigationBar(appState = appState)
    },
    contentWindowInsets = WindowInsets(0, 0, 0, 0),  // ← отключает автоматические отступы для контента
) { paddingValues ->
    NavHost(
        navController = appState.navController,
        startDestination = Screen.Parks.route,
        modifier = Modifier.padding(paddingValues),  // ← paddingValues применяется к NavHost для BottomNavigationBar
    ) {
        // Маршруты навигации
    }
}
```

## Экраны с TopAppBar

### Pattern 1: Экран с TopAppBar + LazyColumn

**ВАЖНО:** Для LazyColumn нужно применять весь `paddingValues` через модификатор, а не через `contentPadding`!

#### Пример из ParksRootScreen

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParksRootScreen(
    parks: List<Park>,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = stringResource(R.string.parks_title, parks.size.toString()))
                },
                actions = {
                    IconButton(
                        onClick = {
                            Log.d("ParksRootScreen", "Кнопка фильтрации нажата")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterAlt,
                            contentDescription = stringResource(R.string.filter_parks)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),  // ← ВАЖНО: весь paddingValues через модификатор
            contentPadding = PaddingValues(
                start = dimensionResource(R.dimen.spacing_regular),
                top = dimensionResource(R.dimen.spacing_small),  // ← Маленький визуальный отступ
                end = dimensionResource(R.dimen.spacing_regular),
                bottom = dimensionResource(R.dimen.spacing_regular)
            ),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
            horizontalAlignment = Alignment.Start,
        ) {
            items(parks, key = { it.id }) { park ->
                ParkRowView(...)
            }
        }
    }
}
```

#### Пример из EventsScreen (PastEventsScreen)

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastEventsScreen(
    events: List<Event>,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.events_title))
                },
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier.padding(paddingValues),  // ← ВАЖНО: весь paddingValues через модификатор
            contentPadding = PaddingValues(
                start = dimensionResource(id = R.dimen.spacing_regular),
                top = dimensionResource(id = R.dimen.spacing_small),  // ← Маленький визуальный отступ
                end = dimensionResource(id = R.dimen.spacing_regular),
                bottom = dimensionResource(id = R.dimen.spacing_regular)
            ),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_small)),
            horizontalAlignment = Alignment.Start,
        ) {
            items(
                events,
                key = { it.id }) { event ->
                EventRowView(...)
            }
        }
    }
}
```

### Pattern 2: Экран с TopAppBar + Column

**ВАЖНО:** Использовать `fillMaxWidth()` вместо `fillMaxSize()` для правильной работы безопасных зон.

#### Пример из MoreScreen

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    navController: NavHostController? = null
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.more))
                },
            )
        }
    ) { paddingValues ->
        ScreenContent(
            modifier = Modifier.padding(paddingValues),  // ← ВАЖНО: весь paddingValues передается в контент
            context = context,
            uriHandler = uriHandler,
            navController = navController
        )
    }
}

@Composable
private fun ScreenContent(
    modifier: Modifier = Modifier,
    context: Context,
    uriHandler: UriHandler,
    navController: NavHostController? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_small_plus)),
        modifier = modifier
            .fillMaxWidth()  // ← ВАЖНО: fillMaxWidth, а не fillMaxSize
            .verticalScroll(rememberScrollState())
    ) {
        // Контент
    }
}
```

#### Пример из ThemeIconScreen

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeIconScreenContent(
    params: ThemeIconScreenParams,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_theme_and_icon)) },
                navigationIcon = {
                    IconButton(onClick = params.onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()  // ← ВАЖНО: fillMaxWidth, а не fillMaxSize
                    .verticalScroll(scrollState)
                    .padding(paddingValues)  // ← ВАЖНО: весь paddingValues через модификатор
                    .padding(16.dp),  // ← Визуальный отступ для контента
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Контент: ThemeSection, DynamicColorsSection, IconSection
        }
    }
}
```

### Экраны без TopAppBar

В текущей реализации проекта swparks все экраны используют TopAppBar, поэтому этот сценарий пока не применяется.

**Примечание:** Если появится экран без TopAppBar, необходимо вручную добавить безопасную зону сверху через `WindowInsets.systemBars.top`.

## Импорты для безопасных зон

```kotlin
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.res.dimensionResource
```

## Типичные ошибки

### Ошибка 1: Использование fillMaxSize() вместо fillMaxWidth()

**Проблема:** При использовании `fillMaxSize()` вместо `fillMaxWidth()` для Column при повороте экрана контент наезжает на камеру или динамик.

**Решение:** Используй `fillMaxWidth()` вместо `fillMaxSize()` для Column с вертикальной прокруткой.

```kotlin
// ❌ ПЛОХО
Column(
    modifier = Modifier
        .fillMaxSize()  // ← При повороте контент заезжает на камеру
        .padding(paddingValues)
        .verticalScroll(rememberScrollState())
)

// ✅ ХОРОШО
Column(
    modifier = Modifier
        .fillMaxWidth()  // ← Правильно: только ширина
        .padding(paddingValues)
        .verticalScroll(rememberScrollState())
)
```

### Ошибка 2: Использование contentPadding для LazyColumn с calculateTopPadding()

**Проблема:** При использовании `calculateTopPadding()` в `contentPadding` LazyColumn контент заезжает на камеру при повороте экрана.

**Решение:** Применяй весь `paddingValues` через модификатор LazyColumn, а `contentPadding` используй только для горизонтальных отступов и небольшого визуального отступа сверху.

```kotlin
// ❌ ПЛОХО
LazyColumn(
    modifier = Modifier,
    contentPadding = PaddingValues(
        start = 16.dp,
        top = paddingValues.calculateTopPadding(),  // ← НЕПРАВИЛЬНО!
        end = 16.dp,
        bottom = 16.dp
    )
)

// ✅ ХОРОШО
LazyColumn(
    modifier = Modifier.padding(paddingValues),  // ← ВАЖНО: весь paddingValues через модификатор
    contentPadding = PaddingValues(
        start = dimensionResource(R.dimen.spacing_regular),
        top = dimensionResource(R.dimen.spacing_small),  // Маленький визуальный отступ
        end = dimensionResource(R.dimen.spacing_regular),
        bottom = dimensionResource(R.dimen.spacing_regular)
    )
)
```

### Ошибка 3: Добавление windowInsets в TopAppBar

**Проблема:** Отключаются безопасные зоны, контент заезжает под камеру/динамик.

**Решение:** Не указывать `windowInsets` в TopAppBar, обработка автоматическая.

```kotlin
// ❌ ПЛОХО
CenterAlignedTopAppBar(
    title = { Text("Заголовок") },
    windowInsets = WindowInsets(top = 0)  // ← Отключает safe zones
)

// ✅ ХОРОШО
CenterAlignedTopAppBar(
    title = { Text("Заголовок") }
    // windowInsets НЕ указывается - автоматическая обработка
)
```

### Ошибка 4: Игнорирование paddingValues в контенте

**Проблема:** Контент перекрывается TopAppBar или BottomBar.

**Решение:** Всегда используй `paddingValues` в контенте.

```kotlin
// ❌ ПЛОХО - игнорирует paddingValues
Scaffold(
    topBar = { CenterAlignedTopAppBar(title = { Text("Заголовок") }) }
) { paddingValues ->
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())  // ← Нет paddingValues!
    ) { content... }
}

// ✅ ХОРОШО - использует paddingValues
Scaffold(
    topBar = { CenterAlignedTopAppBar(title = { Text("Заголовок") }) }
) { paddingValues ->
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(paddingValues)  // ← ВАЖНО: применяем paddingValues
            .verticalScroll(rememberScrollState())
    ) { content... }
}
```

### Ошибка 5: Отсутствие безопасной зоны на экранах без TopAppBar

**Проблема:** Контент заезжает под статус-бар.

**Решение:** Добавляй `WindowInsets.systemBars.top` вручную на экранах без TopAppBar.

```kotlin
// ❌ ПЛОХО
Scaffold(
    // Без topBar
) { paddingValues ->
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(paddingValues)  // ← Нет WindowInsets.systemBars.top!
            .verticalScroll(rememberScrollState())
    ) { content... }
}

// ✅ ХОРОШО
Scaffold(
    // Без topBar
) { paddingValues ->
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = WindowInsets.systemBars.top,  // ← ВАЖНО: добавлена safe zone вручную
                bottom = paddingValues.calculateBottomPadding()
            )
            .verticalScroll(rememberScrollState())
    ) { content... }
}
```

## Полные примеры реализованных экранов

### ParksRootScreen - LazyColumn

Полный пример экрана площадок с TopAppBar и LazyColumn:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParksRootScreen(
    parks: List<Park>,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = stringResource(R.string.parks_title, parks.size.toString()))
                },
                actions = {
                    IconButton(
                        onClick = {
                            Log.d("ParksRootScreen", "Кнопка фильтрации нажата")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterAlt,
                            contentDescription = stringResource(R.string.filter_parks)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),  // ← ВАЖНО
            contentPadding = PaddingValues(
                start = dimensionResource(R.dimen.spacing_regular),
                top = dimensionResource(R.dimen.spacing_small),
                end = dimensionResource(R.dimen.spacing_regular),
                bottom = dimensionResource(R.dimen.spacing_regular)
            ),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
            horizontalAlignment = Alignment.Start,
        ) {
            items(parks, key = { it.id }) { park ->
                ParkRowView(
                    data = ParkRowData(
                        imageStringURL = park.preview,
                        name = park.name,
                        address = park.address,
                        peopleTrainCount = park.trainingUsersCount ?: 0,
                        onClick = {
                            Log.d("ParksRootScreen", "Нажата площадка: ${park.name}")
                        }
                    )
                )
            }
        }
    }
}
```

### EventsScreen - LazyColumn

Полный пример экрана мероприятий с UI state и LazyColumn:

```kotlin
@Composable
fun EventsScreen(
    viewModel: EventsViewModel = viewModel(factory = EventsViewModel.Factory)
) {
    when (val uiState = viewModel.eventsUIState) {
        is EventsUIState.Loading -> LoadingOverlayView()
        is EventsUIState.Success -> PastEventsScreen(
            events = uiState.events,
            modifier = Modifier.fillMaxWidth()
        )
        is EventsUIState.Error -> ErrorContentView(
            retryAction = { viewModel.getPastEvents() },
            message = uiState.message
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastEventsScreen(
    events: List<Event>,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.events_title))
                },
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier.padding(paddingValues),  // ← ВАЖНО
            contentPadding = PaddingValues(
                start = dimensionResource(id = R.dimen.spacing_regular),
                top = dimensionResource(id = R.dimen.spacing_small),
                end = dimensionResource(id = R.dimen.spacing_regular),
                bottom = dimensionResource(id = R.dimen.spacing_regular)
            ),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_small)),
            horizontalAlignment = Alignment.Start,
        ) {
            items(
                events,
                key = { it.id }) { event ->
                EventRowView(
                    data = EventRowData(
                        imageStringURL = event.preview,
                        name = event.title,
                        dateString = event.beginDate,
                        address = "${event.countryID}, ${event.cityID}",
                        onClick = {
                            Log.d("EventsScreen", "Нажато мероприятие: ${event.title}")
                        }
                    )
                )
            }
        }
    }
}
```

### MoreScreen - Column

Полный пример экрана "Ещё" с TopAppBar и Column:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    navController: NavHostController? = null
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.more))
                },
            )
        }
    ) {
        ScreenContent(
            modifier = Modifier.padding(it),  // ← ВАЖНО: paddingValues передается в контент
            context = context,
            uriHandler = uriHandler,
            navController = navController
        )
    }
}

@Composable
private fun ScreenContent(
    modifier: Modifier = Modifier,
    context: Context,
    uriHandler: UriHandler,
    navController: NavHostController? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_small_plus)),
        modifier = modifier
            .fillMaxWidth()  // ← ВАЖНО
            .verticalScroll(rememberScrollState())
    ) {
        SettingsSection(navController = navController)
        HorizontalDivider()
        AboutAppSection(
            context = context,
            uriHandler = uriHandler
        )
        HorizontalDivider()
        OtherAppsSection(uriHandler = uriHandler)
        HorizontalDivider()
        SupportProjectSection(uriHandler = uriHandler)
    }
}
```

### ThemeIconScreen - Column

Полный пример экрана выбора темы и иконки с TopAppBar и Column:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeIconScreenContent(
    params: ThemeIconScreenParams,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_theme_and_icon)) },
                navigationIcon = {
                    IconButton(onClick = params.onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()  // ← ВАЖНО
                    .verticalScroll(scrollState)
                    .padding(paddingValues)  // ← ВАЖНО
                    .padding(16.dp),  // ← Визуальный отступ для контента
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ThemeSection(
                theme = params.theme,
                onThemeChange = params.onThemeChange,
            )
            HorizontalDivider()
            DynamicColorsSection(
                useDynamicColors = params.useDynamicColors,
                onDynamicColorsChange = params.onDynamicColorsChange,
            )
            HorizontalDivider()
            IconSection(
                icon = params.icon,
                onIconChange = params.onIconChange,
            )
        }
    }
}
```
