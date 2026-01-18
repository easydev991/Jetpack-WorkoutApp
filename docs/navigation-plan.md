# План рефакторинга навигации в Jetpack-WorkoutApp

## Текущее состояние навигации

### Анализ текущей реализации (Jetpack-WorkoutApp)

**Проблемы:**

1. **Множественные NavHost вместо единого**
   - Каждый раздел создает собственный NavHost (`ParksNavHost`, `EventsNavHost`, `MessagesNavHost`, `ProfileNavHost`)
   - Отсутствует централизованный контейнер для всех маршрутов
   - Сложность управления при масштабировании до 30+ экранов

2. **Разделение TabBarItem и маршрутов**
   - `TabBarItem` используется для вкладок (5 шт)
   - Маршруты определены в разных местах, нет единого источника истины
   - Отсутствует централизованный класс `Screen` для всех экранов

3. **Отсутствие RootScreenViewModel**
   - Нет централизованного управления состоянием текущей вкладки
   - Сложность синхронизации между UI и навигацией

4. **Отсутствие типобезопасной навигации**
   - Нет методов `createRoute()` для создания маршрутов с параметрами
   - Ручное формирование строк маршрутов, что увеличивает риск ошибок

5. **Отсутствие аналитики навигации**
   - Нет логирования screen_view событий для Firebase Analytics
   - Нет breadcrumb logs для отладки

### Анализ референсной реализации (JetpackDays)

**Преимущества:**

1. **Единый централизованный Screen класс**
   - Sealed class с route, icon, titleResId
   - Функции `createRoute()` для создания маршрутов с параметрами
   - Единственный источник истины для всех маршрутов

2. **Один NavHost в RootScreen**
   - Модульные функции для каждого назначения (`mainScreenDestination`, `detailScreenDestination` и т.д.)
   - Легкая масштабируемость при добавлении новых экранов
   - Управление видимостью BottomNavigationBar на основе текущего маршрута

3. **RootScreenViewModel для управления состоянием**
   - Управление текущей вкладкой через ViewModel
   - Методы `switchTab()`, `isEventsTabSelected()`, `isMoreTabSelected()`
   - Автоматическая синхронизация с текущим маршрутом через `updateTabBasedOnRoute()`

4. **Интеграция с Firebase Analytics**
   - Логирование screen_view событий для каждого экрана
   - Breadcrumb logs для отладки навигации

5. **Типобезопасная навигация**
   - `navController.navigate(Screen.ItemDetail.createRoute(itemId))`
   - Минимизация ошибок при формировании маршрутов

---

## Цели рефакторинга

1. **Фундамент для масштабируемой навигации** - создать базовый подход, который позволит легко добавлять новые экраны без значительных изменений в архитектуре
2. **Единая навигационная архитектура** - привести навигацию в соответствие с JetpackDays
3. **Типобезопасность** - использование методов `createRoute()` для навигации с параметрами
4. **Аналитика** - интеграция с Firebase Analytics для отслеживания экранов
5. **Поддерживаемость** - централизованное управление всеми маршрутами

**Важно:** Не реализовывать все экраны сразу, создать фундамент для легкого добавления новых экранов в будущем.

---

## Навигационная структура приложения

### Верхнеуровневые вкладки (5 шт)

1. **Площадки (Parks)**
2. **Мероприятия (Events)**
3. **Сообщения (Messages)**
4. **Профиль (Profile)**
5. **Ещё (More)**

---

## Детальное описание навигационных путей

### 1. Вкладка "Площадки" (Parks)

**Экраны:**

- `ParksRootScreen` - корневой экран с переключением между картой и списком
- `ParksMapScreen` - экран карты площадок (режим карты)
- `ParksListScreen` - экран списка площадок (режим списка)
- `ParkDetailScreen` - детальный экран площадки
- `CreateParkScreen` - экран создания новой площадки
- `EditParkScreen` - экран редактирования площадки (только для автора)
- `ParkFilterScreen` - экран фильтрации площадок (модальное представление)

**Навигация:**

**С ParksRootScreen:**

- Переключение между режимами: Карта ↔ Список (без изменения маршрута, только состояние UI)
- Открытие детального экрана площадки → `navController.navigate(Screen.ParkDetail.createRoute(parkId))`
- Создание новой площадки (кнопка + справа в TopAppBar) → `navController.navigate(Screen.CreatePark.route)`
- Открытие фильтра площадок (кнопка слева в TopAppBar) → открыть модальное окно

