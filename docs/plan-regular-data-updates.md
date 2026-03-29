# План: Регулярное обновление парков и справочников

Реализация автоматической проверки и обновления данных о парках и странах/городах по аналогии с iOS версией.

## Контекст

**iOS реализация (эталон):**
- `lastParksUpdateDateString` в UserDefaults, дефолт `"2025-10-25T00:00:00"`
- `lastCountriesUpdateDate` в UserDefaults
- Проверка: если >1 дня с последнего обновления → обновить с сервера
- После успешного обновления: сохранить текущую дату

**Android текущее состояние:**
- `SWRepository.getUpdatedParks(date: String)` уже существует
- `CountriesRepositoryImpl.updateCountriesFromServer()` уже существует
- Нет хранения дат последнего обновления
- Нет автоматических проверок при запуске

## Стратегия сохранения

### Общий принцип

Для Android-приложения используется гибридная стратегия хранения:

- **parks** сохраняются в **Room** как основной локальный источник данных для UI;
- **countries/cities** сохраняются в **локальном JSON-файле** в app-private storage;
- **даты последней синхронизации** сохраняются в **DataStore** через `UserPreferencesRepository`.

Такой подход лучше соответствует текущей Android-архитектуре проекта:
- `Room` подходит для offline-first работы со списком parks, который регулярно обновляется delta-изменениями с сервера;
- локальный JSON-файл подходит для полного справочника стран и городов, который обновляется целиком и должен сохраняться между перезапусками приложения;
- `assets` используются только как начальный seed и **никогда не обновляются** во время работы приложения.

### Сохранение parks

- Исходный bundled JSON с parks используется только как начальный seed.
- При первом запуске seed-данные импортируются в локальное хранилище приложения.
- Основной источник parks для UI после этого — **Room**, а не исходный JSON.
- Регулярное обновление parks выполняется через `getUpdatedParks(date)`, который возвращает только изменённые parks.
- Полученные delta-изменения мержатся в Room по id.
- После успешного merge обновляется `lastParksUpdateDate`.

### Сохранение countries/cities

- Исходный bundled JSON со справочником стран и городов используется только как начальный seed.
- При первом запуске seed-файл копируется в app-private storage приложения.
- Основной источник countries/cities для приложения после этого — **локальный JSON-файл в памяти приложения**, а не `assets`.
- При регулярном обновлении с сервера загружается **полный справочник**.
- После успешной загрузки новый JSON атомарно заменяет текущий локальный файл.
- Только после успешной записи файла обновляется `lastCountriesUpdateDate`.

### Требования к персистентности

- И parks, и countries/cities должны быть доступны **офлайн**.
- И parks, и countries/cities должны сохраняться между перезапусками приложения.
- Потеря данных при перезапуске приложения недопустима.
- Ошибка синхронизации не должна удалять или повреждать ранее успешно сохранённые локальные данные.

## Зависимости

```
[Clock] ─────────────────────────────────────────────────────────┐
   │                                                         │
   ↓                                                         ↓
[SyncDateUtils.isUpdateNeeded()]                    [SyncParksUseCase]
                                                          │
[UserPreferencesRepository] ←────────────────────────────┤
         ↓                                              ↓
[DataStore dates]                              [SWRepository.getUpdatedParks]
                                                          │
[SyncCountriesUseCase] ← [CountriesRepository]          │
         ↓                                               ↓
[AppForegroundSyncHandler (RootScreen - ON_START)]       ↓
                    ──────────────────────────────────────┘
```

**Важно:**
- Синхронизация стран вызывается при `ON_START` lifecycle event в RootScreen через `AppForegroundSyncHandler`,
  аналогично iOS `scenePhase == .active`. НЕ использовать `init` в ViewModel!
- `Clock` — интерфейс для детерминированных тестов. `SystemClock` в продакшене, `TestClock` в тестах.
- Parks синхронизация ожидает реализации Этапа 6 (интеграция с ParksRootViewModel).

## Этап 1: Domain Layer (Clock + Extensions)

**Проблема:** Тесты с `System.currentTimeMillis()` хрупкие и недетерминированные.
**Решение:** Clock abstraction для контроля времени в тестах.

- [x] Создать интерфейс `Clock` в `domain/util/Clock.kt`

   ```kotlin
   interface Clock {
       fun now(): Instant
       fun nowIsoString(): String
   }
   ```

- [x] Создать `SystemClock` реализацию в `data/util/SystemClock.kt`

   ```kotlin
   class SystemClock : Clock {
       override fun now(): Instant = Instant.now()
       // форматирование в ISO 8601
   }
   ```

