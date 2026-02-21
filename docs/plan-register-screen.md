# План реализации экрана регистрации (RegisterUserScreen)

## Описание задачи

Реализовать экран регистрации `RegisterUserScreen` в Android-приложении по аналогии с iOS (коммит `990c88c0`, экран `AccountInfoView(mode: .create)`).

---

## Статус реализации

### ✅ Этап 1: Строковые ресурсы

**Реализовано полностью.** Добавлены строки в `values/strings.xml` и `values-ru/strings.xml`:
- `registration` - заголовок экрана
- `register` - кнопка
- `i_accept_terms` - начало фразы принятия соглашения
- `user_agreement` - ссылка на соглашение
- `user_agreement_url` - URL соглашения
- `minimum_age_13` - ошибка возраста
- `registration_success` / `registration_error` - сообщения

---

### ✅ Этап 2: ViewModel

#### ✅ 2.1. RegisterForm

**Файл:** `app/src/main/java/com/swparks/ui/model/RegisterForm.kt`

Реализован с валидацией:
- `login`, `email`, `password`, `fullName`, `genderCode`, `birthDate`, `countryId`, `cityId`, `isPolicyAccepted`
- Свойство `isValid: Boolean` - проверяет все поля + email валидность + возраст 13+
- Минимальная длина пароля: 6 символов

#### ✅ 2.2. UI State

**Файл:** `app/src/main/java/com/swparks/ui/state/RegisterUiState.kt`

Реализован sealed class:
- `Idle` - начальное состояние
- `Loading` - загрузка
- `Success(userId)` - успех
- `Error(message, exception)` - ошибка

**Дополнительно:** `app/src/main/java/com/swparks/ui/state/RegisterEvent.kt` - события для одноразовых операций

#### ✅ 2.3. Interface

**Файл:** `app/src/main/java/com/swparks/ui/viewmodel/IRegisterViewModel.kt`

Интерфейс включает:
- `uiState`, `registerEvents`, `form`, `countries`, `cities`, `selectedCountry`, `selectedCity`
- `loginError`, `emailError`, `passwordError`, `birthDateError`
- Методы для обновления полей и регистрации

#### ✅ 2.4. Implementation

**Файл:** `app/src/main/java/com/swparks/ui/viewmodel/RegisterViewModel.kt`

Реализован с зависимостями:
- `SWRepository`, `SecureTokenRepository`, `UserPreferencesRepository`, `TokenEncoder`, `CountriesRepository`

Логика регистрации:
1. Создает `RegistrationRequest` из формы
2. Вызывает `SWRepository.register(request)`
3. При успехе: сохраняет токен и userId, отправляет событие `Success`
4. При ошибке: устанавливает `Error` состояние

---

### ✅ Этап 3: UI экрана

**Файл:** `app/src/main/java/com/swparks/ui/screens/auth/RegisterUserScreen.kt`

**Реализовано:**
- Scaffold с topBar (заголовок + крестик закрытия)
- Форма с вертикальным скроллом
- BottomBar с PolicyToggle и кнопкой регистрации
- LoadingOverlay при загрузке
- Обработка событий через `LaunchedEffect`

**Переиспользованы компоненты:**
- LoginField, EmailField, PasswordField, FullNameField (SWTextField)
- GenderRadioButtons (RadioButton из ThemeIconScreen)
- BirthdayPicker (SWDateTimePicker с ограничением 13+)
- CountryPicker, CityPicker (ListRowView с навигацией)
- PolicyToggle (Switch + AnnotatedString со ссылкой)

---

### ✅ Этап 4: Навигация внутри sheet

**Файл:** `app/src/main/java/com/swparks/ui/screens/auth/RegisterSheetHost.kt`

**Архитектура навигации:**

```
RegisterSheetHost
├── NavHost (registerNavHost)
│   ├── register (RegisterUserScreen)
│   ├── select_country (RegisterSelectCountryScreen)
│   └── select_city (RegisterSelectCityScreen)
```

**Преимущества реализованного подхода:**
- Sheet не закрывается при навигации
- Можно вернуться назад на экран регистрации
- Сохраняется состояние формы при выборе страны/города

**Файлы экранов выбора:**
- `RegisterSelectCountryScreen.kt` - выбор страны с поиском
- `RegisterSelectCityScreen.kt` - выбор города с поиском

---

### ✅ Этап 5: Интеграция

