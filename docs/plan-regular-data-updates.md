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
[Clock] ─────────────────────────────────────┐
   │                                         │
   ↓                                         ↓
[SyncDateUtils.isUpdateNeeded()]    [SyncParksUseCase]
                                           │
[UserPreferencesRepository] ←──────────────┤
         ↓                                 ↓
[DataStore dates]                 [SWRepository.getUpdatedParks]
                                           │
[SyncCountriesUseCase] ← [CountriesRepository]
         ↓
[RootScreen - ON_START lifecycle event]
         ↓
[ParksRootViewModel - при входе на экран]
```

**Важно:**
- Синхронизация стран вызывается при `ON_START` lifecycle event в RootScreen,
  аналогично iOS `scenePhase == .active`. НЕ использовать `init` в ViewModel!
- `Clock` — интерфейс для детерминированных тестов. `SystemClock` в продакшене, `TestClock` в тестах.

## Этап 1: Domain Layer (Clock + Extensions)

**Проблема:** Тесты с `System.currentTimeMillis()` хрупкие и недетерминированные.
**Решение:** Clock abstraction для контроля времени в тестах.

- [ ] Создать интерфейс `Clock` в `domain/util/Clock.kt`

   ```kotlin
   interface Clock {
       fun now(): Instant
       fun nowIsoString(): String
   }
   ```

- [ ] Создать `SystemClock` реализацию в `data/util/SystemClock.kt`

   ```kotlin
   class SystemClock : Clock {
       override fun now(): Instant = Instant.now()
       // форматирование в ISO 8601
   }
   ```

- [ ] Написать тесты для `TestClock` (фиксированное время)
  - Тест: now() возвращает фиксированное время
  - Тест: nowIsoString() корректный формат
- [ ] Создать `TestClock` для тестов в `test/` (не в main)

   ```kotlin
   class TestClock(private val fixedInstant: Instant) : Clock { ... }
   ```

- [ ] Написать тесты для `isUpdateNeeded()` с `TestClock`
  - Тест: <1 дня → false
  - Тест: >1 дня → true
  - Тест: null дата → true
  - Тест: дефолтная дата "2025-10-25T00:00:00" → true
  - Тест: битая ISO-строка → true (приложение не падает, безопаснее считать что синк нужен)
- [ ] Реализовать extension функцию `String?.isUpdateNeeded(clock: Clock): Boolean`

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

- [ ] Написать тесты для `UserPreferencesRepository` новые методы
  - Тест сохранения `lastParksUpdateDate`
  - Тест сохранения `lastCountriesUpdateDate`
  - Тест чтения с дефолтными значениями (`"2025-10-25T00:00:00"`)
  - Тест Flow реактивности
- [ ] Добавить в `UserPreferencesRepository`:

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

## Этап 3: Data Layer (Local storage for parks and countries)

**Цель:** до реализации Use Case явно покрыть тестами и реализовать локальное хранение данных согласно выбранной стратегии:
- parks → Room
- countries/cities → локальный JSON-файл в app-private storage

### 3.1 Parks local storage (Room)

- [ ] Написать тесты для локального хранения parks
  - Тест: seed parks импортируются в Room при первой инициализации
  - Тест: повторная инициализация не дублирует parks в Room
  - Тест: delta-обновление parks делает upsert по id
  - Тест: существующий park обновляется при совпадении id
  - Тест: новый park добавляется при новом id

- [ ] Реализовать локальное хранение parks в Room
  - Определить основной локальный источник данных для parks как Room
  - Использовать bundled JSON с parks только как seed
  - При первом запуске импортировать seed parks в Room
  - Реализовать upsert delta-изменений parks в Room по id
  - Не изменять исходный bundled JSON во время работы приложения

### 3.2 Countries/cities local storage (JSON in app-private storage)

- [ ] Написать тесты для локального хранения countries/cities
  - Тест: при отсутствии локального файла seed JSON копируется в app-private storage
  - Тест: при наличии локального файла seed повторно не копируется
  - Тест: чтение countries/cities идёт из локального JSON-файла
  - Тест: полный справочник с сервера атомарно заменяет текущий локальный JSON
  - Тест: при ошибке записи старый локальный JSON не теряется

- [ ] Реализовать локальное хранение countries/cities в app-private storage
  - Определить основной локальный источник данных для countries/cities как JSON-файл в app-private storage
  - Использовать bundled JSON со справочником только как seed
  - При первом запуске копировать seed-файл в app-private storage
  - Реализовать чтение countries/cities из локального JSON-файла
  - Реализовать атомарную замену локального JSON после успешной загрузки полного справочника с сервера
  - Не изменять исходный bundled JSON во время работы приложения

### 3.3 Интеграция repository

- [ ] Обновить `SWRepository`
  - Использовать Room как локальный источник parks
  - Добавить/уточнить методы для seed-инициализации parks
  - Добавить/уточнить методы для upsert delta-обновлений parks

- [ ] Обновить `CountriesRepository`
  - Использовать локальный JSON-файл как основной источник countries/cities
  - Добавить/уточнить методы для seed-инициализации JSON-файла
  - Добавить/уточнить методы для атомарного обновления полного справочника

**Важно:** только после завершения этого этапа переходить к Use Case, так как `SyncParksUseCase` и `SyncCountriesUseCase` должны опираться на уже реализованную и протестированную локальную стратегию хранения.

## Этап 4: Domain Layer (Use Cases)

**Важно:** Use Cases принимают `Clock` в конструкторе для тестируемости.

**Предусловие:** локальная стратегия хранения уже реализована на предыдущем этапе:
- parks читаются из Room и обновляются через upsert delta-изменений;
- countries/cities читаются из локального JSON-файла и обновляются полной атомарной заменой файла.

- [ ] Написать тесты для `SyncParksUseCase` с `TestClock`
   - Тест: needUpdate = false → не обновлять
   - Тест: needUpdate = true → вызвать getUpdatedParks
   - Тест: успешное обновление (непустой список) → сохранить дату через `clock.nowIsoString()`
   - Тест: успешное обновление (пустой список) → сохранить дату
   - Тест: ошибка сети → не обновлять дату
- [ ] Создать `SyncParksUseCase(clock: Clock, ...)`
   - **Источник даты:** только `lastParksUpdateDate` из `UserPreferencesRepository`. UseCase никогда не вызывает обновление parks без даты.
   - Проверить `lastParksUpdateDate.isUpdateNeeded(clock)`
   - Вызвать `SWRepository.getUpdatedParks(date)` (date всегда непустая, дефолт "2025-10-25T00:00:00")
   - **При успешном ответе:** `SWRepository` получает delta-изменения parks и делает upsert в Room по id, после чего `SyncParksUseCase` обновляет `lastParksUpdateDate` на `clock.nowIsoString()`
   - При ошибке: не обновлять дату
- [ ] Написать тесты для `SyncCountriesUseCase` с `TestClock`
   - Тест: needUpdate = false → не обновлять
   - Тест: needUpdate = true → вызвать updateCountriesFromServer
   - Тест: успешное обновление → сохранить дату через `clock.nowIsoString()`
   - Тест: ошибка сети → не обновлять дату
- [ ] Создать `SyncCountriesUseCase(clock: Clock, ...)`
   - Проверить `lastCountriesUpdateDate.isUpdateNeeded(clock)`
   - Вызвать `CountriesRepository.updateCountriesFromServer()` (полный справочник, без передачи даты на сервер)
   - **При успешном ответе:** `CountriesRepository` получает полный справочник стран и городов, сериализует его в JSON и атомарно заменяет текущий локальный файл в app-private storage, после чего `SyncCountriesUseCase` обновляет `lastCountriesUpdateDate` на `clock.nowIsoString()`
   - При ошибке: не обновлять дату
   **Примечание:** countries обновляются полностью (в отличие от parks с delta), поэтому дата обновляется только после успешного сохранения.
- [ ] Добавить `SystemClock` и Use Cases в `AppContainer`

## Этап 5: UI Layer (RootScreen - Lifecycle)

**Важно:** Синхронизация стран при ON_START lifecycle event, аналогично iOS `scenePhase == .active`.

- [ ] Написать тесты для `AppForegroundSyncHandler`
   - Тест: ON_START → вызывается syncCountries
   - Тест: другие события → не вызывается
   - Тест: ошибка sync не блокирует UI
   - Тест: повторный ON_START → проверяется needUpdate
- [ ] Создать `AppForegroundSyncHandler` composable в отдельном файле `ui/screens/AppForegroundSyncHandler.kt`
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

- [ ] Добавить `SyncCountriesUseCase` в `RootScreen` (получить из `LocalAppContainer`)
- [ ] Вызвать `AppForegroundSyncHandler` в начале `RootScreen`

## Этап 6: UI Layer (ParksRootViewModel)

- [ ] Написать тесты для `ParksRootViewModel` обновления
  - Тест: при загрузке проверяется needUpdate
  - Тест: если parks уже загружены и не нужен refresh → не обновлять
  - Тест: если needUpdate → вызвать syncParksUseCase
  - Тест: принудительный refresh → обновить независимо от даты
- [ ] Обновить `ParksRootViewModel`
  - Добавить `SyncParksUseCase` в конструктор
  - Добавить логику в `loadParks()` или отдельный метод
  - Проверить needUpdate при первом входе на экран
- [ ] Обновить `ParksRootScreen` если нужно
  - Обработать pull-to-refresh с принудительным обновлением

## Этап 7: Интеграция и тестирование

- [ ] Обновить `AppContainer.kt`
  - Создать `SystemClock` (singleton)
  - Создать `SyncParksUseCase(clock, ...)`
  - Создать `SyncCountriesUseCase(clock, ...)`
  - Передать в ViewModels
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
- [ ] При первом входе на экран parks загружаются из локального источника данных (Room)
- [ ] Parks доступны офлайн и не теряются после перезапуска приложения
- [ ] При первом заполнении локального хранилища seed parks импортируются в Room один раз без дублирования
- [ ] При устаревшей дате синхронизации (>1 дня) — дополнительно выполняется delta sync с сервера
- [ ] При повторном входе <1 дня — данные берутся из кэша без запроса к серверу
- [ ] Pull-to-refresh работает с принудительным обновлением

**Общее:**
- [ ] Ошибки сети не блокируют работу приложения
- [ ] `make format` без ошибок
- [ ] `make lint` без ошибок
- [ ] `make test` без ошибок
- [ ] *(Если добавлены UI/instrumentation тесты)* `make android-test` без ошибок

## Файлы для изменения

### Новые файлы

- `app/src/main/java/com/swparks/domain/util/Clock.kt`
- `app/src/main/java/com/swparks/domain/util/SyncDateUtils.kt` (isUpdateNeeded extension)
- `app/src/main/java/com/swparks/data/util/SystemClock.kt`
- `app/src/main/java/com/swparks/domain/usecase/SyncParksUseCase.kt`
- `app/src/main/java/com/swparks/domain/usecase/SyncCountriesUseCase.kt`
- `app/src/main/java/com/swparks/ui/screens/AppForegroundSyncHandler.kt`
- `app/src/test/java/com/swparks/domain/util/TestClock.kt`
- `app/src/test/java/com/swparks/domain/util/SyncDateUtilsTest.kt`
- `app/src/test/java/com/swparks/domain/usecase/SyncParksUseCaseTest.kt`
- `app/src/test/java/com/swparks/domain/usecase/SyncCountriesUseCaseTest.kt`
- `app/src/test/java/com/swparks/ui/screens/AppForegroundSyncHandlerTest.kt`
- `app/src/test/java/com/swparks/data/repository/SWRepositoryParksSyncTest.kt`
- `app/src/test/java/com/swparks/data/repository/CountriesRepositoryStorageTest.kt`

### Изменяемые файлы

- `app/src/main/java/com/swparks/data/UserPreferencesRepository.kt`
- `app/src/main/java/com/swparks/data/AppContainer.kt`
- `app/src/main/java/com/swparks/data/repository/SWRepository.kt`
- `app/src/main/java/com/swparks/data/repository/CountriesRepositoryImpl.kt`
- `app/src/main/java/com/swparks/ui/screens/RootScreen.kt`
- `app/src/main/java/com/swparks/ui/viewmodel/ParksRootViewModel.kt`
- `app/src/test/java/com/swparks/data/UserPreferencesRepositoryTest.kt`
- `app/src/test/java/com/swparks/ui/viewmodel/ParksRootViewModelTest.kt`

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
