# Jetpack Workout App - Технический контекст

## Архитектура приложения

Приложение построено по архитектуре MVVM (Model-View-ViewModel) с использованием современных технологий Android:

- **Язык программирования**: Kotlin
- **Фреймворк UI**: Jetpack Compose
- **Сетевое взаимодействие**: Retrofit с Kotlinx Serialization
- **Локальное хранилище**: Room Database
- **Управление состоянием**: Jetpack Compose State
- **Асинхронные операции**: Kotlin Coroutines
- **Навигация**: Compose Navigation

## Архитектура SWParks

### MVVM

- **Model**: Data layer (Room, repositories, API)
- **View**: Compose UI
- **ViewModel**: UI state management

### Clean Architecture

```
Presentation (UI, ViewModel)
├── Domain (Use Cases, entities)
└── Data (Repositories, локальные и сетевые источники)
```

**Важно:** Есть сетевые источники данных (SWApi) для работы с сервером workout.su

---

### Dependency Injection (DI)

**Ручной DI через factory методы** в `AppContainer`. Проект не использует Hilt.

**Причины:** простой граф зависимостей, быстрая компиляция, меньше зависимостей, простые тесты.

**DI контейнер:**

- **AppContainer** - factory методы для создания репозиториев, API клиентов и ViewModel
- **AuthContainer** (опционально) - factory методы для авторизации и сессии
- **PreferencesContainer** (опционально) - factory методы для DataStore и настроек

**Примечание:** Hilt стоит рассмотреть при росте проекта (>15 ViewModel, сложные графы зависимостей).

### Слои

#### Data

- **Room** (опционально) - для кэширования данных офлайн
- **Repository Implementations**:
  - `SWRepository` - основной репозиторий для работы с данными (площадки, мероприятия, пользователи и т.д.)
  - `UserPreferencesRepository` - хранение настроек (тема, язык, токен авторизации)
- **API Client**: `SWApi` - HTTP клиент для запросов к серверу (Retrofit + Kotlinx Serialization)
- **Formatter** (опционально) - локализация и форматирование данных

#### Domain

- **Use Cases**: бизнес-логика, один use case - одна ответственность:
  - **Авторизация**: `LoginUseCase`, `LogoutUseCase`, `RecoveryPasswordUseCase`
  - **Площадки**: `GetParksUseCase`, `GetParkDetailUseCase`, `CreateParkUseCase`, `UpdateParkUseCase`
  - **Мероприятия**: `GetEventsUseCase`, `GetEventDetailUseCase`, `CreateEventUseCase`, `UpdateEventUseCase`
  - **Профиль**: `GetUserProfileUseCase`, `UpdateUserProfileUseCase`
  - **Друзья**: `GetFriendsUseCase`, `GetFriendRequestsUseCase`, `AcceptFriendRequestUseCase`
  - **Сообщения**: `GetDialogsUseCase`, `GetMessagesUseCase`, `SendMessageUseCase`
  - **Дневники**: `GetJournalsUseCase`, `GetJournalEntriesUseCase`
  - И другие use cases для бизнес-логики приложения
- **Entities**: модели данных:
  - `User`, `LoginSuccess` - пользователи и авторизация
  - `Park`, `ParkType`, `ParkSize` - площадки
  - `Event`, `EventKind` - мероприятия
  - `Comment` - комментарии
  - `Photo` - фотографии
  - `Country`, `City` - географические данные
  - `FriendAction`, `BlacklistAction` - действия над друзьями/черным списком
  - `Gender`, `TabBarItem` - вспомогательные модели
- **Repository Interfaces** (опционально) - абстракции репозиториев для тестирования

#### Presentation

- **ViewModels**:
  - `AuthViewModel` - авторизация и восстановление пароля
  - `ProfileViewModel`, `EditProfileViewModel` - профиль и редактирование
  - `ParksListViewModel`, `ParksMapViewModel`, `ParkDetailViewModel`, `EditParkViewModel` - площадки
  - `EventsViewModel`, `EventDetailViewModel`, `EditEventViewModel` - мероприятия
  - `DialogsListViewModel`, `ChatViewModel` - сообщения
  - `JournalsListViewModel`, `JournalDetailViewModel`, `EditJournalViewModel` - дневники
  - `RootScreenViewModel` - управление навигацией по вкладкам
  - И другие ViewModels для оставшихся экранов
