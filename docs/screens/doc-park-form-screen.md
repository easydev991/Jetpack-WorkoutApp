# ParkFormScreen (создание/редактирование площадки)

## Обзор

Экран для создания и редактирования площадок (парков) с двумя режимами работы.

**Источник истины для UI (iOS):** `ParkFormScreen.swift` + `ParkForm.swift`

**Референс (Android):** `EventFormScreen.kt`

## Режимы работы

| Режим  | Route                     | Описание                             |
|--------|---------------------------|--------------------------------------|
| Create | `Screen.CreatePark`       | Создание новой площадки              |
| Edit   | `Screen.EditPark(parkId)` | Редактирование существующей площадки |

## Поля формы

### iOS ParkForm

| Поле            | Тип         | Описание                     |
|-----------------|-------------|------------------------------|
| `address`       | String      | Адрес площадки               |
| `latitude`      | String      | Широта                       |
| `longitude`     | String      | Долгота                      |
| `cityId`        | Int?        | ID города                    |
| `typeId`        | Int         | Тип площадки (ParkGrade)     |
| `sizeId`        | Int         | Размер площадки (ParkSize)   |
| `photosCount`   | Int         | Количество существующих фото |
| `newMediaFiles` | [MediaFile] | Новые фото                   |

### Android ParkType / ParkSize

| ParkType  | rawValue | R.string         |
|-----------|----------|------------------|
| SOVIET    | 1        | `soviet_park`    |
| MODERN    | 2        | `modern_park`    |
| COLLARS   | 3        | `collars_park`   |
| LEGENDARY | 6        | `legendary_park` |

| ParkSize | rawValue | R.string      |
|----------|----------|---------------|
| SMALL    | 1        | `small_park`  |
| MEDIUM   | 2        | `medium_park` |
| LARGE    | 3        | `large_park`  |

## Архитектура

### Navigation

- `Screen.CreatePark` / `Screen.EditPark(parkId: Long)` — routes
- `NewParkDraft` — draft для передачи данных между экранами
- Навигация: `ParkDetail → EditPark` через `NavigateToEditPark` event → coordinator → `RootScreen`

### State Management

- `ParkFormUiState` — data class для состояния UI
- `ParkFormEvent` — sealed interface для одноразовых событий (Saved, NavigateBack, ShowPhotoPicker)
- `IParkFormViewModel` + `ParkFormViewModel`
- Одноразовые события через `Channel`

### Data Flow

```
UI → ViewModel → UseCase → Repository → API/Room/DataStore
```

## Компоненты

| Компонент                       | Назначение                                 |
|---------------------------------|--------------------------------------------|
| `FormCardContainer`             | Контейнер карточки формы                   |
| `PickedImagesGrid`              | Сетка выбранных фото                       |
| `ImagePreviewDialog`            | Диалог предпросмотра фото                  |
| `LoadingOverlayView`            | Overlay загрузки                           |
| `KeyboardAwareBottomBar`        | Нижняя панель с учётом клавиатуры          |
| `CreateParkFab`                 | FAB для создания площадки                  |
| `LocationPermissionAlertDialog` | Диалог разрешения геолокации               |
| `RadioButton`                   | Компонент для выбора типа/размера площадки |

## Геолокация

### Permissions

- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`
- `LocationPermissionState` — состояние разрешений

### Services

| Сервис             | Реализация                                                     |
|--------------------|----------------------------------------------------------------|
| `LocationService`  | `LocationServiceImpl` (FusedLocationProviderClient + fallback) |
| `GeocodingService` | `GeocodingServiceImpl` (Geocoder с `Locale("ru", "RU")`)       |

### UseCases

- `FindCityByCoordinatesUseCase` — определение города по координатам
- Fallback: `currentUser.cityID` при неудаче геокодирования

### Error Handling

- `GeocodingFailureKind` — типы ошибок геокодирования
- `LocationFailed` / `GeocodingFailed` — mapped errors
- String resources: `location_fetch_failed`, `geocoding_address_build_fail` (EN/RU)

## Интеграция с экранами

### ParksRootScreen (Create entrypoint)

- `CreateParkFab` + `LocationPermissionState` интегрированы
- FAB → `CreatePark` route
- `createParkLocationHandler` — обработчик геолокации

### ParkDetailScreen (Edit flow)

- `NavigateToEditPark` → coordinator → `RootScreen`
- `updatedPark` через `savedStateHandle`
- Refresh после сохранения

### ParksRootViewModel

- `IParksRootViewModel` + `ParksRootViewModel`
- `StateFlow` / `SharedFlow` для state/events
- `FakeParksRootViewModel` для androidTest

## Кэширование (Cache sync)

**Проблема:** `savePark()` / `deletePark()` не обновляли локальный кэш `UserEntity.addedParks`.

**Решение:** Приватные методы в `SWRepository` для синхронизации кэша пользователя.

| Операция                                   | Описание                            |
|--------------------------------------------|-------------------------------------|
| `SWRepository.updateUserAddedParksCache()` | Добавление парка в кэш пользователя |
| `SWRepository.removeParkFromUser()`        | Удаление парка из кэша пользователя |
| `UserDao.update()`                         | Сохранение изменений в Room         |
| `SWRepository.savePark()`                  | Синхронизация кэша                  |
| `SWRepository.deletePark()`                | Синхронизация кэша                  |

## Russian locale для Geocoding

**Проблема:** `Geocoder` использовал дефолтную локаль устройства → `countryName`/`locality` на английском, но `CountriesRepository` содержит только русские названия.

**Решение:**

- `geocoderProvider` signature: `(Context) → (Context, Locale) → Geocoder`
- Lazy init: `geocoderProvider(context, Locale("ru", "RU"))`

## Тесты

| Этап    | Компонент                                                                 | Тесты       |
|---------|---------------------------------------------------------------------------|-------------|
| 1 (TDD) | ParkForm модель                                                           | 35          |
| 2 (TDD) | Data Layer, SWRepository.savePark                                         | —           |
| 3 (TDD) | IParkFormViewModel + ParkFormViewModel                                    | 33          |
| 4 (TDD) | UI Layer (Compose)                                                        | 17          |
| 5.4     | FindCityByCoordinatesUseCase                                              | 8           |
| 5.4.4   | ParkForm geocoding                                                        | 6           |
| 5.4.7   | ParksRootScreenTest, ParkFormViewModel, LocationPermissionAlertDialogTest | androidTest |
| 6.0.1   | ParksRootViewModel                                                        | 11          |
| 6.1     | ParksRootScreen permission UI                                             | 9           |
| 6.3     | ParksRootScreen permission UI-тесты, ParkNavigationCoordinator            | unit        |
| 7       | SWRepositoryParksTest (cache sync)                                        | unit        |
| 8       | GeocodingServiceImpl (Russian locale)                                     | 5           |

**Примечание:** Нет отдельных тестов UserDao; тесты park operations в `SWRepositoryParksTest`.

## Готово

- [x] Этапы 1-4: ParkForm (35 тестов), ParkFormViewModel (33 теста), ParkFormScreen (17 тестов)
- [x] Этап 5: routes, nav args, `NewParkDraft`, permissions, FAB, services, DI, error mapping
- [x] Этап 6.0.1: ParksRootViewModel (11 unit-тестов), `FakeParksRootViewModel` для androidTest
- [x] Этап 6.1 (Create entrypoint): ParksRootScreen integration tests (9 тестов)
- [x] Этап 6.2 (Edit flow): `ParkDetail → EditPark`, refresh после сохранения
- [x] Этап 6.3: ParksRootScreen permission UI-тесты, `ParkNavigationCoordinator`, `RootScreen` integration
- [x] Этап 7 (Cache sync): `SWRepository.updateUserAddedParksCache()` / `removeParkFromUser()`, cache sync
- [x] Этап 8 (Russian locale): `GeocodingServiceImpl` использует `Locale("ru", "RU")`
- [x] `make lint` ✅, `make test` ✅

## Out of Scope

- Offline поддержка
- Дополнительная валидация координат
