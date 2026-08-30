# Справочник стран и городов

## Расположение в коде

| Файл                                          | Назначение                               |
|-----------------------------------------------|------------------------------------------|
| `data/repository/CountriesRepositoryImpl.kt`  | Репозиторий (`open class`; чтение из JSON и с сервера, кэширование). Доменный интерфейс не выделялся — use case'ы зависят от impl напрямую |
| `domain/usecase/GetCountriesUseCase.kt`       | Получить все страны (Flow)               |
| `domain/usecase/GetCountryByIdUseCase.kt`     | Получить страну по ID                    |
| `domain/usecase/GetCityByIdUseCase.kt`        | Получить город по ID                     |
| `domain/usecase/GetCitiesByCountryUseCase.kt` | Получить города страны                   |
| `domain/usecase/SyncCountriesUseCase.kt`      | Обновление справочника с сервера         |
| `domain/usecase/FindCityByCoordinatesUseCase.kt` | Поиск города по координатам           |
| `app/src/main/assets/countries.json`          | Локальный справочник                     |
| `app/src/test/.../CountriesRepositoryTest.kt` | Unit-тесты |
| `app/src/test/.../CountriesRepositoryLocalStorageTest.kt` | Unit-тесты локального хранилища |

## Реализовано

- ✅ `CountriesRepositoryImpl` (доменный интерфейс не выделялся) и use case'ы чтения, а также `SyncCountriesUseCase` и `FindCityByCoordinatesUseCase`
- ✅ `CountriesRepositoryImpl`: чтение из `assets/countries.json`, обновление с сервера (`updateCountriesFromServer()`), кэширование через `MutableStateFlow`
- ✅ Оптимизация поиска через `citiesByIdMap` и `countriesByIdMap` (O(1))
- ✅ DI в `AppContainer`; используется в `ProfileViewModel`, `ParksRootViewModel`, `EventsViewModel`, `EventDetailViewModel`, `ParkDetailViewModel`, `RegisterViewModel`, `EditProfileViewModel`, `OtherUserProfileViewModel`
- ✅ Unit-тесты: `CountriesRepositoryTest` и `CountriesRepositoryLocalStorageTest`

---

## Невыполненные задачи

### Интеграция с экранами площадок и мероприятий

- [x] `ParkDetailScreen` — `ParkDetailViewModel` резолвит страну и город через репозиторий
- [x] `EventDetailScreen` — `EventDetailViewModel` резолвит страну и город через репозиторий
- [ ] `CreateEditParkScreen` — `ParkFormViewModel` справочник не использует
- [ ] `CreateEditEventScreen` — `EventFormViewModel` справочник не использует

### Тестирование

- [ ] Проверка покрытия кода ≥ 80%

---

## Обновление справочника с сервера (реализовано)

- `SyncCountriesUseCase`: проверяет дату последнего обновления и при необходимости вызывает `countriesRepository.updateCountriesFromServer()`; дата хранится в DataStore (`UserPreferencesRepository.lastCountriesUpdateDate`)
- Ошибки обновления логируются и уходят в аналитику (`COUNTRIES_UPDATE_FAILED`)
- [ ] Осталось: индикатор загрузки при обновлении в UI (сейчас синхронизация проходит без UI)
