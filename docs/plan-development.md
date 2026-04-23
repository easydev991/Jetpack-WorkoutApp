# План разработки Android-приложения SW Parks

## Текущее состояние проекта

### iOS-приложение (SwiftUI-WorkoutApp)

- ✅ **Полностью реализовано** и работает исправно
- ✅ Все экраны и функции работают
- ✅ Полная документация функционала доступна в `SwiftUI-WorkoutApp/docs/feature-map.md`

### Android-приложение (Jetpack-WorkoutApp)

- 🚧 **В активной разработке**
- ✅ Дизайн-система: 42 компонента
- ✅ Модели данных: 19 API + 9 доменных + 22 UI модели
- ✅ API клиент SWApi: 62 endpoint-аннотации в `SWApi`
- ✅ Безопасность токена: шифрование, интерцепторы
- ✅ Use Cases: 32 реализации + 24 интерфейса (`56` файлов)
- ✅ ViewModels: 30 реализаций + 25 интерфейсов (`55` файлов)
- ✅ Namespace: `com.swparks`

#### Реализованные экраны

**Полностью реализованы (Domain, Data, UI, тесты):**
- RootScreen, MoreScreen, ThemeIconScreen, LoginScreen
- EditProfileScreen (включает SelectCountryScreen/SelectCityScreen), ChangePasswordScreen, OtherUserProfileScreen, SearchUserScreen
- RegisterUserScreen (включает RegisterSelectCountryScreen/RegisterSelectCityScreen)
- MyFriendsScreen (включает запросы в друзья), MyBlacklistScreen, UserFriendsScreen, ProfileRootScreen, UserTrainingParksScreen
- JournalsListScreen (100% - 4 итерации, включает создание/удаление, унифицированные уведомления), JournalEntriesScreen (6 итераций, включает создание/редактирование/удаление записей), JournalSettingsDialog
- TextEntryScreen, MessagesRootScreen, ChatScreen
- EventsScreen, EventFormScreen, ParticipantsScreen

**Частично реализованы (базовый функционал):** ParksRootScreen, PhotoDetailScreen

**Реализованы как вспомогательные production-экраны/сценарии:** EventDetailScreen, ParksAddedByUserScreen (включая retry/loading-контракт и тесты), FeedbackSender, ItemListScreen

**Не реализованы:** часть экранов вкладки "Площадки" (ParksMap/ParksList расширенные режимы) и отдельные специализированные сценарии

#### Реализованные модели данных

**API модели (19):** User, Park, Event, Comment, Photo, JournalResponse, JournalEntryResponse, DialogResponse, MessageResponse, City, Country, ParkType, ParkSize, LoginSuccess, SocialUpdates, ApiFriendAction, ApiBlacklistOption и связанные сетевые DTO

**Доменные модели (9):** AppIcon, AppTheme, Journal, JournalEntry, FriendAction, EditProfileLocations, RegistrationParams и дополнительные доменные сущности

**UI модели (22):** LoginCredentials, ParkForm, EventForm, MainUserForm, RegisterForm, EditJournalSettingsRequest, FriendAction, BlacklistAction, Gender, JournalAccess, TextEntryOption, TextEntryMode, EditInfo, EventKind, EventType, EventFormMode, ParticipantsMode, PickedImageItem, PickedImagesState, MapUriSet и связанные UI state/request модели

#### Реализованная архитектура

**Data Layer:** AppContainer (DI), SWRepository (покрывает весь текущий контракт `SWApi`), UserPreferencesRepository (DataStore), SecureTokenRepository (Tink шифрование)

**Network Layer:** SWApi, RetryInterceptor, AuthInterceptor (401), TokenInterceptor

**Security:** CryptoManager (AES-128-GCM-HKDF), EncryptedStringSerializer

**Domain Layer:** 32 реализации use case + 24 интерфейса, IconManager, исключения (ServerException, NetworkException)

**UI layer:** 30 ViewModel + 25 интерфейсов/контрактов

---

## Общий план реализации по вкладкам (TDD)

### Принципы разработки

**TDD (Test-Driven Development):**

