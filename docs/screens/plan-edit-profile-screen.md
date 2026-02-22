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
    val selectedAvatarUri: Uri?,        // выбранное фото профиля
    val avatarError: String?,           // ошибка загрузки фото
    val emailError: String?,            // ошибка валидации email
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false
) {
    val hasChanges: Boolean get() = userForm != initialForm || selectedAvatarUri != null
}
```

---

## Выполненные итерации

### ✅ Итерация 1-2: UI экрана и сохранение

Созданы `EditProfileScreen.kt`, `EditProfileViewModel.kt`, `EditProfileUiState.kt`. Реализованы: верстка, подписка на `SWRepository.userFlow`, `onSaveClick()` с `editUser()`, проверка `hasChanges`, состояния `isSaving`/`isUploadingAvatar`, навигация.

### ✅ Итерация 2.1-3.4: Багфиксы и UX улучшения

- DatePicker sync, GenderPicker/ChangePasswordButton на ListRowView, DropdownMenu
- LoadingOverlayView, иконка ArrowBack, UTC timezone, минимальный возраст 13 лет
- Интеграция `UserNotifier` и `Logger` через интерфейсы
- Автовыбор первого города при смене страны (`EditProfileLocations.selectCountry()`)
- Откат несохранённых изменений через `resetChanges()` + `DisposableEffect`

### ✅ Итерация 3: Экраны выбора страны/города

Реализованы: `EditProfileLocations` (domain), `ItemListScreen` (UI), `SelectCountryScreen`, `SelectCityScreen`, навигация в RootScreen.kt. Подробнее: [item-list-screen-plan.md](./item-list-screen-plan.md)

### ✅ Итерация 4: ChangePasswordScreen

UI с 3 текстовыми полями, ViewModel с валидацией (6-32 символа), UseCase для API. Навигация: `Screen.ChangePassword` → `change_password`. Тесты: 30 (14+8+8).

### ✅ Итерация 5: AvatarPicker

Photo Picker через `ActivityResultContracts.PickVisualMedia()` (без разрешений). MIME-проверка (`ImageUtils`), конвертация (`UriUtils`), сжатие. Тесты: 3.

### ✅ Итерация 6: Валидация email

Regex-валидация "на лету" в `onEmailChange()`, отображение ошибки в UI. Тесты: 9.

---

## Локализация

### Добавленные строковые ресурсы

| Ключ | RU | EN | Назначение |
|------|----|----|------------|
| `change_photo` | Изменить фотографию | Change Photo | Кнопка под аватаром |
| `full_name` | Имя и фамилия | Full name | Placeholder для поля имени |
| `select_gender` | Выбери пол | Select gender | Placeholder для GenderPicker |
| `email_invalid` | Введите корректный email | Enter a valid email | Ошибка валидации email |
| `avatar_error_unsupported_type` | Неподдерживаемый формат изображения | Unsupported image format | Ошибка при выборе невалидного файла |
| `avatar_error_read_failed` | Не удалось прочитать изображение | Failed to read image | Ошибка при чтении файла |
| `password_short` | Пароль слишком короткий | Password is too short | Ошибка длины пароля |
| `password_not_match` | Пароли не совпадают | Passwords do not match | Ошибка несовпадения паролей |
| `password_changed_successfully` | Пароль успешно изменён | Password changed successfully | Успешная смена пароля |

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

---

## Тестирование

### EditProfile (53 теста)

| Файл | Тестов | Покрытие |
|------|--------|----------|
| `EditProfileViewModelTest` | 31 | Avatar, hasChanges, canSave, onSave, resetChanges, email validation, delete profile |
| `EditProfileViewModelSelectionTest` | 7 | Country/city selection |
| `EditProfileLocationsTest` | 11 | Domain model |
| `DeleteUserUseCaseTest` | 4 | Delete user API success/error |

### ChangePassword (30 тестов)

| Файл | Тестов | Покрытие |
|------|--------|----------|
| `ChangePasswordViewModelTest` | 14 | Initial state, input, canSave, onSave |
| `ChangePasswordUiStateTest` | 8 | canSave, password errors |
| `ChangePasswordUseCaseTest` | 8 | API success/error |

---

## Оставшиеся задачи

### ✅ ~~Блокировка Save при ошибке email~~ (выполнено 2026-02-22)

**Реализовано**:
- Добавлено `canSave` computed property в `EditProfileUiState`: `hasChanges && emailError == null`
- Обновлено условие enabled кнопки в `EditProfileScreen`
- Добавлено 5 тестов для canSave в `EditProfileViewModelTest`

### ✅ Кнопка удаления профиля в TopAppBar (выполнено 2026-02-22)

**Приоритет**: Средний

**Описание**: В TopAppBar справа добавить кнопку с иконкой корзины для удаления профиля. При нажатии отображать диалог подтверждения.

**Реализовано**:
- UI: кнопка с иконкой корзины в `actions` TopAppBar
- Диалог подтверждения с локализацией (RU/EN)
- LoadingOverlayView при удалении (`isDeleting` состояние)
- `DeleteUserUseCase` - вызывает API, очищает токен и все локальные данные
- `NavigateToLogin` событие для навигации после успешного удаления
- Обработка ошибок через `UserNotifier`

**Архитектура**:
- `IDeleteUserUseCase` / `DeleteUserUseCase` - domain layer
- `EditProfileUiState.isDeleting` - UI состояние
- `EditProfileEvent.NavigateToLogin` - навигационное событие
- `EditProfileViewModel.onDeleteProfileClick()` - обработчик

**Тесты**:
- `DeleteUserUseCaseTest` - 4 теста (success, failure, data clearing)
- `EditProfileViewModelTest` - 4 теста (delete profile)

**Навигация после удаления**:
- `onNavigateToLogin` в `RootScreen.kt` выполняет `popBackStack(Screen.Profile.route, inclusive = false)`
- Пользователь возвращается на `ProfileRootScreen`
- `ProfileRootScreen` автоматически показывает `IncognitoProfileView` (т.к. `user == null` после `forceLogout()`)

**Локализация**:

| Ключ | RU | EN |
|------|----|----|
| `delete_profile_title` | Удаление профиля | Delete profile |
| `delete_profile_message` | Все данные вашего профиля будут необратимо удалены | All your profile data will be permanently deleted |

### ⚠️ Feedback для отсутствующих стран/городов

**Приоритет**: Низкий

**Описание**: Кнопка "Написать нам" в empty state экранов выбора страны/города не реализована.

**План**: [item-list-screen-plan.md](./item-list-screen-plan.md) (Этап 5)

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-02-22 | Навигация после удаления: `onNavigateToLogin` → `popBackStack(Profile)`, возврат на `ProfileRootScreen` с `IncognitoProfileView` |
| 2026-02-22 | Удаление профиля: `DeleteUserUseCase`, `NavigateToLogin`, `isDeleting` состояние, 8 тестов |
| 2026-02-22 | Кнопка удаления профиля: иконка корзины в TopAppBar, диалог подтверждения с локализацией |
| 2026-02-22 | Реализована блокировка Save при ошибке email: добавлено `canSave` в UiState, 5 тестов |
| 2026-02-22 | Сжатие плана: объединены итерации 1-3.4, сокращены таблицы компонентов |
| 2026-02-22 | Актуализация тестов: EditProfile (40), ChangePassword (30) |
