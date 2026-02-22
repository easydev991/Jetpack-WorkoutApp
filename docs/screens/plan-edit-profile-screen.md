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
├── Scaffold (topBar, content, bottomBar)
│   ├── AvatarSection (AsyncImage + SWButton)
│   ├── FormCardContainer (текстовые поля)
│   └── FormCardContainer (пикеры: gender, birthDate, country, city)
└── LoadingOverlayView (isSaving/isUploadingAvatar/isDeleting)
```

### Состояние UI

```kotlin
data class EditProfileUiState(
    val userForm: MainUserForm,
    val initialForm: MainUserForm,
    val selectedCountry: Country?,
    val selectedCity: City?,
    val selectedAvatarUri: Uri?,
    val avatarError: String?,
    val emailError: String?,
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val isDeleting: Boolean = false
) {
    val hasChanges: Boolean get() = userForm != initialForm || selectedAvatarUri != null
    val canSave: Boolean get() = hasChanges && emailError == null
}
```

---

## Выполненные итерации

### ✅ Итерация 1-3: UI, сохранение, багфиксы, экраны выбора

- `EditProfileScreen.kt`, `EditProfileViewModel.kt`, `EditProfileUiState.kt`
- Подписка на `SWRepository.userFlow`, `onSaveClick()` с `editUser()`
- DatePicker sync, GenderPicker/ChangePasswordButton на ListRowView
- `EditProfileLocations` (domain), `SelectCountryScreen`, `SelectCityScreen`
- Автовыбор города при смене страны, `resetChanges()` + `DisposableEffect`

### ✅ Итерация 4: ChangePasswordScreen

UI с 3 текстовыми полями, ViewModel с валидацией (6-32 символа), UseCase для API. Тесты: 30.

### ✅ Итерация 5: AvatarPicker

Photo Picker через `ActivityResultContracts.PickVisualMedia()`. MIME-проверка, конвертация, сжатие. Тесты: 3.

### ✅ Итерация 6: Валидация email и блокировка Save

Regex-валидация "на лету" в `onEmailChange()`. `canSave = hasChanges && emailError == null`. Тесты: 5.

### ✅ Удаление профиля (2026-02-22)

Кнопка корзины в TopAppBar, диалог подтверждения, `DeleteUserUseCase`, `NavigateToLogin` событие. Тесты: 8.

### ✅ Устранение утечки Context (2026-02-22)

Удалён `context: Context` из конструктора `EditProfileViewModel`, используется `AvatarHelper` + `ResourcesProvider`.

### ✅ Feedback для отсутствующих стран/городов (2026-02-22)

Кнопка "Написать нам" в empty state экранов выбора страны/города реализована.

---

## Локализация

### Добавленные строковые ресурсы

| Ключ | RU | EN |
|------|----|----|
| `change_photo` | Изменить фотографию | Change Photo |
| `email_invalid` | Введите корректный email | Enter a valid email |
| `avatar_error_unsupported_type` | Неподдерживаемый формат изображения | Unsupported image format |
| `avatar_error_read_failed` | Не удалось прочитать изображение | Failed to read image |
| `password_short` / `_not_match` / `_changed_successfully` | Ошибки и успех смены пароля | Password errors/success |
| `delete_profile_title` / `_message` | Удаление профиля | Delete profile |

---

## Тестирование

### EditProfile (53 теста)

| Файл | Тестов | Покрытие |
|------|--------|----------|
| `EditProfileViewModelTest` | 31 | Avatar, hasChanges, canSave, email validation, delete |
| `EditProfileViewModelSelectionTest` | 7 | Country/city selection |
| `EditProfileLocationsTest` | 11 | Domain model |
| `DeleteUserUseCaseTest` | 4 | Delete user API |

### ChangePassword (30 тестов)

| Файл | Тестов |
|------|--------|
| `ChangePasswordViewModelTest` | 14 |
| `ChangePasswordUiStateTest` | 8 |
| `ChangePasswordUseCaseTest` | 8 |

---

## Оставшиеся задачи

Нет

---

## Выполненные задачи (архив)

### ✅ Упрощение валидации даты рождения (2026-02-23)

Упрощена валидация возраста - убрана проверка на 13 лет, оставлена только проверка на дату не в будущем. При выборе даты в будущем отображается ошибка под пикером.

**Изменения**:
- `EditProfileScreen.kt`: `yearRange = 1900..currentYear`, параметр `error` в BirthdayPicker, отображение ошибки
- `EditProfileViewModel.kt`: extension `LocalDate.isInFuture()`
- `EditProfileUiState.kt`: поле `birthDateError`, учёт в `canSave` (уже было реализовано ранее)
- `EditProfileViewModelTest.kt`: 5 тестов валидации даты рождения (уже были реализованы ранее)

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-02-23 | Упрощение валидации даты рождения (UI изменения) |
| 2026-02-22 | Feedback для отсутствующих стран/городов отмечен как выполненный |
| 2026-02-22 | Сжатие плана: объединены итерации 1-3, сокращены таблицы |
| 2026-02-22 | Добавлена задача "Упрощение валидации даты рождения" |
| 2026-02-22 | Устранение утечки Context, удаление профиля, блокировка Save |