**RegisterSheetHost** - реализован как ModalBottomSheet:
- Блокировка закрытия по свайпу вниз (через `NestedScrollConnection`)
- Блокировка закрытия по системной кнопке "назад"
- Закрытие только по крестику или после успешной регистрации

**RootScreen.kt** - интегрирован RegisterSheetHost:
- Состояние `showRegisterSheet`
- Callback `onRegisterSuccess` загружает профиль и диалоги

**IncognitoProfileView.kt** - добавлена кнопка "Регистрация":
- Параметр `onClickRegister`
- Открывает RegisterSheetHost

---

### ✅ Этап 6: DI контейнер

**AppContainer.kt** - добавлен factory метод:

```kotlin
fun registerViewModelFactory(): RegisterViewModel
```

---

### ✅ Этап 7: Тестирование

**Файл:** `app/src/test/java/com/swparks/ui/viewmodel/RegisterViewModelTest.kt`

**Покрытие тестами:**
- ✅ Валидация полей (login, email, password, age 13+)
- ✅ Успешная регистрация с проверкой сохранения токена и userId
- ✅ Ошибки регистрации (сетевая ошибка)
- ✅ Выбор страны/города
- ✅ Валидация возраста (минимум 13 лет)
- ✅ Сброс состояния через `resetForNewSession()`

---

### ✅ Этап 8: Финализация

- [x] Локализация (EN/RU) - реализовано
- [x] Тёмная/светлая тема - использует Material3 тему
- [x] Портретная/ландшафтная ориентация - поддерживается через скролл
- [x] Код отформатирован - `make format` выполнен

---

### ✅ Этап 9: Исправление найденных проблем

#### 9.1. Блокировка свайпа sheet ✅

**Решение:** Использован `NestedScrollConnection` который блокирует только свайп вниз для закрытия sheet, разрешая скролл контента.

#### 9.2. Выбор страны и города ✅

**Решение:** Реализована внутренняя навигация внутри sheet через NavHost с экранами:
- `RegisterSelectCountryScreen` - выбор страны с поиском
- `RegisterSelectCityScreen` - выбор города с поиском

CountryPicker и CityPicker используют `clickable` модификатор с `ListRowView` и `showChevron = true`.

---

## Критерии приемки

- [x] Экран открывается из IncognitoProfileView
- [x] Все поля работают и валидируются
- [x] Кнопка недоступна при неполной форме
- [x] Радио-кнопки пола работают
- [x] DatePicker ограничивает возраст 13+
- [x] Toggle со ссылкой на соглашение над кнопкой регистрации
- [x] Регистрация через API успешна
- [x] Токен сохраняется после регистрации
- [x] userId сохраняется после регистрации
- [x] Профиль загружается после регистрации
- [x] Диалоги загружаются после регистрации
- [x] Закрытие по крестику в topBar
- [x] Навигация на выбор страны/города работает
- [x] Sheet нельзя потянуть вниз для закрытия (только крестик)
- [x] Скролл контента внутри sheet работает корректно
- [x] Выбор страны открывает экран выбора страны
- [x] Выбор города доступен после выбора страны и открывает экран выбора города
- [x] Шеврон отображается для CountryPicker и CityPicker
- [x] Тесты проходят
- [x] Код отформатирован

---

## Реализованные файлы

### UI Models & State

- `app/src/main/java/com/swparks/ui/model/RegisterForm.kt`
- `app/src/main/java/com/swparks/ui/state/RegisterUiState.kt`
- `app/src/main/java/com/swparks/ui/state/RegisterEvent.kt`

### ViewModel

- `app/src/main/java/com/swparks/ui/viewmodel/IRegisterViewModel.kt`
- `app/src/main/java/com/swparks/ui/viewmodel/RegisterViewModel.kt`

### UI Screens

- `app/src/main/java/com/swparks/ui/screens/auth/RegisterUserScreen.kt`
- `app/src/main/java/com/swparks/ui/screens/auth/RegisterSheetHost.kt`
- `app/src/main/java/com/swparks/ui/screens/auth/RegisterSelectCountryScreen.kt`
- `app/src/main/java/com/swparks/ui/screens/auth/RegisterSelectCityScreen.kt`

### Tests

- `app/src/test/java/com/swparks/ui/viewmodel/RegisterViewModelTest.kt`

### Resources

- `app/src/main/res/values/strings.xml` (строки EN)
- `app/src/main/res/values-ru/strings.xml` (строки RU)

