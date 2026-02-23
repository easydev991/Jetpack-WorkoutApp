# Экран редактирования профиля (EditProfileScreen)

## Обзор

Экран для изменения личных данных пользователя. Реализован по образцу iOS-версии `EditProfileScreen.swift`.

### Референсы

- **iOS**: `SwiftUI-WorkoutApp/Screens/Profile/EditProfile/EditProfileScreen.swift`
- **Android**: `app/src/main/java/com/swparks/ui/screens/profile/EditProfileScreen.kt`

---

## Архитектура

### Структура UI

```
EditProfileScreen
├── Scaffold (topBar, content, bottomBar)
│   ├── AvatarSection (AsyncImage + SWButton)
│   ├── FormCardContainer (текстовые поля)
│   │   ├── LoginField (SWTextField)
│   │   ├── EmailField (SWTextField с валидацией)
│   │   ├── FullNameField (SWTextField)
│   │   └── ChangePasswordButton (ListRowView)
│   ├── FormCardContainer (GenderPicker)
│   ├── FormCardContainer (BirthdayPicker)
│   └── FormCardContainer (CountryPicker, CityPicker)
└── LoadingOverlayView (isLoading)
```

### Состояние UI

```kotlin
data class EditProfileUiState(
    val userForm: MainUserForm,
    val initialForm: MainUserForm,
    val countries: List<Country>,
    val cities: List<City>,
    val selectedCountry: Country?,
    val selectedCity: City?,
    val initialCountry: Country?,
    val initialCity: City?,
    val selectedAvatarUri: Uri?,
    val avatarError: String?,
    val emailError: String?,
    val birthDateError: String?,
    val isLoading: Boolean
) {
    val hasChanges: Boolean
    val canSave: Boolean
}
```

### События навигации

```kotlin
sealed interface EditProfileEvent {
    data object NavigateBack : EditProfileEvent
    data object NavigateToLogin : EditProfileEvent
    data class NavigateToChangePassword(val userId: Long) : EditProfileEvent
    data class NavigateToSelectCountry(val currentCountryId: Int?) : EditProfileEvent
    data class NavigateToSelectCity(val currentCityId: Int?, val countryId: Int) : EditProfileEvent
}
```

---

## Реализованный функционал

### Редактируемые поля

| Поле | Компонент | Особенности |
|------|-----------|-------------|
| Аватар | AsyncImage + Photo Picker | Выбор из галереи, сжатие, MIME-проверка |
| Логин | SWTextField | Текстовое поле |
| Email | SWTextField | Валидация regex, отображение ошибки |
| Полное имя | SWTextField | Текстовое поле |
| Пароль | ListRowView | Переход на ChangePasswordScreen |
| Пол | GenderPicker (DropdownMenu) | Выпадающий список (мужской/женский) |
| Дата рождения | SWDateTimePicker | Режим BIRTHDAY, год 1900-текущий |
| Страна | ListRowView | Переход на SelectCountryScreen |
| Город | ListRowView | Переход на SelectCityScreen |

### Валидация

#### Email

- Regex-валидация при вводе: `^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$`
- Ошибка отображается под полем
- Блокирует кнопку Save при невалидном email

#### Дата рождения

- Диапазон годов: 1900 — текущий год
- Проверка: дата не должна быть в будущем
- Ошибка отображается под пикером
- Блокирует кнопку Save при ошибке

### Аватар

- Photo Picker через `ActivityResultContracts.PickVisualMedia()` (только изображения)
- MIME-проверка: поддерживаемые форматы изображений
- Конвертация Uri → ByteArray через `AvatarHelper`
- Автоматическое сжатие через `ImageUtils.compressIfNeeded()`
- Отображение ошибки при неудачной загрузке
- Превью выбранного фото сразу после выбора

### Сохранение

- Кнопка Save активна только при `canSave = hasChanges && emailError == null && birthDateError == null`
- Блокировка повторного сохранения во время запроса
- Отправка формы и аватара на сервер через `SWRepository.editUser()`
- Обновление `initialForm` после успешного сохранения
- Автоматический возврат на предыдущий экран

### Удаление профиля

- Кнопка корзины в TopAppBar
- Диалог подтверждения перед удалением
- Вызов `DeleteUserUseCase` для удаления на сервере
- Переход на экран логина после успешного удаления

### Выбор страны и города

- Отдельные экраны `SelectCountryScreen` и `SelectCityScreen`
- Автоматический сброс города при смене страны
- Автовыбор страны при выборе города из другой страны
- Сброс несохранённых изменений при закрытии экрана через `resetChanges()`

---

## Зависимости ViewModel

```kotlin
class EditProfileViewModel(
    private val swRepository: SWRepository,           // Данные пользователя
    private val countriesRepository: CountriesRepository,  // Справочник стран/городов
    private val deleteUserUseCase: IDeleteUserUseCase,     // Удаление профиля
    private val avatarHelper: AvatarHelper,          // Конвертация Uri → ByteArray
    private val logger: Logger,                      // Логирование
    private val userNotifier: UserNotifier,          // Отображение ошибок
    private val resources: ResourcesProvider         // Строковые ресурсы
)
```

---

## Локализация

| Ключ | RU | EN |
|------|----|----|
| `change_photo` | Изменить фотографию | Change Photo |
| `email_invalid` | Введите корректный email | Enter a valid email |
| `birth_date_in_future` | Дата рождения не может быть в будущем | Birth date cannot be in the future |
| `avatar_error_unsupported_type` | Неподдерживаемый формат изображения | Unsupported image format |
| `avatar_error_read_failed` | Не удалось прочитать изображение | Failed to read image |
| `delete_profile_title` | Удаление профиля | Delete Profile |
| `delete_profile_message` | Вы уверены, что хотите удалить свой профиль? | Are you sure you want to delete your profile? |

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