- [x] Написать тесты для `TestClock` (фиксированное время)
  - Тест: now() возвращает фиксированное время
  - Тест: nowIsoString() корректный формат
- [x] Создать `TestClock` для тестов в `test/` (не в main)

   ```kotlin
   class TestClock(private val fixedInstant: Instant) : Clock { ... }
   ```

- [x] Написать тесты для `isUpdateNeeded()` с `TestClock`
  - Тест: <1 дня → false
  - Тест: >1 дня → true
  - Тест: null дата → true
  - Тест: дефолтная дата "2025-10-25T00:00:00" → true
  - Тест: битая ISO-строка → true (приложение не падает, безопаснее считать что синк нужен)
- [x] Реализовать extension функцию `String?.isUpdateNeeded(clock: Clock): Boolean`

   ```kotlin
   fun String?.isUpdateNeeded(clock: Clock): Boolean {
       if (this == null) return true
       val lastUpdate = runCatching { Instant.parse(this) }.getOrNull()
           ?: return true // битая строка → true (fail-safe)
       val now = clock.now()
       // сравнение: если >1 дня → true
   }
   ```

## Этап 2: Data Layer (Preferences)

- [x] Написать тесты для `UserPreferencesRepository` новые методы
  - [x] Тест сохранения `lastParksUpdateDate`
  - [x] Тест сохранения `lastCountriesUpdateDate`
  - [x] Тест чтения с дефолтными значениями (`"2025-10-25T00:00:00"`)
  - [x] Тест Flow реактивности
- [x] Добавить в `UserPreferencesRepository`:

```kotlin
private companion object {
    val last_parks_update_date = stringPreferencesKey("lastParksUpdateDate")
    val last_countries_update_date = stringPreferencesKey("lastCountriesUpdateDate")
}

val lastParksUpdateDate: Flow<String> = dataStore.data
    .catch { _->
        emit(emptyPreferences())
    }
    .map { it[last_parks_update_date] ?: "2025-10-25T00:00:00" }
    .distinctUntilChanged()

val lastCountriesUpdateDate: Flow<String?> = dataStore.data
    .catch { _ ->
        emit(emptyPreferences())
    }
    .map { it[last_countries_update_date] }
    .distinctUntilChanged()

suspend fun setLastParksUpdateDate(date: String)
suspend fun setLastCountriesUpdateDate(date: String)
```

**Примечание:** `.catch { emit(emptyPreferences()) }` — стандартный паттерн DataStore Preferences для обработки IOException. Не использовать `emit(exception)` — тип не совпадёт.

**Примечание по тестированию сеттеров (MockK):** `verify { mockDataStore.edit(any()) }` и `coVerify { mockDataStore.edit(any()) }` не работают для extension function `edit()` — ошибка "Unresolved reference 'edit'". Обходной путь: не проверять вызов явно, а полагаться на `relaxed = true` у mock, чтобы функция выполнялась без ошибок. Это приемлемое ограничение, так как suspend-функции — тонкие обёртки над `dataStore.edit {}`.

## Этап 3: Data Layer (Local storage for parks and countries)

**Цель:** до реализации Use Case явно покрыть тестами и реализовать локальное хранение данных согласно выбранной стратегии:
- parks → Room
- countries/cities → локальный JSON-файл в app-private storage

### 3.1 Parks local storage (Room)

- [x] Написать тесты для локального хранения parks
  - Тест: seed parks импортируются в Room при первой инициализации
  - Тест: повторная инициализация не дублирует parks в Room
  - Тест: delta-обновление parks делает upsert по id
  - Тест: существующий park обновляется при совпадении id
  - Тест: новый park добавляется при новом id

- [x] Реализовать локальное хранение parks в Room
  - Определить основной локальный источник данных для parks как Room
  - Использовать bundled JSON с parks только как seed
  - При первом запуске импортировать seed parks в Room
  - Реализовать upsert delta-изменений parks в Room по id
  - Не изменять исходный bundled JSON во время работы приложения

**Файлы:**
- `app/src/main/java/com/swparks/data/database/entity/ParkEntity.kt` — Room entity
- `app/src/main/java/com/swparks/data/database/dao/ParkDao.kt` — DAO с insertAll (REPLACE), getAllParks, getParkById, isEmpty
- `app/src/main/java/com/swparks/data/database/SWDatabase.kt` — добавлен ParkEntity, version 4, parkDao()
- `app/src/main/java/com/swparks/data/AppContainer.kt` — добавлен parkDao
- `app/src/main/java/com/swparks/data/repository/SWRepository.kt` — добавлены getParksFlow(), importSeedParks(), upsertParks()

