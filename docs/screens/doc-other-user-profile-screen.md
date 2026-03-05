# Экран профиля другого пользователя (OtherUserProfileScreen)

## Общее описание

Экран профиля другого пользователя. Отличия от ProfileRootScreen:
- В TopAppBar — кнопка управления черным списком (вместо поиска)
- SendMessageButton (filled) + FriendActionButton (tinted) вместо EditProfileButton
- Нет BlacklistButton и LogoutButton
- Использует `getUser(userId)`, pull-to-refresh и LoadingOverlayView
- TextEntrySheet для отправки сообщений

---

## Реализованный функционал ✅

### Базовая функциональность

- [x] `FriendAction` enum (SEND_FRIEND_REQUEST, REMOVE_FRIEND)
- [x] `BlacklistAction` enum (BLOCK, UNBLOCK)
- [x] `OtherUserProfileUiState` sealed class (Loading, UserNotFound, BlockedByUser, Success, Error)
- [x] ViewModel с кэшированием viewedUser, currentUser, friends, blacklist
- [x] DI через AppContainer, навигация из MyFriendsScreen/SearchUserScreen

### UI компоненты

- [x] `SendMessageButton` - primary кнопка для открытия TextEntrySheet
- [x] `FriendActionButton` - tinted кнопка с иконкой (PersonAdd/PersonRemove)
- [x] `BlacklistActionDialog` - диалог подтверждения блокировки/разблокировки
- [x] `RemoveFriendDialog` - диалог подтверждения удаления из друзей
- [x] `UserNotFoundContent` - экран "Пользователь не найден"
- [x] `BlockedByUserContent` - экран "Пользователь ограничил доступ"
- [x] `ErrorContent` - экран ошибки с кнопкой повтора

### Friend Actions

- [x] Отправка заявки в друзья (SEND_FRIEND_REQUEST)
- [x] Удаление из друзей с диалогом подтверждения (REMOVE_FRIEND)
- [x] LoadingOverlayView при выполнении действия (`isFriendActionLoading`)
- [x] Snackbar через UserNotifier

### Blacklist

- [x] Блокировка/разблокировка пользователя
- [x] Диалоги подтверждения (BlacklistActionDialog)
- [x] Возврат назад после успешной блокировки (`onBlocked` callback)
- [x] Отображение состояния "Пользователь ограничил доступ" (`BlockedByUser`)

### Отправка сообщений

- [x] TextEntrySheet для ввода текста сообщения
- [x] `TextEntryMode.Message(userId, userName)` режим
- [x] Обратные вызовы `onDismissed` и `onSendSuccess`

### UI доработки

- [x] JournalsButton скрыт при `journalCount=0`
- [x] Кнопки disabled при `isRefreshing || isInBlacklist || isFriendActionLoading`
- [x] Защита от просмотра собственного профиля (редирект на `onNavigateToOwnProfile`)
- [x] Pull-to-refresh через Material3 `PullToRefreshBox`

### Кэширование

- [x] Кэширование адреса: при refreshUser не загружаем country/city если ID не изменились
- [x] `lastCountryId`, `lastCityId`, `cachedCountry`, `cachedCity` в ViewModel

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
(canRetry=false)
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
                           │
                           ▼
                    ProfileContent
                    ├─ SendMessageButton → TextEntrySheet
                    ├─ FriendActionButton → Action/Dialog
                    ├─ FriendsButton
                    ├─ UsedParksButton
                    ├─ AddedParksButton
                    └─ JournalsButton (if > 0)
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

| Файл                                           | Назначение                                                                                                                                                      |
|------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ui/screens/profile/OtherUserProfileScreen.kt` | UI экран, компоненты: SendMessageButton, FriendActionButton, BlacklistActionDialog, RemoveFriendDialog, UserNotFoundContent, BlockedByUserContent, ErrorContent |
| `ui/viewmodel/OtherUserProfileViewModel.kt`    | ViewModel для управления состоянием                                                                                                                             |
| `ui/viewmodel/OtherUserProfileUiState.kt`      | Sealed class состояний UI                                                                                                                                       |
| `ui/viewmodel/IOtherUserProfileViewModel.kt`   | Интерфейс ViewModel                                                                                                                                             |
| `ui/screen/profile/ProfileButtons.kt`          | Переиспользуемые кнопки: FriendsButton, UsedParksButton, AddedParksButton, JournalsButton                                                                       |
| `ui/screens/common/TextEntrySheetHost.kt`      | Sheet для отправки сообщений                                                                                                                                    |

---

## Примечания

1. **Pull-to-refresh:** LoadingOverlayView только при `uiState is Loading && !isRefreshing`
2. **Friend action loading:** Отдельный LoadingOverlayView при `isFriendActionLoading`
3. **Иконки:** `Icons.Outlined.*` (не `Icons.Default.*`)
4. **Блокировка:** После успешной блокировки вызывается `onBlocked()` callback
5. **Timeout currentUser:** 10 сек (`CURRENT_USER_LOAD_TIMEOUT_MS`), иначе `canRetry=false`
6. **Тесты:** Приватные composables помечать `internal`, для дублей текста использовать `onAllNodesWithText(text)[index]`
7. **Собственный профиль:** При `viewedUserId == currentUserId` вызывается `onNavigateToOwnProfile()`
8. **Адрес:** Кэшируется в ViewModel для оптимизации refreshUser
