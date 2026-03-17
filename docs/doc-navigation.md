# План реализации навигации в Jetpack-WorkoutApp

## Введение

Этот план описывает реализацию навигации в приложении Jetpack-WorkoutApp на основе официального примера Now in Android (android/nowinandroid).

## Основные принципы из Now in Android

### Реализованные принципы

- **Единый NavHost** в приложении вместо множественных
- **AppState** для управления состоянием навигации и авторизации
- **Centralized Destinations** через sealed class Screen
- **Navigation Options** для вкладок (popUpTo, saveState, launchSingleTop, restoreState)
- **Верхнеуровневые назначения (вкладки)**: Parks, Events, Messages, Profile, More
- **Иерархия маршрутов**: каждый маршрут имеет parentTab для отслеживания вложенности (влияет только на UI)
- **"Липкое" состояние вкладки**: currentTopLevelDestination запоминает вкладку при входе и сохраняет при навигации вглубь

## Реализованные этапы

### Этап 1: Создание структуры навигации ✅

- ✅ `TopLevelDestination.kt` - data class для верхнеуровневых назначений (вкладок)
- ✅ `Destinations.kt` - sealed class Screen со всеми маршрутами навигации (40+ маршрутов)
- ✅ `AppState.kt` - класс для управления состоянием навигации и авторизации
  - Управление навигацией через NavHostController
  - "Липкое" состояние currentTopLevelDestination — запоминает вкладку при входе
  - Слушатель onDestinationChanged через DisposableEffect
  - Логика переключения вкладок с сохранением состояния
  - Управление состоянием авторизации (currentUser, isAuthorized)
- ✅ `Navigation.kt` (BottomNavigationBar) - компонент BottomNavigationBar для отображения вкладок
- ✅ Строковые ресурсы для заголовков экранов (parks_title, events_title, messages_title, profile_title, more_title)

### Этап 2: Рефакторинг RootScreen ✅

- ✅ Обновлен RootScreen.kt с использованием AppState и единого NavHost
- ✅ Реализован единый NavHost с 5 верхнеуровневыми вкладками
- ✅ Реализованы корневые экраны для всех вкладок:
  - ParksRootScreen
  - EventsScreen
  - MessagesRootScreen
  - ProfileRootScreen
  - MoreScreen
- ✅ Реализован единый ProfileViewModel на уровне RootScreen для синхронизации состояния между вкладками
- ✅ Интеграция с LoginSheetHost для авторизации
- ✅ Отображение ошибок через SnackbarHostState
- ✅ Реализована логика TopAppBar: показывается только для корневых экранов вкладок

### Этап 3: Иконки и дизайн ✅

- ✅ Иконки для вкладок обновлены в TopLevelDestinations
  - Icons.Filled.* для выбранной вкладки
  - Icons.Outlined.* для невыбранной вкладки
- ✅ Использованы Material Icons:
  - Parks: Icons.Filled.Map / Icons.Outlined.Map
  - Events: Icons.Filled.Event / Icons.Outlined.Event
  - Messages: Icons.AutoMirrored.Filled.Chat / Icons.AutoMirrored.Outlined.Chat
  - Profile: Icons.Filled.Person / Icons.Outlined.Person
  - More: Icons.AutoMirrored.Filled.List / Icons.AutoMirrored.Outlined.List

### Этап 4: Реализованные экраны ✅

#### Корневые экраны вкладок

- ✅ ParksRootScreen - список всех площадок
- ✅ EventsScreen - список мероприятий
- ✅ MessagesRootScreen - список диалогов
- ✅ ProfileRootScreen - профиль пользователя с авторизацией
- ✅ MoreScreen - экран настроек "Ещё"

#### Детальные экраны площадок

- ✅ ParksAddedByUserScreen - список площадок, добавленных пользователем

#### Детальные экраны профиля

- ✅ MyFriendsScreen - список друзей пользователя

#### Экраны настроек

- ✅ ThemeIconScreen - выбор иконки приложения

#### Компоненты UI

- ✅ ParksTopAppBar - верхняя панель для вкладки Площадки
- ✅ EventsTopAppBar - верхняя панель для вкладки Мероприятия
- ✅ MessagesTopAppBar - верхняя панель для вкладки Сообщения
- ✅ ProfileTopAppBar - верхняя панель для вкладки Профиль
- ✅ MoreTopAppBar - верхняя панель для вкладки Ещё

### Этап 5: Тестирование навигации ✅

- ✅ Переключение между вкладками работает корректно
- ✅ Сохранение состояния при переключении вкладок работает
- ✅ Повторное нажатие на вкладку сбрасывает стек навигации
- ✅ Реализована логика `isCurrentRouteTopLevel` для определения типа экрана
- ✅ Авторизация синхронизируется через AppState между всеми вкладками

### Этап 6: Проверка качества кода ✅