### 3.2 Countries/cities local storage (JSON in app-private storage)

- [x] Написать тесты для локального хранения countries/cities
  - Тест: при отсутствии локального файла seed JSON копируется в app-private storage
  - Тест: при наличии локального файла seed повторно не копируется
  - Тест: чтение countries/cities идёт из локального JSON-файла
  - Тест: полный справочник с сервера атомарно заменяет текущий локальный JSON
  - Тест: при ошибке записи старый локальный JSON не теряется

- [x] Реализовать локальное хранение countries/cities в app-private storage
  - Определить основной локальный источник данных для countries/cities как JSON-файл в app-private storage
  - Использовать bundled JSON со справочником только как seed
  - При первом запуске копировать seed-файл в app-private storage
  - Реализовать чтение countries/cities из локального JSON-файла
  - Реализовать атомарную замену локального JSON после успешной загрузки полного справочника с сервера
  - Не изменять исходный bundled JSON во время работы приложения

**Файлы:**
- `app/src/main/java/com/swparks/data/repository/CountriesRepositoryImpl.kt` — обновлён: copySeedFromAssets(), loadCountriesFromLocalFile(), saveCountriesToLocalFile() с атомарной записью

### 3.3 Интеграция repository

- [x] Обновить `SWRepository`
  - Использовать Room как локальный источник parks
  - Добавлены методы для seed-инициализации parks
  - Добавлены методы для upsert delta-обновлений parks

- [x] Обновить `CountriesRepository`
  - Использовать локальный JSON-файл как основной источник countries/cities
  - Добавлены методы для seed-инициализации JSON-файла
  - Добавлены методы для атомарного обновления полного справочника

**Важно:** только после завершения этого этапа переходить к Use Case, так как `SyncParksUseCase` и `SyncCountriesUseCase` должны опираться на уже реализованную и протестированную локальную стратегию хранения.

## Этап 4: Domain Layer (Use Cases)

**Важно:** Use Cases принимают `Clock` в конструкторе для тестируемости.

**Предусловие:** локальная стратегия хранения уже реализована на предыдущем этапе:
- parks читаются из Room и обновляются через upsert delta-изменений;
- countries/cities читаются из локального JSON-файла и обновляются полной атомарной заменой файла.

- [x] Написать тесты для `SyncParksUseCase` с `TestClock` (7 тестов)
  - Тест: needUpdate = false → не обновлять
  - Тест: needUpdate = true → вызвать getUpdatedParks
  - Тест: успешное обновление (непустой список) → сохранить дату через `clock.nowIsoString()`
  - Тест: успешное обновление (пустой список) → сохранить дату
  - Тест: ошибка сети → не обновлять дату
  - Тест: parks не обновляются когда needUpdate = true но уже идёт загрузка
  - Тест:parks обновляются при needUpdate = true даже если isLoading = false

- [x] Создать `SyncParksUseCase(clock: Clock, userPreferencesRepository: UserPreferencesRepository, swRepository: SWRepository)`
  - **Источник даты:** только `lastParksUpdateDate` из `UserPreferencesRepository`. UseCase никогда не вызывает обновление parks без даты.
  - Проверить `lastParksUpdateDate.isUpdateNeeded(clock)`
  - Вызвать `SWRepository.getUpdatedParks(date)` (date всегда непустая, дефолт "2025-10-25T00:00:00")
  - **При успешном ответе:** `SWRepository` получает delta-изменения parks и делает upsert в Room по id, после чего `SyncParksUseCase` обновляет `lastParksUpdateDate` на `clock.nowIsoString()`
  - При ошибке: не обновлять дату
- [x] Написать тесты для `SyncCountriesUseCase` с `TestClock` (6 тестов)
  - Тест: needUpdate = false → не обновлять
  - Тест: needUpdate = true → вызвать updateCountriesFromServer
  - Тест: успешное обновление → сохранить дату через `clock.nowIsoString()`
  - Тест: ошибка сети → не обновлять дату
  - Тест: countries не обновляются когда needUpdate = true но уже идёт загрузка
  - Тест: countries обновляются при needUpdate = true даже если isLoading = false

