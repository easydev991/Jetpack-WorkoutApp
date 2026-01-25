# План разработки Android-приложения SW Parks

## Текущее состояние проекта

### iOS-приложение (SwiftUI-WorkoutApp)

- ✅ **Полностью реализовано** и работает исправно
- ✅ Все экраны и функции работают
- ✅ Полная документация функционала доступна в `SwiftUI-WorkoutApp/docs/feature-map.md`

### Android-приложение (Jetpack-WorkoutApp)

- 🚧 **В активной разработке**
- ✅ Реализованы компоненты дизайн-системы (27 компонентов)
- ✅ Реализованы модели данных (30+ моделей)
- ✅ Реализован полный API клиент SWApi с 57 эндпоинтами
- ✅ Реализован репозиторий SWRepository
- ✅ Реализована базовая навигация через RootScreen
- ✅ Реализована архитектура безопасности для токена (CryptoManager, SecureTokenRepository, TokenInterceptor, AuthInterceptor)
- ✅ Реализованы Use Cases для авторизации (LoginUseCase, LogoutUseCase, IconManager)
- ✅ Реализован AuthViewModel
- ✅ Все баги API исправлены (7 из 7)
- ✅ MoreScreen и ThemeIconScreen полностью реализованы (вкладка "Ещё")
- ⚠️ EventsScreen имеет ViewModel с базовым функционалом
- ⚠️ ParksRootScreen отображает данные из assets, но без бизнес-логики
- ⚠️ ProfileRootScreen, MessagesRootScreen - заглушки
- ❌ Экраны авторизации не реализованы
- ❌ Большинство детальных экранов не имеют функционала
- ✅ Namespace изменен на `com.swparks`

#### Реализованные компоненты дизайн-системы (готовы к использованию)

- **Базовые**: SWButton, SWTextField, SWTextEditor, SWDateTimePicker
- **Изображения**: SWAsyncImage
- **Строки списков**: ListRowView, ParkRowView, EventRowView, JournalRowView, UserRowView, FriendRequestRowView, DialogRowView, MessageBubbleView, CommentRowView, CheckmarkRowView
- **Контейнеры**: FormRowView, FormRowContainer, FormCardContainer, SectionView
- **Профиля**: UserProfileCardView, IncognitoProfileView
- **Состояний и ошибок**: LoadingOverlayView, ErrorContentView
- **Модификаторы**: CircleBadgeView, CircleBackgroundModifier, ScaleOnTapModifier, ColorScheme+isLight

**Всего:** 27 компонентов

#### Реализованные экраны

- ✅ **RootScreen** - корневой экран с навигацией по вкладкам
- ⚠️ **ParksRootScreen** - экран раздела площадок (отображают данные из assets)
- ⚠️ **EventsScreen** - экран раздела мероприятий (есть ViewModel, минимальный функционал)
- ✅ **MoreScreen** - экран раздела настроек (полностью реализован)
- ✅ **ThemeIconScreen** - экран темы и иконки (полностью реализован)
- ❌ **ProfileRootScreen** - экран раздела профиля (только заглушка)
- ❌ **MessagesRootScreen** - экран раздела сообщений (только заглушка)

#### Реализованные модели данных

- **Пользователи и авторизация**: User, LoginSuccess, MainUserForm, RegistrationRequest, ResetPasswordRequest, ChangePasswordRequest
- **Площадки**: Park, ParkType, ParkSize, ParkForm
- **Мероприятия**: Event, EventKind, EventType, EventForm
- **Комментарии и фотографии**: Comment, Photo
- **Друзья и черный список**: FriendAction, BlacklistAction, ApiFriendAction, ApiBlacklistOption
- **Географические данные**: Country, City, Gender
- **Навигация**: TabBarItem, TopLevelDestination
- **Дневники**: JournalResponse, JournalEntryResponse, JournalAccess, EditJournalSettingsRequest
- **Сообщения**: DialogResponse, MessageResponse, MarkAsReadRequest
- **Другие**: TextEntryOption, SocialUpdates, ErrorResponse

**Всего:** 30+ моделей данных

#### Реализованная архитектура

**Data Layer:** AppContainer (DI), SWRepository (57 endpoints), UserPreferencesRepository (DataStore), SecureTokenRepository (защищенный токен с шифрованием Tink)

**Network Layer:** SWApi (57 endpoints), AuthInterceptor (обработка 401), TokenInterceptor (добавление токена в заголовок)

