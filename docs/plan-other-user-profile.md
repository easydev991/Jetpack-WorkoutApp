# План реализации OtherUserProfileScreen

## Общее описание

Экран профиля другого пользователя. Отличия от ProfileRootScreen:
- В TopAppBar — кнопка управления черным списком (вместо поиска)
- SendMessageButton (primary) + FriendActionButton (tinted) вместо EditProfileButton
- Нет BlacklistButton и LogoutButton
- Использует `getUser(userId)`, pull-to-refresh и LoadingOverlayView

---

## Выполненные этапы ✅

### Реализация

- FriendAction/BlacklistAction enums, OtherUserProfileUiState, ViewModel с кэшированием
- ProfileButtons.kt, OtherUserProfileScreen с ProfileContent, SendMessageButton, FriendActionButton
- DI через AppContainer, навигация из MyFriendsScreen/SearchUserScreen, ресурсы en/ru
- Unit-тесты ViewModel (OtherUserProfileViewModelTest, FriendsListViewModelTest)
- UI-тесты: OtherUserProfileScreenTest (20), UserFriendsScreenTest (10)

### Багфиксы и доработки

- JournalsButton при journalCount=0 → проверка `journalCount > 0`
- isFriend/isInBlacklist не работали → исправлено в SWRepository.getUser()
- Кнопки disabled при блокировке: `buttonsEnabled = !isRefreshing && !isInBlacklist`
- Скрытие edit-кнопок в чужих дневниках (JournalsListScreen, JournalEntriesScreen)
- EmptyStateView.buttonTitle опциональный (null = без кнопки)
- Complaint sealed class + FeedbackSender.sendComplaint() для жалоб (скрыто до доработки)
- TextEntryMode.Message для отправки сообщений через TextEntrySheetHost
- Snackbar через UserNotifier:
  - При добавлении в друзья: "Запрос отправлен!" (`friend_request_sent`)
  - При удалении из друзей: "Обновлен список друзей" (`friends_list_updated`)
  - При отправке сообщения: "Сообщение отправлено!" (`message_sent`)

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

## Оставшиеся задачи

- [ ] Ручное тестирование на устройстве

---

## Примечания

1. **Кнопки профиля:** FriendsButton, UsedParksButton, AddedParksButton, JournalsButton → ProfileButtons.kt
2. **Pull-to-refresh:** LoadingOverlayView только при `uiState is Loading && !isRefreshing`
3. **Иконки:** `Icons.Outlined.*` (не `Icons.Default.*`)
4. **Блокировка:** После успешной блокировки вызывается `onBlocked()` callback
5. **Кэширование адреса:** При refreshUser не загружаем country/city если ID не изменились
6. **Timeout currentUser:** 10 сек, иначе `canRetry=false`
7. **Тесты:** Приватные composables помечать `internal`, для дублей текста использовать `onAllNodesWithText(text)[index]`