**С ParkDetailScreen:**

- Возврат назад → `navController.popBackStack()`
- Изменение площадки (только для автора) → `navController.navigate(Screen.EditPark.createRoute(parkId))`
- Создание события в календаре → `navController.navigate(Screen.CreateEventForPark.createRoute(parkId, date, time))`
- Маршрут до площадки → `navController.navigate(Screen.ParkRoute.createRoute(parkId))`
- Добавление комментария → `navController.navigate(Screen.AddParkComment.createRoute(parkId))`
- Люди, тренирующиеся на площадке → `navController.navigate(Screen.ParkTrainees.createRoute(parkId))`
- Галерея фотографий площадки → `navController.navigate(Screen.ParkGallery.createRoute(parkId))`
- Поделиться ссылкой (кнопка справа в TopAppBar) → системное share intent
- Отправить в поддержку email об обновлении (если площадка не автора) → системный email intent
- Создание мероприятия на площадке → `navController.navigate(Screen.CreateEventForPark.createRoute(parkId, date, time))`

**С CreateParkScreen / EditParkScreen:**

- Возврат назад → `navController.popBackStack()`

**С ParkFilterScreen (модальное окно):**

- Закрытие без сохранения → dismiss модального окна
- Применение фильтров → dismiss модального окна и обновить данные на ParksRootScreen

---

### 2. Вкладка "Мероприятия" (Events)

**Экраны:**

- `EventsRootScreen` - корневой экран с переключением между будущими и прошедшими мероприятиями
- `EventsUpcomingScreen` - экран будущих мероприятий
- `EventsPastScreen` - экран прошедших мероприятий
- `EventDetailScreen` - детальный экран мероприятия
- `CreateEventScreen` - экран создания нового мероприятия
- `EditEventScreen` - экран редактирования мероприятия (только для автора)

**Навигация:**

**С EventsRootScreen:**

- Переключение между режимами: Будущие ↔ Прошедшие (без изменения маршрута, только состояние UI)
- Открытие детального экрана мероприятия → `navController.navigate(Screen.EventDetail.createRoute(eventId))`
- Создание нового мероприятия (кнопка справа в TopAppBar) → `navController.navigate(Screen.CreateEvent.route)`

**С EventDetailScreen:**

- Возврат назад → `navController.popBackStack()`
- Изменение мероприятия (только для автора) → `navController.navigate(Screen.EditEvent.createRoute(eventId))`
- Список участников мероприятия → `navController.navigate(Screen.EventParticipants.createRoute(eventId))`
- Галерея фотографий мероприятия → `navController.navigate(Screen.EventGallery.createRoute(eventId))`
- Добавление комментария → `navController.navigate(Screen.AddEventComment.createRoute(eventId))`
- Поделиться мероприятием (кнопка справа в TopAppBar) → системное share intent

**С CreateEventScreen / EditEventScreen:**

- Возврат назад → `navController.popBackStack()`

---

### 3. Вкладка "Сообщения" (Messages)

**Экраны:**

- `MessagesRootScreen` - корневой экран вкладки
- `MessagesAuthPromptScreen` - экран с предложением авторизоваться (для неавторизованных пользователей)
- `DialogsListScreen` - экран списка диалогов (для авторизованных пользователей)
- `ChatScreen` - экран выбранного диалога
- `FriendsScreen` - экран списка друзей (для начала нового диалога)
- `UserSearchScreen` - экран поиска пользователей по нику (для начала нового диалога или с профиля)

**Навигация:**

**С MessagesRootScreen:**

- Если пользователь НЕ авторизован → показывать MessagesAuthPromptScreen
- Если пользователь авторизован → показывать DialogsListScreen

**С MessagesAuthPromptScreen (для неавторизованных):**

- Открыть модальное окно авторизации (LoginScreen) → показать модальное окно

**С DialogsListScreen (для авторизованных):**

- Открыть выбранный диалог → `navController.navigate(Screen.Chat.createRoute(dialogId))`
- Начать новый диалог (кнопка справа в TopAppBar):
  - Если есть друзья → `navController.navigate(Screen.Friends.route)`
  - Если нет друзей → `navController.navigate(Screen.UserSearch.route)`

**С ChatScreen:**