- ✅ Форматирование (ktlintFormat)
- ✅ Проверка линтеров (ktlintCheck, detekt)

## Актуальные файлы навигации

- `navigation/TopLevelDestination.kt` - data class для верхнеуровневых назначений (вкладок)
- `navigation/Destinations.kt` - sealed class Screen со всеми маршрутами навигации (40+ маршрутов)
- `navigation/AppState.kt` - класс для управления состоянием навигации и авторизации
- `navigation/Navigation.kt` - BottomNavigationBar для отображения вкладок
- `RootScreen.kt` - корневой экран с единым NavHost

## Структура маршрутов в Destinations.kt

### Верхнеуровневые вкладки (5 шт)

- `Screen.Parks` - вкладка Площадки
- `Screen.Events` - вкладка Мероприятия
- `Screen.Messages` - вкладка Сообщения
- `Screen.Profile` - вкладка Профиль
- `Screen.More` - вкладка Ещё

### Детальные экраны площадок (9 маршрутов)

- ✅ `Screen.ParkDetail` - реализован
- ✅ `Screen.CreatePark` - реализован
- ✅ `Screen.EditPark` - реализован
- ⏳ `Screen.ParkFilter` - маршрут есть, экран не подключен в RootScreen
- ✅ `Screen.CreateEventForPark` - реализован
- ⏳ `Screen.ParkRoute` - маршрут есть, экран не подключен в RootScreen
- ⏳ `Screen.AddParkComment` - маршрут есть, экран не подключен в RootScreen
- ✅ `Screen.ParkTrainees` - реализован
- ⏳ `Screen.ParkGallery` - маршрут есть, экран не подключен в RootScreen

### Детальные экраны мероприятий (6 маршрутов)

- ✅ `Screen.EventDetail` - реализован
- ✅ `Screen.CreateEvent` - реализован
- ✅ `Screen.EditEvent` - реализован
- ✅ `Screen.EventParticipants` - реализован
- ⏳ `Screen.EventGallery` - галерея фото мероприятия (маршрут есть, экрана нет)
- ⏳ `Screen.AddEventComment` - добавление комментария к мероприятию (маршрут есть, экрана нет)

### Экраны сообщений (4 маршрута)

- ✅ `Screen.Chat` - реализован
- ⏳ `Screen.Friends` - список друзей (маршрут есть, экрана нет)
- ✅ `Screen.MyFriends` - мои друзья (реализован)
- ✅ `Screen.UserSearch` - реализован

### Экраны профиля

- ✅ `Screen.EditProfile` - реализован
- ✅ `Screen.UserParks` - площадки пользователя (реализован через ParksAddedByUserScreen)
- ✅ `Screen.UserTrainingParks` - реализован
- ✅ `Screen.UserFriends` - реализован
- ✅ `Screen.OtherUserProfile` - реализован
- ✅ `Screen.Blacklist` - реализован
- ✅ `Screen.JournalsList` - реализован
- ✅ `Screen.JournalEntries` - реализован
- ✅ `Screen.ChangePassword` - реализован
- ✅ `Screen.SelectCountry` - реализован
- ✅ `Screen.SelectCity` - реализован

### Экраны настроек (1 маршрут) ✅

- ✅ `Screen.ThemeIcon` - выбор иконки приложения (реализован)

**Общее количество маршрутов в Destinations.kt**: 40+
**Актуализация**: 2026-03-17. Статусы обновлены по фактическому NavHost в RootScreen.

## План дальнейших задач

### Приоритет 1: Детальные экраны площадок

1. **ParkDetailScreen** - детальный экран площадки
   - Отображение информации о площадке
   - Список тренирующихся
   - Галерея фото
   - Комментарии
   - Создание мероприятия для площадки
   - Построение маршрута

2. **CreateParkScreen** - создание площадки
   - Форма создания площадки
   - Выбор страны/города (через CountriesRepository)
   - Загрузка фото

3. **EditParkScreen** - редактирование площадки
   - Форма редактирования
   - Обновление фото

### Приоритет 2: Детальные экраны мероприятий

1. **EventDetailScreen** - детальный экран мероприятия
   - Отображение информации о мероприятии
   - Список участников
   - Галерея фото
   - Комментарии
   - Кнопка "Пойти на мероприятие"

2. **CreateEventScreen** - создание мероприятия
   - Форма создания мероприятия
   - Выбор площадки
   - Выбор даты и времени

3. **EditEventScreen** - редактирование мероприятия
   - Форма редактирования

### Приоритет 3: Экраны сообщений

1. **ChatScreen** - чат с пользователем
   - История сообщений
   - Отправка новых сообщений

2. **FriendsScreen** - список друзей
   - Список друзей пользователя
   - Добавление в друзья
   - Удаление из друзей

3. **UserSearchScreen** - поиск пользователей
   - Поиск по логину/email
   - Просмотр профиля

### Приоритет 4: Экраны профиля