**Security:** CryptoManager (AES-128-GCM-HKDF), EncryptedStringSerializer

**Domain Layer:** LoginUseCase, LogoutUseCase, IconManager, ServerException, NetworkException

**Presentation Layer:** AuthViewModel, EventsViewModel, ThemeIconViewModel, MainActivityViewModel

**Утилиты:** FlexibleDateDeserializer, NetworkUtils, JsonUtils, ReadJSONFromAssets

---

## Общий план реализации по вкладкам (TDD)

### Принципы разработки

**TDD (Test-Driven Development):**

1. **Red**: Написать тесты для целевой логики ДО реализации кода (тесты должны падать)
2. **Green**: Реализовать минимальный код для прохождения тестов
3. **Refactor**: Улучшить код после успешных тестов

**Правила TDD:**

- Всегда начинать с тестов перед написанием кода
- Не менять код пока тесты проходят успешно
- Итеративно дорабатывать функционал до прохождения тестов
- Тесты → Логика → UI (в строгом порядке)

**Пирамида тестирования:**

- Unit-тесты (70%) - бизнес-логика изолированно, MockK для зависимостей
- Integration (20%) - взаимодействие слоев (DAO, Repository), реальные реализации БД
- UI (10%) - критические сценарии, Compose Testing для компонентов

---

## Вкладки навигации

План реализации организован по вкладкам нижней навигации в следующем порядке:

### 1. Вкладка "Ещё" (More)

**Почему первой:**

- Минимальная зависимость от других экранов
- Простая бизнес-логика (настройки приложения)
- Можно почти целиком скопировать логику из JetpackDays (ThemeIcon)
- Позволяет отработать подход TDD на простом функционале

**Экраны для реализации:**

- ✅ `MoreScreen` - экран настроек (раздел "Ещё")
- ✅ `ThemeIconScreen` - экран темы и иконки приложения, и для Android 12+ тоггл для динамических цветов (референс: JetpackDays)

**Использование референса:**

- Логика экрана ThemeIcon из JetpackDays почти полностью переносится
- Один комбинированный экран для выбора темы и иконки (не нужно делать 2 отдельных экрана), и для Android 12+ тоггл для динамических цветов
- Отличаются только фактические иконки приложения
- Локализация та же (русская и английская)

**Статус реализации:** ✅ **ЗАВЕРШЕНО**

- ✅ ThemeIconScreen: полный стек (Domain: AppTheme, AppIcon, IconManager; Data: AppSettingsDataStore; UI: ThemeIconUiState, ThemeIconViewModel, ThemeIconScreen)
- ✅ MoreScreen: 4 секции, все функции (shareApp, openGitHub, sendFeedback)
- ✅ Иконки: activity-aliases (11 штук), PNG и adaptive icons
- ✅ Unit-тесты: SettingsModelsTest.kt, ThemeIconViewModelTest.kt
- ✅ UI-тесты: ThemeIconScreenTest.kt, MoreScreenTest.kt (22 теста)

**Детальный план:** `docs/screens/1_More_Screen.md`

---

### 2. Авторизация (Auth)

**Почему второй:**

- Требуется для всех экранов с бизнес-логикой
- Экран входа должен показываться при запуске для неавторизованных пользователей
- Базовая инфраструктура уже реализована (API, Use Cases, AuthViewModel)

**Экраны для реализации:**

- `LoginScreen` - экран входа (с кнопкой восстановления пароля)

**Основные функции:**

- Вход в сервис workout.su под существующей учетной записью
- Восстановление пароля от учетной записи (через email)
- Сохранение токена авторизации в DataStore с шифрованием Tink
- Проверка авторизации при запуске приложения
- Выход из учетной записи с очисткой токена и флага авторизации

**Технические задачи:**

- ✅ API endpoints для авторизации и восстановления пароля (57 endpoints)
- ✅ Безопасное хранение токена авторизации (CryptoManager, SecureTokenRepository, TokenInterceptor)
- ✅ LoginUseCase, LogoutUseCase, AuthViewModel
- ⚠️ Реализовать LoginScreen (с кнопкой восстановления пароля)
- ⚠️ Реализовать проверку авторизации при запуске и защиту экранов

**Детальные планы:**