- **UI State**: sealed классы (Loading/Success/Error) для состояний экранов:
  - `AuthUiState`, `ProfileUiState`, `ParksUiState`, `EventsUiState`
  - `DialogsUiState`, `ChatUiState`, `JournalsUiState`
- **Screens**: Compose экраны в `ui/screens/`:
  - `auth/` - экраны авторизации (LoginScreen, RecoveryScreen)
  - `profile/` - экраны профиля (ProfileScreen, EditProfileScreen, FriendsScreen и т.д.)
  - `parks/` - экраны площадок (ParksListScreen, ParksMapScreen, ParkDetailScreen и т.д.)
  - `events/` - экраны мероприятий (EventsScreen, EventDetailScreen, EditEventScreen)
  - `messages/` - экраны сообщений (DialogsListScreen, ChatScreen)
  - `journals/` - экраны дневников (JournalsListScreen, JournalDetailScreen и т.д.)
  - `settings/` - экраны настроек (MoreScreen, ThemeSettingsScreen, AppDataScreen и т.д.)
  - `RootScreen` - корневой экран с навигацией по вкладкам
- **Navigation**: `navigation/Screen.kt` - централизованные маршруты навигации
- **Theme**: тема в `ui/theme/`
- **Design System**: компоненты дизайн-системы в `ui/ds/` (23 компонента)

#### Utilities

- **Logger** - интерфейс (аналог JetpackDays - AndroidLogger, NoOpLogger)
- **Analytics** (опционально) - `FirebaseAnalyticsHelper` для screen_view и событий (только release)
- **Crash** (опционально) - `CrashlyticsHelper` для отчетов о крашах (только release)
- **NetworkUtils** (опционально) - утилиты для работы с сетью
- **ImageUtils** (опционально) - утилиты для работы с изображениями

### Отличия от JetpackDays

#### Сетевые функции

- **SWApi** - HTTP клиент для работы с сервером workout.su (отсутствует в JetpackDays)
- **Retrofit + Kotlinx Serialization** - для API запросов (отсутствует в JetpackDays)
- **Авторизация** - Login/Logout с токеном авторизации (отсутствует в JetpackDays)

#### Масштаб проекта

- **Больше экранов** - ~30+ экранов vs ~7 в JetpackDays
- **Больше моделей** - 14+ моделей vs ~7 в JetpackDays
- **Сложные бизнес-потоки** - авторизация, друзья, сообщения, дневники (отсутствуют в JetpackDays)

### Референсы

#### Архитектура и структура

- **JetpackDays** - референсный проект для базовой архитектуры (MVVM, Clean Architecture, ручной DI)
- Подход к слоистой архитектуре идентичен JetpackDays
- Dependency Injection через factory методы (без Hilt) - как в JetpackDays

#### Функционал и экраны

- **SwiftUI-WorkoutApp** - iOS-приложение - эталон функциональности и дизайна
- При разработке экранов ориентироваться на iOS-версию для консистентности UI и UX

#### Компоненты дизайн-системы

- ✅ Компоненты дизайн-системы уже реализованы и готовы к использованию (23 компонента)
- Использовать готовые компоненты для верстки экранов

## Структура проекта

```
app/
├── src/main/
│   ├── java/com/swparks/
│   │   ├── data/           # Слой данных (репозитории, источники данных)
│   │   ├── model/          # Модели данных
│   │   ├── navigation/     # Навигация (маршруты, состояние навигации)
│   │   ├── network/        # Сетевой слой (API интерфейсы, DTO)
│   │   ├── ui/             # UI слой (экраны, компонуемые элементы, темы)
│   │   │   ├── ds/         # Компоненты пользовательского интерфейса
│   │   │   ├── navigation/ # Компоненты навигации UI
│   │   │   ├── screens/    # Экраны приложения
│   │   │   ├── state/      # Состояния UI компонентов
│   │   │   ├── theme/      # Тема приложения
│   │   │   └── viewmodel/  # ViewModel классы
│   │   ├── utils/          # Утилиты и вспомогательные функции
│   │   ├── JetpackWorkoutApplication.kt  # Класс приложения
│   │   └── MainActivity.kt # Главная активность
│   ├── res/                # Ресурсы (строки, изображения и т.д.)
│   └── AndroidManifest.xml # Манифест приложения
```