- Возврат назад → `navController.popBackStack()`
- Открыть профиль пользователя, с которым ведется диалог → `navController.navigate(Screen.Profile.createRoute(userId))`

**С FriendsScreen:**

- Возврат назад → `navController.popBackStack()`
- Открыть профиль друга → `navController.navigate(Screen.Profile.createRoute(userId))`

**С UserSearchScreen:**

- Возврат назад → `navController.popBackStack()`
- Открыть профиль найденного пользователя → `navController.navigate(Screen.Profile.createRoute(userId))`
- Начать диалог с пользователем → `navController.navigate(Screen.Chat.createRoute(dialogId))`

---

### 4. Вкладка "Профиль" (Profile)

**Экраны:**

- `ProfileRootScreen` - корневой экран вкладки
- `ProfileAuthPromptScreen` - экран с предложением авторизоваться (для неавторизованных пользователей)
- `ProfileScreen` - экран профиля (как для текущего пользователя, так и для других)
- `EditProfileScreen` - экран редактирования профиля (только для текущего пользователя)
- `FriendsScreen` - экран списка друзей (переиспользуется из сообщений)
- `UserParksScreen` - экран списка площадок, добавленных пользователем
- `UserTrainingParksScreen` - экран списка площадок, где тренируется пользователь
- `BlacklistScreen` - экран черного списка (только для текущего пользователя)
- `JournalsListScreen` - экран списка дневников пользователя
- `JournalDetailScreen` - детальный экран дневника
- `CreateJournalScreen` - экран создания дневника
- `EditJournalScreen` - экран редактирования дневника
- `ChangePasswordScreen` - экран изменения пароля
- `SelectCountryScreen` - экран выбора страны
- `SelectCityScreen` - экран выбора города

**Навигация:**

**С ProfileRootScreen:**

- Если пользователь НЕ авторизован → показывать ProfileAuthPromptScreen
- Если пользователь авторизован → показывать ProfileScreen текущего пользователя

**С ProfileAuthPromptScreen (для неавторизованных):**

- Открыть модальное окно авторизации (LoginScreen) → показать модальное окно

**С ProfileScreen:**

- Поиск пользователей по нику (кнопка справа сверху, модальное окно) → открыть UserSearchScreen как модальное окно
- Редактирование профиля (только для текущего пользователя) → `navController.navigate(Screen.EditProfile.route)`
- Список друзей (если есть друзья) → `navController.navigate(Screen.Friends.route)`
- Список добавленных площадок → `navController.navigate(Screen.UserParks.createRoute(userId))`
- Список площадок, где тренируется (если есть площадки) → `navController.navigate(Screen.UserTrainingParks.createRoute(userId))`
- Черный список (если есть, только для текущего пользователя) → `navController.navigate(Screen.Blacklist.route)`
- Список дневников → `navController.navigate(Screen.JournalsList.createRoute(userId))`

**С чужого профиля:**

- Отправить сообщение (модальное окно) → открыть UserSearchScreen как модальное окно для отправки сообщения

**С EditProfileScreen:**

- Возврат назад → `navController.popBackStack()`
- Изменение пароля → `navController.navigate(Screen.ChangePassword.route)`
- Выбор страны → `navController.navigate(Screen.SelectCountry.route)`
- Выбор города → `navController.navigate(Screen.SelectCity.route)`

**С FriendsScreen:**

- Открыть профиль друга → `navController.navigate(Screen.Profile.createRoute(userId))`
- Возврат назад → `navController.popBackStack()`

**С UserParksScreen / UserTrainingParksScreen:**

- Открыть детальный экран площадки → `navController.navigate(Screen.ParkDetail.createRoute(parkId))`
- Возврат назад → `navController.popBackStack()`

**С JournalsListScreen:**

- Создать дневник → `navController.navigate(Screen.CreateJournal.route)`
- Открыть детальный экран дневника → `navController.navigate(Screen.JournalDetail.createRoute(journalId))`
- Возврат назад → `navController.popBackStack()`

**С JournalDetailScreen:**

- Редактировать дневник → `navController.navigate(Screen.EditJournal.createRoute(journalId))`
- Добавить запись в дневник → `navController.navigate(Screen.AddJournalEntry.createRoute(journalId))`
- Настройки доступа/видимости/комментирования → открыть модальное окно
- Возврат назад → `navController.popBackStack()`

