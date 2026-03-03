# План доработок экрана со списком мероприятий (EventsScreen)

## Прогресс выполнения

**Этап 1: 100%** ✅ (завершён 2026-03-03)
**Этап 2: 100%** ✅ (завершён 2026-03-03)
**Этап 3: 100%** ✅ (завершён 2026-03-03)
**Этап 4: 100%** ✅ (завершён 2026-03-03)
**Этап 5: 100%** ✅ (баг 5.1 ✅, баг 5.2 ✅, баг 5.3 ✅, баг 5.4 ✅)

---

## Описание задачи

Доработка существующего экрана `EventsScreen` для поддержки двух типов мероприятий:
- **Предстоящие мероприятия** (future_events) - загружаются с сервера 1 раз при первом открытии приложения, кэшируются в памяти до закрытия приложения (без БД), доступны для pull-to-refresh
- **Прошедшие мероприятия** (past_events) - кэшируются в локальную БД Room, доступны офлайн, доступны для pull-to-refresh

### Референсы

- iOS-версия: `SwiftUI-WorkoutApp/Screens/Events/EventsListScreen.swift`
- Android-референсы: `JournalsListScreen.kt`, `MyFriendsScreen.kt`

### Ключевые отличия от iOS

- Вместо кнопки `+` в TopAppBar используем FAB-кнопку
- FAB видна только авторизованному пользователю
- На первой итерации FAB логирует нажатие в консоль

---

## Этап 1: Data Layer - кэширование прошедших мероприятий ✅

**Выполнено:**
- `EventEntity.kt` - entity с мапперами `toEntity()`/`toEvent()`
- `EventDao.kt` - DAO с `getAllPastEvents()`, `insertEvents()`, `deleteAll()`, `replaceAll()`
- `SWDatabase.kt` - добавлен `EventEntity::class` и `eventDao()`
- `SWRepository.kt` - методы `getPastEventsFlow()` и `syncPastEvents()`
- `AppContainer.kt` - инъекция `eventDao` в `SWRepositoryImp`

---

## Этап 2: Domain Layer - ViewModel ✅

**Выполнено:**
- `EventsUIState` - sealed interface с Content, Error, InitialLoading
- `IEventsViewModel` - интерфей с `eventsUIState`, `isAuthorized`, `onTabSelected`, `refresh`, `onEventClick`, `onFabClick`
- `EventsViewModel` - переписан с Logger, UserPreferencesRepository, кэшированием FUTURE в памяти, подпиской на PAST из БД через Flow
- `EventsViewModelTest.kt` - 13 unit-тестов (init, tab selection, refresh, click handlers, error handling)

---

## Этап 3: UI Layer - EventsScreen ✅

**Выполнено:**
- `SegmentedButtonRow` - Material3 `SingleChoiceSegmentedButtonRow` для переключения FUTURE/PAST
- `EventsScreen.kt` - Scaffold с topBar, FAB, PullToRefreshBox, EventsList с EmptyStateView
- FAB - отображается только для авторизованных, логирует нажатие
- Pull-to-Refresh - работает для обоих типов (FUTURE: обновляет кэш в памяти, PAST: синхронизирует с API и сохраняет в БД)
- Локализация - `events_empty_future`, `events_empty_past`, `events_fab_description`

---

## Этап 4: UI тесты и интеграция ✅

**Выполнено 2026-03-03:**
- Создан `FakeEventsViewModel.kt` для тестов
- Создан `EventsScreenTest.kt` с 14 UI тестами:
- Unit-тесты проходят (1095)
- Код отформатирован
- Интеграция с RootScreen работает

---

## Файлы для изменения

| Файл                                   | Действие   | Статус           |
|----------------------------------------|------------|------------------|
| `EventEntity.kt`                       | Создать    | ✅ Выполнено     |
| `EventDao.kt`                          | Создать    | ✅ Выполнено     |
| `SWDatabase.kt`                        | Изменить   | ✅ Выполнено     |
| `SWRepository.kt`                      | Изменить   | ✅ Выполнено     |
| `EventsViewModel.kt`                   | Переписать | ✅ Выполнено     |
| `IEventsViewModel.kt`                  | Изменить   | ✅ Выполнено     |
| `EventsUIState` (в EventsViewModel.kt) | Изменить   | ✅ Выполнено     |
| `EventsScreen.kt`                      | Переписать | ✅ Выполнено     |
| `EventsViewModelTest.kt`               | Переписать | ✅ Выполнено (13 тестов) |
| `EventsScreenTest.kt`                  | Создать    | ✅ Выполнено (14 тестов) |
| `FakeEventsViewModel.kt`                | Создать    | ✅ Выполнено     |
| `AppContainer.kt`                      | Изменить   | ✅ Выполнено     |
| `strings.xml`                          | Изменить   | ✅ Выполнено     |
| `strings-ru/strings.xml`               | Изменить   | ✅ Выполнено     |