- ✅ `docs/auth-token-plan.md` - план безопасной работы с токеном (ЗАВЕРШЕН)
- ✅ `docs/api-implementation-plan.md` - план реализации API (57 endpoints - ЗАВЕРШЕН)
- 📄 `docs/screens/2_Login_Screen.md` - экран входа (НЕ СОЗДАН)

---

### 3. Вкладка "Профиль" (Profile)

**Почему третий:**

- Авторизация требуется для большинства других экранов
- Простые CRUD-операции для начала
- Готовые компоненты дизайн-системы для профиля

**Экраны для реализации:**

- `ProfileScreen` - экран профиля (просмотр)
- `EditProfileScreen` - экран редактирования профиля
- `ChangePasswordScreen` - экран смены пароля
- `FriendsScreen` - экран списка друзей
- `FriendRequestsScreen` - экран запросов в друзья
- `UserParksScreen` - экран площадок пользователя
- `BlacklistScreen` - экран черного списка
- `JournalsScreen` - экран дневников (навигация к функционалу дневников)

**Основные функции:**

- Отображение профиля пользователя (аватар, логин, информация)
- Просмотр списка друзей и площадок пользователя
- Управление дневниками и черным списком
- Отправка/принятие/отклонение запросов на добавление в друзья
- Редактирование профиля
- Поиск пользователей по нику

**Детальные планы:**

- `docs/screens/3.1_Profile_Screen.md` - основной экран профиля
- `docs/screens/3.2_EditProfile_Screen.md` - редактирование профиля
- `docs/screens/3.2.1_ChangePasswordScreen.md` - экран смены пароля
- `docs/screens/3.3_Friends_Screen.md` - экран друзей
- `docs/screens/3.4_Blacklist_Screen.md` - черный список
- `docs/screens/3.5_UserParks_Screen.md` - площадки пользователя
- `docs/screens/3.6_Journals_Screen.md` - дневники (навигация)

---

### 4. Вкладка "Площадки" (Parks)

**Почему четвертый:**

- Офлайн-режим (данные из assets) - проще начать
- Готовые компоненты дизайн-системы (ParkRowView, ListRowView)
- Базовый CRUD для работы с площадками

**Экраны для реализации:**

- `ParksListScreen` - экран списка площадок (с фильтрами)
- `ParksMapScreen` - экран карты площадок
- `ParkDetailScreen` - экран детальной информации о площадке
- `EditParkScreen` - экран создания/редактирования площадки

**Основные функции:**

- Просмотр площадок на карте (оффлайн)
- Просмотр площадок списком (оффлайн)
- Фильтр по размеру, типу и городу (оффлайн)
- Создание новой площадки (макс. 15 фото)
- Изменение/удаление своей площадки
- Просмотр детальной информации о площадке
- Создание мероприятия для площадки
- Построение маршрута до площадки через Google Maps
- Поделиться ссылкой на площадку
- Создавать/изменять/удалять свои комментарии
- Отправить письмо для обновления данных о площадке
- Отправить письмо с жалобой на фото/комментарий
- Указать, что тренируешься/не тренируешься на площадке

**Технические задачи:**

- Использовать Google Maps SDK для карты
- Реализовать фильтрацию площадок (размер, тип, город)
- Загрузка фото до 15 шт.
- Реализовать комментарии к площадкам
- Функция "тренируюсь/не тренируюсь"
- Шаринг ссылки и построение маршрута

**Детальные планы:**

- `docs/screens/4_Parks_Screen.md` - экран списка площадок
- `docs/screens/4.1_Parks_Map_Screen.md` - экран карты площадок
- `docs/screens/4.2_Park_Detail_Screen.md` - детальный экран площадки
- `docs/screens/4.3_Edit_Park_Screen.md` - создание/редактирование площадки

---

### 5. Вкладка "Мероприятия" (Events)

**Почему пятый:**

- Похожая логика на площадки (CRUD, комментарии)
- Готовые компоненты дизайн-системы (EventRowView)
- Частичная реализация уже существует

**Экраны для реализации:**

- `EventsScreen` - экран списка мероприятий (планируемые/прошедшие)
- `EventDetailScreen` - экран детальной информации о мероприятии
- `EditEventScreen` - экран создания/редактирования мероприятия

**Основные функции:**

- Просмотр списка планируемых мероприятий
- Просмотр списка прошедших мероприятий
- Создание нового мероприятия (макс. 15 фото)
- Изменение/удаление своего мероприятия
- Просмотр детальной информации о мероприятии
- Поделиться ссылкой на мероприятие
- Создавать/изменять/удалять свои комментарии
- Отправить письмо с жалобой на фото/комментарий
- Указать, что идешь/не идешь на мероприятие

