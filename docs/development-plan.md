# План разработки Android-приложения SW Parks

## Текущее состояние проекта

### iOS-приложение (SwiftUI-WorkoutApp)

- ✅ **Полностью реализовано** и работает исправно
- ✅ Все экраны и функции работают
- ✅ Полная документация функционала доступна в `SwiftUI-WorkoutApp/docs/feature-map.md`

### Android-приложение (Jetpack-WorkoutApp)

- 🚧 **В активной разработке**
- ✅ Дизайн-система: 28 компонентов готовых к использованию
- ✅ Модели данных: 17 API моделей + 4 доменные модели + 12 UI моделей
- ✅ API клиент SWApi: 57 endpoints
- ✅ Архитектура безопасности токена: шифрование, интерцепторы
- ✅ Use Cases: авторизация, справочники стран/городов
- ✅ ViewModels: AuthViewModel, LoginViewModel, ThemeIconViewModel, EventsViewModel, ProfileViewModel, MainActivityViewModel, FriendsListViewModel
- ✅ Namespace: `com.swparks`

#### Реализованные экраны

**Полностью реализованы:**
- ✅ RootScreen - навигация по вкладкам
- ✅ MoreScreen - настройки (4 секции, все функции)
- ✅ ThemeIconScreen - тема и иконки (11 иконок, activity-aliases)
- ✅ LoginScreen - авторизация (Domain, Data, UI слои, 68 unit-тестов, 15 UI-тестов)

**Частично реализованы:**
- ⚠️ EventsScreen - базовый функционал (загрузка прошедших мероприятий)
- ⚠️ ParksRootScreen - отображение данных из assets (без бизнес-логики)
- ⚠️ ProfileRootScreen - базовый функционал (отображение профиля, кнопка выхода)
- ⚠️ MyFriendsScreen - без полной бизнес-логики
- ⚠️ ParksAddedByUserScreen - без полной бизнес-логики

**Не реализованы:**
- ❌ MessagesRootScreen - только заглушка
- ❌ Большинство детальных экранов

#### Реализованные модели данных

**API модели (data/model/) - 17 файлов:** User, Park, Event, Comment, Photo, JournalResponse, JournalEntryResponse, DialogResponse, MessageResponse, City, Country, Gender, ParkType, ParkSize, EventKind, EventType, SocialUpdates, ApiFriendAction, ApiBlacklistOption

**Доменные модели (domain/model/) - 4 файла:** AppIcon, AppTheme, Journal, JournalEntry

**UI модели (ui/model/) - 12 файлов:** LoginCredentials, ParkForm, EventForm, MainUserForm, RegistrationRequest, ResetPasswordRequest, EditJournalSettingsRequest, FriendAction, BlacklistAction, Gender, JournalAccess, TextEntryOption

#### Реализованная архитектура

**Data Layer:** AppContainer (DI), SWRepository (57 endpoints), UserPreferencesRepository (DataStore), SecureTokenRepository (Tink шифрование)

**Network Layer:** SWApi, RetryInterceptor, AuthInterceptor (401), TokenInterceptor

**Security:** CryptoManager (AES-128-GCM-HKDF), EncryptedStringSerializer

**Domain Layer:** LoginUseCase, LogoutUseCase, ResetPasswordUseCase, GetCountriesUseCase, GetCitiesByCountryUseCase, GetCountryByIdUseCase, GetCityByIdUseCase, IconManager, ServerException, NetworkException

**UI layer:** ViewModels (Auth, Login, ThemeIcon, Events, Profile, MainActivity, FriendsList)

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

**Экраны:**
- ✅ MoreScreen - экран настроек
- ✅ ThemeIconScreen - тема и иконки

**Статус:**
- ✅ Полный стек (Domain, Data, UI)
- ✅ Unit-тесты: SettingsModelsTest.kt, ThemeIconViewModelTest.kt
- ✅ UI-тесты: ThemeIconScreenTest.kt, MoreScreenTest.kt (22 теста)

**Детальный план:** `docs/screens/1_More_Screen.md`

---

### 2. Авторизация (Auth) - ⚠️ ЧАСТИЧНО ЗАВЕРШЕНО

**Экраны:**
- ✅ LoginScreen - экран входа
- ❌ Проверка авторизации при запуске и защита экранов

**Статус:**
- ✅ LoginScreen: Domain, Data, UI, 68 unit-тестов, 15 UI-тестов
- ❌ Интеграция с приложением при запуске (требуется)

**Детальный план:** `docs/screens/2_Login_Screen.md`

---

### 3. Вкладка "Профиль" (Profile)