## Зависимости

Основные зависимости проекта:

- `androidx.core:core-ktx:1.17.0`
- `androidx.lifecycle:lifecycle-runtime-ktx:2.10.0`
- `androidx.activity:activity-compose:1.12.2`
- `androidx.compose:*:compose-bom:2026.01.00` (Material 3)
- `androidx.navigation:navigation-compose:2.9.6`
- `com.squareup.retrofit2:retrofit:3.0.0`
- `org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0`
- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2`
- `io.coil-kt:coil-compose:2.7.0`
- `androidx.datastore:datastore-preferences:1.2.0`
- `androidx.room:room-runtime:2.8.4`
- `androidx.room:room-ktx:2.8.4`
- `com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0`

## Конфигурация сборки

- **compileSdk**: 36
- **minSdk**: 26
- **targetSdk**: 35
- **Компиляция Kotlin**: JVM Target 17
- **Compose**: Включена поддержка
- **ProGuard/R8**: Включена минификация для релизных сборок
- **Gradle Plugin**: 9.0.0
- **Kotlin Version**: 2.3.0

## Ключевые компоненты

1. **JetpackWorkoutApplication**: Класс приложения, инициализирующий контейнер зависимостей
2. **MainActivity**: Главная активность, устанавливающая Compose контент
3. **RootScreen**: Корневой экран приложения, управляющий основной навигацией
4. **AppContainer**: Контейнер для управления зависимостями приложения

## Слой данных

- Использует Repository паттерн для управления источниками данных
- Локальное хранилище реализовано через Room Database
- Предпочтения хранятся в DataStore
- Сетевые запросы обрабатываются через Retrofit

## UI слой

- Построен полностью на Jetpack Compose
- Использует Material Design 3 компоненты
- Тема приложения определена в `ui.theme` пакете
- Навигация между экранами реализована через Navigation Compose
- Используется система верхнеуровневой навигации с 5 вкладками: Parks, Events, Messages, Profile и More
- Реализованы детальные экраны для просмотра и редактирования объектов (площадок, мероприятий, профилей и т.д.)
- Навигационное состояние управляется через AppState с использованием rememberAppState и NavHostController
- Для каждой вкладки реализованы соответствующие иконки (выбранные и не выбранные), текстовые подписи и заголовки
- Используется BottomNavigationBar для отображения навигационной панели в нижней части экрана

## Разрешения

Приложение запрашивает следующие разрешения:

- `INTERNET` - для сетевого взаимодействия
- `CAMERA` - для съемки фотографий
- `ACCESS_NETWORK_STATE` - для проверки состояния сети
- `ACCESS_FINE_LOCATION` и `ACCESS_COARSE_LOCATION` - для определения местоположения
- `READ_MEDIA_IMAGES` - для доступа к медиафайлам

## Сборка и разработка

Для сборки проекта используется Gradle с Kotlin DSL. Основные команды:

- `./gradlew build` - полная сборка проекта
- `./gradlew assembleDebug` - сборка отладочной версии
- `./gradlew installDebug` - установка отладочной версии на устройство

## Тестирование

### Типы тестов

- **Unit**: бизнес-логика изолированно, MockK для зависимостей, AAA паттерн
- **Integration**: взаимодействие слоев (DAO, Repository), реальные реализации БД
- **UI**: критические сценарии, Compose Testing для компонентов

### Инструменты

- JUnit 5 - unit-тесты
- MockK - мокирование
- Compose Testing - Compose компоненты
- Room Testing - для интеграционных тестов БД
- kotlinx-coroutines-test - для тестирования корутин
- Turbine - для тестирования Flow/StateFlow (app.cash.turbine:turbine:1.1.0)

### Структура

- `app/src/test/` - unit-тесты (ViewModels, Use Cases, Domain models)
- `app/src/androidTest/` - integration/UI тесты (DAO, Repository, UI компоненты)
- Структура зеркалит код
- Имена классов: `*Test`

### Best Practices

**Важно:** Ограничения на интеграционные тесты с ViewModels

- ⚠️ **Допустимо:** Для интеграционных тестов ViewModels использовать `runTest`, `MainDispatcherRule` и `Turbine`
- ✅ **Рекомендуется:** Тестировать ViewModels только через unit-тесты с MockK
- ✅ **Допустимо:** Тестировать DAO и Repository через интеграционные тесты без ViewModels
- ✅ **Допустимо:** UI-тесты для Compose компонентов без бизнес-логики

**Причина ограничений:**

- Конфликт между `runBlocking` и `viewModelScope.launch`
- Flow репозитория не активируется корректно в тестах
- Тесты зависают бесконечно или падают
- Unit-тесты с MockK обеспечивают лучшее покрытие бизнес-логики

#### Рабочий подход к тестированию

**Unit-тесты ViewModels (с MockK):**

```kotlin
@Test
fun loadItems_whenRepositoryReturnsData_thenSuccessState() {
    // Given
    val mockRepository = mockk<ItemRepository>()
    every { mockRepository.getAllItems() } returns flowOf(listOf(item))
    val viewModel = MainScreenViewModel(mockRepository)

    // When
    viewModel.loadItems()

    // Then
    assertEquals(MainScreenState.Success(listOf(item)), viewModel.uiState.value)
}
```

**Интеграционные тесты DAO и Repository:**

```kotlin
@Test
fun test() {
    runBlocking {
        repository.insertItem(item)
        val result = repository.getItemById(id)
        assertNotNull(result)
    }
}
```

- ✅ Прямые вызовы DAO/Repository
- ✅ Синхронные операции с БД
- ✅ Блокируют поток до завершения корутины
- ✅ Не используют ViewModel
- ✅ Используют только repository

**Интеграционные тесты ViewModels (только для существующих):**

```kotlin
@Test
fun whenItemExistsInDatabase_thenLoadsSuccessfully() {
    runTest {
        val insertedId = repository.insertItem(testItem)
        val savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId))
        viewModel = DetailScreenViewModel(repository, NoOpLogger(), savedStateHandle)

        // Тестируем эмиссии StateFlow с помощью Turbine
        viewModel.uiState.test {
            val loadingState = awaitItem()
            assertTrue(loadingState is DetailScreenState.Loading)

            val successState = awaitItem()
            assertTrue(successState is DetailScreenState.Success)
        }
    }
}
```

- ✅ Использовать `runTest` вместо `runBlocking`
- ✅ Использовать `MainDispatcherRule` для замены `Dispatchers.Main`
- ✅ Использовать `Turbine` для тестирования StateFlow эмиссий
- ✅ Использовать `advanceUntilIdle()` для ожидания завершения корутин

**UI-тесты Compose компонентов:**

```kotlin
@Test
fun daysCountText_whenToday_thenShowsToday() {
    composeTestRule.setContent {
        DaysCountText(item)
    }
    composeTestRule.onNodeWithText("Сегодня").assertIsDisplayed()
}
```

- ✅ Тестируют UI компоненты в изоляции
- ✅ Используют Compose Testing
- ✅ Быстрые и надежные
- ✅ Не зависят от ViewModel

### Тестирование сетевых функций

- ✅ **Только на моках**: Использовать MockK для API клиентов (SWApi, Retrofit)
- ❌ **Без реальных запросов**: Запрещены реальные HTTP запросы к серверу в тестах
- ✅ **Изолированное тестирование**: Тестировать бизнес-логику независимо от сети
- ✅ **Предсказуемые ответы**: Использовать фиксированные mock-ответы от сервера

**Пример тестирования с моком:**

```kotlin
@Test
fun loadEvents_whenApiReturnsData_thenSuccess() {
    // Given
    val mockApi = mockk<SWApi>()
    coEvery { mockApi.getEvents() } returns mockEventResponse
    val repository = SWRepository(mockApi)

    // When
    val result = runBlocking { repository.getEvents() }

    // Then
    assertNotNull(result)
    assertEquals(2, result.size)
}
```

**Мокирование HttpException:**

```kotlin
@Test
fun getEvents_whenApiThrowsHttpException_thenThrowsHttpException() = runTest {
    // Given
    val mockResponse = mockk<Response<*>>(relaxed = true)
    every { mockResponse.code() } returns 404
    val mockApi = mockk<SWApi>()
    coEvery { mockApi.getEvents() } throws HttpException(mockResponse)

    // When/Then
    assertThrows<HttpException> { repository.getEvents() }
}
```

### Тестирование Flow с исключениями

**Важно:** Для Flow с исключениями, которые обрабатываются через `catch`, используйте `first()` или `collect()` вместо Turbine.

**Тестирование IOException (обрабатывается в catch):**

```kotlin
@Test
fun isAuthorized_whenIOExceptionOccurs_thenEmitsEmptyPreferences() = runTest {
    // Given
    val mockDataStore = mockk<DataStore<Preferences>>()
    every { mockDataStore.data } returns flow { throw IOException("Test error") }
    val repository = UserPreferencesRepository(mockDataStore)

    // When
    val result = repository.isAuthorized.first()

    // Then
    assertEquals(false, result)
}
```

**Тестирование других исключений (пробрасываются дальше):**

```kotlin
@Test
fun isAuthorized_whenOtherExceptionOccurs_thenThrowsException() = runTest {
    // Given
    val testException = RuntimeException("Test error")
    val mockDataStore = mockk<DataStore<Preferences>>()
    every { mockDataStore.data } returns flow { throw testException }
    val repository = UserPreferencesRepository(mockDataStore)

    // When/Then
    var exceptionThrown = false
    try {
        repository.isAuthorized.collect { }
    } catch (e: RuntimeException) {
        exceptionThrown = true
    }
    assertTrue(exceptionThrown)
}
```

**Мокирование Android Log:**

```kotlin
@Before
fun setup() {
    mockkStatic(Log::class)
    every { Log.e(any(), any(), any()) } returns 0
}

