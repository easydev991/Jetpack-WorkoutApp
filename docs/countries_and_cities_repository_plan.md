# План реализации справочника стран и городов

## Обзор

Необходимо создать репозиторий для работы со справочником стран и городов, который:
- Загружает данные из локального JSON-файла `countries.json` на первой итерации
- Предоставляет возможность получения стран и городов по идентификаторам
- Поддерживает будущую интеграцию с API для обновления справочника
- Может быть использован из любой точки приложения

## Архитектурные решения

### Текущая ситуация

- **Модели данных**: `Country` и `City` уже существуют в `domain/model/`
- **API endpoint**: `@GET("countries")` уже существует в `SWApi.kt`
- **Локальные данные**: Файл `countries.json` содержит полный список стран и городов

### Подход к реализации

- **Гибридный подход**: чтение из локального JSON-файла + возможность обновления с сервера в будущем
- **Отдельный репозиторий**: `CountriesRepository` для изоляции логики справочника
- **Использование Flow**: для реактивного доступа к данным
- **Кэширование в памяти**: данные загружаются один раз при первом обращении

---

## Этап 1: Domain Layer

### 1.1. Создать интерфейс CountriesRepository

**Файл**: `app/src/main/java/com/swparks/domain/repository/CountriesRepository.kt`

**Методы**:
- `fun getCountriesFlow(): Flow<List<Country>>` - получить список всех стран
- `suspend fun getCountryById(countryId: String): Country?` - получить страну по ID
- `suspend fun getCityById(cityId: String): City?` - получить город по ID
- `suspend fun getCitiesByCountry(countryId: String): List<City>` - получить города страны
- `suspend fun updateCountriesFromServer(): Result<Unit>` - обновить справочник с сервера (для будущего использования)

**Примечание**: Все ID имеют тип `String`, так как в JSON-файле они передаются как строки

### 1.2. Создать use cases (опционально для первой итерации)

**Файлы**:
- `app/src/main/java/com/swparks/domain/usecase/GetCountriesUseCase.kt`
- `app/src/main/java/com/swparks/domain/usecase/GetCountryByIdUseCase.kt`
- `app/src/main/java/com/swparks/domain/usecase/GetCityByIdUseCase.kt`
- `app/src/main/java/com/swparks/domain/usecase/GetCitiesByCountryUseCase.kt`

**Назначение**:
- Упрощение ViewModels
- Изоляция бизнес-логики доступа к справочнику
- Возможность добавления валидации и фильтрации в будущем

---

## Этап 2: Data Layer

### 2.1. Создать CountriesRepositoryImpl

**Файл**: `app/src/main/java/com/swparks/data/repository/CountriesRepositoryImpl.kt`

**Зависимости**:
- `Context` - для доступа к assets (передается в `ReadJSONFromAssets()`)
- `SWApi` - для будущих запросов к серверу
- `Logger` - для логирования

**Импорты**:
- `com.swparks.utils.ReadJSONFromAssets` - для чтения JSON из assets
- `kotlinx.serialization.Json` - для десериализации JSON
- `kotlinx.serialization.SerializationException` - для обработки ошибок десериализации

**Логика работы**:

#### Чтение из локального JSON-файла

- Использовать существующую функцию `ReadJSONFromAssets()` из `com.swparks.utils` для чтения JSON-строки из `assets/countries.json`
- Десериализовать JSON в `List<Country>` с помощью `kotlinx.serialization.Json.decodeFromString()`
- Кэшировать данные в памяти (private variable)

**Пример реализации**:

```kotlin
private suspend fun loadCountriesFromAssets(): List<Country> {
    val jsonString = ReadJSONFromAssets(context, "countries.json")
    if (jsonString.isEmpty()) {
        Log.e("CountriesRepository", "Не удалось прочитать countries.json")
        return emptyList()
    }
    return try {
        Json.decodeFromString<List<Country>>(jsonString)
    } catch (e: SerializationException) {
        Log.e("CountriesRepository", "Ошибка десериализации JSON: ${e.message}")
        emptyList()
    }
}
```

#### Реактивный доступ

- `getCountriesFlow()` возвращает Flow с кэшированными данными
- Использовать `MutableStateFlow` для хранения данных в памяти

#### Получение данных по ID

- `getCountryById()` - поиск страны в кэше по ID
- `getCityById()` - поиск города в кэше по ID (перебор всех городов всех стран)
- `getCitiesByCountry()` - получить города конкретной страны из кэша

