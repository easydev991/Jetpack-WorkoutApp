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

- **Room Database**: `SWDatabase` - основная база данных приложения
- **Repository Implementations**:
  - `SWRepository` - основной репозиторий для работы с данными (площадки, мероприятия, пользователи и т.д.)
  - `UserPreferencesRepository` - хранение настроек (тема, язык, токен авторизации)
  - `SecureTokenRepository` - защищенное хранение токена с шифрованием
  - `CountriesRepository` - репозиторий для работы со странами и городами
- **API Client**: `SWApi` - HTTP клиент для запросов к серверу (Retrofit + Kotlinx Serialization)
- **Interceptors** (OkHttp):
  - `AuthInterceptor` - обработка ошибок 401 (не авторизован)
  - `RetryInterceptor` - автоматический повтор запросов при временных ошибках
  - `TokenInterceptor` - добавление токена авторизации в заголовки запросов
- **Crypto**: менеджеры шифрования (AES-128-GCM-HKDF через Google Tink)
- **Serialization**: сериализаторы для JSON и шифрования
- **DateTime**: десериализатор для различных форматов дат
- **Error Handling**: `APIError`, `ErrorResponse`, `StatusCodeGroup`, `NetworkUtils`, `TokenEncoder`

#### Domain

- **Use Cases**: бизнес-логика, один use case - одна ответственность:
  - Авторизация: `LoginUseCase`, `LogoutUseCase`, `ResetPasswordUseCase`
  - Справочник: `GetCountriesUseCase`, `GetCitiesByCountryUseCase`, `GetCountryByIdUseCase`, `GetCityByIdUseCase`
  - Настройки: `IconManager` - управление иконками приложения (11 иконок)
- **Models**: доменные модели (`AppIcon`, `AppTheme` - настройки приложения)
- **Exceptions**: исключения доменного слоя (`NetworkException`, `ServerException`)
- **Repository Interfaces**: интерфейсы репозиториев (например, `CountriesRepository`)

#### Presentation

- **ViewModels**: `AuthViewModel`, `LoginViewModel`, `ProfileViewModel`, `EventsViewModel`, `ThemeIconViewModel`, `MainActivityViewModel`, `FriendsListViewModel` и др.
- **UI State**: sealed классы (Loading/Success/Error) для состояний экранов
- **Screens**: Compose экраны в `ui/screens/`:
  - `auth/` - экраны авторизации (LoginScreen, RecoveryScreen)
  - `profile/` - экраны профиля (ProfileScreen, EditProfileScreen, FriendsScreen и т.д.)
  - `parks/` - экраны площадок (ParksListScreen, ParksMapScreen, ParkDetailScreen и т.д.)
  - `events/` - экраны мероприятий (EventsScreen, EventDetailScreen, EditEventScreen)
  - `messages/` - экраны сообщений (DialogsListScreen, ChatScreen)
  - `journals/` - экраны дневников (JournalsListScreen, JournalDetailScreen и т.д.)
  - `settings/` - экраны настроек (MoreScreen, ThemeSettingsScreen, AppDataScreen и т.д.)
  - `RootScreen` - корневой экран с навигацией по вкладкам
- **Navigation**: навигация в `navigation/` (`AppState.kt`, `Destinations.kt`, `Navigation.kt`, `TopLevelDestination.kt`)
- **Theme**: тема в `ui/theme/`
- **Design System**: компоненты дизайн-системы в `ui/ds/` (28 компонентов)

#### Utilities

- **Logger**: интерфейс для логирования (`AndroidLogger`, `NoOpLogger`)
- **ErrorReporter**: интерфейс для отчетов об ошибках (`ErrorHandler`)
- **AppConstants**: константы приложения
- `setValueIfChanged` - утилита для обновления State с проверкой изменения значения

### Отличия от nowinandroid

#### Сетевые функции