@After
fun tearDown() {
    unmockkAll()
}
```

### Общие практики

- Быстрые и независимые тесты
- Описательные имена
- Один тест - одна проверка
- Тестировать поведение, не реализацию
- Интеграционные тесты только для DAO и Repository
- Unit-тесты для ViewModels с моками
- Сетевые функции только на моках (без реальных запросов)
- UI-тесты для Compose компонентов без бизнес-логики
- Использовать JUnit 5 аннотации (`@Test`, `@Before`, `@After`)
- Использовать assertions JUnit 5 (`assertEquals`, `assertTrue`, `assertNull`)

## Стиль кода

### Kotlin

- Data классы для моделей
- **Безопасное разворачивание опционалов**: использовать `?`, `?:`, `let`, `checkNotNull` вместо `!!`
- Sealed классы для состояний/результатов
- Extension functions для читаемости

### Compose

- `State`/`MutableState` для UI состояния
- Однонаправленный поток данных (состояние flows down, события flows up)
- `ViewModel` для состояния UI
- `CompositionLocal` только для темы/глобальной конфигурации

```kotlin
@Preview
@Composable
fun ComponentPreview() {
    swParksTheme {
        Component()
    }
}
```

### Обработка ошибок

**В Use Cases** - использовать стандартный `Result<T>`:

```kotlin
suspend operator fun invoke(uri: Uri): Result<Int> =
    try {
        // логика
        Result.success(items.size)
    } catch (e: IOException) {
        Result.failure(BackupException("Не удалось экспортировать данные: ${e.message}", e))
    }