#### Обновление с сервера (для будущего использования)

- `updateCountriesFromServer()` - вызвать `swApi.getCountries()` и обновить кэш
- Обработать ошибки сети (IOException, HttpException)
- Логировать ошибки на русском языке

**Обработка ошибок**:
- Ошибки чтения JSON-файла - вернуть пустой список и залогировать ошибку
- Ошибки сети при обновлении - вернуть Result.failure() с описанием ошибки

### 2.2. Использование существующей функции ReadJSONFromAssets

**Файл**: `app/src/main/java/com/swparks/utils/ReadJSONFromAssets.kt` (уже существует)

**Назначение**:
- Чтение JSON-файлов из assets
- Обработка ошибок чтения файлов
- Логирование на русском языке

**Использование в CountriesRepositoryImpl**:
- Функция уже готова к использованию
- Принимает `Context` и путь к файлу в assets
- Возвращает JSON-строку или пустую строку при ошибке
- Логирует ошибки чтения файлов

**Примечание**: Не нужно создавать отдельный класс для чтения JSON. Используем существующую функцию `ReadJSONFromAssets()` внутри `CountriesRepositoryImpl`.

---

## Этап 3: Dependency Injection

### 3.1. Добавить CountriesRepository в AppContainer

**Файл**: `app/src/main/java/com/swparks/data/AppContainer.kt`

**Изменения**:
1. Добавить в интерфейс `AppContainer`:

   ```kotlin
   val countriesRepository: com.swparks.domain.repository.CountriesRepository
   ```

2. В классе `DefaultAppContainer` добавить создание репозитория:

   ```kotlin
   override val countriesRepository: CountriesRepository by lazy {
       CountriesRepositoryImpl(
           context = context,
           swApi = retrofitService,
           logger = logger
       )
   }
   ```

**Примечание**: Не забудьте добавить import для CountriesRepositoryImpl

---

## Этап 4: Использование в приложении

### 4.1. Примеры использования в ViewModels

**Пример 1: Профиль пользователя (ProfileViewModel)**

```kotlin
// Получение списка стран для выбора в профиле
val countriesState: StateFlow<List<Country>> = countriesRepository.getCountriesFlow()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
```

**Пример 2: Отображение страны и города площадке (ParkDetailViewModel)**

```kotlin
// Получение страны по ID
private fun loadCountryDetails(countryId: String) {
    viewModelScope.launch {
        val country = countriesRepository.getCountryById(countryId)
        _uiState.update { it.copy(country = country) }
    }
}

// Получение города по ID
private fun loadCityDetails(cityId: String) {
    viewModelScope.launch {
        val city = countriesRepository.getCityById(cityId)
        _uiState.update { it.copy(city = city) }
    }
}
```

**Пример 3: Получение списка городов страны**

```kotlin
// Получение городов выбранной страны
suspend fun getCitiesForCountry(countryId: String): List<City> {
    return countriesRepository.getCitiesByCountry(countryId)
}
```

### 4.2. Интеграция с существующими экранами

**Экраны, которые могут использовать CountriesRepository**:
- `ProfileScreen` - выбор страны и города в профиле
- `ParkDetailScreen` - отображение страны и города площадки
- `EventDetailScreen` - отображение страны и города мероприятия
- `CreateEditParkScreen` - выбор страны и города при создании площадки
- `CreateEditEventScreen` - выбор страны и города при создании мероприятия

**Инструкция по интеграции**:
1. Внедрить `CountriesRepository` через AppContainer в ViewModel
2. Создать методы для загрузки стран и городов
3. Добавить состояния в UI state для хранения загруженных данных
4. Использовать данные для отображения и выбора в UI

---

## Этап 5: Тестирование

### 5.1. Unit-тесты для CountriesRepositoryImpl

**Файл**: `app/src/test/java/com/swparks/data/repository/CountriesRepositoryTest.kt`

**Тесты**:
- `testGetCountriesFlow_returnsCachedCountries()` - проверка получения списка стран
- `testGetCountryById_existingId_returnsCountry()` - проверка получения страны по существующему ID
- `testGetCountryById_nonExistingId_returnsNull()` - проверка возврата null для несуществующего ID
- `testGetCityById_existingId_returnsCity()` - проверка получения города по существующему ID
- `testGetCityById_nonExistingId_returnsNull()` - проверка возврата null для несуществующего ID
- `testGetCitiesByCountry_returnsCorrectCities()` - проверка получения городов страны
- `testUpdateCountriesFromServer_success()` - проверка успешного обновления с сервера
- `testUpdateCountriesFromServer_networkError()` - проверка обработки ошибок сети

