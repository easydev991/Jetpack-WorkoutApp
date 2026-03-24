# План: Добавление фильтра парков по городу (Android)

## Контекст

В iOS-приложении (`SwiftUI-WorkoutApp`) реализован фильтр парков по городу:
- `SWTextFieldSearchButton` — кнопка с иконкой поиска и названием города
- При нажатии открывается `ItemListScreen` с выбором города
- Выбранный город сохраняется в `UserDefaults` (`selectedCityFilter`)
- Фильтрация парков происходит по `cityId`

В Android-приложении (`Jetpack-WorkoutApp`):
- `ItemListScreen` — stateless экран выбора элемента (уже существует!)
- `ItemListMode` — enum с `COUNTRY` и `CITY` режимами
- `IFilterParksUseCase` — существующий UseCase для фильтрации парков (НО не фильтрует по городу!)
- `CountriesRepository` — уже имеет `getAllCities()` и `getCityById()` (НЕ нужен отдельный CitiesRepository!)
- `ParkFilter` — существующий фильтр (size, type), НЕ включает город
- `ParksRootScreen` — главный экран парков, НО нет фильтра по городу

## Задача

Добавить в `ParksRootScreen` аналог `SWTextFieldSearchButton`, который:
1. Отображается под `TopAppBar`
2. Позволяет выбрать город через `ItemListScreen` (с режимом `CITY`)
3. Сохраняет выбранный город в `ParkFilter.selectedCityId`
4. Фильтрует парки по выбранному городу через `IFilterParksUseCase`

---

## Типы данных (подтверждено)

- `City.id`: `String` ( numeric: "1", "2", "3"... )
- `Park.cityID`: `Int`
- `countriesRepository.getCityById(cityId: String)` — принимает String
- `getAllCities()` возвращает `List<City>` с `id: String`
- При фильтрации парков: `city.id.toIntOrNull()` → Int (безопасно, т.к. ID числовые)
- При поиске города по ID: `cityId.toString()` (Int → String)

---

## Этап 1: Модели данных (Domain Layer)

### 1.1. Обновить `ParkFilter`

- [ ] Добавить поле `selectedCityId: Int?` в `data class ParkFilter`
- [ ] Обновить `isDefault` с учётом `selectedCityId`
- [ ] Написать unit-тесты для `ParkFilter`

**Файл:** `app/src/main/java/com/swparks/data/model/ParkFilter.kt`

### 1.2. Обновить `ParksFilterDataStore`

- [ ] Добавить ключ `selectedCityId` для сохранения в DataStore
- [ ] Обновить методы `save()` / `load()` для работы с `selectedCityId`
- [ ] Написать unit-тесты для `ParksFilterDataStore`

**Файл:** `app/src/main/java/com/swparks/data/preferences/ParksFilterDataStore.kt`

### 1.3. Обновить `FilterParksUseCase`

- [ ] Добавить фильтрацию по `filter.selectedCityId` в `invoke()`:
  ```kotlin
  return allParks.filter { park ->
      allowedSizes.contains(park.sizeID) &&
      allowedTypes.contains(park.typeID) &&
      (filter.selectedCityId == null || park.cityID == filter.selectedCityId)
  }
  ```
- [ ] **Важно:** НЕ менять сигнатуру `invoke()` — `selectedCityId` уже в `ParkFilter`
- [ ] Написать unit-тесты для `FilterParksUseCase` с фильтрацией по городу

**Файл:** `app/src/main/java/com/swparks/domain/usecase/FilterParksUseCase.kt`

---

## Этап 2: UI Layer — ViewModel

### 2.1. Обновить `IParksRootViewModel`