- [x] Создать `SyncCountriesUseCase(clock: Clock, userPreferencesRepository: UserPreferencesRepository, countriesRepository: CountriesRepository)`
  - Проверить `lastCountriesUpdateDate.isUpdateNeeded(clock)`
  - Вызвать `CountriesRepository.updateCountriesFromServer()` (полный справочник, без передачи даты на сервер)
  - **При успешном ответе:** `CountriesRepository` получает полный справочник стран и городов, сериализует его в JSON и атомарно заменяет текущий локальный файл в app-private storage, после чего `SyncCountriesUseCase` обновляет `lastCountriesUpdateDate` на `clock.nowIsoString()`
  - При ошибке: не обновлять дату
 **Примечание:** countries обновляются полностью (в отличие от parks с delta), поэтому дата обновляется только после успешного сохранения.
- [x] Добавить `SystemClock` и Use Cases в `AppContainer`

**Реализованные файлы:**
- `app/src/main/java/com/swparks/domain/usecase/SyncParksUseCase.kt`
- `app/src/main/java/com/swparks/domain/usecase/SyncCountriesUseCase.kt`
- `app/src/test/java/com/swparks/domain/usecase/SyncParksUseCaseTest.kt`
- `app/src/test/java/com/swparks/domain/usecase/SyncCountriesUseCaseTest.kt`
- `app/src/main/java/com/swparks/data/AppContainer.kt` (обновлён: добавлены clock, syncParksUseCase, syncCountriesUseCase)
- `app/src/main/java/com/swparks/data/DefaultAppContainer.kt` (обновлён: создан SystemClock и экземпляры Use Cases)

## Этап 5: UI Layer (RootScreen - Lifecycle)

**Важно:** Синхронизация стран при ON_START lifecycle event, аналогично iOS `scenePhase == .active`.

- [x] Написать тесты для `AppForegroundSyncHandler`
  - Тест: ON_START → вызывается syncCountries
  - Тест: другие события → не вызывается
  - Тест: ошибка sync не блокирует UI
  - Тест: повторный ON_START → проверяется needUpdate
- [x] Создать `AppForegroundSyncHandler` composable в отдельном файле `ui/screens/AppForegroundSyncHandler.kt`

   ```kotlin
   @Composable
   fun AppForegroundSyncHandler(syncCountriesUseCase: SyncCountriesUseCase) {
       val lifecycleOwner = LocalLifecycleOwner.current
       DisposableEffect(lifecycleOwner) {
           val observer = LifecycleEventObserver { _, event ->
               if (event == Lifecycle.Event.ON_START) {
                   // Запуск корутины для syncCountries
               }
           }
           lifecycleOwner.lifecycle.addObserver(observer)
           onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
       }
   }
   ```

- [x] Добавить `SyncCountriesUseCase` в `RootScreen` (получить из `AppContainer`)
- [x] Вызвать `AppForegroundSyncHandler` в начале `RootScreen`

**Реализованные файлы:**
- `app/src/main/java/com/swparks/ui/screens/AppForegroundSyncHandler.kt`
- `app/src/test/java/com/swparks/ui/screens/AppForegroundSyncHandlerTest.kt`
- `app/src/main/java/com/swparks/ui/screens/RootScreen.kt` (обновлён: добавлен вызов `AppForegroundSyncHandler`)

## Этап 6: UI Layer (ParksRootViewModel)

- [x] Написать тесты для `ParksRootViewModel` обновления
  - Тест: при загрузке проверяется needUpdate
  - Тест: если parks уже загружены и не нужен refresh → не обновлять
  - Тест: если needUpdate → вызвать syncParksUseCase
  - Тест: принудительный refresh → обновить независимо от даты
- [x] Обновить `ParksRootViewModel`
  - Добавить `SyncParksUseCase` в конструктор
  - Подписаться на локальный источник parks через `SWRepository.getParksFlow()`
  - Импортировать seed parks в Room при первой инициализации
  - Проверить needUpdate при первом входе на экран
- [x] Обновить `ParksRootScreen`
  - Обработать pull-to-refresh с принудительным обновлением

**Реализованные файлы:**
- `app/src/main/java/com/swparks/ui/viewmodel/ParksRootViewModel.kt`
- `app/src/test/java/com/swparks/ui/viewmodel/ParksRootViewModelTest.kt`
- `app/src/main/java/com/swparks/ui/screens/parks/ParksRootScreen.kt`
- `app/src/main/java/com/swparks/ui/screens/RootScreen.kt` (обновлён: parks больше не читаются напрямую из `assets` для ParksRootScreen)

## Этап 6.1: Refactor (InitializeParksUseCase)