**Технические задачи:**

- Переключение между планируемыми и прошедшими мероприятиями
- Загрузка фото до 15 шт.
- Реализовать комментарии к мероприятиям
- Функция "иду/не иду" на мероприятие
- Шаринг ссылки на мероприятие

**Детальные планы:**

- `docs/screens/5_Events_Screen.md` - экран списка мероприятий
- `docs/screens/5.1_Event_Detail_Screen.md` - детальный экран мероприятия
- `docs/screens/5.2_Edit_Event_Screen.md` - создание/редактирование мероприятия

---

### 6. Вкладка "Сообщения" (Messages)

**Почему шестой:**

- Зависит от профиля (для списка диалогов)
- HTTP запросы для обновления данных (как в iOS-приложении)

**Экраны для реализации:**

- `DialogsListScreen` - экран списка диалогов
- `ChatScreen` - экран чата с пользователем

**Основные функции:**

- Отправлять сообщения другим пользователям (из профиля)
- Просмотр своих диалогов в формате чатов
- Отправлять новые сообщения
- Видеть количество непрочитанных сообщений

**Технические задачи:**

- Создать API endpoints для работы с сообщениями
- Реализовать список диалогов с непрочитанными сообщениями
- Реализовать чат с отправкой и получением сообщений
- Реализовать переход на чат из профиля пользователя

**Детальные планы:**

- `docs/screens/6_Dialogs_Screen.md` - экран списка диалогов
- `docs/screens/6.1_Chat_Screen.md` - экран чата

---

### 7. Дневники (Journals)

**Почему отдельная секция:**

- Дополнительный функционал (не основной MVP)
- Зависит от профиля и авторизации
- Сложная логика настроек доступа

**Экраны для реализации:**

- `JournalsListScreen` - экран списка дневников пользователя
- `JournalDetailScreen` - экран дневника с записями
- `EditJournalScreen` - экран создания/редактирования дневника
- `EditJournalEntryScreen` - экран создания/редактирования записи
- `JournalAccessSettingsScreen` - экран настроек доступа к дневнику

**Основные функции:**

- Просмотр/комментирование чужих дневников (при наличии доступа)
- Создание/изменение/удаление дневников и записей
- Изменение настроек доступа для своих дневников (публичный/приватный)

**Технические задачи:**

- Создать API endpoints для работы с дневниками
- Реализовать список дневников пользователя
- Реализовать просмотр дневника с записями
- Реализовать создание/редактирование дневников и записей
- Реализовать комментарии к записям дневников
- Реализовать настройки доступа к дневникам

**Детальные планы:**

- `docs/screens/7_Journals_Screen.md` - экран списка дневников
- `docs/screens/7.1_Journal_Detail_Screen.md` - экран дневника с записями
- `docs/screens/7.2_Edit_Journal_Screen.md` - создание/редактирование дневника
- `docs/screens/7.3_Edit_Journal_Entry_Screen.md` - создание/редактирование записи
- `docs/screens/7.4_Journal_Access_Settings_Screen.md` - настройки доступа

---

## Формат детальных документов для каждого экрана

Каждый экран будет иметь отдельный документ в папке `docs/screens/` по аналогии с JetpackDays.

### Структура документа экрана

```markdown
# Экран X.Y: Название экрана (русское и английское)

## Статус

- Статус реализации (Новый план / В работе / Выполнен)

## Обзор

Краткое описание экрана и его назначения

## Назначение

Основная цель и функции экрана

## Компоненты

Список основных компонентов экрана

## Навигация

- Откуда переходят на этот экран
- Куда можно перейти с этого экрана

## Зависимости от других этапов

- Какие экраны/компоненты требуются для работы этого экрана

## Подробный план реализации по TDD

### Шаг 1: [Название шага]

- Статус
- Детальные задачи

### Шаг 2: [Название шага]

...

## Реализация

- Файловая структура
- Основные компоненты
- Архитектура

## Тестирование

- Unit-тесты (с описанием)
- Интеграционные тесты (с описанием)
- UI-тесты (с описанием)

## Критерии завершения этапа

- Чеклист завершения

## Блокируемые этапы

- Какие экраны блокирует этот этап

## Примечания

- Важные замечания и особенности

## Текущее состояние реализации

- Выполнено / Не выполнено

## История изменений

- Дата и описание изменений
```