- [ ] Добавить `selectedCity: City?` в `ParksRootUiState` (для отображения названия в `SearchCityButton`)
- [ ] Добавить `cities: List<City>` в state (полный список для поиска, загружается асинхронно)
- [ ] Добавить `isLoadingCities: Boolean` в state (для индикации загрузки)
- [ ] Добавить `citySearchQuery: String` в state (для фильтрации списка городов в `ItemListScreen`)
- [ ] Добавить `filteredParks: List<Park>` в state (результат `filterParksUseCase`, вычисляется ViewModel)
- [ ] Добавить computed property `cityNames: List<String>` — производное от `cities` + `citySearchQuery` (НЕ в state!)
- [ ] Добавить методы `onSelectCityClick`, `onCitySearchQueryChange(query: String)`, `onCitySelected(cityName: String)`, `onClearCityFilter` в интерфейс
- [ ] Объявить extension function `toItemListUiState()` **рядом с интерфейсом** (НЕ в самом интерфейсе — extension functions нельзя объявлять в интерфейсе):
  ```kotlin
  // В файле IParksRootViewModel.kt или рядом
  fun ParksRootUiState.toItemListUiState() = ItemListUiState(
      mode = CITY,
      items = cityNames,           // computed property, фильтруется по citySearchQuery
      searchQuery = citySearchQuery,
      isLoading = isLoadingCities
  )
  ```
- [ ] **Реализация** `toItemListUiState()` — в `ParksRootViewModel.kt` или в отдельном файле-экстеншн
- [ ] Написать unit-тесты

**Примечание:** `cityNames` — derived state (вычисляется из `cities` + `citySearchQuery`), НЕ хранится в state напрямую.

**Файл:** `app/src/main/java/com/swparks/ui/viewmodel/IParksRootViewModel.kt`

### 2.2. Обновить `ParksRootViewModel`

- [ ] Реализовать методы интерфейса
- [ ] При инициализации ViewModel: загрузить список городов через `countriesRepository.getAllCities()` в state `cities`, установить `isLoadingCities = true`, затем `isLoadingCities = false`
- [ ] `cityNames` вычисляется как `cities.filter { it.name.contains(citySearchQuery, ignoreCase = true) }.map { it.name }`
- [ ] В `onCitySelected(cityName: String)` — найти город по имени в `cities`:
  ```kotlin
  fun onCitySelected(cityName: String) {
      val city = cities.find { it.name == cityName }
      val cityId = city?.id?.toIntOrNull()  // city.id это "1", "2"... -> Int 1, 2...
      // ... сохранить cityId в фильтр и city в state для отображения
  }
  ```
- [ ] Сохранять выбранный `City` в state (не только `cityId`) — для отображения названия в `SearchCityButton`
- [ ] При изменении `selectedCityId`: обновить `localFilter` в `_uiState`, вызвать `parksFilterDataStore.saveFilter(localFilter)` для персистентности, пересчитать `filteredParks` через `filterParksUseCase`
- [ ] **Примечание об архитектуре (Вариант A):** `filterParksUseCase` вызывается В ViewModel (как во всех остальных ViewModel приложения), а НЕ в UI-слое через `derivedStateOf`. ViewModel хранит `filteredParks` в state. Это стандартный паттерн MVVM, принятый в проекте.
- [ ] При восстановлении состояния: `parksFilterDataStore.filter` автоматически эмитит сохранённый `ParkFilter` при загрузке DataStore. Collector в `init {}` синхронизирует его с `localFilter` и пересчитывает `filteredParks`. `selectedCity` восстанавливается через `countriesRepository.getCityById(cityId.toString())` после загрузки `cities`.
- [ ] `onCitySearchQueryChange` — обновляет `citySearchQuery`, что триггерит пересчёт `cityNames`
- [ ] `onClearCityFilter` — сбрасывает `selectedCityId` и `selectedCity` в null, обновляет `localFilter`, сохраняет в DataStore, пересчитывает `filteredParks`
- [ ] Написать unit-тесты с моком `CountriesRepository`

