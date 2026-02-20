# План: Экран редактирования профиля (EditProfileScreen)

## Обзор

Экран для изменения личных данных пользователя. Реализован по образцу iOS-версии `EditProfileScreen.swift`.

### Референсы

- **iOS**: `SwiftUI-WorkoutApp/Screens/Profile/EditProfile/EditProfileScreen.swift`
- **Android**: `app/src/main/java/com/swparks/ui/screens/profile/EditProfileScreen.kt`

---

## Архитектура экрана

### Структура UI

```
EditProfileScreen
├── Scaffold
│   ├── topBar = TopAppBar (заголовок + стрелка назад)
│   ├── content = Column + verticalScroll + imePadding
│   │   ├── AvatarSection (AsyncImage + SWButton)
│   │   ├── FormCardContainer (текстовые поля)
│   │   ├── ChangePasswordButton (ListRowView)
│   │   └── FormCardContainer (пикеры)
│   ├── bottomBar = SWButton ("Сохранить")
│   └── LoadingOverlayView (при isSaving)
```

### Компоненты дизайн-системы

| Компонент | Файл | Назначение |
|-----------|------|------------|
| `SWTextField` | `ui/ds/SWTextField.kt` | Текстовые поля |
| `SWButton` | `ui/ds/SWButton.kt` | Кнопки |
| `SWDateTimePicker` | `ui/ds/SWDateTimePicker.kt` | Пикер даты рождения |
| `ListRowView` | `ui/ds/ListRowView.kt` | Строки с иконками |
| `FormCardContainer` | `ui/ds/FormCardContainer.kt` | Контейнер для секций |
| `LoadingOverlayView` | `ui/ds/LoadingOverlayView.kt` | Оверлей загрузки |

### Состояние UI

```kotlin
data class EditProfileUiState(
    val userForm: MainUserForm,
    val initialForm: MainUserForm,      // снимок исходных значений
    val selectedCountry: Country?,
    val selectedCity: City?,
    val isSaving: Boolean = false
) {
    val hasChanges: Boolean get() = userForm != initialForm
}
```

---

## Выполненные итерации

### ✅ Итерация 1: UI экрана

Созданы файлы: `EditProfileScreen.kt`, `IEditProfileViewModel.kt`, `EditProfileViewModel.kt`, `EditProfileUiState.kt`. Реализованы: верстка компонентов, подписка на `SWRepository.userFlow`, навигация из ProfileRootScreen, `imePadding()`.

### ✅ Итерация 2: Сохранение на сервер

`onSaveClick()` с `SWRepository.editUser()`, проверка `hasChanges`, состояние `isSaving`, обновление `initialForm`, навигация назад, сохранение в Room.

### ✅ Итерация 2.1-2.3: Багфиксы и UX

Исправлены: синхронизация DatePicker, GenderPicker/ChangePasswordButton на ListRowView, DropdownMenu. Добавлены: LoadingOverlayView, иконка ArrowBack, UTC timezone. Добавлена иконка `outline_key.xml`, строка `select_gender`.

### ✅ Итерация 2.4: Ограничение минимального возраста

`yearRange = 1900..(currentYear - 13)` в BirthdayPicker — пользователь не может указать возраст младше 13 лет.

### ✅ Итерация 3.1: Интеграция UserNotifier для обработки ошибок

Добавлена зависимость `userNotifier: UserNotifier` в конструктор ViewModel. Заменены `_events.emit(EditProfileEvent.ShowError(...))` на `userNotifier.handleError(AppError.Generic(...))`. Удалено событие `ShowError` из `EditProfileEvent` и обработка в `EditProfileScreen`. Обновлена фабрика в `AppContainer`.

### ✅ Итерация 3.2: Добавление Logger через интерфейс

Добавлена зависимость `logger: Logger` в конструктор ViewModel. Заменены все вызовы `Log.i/d/w/e(TAG, ...)` на `logger.i/d/w/e(TAG, ...)`. Обновлена фабрика в `AppContainer`. Удален импорт `android.util.Log`.

### ✅ Итерация 3.3: Автоматический выбор города при смене страны

**Проблема**: Сервер не принимает `country_id` без `city_id` - при сохранении профиля с новой страной, но без города, сервер игнорировал изменения.

**Решение**: Обновлена логика `EditProfileLocations.selectCountry()` - при смене страны автоматически выбирается первый город из новой страны вместо `null`.