1. **Red**: Написать тесты ДО реализации кода
2. **Green**: Реализовать минимальный код для прохождения тестов
3. **Refactor**: Улучшить код после успешных тестов

**Пирамида тестирования:**

- Unit-тесты (70%) - бизнес-логика изолированно, MockK
- Integration (20%) - взаимодействие слоев (DAO, Repository)
- UI (10%) - критические сценарии, Compose Testing

---

## Вкладки навигации

### 1. Вкладка "Ещё" (More) - ✅ ЗАВЕРШЕНО

**Экраны:** MoreScreen (настройки), ThemeIconScreen (тема и иконки)

**Статус:** Полный стек (Domain, Data, UI), unit-тесты, UI-тесты (11 тестов)

**Детальный план:** `docs/screens/doc-more-screen.md`

---

### 2. Авторизация (Auth) - ✅ ЗАВЕРШЕНО

**Экраны:**
- ✅ LoginScreen (Domain, Data, UI, тесты)
- ✅ RegisterUserScreen (Domain, Data, UI, тесты, включает выбор страны/города)

**Статус:** Полный стек (Domain, Data, UI, тесты)

**Детальные планы:**
- `docs/screens/doc-login-screen.md`
- `docs/screens/doc-register-screen.md`

---

### 3. Вкладка "Профиль" (Profile) - ✅ ЗАВЕРШЕНО

**Экраны:**
- ✅ ProfileRootScreen - просмотр профиля (Domain, Data, UI, тесты)
- ✅ EditProfileScreen - редактирование профиля (Domain, Data, UI, тесты, включает выбор страны/города)
- ✅ ChangePasswordScreen - смена пароля (Domain, Data, UI, тесты)
- ✅ MyFriendsScreen - список друзей (Domain, Data, UI, тесты, включает запросы в друзья)
- ✅ UserFriendsScreen - друзья пользователя (Domain, Data, UI, тесты)
- ✅ UserTrainingParksScreen - площадки пользователя (Domain, Data, UI, тесты)
- ✅ MyBlacklistScreen - черный список (Domain, Data, UI, тесты)
- ✅ OtherUserProfileScreen - профиль другого пользователя (Domain, Data, UI, тесты)
- ✅ SearchUserScreen - поиск пользователей (Domain, Data, UI, тесты)

**Основные функции:**
- Отображение профиля, друзей, площадок
- Управление друзьями и черным списком (включая запросы в друзья)
- Редактирование профиля (включая выбор страны/города)
- Поиск пользователей
- Просмотр профилей других пользователей
- Смена пароля

**Детальные планы:**
- `docs/screens/doc-current-user-profile-screen.md`
- `docs/screens/doc-edit-profile-screen.md`
- `docs/screens/doc-change-password-screen.md`
- `docs/screens/doc-myfriends-screen.md`
- `docs/screens/doc-other-user-profile-screen.md`
- `docs/screens/doc-search-user-screen.md`
- `docs/screens/doc-user-training-parks-room-cache-tdd.md`

---

### 4. Вкладка "Площадки" (Parks)

**Экраны:**
- ParksListScreen - список площадок (с фильтрами)
- ParksMapScreen - карта площадок
- ParkDetailScreen - детальная информация
- EditParkScreen - создание/редактирование площадки

**Основные функции:**
- Просмотр площадок (карта/список, оффлайн)
- Фильтрация (размер, тип, город)
- Создание/редактирование площадки (до 15 фото)
- Комментарии, "тренируюсь/не тренируюсь"
- Шаринг и маршрут через Google Maps

**Технические задачи:**
- Google Maps SDK
- Фильтрация площадок
- Загрузка фото (до 15 шт.)
- Комментарии и функции социальной активности

**Детальные планы:**
- `docs/screens/doc-park-filter-screen.md`
- `docs/screens/doc-park-detail-screen.md`
- `docs/screens/doc-park-detail-offline-tdd.md` — **TDD план** Offline-поддержка ParkDetailScreen
- `docs/screens/doc-park-form-screen.md`
- `docs/screens/doc-photo-detail-screen.md`

---

