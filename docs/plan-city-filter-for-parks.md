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
- `FilterParksUseCase` — UseCase для фильтрации парков (на момент старта плана не фильтровал по городу; сейчас фильтрует через `ParkFilter.selectedCityId`)
- `CountriesRepository` — уже имеет `getAllCities()` и `getCityById()` (НЕ нужен отдельный CitiesRepository!)
- `ParkFilter` — существующий фильтр (size, type), НЕ включает город
- `ParksRootScreen` — главный экран парков, НО нет фильтра по городу

## Задача

Добавить в `ParksRootScreen` аналог `SWTextFieldSearchButton`, который:
1. Отображается под `TopAppBar`
2. Позволяет выбрать город через `ItemListScreen` (с режимом `CITY`)
3. Сохраняет выбранный город в `ParkFilter.selectedCityId`
4. Фильтрует парки по выбранному городу через `IFilterParksUseCase`

**Типы данных:** `City.id: String` ("1", "2"...), `Park.cityID: Int`, `countriesRepository.getCityById(cityId: String)`, `getAllCities(): List<City>`.

---

## Этап 1: Модели данных (Domain Layer) ✅

- [x] `ParkFilter` — добавлено поле `selectedCityId: Int?`, обновлён `isDefault`, написаны тесты
- [x] `ParksFilterDataStore` — добавлен ключ `selectedCityId`, обновлены `save()`/`load()`, написаны тесты
- [x] `FilterParksUseCase` — добавлена фильтрация по `filter.selectedCityId`, написаны тесты

**Файлы:** `ParkFilter.kt`, `ParksFilterDataStore.kt`, `FilterParksUseCase.kt`

---

## Этап 2: UI Layer — ViewModel ✅

### 2.1. Обновить `IParksRootViewModel`

- [x] Добавлены в `ParksRootUiState`: `selectedCity: City?`, `cities: List<City>`, `isLoadingCities: Boolean`, `citySearchQuery: String`, `filteredParks: List<Park>`
- [x] Добавлено computed property `cityNames: List<String>` (derived от `cities` + `citySearchQuery`)
- [x] Добавлены методы: `onSelectCityClick`, `onCitySearchQueryChange`, `onCitySelected`, `onClearCityFilter`
- [x] Добавлена extension function `toItemListUiState()` для конвертации в `ItemListUiState`
- [x] Написать unit-тесты (`ParksRootViewModelTest`: `onCitySelected_*`, `toItemListUiState_*`, `showNoParksFound_*`)

### 2.2. Обновить `ParksRootViewModel`

- [x] Реализованы все методы интерфейса; `cityNames` вычисляется через `cities.filter { ... }.map { it.name }`
- [x] При инициализации: загрузка городов через `countriesRepository.getAllCities()`
- [x] `onCitySelected(cityId: String)`: поиск города по id (`cities.find { it.id == cityId }`) → `city.id.toIntOrNull()` → сохранение в `localFilter` + `selectedCity`, обновление камеры на город, пересчёт `filteredParks` (актуализация: сигнатура переведена с имени города на `cityId: String`, добавлена аналитика `SELECT_PARK_FILTER_CITY`)
- [x] `onClearCityFilter`: сброс `selectedCityId`/`selectedCity` в null, сохранение, пересчёт
- [x] При восстановлении: collector в `init {}` синхронизирует `localFilter` из DataStore, `selectedCity` восстанавливается через `getCityById`
- [x] `onCitySearchQueryChange`: обновляет `citySearchQuery` → пересчёт `cityNames`
- [x] Написать unit-тесты с моком `CountriesRepository`

**Примечание:** Архитектура — `filterParksUseCase` вызывается в ViewModel (MVVM-паттерн проекта), `CountriesRepository` уже существует.

**Файлы:** `IParksRootViewModel.kt`, `ParksRootViewModel.kt`

---

## Этап 3: UI Layer — Компоненты ✅

- [x] `SearchCityButton` — stateless composable (cityName, onClick, onClearClick), unit-тесты написаны
- [x] `ParksRootScreen` — добавлен `SearchCityButton` под TopAppBar, `filteredParks` из ViewModel
- [x] `ItemListScreen` переиспользуется (Вариант B — `ParksRootViewModel` управляет состоянием)

