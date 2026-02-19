# Экран профиля другого пользователя (OtherUserProfileScreen)

## Общее описание

Экран профиля другого пользователя. Отличия от ProfileRootScreen:
- В TopAppBar — кнопка управления черным списком (вместо поиска)
- SendMessageButton (primary) + FriendActionButton (tinted) вместо EditProfileButton
- Нет BlacklistButton и LogoutButton
- Использует `getUser(userId)`, pull-to-refresh и LoadingOverlayView

---

## Реализованный функционал ✅

### Базовая функциональность

- [x] FriendAction/BlacklistAction enums, OtherUserProfileUiState
- [x] ViewModel с кэшированием viewedUser, currentUser, friends, blacklist
- [x] ProfileButtons.kt: SendMessageButton, FriendActionButton
- [x] DI через AppContainer, навигация из MyFriendsScreen/SearchUserScreen

### Friend Actions

- [x] Добавление в друзья (отправка заявки)
- [x] Принятие входящей заявки
- [x] Удаление из друзей с диалогом подтверждения (RemoveFriendDialog)
- [x] Отмена отправленной заявки
- [x] LoadingOverlayView при выполнении действия (`isFriendActionLoading`)
- [x] Snackbar через UserNotifier

### Blacklist

- [x] Блокировка/разблокировка пользователя
- [x] Диалоги подтверждения
- [x] Отображение состояния "Пользователь ограничил доступ" (`isBlockedMe`)

### UI доработки

- [x] JournalsButton при `journalCount=0` скрыт
- [x] Кнопки disabled при `isRefreshing || isInBlacklist || isFriendActionLoading`
- [x] Скрытие edit-кнопок в чужих дневниках
- [x] EmptyStateView.buttonTitle опциональный

### Кэширование

- [x] Обновление `isFriend`/`friendsCount` после friendAction
- [x] Исправлен баг с `markAsFriend` при отправке заявки
- [x] Кэширование адреса: при refreshUser не загружаем country/city если ID не изменились

---

## Диаграмма состояний UI

```
isLoadingCurrentUser (timeout 10s)
         │
    ┌────┴────┐
    │         │
[timeout]  [loaded]
    │         │
    ▼         ▼
Auth Error  Loading → getUser
              │
       ┌──────┴──────┐
       │             │
    [404/403]      [200]
       │             │
       ▼             ▼
  UserNotFound   isBlockedMe?
                     │
              ┌──────┴──────┐
              │             │
           [true]        [false]
              │             │
              ▼             ▼
        BlockedByUser   Success → pull-to-refresh
```

---

## Тестирование

### Автоматические тесты ✅

- **Unit-тесты**: `OtherUserProfileViewModelTest`, `FriendsListViewModelTest`
- **UI тесты**: `OtherUserProfileScreenTest` (20), `UserFriendsScreenTest` (10)

### Ручное тестирование (требуется устройство)

- [ ] Просмотр профиля другого пользователя
- [ ] Отправка заявки в друзья
- [ ] Принятие/отклонение заявки
- [ ] Удаление из друзей
- [ ] Блокировка/разблокировка
- [ ] Отправка сообщения
- [ ] Pull-to-refresh

---

## Файлы

| Файл | Назначение |
|------|------------|
| `OtherUserProfileScreen.kt` | UI экран профиля |
| `OtherUserProfileViewModel.kt` | ViewModel для управления состоянием |
| `OtherUserProfileUiState.kt` | Sealed class состояний UI |
| `IOtherUserProfileViewModel.kt` | Интерфейс ViewModel |
| `ProfileButtons.kt` | Кнопки SendMessage, FriendAction, Blacklist |

---

## Примечания

1. **Pull-to-refresh:** LoadingOverlayView только при `uiState is Loading && !isRefreshing`
2. **Иконки:** `Icons.Outlined.*` (не `Icons.Default.*`)
3. **Блокировка:** После успешной блокировки вызывается `onBlocked()` callback
4. **Timeout currentUser:** 10 сек, иначе `canRetry=false`
5. **Тесты:** Приватные composables помечать `internal`, для дублей текста использовать `onAllNodesWithText(text)[index]`
6. **Обновление кэша:** При удалении друга обновляется `isFriend=0` и `friendsCount--` текущего пользователя