---

## Примечания

### Сортировка мероприятий

Все мероприятия сортируются по `beginDate` от новых к старым:

```kotlin
events.sortedByDescending { it.beginDate }
```

### Авторизация

Проверка авторизации через `viewModel.isAuthorized` (StateFlow из UserPreferencesRepository)

```kotlin
val isAuthorized by viewModel.isAuthorized.collectAsState()
if (isAuthorized) {
    // Показать FAB
}
```

### Обработка ошибок

Все ошибки обрабатываются через `userNotifier.handleError(AppError)`

```kotlin
userNotifier.handleError(
    AppError.Network(
        message = "Не удалось загрузить мероприятия",
        throwable = e
    )
)
```

### Логирование

Логгер передается через интерфейс `Logger`:

```kotlin
class EventsViewModel(
    private val getFutureEventsUseCase: IGetFutureEventsUseCase,
    private val getPastEventsFlowUseCase: IGetPastEventsFlowUseCase,
    private val syncPastEventsUseCase: ISyncPastEventsUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userNotifier: UserNotifier,
    private val logger: Logger,
)
```

---

## Этап 5: Исправление багов 🟡

### Баг 5.1: PAST-события не загружаются при переключении вкладки ✅ ИСПРАВЛЕНО И ПРОВЕРЕНО

**Симптомы:**
- При переключении на вкладку PAST индикатор загрузки крутится бесконечно
- Список прошедших мероприятий не отображается
- Логи показывают: HTTP запрос отправлен, но ответ никогда не приходит

**Найденная причина:**
EventsViewModel вызывал репозиторий **напрямую**, а JournalsViewModel использует **Use Cases**.

**Сравнение архитектуры:**

| Экран | Паттерн | Результат |
|-------|---------|-----------|
| JournalsListScreen | ViewModel → UseCase → Repository | ✅ Работает |
| EventsScreen | ViewModel → Repository (напрямую) | ❌ Виснет |

**Выполненное исправление:**

1. **Созданы Use Cases для мероприятий:**
   - `IGetPastEventsFlowUseCase.kt` - интерфейс для получения Flow прошедших мероприятий
   - `GetPastEventsFlowUseCase.kt` - реализация (Flow из БД)
   - `ISyncPastEventsUseCase.kt` - интерфейс для синхронизации
   - `SyncPastEventsUseCase.kt` - реализация (API → БД)
   - `IGetFutureEventsUseCase.kt` - интерфейс для будущих мероприятий
   - `GetFutureEventsUseCase.kt` - реализация (API)

2. **Переписан EventsViewModel:**
   - Конструктор теперь принимает Use Cases вместо SWRepository
   - `loadFutureEvents()` использует `getFutureEventsUseCase()`
   - `observePastEvents()` использует `getPastEventsFlowUseCase()`
   - `syncPastEvents()` использует `syncPastEventsUseCase()`

3. **Обновлён AppContainer:**
   - Добавлены объявления Use Cases в интерфейс
   - Добавлены реализации Use Cases
   - `eventsViewModelFactory()` инъектирует Use Cases

4. **Обновлены unit-тесты:**
   - `EventsViewModelTest.kt` переписан для использования mock Use Cases
   - Все 14 тестов проходят

5. **Проверено на устройстве:**
   - PAST события успешно загружаются (50 мероприятий)
   - Логи подтверждают: `Результат syncPastEvents: isSuccess=true`
   - Данные получены через Flow: `Получены прошедшие мероприятия из Flow: 50 шт.`