**Цель:** убрать `Context` из `ParksRootViewModel` и вынести bootstrap/seed-инициализацию parks в отдельную абстракцию.

- [x] Создать `InitializeParksUseCase`
  - Спрятать внутри него platform-зависимость от `Context`
  - Вынести в него seed-инициализацию parks (`importSeedParks`)
  - Оставить `ParksRootViewModel` только orchestration-логику без прямой зависимости от `Context`

- [x] Обновить `ParksRootViewModel`
  - Удалить `private val appContext: Context` из конструктора
  - Использовать `InitializeParksUseCase` вместо прямого вызова `swRepository.importSeedParks(appContext)`
  - Сохранить текущую подписку на `SWRepository.getParksFlow()` и pull-to-refresh через `SyncParksUseCase`

- [x] Обновить DI / AppContainer
  - Создать экземпляр `InitializeParksUseCase`
  - Передать его в `ParksRootViewModel`

- [x] Обновить тесты
  - Заменить проверки прямого вызова `swRepository.importSeedParks(...)` на проверки `InitializeParksUseCase`
  - При необходимости скорректировать тестовые фикстуры `ParksRootViewModelTest`
  - Убедиться, что сценарии init, Room flow и pull-to-refresh остаются покрыты

## Этап 7: Интеграция и тестирование

- [x] Обновить `AppContainer.kt`
  - Создать `SystemClock` (singleton)
  - Создать `SyncParksUseCase(clock, ...)`
  - Создать `SyncCountriesUseCase(clock, ...)`
  - Передать `SyncCountriesUseCase` в `RootScreen`
- [ ] Добавить логирование (на русском)
  - "Проверка необходимости обновления парков"
  - "Обновление парков с сервера"
  - "Ошибка обновления парков"
  - Аналогично для стран
- [ ] *(Опционально)* Написать интеграционные тесты
  - Тест полного цикла обновления парков
  - Тест полного цикла обновления стран
- [ ] Проверить работу вручную при:
  - Первом запуске (дефолтная дата → обновление)
  - Повторном запуске <1 дня (нет обновления)
  - Повторном запуске >1 дня (обновление)
  - Отсутствии сети (обработка ошибки)

## Критерии завершения

**Countries (триггер: ON_START / возврат в foreground):**
- [ ] При первом запуске countries/cities доступны из локального seed-файла
- [ ] При необходимости синхронизации выполняется обновление полного справочника с сервера
- [ ] При возврате в приложение <1 дня — данные берутся из кэша
- [ ] При возврате в приложение >1 дня — countries обновляются с сервера
- [ ] Countries/cities доступны офлайн и не теряются после перезапуска приложения
- [ ] При отсутствии локального countries JSON seed-файл копируется в app-private storage один раз

**Parks (триггер: вход на экран parks):**
- [x] При первом входе на экран parks загружаются из локального источника данных (Room)
- [x] Parks доступны офлайн и не теряются после перезапуска приложения
- [x] При первом заполнении локального хранилища seed parks импортируются в Room один раз без дублирования
- [x] При устаревшей дате синхронизации (>1 дня) — дополнительно выполняется delta sync с сервера
- [x] При повторном входе <1 дня — данные берутся из кэша без запроса к серверу
- [x] Pull-to-refresh работает с принудительным обновлением

**Общее:**
- [ ] Ошибки сети не блокируют работу приложения
- [x] `make format` без ошибок
- [x] `make lint` без ошибок
- [x] `make test` без ошибок
- [ ] *(Если добавлены UI/instrumentation тесты)* `make android-test` без ошибок

## Файлы для изменения

### Новые файлы

- [x] `app/src/main/java/com/swparks/domain/util/Clock.kt` ✅
- [x] `app/src/main/java/com/swparks/domain/util/SyncDateUtils.kt` ✅ (isUpdateNeeded extension)
- [x] `app/src/main/java/com/swparks/data/util/SystemClock.kt` ✅
- [x] `app/src/main/java/com/swparks/domain/usecase/SyncParksUseCase.kt` ✅
- [x] `app/src/main/java/com/swparks/domain/usecase/SyncCountriesUseCase.kt` ✅
- [x] `app/src/main/java/com/swparks/ui/screens/AppForegroundSyncHandler.kt` ✅
- [x] `app/src/test/java/com/swparks/domain/util/TestClock.kt` ✅
- [x] `app/src/test/java/com/swparks/domain/util/SyncDateUtilsTest.kt` ✅
- [x] `app/src/test/java/com/swparks/domain/usecase/SyncParksUseCaseTest.kt` ✅
- [x] `app/src/test/java/com/swparks/domain/usecase/SyncCountriesUseCaseTest.kt` ✅
- [x] `app/src/test/java/com/swparks/ui/screens/AppForegroundSyncHandlerTest.kt` ✅
- [ ] `app/src/test/java/com/swparks/data/repository/SWRepositoryParksSyncTest.kt`
- [ ] `app/src/test/java/com/swparks/data/repository/CountriesRepositoryStorageTest.kt`
- [x] `app/src/test/java/com/swparks/data/repository/SWRepositoryParksLocalStorageTest.kt` ✅
- [x] `app/src/test/java/com/swparks/data/repository/CountriesRepositoryLocalStorageTest.kt` ✅