```

**Для UI состояний** - использовать sealed классы:

```kotlin
sealed class DetailScreenState {
    data object Loading : DetailScreenState()
    data class Success(val item: Item) : DetailScreenState()
    data class Error(val message: String) : DetailScreenState()
}
```

**Безопасное разворачивание опционалов**:

```kotlin
// ❌ Плохо: использовать !!
val itemId = savedStateHandle["itemId"]!!

// ✅ Хорошо: использовать checkNotNull с информативным сообщением
private val itemId: Long =
    checkNotNull(savedStateHandle["itemId"]) {
        "ItemId parameter is required"
    }

// ✅ Хорошо: использовать let для null-safe вызова
repository.getItemById(itemId)?.let { item ->
    // работаем с item
}

// ✅ Хорошо: использовать Elvis оператор
val icon = screen.icon ?: defaultIcon
```

### Навигация

```kotlin
sealed class Screen(
    val route: String,
    val icon: ImageVector? = null,
    val titleResId: Int? = null,
) {
    object Events : Screen(
        route = "events",
        icon = Icons.AutoMirrored.Filled.List,
        titleResId = R.string.events,
    )

    object ItemDetail : Screen(
        route = "item_detail/{itemId}",
    ) {
        fun createRoute(itemId: Long) = "item_detail/$itemId"
    }
}
```

### Именование

- Классы: `PascalCase`
- Функции/переменные: `camelCase`
- Константы: `UPPER_SNAKE_CASE`
- Пакеты: `lowercase.with.dots`

### Комментарии

- KDoc для публичных API
- Объяснять "почему", не "что"
- Логи на русском

### UI State

Использовать `data class` для сложных состояний, `sealed class` для состояний с несколькими вариантами:

```kotlin
// Простой state - data class
data class RootScreenState(
    val currentTab: Screen = Screen.Events,
    val tabs: List<Screen> = listOf(Screen.Events, Screen.More),
)