**Моки**:
- Mock для `Context` - для доступа к assets
- Mock для `SWApi` - для тестирования обновления с сервера
- Фиктивные данные для тестов

---

## Этап 6: Будущее расширение

### 6.1. Обновление справочника с сервера

**Когда потребуется**:
- Добавить автоматическое обновление при старте приложения (например, раз в неделю)
- Добавить кнопку "Обновить справочник" в настройках
- Использовать периодическое обновление в фоне

**Реализация**:
- Вызывать `countriesRepository.updateCountriesFromServer()` при необходимости
- Хранить дату последнего обновления в DataStore
- Показывать пользователю индикатор загрузки при обновлении

### 6.2. Оптимизация поиска городов по ID

**Текущий подход**: перебор всех городов всех стран

**Оптимизация**:
- Создать `Map<String, City>` для быстрого поиска городов по ID
- Заполнять map при загрузке данных из JSON
- Использовать map для O(1) поиска вместо O(n)

**Пример**:

```kotlin
private val citiesById: Map<String, City> by lazy {
    countries.flatMap { country -> country.cities }
        .associateBy { city -> city.id }
}

suspend fun getCityById(cityId: String): City? = citiesById[cityId]
```

---

## Критерии завершения

### Этап 1: Domain Layer

- [ ] Создан интерфейс `CountriesRepository`
- [ ] Созданы use cases для доступа к справочнику

### Этап 2: Data Layer

- [ ] Создан `CountriesRepositoryImpl` с чтением из JSON
- [ ] Использована существующая функция `ReadJSONFromAssets()` для чтения JSON
- [ ] Реализованы методы для получения стран и городов по ID
- [ ] Добавлено кэширование данных в памяти
- [ ] Реализован метод для обновления с сервера (для будущего использования)
- [ ] Добавлена обработка ошибок чтения JSON
- [ ] Добавлено логирование на русском языке

### Этап 3: Dependency Injection

- [ ] `CountriesRepository` добавлен в `AppContainer`
- [ ] Репозиторий внедрен через DI

### Этап 4: Использование в приложении

- [ ] Созданы примеры использования в ViewModels
- [ ] Интегрировано с минимум одним экраном (например, ProfileScreen)
- [ ] Данные о странах и городах корректно отображаются в UI

### Этап 5: Тестирование

- [ ] Написаны unit-тесты для `CountriesRepositoryImpl`
- [ ] Все тесты проходят успешно
- [ ] Покрытие кода тестами ≥ 80%

---

## Примечания и рекомендации

### Архитектурные рекомендации

- **Изоляция логики**: Использовать отдельный репозиторий для справочника стран
- **Реактивность**: Использовать Flow для реактивного доступа к данным
- **Безопасное разворачивание опционалов**: Не использовать `!!` для извлечения опционалов
- **Обработка ошибок**: Логировать ошибки на русском языке

### Производительность

- **Кэширование**: Данные загружаются один раз при первом обращении
- **Оптимизация поиска**: Рассмотреть использование Map для быстрого поиска городов по ID
- **Минимизация запросов**: Не делать лишних запросов к серверу (читать из локального JSON)

### Будущее развитие

- **Обновление с сервера**: Метод `updateCountriesFromServer()` уже готов для использования
- **Периодическое обновление**: Добавить автоматическое обновление справочника в будущем
- **Фильтрация и поиск**: Добавить возможности фильтрации и поиска стран/городов

### Совместимость с iOS-приложением

- **Формат данных**: Использовать такой же формат JSON, как в iOS-приложении
- **Модели данных**: Использовать уже существующие модели `Country` и `City`
- **API endpoint**: Использовать существующий endpoint `/countries`

---

## Приоритет задач

### Первая итерация (обязательно)

1. Создать интерфейс `CountriesRepository`
2. Реализовать `CountriesRepositoryImpl` с чтением из JSON
3. Добавить репозиторий в `AppContainer`
4. Интегрировать с одним экраном для проверки работоспособности

### Вторая итерация (рекомендуется)

1. Создать use cases для упрощения ViewModels
2. Интегрировать с другими экранами приложения
3. Написать unit-тесты для репозитория

### Третья итерация (будущее)

1. Добавить оптимизацию поиска городов через Map
2. Реализовать обновление справочника с сервера
3. Добавить периодическое обновление в фоне