**С CreateJournalScreen / EditJournalScreen:**

- Возврат назад → `navController.popBackStack()`

---

### 5. Вкладка "Ещё" (More)

**Экраны:**

- `MoreScreen` - экран настроек (раздел "Ещё")
- `ThemeIconScreen` - экран темы и иконки приложения (референс: JetpackDays)

**Навигация:**

**С MoreScreen:**

- Тема и иконка приложения → `navController.navigate(Screen.ThemeIcon.route)`
- Магазин Workout (кнопка внизу) → открыть GitHub репозиторий приложения в браузере

**С ThemeIconScreen:**

- Возврат назад → `navController.popBackStack()`

---

### Экраны авторизации

**Экраны:**

- `LoginScreen` - экран входа (модальное окно)
- `RegisterScreen` - экран регистрации (модальное окно)

**Навигация:**

**С LoginScreen (модальное окно):**

- Кнопка "Войти" → выполнить авторизацию и закрыть модальное окно
- Кнопка "Восстановить пароль" → отправить запрос на сервер для сброса пароля по логину и показать alert с результатом
- Кнопка "Регистрация" (дополнительно к iOS версии) → открыть модальное окно RegisterScreen
- Закрыть модальное окно без авторизации → dismiss

**С RegisterScreen (модальное окно):**

- Кнопка "Зарегистрироваться" → выполнить регистрацию и закрыть модальное окно
- Возврат к LoginScreen → dismiss и открыть модальное окно LoginScreen

---

## План реализации

### Этап 1: Подготовка и анализ (1-2 часа)

**Задачи:**

1. **Создать список всех экранов с маршрутами**
   - На основе iOS-приложения (SwiftUI-WorkoutApp)
   - Учесть описанные выше навигационные пути
   - Определить маршруты с параметрами

2. **Определить маршруты с параметрами**
   - Детальные экраны с ID (ParkDetail, EventDetail, Profile, Chat и т.д.)
   - Экраны создания/редактирования (CreatePark, EditPark, CreateEvent, EditEvent и т.д.)
   - Экраны с несколькими параметрами (CreateEventForPark с parkId, date, time)

3. **Определить какие экраны реализуются сейчас (заглушки)**
   - ParksRootScreen, EventsRootScreen, MessagesRootScreen, ProfileRootScreen, MoreScreen
   - Определить минимальный набор маршрутов для первой реализации

**Результат:**

- Документ со списком всех экранов и маршрутами
- Понимание иерархии навигации
- Выделение минимального набора экранов для первой реализации

---

### Этап 2: Создание централизованного Screen класса (1-2 часа)

**Задачи:**

1. **Создать файл `ui/navigation/Screen.kt`**
   - Sealed class `Screen` с параметрами:
     - `route: String` - маршрут навигации
     - `icon: ImageVector?` - иконка для вкладок (только для Tab экранов)
     - `titleResId: Int?` - строковый ресурс заголовка

2. **Определить экраны вкладок (Tab экраны)**

   ```kotlin
   object Parks : Screen(
       route = "parks",
       icon = Icons.Filled.Park,
       titleResId = R.string.parks,
   )

   object Events : Screen(
       route = "events",
       icon = Icons.Filled.Event,
       titleResId = R.string.events,
   )

   object Messages : Screen(
       route = "messages",
       icon = Icons.Filled.Chat,
       titleResId = R.string.messages,
   )

   object Profile : Screen(
       route = "profile",
       titleResId = R.string.profile,
   )

   object More : Screen(
       route = "more",
       icon = Icons.Filled.MoreVert,
       titleResId = R.string.more,
   )
   ```

   - **Примечание:** Profile не имеет иконки, так как это детальный экран, а не вкладка