**Порядок действий при выборе города:**
1. `onCitySelected(cityName)` → найти `City` в `cities` по имени
2. Найти `cityId = city?.id?.toIntOrNull()`
3. Создать новый `filter = localFilter.copy(selectedCityId = cityId)`
4. `_uiState.value = _uiState.value.copy(localFilter = filter, selectedCity = city)` (UI обновится мгновенно)
5. `parksFilterDataStore.saveFilter(filter)` (сохранить персистентно)
6. `filteredParks = filterParksUseCase(parks, localFilter)` (пересчитать отфильтрованные парки)

**Примечание о двойной фильтрации:** При сохранении в DataStore (step 5), collector в `init {}` также получит обновлённый `ParkFilter` из flow и пересчитает `filteredParks` ещё раз. Результат одинаковый — это ожидаемое поведение (не баг), так как DataStore работает асинхронно. Избежать двойного пересчёта можно добавив проверку `if (localFilter != emittedFilter)`, но это усложняет код без практической пользы.

**Файл:** `app/src/main/java/com/swparks/ui/viewmodel/ParksRootViewModel.kt`

**Примечание:** `CountriesRepository` уже существует, содержит `getAllCities()` и `getCityById(cityId: String)`. Отдельный `CitiesRepository` НЕ нужен.

**Примечание по ItemListScreen:** `ItemListScreen` работает со `items: List<String>` (имена), поэтому возврат выбранного города происходит по имени (`cityName: String`). Это workaround, но оправданный — `ItemListScreen` stateless и переиспользуемый. Поиск `City` по имени в `cities` работает корректно, если список загружен заранее.



---

## Этап 3: UI Layer — Компоненты

### 3.1. Создать `SearchCityButton` (аналог `SWTextFieldSearchButton`)

- [ ] Создать stateless composable `SearchCityButton`
- [ ] Параметры: `cityName: String?`, `onClick: () -> Unit`, `onClearClick: (() -> Unit)?`
- [ ] UI: TextField-styled button с иконкой поиска, текстом города, опциональной кнопкой очистки
- [ ] Написать unit-тесты (Robolectric + MockK + Turbine, как в проекте)

**Файл:** `app/src/main/java/com/swparks/ui/components/SearchCityButton.kt`

### 3.2. Обновить `ParksRootScreen`

- [ ] Добавить `SearchCityButton` под `TopAppBar`, над списком парков
- [ ] При `onSelectCityClick` → `navController.navigate(Screen.SelectCityForFilter.route)`
- [ ] Передавать в `ItemListScreen` (route: `Screen.SelectCityForFilter.route`):
  - `state` = `ItemListUiState(mode = CITY, items = viewModel.cityNames, ...)`
  - `onItemSelected` → `viewModel.onCitySelected(cityName)` (String, не id!)
  - `onBackClick` → `navController.popBackStack()`
- [ ] Получать `filteredParks` из `viewModel.uiState.value.filteredParks` (а не через `derivedStateOf`)

**Файл:** `app/src/main/java/com/swparks/ui/screens/parks/ParksRootScreen.kt`

**Важно:** `SearchCityButton.onClearClick` → `viewModel.onClearCityFilter()`, НЕ навигация

### 3.3. Переиспользовать `ItemListScreen`

- [ ] `ItemListScreen` уже stateless — используем напрямую
- [ ] НЕ использовать `SelectCityScreen` (он привязан к `IEditProfileViewModel`)

**Выбран Вариант B:** `ParksRootViewModel` управляет состоянием `ItemListScreen` напрямую

**Обоснование:** Вариант B проще — меньше кода, `ParksRootViewModel` уже имеет доступ к `countriesRepository` для получения списка городов. `ItemListScreen` полностью stateless и принимает всё нужное через `ItemListUiState`.

---

## Этап 4: Навигация

### 4.1. Добавить route для экрана выбора города

- [ ] Добавить `SelectCityForFilter` в `Screen` sealed class:
  ```kotlin
  object SelectCityForFilter : Screen("select_city_for_filter", parentTab = Parks)
  ```