### 5. Вкладка "Мероприятия" (Events) - ✅ КЛЮЧЕВОЙ ФУНКЦИОНАЛ ЗАВЕРШЕН

**Экраны:**
- ✅ EventsScreen - список мероприятий
- ⚠️ EventDetailScreen - детальная информация реализована частично
- ✅ EventFormScreen - создание/редактирование мероприятия
- ✅ EventParticipantsScreen - список участников мероприятия

**Основные функции:**
- Просмотр мероприятий (планируемые/прошедшие)
- Создание/редактирование мероприятия (до 15 фото)
- Просмотр участников мероприятия
- Комментарии, "иду/не иду"
- Шаринг

**Технические задачи:**
- ✅ Переключение между типами мероприятий
- ✅ Загрузка фото (до 15 шт.) в `EventFormScreen`
- ✅ Комментарии и функции социальной активности реализованы в `EventDetailScreen`
- ⚠️ Остаются некритичные улучшения `EventDetailScreen`: пагинация комментариев и отдельная оптимизация загрузки больших фото

**Детальные планы:**
- `docs/screens/doc-events-screen.md`
- `docs/screens/doc-event-detail-screen.md`
- `docs/screens/doc-event-form-screen.md`

---

### 6. Вкладка "Сообщения" (Messages) - ✅ ЗАВЕРШЕНО

**Экраны:**
- ✅ MessagesRootScreen - список диалогов (Domain, Data, UI, тесты)
- ✅ ChatScreen - чат с пользователем (Domain, Data, UI, тесты)

**Основные функции:**
- Список диалогов с непрочитанными сообщениями
- Удаление диалогов (long press → dropdown menu)
- Mark as read (long press → dropdown menu)
- Чат с отправкой/получением сообщений
- Автоматическая прокрутка к последнему сообщению
- MarkAsRead при просмотре последнего сообщения
- Обновление списка диалогов после отправки сообщения
- Блокировка ввода во время отправки

**Технические задачи:**
- ✅ API endpoints для сообщений (реализованы в SWApi)
- ✅ Список диалогов с непрочитанными
- ✅ Чат с отправкой/получением
- ✅ Переход из профиля пользователя

**Детальная документация:**
- `docs/screens/doc-dialogs-list-screen.md`
- `docs/screens/doc-chat-screen.md`

---

### 7. Дневники (Journals) - ✅ ЗАВЕРШЕНО

**Экраны:**
- ✅ JournalsListScreen - список дневников (Domain, Data, UI, тесты) - 100% реализован (3 итерации: базовый экран, удаление, создание, унифицированные уведомления)
- ✅ JournalEntriesScreen - дневник с записями (Domain, Data, UI, тесты) - включает детальный просмотр, создание, редактирование и удаление записей (6 итераций)
- ✅ JournalSettingsDialog - настройки дневника (UI, тесты) - редактирование названия и уровней доступа

**Основные функции:**
- Просмотр списка дневников с Pull-to-Refresh
- Создание дневников через FAB и TextEntrySheet
- Удаление дневников с диалогом подтверждения
- Просмотр дневника с записями (детальная информация)
- Создание записей через FAB и TextEntrySheet
- Редактирование записей через TextEntrySheet (режим EditJournalEntry)
- Удаление записей с диалогом подтверждения (кроме первой записи)
- Редактирование названия и настроек доступа дневника через JournalSettingsDialog
- Унифицированные уведомления через UserNotifier

**Технические задачи:**
- API endpoints для дневников (реализованы в SWApi)
- Список дневников с кешированием
- Просмотр дневника с записями (offline-first)
- Создание/удаление дневников
- Создание/удаление/редактирование записей
- Настройки доступа (название, просмотр, комментарии)
- Retry/loading-контракт приведен к общему паттерну и задокументирован в `docs/doc-retry-loading-state.md`

**Детальные планы:**
- `docs/screens/doc-journals-list-screen.md` - 100% завершен (4 итерации: базовый экран, удаление, создание, унификация уведомлений)
- `docs/screens/doc-journal-entries-screen.md` - реализован (6 итераций с багфиксами)
- `docs/screens/doc-journal-settings-dialog.md` - реализован