**Экраны:**
- ProfileScreen - просмотр профиля
- EditProfileScreen - редактирование профиля
- ChangePasswordScreen - смена пароля
- FriendsScreen - список друзей
- FriendRequestsScreen - запросы в друзья
- UserParksScreen - площадки пользователя
- BlacklistScreen - черный список
- JournalsScreen - дневники (навигация)

**Основные функции:**
- Отображение профиля, друзей, площадок
- Управление друзьями и черным списком
- Редактирование профиля
- Поиск пользователей

**Детальные планы:**
- `docs/screens/3.1_Profile_Screen.md`
- `docs/screens/3.2_EditProfile_Screen.md`
- `docs/screens/3.2.1_ChangePasswordScreen.md`
- `docs/screens/3.3_Friends_Screen.md`
- `docs/screens/3.4_Blacklist_Screen.md`
- `docs/screens/3.5_UserParks_Screen.md`
- `docs/screens/3.6_Journals_Screen.md`

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
- `docs/screens/4_Parks_Screen.md`
- `docs/screens/4.1_Parks_Map_Screen.md`
- `docs/screens/4.2_Park_Detail_Screen.md`
- `docs/screens/4.3_Edit_Park_Screen.md`

---

### 5. Вкладка "Мероприятия" (Events)

**Экраны:**
- EventsScreen - список мероприятий (планируемые/прошедшие)
- EventDetailScreen - детальная информация
- EditEventScreen - создание/редактирование мероприятия

**Основные функции:**
- Просмотр мероприятий (планируемые/прошедшие)
- Создание/редактирование мероприятия (до 15 фото)
- Комментарии, "иду/не иду"
- Шаринг

**Технические задачи:**
- Переключение между типами мероприятий
- Загрузка фото (до 15 шт.)
- Комментарии и функции социальной активности

**Детальные планы:**
- `docs/screens/5_Events_Screen.md`
- `docs/screens/5.1_Event_Detail_Screen.md`
- `docs/screens/5.2_Edit_Event_Screen.md`

---

### 6. Вкладка "Сообщения" (Messages)

**Экраны:**
- DialogsListScreen - список диалогов
- ChatScreen - чат с пользователем

**Основные функции:**
- Отправка сообщений из профиля
- Просмотр диалогов (чат)
- Отправка новых сообщений
- Непрочитанные сообщения

**Технические задачи:**
- API endpoints для сообщений (реализованы в SWApi)
- Список диалогов с непрочитанными
- Чат с отправкой/получением
- Переход из профиля пользователя

**Детальные планы:**
- `docs/screens/6_Dialogs_Screen.md`
- `docs/screens/6.1_Chat_Screen.md`

---

### 7. Дневники (Journals)

**Экраны:**
- JournalsListScreen - список дневников
- JournalDetailScreen - дневник с записями
- EditJournalScreen - создание/редактирование дневника
- EditJournalEntryScreen - создание/редактирование записи
- JournalAccessSettingsScreen - настройки доступа

**Основные функции:**
- Просмотр/комментирование дневников (при доступе)
- Создание/редактирование дневников и записей
- Настройки доступа (публичный/приватный)

**Технические задачи:**
- API endpoints для дневников (реализованы в SWApi)
- Список дневников
- Просмотр дневника с записями
- Комментарии
- Настройки доступа

**Детальные планы:**
- `docs/screens/7_Journals_Screen.md`
- `docs/screens/7.1_Journal_Detail_Screen.md`
- `docs/screens/7.2_Edit_Journal_Screen.md`
- `docs/screens/7.3_Edit_Journal_Entry_Screen.md`
- `docs/screens/7.4_Journal_Access_Settings_Screen.md`

---

## Технологический стек

**Основные:** Kotlin 2.3.0, Jetpack Compose (BOM 2026.01.00), Material 3, Android Gradle Plugin 9.0.0, Java 17

**Требования:** minSdk 26, targetSdk 35, compileSdk 36

**Архитектура:** ViewModel 2.10.0, Navigation 2.9.6, Lifecycle 2.10.0, Room 2.8.4

**Сеть:** Retrofit 3.0.0, Kotlinx Serialization 1.9.0, Coroutines 1.10.2

**Изображения:** Coil Compose 2.7.0

**Хранение:** DataStore 1.2.0, Room 2.8.4

**Качество:** KtLint 14.0.1, Detekt 1.23.8, KSP 2.3.2

**Тестирование:** JUnit 4.13.2, MockK 1.14.7, Turbine 1.1.0, Compose UI Test

---

## Архитектура проекта

### Текущая структура