3. **Определить экраны раздела "Площадки"**

   ```kotlin
   object ParksMap : Screen(route = "parks_map")
   object ParksList : Screen(route = "parks_list")

   object ParkDetail : Screen(route = "park_detail/{parkId}") {
       fun createRoute(parkId: Long) = "park_detail/$parkId"
   }

   object CreatePark : Screen(route = "create_park")
   object EditPark : Screen(route = "edit_park/{parkId}") {
       fun createRoute(parkId: Long) = "edit_park/$parkId"
   }

   object ParkFilter : Screen(route = "park_filter")

   object CreateEventForPark : Screen(route = "create_event_for_park/{parkId}") {
       fun createRoute(parkId: Long, date: LocalDate, time: LocalTime) =
           "create_event_for_park/$parkId/${date.toString()}/${time.toString()}"
   }

   object ParkRoute : Screen(route = "park_route/{parkId}") {
       fun createRoute(parkId: Long) = "park_route/$parkId"
   }

   object AddParkComment : Screen(route = "add_park_comment/{parkId}") {
       fun createRoute(parkId: Long) = "add_park_comment/$parkId"
   }

   object ParkTrainees : Screen(route = "park_trainees/{parkId}") {
       fun createRoute(parkId: Long) = "park_trainees/$parkId"
   }

   object ParkGallery : Screen(route = "park_gallery/{parkId}") {
       fun createRoute(parkId: Long) = "park_gallery/$parkId"
   }
   ```

4. **Определить экраны раздела "Мероприятия"**

   ```kotlin
   object EventsUpcoming : Screen(route = "events_upcoming")
   object EventsPast : Screen(route = "events_past")

   object EventDetail : Screen(route = "event_detail/{eventId}") {
       fun createRoute(eventId: Long) = "event_detail/$eventId"
   }

   object CreateEvent : Screen(route = "create_event")
   object EditEvent : Screen(route = "edit_event/{eventId}") {
       fun createRoute(eventId: Long) = "edit_event/$eventId"
   }

   object EventParticipants : Screen(route = "event_participants/{eventId}") {
       fun createRoute(eventId: Long) = "event_participants/$eventId"
   }

   object EventGallery : Screen(route = "event_gallery/{eventId}") {
       fun createRoute(eventId: Long) = "event_gallery/$eventId"
   }

   object AddEventComment : Screen(route = "add_event_comment/{eventId}") {
       fun createRoute(eventId: Long) = "add_event_comment/$eventId"
   }
   ```

5. **Определить экраны раздела "Сообщения"**

   ```kotlin
   object MessagesRoot : Screen(route = "messages_root")
   object DialogsList : Screen(route = "dialogs_list")

   object Chat : Screen(route = "chat/{dialogId}") {
       fun createRoute(dialogId: Long) = "chat/$dialogId"
   }

   object Friends : Screen(route = "friends")
   object UserSearch : Screen(route = "user_search")
   ```

**Примечание:** MessagesRoot используется для показа либо MessagesAuthPromptScreen (неавторизованные), либо DialogsListScreen (авторизованные).

6. **Определить экраны раздела "Профиль"**

   ```kotlin
   object ProfileRoot : Screen(route = "profile_root")
   object Profile : Screen(route = "profile/{userId}") {
       fun createRoute(userId: Long) = "profile/$userId"
   }

   object EditProfile : Screen(route = "edit_profile")

   object UserParks : Screen(route = "user_parks/{userId}") {
       fun createRoute(userId: Long) = "user_parks/$userId"
   }

   object UserTrainingParks : Screen(route = "user_training_parks/{userId}") {
       fun createRoute(userId: Long) = "user_training_parks/$userId"
   }

   object Blacklist : Screen(route = "blacklist")

   object JournalsList : Screen(route = "journals_list/{userId}") {
       fun createRoute(userId: Long) = "journals_list/$userId"
   }

   object JournalDetail : Screen(route = "journal_detail/{journalId}") {
       fun createRoute(journalId: Long) = "journal_detail/$journalId"
   }

   object CreateJournal : Screen(route = "create_journal")
   object EditJournal : Screen(route = "edit_journal/{journalId}") {
       fun createRoute(journalId: Long) = "edit_journal/$journalId"
   }

   object AddJournalEntry : Screen(route = "add_journal_entry/{journalId}") {
       fun createRoute(journalId: Long) = "add_journal_entry/$journalId"
   }

   object ChangePassword : Screen(route = "change_password")
   object SelectCountry : Screen(route = "select_country")
   object SelectCity : Screen(route = "select_city")
   ```

**Примечание:** ProfileRoot используется для показа либо ProfileAuthPromptScreen (неавторизованные), либо ProfileScreen текущего пользователя (авторизованные).

7. **Определить экраны раздела "Настройки"**

   ```kotlin
   object ThemeIcon : Screen(route = "theme_icon")
   ```

8. **Определить экраны авторизации и модальные окна**

   ```kotlin
   object Login : Screen(route = "login")
   object Register : Screen(route = "register")
   ```