**Важно:** `SearchCityButton.onClearClick` → `viewModel.onClearCityFilter()`, НЕ навигация

**Файлы:** `SearchCityButton.kt`, `ParksRootScreen.kt`

---

## Этап 4: Навигация ✅

- [x] `SelectCityForFilter` добавлен в `Screen` sealed class, `BOTTOM_BAR_HIDDEN_BASE_ROUTES`, `Screen.allScreens`
- [x] Composable в `RootScreen.kt`: `ItemListScreen` с `toItemListUiState()`, `onContactUs` (уже существующий `LocationFeedback`)
- [x] `parentTab = Parks`, `BOTTOM_BAR_HIDDEN_BASE_ROUTES` скрывает bottom nav

**Файлы:** `Destinations.kt`, `RootScreen.kt`

---

## Этап 5: Локализация ✅

- [x] Строка `select_city` уже существует с переводами: `"Select a city"` / `"Выбери город"`

---

## Критерии завершения

- [x] `SearchCityButton` отображается под TopAppBar на `ParksRootScreen` (Этап 3)
- [x] Нажатие открывает `ItemListScreen` (режим CITY) (Этап 4)
- [x] Выбор города сохраняется в `ParksFilterDataStore` (Этап 1, 2)
- [x] `FilterParksUseCase` фильтрует парки по `cityId` (Этап 1)
- [x] Кнопка очистки сбрасывает фильтр города (Этап 3)
- [x] Unit-тесты проходят (`./gradlew :app:testDebugUnitTest`)
- [x] Линт проходит (`make lint`)

---

## Примечание по существующим тестам

- [x] `ParksRootViewModelTest.kt` — добавлены моки `countriesRepository`, обновлены initial state и assertions
- [x] `FilterParksUseCaseTest.kt` — добавлены тест-кейсы для фильтрации по городу (Этап 1)
- [x] `ParkFilter` и `ParksFilterDataStore` тесты обновлены для нового поля (Этап 1)
- [x] `FakeParksRootViewModel` — обновлён для новых методов интерфейса

---

## Этап 6: Исправление бага — фильтры Size/Type теряют selectedCityId ✅

### Баг

`onFilterApply()` сохранял `localFilter` (size+type от диалога) **без** `selectedCityId` — город из `uiState.selectedCity` терялся.

### Архитектура

`localFilter` в `ParksFilterDialog` — только size/type. `onFilterApply()` должен **снаружи** объединить с `selectedCityId` из `uiState.selectedCity`.

### 6.1: Тесты (Red) ✅

**Файлы:** `app/src/test/java/com/swparks/ui/viewmodel/ParksRootViewModelTest.kt`

- [x] Тест 6.1.1 (`onFilterApply_whenCitySelected_savesFilterWithSelectedCityId`) — проверяет что `saveFilter` получает `selectedCityId = 1`
- [x] Тест 6.1.2 (`onFilterApply_whenCitySelectedAndSizeFilterApplied_filtersParksByBoth`) — end-to-end: город + size фильтр вместе, `filteredParks` содержит только park2

### 6.2: Исправление (Green) ✅

**Файл:** `app/src/main/java/com/swparks/ui/viewmodel/ParksRootViewModel.kt`

```kotlin
override fun onFilterApply() {
    val sizeTypeFilter = _uiState.value.localFilter
    val cityId = _uiState.value.selectedCity?.id?.toIntOrNull()
    val finalFilter = sizeTypeFilter.copy(selectedCityId = cityId)
    viewModelScope.launch { parksFilterDataStore.saveFilter(finalFilter) }
    _uiState.value = _uiState.value.copy(showFilterDialog = false)
}
```

### 6.3: Верификация (Refactor) ✅

- [x] Все тесты проходят
- [x] `make lint` ✅ / `make test` ✅

---

## Этап 7: Экран "Парки не найдены" (NoParksFoundView)

### iOS-аналог

`ParksMapScreen.NoParksFoundView` — overlay-карточка поверх контента с парками:
- Показывается когда `showNoParksFound = isFilteredParksEmpty && didParksManagerLoad && !isLoading`
- Две кнопки: "Выбрать другой город" (всегда) + "Изменить фильтры" (только если `isFilterEdited`)

### Архитектура Android

