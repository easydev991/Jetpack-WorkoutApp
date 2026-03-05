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

## Этап 5: Feedback ✅

**Реализовано:**
- `util/Feedback.kt` — объект с recipients (<info@workout.su>, <easy_dev991@mai.ru>)
- `util/AppVersionProvider.kt` — getVersion(), getVersionCode() с PackageInfoCompat
- `util/LocationFeedback.kt` — sealed class Country/City с companion object factory methods
- `FeedbackSender.sendLocationFeedback()` — ACTION_SENDTO + mailto: + ActivityNotFoundException + Toast
- `SelectCountryScreen.kt` — интеграция с LocalContext.current
- `SelectCityScreen.kt` — интеграция с LocalContext.current
- `res/values/strings.xml` — 5 ключей EN
- `res/values-ru/strings.xml` — 5 ключей RU
- `Feedback.recipients` используется консистентно во всех методах FeedbackSender

**Ключевые решения:**
- `ACTION_SENDTO` + `mailto:` вместо `ACTION_SEND` — консистентно с iOS
- Unit-тесты для LocationFeedback/AppVersionProvider требуют Robolectric — отложены в пользу ручного тестирования

---

## Критерии завершения

### Этапы 1-4 ✅

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

### Этап 5: Feedback ✅

- [x] Кнопка "Написать нам" открывает почтовый клиент (ACTION_SENDTO + mailto:)
- [x] Subject письма корректный (Country/City)
- [x] Body письма содержит Android SDK, версию приложения, вопрос
- [x] Toast "Почтовый клиент не установлен" если нет email-приложения
- [x] `Feedback.recipients` используется консистентно во всех методах FeedbackSender
- [x] Строковые ресурсы без trailing spaces
- [x] План актуализирован (Этап 5 → ✅)
- [x] Ручное тестирование на устройстве (открытие email клиента)
- [x] Ручное тестирование на устройстве (Toast при отсутствии email-приложения)

---

## Тестирование

### Автоматические тесты

- **UI тесты:** Отсутствуют (ItemListScreenTest.kt не создан)
- **Unit-тесты:** Отложены для LocationFeedback/AppVersionProvider (требуют Robolectric)

### Ручное тестирование ✅

- [x] Экран выбора страны отображает список стран
- [x] Экран выбора города отображает список городов
- [x] Поиск фильтрует список
- [x] Empty state с кнопкой "Написать нам"
- [x] Кнопка "Написать нам" открывает почтовый клиент
- [x] Toast при отсутствии email-приложения