**Чек-лист:**
- [x] Создать `IGetPastEventsFlowUseCase.kt`
- [x] Создать `GetPastEventsFlowUseCase.kt`
- [x] Создать `ISyncPastEventsUseCase.kt`
- [x] Создать `SyncPastEventsUseCase.kt`
- [x] Создать `IGetFutureEventsUseCase.kt`
- [x] Создать `GetFutureEventsUseCase.kt`
- [x] Переписать `EventsViewModel.kt` для использования Use Cases
- [x] Обновить `AppContainer.kt` для инъекции Use Cases
- [x] Обновить unit-тесты `EventsViewModelTest.kt`
- [x] Проверить на устройстве ✅ (50 мероприятий загружено)

---

### Баг 5.2: Pull-to-refresh не работает на пустом списке ✅ ИСПРАВЛЕНО

**Проблема:**
Pull-to-refresh работает когда список мероприятий есть, но не работает когда список пуст (нет мероприятий). Индикатор обновления не появляется при свайпе вниз.

**Причина:**
EmptyStateView внутри PullToRefreshBox не имеет достаточной высоты для активации pull-to-refresh жеста. Нужно обернуть EmptyStateView в Box с `fillMaxSize()` и `verticalScroll()` (как в DialogsContent)

**Выполненное исправление:**

1. **IEventsViewModel.kt** — добавлен `isRefreshing`:

   ```kotlin
   val isRefreshing: StateFlow<Boolean>
   ```

2. **EventsViewModel.kt** — добавлены `_isRefreshing` и обновлён `refresh()`:

   ```kotlin
   private val _isRefreshing = MutableStateFlow(false)
   override val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

   override fun refresh() {
       viewModelScope.launch {
           _isRefreshing.value = true
           try {
               when (_selectedTab.value) {
                   EventKind.FUTURE -> loadFutureEventsInternal()
                   EventKind.PAST -> syncPastEventsInternal()
               }
           } finally {
               _isRefreshing.value = false
           }
       }
   }
   ```

3. **EventsScreen.kt** — используется `isRefreshing` и EmptyStateView обёрнут в Box с scroll:

   ```kotlin
   val isRefreshing by viewModel.isRefreshing.collectAsState()
   
   PullToRefreshBox(
       isRefreshing = isRefreshing,
       onRefresh = onRefresh,
       modifier = modifier.fillMaxSize()
   ) {
       if (events.isEmpty()) {
           Box(
               modifier = Modifier
                   .fillMaxSize()
                   .verticalScroll(rememberScrollState()),
               contentAlignment = Alignment.Center
           ) {
               EmptyStateView(...)
           }
       } else {
           EventsList(...)
       }
   }
   ```

4. **FakeEventsViewModel.kt** — добавлен `isRefreshing`:

   ```kotlin
   override val isRefreshing: StateFlow<Boolean> = MutableStateFlow(false)
   ```

**Чек-лист:**
- [x] Добавить `isRefreshing` в IEventsViewModel
- [x] Добавить `_isRefreshing` в EventsViewModel
- [x] Обновить метод `refresh()` для управления `isRefreshing`
- [x] Обновить EventsScreen для использования `isRefreshing`
- [x] Обернуть EmptyStateView в Box с fillMaxSize() и verticalScroll()
- [x] Обновить FakeEventsViewModel
- [x] Unit-тесты проходят

---

### Баг 5.4: Список прошедших мероприятий пропадает при переключении вкладок ✅ ИСПРАВЛЕНО

**Симптомы:**
- При первой загрузке список прошедших мероприятий отображается корректно (50 шт.)
- При переключении FUTURE → PAST → FUTURE → PAST список пропадает
- Отображается EmptyStateView ("no upcoming events")
- Логи при первой загрузке: `Получены прошедшие мероприятия из Flow: 50 шт.`
- Логи при переключении вкладок: только `Переключение вкладки: PAST` (без получения из Flow)

**Причина (анализ кода EventsViewModel.kt):**

Проблема в методе `onTabSelected()` (строки 182-199):

```kotlin
override fun onTabSelected(tab: EventKind) {
    _selectedTab.value = tab

    if (tab == EventKind.FUTURE) {
        // FUTURE обновляет UI state из кэша ✅
        val events = futureEventsCache ?: emptyList()
        _eventsUIState.value = EventsUIState.Content(...)
    } else {
        // PAST только запускает синхронизацию при первой загрузке
        // НО не обновляет UI state при последующих переключениях! ❌
        if (!hasLoadedPastEvents) {
            syncPastEvents()
        }
        // ← здесь должен быть код для обновления UI state из кэша PAST
    }
}
```