- `showNoParksFound` (computed): `filteredParks.isEmpty() && selectedCity != null && !isLoadingFilter && !isLoadingCities && hasParks`
- `hasParks = allParks.isNotEmpty()` — флаг что парки загружены (добавить в ViewModel)
- `isSizeTypeFilterEdited = ParkFilter(sizes=localFilter.sizes, types=localFilter.types) != ParkFilter()` — **НЕ** `localFilter.isDefault` (иначе кнопка "Изменить фильтры" покажется и при выбранном городе)
- `NoParksFoundView` — overlay на `ParksListView` в `ParksRootScreen`
- Действия: `onNavigateToSelectCity` (параметр composable, навигация) + `onShowFilterDialog()` (ViewModel)

### 7.1: Unit-тесты ViewModel (TDD — Red) ✅

**Файл:** `app/src/test/java/com/swparks/ui/viewmodel/ParksRootViewModelTest.kt`

**Примечание:** `showNoParksFound` и `isSizeTypeFilterEdited` — computed extension properties на `ParksRootUiState`. Тесты проверяют `viewModel.uiState.value.showNoParksFound`.

- [x] Тест: `showNoParksFound = true` когда `filteredParks.isEmpty() && selectedCity != null && hasParks && !isLoadingFilter && !isLoadingCities`
- [x] Тест: `showNoParksFound = false` когда `selectedCity == null`
- [x] Тест: `showNoParksFound = false` когда `hasParks = false` (парки ещё не загружены)
- [x] Тест: `showNoParksFound = false` когда `isLoadingCities = true`
- [x] Тест: `isSizeTypeFilterEdited = true` когда изменены size/type (city дефолтный)
- [x] Тест: `isSizeTypeFilterEdited = false` когда только город выбран (size/type дефолтные)

### 7.2: Instrumented UI-тесты (TDD — Red) ✅

**Файл:** `app/src/androidTest/java/com/swparks/ui/screens/parks/ParksRootScreenTest.kt`

- [x] Тест: `NoParksFoundView` отображается когда `showNoParksFound = true`
- [x] Тест: `NoParksFoundView` НЕ отображается когда `showNoParksFound = false`
- [x] Тест: кнопка "Выбрать другой город" видна всегда когда показана NoParksFoundView
- [x] Тест: кнопка "Изменить фильтры" видна только при `isSizeTypeFilterEdited = true`

### 7.3: Реализация (Green) ✅

**Файл:** `app/src/main/java/com/swparks/ui/screens/parks/NoParksFoundView.kt`

- [x] Создать `NoParksFoundView` composable: карточка с заголовком из `no_parks_found` ("Площадки не найдены")
- [x] Кнопка "Выбрать другой город" (`select_another_city`) — вызывает `onSelectCity`
- [x] Кнопка "Изменить фильтры" (`change_filters`) — вызывает `onOpenFilters` (только при `isSizeTypeFilterEdited`)
- [x] Добавить `showNoParksFound` (computed property) в `ParksRootUiState`
- [x] Добавить `hasParks: Boolean` в `ParksRootUiState` (обновляется в `updateParks()`)

```kotlin
override fun updateParks(parks: List<Park>) {
    allParks = parks
    _uiState.value = _uiState.value.copy(hasParks = parks.isNotEmpty())
    recalculateFilteredParks()
}
```

- [x] Добавить `isSizeTypeFilterEdited` computed в `ParksRootUiState`
- [x] Добавить `NoParksFoundView` overlay в `ParksRootScreen` над `ParksListView`
- [x] Создать string resources: `select_another_city` ("Выбрать другой город" / "Select another city"), `change_filters` ("Изменить фильтры" / "Change filters")

### 7.4: Верификация (Refactor) ✅

- [x] `make lint` ✅ (pre-existing issues only — LongMethod/TooManyFunctions, not introduced by this work)
- [x] `make test` ✅ (`./gradlew :app:testDebugUnitTest` — all tests pass)
- [x] `make build` ✅ (`./gradlew :app:assembleDebug` — BUILD SUCCESSFUL)

---

## Этап 8: Исправление бага — NoParksFoundView не отображается ✅ (реализовано иначе, чем планировалось)

### Баг

`NoParksFoundView` не показывается когда фильтр возвращает 0 парков.