// Сложный state с вариантами - sealed class
sealed class DetailScreenState {
    data object Loading : DetailScreenState()
    data class Success(val item: Item) : DetailScreenState()
    data class Error(val message: String) : DetailScreenState()
}
```

## Качество кода

После изменений в коде нужно выполнять команду `make format`.

### Линтинг

- ktlint: `./gradlew ktlintCheck`, `./gradlew ktlintFormat`
- detekt: `./gradlew detekt` (конфиг: `config/detekt/detekt.yml`)

### Требования

- Все замечания устранены перед коммитом
- Автоисправления применяются регулярно
- Новый код не добавляет проблем
- Не использовать устаревшие API (deprecated) - заменять на актуальные альтернативы

### Стабильность

- **Краши запрещены**: приложение не должно крашиться, даже если функционал не реализован
- При невозможности выполнить операцию - логировать ошибку в консоль через `Log.e()`
- Использовать try-catch и безопасные методы для обработки исключений

### Документация

- KDoc для публичных API
- Осмысленные имена
- Комментарии для сложной логики
- Документация актуальна

## Локализация

### Языки

- Русский (ru) - основной
- Английский (en)

### Ресурсы строк

```xml
<!-- res/values/strings.xml -->
<string name="days_count_format">%d дней</string>

<!-- res/values-en/strings.xml -->
<string name="days_count_format">%d days</string>
```

### Множественное число

```xml
<plurals name="days_count">
    <item quantity="one">%d день</item>
    <item quantity="few">%d дня</item>
    <item quantity="many">%d дней</item>
    <item quantity="other">%d дней</item>
