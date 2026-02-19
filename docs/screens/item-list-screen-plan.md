# План: Переиспользуемый экран выбора страны/города (ItemListScreen)

## Обзор

Реализовать переиспользуемый экран для выбора страны или города из списка. Аналог iOS `ItemListScreen.swift`.

### Референсы

- **iOS ItemListScreen**: `SwiftUI-WorkoutApp/Libraries/SWDesignSystem/Sources/SWDesignSystem/Public/ItemListScreen.swift`
- **iOS EditProfileLocations**: `SwiftUI-WorkoutApp/Libraries/SWModels/Sources/SWModels/EditProfileLocations.swift`

### Существующие компоненты Android

- `CheckmarkRowView`, `CountriesRepository`, `EditProfileViewModel`

---

## Этап 1: Domain Layer - EditProfileLocations ✅

**Реализовано:**
- `EditProfileLocations.kt` - модель с методами `selectCountry()`, `selectCity()`, `fromCountries()`
- `EditProfileLocationsTest.kt` - 11 unit-тестов (isEmpty, selectCountry*, selectCity*)

---

## Этап 2: Presentation Layer - ItemListScreen ✅

**Реализовано:**
- `ItemListMode.kt` - enum (COUNTRY, CITY) с titleResId, helpMessageResId
- `ItemListUiState.kt` - pure data class (mode, items, selectedItem, searchQuery, isEmpty)
- `ItemListScreen.kt` - stateless composable с SearchBar, LazyColumn, EmptyStateView
- Wrapper экраны: `SelectCountryScreen.kt`, `SelectCityScreen.kt` с локальным состоянием поиска

**Архитектура:**
- State Hoisting: поиск через `rememberSaveable` в wrapper экранах
- Single Source of Truth: данные из `EditProfileViewModel`
- Stateless ItemListScreen: переиспользуемый компонент

---

## Этап 3: Интеграция с EditProfileScreen ✅

### 3.1 ViewModel

**Реализовано:**
- `EditProfileViewModel.kt` - методы `onCountrySelected()`, `onCitySelected()`
- `IEditProfileViewModel.kt` - интерфейс обновлён
- `EditProfileViewModelSelectionTest.kt` - 5 unit-тестов

### 3.2 Навигация

**Реализовано в `RootScreen.kt`:**
- `EditProfileViewModel` создаётся на уровне RootScreen (shared между экранами)
- Добавлены composable для `SelectCountryScreen` и `SelectCityScreen`
- Добавлены колбэки навигации в `EditProfileScreen`

---

## Этап 4: Локализация ✅

**Реализовано:**
- `values/strings.xml`: search, help_country_not_found, help_city_not_found, contact_us
- `values-ru/strings.xml`: русские переводы

---

## Этап 5: Feedback (опционально)

- [ ] Реализовать отправку feedback при нажатии "Написать нам" (email клиент)

---

## Критерии завершения

- [x] Все unit-тесты проходят
- [x] Проект собирается без ошибок
- [x] `make format` не показывает новых предупреждений
- [x] Экран выбора страны отображает список стран с галочкой напротив выбранной
- [x] Экран выбора города отображает список городов с галочкой напротив выбранной
- [x] Поиск фильтрует список
- [x] При отсутствии результатов отображается empty state с кнопкой "Написать нам"
- [x] При выборе страны/города происходит возврат на EditProfileScreen
- [x] Выбранные страна/город корректно отображаются на EditProfileScreen
- [x] Локализация работает для RU и EN