Flow в `observePastEvents()` (строки 165-180) эмитит только при изменении данных в БД:
- При первой синхронизации — эмитит ✅
- При последующих переключениях — БД не изменилась, Flow не эмитит ❌

**Выполненное исправление:**

1. **EventsViewModel.kt** — добавлен кэш для PAST:

   ```kotlin
   private var pastEventsCache: List<Event>? = null
   ```

2. **EventsViewModel.kt** — обновлён `observePastEvents()` для сохранения в кэш:

   ```kotlin
   private fun observePastEvents() {
       viewModelScope.launch {
           getPastEventsFlowUseCase().collect { pastEvents ->
               val sortedEvents = pastEvents.sortedByDescending { it.beginDate }
               pastEventsCache = sortedEvents  // ← кэшируем
               // ...
           }
       }
   }
   ```

3. **EventsViewModel.kt** — обновлён `onTabSelected()` для PAST:

   ```kotlin
   override fun onTabSelected(tab: EventKind) {
       _selectedTab.value = tab

       if (tab == EventKind.FUTURE) {
           val events = futureEventsCache ?: emptyList()
           _eventsUIState.value = EventsUIState.Content(...)
       } else {
           val events = pastEventsCache ?: emptyList()
           _eventsUIState.value = EventsUIState.Content(
               events = events,
               selectedTab = EventKind.PAST,
               isLoading = false
           )

           if (!hasLoadedPastEvents) {
               syncPastEvents()
           }
       }
   }
   ```

**Чек-лист:**
- [x] Добавить `private var pastEventsCache: List<Event>? = null` в EventsViewModel
- [x] Обновить `observePastEvents()` для сохранения в `pastEventsCache`
- [x] Обновить `onTabSelected()` для PAST — устанавливать UI state из кэша
- [x] Unit-тесты проходят
- [ ] Проверить на устройстве: переключение FUTURE ↔ PAST несколько раз

---

### Баг 5.3: Лишний отступ между TopAppBar и контентом ✅ ИСПРАВЛЕНО

**Проблема:**
Между TopAppBar и SegmentedButtonRow появлялось лишнее пустое пространство.

**Причина:**
EventsScreen использовал вложенный Scaffold с собственным TopAppBar, а RootScreen также рендерил EventsTopAppBar. Это приводило к двойному рендерингу TopAppBar и лишним отступам.

**Выполненное исправление:**
- Удалён вложенный Scaffold из EventsScreen.kt
- Заменён на простой Box с контентом
- TopAppBar теперь рендерится только в RootScreen

**Результат:**
Лишний отступ устранён, TopAppBar отображается корректно.

---

## История изменений

| Дата       | Изменение                                                    |
|------------|--------------------------------------------------------------|
| 2026-03-03 | Создан план доработок EventsScreen                           |
| 2026-03-03 | Этап 1-3 выполнены полностью                                 |
| 2026-03-03 | **Сжатие плана**: описания выполненных этапов уплотнены, дублирование удалено |
| 2026-03-03 | **Этап 4 завершён**: UI тесты написаны, проходят, код отформатирован |
| 2026-03-03 | **Этап 5 обновлён**: добавлены баги 5.1 (PAST), 5.2 (pull-to-refresh), 5.3 (отступ) с пошаговым планом исправления |
| 2026-03-03 | **Баг 5.1 исправлен и проверен**: EventsViewModel переписан на Use Cases, созданы 6 Use Case файлов, тесты обновлены, проверено на устройстве (50 мероприятий загружено) |
| 2026-03-03 | **Баг 5.3 исправлен**: Удалён вложенный Scaffold из EventsScreen, TopAppBar теперь рендерится только в RootScreen |
| 2026-03-03 | **Баг 5.2 актуализирован**: Pull-to-refresh работает когда список есть, но не работает на пустом списке |
| 2026-03-03 | **Баг 5.4 добавлен**: Список прошедших мероприятий пропадает при переключении вкладок (анализ кода + план исправления) |
| 2026-03-03 | **Баг 5.4 исправлен**: Добавлен pastEventsCache, обновлён onTabSelected() для обновления UI state при переключении |
| 2026-03-03 | **Баг 5.2 исправлен**: Добавлен isRefreshing StateFlow, EmptyStateView обёрнут в Box с scroll для pull-to-refresh на пустом списке |