**Причина (исходная):** `RootScreen` передавал `filteredParks` (уже отфильтрованные) в `ParksRootScreen`, из-за чего `hasParks` считался по отфильтрованному набору.

**Как исправлено фактически (актуализация):** `ParksRootScreen` больше не получает парки снаружи — параметр `parks` удалён из его сигнатуры. `ParksRootViewModel` сам подписан на полный поток парков: `observeParks()` в `init {}` собирает `parksEventsRepository.getParksFlow()` и вызывает `updateParks(allParks)`, поэтому `hasParks` считается по всем паркам, а `filteredParks` — внутри ViewModel через `FilterParksUseCase`. Счётчик в `RootScreen` берётся из `parksRootUiState.filteredParks.size`.

### 8.1: Unit-тесты (TDD — Red)

**Файл:** `app/src/test/java/com/swparks/ui/viewmodel/ParksRootViewModelTest.kt`

- [x] Частично покрыто существующими тестами: `init_whenParksFlowEmits_updatesUiStateFromLocalStorage` проверяет `hasParks` и `filteredParks` после эмита потока парков
- [ ] Выделенные тесты из плана не написаны: `updateParks_whenCalledWithAllParks_setsHasParksTrue`, `updateParks_whenCalledWithEmptyList_setsHasParksFalse`, `showNoParksFound_whenAllParksLoadedButFilterReturnsEmpty_showsNoParksFound`

### 8.2: Исправление (Green) ✅

- [x] Выполнено через перевод ViewModel на самостоятельную подписку на полный поток парков (см. описание выше)

### 8.3: Верификация (Refactor)

- [ ] Ручное тестирование: выбрать город + строгий фильтр → NoParksFoundView отображается (из содержимого репозитория не подтверждается)

---

## Этап 9: Исправление бага — выбранный город не отмечен на экране выбора города ✅

### Баг

Когда отображается `NoParksFoundView` и пользователь нажимает кнопку "Выбрать другой город", открывается `ItemListScreen` (режим CITY), но выбранный город **не отмечен**. Пользователь может выбрать тот же город повторно.

### Причина

В `ParksRootViewModel.toItemListUiState()`:

```kotlin
fun ParksRootUiState.toItemListUiState(): ItemListUiState = ItemListUiState(
    ...
    selectedItem = null,  // <-- должен передавать выбранный город
    ...
)
```

### Архитектура

- `selectedItem: String?` в `ItemListUiState` — имя выбранного элемента
- Внутри `ItemsList` уже реализована логика блокировки нажатий на выбранном элементе (isSelected)
- Нужно передать `selectedCity?.name` вместо `null`

### 9.1: Unit-тесты (TDD — Red) ✅

**Файл:** `app/src/test/java/com/swparks/ui/viewmodel/ParksRootViewModelTest.kt`

- [x] Тест: `toItemListUiState_whenCitySelected_setsSelectedItemToCityName` — проверяет что `selectedItem = selectedCity?.id` (актуализация: тест носит историческое имя, но проверяет id города, а не имя)
- [x] Тест: `toItemListUiState_whenNoCitySelected_setsSelectedItemToNull` — проверяет что `selectedItem = null` когда город не выбран

### 9.2: Исправление (Green) ✅

**Файл:** `app/src/main/java/com/swparks/ui/viewmodel/IParksRootViewModel.kt`

```kotlin
fun ParksRootUiState.toItemListUiState(): ItemListUiState =
    ItemListUiState(
        mode = ItemListMode.CITY,
        items =
            cities
                .filter { it.name.contains(citySearchQuery, ignoreCase = true) }
                .map { SelectableItem(it.id, it.name) },
        selectedItem = selectedCity?.id,
        searchQuery = citySearchQuery,
        isEmpty = false
    )
```

Актуализация: после этапа 1 плана `docs/plan-fix-crashes-maplibre-and-duplicate-keys.md` элементы переведены на `SelectableItem(id, name)`, а `selectedItem` хранит `id` города (не имя) — это также снимает коллизию ключей `LazyColumn` при дублях имён городов.

### 9.3: Верификация (Refactor) ✅

- [x] `make lint` ✅ (только pre-existing markdown warnings)
- [x] `make test` ✅
- [ ] Ручное тестирование: NoParksFoundView → "Выбрать другой город" → виден выбранный город с галочкой