**Примечание:** LoginScreen и RegisterScreen открываются как модальные окна с MessagesAuthPromptScreen и ProfileAuthPromptScreen.

**Результат:**

- Централизованный класс `Screen` с определением всех маршрутов
- Методы `createRoute()` для навигации с параметрами
- Готовый фундамент для легкого добавления новых экранов

---

### Этап 3: Создание RootScreenViewModel и состояния (30 минут)

**Задачи:**

1. **Создать файл `ui/state/RootScreenState.kt`**

   ```kotlin
   data class RootScreenState(
       val currentTab: Screen = Screen.Parks,
       val tabs: List<Screen> = listOf(
           Screen.Parks,
           Screen.Events,
           Screen.Messages,
           Screen.Profile,
           Screen.More,
       ),
   )
   ```

2. **Создать файл `ui/viewmodel/RootScreenViewModel.kt`**

   ```kotlin
   class RootScreenViewModel : ViewModel() {
       private val _currentTab = mutableStateOf<Screen>(Screen.Parks)
       val currentTab: State<Screen> = _currentTab

       val tabs = listOf(
           Screen.Parks,
           Screen.Events,
           Screen.Messages,
           Screen.Profile,
           Screen.More,
       )

       fun switchTab(tab: Screen) {
           if (tabs.contains(tab)) {
               _currentTab.value = tab
           }
       }

       fun isParksTabSelected(): Boolean = _currentTab.value == Screen.Parks
       fun isEventsTabSelected(): Boolean = _currentTab.value == Screen.Events
       fun isMessagesTabSelected(): Boolean = _currentTab.value == Screen.Messages
       fun isProfileTabSelected(): Boolean = _currentTab.value == Screen.Profile
       fun isMoreTabSelected(): Boolean = _currentTab.value == Screen.More
   }
   ```

**Результат:**

- ViewModel для управления состоянием вкладок
- Состояние UI для RootScreen

---

### Этап 4: Рефакторинг RootScreen (1-2 часа)

**Задачи:**

1. **Создать файл `ui/screens/RootScreenComponents.kt`**
   - Модульная функция `navigationBarContent()` для BottomNavigationBar
   - Модульные функции для назначения экранов (в будущем)
   - Модульная функция `navHostContent()` для основного NavHost
   - Модульная функция `updateTabBasedOnRoute()` для синхронизации вкладок

2. **Обновить файл `ui/screens/RootScreen.kt`**
   - Использовать новый RootScreenViewModel
   - Использовать Screen класс вместо TabBarItem
   - Управлять видимостью BottomNavigationBar на основе текущего маршрута
   - Интегрировать Firebase Analytics (если применимо)

3. **Создать минимальный набор назначений для текущих экранов-заглушек**
   - `parksRootDestination` для ParksRootScreen
   - `eventsRootDestination` для EventsRootScreen
   - `messagesRootDestination` для MessagesRootScreen (с проверкой авторизации)
   - `profileRootDestination` для ProfileRootScreen (с проверкой авторизации)
   - `moreDestination` для MoreScreen

**Важно:** Для MessagesRootScreen и ProfileRootScreen нужно добавить логику проверки авторизации:

- Если пользователь не авторизован → показывать AuthPromptScreen
- Если пользователь авторизован → показывать полноценный контент (DialogsList для сообщений, Profile для профиля)

**Пример кода для `navigationBarContent()`:**

```kotlin
@Composable
internal fun navigationBarContent(
    items: List<Screen>,
    viewModel: RootScreenViewModel,
    navController: NavHostController,
) {
    val currentTab by viewModel.currentTab

    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = screen.icon!!,
                        contentDescription = stringResource(id = screen.titleResId!!),
                    )
                },
                label = {
                    Text(text = stringResource(id = screen.titleResId!!))
                },
                selected = currentTab == screen,
                onClick = {
                    viewModel.switchTab(screen)
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}
```

**Пример кода для назначения экрана с проверкой авторизации:**