1. **EditProfileScreen** - редактирование профиля
   - Изменение имени, даты рождения
   - Выбор страны/города
   - Загрузка аватара

2. **UserTrainingParksScreen** - площадки, где тренируется пользователь

3. **BlacklistScreen** - черный список пользователей
   - Список заблокированных пользователей
   - Добавление/удаление из черного списка

### Приоритет 5: Экраны дневников

1. **JournalsListScreen** - список дневников пользователя

2. **JournalDetailScreen** - детальный экран дневника
   - Список записей
   - Создание новых записей
   - Редактирование записей

3. **CreateJournalScreen** - создание дневника

4. **EditJournalScreen** - редактирование настроек дневника

5. **AddJournalEntryScreen** - добавление записи в дневник

### Приоритет 6: Экраны настроек

1. **ChangePasswordScreen** - смена пароля

2. **SelectCountryScreen** - выбор страны (для профиля/площадок/мероприятий)

3. **SelectCityScreen** - выбор города (для профиля/площадок/мероприятий)

### Приоритет 7: Дополнительные функции

1. Добавление маршрутов с параметрами (частично реализовано в Destinations.kt)
2. Интеграция с Firebase Analytics (при необходимости)
3. Реализация сценариев для неавторизованных пользователей (частично - LoginSheet)
4. Добавление глубокой навигации (deep linking)
5. Оптимизация производительности навигации
6. Реализация анимаций переходов между экранами

## Анализ архитектурного подхода

Jetpack WorkoutApp частично следует архитектурному подходу Now in Android, особенно в части:

### Совпадения с Now in Android

- ✅ Использование Jetpack Compose для UI
- ✅ Использование Kotlin Flows для реактивности
- ✅ Разделение на слои (Presentation, Domain, Data)
- ✅ Единый NavHost вместо множественных
- ✅ AppState для управления состоянием навигации
- ✅ Centralized Destinations через sealed class Screen
- ✅ Navigation Options для вкладок (popUpTo, saveState, launchSingleTop, restoreState)

### Отличия от Now in Android

- ⚠️ AppState дополнительно управляет состоянием авторизации (currentUser, isAuthorized)
- ⚠️ Используется BottomNavigationBar вместо NavigationRail
- ⚠️ Больше функциональных областей (Parks, Events, Messages, Profile, More) vs 3 вкладки в NIA
- ⚠️ Сетевые функции через Retrofit с авторизацией (SWApi)
- ⚠️ Локальные данные через Room Database + JSON assets (countries.json, parks.json)

### Рекомендации

1. **Для детальных экранов**: Использовать ViewModel + Repository паттерн как в ProfileViewModel
2. **Для авторизации**: Продолжить использовать LoginSheetHost с синхронизацией через AppState
3. **Для_countries/cities**: Использовать CountriesRepository для выбора страны/города на экранах создания
4. **Для навигации**: Использовать "липкое" состояние вкладки — parentTab влияет только на UI, а активная вкладка определяется физическим стеком

## Технический долг

1. **Не реализованные экраны**: часть маршрутов в `Destinations.kt` пока не подключена в `RootScreen` (например, `ParkFilter`, `ParkRoute`, `AddParkComment`, `ParkGallery`, `EventGallery`, `AddEventComment`, `Friends`)
2. **Маршруты без экранов**: некоторые маршруты в Destinations.kt не имеют соответствующих экранов
3. **Интеграция с API**: большинство экранов пока работают с заглушками или JSON файлами
4. **Тестирование навигации**: отсутствуют UI-тесты для навигации
5. **Документация**: ✅ обновлена (docs/bottom-navigation-pop-to-root.md)

## Архитектурный принцип: разделение логики и физики навигации

### Проблема

Раньше навигация зависела от поля `parentTab` в `Destinations.kt`. Система "думала": *"Раз этот экран относится к профилю, значит мы сейчас в профиле"*. Это ломало логику при кросс-навигации (открытие экрана Chat из вкладки Parks).

### Решение

**"Липкое" состояние `currentTopLevelDestination`** — запоминает вкладку при входе и сохраняет при навигации вглубь.

- **Физика** (фактический стек): определяется тем, из какой вкладки мы открыли экран
- **Логика** (parentTab): определяет только UI-поведение (какой TopAppBar показывать)

### Пример

Если открыть `Chat` (parentTab = Messages) из вкладки `Parks`:
- Активная вкладка в BottomBar: `Parks` (физический стек)
- TopAppBar: показываем собственный TopAppBar экрана `Chat` (потому что есть parentTab)
- Кнопка "Назад": вернёт в `Parks` (физический стек)
- Повторное нажатие на "Parks": сбросит стек до корня

### Добавление новых экранов

```kotlin
object NewScreen : Screen("new_screen", parentTab = Screen.Profile)
```

Это будет работать корректно независимо от того, из какой вкладки открыт экран.
