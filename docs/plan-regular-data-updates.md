# План: Регулярное обновление парков и справочников

Реализация автоматической проверки и обновления данных о парках и странах/городах по аналогии с iOS версией.

## Стратегия

- **parks** → Room (offline-first, delta-обновления)
- **countries/cities** → локальный JSON-файл в app-private storage (полное обновление)
- **даты синхронизации** → DataStore
- **seed-файлы** → только как начальные данные, не изменяются при работе

## Выполненные этапы

### Этап 1-3: Data Layer

- [x] Clock abstraction (SystemClock + TestClock)
- [x] DataStore для дат синхронизации (lastParksUpdateDate, lastCountriesUpdateDate)
- [x] Локальное хранение parks (Room: ParkEntity, ParkDao, upsert)
- [x] Локальное хранение countries/cities (JSON-файл: copySeedFromAssets, атомарная запись)

### Этап 4: Domain Layer (Use Cases)

- [x] SyncParksUseCase (delta-обновление, date-only для API)
- [x] SyncCountriesUseCase (полный справочник)
- [x] isUpdateNeeded() extension (проверка >1 дня)

### Этап 5: UI Layer - Countries sync

- [x] AppForegroundSyncHandler (ON_START lifecycle)
- [x] Интеграция в RootScreen

### Этап 6: UI Layer - Parks sync

- [x] ParksRootViewModel (seed-инициализация, подписка на Room, sync)
- [x] InitializeParksUseCase (изоляция Context)
- [x] Кнопка обновления в TopAppBar с LoadingOverlayView

### Этап 7: Интеграция

- [x] Логирование на русском
- [x] Все тесты (1735) проходят
- [x] format/lint без ошибок

## Исправленные баги

1. **null date в countries** → изменён тип на String с дефолтом
2. **redundant null check** → удалён в SyncCountriesUseCase
3. **timezone bug** → добавлен "Z" суффикс в SystemClock.nowIsoString()
4. **API date format** → добавлено `take(10)` для передачи только даты
5. **concurrent refresh** → добавлена защита в refresh()

## Файлы

**Новые:**
- `Clock.kt`, `SyncDateUtils.kt`, `SystemClock.kt`, `SyncParksUseCase.kt`, `SyncCountriesUseCase.kt`
- `AppForegroundSyncHandler.kt`, `InitializeParksUseCase.kt`
- `TestClock.kt`, `SyncParksUseCaseTest.kt`, `SyncCountriesUseCaseTest.kt`, ...

**Изменённые:**
- `UserPreferencesRepository.kt`, `SWRepository.kt`, `CountriesRepositoryImpl.kt`
- `ParksRootViewModel.kt`, `ParksRootScreen.kt`, `RootScreen.kt`, ...

## Критерии завершения

- [x] Countries доступны офлайн из seed или обновляются при >1 дня
- [x] Parks загружаются из Room, delta-sync при >1 дня
- [x] Pull-to-refresh заменён на кнопку в TopAppBar
- [x] Ошибки сети не блокируют UI
- [x] make test/format/lint без ошибок

## Осталось

- [ ] *(Опционально)* Ручное тестирование сценариев синхронизации

## Новые задачи

### 1. Логирование валидации парков

Добавить логирование количества валидных парков и количества отфильтрованных в `normalizeSelectedCityParks()`:
- В начале функции: `totalCount = parks.size`
- После `mapNotNull`: `validCount = parksWithDistance.size`
- Логировать: `"Валидация парков: всего=$totalCount, валидных=$validCount, отфильтровано=${totalCount - validCount}"`

### 2. Offline-поддержка для ParkDetailScreen

При ошибках сети/сервера показывать данные из Room:
1. Добавить метод в `ParkDao`: `suspend fun getParkById(id: Long): ParkEntity?`
2. Обновить `SWRepository.getPark()`: при IOException - fallback на Room
3. Обновить `ParkDetailViewModel.loadPark()`: при ошибке - использовать кэшированные данные