### Изменяемые файлы

- [x] `app/src/main/java/com/swparks/data/UserPreferencesRepository.kt` ✅
- [x] `app/src/main/java/com/swparks/data/repository/SWRepository.kt` ✅
- [x] `app/src/main/java/com/swparks/data/repository/CountriesRepositoryImpl.kt` ✅
- [x] `app/src/main/java/com/swparks/data/AppContainer.kt` ✅ (добавлены clock, syncParksUseCase, syncCountriesUseCase)
- [x] `app/src/main/java/com/swparks/data/DefaultAppContainer.kt` ✅ (создан SystemClock и экземпляры Use Cases)
- [x] `app/src/main/java/com/swparks/ui/screens/RootScreen.kt` ✅ (добавлен `AppForegroundSyncHandler`)
- [x] `app/src/main/java/com/swparks/ui/viewmodel/ParksRootViewModel.kt` ✅ (добавлены подписка на Room, seed-инициализация и sync parks)
- [x] `app/src/test/java/com/swparks/data/UserPreferencesRepositoryTest.kt` ✅
- [x] `app/src/test/java/com/swparks/data/repository/CountriesRepositoryTest.kt` ✅ (обновлён для работы с локальным JSON-файлом)
- [x] `app/src/test/java/com/swparks/ui/viewmodel/ParksRootViewModelTest.kt` ✅ (добавлены тесты init sync, Room flow и pull-to-refresh)

## Примечания

- Дефолтное значение `lastParksUpdateDateString` = `"2025-10-25T00:00:00"` (как в iOS). Это значение **обязательно** для `SyncParksUseCase`, так как `getUpdatedParks(date)` вызывается только с непустой датой из хранилища.
- После успешного обновления парков дата = текущее время
- После успешного обновления стран дата = текущее время
- **Важно:** Пустой список от `getUpdatedParks` — это нормальный успешный ответ "изменений нет". Дата обновляется даже при пустом списке, чтобы избежать повторных запросов при каждом открытии экрана.
- **Clock pattern:** Все проверки времени и генерация дат через `Clock` интерфейс. Это позволяет писать детерминированные тесты с фиксированным `now`.
- **parks vs countries:** parks используют delta update (по дате), countries — полный справочник без передачи даты на сервер.
- Следовать правилам из `AGENTS.md`
- TDD: сначала тесты, потом реализация
- **Стратегия хранения:** parks хранятся в Room, countries/cities — в локальном JSON-файле в app-private storage, даты синхронизации — в DataStore.
- **Seed-файлы:** bundled JSON в `assets` используются только как начальные данные и не изменяются во время работы приложения.
- **Персистентность:** локально сохранённые parks и countries/cities должны переживать перезапуск приложения и быть доступны офлайн.
- **Реализация по TDD:** сначала тесты и реализация локального хранения (Room для parks, JSON-файл для countries/cities), затем Use Case, затем UI-интеграция.
- **Текущий статус:** Этапы 1-6 завершены. Этап 7 закрыт частично: `make format`, `make lint` и `make test` выполнены успешно. Остались ручные проверки, возможные интеграционные тесты и последующая полировка архитектуры/линтера.
- **Реализовано:**
  - Clock abstraction + SystemClock
  - DataStore для дат синхронизации
  - Локальное хранение parks (Room) и countries/cities (JSON-файл)
  - Use Cases для синхронизации parks и countries
  - Countries синхронизация при ON_START lifecycle event (RootScreen → AppForegroundSyncHandler)
  - Parks синхронизация при входе на экран parks (ParksRootViewModel + pull-to-refresh)
- **Ожидает реализации:** ручные сценарии проверки, при необходимости интеграционные тесты и рефакторинг для снижения detekt-шума.