---

## Технологический стек Android-приложения

### Основные технологии

- **Kotlin** 2.3.0 - язык программирования
- **Jetpack Compose** - UI фреймворк (Compose BOM 2026.01.00)
- **Material 3** - дизайн-система
- **Android Gradle Plugin** 9.0.0
- **Java** 17 - JDK версия
- **Kotlin Compose Compiler** (встроен в Kotlin 2.3.0)

### Минимальные требования

- **minSdk**: 26 (Android 8.0)
- **targetSdk**: 35
- **compileSdk**: 36

### Архитектурные компоненты

- **ViewModel Compose** 2.10.0 - для управления состоянием экранов
- **Navigation Compose** 2.9.6 - для навигации между экранами
- **Lifecycle Runtime KTX** 2.10.0 - для работы с жизненным циклом
- **Room** 2.8.4 - локальное хранение данных

### Сетевое взаимодействие

- **Retrofit** 3.0.0 - HTTP клиент для API запросов
- **Kotlinx Serialization** 1.9.0 - сериализация/десериализация JSON
- **Retrofit Kotlinx Serialization Converter** 1.0.0 - конвертер для Retrofit

### Асинхронность

- **Kotlin Coroutines** 1.10.2 - для асинхронных операций

### Загрузка изображений

- **Coil Compose** 2.7.0 - библиотека для загрузки и кэширования изображений

### Хранение данных

- **DataStore Preferences** 1.2.0 - для хранения настроек и пользовательских данных
- **Room** 2.8.4 - для локального хранения данных (Kotlin extensions и runtime)

### Дополнительные библиотеки

- **Core KTX** 1.17.0 - расширения Kotlin для Android
- **Activity Compose** 1.12.2 - интеграция Compose с Activity
- **AppCompat** 1.7.1 - поддержка устаревших компонентов

### Код и качество

- **KtLint** 14.0.1 - линтер и форматирование кода
- **Detekt** 1.23.8 - статический анализ кода
- **KSP** 2.3.2 - процессор аннотаций (вместо KAPT)

### Тестирование

- **JUnit** 4.13.2 - unit-тестирование
- **AndroidX Test Ext JUnit** 1.3.0 - инструментальные тесты
- **Espresso Core** 3.7.0 - UI тестирование
- **Compose UI Test JUnit4** - тестирование Compose UI
- **MockK** 1.14.7 - создание моков для тестов
- **Turbine** 1.1.0 - для тестирования Flow/StateFlow

---

## Архитектура проекта

### Текущая структура

```text
app/src/main/java/com/swparks/
├── data/              # Data layer
│   ├── AppContainer.kt           # DI контейнер
│   ├── SWRepository.kt           # Репозиторий (57 endpoints)
│   ├── UserPreferencesRepository.kt # DataStore настройки
│   ├── SecureTokenRepository.kt   # Репозиторий для защищенного токена
│   ├── APIError.kt, ErrorResponse.kt, StatusCodeGroup.kt
│   ├── NetworkUtils.kt
│   ├── crypto/                   # Шифрование через Tink
│   │   ├── CryptoManager.kt
│   │   └── CryptoManagerImpl.kt
│   ├── datetime/                 # Десериализация дат
│   │   └── FlexibleDateDeserializer.kt
│   ├── interceptor/              # Interceptor'ы для OkHttp
│   │   ├── AuthInterceptor.kt    # Обработка 401
│   │   └── TokenInterceptor.kt   # Добавление токена в заголовок
│   ├── serialization/            # Сериализаторы для JSON
│   │   ├── IntStringSerializer.kt
│   │   └── LongStringSerializer.kt
│   └── serializer/              # Сериализатор для шифрования
│       └── EncryptedStringSerializer.kt
├── domain/            # Domain layer
│   ├── exception/               # Исключения
│   │   ├── NetworkException.kt
│   │   └── ServerException.kt
│   └── usecase/                # Use cases
│       ├── ILoginUseCase.kt
│       ├── ILogoutUseCase.kt
│       ├── LoginUseCase.kt
│       └── LogoutUseCase.kt
├── model/             # Модели данных (30+ моделей)
│   ├── User, LoginSuccess, MainUserForm, RegistrationRequest
│   ├── ResetPasswordRequest, ChangePasswordRequest
│   ├── Park, ParkType, ParkSize, ParkForm
│   ├── Event, EventKind, EventType, EventForm
│   ├── Comment, Photo
│   ├── FriendAction, BlacklistAction, ApiFriendAction, ApiBlacklistOption
│   ├── Country, City, Gender
│   ├── TabBarItem, TopLevelDestination
│   ├── JournalResponse, JournalEntryResponse, JournalAccess
│   ├── EditJournalSettingsRequest
│   ├── DialogResponse, MessageResponse, MarkAsReadRequest
│   ├── TextEntryOption, SocialUpdates, ErrorResponse
│   └── ... (и другие модели)
├── network/           # API клиенты
│   └── SWApi.kt                  # HTTP клиент (57 endpoints)
├── ui/
│   ├── ds/            # Компоненты дизайн-системы (27 компонентов)
│   ├── screens/       # Экраны приложения
│   │   ├── events/   # Мероприятия (EventsScreen, EventsViewModel)
│   │   ├── profile/  # Профиль (ProfileRootScreen - заглушка)
│   │   ├── messages/ # Сообщения (MessagesRootScreen - заглушка)
│   │   ├── more/     # Настройки (MoreScreen - заглушка)
│   │   ├── parks/    # Площадки (ParksRootScreen - заглушка)
│   │   └── RootScreen.kt
│   ├── viewmodel/     # ViewModels
│   │   └── AuthViewModel.kt
│   └── theme/         # Тема и цвета
├── navigation/        # Навигация
│   ├── AppState.kt
│   ├── Destinations.kt
│   ├── Navigation.kt
│   └── TopLevelDestination.kt
└── utils/             # Утилиты
    ├── JsonUtils.kt
    └── ReadJSONFromAssets.kt
```