---

---

## 🔧 Этап 10: Исправление найденных багов

### 10.1. Скролл контента в RegisterSheetHost ✅

**Проблема:** Скролл контента не работает, но жесты свайпа для закрытия sheet работают (можно потянуть вниз и окно идет вниз).

**Причина:** `NestedScrollConnection.onPreScroll` блокирует свайп вниз (positive y), что предотвращает закрытие sheet, но также блокирует корректную работу скролла контента.

**Решение:** Убран `nestedScroll` модификатор и `NestedScrollConnection`. Оставлен только `confirmValueChange` для блокировки скрытия sheet. Это позволяет:
- Скроллу контента работать нормально
- Блокировать закрытие sheet через `confirmValueChange`

**Файл:** `app/src/main/java/com/swparks/ui/screens/auth/RegisterSheetHost.kt`

### 10.2. Загрузка списка стран ✅

**Проблема:** Список стран на экране регистрации пустой при первом открытии. После авторизации и выхода - список появляется.

**Причина:** `CountriesRepositoryImpl.getCountriesFlow()` не триггерит загрузку данных из assets. Загрузка происходит только при вызове `getCountryById`, `getCityById`, `getCitiesByCountry`.

**Решение:**
1. ✅ Добавлен метод `ensureCountriesLoaded()` в интерфейс `CountriesRepository`
2. ✅ Реализован метод в `CountriesRepositoryImpl` - вызывает `loadCountriesFromAssets()`
3. ✅ Вызывается `ensureCountriesLoaded()` в `RegisterViewModel.init` перед подпиской на Flow

**Файлы:**
- `app/src/main/java/com/swparks/domain/repository/CountriesRepository.kt`
- `app/src/main/java/com/swparks/data/repository/CountriesRepositoryImpl.kt`
- `app/src/main/java/com/swparks/ui/viewmodel/RegisterViewModel.kt`

---

## Критерии приемки (этап 10)

- [x] Скролл контента в RegisterUserScreen работает корректно
- [x] Sheet нельзя закрыть свайпом вниз (только крестик)
- [x] Список стран загружается при первом открытии регистрации
- [x] Выбор страны/города работает без предварительной авторизации

---

---

## 🔧 Этап 11: Выбор города без выбранной страны

### 11.1. Логика выбора города

**Требование:** Если страна еще не выбрана, пользователь может перейти на экран выбора города и увидеть ВСЕ города для всех стран одним списком. При выборе города автоматически выбирается страна этого города. При следующем открытии экрана выбора города - показываются города только для выбранной страны.

**Реализация:**
1. ✅ Добавить метод `getAllCities()` в CountriesRepository - возвращает все города из всех стран
2. ✅ Добавить метод `getCountryForCity(cityId: String)` в CountriesRepository - возвращает страну для города
3. ✅ Обновить ViewModel:
   - Добавить `allCities: StateFlow<List<City>>` для хранения всех городов
   - Метод `loadAllCities()` - загружает все города
   - Обновить `onCitySelectedByName()` - при выборе города автоматически выбирать страну
4. ✅ Обновить RegisterSelectCityScreen:
   - Если страна не выбрана - показывать все города
   - Если страна выбрана - показывать города только этой страны
5. ✅ Добавить тесты для новой логики
6. ✅ Запустить форматирование и тесты

**Статус: ЗАВЕРШЕНО**

**Файлы для изменения:**
- `app/src/main/java/com/swparks/domain/repository/CountriesRepository.kt`
- `app/src/main/java/com/swparks/data/repository/CountriesRepositoryImpl.kt`
- `app/src/main/java/com/swparks/ui/viewmodel/IRegisterViewModel.kt`
- `app/src/main/java/com/swparks/ui/viewmodel/RegisterViewModel.kt`
- `app/src/main/java/com/swparks/ui/screens/auth/RegisterSelectCityScreen.kt`
- `app/src/test/java/com/swparks/ui/viewmodel/RegisterViewModelTest.kt`

---

## Критерии приемки (этап 11)

- [x] Если страна не выбрана, при переходе на экран выбора города показываются все города
- [x] При выборе города из общего списка автоматически выбирается страна
- [x] После выбора страны, на экране выбора города показываются только города этой страны
- [x] Тесты проходят
- [x] Код отформатирован

---

## Задача завершена ✅

Экран регистрации полностью реализован и исправлен.

**Дата завершения:** Февраль 2026