- **SWApi** - HTTP клиент для работы с сервером workout.su (отсутствует в nowinandroid)
- **Retrofit + Kotlinx Serialization** - для API запросов (отсутствует в nowinandroid)
- **Авторизация** - Login/Logout с токеном авторизации (отсутствует в nowinandroid)

#### Масштаб проекта

- **Больше экранов** - ~30+ экранов vs ~7 в nowinandroid
- **Больше моделей** - 30+ моделей vs ~7 в nowinandroid
- **Сложные бизнес-потоки** - авторизация, друзья, сообщения, дневники (отсутствуют в nowinandroid)

### Референсы

#### Архитектура и структура

- **nowinandroid** - референсный проект для базовой архитектуры (MVVM, Clean Architecture, ручной DI)
- Подход к слоистой архитектуре идентичен nowinandroid
- Dependency Injection через factory методы (без Hilt) - как в nowinandroid

#### Функционал и экраны

- **SwiftUI-WorkoutApp** - iOS-приложение - эталон функциональности и дизайна
- При разработке экранов ориентироваться на iOS-версию для консистентности UI и UX

#### Компоненты дизайн-системы

- ✅ Компоненты дизайн-системы уже реализованы и готовы к использованию (28 компонентов)
- Использовать готовые компоненты для верстки экранов

## Структура проекта

```
app/
├── src/main/
│   ├── java/com/swparks/
│   │   ├── data/           # Слой данных (репозитории, источники данных)
│   │   │   ├── crypto/     # Шифрование
│   │   │   ├── database/   # Room Database
│   │   │   ├── datetime/   # Десериализация дат
│   │   │   ├── interceptor/# Interceptors
│   │   │   ├── preferences/# DataStore preferences
│   │   │   ├── repository/ # Реализации репозиториев
│   │   │   ├── serialization/# Сериализаторы для JSON
│   │   │   └── serializer/ # Сериализатор для шифрования
│   │   ├── domain/         # Domain layer
│   │   │   ├── exception/  # Исключения
│   │   │   ├── model/      # Доменные модели
│   │   │   ├── repository/ # Интерфейсы репозиториев
│   │   │   └── usecase/    # Use cases
│   │   ├── model/          # Модели данных (30+ моделей)
│   │   ├── navigation/     # Навигация
│   │   ├── network/        # Сетевой слой (SWApi - 57 endpoints)
│   │   ├── ui/             # UI слой
│   │   │   ├── ds/         # Компоненты пользовательского интерфейса (28 компонентов)
│   │   │   ├── screen/     # Компоненты экранов
│   │   │   ├── screens/    # Экраны приложения
│   │   │   ├── state/      # Состояния UI компонентов
│   │   │   ├── theme/      # Тема приложения
│   │   │   └── viewmodel/  # ViewModel классы
│   │   ├── util/           # Утилиты
│   │   ├── utils/          # Дополнительные утилиты
│   │   ├── viewmodel/      # ViewModels верхнего уровня
│   │   ├── JetpackWorkoutApplication.kt
│   │   └── MainActivity.kt
│   ├── res/                # Ресурсы
│   │   ├── values/         # Строковые ресурсы (английский)
│   │   ├── values-ru/      # Строковые ресурсы (русский)
│   │   ├── drawable/       # Изображения
│   │   ├── mipmap-*/       # Иконки приложения (11 наборов)
│   │   └── xml/            # XML конфигурации
│   └── AndroidManifest.xml
```

## Зависимости

Основные зависимости проекта:

- `androidx.core:core-ktx:1.17.0`
- `androidx.lifecycle:lifecycle-runtime-ktx:2.10.0`
- `androidx.activity:activity-compose:1.12.2`
- `androidx.compose:*:compose-bom:2026.01.00` (Material 3)
- `androidx.appcompat:appcompat:1.7.1`
- `androidx.compose.material:material-icons-extended:1.7.8`
- `androidx.navigation:navigation-compose:2.9.6`
- `com.squareup.retrofit2:retrofit:3.0.0`
- `org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0`
- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2`
- `io.coil-kt:coil-compose:2.7.0`
- `androidx.datastore:datastore-preferences:1.2.0`
- `androidx.room:room-runtime:2.8.4`
- `androidx.room:room-ktx:2.8.4`
- `androidx.room:room-compiler:2.8.4` (KSP)
- `com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0`
- `com.google.crypto.tink:tink-android:1.20.0`
- `com.google.devtools.ksp:2.3.2`

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

## Безопасность токена авторизации

Токен авторизации хранится безопасно с использованием следующих механизмов:

- **Шифрование**: AES-128-GCM-HKDF через Google Tink библиотеку
- **CryptoManager**: интерфейс и реализация для операций шифрования/дешифрования
- **EncryptedStringSerializer**: сериализатор для автоматического шифрования/дешифрования строк в DataStore
- **SecureTokenRepository**: репозиторий для безопасного хранения и получения токена
- **TokenInterceptor**: автоматически добавляет токен в заголовки всех запросов к API
- **AuthInterceptor**: обрабатывает ошибки 401 и очищает токен при необходимости
- **RetryInterceptor**: автоматически повторяет запросы при временных сетевых ошибках

## Смена иконки приложения

Приложение поддерживает 11 разных иконок для персонализации:

- Реализовано через Activity Aliases в AndroidManifest.xml
- Каждая иконка имеет отдельный alias: `MainActivityAliasIcon1`...`MainActivityAliasIcon11`
- Иконки хранятся в `mipmap-*/ic_launcher_1`...`ic_launcher_11`
- Пользователь может выбрать иконку через `ThemeIconScreen`
- Используются PNG и adaptive icons для разных размеров экранов
- Для Android 12+ поддерживается динамическая цветовая тема

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
- `CAMERA` (опционально) - для съемки фотографий
- `ACCESS_NETWORK_STATE` - для проверки состояния сети
- `ACCESS_WIFI_STATE` - для проверки состояния Wi-Fi
- `ACCESS_FINE_LOCATION` и `ACCESS_COARSE_LOCATION` - для определения местоположения
- `READ_MEDIA_IMAGES` - для доступа к медиафайлам

## Сборка и разработка

Для сборки проекта используется Gradle с Kotlin DSL. Основные команды:

- `./gradlew build` - полная сборка проекта
- `./gradlew assembleDebug` - сборка отладочной версии
- `./gradlew installDebug` - установка отладочной версии на устройство

## Тестирование

### Инструменты

- JUnit 5 - unit-тесты
- MockK - мокирование
- Compose Testing - Compose компоненты
- Room Testing - для интеграционных тестов БД
- kotlinx-coroutines-test - для тестирования корутин
- Turbine - для тестирования Flow/StateFlow (app.cash.turbine:turbine:1.2.1)

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

### Подходы к тестированию

#### Unit-тесты ViewModels (с MockK)

- Мокировать зависимости с помощью MockK
- Тестировать логику изолированно от внешних зависимостей
- Проверять состояния UI после вызова методов ViewModel

#### Интеграционные тесты DAO и Repository

- Использовать `runBlocking` для блокирующих операций
- Прямые вызовы DAO/Repository
- Тестировать взаимодействие с реальной БД

#### Интеграционные тесты ViewModels (только для существующих)

- Использовать `runTest` вместо `runBlocking`
- Использовать `MainDispatcherRule` для замены `Dispatchers.Main`
- Использовать `Turbine` для тестирования StateFlow эмиссий
- Использовать `advanceUntilIdle()` для ожидания завершения корутин

#### UI-тесты Compose компонентов

- Тестируют UI компоненты в изоляции
- Используют Compose Testing
- Быстрые и надежные
- Не зависят от ViewModel

### Тестирование сетевых функций

- ✅ **Только на моках**: Использовать MockK для API клиентов (SWApi, Retrofit)
- ❌ **Без реальных запросов**: Запрещены реальные HTTP запросы к серверу в тестах
- ✅ **Изолированное тестирование**: Тестировать бизнес-логику независимо от сети
- ✅ **Предсказуемые ответы**: Использовать фиксированные mock-ответы от сервера

### Тестирование Flow с исключениями

**Важно:** Для Flow с исключениями, которые обрабатываются через `catch`, используйте `first()` или `collect()` вместо Turbine.

- **IOException** (обрабатывается в catch) - использовать `first()` для получения результата
- **Другие исключения** (пробрасываются дальше) - использовать `collect()` и перехватывать исключения

### Мокирование Android Log

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

**Подробные правила тестирования см. в `.cursor/rules/tdd.mdc`**

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

### Обработка ошибок

**В Use Cases** - использовать стандартный `Result<T>` для возврата результата
**Для UI состояний** - использовать sealed классы (Loading/Success/Error)

**Безопасное разворачивание опционалов:**
- ❌ Плохо: использовать `!!`
- ✅ Хорошо: использовать `checkNotNull` с информативным сообщением
- ✅ Хорошо: использовать `let` для null-safe вызова
- ✅ Хорошо: использовать Elvis оператор `?:`

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

- Использовать `data class` для простых состояний
- Использовать `sealed class` для состояний с несколькими вариантами (Loading, Success, Error)

**Подробные правила стиля кода см. в `.cursor/rules/code-style.mdc`**

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

**Подробные требования к качеству кода см. в `.cursor/rules/code-quality.mdc`**

## Локализация

### Языки

- Русский (ru) - основной
- Английский (en) - по умолчанию

**Подробные правила локализации см. в `.cursor/rules/localization.mdc`**

## Правила разработки Android SW Parks

### Терминология

#### Проекты

- **android** (или **андроид**, или **площадки**) - текущий проект **Jetpack-WorkoutApp**, Android-версия приложения (a.k.a swparks)
- **ios** (или **iOS**) - проект **SwiftUI-WorkoutApp**, iOS-версия приложения, которая уже полностью реализована и работает исправно
- **nowinandroid** (или **nia**) - референсный проект для архитектуры, навигации и технических решений Android-разработки

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

### Правила TDD (Test-Driven Development)

#### Порядок разработки

**Важно:** Строго соблюдать порядок:
**1.** Тесты → **2.** Логика → **3.** UI

1. Писать модульные тесты для бизнес-логики перед реализацией
2. Реализовывать функциональность слоя домена и данных перед UI
3. Проверять бизнес-логику тестами перед разработкой UI
4. Создавать Compose UI только после тестирования логики

#### Пирамида тестирования

- Модульные тесты (70%)
- Интеграционные тесты (20%)
- UI тесты (10%)

#### Цикл TDD

**1. Red**: Написать падающий тест
**2. Green**: Написать минимальный код для прохождения теста
**3. Refactor**: Улучшить код, сохраняя тесты зелеными

#### Критерии приемки

- **Тесты проходят успешно** (unit-тесты и инструментальные тесты)
- **Проект собирается** без ошибок
- **Код соответствует** стандартам проекта

**Подробные правила TDD см. в `.cursor/rules/tdd.mdc`**

## Qwen Added Memories

- После написания кода выполняй команду make format для приведения кода к одному стилю, и чини новые предупреждения, которые показала эта команда
- Поддерживай в актуальном состоянии документацию по проекту в папке docs/, и когда планируешь реализацию любой задачи, проверяй, есть ли в этой папке план для этой задачи - если есть, то актуализируй этот план, а если нет - создай новый md-документ с пошаговым планом реализации этой задачи, прежде чем приступать к написанию кода
- Актуализируй файл QWEN.md при значительных изменениях в архитектуре, структуре проекта или ключевых компонентах
- Обрати внимание на файлы в .cursor/rules/ - они содержат важные правила разработки, которые могут быть полезны для понимания архитектуры