### Структура тестов

```text
app/src/test/java/com/swparks/                       # Unit-тесты
├── data/                                          # Тесты Data layer
│   ├── crypto/                                     # Тесты шифрования
│   │   └── CryptoManagerIntegrationTest.kt
│   ├── interceptor/                                 # Тесты интерцепторов
│   │   ├── AuthInterceptorTest.kt
│   │   └── TokenInterceptorTest.kt
│   ├── repository/                                 # Тесты репозитория
│   │   ├── SWRepositoryTest.kt
│   │   ├── SWRepositoryAuthTest.kt
│   │   ├── SWRepositoryProfileTest.kt
│   │   ├── SWRepositoryFriendsTest.kt
│   │   ├── SWRepositoryParksTest.kt
│   │   ├── SWRepositoryEventsTest.kt
│   │   ├── SWRepositoryMessagesTest.kt
│   │   ├── SWRepositoryJournalsTest.kt
│   │   └── SWRepositoryCommentsTest.kt
│   ├── serialization/                              # Тесты сериализаторов
│   │   ├── IntStringSerializerTest.kt
│   │   └── LongStringSerializerTest.kt
│   ├── serializer/                                 # Тесты сериализаторов
│   │   └── EncryptedStringSerializerTest.kt
│   ├── datetime/                                   # Тесты десериализации дат
│   │   └── FlexibleDateDeserializerTest.kt
│   ├── SecureTokenRepositoryTest.kt               # Тесты защищенного репозитория
│   └── UserPreferencesRepositoryTest.kt            # Тесты репозитория настроек
├── domain/                                        # Тесты Domain layer
│   └── usecase/                                   # Тесты use cases
│       ├── LoginUseCaseTest.kt
│       └── LogoutUseCaseTest.kt
├── model/                                         # Тесты моделей данных
│   ├── LoginSuccessTest.kt
│   ├── UserTest.kt, MainUserFormTest.kt
│   ├── ParkTest.kt, ParkFormTest.kt, ParkDeserializationTest.kt
│   ├── EventTest.kt, EventFormTest.kt, EventDeserializationTest.kt
│   ├── CommentTest.kt
│   ├── JournalResponseTest.kt, JournalEntryResponseTest.kt
│   ├── DialogResponseTest.kt, MessageResponseTest.kt
└── utils/                                         # Тесты утилит
    ├── JsonUtilsTest.kt
    └── ReadJSONFromAssetsTest.kt

app/src/androidTest/java/com/swparks/               # Интеграционные тесты
└── data/crypto/
    └── CryptoManagerIntegrationTest.kt               # Интеграционные тесты шифрования
```

### Целевая структура (референс: JetpackDays)

