# Справочник стран и городов

## Расположение в коде

| Файл                                          | Назначение                               |
|-----------------------------------------------|------------------------------------------|
| `domain/repository/CountriesRepository.kt`    | Интерфейс репозитория                    |
| `data/repository/CountriesRepositoryImpl.kt`  | Реализация (чтение из JSON, кэширование) |
| `domain/usecase/GetCountriesUseCase.kt`       | Получить все страны                      |
| `domain/usecase/GetCountryByIdUseCase.kt`     | Получить страну по ID                    |
| `domain/usecase/GetCityByIdUseCase.kt`        | Получить город по ID                     |
| `domain/usecase/GetCitiesByCountryUseCase.kt` | Получить города страны                   |
| `app/src/main/assets/countries.json`          | Локальный справочник                     |
| `app/src/test/.../CountriesRepositoryTest.kt` | Unit-тесты (12)                          |

## Реализовано

- ✅ Интерфейс `CountriesRepository` и 4 use case
- ✅ `CountriesRepositoryImpl`: чтение из `assets/countries.json`, кэширование через `MutableStateFlow`
- ✅ Оптимизация поиска через `citiesByIdMap` и `countriesByIdMap` (O(1))
- ✅ DI в `AppContainer`, используется в `ProfileViewModel`
- ✅ Unit-тесты (12)

---

## Невыполненные задачи

### Интеграция с экранами площадок и мероприятий

- [ ] `ParkDetailScreen`, `CreateEditParkScreen`
- [ ] `EventDetailScreen`, `CreateEditEventScreen`

### Тестирование

- [ ] Проверка покрытия кода ≥ 80%

---

## Будущее расширение

### Обновление справочника с сервера

- Вызывать `countriesRepository.updateCountriesFromServer()` при необходимости
- Хранить дату последнего обновления в DataStore
- Показывать индикатор загрузки при обновлении
