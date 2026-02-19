# Экран профиля текущего пользователя (ProfileRootScreen)

## Обзор

Экран профиля авторизованного пользователя с кнопками навигации к функционалу приложения. Реализует реактивное отображение данных через `currentUser: StateFlow<User?>`.

### Ключевые особенности

- **Реактивный UI**: автоматическое обновление при изменении данных через Flow
- **Авторизация**: отображение `IncognitoProfileView` для неавторизованных пользователей
- **Pull-to-refresh**: обновление профиля с сервера
- **Навигация**: кнопки для перехода к связанным экранам

---

## Реализованный функционал ✅

### Реактивное отображение профиля

- [x] `ProfileViewModel.currentUser: StateFlow<User?>` - единый источник правды
- [x] Подписка UI через `collectAsState()` для автоматической рекомпозиции
- [x] Обновление UI после авторизации без перезапуска приложения
- [x] `AppState.currentUser` - синхронизация для TopAppBar (кнопка поиска)

### UI экрана профиля

- [x] `ProfileRootScreen` - полная реализация с вертикальной прокруткой
- [x] `UserProfileCardView` - карточка с фото, именем, адресом
- [x] `IncognitoProfileView` - экран для неавторизованных пользователей
- [x] `ProfileTopAppBar` с кнопкой поиска пользователей (только для авторизованных)
- [x] Pull-to-refresh через `PullToRefreshBox`

### Кнопки навигации

| Кнопка | Условие отображения | Навигация | Статус |
|--------|---------------------|-----------|--------|
| Изменить профиль | Авторизован | `Screen.EditProfile` | ✅ |
| Друзья | Авторизован | `Screen.MyFriends` | ✅ |
| Где тренируется | `hasUsedParks` | `Screen.UserTrainingParks` | ✅ |
| Добавленные площадки | `hasAddedParks` | `Screen.UserParks` | ✅ |
| Дневники | Авторизован | `Screen.JournalsList` | ✅ |
| Чёрный список | Есть в списке | `Screen.Blacklist` | ✅ |
| Выйти | Авторизован | AlertDialog → logout | ✅ |

### UI доработки

- [x] FriendsButton: скрытие текста про друзей при `friendsCount=0` (показывается только badge с заявками)
- [x] AlertDialog для подтверждения логаута
- [x] Логирование всех нажатий кнопок на русском

---

## Архитектура

### MVVM + StateFlow

```
UserDao (Flow<User?>)
    ↓
SWRepository.getCurrentUserFlow()
    ↓
ProfileViewModel.currentUser (StateFlow<User?>)
    ↓
ProfileRootScreen (collectAsState)
    ↓
UI компоненты
```

### Data Layer

- **UserPreferencesRepository.currentUserId**: `Flow<Long?>` для реактивного отслеживания авторизации
- **SWRepository.getCurrentUserFlow()**: объединяет preferences и UserDao через `flatMapLatest`

### Domain Layer

- **LoginUseCase**: сохраняет userId после успешной авторизации
- **LogoutUseCase**: очищает userId при выходе

---

## Тестирование

### Автоматические тесты ✅

- **Unit-тесты**: `ProfileViewModelTest`
- **UI тесты**: `ProfileRootScreenTest`
- **LoginUiState**: `LoginUiStateTest` (9 тестов)

### Ручное тестирование (требуется устройство)

- [ ] Отображение профиля при авторизации
- [ ] Отображение IncognitoProfileView при отсутствии пользователя
- [ ] Автоматическое обновление UI после авторизации
- [ ] Pull-to-refresh
- [ ] Навигация по всем кнопкам
- [ ] Логаут с подтверждением

---

## Файлы

| Файл | Назначение |
|------|------------|
| `ProfileRootScreen.kt` | UI экран профиля |
| `ProfileViewModel.kt` | ViewModel для управления состоянием |
| `ProfileUiState.kt` | Sealed class состояний UI |
| `IProfileViewModel.kt` | Интерфейс ViewModel |
| `ProfileButtons.kt` | Компоненты кнопок профиля |

---

## Примечания

### Ограничения

1. **AppState**: Кнопка поиска синхронизируется через `appState.currentUser`, а не напрямую из ViewModel
2. **Логаут**: Вызывается через `appContainer?.logoutUseCase` из UI (в идеале - через ViewModel)

### Рекомендации для рефакторинга

```kotlin
// В ProfileViewModel
fun logout() {
    viewModelScope.launch {
        logoutUseCase?.invoke()
    }
}

// В ProfileRootScreen
AlertDialog(
    confirmButton = {
        TextButton(onClick = { viewModel.logout() }) { ... }
    }
)
```

### Консистентность с iOS

- Порядок кнопок соответствует iOS-версии
- Условное отображение кнопок аналогично iOS
- AlertDialog для логаута аналогичен iOS confirmationDialog