```text
app/src/main/java/com/swparks/
├── data/              # Data layer
│   ├── database/       # Room entities, DAO, DB, converters
│   ├── repository/     # Repository implementations
│   └── preferences/   # DataStore preferences
├── domain/            # Domain layer (опционально)
│   ├── model/         # Domain entities
│   ├── repository/    # Repository interfaces
│   └── usecase/       # Use cases
├── ui/                # Presentation layer
│   ├── component/      # Reusable UI components (ds/)
│   ├── screen/        # Screens
│   ├── state/         # UI state classes
│   └── theme/         # App theme
├── viewmodel/         # ViewModels
├── navigation/        # Navigation routes
├── di/               # Dependency injection (AppContainer)
└── util/             # Utilities
```

---

## Референсы для разработки

### Архитектура и структура

- **JetpackDays** - референсный проект для архитектуры, навигации, DI и структуры проекта
- Рекомендации из `.cursor/rules/architecture.mdc` - MVVM, Clean Architecture, ручной DI
- Рекомендации из `.cursor/rules/testing.mdc` - тестирование (TDD, пирамида тестирования)

### Функционал и экраны

- **SwiftUI-WorkoutApp** - iOS-приложение - эталон функциональности и дизайна
- `SwiftUI-WorkoutApp/docs/feature-map.md` - карта экранов и функционала
- При разработке экранов ориентироваться на iOS-версию для консистентности UI и UX

### Компоненты дизайн-системы

- ✅ Компоненты дизайн-системы уже реализованы и готовы к использованию (27 компонентов)
- Использовать готовые компоненты для верстки экранов

### Документация JetpackDays

- `JetpackDays/docs/Screen_1.1_Root_Screen.md` - корневой экран с навигацией
- `JetpackDays/docs/Screen_2.1_Main_Screen.md` - главный экран списка
- `JetpackDays/docs/Screen_3.1_Item_Screen.md` - детальный экран
- `JetpackDays/docs/Screen_5.1_More_Screen.md` - экран настроек
- `JetpackDays/docs/Screen_5.2_Theme_Icon_Screen.md` - тема и иконка (референс для SW Parks)
- `JetpackDays/docs/Screen_5.3_App_Data_Screen.md` - данные приложения (референс для SW Parks)

---

## Makefile команды

Проект поддерживает Makefile для упрощения разработки.

**Основные команды:**

- `make help` - показать справочное сообщение со всеми командами
- `make build` - сборка APK для отладки
- `make clean` - очистка кэша проекта
- `make install` - установка APK на устройство
- `make all` - полная проверка (сборка + тесты + линтер) и установка APK на устройство

**Тестирование:**

- `make test` - запуск unit-тестов (JVM, без устройства)
- `make android-test` - запуск интеграционных тестов на Android устройстве
- `make test-all` - запуск всех тестов (unit + интеграционные)
- `make android-test-report` - открыть HTML отчет интеграционных тестов в браузере

**Качество кода:**

- `make lint` - запуск ktlint, detekt и markdownlint (проверка)
- `make format` - форматирование кода (ktlint + detekt с исправлениями) и Markdown-файлов
- `make check` - полная проверка (сборка + тесты + линтер)

**Публикация:**

- `make apk` - создать подписанный APK для релизной конфигурации (без повышения версии)
- `make release` - создать подписанную AAB-сборку для публикации (с повышением версии)

---

## Примечания

- iOS-приложение является эталоном функциональности и дизайна
- При разработке Android-версии необходимо ориентироваться на iOS-версию для обеспечения консистентности функционала
- Компоненты дизайн-системы полностью реализованы и готовы к использованию (23 компонента)
- Основная работа сейчас - это реализация бизнес-логики и функционала экранов
- Namespace проекта: `com.swparks`
- Используется современный стек технологий: Kotlin 2.3.0, Compose BOM 2026.01.00, Java 17
- Реализована базовая архитектура с репозиторием, API-клиентом и навигацией
- Для архитектурных и технических решений использовать JetpackDays как референс
- Для вкладки "Ещё" можно почти целиком скопировать логику из JetpackDays для экранов ThemeIcon и AppData (отличаются только иконки)
- Все логи в коде писать на русском языке
- Использовать безопасное разворачивание опционалов (избегать `!!`)
- После любых изменений в коде выполнять `make format`

### Реализованный API

✅ **57 из 57 endpoints полностью реализованы** (100%)