```
app/src/main/java/com/swparks/
├── data/              # Data layer (repository, crypto, datetime, interceptor, serialization, database, model)
│   └── model/         # API модели и DTOs (17 файлов)
├── domain/            # Domain layer (exception, usecase, model)
│   └── model/         # Доменные модели (4 файла)
├── network/           # API клиенты (SWApi - 57 endpoints)
├── ui/
│   ├── ds/            # Компоненты дизайн-системы (28)
│   ├── model/         # UI формы и запросы (12 файлов)
│   ├── screens/       # Экраны приложения
│   ├── viewmodel/     # ViewModels
│   └── theme/         # Тема
├── navigation/        # Навигация
├── util/              # Утилиты (9 файлов)
└── utils/             # Дополнительные утилиты
```

### Структура тестов

```
app/src/test/java/com/swparks/          # Unit-тесты (55+ тестов)
├── data/                                # Data layer тесты
├── domain/                              # Domain layer тесты
├── model/                               # Модели тесты
├── ui/                                  # UI layer тесты
├── network/                             # Сеть тесты
└── utils/                               # Утилиты тесты

app/src/androidTest/java/com/swparks/    # Интеграционные и UI тесты (56+ тестов)
└── ui/screens/                          # UI тесты экранов
```

---

## Референсы для разработки

### Архитектура

- **nowinandroid** - референсный проект для архитектуры, навигации, DI
- `.cursor/rules/architecture.mdc` - MVVM, Clean Architecture, ручной DI

### Функционал и экраны

- **SwiftUI-WorkoutApp** - iOS-приложение (эталон функционала и дизайна)
- `SwiftUI-WorkoutApp/docs/feature-map.md` - карта экранов и функционала

### Компоненты дизайн-системы

- ✅ 28 компонентов готовы к использованию

---

## Makefile команды

**Основные:** `make help`, `make build`, `make clean`, `make install`, `make all`

**Тестирование:** `make test`, `make android-test`, `make test-all`, `make android-test-report`

**Качество:** `make lint`, `make format`, `make check`

**Публикация:** `make apk`, `make release`

---

## Примечания

- iOS-приложение является эталоном функционала и дизайна
- Компоненты дизайн-системы реализованы и готовы
- Namespace: `com.swparks`
- Реализован полный API (57 endpoints), баги исправлены
- Архитектура безопасности токена реализована
- Использовать безопасное разворачивание опционалов (избегать `!!`)
- После изменений кода выполнять `make format`

### Реализованный API

✅ **57 из 57 endpoints (100%)**

- Авторизация/профиль: 7 endpoints
- Друзья/черный список: 10 endpoints
- Страны/города: 1 endpoint
- Площадки: 15 endpoints
- Мероприятия: 12 endpoints
- Сообщения: 5 endpoints
- Дневники: 7 endpoints

### Безопасность авторизации

✅ **Полностью реализована**

- CryptoManager (AES-128-GCM-HKDF)
- SecureTokenRepository (защищенное хранение в DataStore)
- TokenInterceptor (автоматическое добавление токена)
- AuthInterceptor (обработка 401)
- LoginUseCase, LogoutUseCase, AuthViewModel

### Тестирование

**55+ unit-тестов:** Data layer (20+), Domain layer (8+), Model (14+), ViewModel (5+), UI state (1), Utils (2), Network (1)

**1 интеграционный тест:** CryptoManagerIntegrationTest

**56+ UI-тестов:**
- LoginScreenTest: 15 тестов
- MoreScreenTest: 11 тестов
- ThemeIconScreenTest: 11 тестов
- MyFriendsScreenTest: 19 тестов
- RootScreenTest: несколько тестов

---

## История изменений

- **2025-01:** Первоначальный план разработки
- **2026-01-18:** Полная актуализация - организация по вкладкам, TDD подход
- **2026-01-22:** Обновление на основе фактического состояния (API, архитектура, модели)
- **2026-01-25:** Актуализация вкладки "Ещё" (More) - MoreScreen и ThemeIconScreen полностью реализованы
- **2026-01-31:** Актуализация авторизации (Auth) - LoginScreen полностью реализован, 68 unit-тестов, 15 UI-тестов
- **2026-02-07:** Обновление общего статуса:
  - Добавлен новый компонент дизайн-системы (28 компонентов)
  - Добавлены новые экраны: MyFriendsScreen (19 UI-тестов), ParksAddedByUserScreen
  - EventsViewModel перемещен, добавлен FriendsListViewModel
  - Добавлен database слой с тестами
  - Обновлена статистика: 55+ unit-тестов, 1 интеграционный, 56+ UI-тестов
