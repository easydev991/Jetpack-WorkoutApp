# План доработок экрана со списком мероприятий (EventsScreen)

## Прогресс выполнения

- **Этап 1: 100%** ✅ Data Layer - кэширование прошедших мероприятий
- **Этап 2: 100%** ✅ Domain Layer - ViewModel
- **Этап 3: 100%** ✅ UI Layer - EventsScreen
- **Этап 4: 100%** ✅ UI тесты и интеграция
- **Этап 5: 100%** ✅ Исправление багов (5.1-5.5)
- **Этап 6: 100%** ✅ Блокировка нажатий на мероприятия во время refresh
- **Этап 7: 100%** ✅ Отображение адресов мероприятий (7.1-7.12)
- **Этап 8: 100%** ✅ Замена SegmentedButtonRow на PrimaryTabRow

---

## Выполненные этапы

### Этап 1: Data Layer ✅

EventEntity/EventDao, SWRepository с getPastEventsFlow()/syncPastEvents(), инъекция через AppContainer.

### Этап 2: Domain Layer ✅

EventsUIState (Content, Error, InitialLoading), IEventsViewModel + EventsViewModel с кэшированием FUTURE/PAST, unit-тесты.

### Этап 3: UI Layer ✅

SegmentedButtonRow → PrimaryTabRow, Scaffold с FAB, PullToRefreshBox, EventsList, локализация пустых состояний.

### Этап 4: UI тесты ✅

FakeEventsViewModel + EventsScreenTest (19 тестов), интеграция с RootScreen.

### Этап 5: Исправление багов ✅

Загрузка PAST при переключении, pull-to-refresh на пустом списке, отступ TopAppBar, сохранение PAST при переключении вкладок, pull-to-refresh для PAST.

### Этап 6: Блокировка нажатий ✅

`enabled: Boolean` в EventRowData/EventRowView/EventsList, передаётся `!isRefreshing`.

### Этап 7: Адреса мероприятий ✅

EventsUIState в `ui/state/`, addresses Map, CountriesRepository, loadAddress() с fallback, FAB без текста, управление loading-состояниями.

### Этап 8: PrimaryTabRow ✅

Замена SingleChoiceSegmentedButtonRow на PrimaryTabRow с selectedTabIndex, сохранение логики выбора табов.

---

## Описание задачи

Доработка `EventsScreen` для поддержки двух типов мероприятий:
- **Предстоящие** — загружаются с сервера, кэшируются в памяти, pull-to-refresh
- **Прошедшие** — кэшируются в Room, доступны офлайн, pull-to-refresh

### Референсы

- iOS: `SwiftUI-WorkoutApp/Screens/Events/EventsListScreen.swift`
- Android: `JournalsListScreen.kt`, `MyFriendsScreen.kt`

### Отличия от iOS

FAB вместо кнопки в TopAppBar, видна только авторизованному пользователю.

---

## История изменений

| Дата       | Изменение                                       |
|------------|-------------------------------------------------|
| 2026-03-03 | Создан план, этапы 1-4 выполнены                |
| 2026-03-04 | Баги 5.1-5.5 исправлены, этап 6 завершён        |
| 2026-03-04 | Этап 7 завершён: адреса, FAB, loading-состояния |
| 2026-03-04 | Этап 8 завершён: PrimaryTabRow                  |