- **Авторизация и профиль**: 7 endpoints (login, logout, register, resetPassword, changePassword, getProfile, updateProfile)
- **Друзья и черный список**: 10 endpoints (getFriends, getFriendRequests, sendFriendRequest, acceptFriendRequest, rejectFriendRequest, deleteFriend, getBlacklist, addToBlacklist, removeFromBlacklist, getSocialUpdates)
- **Страны и города**: 1 endpoint (getCountries)
- **Площадки**: 15 endpoints (getParks, getPark, createPark, updatePark, deletePark, getUserParks, getComments, createComment, updateComment, deleteComment, reportComment, sendEmailUpdate, setTrainingStatus, updatePark)
- **Мероприятия**: 12 endpoints (getEvents, getEvent, createEvent, updateEvent, deleteEvent, getUpcomingEvents, getPastEvents, getUserEvents, setEventStatus, getComments, createComment, deleteComment)
- **Сообщения**: 5 endpoints (getDialogs, getMessages, sendMessage, deleteDialog, markAsRead)
- **Дневники**: 7 endpoints (getJournals, getJournal, createJournal, updateJournal, deleteJournal, updateJournalSettings, getEntries)

**Статистика:** 100% реализация API сервера, все баги исправлены (7 из 7), 0 расхождений с iOS-версией

### Безопасность авторизации

✅ **Полностью реализована безопасная работа с токеном авторизации**

- CryptoManager (AES-128-GCM-HKDF)
- SecureTokenRepository (защищенное хранение в DataStore)
- TokenInterceptor (автоматическое добавление токена в заголовок)
- AuthInterceptor (обработка ошибок 401)
- LoginUseCase, LogoutUseCase, AuthViewModel

### Тестирование

**Unit-тесты:** Data layer (20+ тестов), Domain layer (4 теста), Model (14+ тестов), ViewModel (3 теста), Utils (2 теста), MockSWApi

**Интеграционные тесты:** CryptoManagerIntegrationTest

**Всего:** 40+ тестов

**Инструменты:** MockK, Turbine, изолированное тестирование без реальных HTTP запросов

### Детальная документация

- ✅ `docs/api-implementation-plan.md` - план реализации API (ЗАВЕРШЕН)
- ✅ `docs/auth-token-plan.md` - план безопасной работы с токеном (ЗАВЕРШЕН)
- ✅ `docs/navigation-plan.md` - план навигации
- ✅ `docs/screens/1_More_Screen.md` - экран настроек и тема/иконка (ЗАВЕРШЕН)
- 📄 Остальные детальные планы экранов не созданы

---

## История изменений

- 2025-01: Первоначальный план разработки
- 2026-01-18: Полная актуализация плана - организация по вкладкам навигации, TDD подход, детальные планы для каждого экрана
- 2026-01-22: Обновление плана на основе фактического состояния проекта:
  - Реализован полный API клиент SWApi с 57 эндпоинтами
  - Реализована архитектура безопасности для токена авторизации (CryptoManager, SecureTokenRepository, TokenInterceptor, AuthInterceptor)
  - Реализованы Use Cases для авторизации (LoginUseCase, LogoutUseCase)
  - Реализован AuthViewModel
  - Все баги API исправлены (7 из 7)
  - Обновлен список моделей данных (30+ моделей)
  - Обновлена структура проекта
- 2026-01-25: Актуализация статуса реализации вкладки "Ещё" (More):
  - ✅ MoreScreen и ThemeIconScreen полностью реализованы
  - ✅ IconManager use case реализован
  - ✅ AppSettingsDataStore для хранения настроек
  - ✅ Модели AppTheme и AppIcon созданы
  - ✅ ThemeIconViewModel создан
  - ✅ Unit-тесты: SettingsModelsTest.kt, ThemeIconViewModelTest.kt
  - ✅ UI-тесты: ThemeIconScreenTest.kt, MoreScreenTest.kt (22 теста)
- 2026-01-25: Актуализация общего статуса проекта:
  - Обновлен список реализованных экранов
  - Обновлены списки Use Cases и ViewModels
  - Обновлено количество unit-тестов для Domain layer (4) и ViewModels (3)
- 2026-01-25: Обновление плана разработки:
  - Этап авторизации (Auth) перемещен сразу после вкладки "Ещё" (новый номер этапа: 2)
  - Обновлена нумерация всех этапов с 2 по 7
  - Обновлены ссылки на детальные документы экранов с учетом новой нумерации