</plurals>
```

### Форматирование

- `String.format()` или `MessageFormat`
- `DateFormat` для дат с учетом локали
- Логи на русском

## Правила разработки Android SW Parks

### Терминология

#### Проекты

- **android** (или **андроид**, или **площадки***) - текущий проект **Jetpack-WorkoutApp**, Android-версия приложения (a.k.a swparks)
- **ios** (или **iOS**) - проект **SwiftUI-WorkoutApp**, iOS-версия приложения, которая уже полностью реализована и работает исправно
- **JetpackDays** (или **days**) - референсный проект для архитектуры, навигации и технических решений Android-разработки

#### Назначение

Android-приложение является портом iOS-приложения на платформу Android. Необходимо реализовать такой же набор функционала, как в iOS-версии.

Пошаговый план разработки находится в файле `docs/development-plan.md`

### Дополнительные правила и рекомендации

В проекте также содержатся важные файлы с правилами разработки в директории `.cursor/rules/`:

- `architecture.mdc` - архитектурные принципы приложения
- `code-quality.mdc` - требования к качеству кода
- `code-style.mdc` - стиль написания кода
- `localization.mdc` - правила локализации
- `overview.mdc` - общее описание проекта
- `tdd.mdc` - правила тестирования

## Правила TDD (Test-Driven Development)

### Порядок разработки

**Важно:** Строго соблюдать порядок:

**1.** Тесты → **2.** Логика → **3.** UI

**1.** Писать модульные тесты для бизнес-логики перед реализацией
**2.** Реализовывать функциональность слоя домена и данных перед UI
**3.** Проверять бизнес-логику тестами перед разработкой UI
**4.** Создавать Compose UI только после тестирования логики

### Пирамида тестирования

- Модульные тесты (70%)
- Интеграционные тесты (20%)
- UI тесты (10%)

### Именование тестов

```kotlin
@Test
fun functionName_whenCondition_thenExpectedResult() {
    // Given
    // When
    // Then
}
```

### Пример

```kotlin
class DaysCalculatorTest {
    @Test
    fun calculateDaysDifference_whenSameDay_thenReturnsZero() {
        // Given
        val date = LocalDate.now()

        // When
        val result = DaysCalculator.calculateDaysDifference(date, date)

        // Then
        assertEquals(0, result)
    }
}
```

### Цикл TDD

**1. Red**: Написать падающий тест
**2. Green**: Написать минимальный код для прохождения теста
**3. Refactor**: Улучшить код, сохраняя тесты зелеными

### Принципы

- Тесты пишутся первыми
- Один тест - одна проверка
- Тесты независимы друг от друга
- Тесты быстрые и читаемые

### Критерии приемки

- **Тесты проходят успешно** (unit-тесты и инструментальные тесты)
- **Проект собирается** без ошибок
- **Код соответствует** стандартам проекта

### Что тестировать

#### Обязательно

- **Бизнес-логика** в сервисах и репозиториях
- **ViewModels** с моками зависимостей
- **Утилиты** и вспомогательные функции
- **Валидация** пользовательского ввода

#### По необходимости

- **UI компоненты** (Compose тесты)
- **Навигация** между экранами
- **Интеграционные тесты** для критичных потоков

#### Не тестировать напрямую

- **View/Composable** функции (только через UI тесты)
- **Android системные компоненты** (Activity, Fragment)
- **Сторонние библиотеки**

### Интеграция с разработкой

#### Перед написанием кода

1. Определи, что нужно протестировать
2. Напиши тест, который описывает желаемое поведение
3. Убедись, что тест падает (красный)

#### Во время реализации

1. Реализуй минимальный код для прохождения теста
2. Запусти тесты, убедись что они проходят (зеленый)
3. При необходимости добавь дополнительные тесты

#### После реализации

1. Рефакторинг кода, сохраняя прохождение тестов
2. Убедись, что все тесты проходят
3. Проверь, что проект собирается без ошибок

**Важно:** Подробные правила тестирования см. в `.cursor/rules/tdd.mdc`

## Qwen Added Memories

- После написания кода выполняй команду make format для приведения кода к одному стилю, и чини новые предупреждения, которые показала эта команда
- Поддерживай в актуальном состоянии документацию по проекту в папке docs/, и когда планируешь реализацию любой задачи, проверяй, есть ли в этой папке план для этой задачи - если есть, то актуализируй этот план, а если нет - создай новый md-документ с пошаговым планом реализации этой задачи, прежде чем приступать к написанию кода
- Актуализируй файл QWEN.md при значительных изменениях в архитектуре, структуре проекта или ключевых компонентах
- Обрати внимание на файлы в .cursor/rules/ - они содержат важные правила разработки, которые могут быть полезны для понимания архитектуры