---

## Технологический стек

**Основные:** Kotlin 2.3.20, Jetpack Compose (BOM 2026.03.01), Material 3, Android Gradle Plugin 9.1.1, Java 21

**Требования:** minSdk 26, targetSdk 36, compileSdk 36

**Архитектура:** ViewModel 2.10.0, Navigation 2.9.7, Lifecycle 2.10.0, Room 2.8.4

**Сеть:** Retrofit 3.0.0, Kotlinx Serialization 1.11.0, Coroutines 1.10.2

**Изображения:** Coil Compose 2.7.0

**Хранение:** DataStore 1.2.1, Room 2.8.4

**Качество:** KtLint 14.2.0, Detekt 1.23.8, KSP 2.3.6

**Тестирование:** JUnit 4.13.2, MockK 1.14.9, Turbine 1.2.1, Compose UI Test, Robolectric 4.16.1

---

## Архитектура проекта

### Текущая структура

```
app/src/main/java/com/swparks/
├── data/              # Data layer (repository, crypto, datetime, interceptor, serialization, database, model)
│   └── model/         # API модели и DTOs (19 файлов)
├── domain/            # Domain layer (exception, usecase, model)
│   └── model/         # Доменные модели (9 файлов)
├── network/           # API клиент (`SWApi`)
├── ui/
│   ├── ds/            # Компоненты дизайн-системы (42 файла)
│   ├── model/         # UI формы и запросы (22 файла)
│   ├── screens/       # Экраны приложения (57 файлов)
│   ├── viewmodel/     # ViewModels (30 реализаций + 25 интерфейсов)
│   └── theme/         # Тема
├── navigation/        # Навигация
└── util/              # Утилиты
```

### Структура тестов

```
app/src/test/java/com/swparks/          # Unit-тесты (1833 @Test)
app/src/androidTest/java/com/swparks/   # Интеграционные и UI тесты (452 @Test)
```

---

## Референсы для разработки

### Архитектура

- **nowinandroid** - референсный проект для архитектуры, навигации, DI
- `.cursor/rules/architecture.mdc` - MVVM, Clean Architecture, ручной DI

### Функционал и экраны

- **SwiftUI-WorkoutApp** - iOS-приложение (эталон функционала и дизайна)
- `docs/feature-map.md` - карта экранов и функционала Android-приложения

### Компоненты дизайн-системы

- ✅ 42 компонента готовы к использованию

---

## Makefile команды

**Основные:** `make help`, `make build`, `make clean`, `make install`, `make all`

**Тестирование:** `make test`, `make android-test`, `make test-all`, `make android-test-report`

**Качество:** `make lint`, `make format`, `make check`

**Публикация:** `make apk`, `make release`

---

## Примечания

- Использовать безопасное разворачивание опционалов (избегать `!!`)
- После изменений кода выполнять `make format`
- iOS-приложение - эталон функционала и дизайна

### Реализованный API

✅ **62 endpoint-аннотации в `SWApi`**

- Авторизация/профиль: 7 endpoints
- Друзья/черный список: 10 endpoints
- Страны/города: 1 endpoint
- Площадки: 15 endpoints
- Мероприятия: 12 endpoints
- Сообщения: 5 endpoints
- Дневники: 7 endpoints

### Безопасность авторизации

✅ **Полностью реализована:** CryptoManager (AES-128-GCM-HKDF), SecureTokenRepository (DataStore), TokenInterceptor, AuthInterceptor (401), Login/Logout use cases, AuthViewModel

### Тестирование

**1833 unit-тестов:** Data, Domain, Model, ViewModel, UI state, Utils, Network

**452 интеграционных и UI тестов:**
- Интеграционные: CryptoManagerIntegrationTest, JournalEntryDaoTest
- UI тесты: LoginScreen, MoreScreen, ThemeIconScreen, MyFriendsScreen, MyBlacklistScreen, ProfileRootScreen, UserFriendsScreen, OtherUserProfileScreen, SearchUserScreen, RootScreen, JournalsListScreen, JournalEntriesScreen, JournalSettingsDialog, TextEntryScreen, MessagesRootScreen, ChatScreen, EventsScreen, EventFormScreen, ParticipantsScreen, ParkFormScreen, ParkDetailScreen, ParksRootScreen, ParksFilterDialog, ParksAddedByUserScreen, PickedImagesGrid и связанные интеграционные сценарии

