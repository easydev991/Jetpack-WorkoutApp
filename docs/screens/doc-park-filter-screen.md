# Фильтрация площадок (Parks Filter)

## Обзор

Мультиселект-фильтр для площадок по размеру, типу и городу. Реализован как аналог iOS `ParkFilterScreen`.

**Исходные данные:** 9000+ площадок — фильтрация O(1) по Set.
**Стек:** Jetpack Compose, `JournalSettingsDialog` как шаблон, `ParkSize`/`ParkType` enums, `CheckmarkRowView`, `ItemListScreen` для выбора города.

---

## Архитектура

```
ParksFilterDialog → ParksRootViewModel → ParksFilterDataStore → FilterParksUseCase → ParkFilter
```

### Слои

| Слой   | Компонент              | Описание                                                                    |
|--------|------------------------|-----------------------------------------------------------------------------|
| UI     | `ParksFilterDialog`    | Диалог с секциями Size/Type, кнопки Reset/Apply                             |
| UI     | `ParksRootScreen`      | `filteredParks` из `ParksRootUiState` (пересчитывается во ViewModel)        |
| UI     | `ItemListScreen`       | Переиспользуемый экран выбора города (режим `CITY`)                         |
| UI     | `CheckmarkRowView`     | Галка с опциональным `onCheckedChange`                                      |
| Domain | `ParkFilter`           | `sizes: Set<ParkSize>`, `types: Set<ParkType>`, `selectedCityId: Int?`      |
| Domain | `FilterParksUseCase`   | `invoke(allParks, filter)` → `List<Park>`, Set lookup O(1) + фильтр по городу |
| Data   | `ParksFilterDataStore` | DataStore persistence, `saveFilter()` / `filter: Flow<ParkFilter>`          |
| Data   | `AppContainer`         | DI: `filterParksUseCase`, `parksFilterDataStore`                            |

---

## Модель фильтра

```kotlin
data class ParkFilter(
    val sizes: Set<ParkSize> = ParkSize.entries.toSet(),
    val types: Set<ParkType> = ParkType.entries.toSet(),
    val selectedCityId: Int? = null
) {
    val isDefault: Boolean get() = this == ParkFilter()
}
```

- По умолчанию **все** размеры и типы выбраны, город не выбран (фильтрация не применяется)
- `isDefault` — проверка, является ли фильтр дефолтным
- Минимальное количество выбранных элементов — **1** (нельзя сбросить все)
- `selectedCityId` — фильтр по городу (`null` = все города); выбор города — через переиспользуемый `ItemListScreen`
- Фильтр применяется к **исходному списку** parks, а не к предварительно отфильтрованному

---

## UI компоненты

### ParksFilterDialog

Аналог `JournalSettingsDialog`. Содержит:
- Заголовок + кнопка ✕
- Секции Size и Type с `CheckmarkRowView`
- Кнопки Reset и Apply

### ViewModel

`IParksRootViewModel` / `ParksRootViewModel` расширен методами фильтра:

**UI State** (`ParksRootUiState`):
- `showFilterDialog: Boolean` — видимость диалога
- `localFilter: ParkFilter` — локальное состояние в диалоге
- `isLoadingFilter: Boolean` — загрузка фильтра из DataStore
- `selectedCity: City?` / `cities: List<City>` / `citySearchQuery: String` — фильтр по городу

**Методы**:
- `onLocalFilterChange(filter)` — обновить локальный фильтр
- `onFilterToggleSize(size)` / `onFilterToggleType(type)` — toggle с валидацией (мин. 1 элемент)
- `onFilterReset()` — сбросить фильтр до дефолтного
- `onFilterApply()` — применить фильтр и сохранить в DataStore
- `onShowFilterDialog()` / `onDismissFilterDialog()` — открыть/закрыть диалог
- `onSelectCityClick()` / `onCitySelected(cityId)` / `onClearCityFilter()` — выбор/сброс города
- `onCitySearchQueryChange(query)` — поиск по городам

### Производительность

- `filteredParks` пересчитывается в `ParksRootViewModel.recalculateFilteredParks()` (размер/тип → город → отсечение парков с невалидными координатами), а не на каждый recompose
- Set lookup O(1) по `rawValue`

---

## Persistence

`ParksFilterDataStore` сохраняет фильтр между сессиями:
- `saveFilter(filter: ParkFilter)`
- `filter: Flow<ParkFilter>`

---

## Previews

| Preview                                | Описание                                                   |
|----------------------------------------|------------------------------------------------------------|
| `ParksFilterDialogPreviewDefault`      | Light theme, default filter                                |
| `ParksFilterDialogPreviewDark`         | Dark theme, default filter                                 |
| `ParksFilterDialogPreviewCustomFilter` | Custom filter (sizes: SMALL, LARGE; types: SOVIET, MODERN) |

---

## Файлы

| Файл                                        | Назначение              |
|---------------------------------------------|-------------------------|
| `data/model/ParkFilter.kt`                  | Модель фильтра          |
| `domain/usecase/FilterParksUseCase.kt`      | Use case фильтрации     |
| `ui/screens/settings/ItemListScreen.kt`     | Экран выбора города     |
| `data/preferences/ParksFilterDataStore.kt`  | Persistence             |
| `ui/ds/CheckmarkRowView.kt`                 | Компонент галки         |
| `ui/screens/parks/ParksFilterDialog.kt`     | Диалог фильтра          |
| `ui/screens/parks/ParksFilterDialogTest.kt` | Instrumented тесты      |
| `ui/viewmodel/IParksRootViewModel.kt`       | Интерфейс ViewModel     |
| `ui/viewmodel/ParksRootViewModel.kt`        | Реализация ViewModel    |
| `ui/screens/RootScreen.kt`                  | Интеграция в RootScreen |
| `data/AppContainer.kt`                      | DI container            |
| `res/values/strings.xml`                    | Строки                  |
| `res/values-ru/strings.xml`                 | Строки (ru)             |

### Тесты

| Тест                          | Покрытие                                          |
|-------------------------------|---------------------------------------------------|
| `ParkFilterTest.kt`           | default filter, equals, performance (9000 парков) |
| `FilterParksUseCaseTest.kt`   | Set lookup                                        |
| `ParksFilterDataStoreTest.kt` | save/load default и custom filter                 |
| `ParksRootViewModelTest.kt`   | toggle size/type, apply                           |
| `ParksFilterDialogTest.kt`    | render, toggle, reset, apply (androidTest)        |

---

## Допущения

1. Фильтр применяется к **исходному списку** parks
2. Фильтр **персистентен** — сохраняется между сессиями
3. По умолчанию **все** размеры и типы выбраны
4. Минимальное количество выбранных элементов — **1**
5. `ParksRootViewModel` сам наблюдает список площадок (`ParksEventsRepository.getParksFlow()`); фильтрация выполняется во ViewModel (`recalculateFilteredParks`), результат кладётся в `ParksRootUiState.filteredParks`
