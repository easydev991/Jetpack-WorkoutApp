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

- **UI**: JournalsButton при journalCount=0 скрыт; кнопки disabled при `isRefreshing || isInBlacklist`; скрытие edit-кнопок в чужих дневниках; EmptyStateView.buttonTitle опциональный; `enabled=false` для всех элементов при friend action
- **Friend actions**: RemoveFriendDialog с подтверждением; LoadingOverlayView при выполнении действия; Snackbar через UserNotifier
- **Кэш**: Обновление isFriend/friendsCount после friendAction; исправлен баг с markAsFriend при отправке заявки
- **Прочее**: TextEntryMode.Message для отправки сообщений; Complaint sealed class (скрыто); исправлен isFriend/isInBlacklist в SWRepository.getUser()

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
8. **Обновление кэша:** При удалении друга обновляется `isFriend=0` и `friendsCount--` текущего пользователя
