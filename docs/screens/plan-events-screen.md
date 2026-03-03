# План доработок экрана со списком мероприятий (EventsScreen)

## Прогресс выполнения

**Этап 1: 100%** ✅ (завершён 2026-03-03)
**Этап 2: 100%** ✅ (завершён 2026-03-03)
**Этап 3: 100%** ✅ (завершён 2026-03-03)
**Этап 4: 100%** ✅ (завершён 2026-03-03)
**Этап 5: 33%** 🟡 (баг 5.1 ✅, баги: 5.2 pull-to-refresh, 5.3 лишний отступ)

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

### Баг 5.2: Pull-to-refresh не работает

**Проблема:**
Pull-to-refresh не отображает состояние обновления (индикатор не крутится).

**Причина:**
- В JournalsListScreen используется отдельный `isRefreshing: StateFlow<Boolean>`
- В EventsScreen используется `isLoading` внутри `EventsUIState.Content`
- PullToRefreshBox требует отдельного `isRefreshing` состояния

**Референс (JournalsListScreen.kt:121-122, 229-233):**

```kotlin
val isRefreshing by viewModel.isRefreshing.collectAsState()
// ...
PullToRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = onRefresh,
    // ...
)
```

**План исправления:**

1. **IEventsViewModel.kt** - добавить:

   ```kotlin
   val isRefreshing: StateFlow<Boolean>
   ```

2. **EventsViewModel.kt** - добавить:

   ```kotlin
   private val _isRefreshing = MutableStateFlow(false)
   override val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
   ```

   Обновить `refresh()`:

   ```kotlin
   override fun refresh() {
       logger.d(TAG, "Обновление списка мероприятий")
       _isRefreshing.value = true
       viewModelScope.launch {
           when (_selectedTab.value) {
               EventKind.FUTURE -> loadFutureEventsInternal()
               EventKind.PAST -> syncPastEventsInternal()
           }
           _isRefreshing.value = false
       }
   }
   ```

3. **EventsScreen.kt** - использовать `isRefreshing`:

   ```kotlin
   val isRefreshing by viewModel.isRefreshing.collectAsState()
   // ...
   PullToRefreshBox(
       isRefreshing = isRefreshing,
       onRefresh = onRefresh,
       // ...
   )
   ```

**Чек-лист:**
- [ ] Добавить `isRefreshing` в IEventsViewModel
- [ ] Добавить `_isRefreshing` в EventsViewModel
- [ ] Обновить метод `refresh()` для управления `isRefreshing`
- [ ] Обновить EventsScreen для использования `isRefreshing`
- [ ] Обновить FakeEventsViewModel
- [ ] Обновить UI тесты

---

### Баг 5.3: Лишний отступ между TopAppBar и контентом

**Проблема:**
Между TopAppBar и SegmentedButtonRow появляется лишнее пустое пространство.

**Причина:**
Scaffold применяет windowInsets для status bar дважды.

**Референс (JournalsListScreen.kt:182):**

```kotlin
Scaffold(
    // ...
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    // ...
)
```

**План исправления (EventsScreen.kt):**

1. Добавить импорт:

   ```kotlin
   import androidx.compose.foundation.layout.WindowInsets
   ```

2. Добавить `contentWindowInsets` в Scaffold (строка 57):

   ```kotlin
   Scaffold(
       topBar = { EventsTopAppBar() },
       contentWindowInsets = WindowInsets(0, 0, 0, 0),
       floatingActionButton = { /* ... */ }
   ) { paddingValues ->
   ```

**Чек-лист:**
- [ ] Добавить `contentWindowInsets = WindowInsets(0, 0, 0, 0)` в Scaffold
- [ ] Проверить визуально на устройстве/эмуляторе

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