---

## История изменений

- **2025-01:** Первоначальный план разработки
- **2026-01-18:** Актуализация - организация по вкладкам, TDD подход
- **2026-01-22:** Обновление на основе фактического состояния (API, архитектура, модели)
- **2026-01-25:** Вкладка "Ещё" (More) завершена - MoreScreen и ThemeIconScreen
- **2026-01-31:** Авторизация (Auth) - LoginScreen завершен
- **2026-02-07:** Дизайн-система (28 компонентов), новые экраны (MyFriendsScreen, ParksAddedByUserScreen), database слой
- **2026-02-11:** Дневники (Journals) - 13 ViewModels, 10 экранов (с тестами), 17 use cases, 766+ unit-тестов, 2 интеграционных теста
- **2026-02-23:** Обновление документации - 31 компонент дизайн-системы, 32 use cases, 36 ViewModels, 85+ unit-тестов, 17 интеграционных и UI тестов, новые экраны (EditProfileScreen, RegisterUserScreen, OtherUserProfileScreen, SearchUserScreen, ChangePasswordScreen, MessagesRootScreen), UI модели (RegisterForm), новые доменные модели (FriendAction, EditProfileLocations)
- **2026-02-23:** Актуализация статусов - вкладка "Профиль" ✅ ЗАВЕРШЕНО, JournalsListScreen 100% реализован (4 итерации: базовый экран, удаление, создание, унифицированные уведомления), JournalEntriesScreen реализован (6 итераций с багфиксами, включает создание/редактирование/удаление записей), JournalSettingsDialog реализован, уточнение функционала (запросы в друзья в MyFriendsScreen, выбор страны/города встроен в EditProfileScreen и RegisterUserScreen)
- **2026-03-02:** Вкладка "Сообщения" (Messages) ✅ ЗАВЕРШЕНО - ChatScreen реализован (ChatViewModel, ChatScreen.kt, 15 unit-тестов), включает: отправка сообщений, markAsRead через repository, обновление списка диалогов через SharedFlow, автопрокрутка, блокировка ввода при отправке
- **2026-03-17:** Актуализация навигации и документации - в `RootScreen` реализованы и подключены `EventDetail/EditEvent/EventParticipants`, `ParkDetail/CreatePark/EditPark`, `Chat`, а также typed args parsers/coordinators для key navigation flows.
- **2026-03-21:** Актуализация метрик проекта - 41 компонент дизайн-системы, 24 use case, 25 ViewModel, 113 unit-тестов, 23 интеграционных/UI тестов, 32 экрана (18 с UI тестами), новые модели (RegistrationParams, EventFormMode, ParticipantsMode, PickedImageItem, PickedImagesState, MapUriSet)
- **2026-03-31:** TDD план для Offline-поддержки ParkDetailScreen — `docs/screens/doc-park-detail-offline-tdd.md`
- **2026-03-31:** Актуализация плана по фактическому состоянию кода: обновлены метрики проекта, статус вкладки Events и ссылка на `docs/screens/doc-event-form-screen.md`
- **2026-04-03:** Актуализация по фактическому состоянию репозитория: Journals переведены в статус ✅ завершено, `ParksAddedByUserScreen` вынесен из частично реализованных сценариев в реализованные вспомогательные экраны, обновлены тестовые метрики (`1833` unit / `452` androidTest), добавлена ссылка на итоговую документацию `docs/doc-retry-loading-state.md`
- **2026-04-03:** Уточнение статусов по фактическому состоянию экранов: `EventDetailScreen`, `FeedbackSender` и `ItemListScreen` переведены из исторического статуса "частично" в реализованные сценарии; `ParksRootScreen` оставлен в частично завершённых из-за отдельного хвоста по persistent tabs / keep-alive карты между bottom-nav вкладками
