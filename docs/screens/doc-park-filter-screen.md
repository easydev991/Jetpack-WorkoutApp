# Фильтрация площадок (Parks Filter)

## Обзор

Мультиселект-фильтр для площадок по размеру и типу. Реализован как аналог iOS `ParkFilterScreen`.

**Исходные данные:** 9000+ площадок — фильтрация O(1) по Set.
**Стек:** Jetpack Compose, `JournalSettingsDialog` как шаблон, `ParkSize`/`ParkType` enums, `CheckmarkRowView`.

---

## Архитектура

```
ParksFilterDialog → ParksRootViewModel → ParksFilterDataStore → FilterParksUseCase → ParkFilter
```

### Слои

| Слой   | Компонент              | Описание                                                           |
|--------|------------------------|--------------------------------------------------------------------|
| UI     | `ParksFilterDialog`    | Диалог с секциями Size/Type, кнопки Reset/Apply                    |
| UI     | `ParksRootScreen`      | `filteredParks` через `derivedStateOf`                             |
| UI     | `CheckmarkRowView`     | Галка с опциональным `onCheckedChange`                             |
| Domain | `ParkFilter`           | `sizes: Set<ParkSize>`, `types: Set<ParkType>`                     |
| Domain | `IFilterParksUseCase`  | `invoke(allParks, filter)` → `List<Park>`, Set lookup O(1)         |
| Domain | `FilterParksUseCase`   | Реализация с Set lookup по `rawValue`                              |
| Data   | `ParksFilterDataStore` | DataStore persistence, `saveFilter()` / `filter: Flow<ParkFilter>` |
| Data   | `AppContainer`         | DI: `filterParksUseCase`, `parksFilterDataStore`                   |

---

## Модель фильтра

```kotlin
data class ParkFilter(
    val sizes: Set<ParkSize> = ParkSize.entries.toSet(),
    val types: Set<ParkType> = ParkType.entries.toSet()
) {
    val isDefault: Boolean get() = this == ParkFilter()
}
```

- По умолчанию **все** размеры и типы выбраны (фильтрация не применяется)
- `isDefault` — проверка, является ли фильтр дефолтным
- Минимальное количество выбранных элементов — **1** (нельзя сбросить все)
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

**Методы**:
- `onLocalFilterChange(filter)` — обновить локальный фильтр
- `onFilterToggleSize(size)` / `onFilterToggleType(type)` — toggle с валидацией (мин. 1 элемент)
- `onFilterReset()` — сбросить фильтр до дефолтного
- `onFilterApply()` — применить фильтр и сохранить в DataStore
- `onShowFilterDialog()` / `onDismissFilterDialog()` — открыть/закрыть диалог

### Производительность

- `derivedStateOf` для `filteredParks` — фильтрация не на каждый recompose
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
| `domain/usecase/IFilterParksUseCase.kt`     | Интерфейс use case      |
| `domain/usecase/FilterParksUseCase.kt`      | Реализация use case     |
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
5. `ParksRootScreen` получает `parks: List<Park>` извне (от `RootScreen`), фильтрация происходит в `RootScreen`