- [ ] Добавить `"select_city_for_filter"` в `BOTTOM_BAR_HIDDEN_BASE_ROUTES` в `RootScreen.kt`
- [ ] Добавить composable для `SelectCityForFilter` в `RootScreen.kt`:
  ```kotlin
  composable(Screen.SelectCityForFilter.route) {
      val context = LocalContext.current
      val viewModel: ParksRootViewModel = viewModel(...)
      val state by viewModel.uiState.collectAsState()
      ItemListScreen(
          state = state.toItemListUiState(), // конвертировать в ItemListUiState
          onSearchQueryChange = viewModel::onCitySearchQueryChange,
          onItemSelected = viewModel::onCitySelected,
          onContactUs = {
              val feedback = LocationFeedback.createCity(context)
              sendLocationFeedback(context, feedback)
          },
          onBackClick = { navController.popBackStack() }
      )
  }
  ```
  **Примечание:** `LocationFeedback.createCity(context)` и `sendLocationFeedback(context, feedback)` уже существуют в проекте. Используются для отправки email с предзаполненными данными (те же что в `SelectCityScreen`).
- [ ] Обновить `Screen.findParentTab()` для нового route (если нужно)
- [ ] Добавить `SelectCityForFilter` в `Screen.allScreens` (companion object в `Screen` sealed class)

**Примечание:** `onContactUs` нужен для отправки email с предзаполненными данными города, если пользователь не нашёл нужный город в списке. `LocationFeedback.createCity()` и `sendLocationFeedback()` уже существуют в проекте.

**Импорты:**
```kotlin
import com.swparks.util.LocationFeedback
import com.swparks.ui.screens.more.sendLocationFeedback
```

**Файл:** 
- `app/src/main/java/com/swparks/navigation/Destinations.kt`
- `app/src/main/java/com/swparks/ui/screens/RootScreen.kt`

**Важно:** `parentTab = Parks` — экран принадлежит вкладке Parks, чтобы подсвечивалась правильная вкладка. `BOTTOM_BAR_HIDDEN_BASE_ROUTES` скрывает bottom navigation.

---

## Этап 5: Локализация

### 5.1. Добавить строки

- [ ] `"Выбери город"` — placeholder для кнопки (если ещё нет)
- [ ] Проверить существующие строки в `strings.xml`

**Файл:** `app/src/main/res/values/strings.xml`

---

## Зависимости между этапами

```
Этап 1 (Model + UseCase) ──→ Этап 2 (ViewModel)
                                       │
                                       ↓
                              Этап 3 (UI Components)
                                       │
                                       ↓
                              Этап 4 (Navigation)
                                       │
                                       ↓
                              Этап 5 (Localization)
```

---

## Критерии завершения

- [ ] `SearchCityButton` отображается под TopAppBar на `ParksRootScreen`
- [ ] Нажатие открывает `ItemListScreen` (режим CITY)
- [ ] Выбор города сохраняется в `ParksFilterDataStore`
- [ ] `IFilterParksUseCase` фильтрует парки по `cityId`
- [ ] Кнопка очистки сбрасывает фильтр города
- [ ] Unit-тесты проходят (`./gradlew :app:testDebugUnitTest`)
- [ ] Линт проходит (`make lint`)

---

## Примечание по существующим тестам

При добавлении новых полей в state (`selectedCity`, `cities`, `isLoadingCities`, `citySearchQuery`, `filteredParks`) потребуется обновить существующие тесты:

- [ ] `ParksRootViewModelTest.kt` — добавить моки для `countriesRepository`, обновить initial state и assertions
- [ ] `FilterParksUseCaseTest.kt` — добавить тест-кейсы для фильтрации по городу
- [ ] Если существуют тесты для `ParkFilter` — обновить `isDefault` assertions
- [ ] Проверить `ParksFilterDataStoreTest.kt` (если есть) на совместимость с новым полем

**Важно:** Новые поля в data class требуют либо явной инициализации в тестах, либо использования `copy()` с явным указанием всех полей.