```kotlin
private fun androidx.navigation.NavGraphBuilder.messagesRootDestination(
    navController: NavHostController,
    viewModel: RootScreenViewModel,
) {
    composable(Screen.MessagesRoot.route) {
        val context = LocalContext.current
        // Firebase Analytics logging (если применимо)

        // Проверка авторизации
        val isLoggedIn = false // TODO: Получить из UserPreferencesRepository

        if (!isLoggedIn) {
            MessagesAuthPromptScreen(
                onLoginClick = {
                    // TODO: Открыть модальное окно LoginScreen
                },
            )
        } else {
            DialogsListScreen(
                onDialogClick = { dialogId ->
                    navController.navigate(Screen.Chat.createRoute(dialogId))
                },
                onCreateDialogClick = {
                    // TODO: Определить наличие друзей и перейти к Friends или UserSearch
                },
            )
        }
    }
}
```

**Примечание:** Для `profileRootDestination` используется аналогичный подход с проверкой авторизации, где для неавторизованных пользователей показывается `ProfileAuthPromptScreen`, а для авторизованных - `ProfileScreen` с передачей `currentUserId`.

**Результат:**

- Единый NavHost в RootScreen
- Централизованная навигация через Screen класс
- Управление состоянием вкладок через RootScreenViewModel
- Минимальный набор назначений для текущих экранов-заглушек
- Логика проверки авторизации для MessagesRootScreen и ProfileRootScreen

---

### Этап 5: Рефакторинг ParksNavHost (1 час)

**Задачи:**

1. **Обновить ParksNavHost.kt**
   - Переместить назначения в RootScreenComponents.kt
   - Использовать Screen класс для всех маршрутов
   - Обновить навигацию на ParkDetailScreen (реализовать базовый вариант)
   - Обновить навигацию на CreateParkScreen (реализовать базовый вариант)
   - Реализовать логирование нажатия на кнопку фильтра в консоль

**Результат:**

- ParksNavHost рефакторен для использования единой навигации
- Все маршруты используют Screen.createRoute()

---

### Этап 6: Рефакторинг остальных NavHost (1-2 часа)

**Задачи:**

1. **Обновить EventsNavHost.kt**
   - Переместить назначения в RootScreenComponents.kt
   - Использовать Screen класс для всех маршрутов

2. **Обновить MessagesNavHost.kt**
   - Переместить назначения в RootScreenComponents.kt
   - Использовать Screen класс для всех маршрутов

3. **Обновить ProfileNavHost.kt**
   - Переместить назначения в RootScreenComponents.kt
   - Использовать Screen класс для всех маршрутов

4. **Удалить старые NavHost файлы**
   - ParksNavHost.kt, EventsNavHost.kt, MessagesNavHost.kt, ProfileNavHost.kt

**Результат:**

- Все NavHost обновлены для использования единой навигации
- Все назначения находятся в RootScreenComponents.kt
- Удалены старые файлы NavHost

---

### Этап 7: Удаление TabBarItem (30 минут)

**Задачи:**

1. **Удалить файл `model/TabBarItem.kt`**
   - Все вкладки теперь используют Screen класс

2. **Обновить использование TabBarItem в проекте**
   - Заменить все вхождения TabBarItem на Screen
   - Обновить импорты

**Результат:**

- Удален устаревший TabBarItem класс
- Единый подход к навигации через Screen класс

---

### Этап 8: Интеграция с Firebase Analytics (опционально, 1 час)

**Задачи:**

1. **Создать класс FirebaseAnalyticsHelper** (если еще не создан)
   - Метод `logScreenView()` для логирования экранов
   - Константы для имен экранов (ScreenNames)

2. **Добавить логирование в назначения экранов**
   - Добавить логирование для каждого назначения экрана

**Результат:**

- Трекинг всех экранов в Firebase Analytics
- Breadcrumb logs для отладки навигации

---

### Этап 9: Тестирование (1-2 часа)

**Задачи:**

1. **Тестирование навигации между вкладками**
   - Переключение между Parks, Events, Messages, Profile, More
   - Сохранение состояния вкладок при переключении

2. **Тестирование навигации на ParksRootScreen**
   - Переключение между режимами карта/список (состояние UI)
   - Логирование нажатия на кнопку фильтра

3. **Тестирование сценариев авторизации**
   - Открытие модального окна авторизации с MessagesAuthPromptScreen
   - Открытие модального окна авторизации с ProfileAuthPromptScreen
   - Закрытие модальных окон без авторизации
   - (В будущем) Проверка переключения контента Messages и Profile после авторизации