**Изменения**:
- `EditProfileLocations.kt`: `newCity = newCountry.cities.firstOrNull()` вместо `null`
- `EditProfileLocationsTest.kt`: обновлены тесты `selectCountry_selectsFirstCity_whenCityNotInNewCountry` и `selectCountry_selectsFirstCity_whenCurrentCityIsNull`

### ✅ Итерация 3.4: Откат несохранённых изменений при закрытии экрана

**Проблема**: Если пользователь выбрал другую страну/город, но не нажал "Сохранить", изменения оставались в ViewModel и отображались при повторном открытии экрана.

**Решение**: Добавлен механизм отката изменений к исходным значениям при закрытии экрана без сохранения.

**Изменения**:
- `EditProfileUiState.kt`: добавлены поля `initialCountry` и `initialCity` для хранения исходных значений
- `IEditProfileViewModel.kt`: добавлен метод `resetChanges()`
- `EditProfileViewModel.kt`:
  - `loadUserData()` сохраняет `initialCountry` и `initialCity`
  - `onSaveClick()` обновляет initial значения при успешном сохранении
  - `resetChanges()` восстанавливает форму и локации к исходным значениям
- `EditProfileScreen.kt`: добавлен `DisposableEffect` для вызова `resetChanges()` при закрытии экрана

**Важно**: Экран смены пароля имеет свою логику и не затрагивается этим механизмом.

---

## Итерация 3: Экраны выбора страны/города ✅

**Подробный план**: [item-list-screen-plan.md](./item-list-screen-plan.md)

### Реализованные компоненты

1. **EditProfileLocations** (domain layer) - логика выбора страны/города с тестами
2. **ItemListScreen** (UI) - stateless composable с SearchBar, LazyColumn, EmptyStateView
3. **SelectCountryScreen** / **SelectCityScreen** - wrapper-экраны с локальным состоянием поиска
4. **Интеграция** с EditProfileViewModel (методы `onCountrySelected()`, `onCitySelected()`)
5. **Навигация** в RootScreen.kt:
   - `EditProfileViewModel` создаётся на уровне RootScreen (shared между экранами)
   - Добавлены composable для SelectCountryScreen и SelectCityScreen
   - Добавлены колбэки навигации в EditProfileScreen

### Нереализованный функционал

**Feedback (отправка сообщения об отсутствии страны/города):**
- ⚠️ Кнопка "Написать нам" отображается в empty state
- ⚠️ Колбэк `onContactUs` содержит заглушку
- 📋 План реализации: [item-list-screen-plan.md](./item-list-screen-plan.md) (Этап 5)

---

## Итерация 4: ChangePasswordScreen 📋 ЗАПЛАНИРОВАНО

**Подробный план**: [6.6_ChangePasswordScreen.md](./6.6_ChangePasswordScreen.md)

### Что нужно сделать

- Проверить существование экрана
- Подключить навигацию

---

## Итерация 5: AvatarPicker (Выбор фото профиля) 📋 ЗАПЛАНИРОВАНО

**Подробный план**: [avatar-picker-plan.md](./avatar-picker-plan.md)

### Что нужно сделать

- Добавить Photo Picker для выбора изображения из галереи
- Обновить UI State для хранения выбранного фото
- Реализовать отправку фото на сервер при сохранении
- Добавить превью выбранного фото в AvatarSection

### Best Practices

- Использовать `ActivityResultContracts.PickVisualMedia()` (Android 13+)
- Fallback на `ActivityResultContracts.GetContent()` для Android < 13
- Не требует разрешений `READ_MEDIA_IMAGES`

---

## Локализация

### Добавленные строковые ресурсы

| Ключ | RU | EN | Назначение |
|------|----|----|------------|
| `change_photo` | Изменить фотографию | Change Photo | Кнопка под аватаром |
| `full_name` | Имя и фамилия | Full name | Placeholder для поля имени |
| `select_gender` | Выбери пол | Select gender | Placeholder для GenderPicker |

### Используемые существующие строки

| Ключ | RU | EN |
|------|----|----|
| `edit_profile` | Изменить профиль | Edit your profile |
| `login` | Логин | Login |
| `email` | Email | Email |
| `change_password` | Изменить пароль | Change password |
| `gender` | Пол | Gender |
| `birthdate` | Дата рождения | Date of birth |
| `country` | Страна | Country |
| `city` | Город | City |
| `save_changes` | Сохранить изменения | Save changes |
| `select_country` | Выбери страну | Select a country |
| `select_city` | Выбери город | Select a city |
| `man` | Мужчина | Man |
| `woman` | Женщина | Woman |
