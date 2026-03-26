# Реализация табов на экране ParksRootScreen

## Этап 1: Модель данных (UI Model)

- [x] Создать enum `ParksTab` в `app/src/main/java/com/swparks/ui/model/ParksTab.kt` (аналогично `EventKind`)
  - MAP (R.string.parks_map)
  - LIST (R.string.parks_list)
- [x] Написать unit-тесты в `app/src/test/java/com/swparks/ui/model/ParksTabTest.kt`
  - Тест получения строкового ресурса
  - Тест entries

## Этап 2: Интерфейс IParksRootViewModel (UI Layer)

- [x] Добавить property `selectedTab: StateFlow<ParksTab>` в `IParksRootViewModel`
- [x] Добавить метод `onTabSelected(tab: ParksTab)` в `IParksRootViewModel`

## Этап 3: ParksRootViewModel (UI Layer)

- [x] Добавить `selectedTab: MutableStateFlow<ParksTab> = MutableStateFlow(ParksTab.LIST)` (по умолчанию — Список)
- [x] Реализовать `onTabSelected(tab: ParksTab)` — обновление состояния

## Этап 4: ParksRootScreen — TabRow (UI Layer)

- [x] Добавить импорты:
  - PrimaryTabRow`,`Tab` из material3
  - `stringResource` для parks_map и parks_list
- [x] Создать `ParksTabRow` composable (по аналогии с `EventsTabRow`)
  - PrimaryTabRow с отступами (spacing_regular, spacing_small)
  - Два Tab: MAP и LIST
  - Табы всегда enabled
  - `LoadingOverlayView()` продолжает блокировать весь `ParksRootScreen` при `isGettingLocation == true`, независимо от выбранного таба
- [x] Добавить `ParksTabRow` в Column между SearchCityButton и контентом

## Этап 5: ParksRootScreen — Контент (UI Layer)

- [x] Добавить `selectedTabIndex`:

  ```kotlin
  val selectedTabIndex = selectedTab.ordinal
  ```

- [x] Изменить content в Column:
  - Под SearchCityButton добавить ParksTabRow
  - При `selectedTab == ParksTab.MAP` — показывать placeholder-заглушку для карты
  - Добавить для placeholder понятную семантику для UI-тестов (`testTag` или текст)
  - При `selectedTab == ParksTab.LIST` — показывать текущее содержимое (ParksListView или NoParksFoundView)

## Этап 6: Тестирование

- [x] Написать unit-тесты для `ParksRootViewModel` в `app/src/test/java/com/swparks/ui/viewmodel/ParksRootViewModelTest.kt`
  - Тест начального selectedTab (LIST)
  - Тест переключения на MAP
  - Тест переключения на LIST
- [x] Обновить `FakeParksRootViewModel` в `app/src/androidTest/java/com/swparks/ui/viewmodel/FakeParksRootViewModel.kt`
  - Добавить `selectedTab: MutableStateFlow<ParksTab>`
  - Добавить `onTabSelected(tab: ParksTab)`
- [x] Написать UI-тесты для `ParksRootScreen` в `app/src/androidTest/java/com/swparks/ui/screens/parks/ParksRootScreenTest.kt`
  - Тест отображения табов
  - Тест переключения табов
  - Тест показа placeholder-заглушки при выборе MAP
  - Тест показа списка при выборе LIST

## Этап 7: Локализация

- [x] Проверить наличие строк parks_map и parks_list (уже есть в values/strings.xml и values-ru/strings.xml)
- [x] Добавить строку `map_coming_soon` в values/strings.xml и values-ru/strings.xml

## Структура изменений

```
app/src/main/java/com/swparks/ui/model/ParksTab.kt (новый)
  enum class ParksTab(@StringRes val description: Int) {
    MAP(R.string.parks_map),
    LIST(R.string.parks_list)
  }

IParksRootViewModel.kt
  + selectedTab: StateFlow<ParksTab>
  + onTabSelected(tab: ParksTab)

ParksRootViewModel.kt
  + selectedTab: MutableStateFlow<ParksTab> = MutableStateFlow(ParksTab.LIST)
  + onTabSelected(tab: ParksTab)

app/src/androidTest/java/com/swparks/ui/viewmodel/FakeParksRootViewModel.kt
  + selectedTab: MutableStateFlow<ParksTab>
  + onTabSelected(tab: ParksTab)

ParksRootScreen.kt
  + ParksTabRow composable
  + TabRow между SearchCityButton и контентом
  + selectedTab.collectAsState()
  + when(selectedTab) -> MAP -> placeholder карты, LIST -> ParksListView / NoParksFoundView
  + LoadingOverlayView продолжает блокировать весь экран поверх любого таба
```

## Зависимости

```
ParksTab.kt (новый enum)
    ↓
IParksRootViewModel.kt (+ selectedTab, onTabSelected)
    ↓
ParksRootViewModel.kt (реализация)
    ↓
ParksRootScreen.kt (TabRow + контент)
```