4. **Тестирование основных сценариев**
   - Навигация между вкладками
   - Возврат к предыдущему экрану через back button
   - Выделение текущей вкладки в BottomNavigationBar

5. **Тестирование Firebase Analytics** (если реализовано)
   - Проверка логирования screen_view событий

**Результат:**

- Навигация работает корректно
- Все тесты пройдены

---

### Этап 10: Форматирование и проверка кода (30 минут)

**Задачи:**

1. **Выполнить форматирование кода**
   - `./gradlew ktlintFormat`
   - `make format` (если используется)

2. **Запустить lint проверку**
   - `./gradlew ktlintCheck`
   - `./gradlew detekt`

3. **Исправить все найденные проблемы**

**Результат:**

- Код соответствует стандартам проекта
- Все lint проверки пройдены

---

## Преимущества нового подхода

1. **Масштабируемость**
   - Единый NavHost легко масштабируется при добавлении новых экранов
   - Модульные функции для каждого назначения упрощают поддержку
   - Добавление нового экрана = добавление новой функции назначения в RootScreenComponents.kt

2. **Типобезопасность**
   - Использование `Screen.createRoute(id)` вместо ручного формирования строк
   - Минимизация ошибок при навигации с параметрами

3. **Централизованное управление**
   - Все маршруты определены в одном месте (Screen класс)
   - RootScreenViewModel управляет состоянием вкладок

4. **Поддерживаемость**
   - Единая архитектура навигации по всему приложению
   - Соответствие референсному проекту (JetpackDays)

5. **Фундамент для развития**
   - Создан базовый подход, который позволяет легко добавлять новые экраны
   - Не нужно сразу реализовывать все экраны, можно добавлять их постепенно

6. **Интеграция с аналитикой**
   - Firebase Analytics для трекинга экранов
   - Breadcrumb logs для отладки навигации

7. **Поддержка сценариев авторизации**
   - Отдельные экраны-заглушки для неавторизованных пользователей
   - Модальные окна для LoginScreen, RegisterScreen
   - Легкое переключение между контентом для авторизованных и неавторизованных пользователей
   - Восстановление пароля реализовано как кнопки в LoginScreen и RegisterScreen, которые отправляют запрос на сервер для сброса пароля по логину

---

## Оценка времени

| Этап | Время |
|------|-------|
| Подготовка и анализ | 1-2 часа |
| Создание Screen класса | 1-2 часа |
| RootScreenViewModel и состояние | 30 минут |
| Рефакторинг RootScreen | 1-2 часа |
| Рефакторинг ParksNavHost | 1 час |
| Рефакторинг остальных NavHost | 1-2 часа |
| Удаление TabBarItem | 30 минут |
| Интеграция с Firebase Analytics | 1 час (опционально) |
| Тестирование | 1-2 часа |
| Форматирование и проверка | 30 минут |
| **Итого** | **9.5-13.5 часов** |

---

## Примечания

- **Фундаментальный подход** - создается база для легкого добавления новых экранов, а не полная реализация всех экранов сразу
- **TDD подход не требуется** для этого рефакторинга - это чисто архитектурные изменения без бизнес-логики
- **Интеграция с Firebase Analytics** может быть реализована позже при необходимости
- **Сохранение функционала** - необходимо убедиться, что все существующие функции работают после рефакторинга
- **Минимальная реализация** - сначала создается фундамент, потом экраны реализуются согласно плану развития
- **Сценарии авторизации** - для вкладок "Сообщения" и "Профиль" реализованы экраны-заглушки с предложением авторизоваться, которые открывают модальные окна (LoginScreen, RegisterScreen). При авторизации контент переключается на полноценный функционал. Модальные окна авторизации не требуют изменения маршрута навигации, а открываются и закрываются как диалоговые окна поверх текущего контента.

---

## Следующие шаги после рефакторинга

После завершения рефакторинга навигации, проект готов к реализации функционала экранов согласно плану разработки (`docs/development-plan.md`):

1. Реализация Use Cases для бизнес-логики
2. Реализация ViewModels для каждого экрана
3. Реализация UI экранов с использованием компонентов дизайн-системы
4. Написание тестов (unit, integration, UI)

Навигация будет готова к масштабированию - добавление новых экранов потребует только:

1. Добавления нового объекта Screen в Screen.kt
2. Создания новой модульной функции назначения в RootScreenComponents.kt
3. Добавления вызова этой функции в navHostContent()
