# Запрет открытия профиля авторизованного пользователя в списках пользователей

## Проблема

На экранах со списками пользователей (`SearchUserScreen`, `UserFriendsScreen`) пользователь может увидеть себя в списке и нажать на свою вьюху. Это приводит к попытке открытия собственного профиля через `OtherUserProfileScreen`, что создает излишне сложную навигацию.

**Решение:** Блокировать вьюхи пользователей с `enabled = false`, если `userId` совпадает с `currentUserId`.

---

## Этап 1: SearchUserScreen

### 1.1. Обновление SearchUserScreen

- [ ] Добавить параметр `currentUserId: Long?` в `SearchUserScreen` и `SearchUserScreenContent`
- [ ] В приватной функции `UsersList` добавить проверку: `val isEnabled = user.id != currentUserId`
- [ ] Визуально выделить заблокированные вьюхи (alpha = 0.5)
- [ ] Блокировать клик через `clickable(enabled = isEnabled)`

**Детали реализации:**
- Параметр `currentUserId: Long?` в `SearchUserScreen` и `SearchUserScreenContent`
- В функции `UsersList` внутри `items(users)`:

  ```kotlin
  val isEnabled = user.id != currentUserId
  Box(
      modifier = Modifier
          .graphicsLayer { alpha = if (isEnabled) 1f else 0.5f }
          .clickable(enabled = isEnabled) { onUserClick(user.id) }
  ) {
      UserRowView(...)
  }
  ```

**Файлы для изменения:**
- `app/src/main/java/com/swparks/ui/screens/profile/SearchUserScreen.kt`

### 1.2. Обновление навигации в RootScreen

- [ ] Передать `currentUser?.id` из `RootScreen` в `SearchUserScreen`
- [ ] `currentUser` уже доступен в RootScreen (строка ~99)

**Файлы для изменения:**
- `app/src/main/java/com/swparks/ui/screens/RootScreen.kt`

---

## Этап 2: UserFriendsScreen

### 2.1. Обновление UserFriendsScreen

- [ ] Добавить параметр `currentUserId: Long?` в `UserFriendsScreen` и `UserFriendsScreenContent`
- [ ] В приватной функции `FriendsList` добавить проверку: `val isEnabled = user.id != currentUserId`
- [ ] Визуально выделить заблокированные вьюхи (alpha = 0.5)
- [ ] Блокировать клик через `clickable(enabled = isEnabled)`

**Детали реализации:**
- Параметр `currentUserId: Long?` в `UserFriendsScreen` и `UserFriendsScreenContent`
- В функции `FriendsList` внутри цикла `friends.forEach`:

  ```kotlin
  val isEnabled = user.id != currentUserId
  Box(
      modifier = Modifier
          .graphicsLayer { alpha = if (isEnabled) 1f else 0.5f }
          .clickable(enabled = isEnabled) { onFriendClick(user.id) }
  ) {
      UserRowView(...)
  }
  ```

**Файлы для изменения:**
- `app/src/main/java/com/swparks/ui/screens/profile/UserFriendsScreen.kt`

### 2.2. Обновление навигации в RootScreen

- [ ] Передать `currentUser?.id` из `RootScreen` в `UserFriendsScreen`
- [ ] `currentUser` уже доступен в RootScreen (строка ~99)

**Файлы для изменения:**
- `app/src/main/java/com/swparks/ui/screens/RootScreen.kt`

---

## Этап 3: MyFriendsScreen

### 3.1. Обновление MyFriendsScreen

- [ ] Добавить параметр `currentUserId: Long?` в `MyFriendsScreen` и `MyFriendsScreenContent`
- [ ] В приватной функции `SuccessContent` добавить проверку для секции друзей: `val isEnabled = user.id != currentUserId && enabled`
- [ ] Визуально выделить заблокированные вьюхи (alpha = 0.5)
- [ ] Блокировать клик через `clickable(enabled = isEnabled)`

**Детали реализации:**
- Параметр `currentUserId: Long?` в `MyFriendsScreen` и `MyFriendsScreenContent`
- В функции `SuccessContent` внутри цикла `friends.forEach`:

  ```kotlin
  val isEnabled = user.id != currentUserId && enabled
  Box(
      modifier = Modifier
          .graphicsLayer { alpha = if (isEnabled) 1f else 0.5f }
          .clickable(enabled = isEnabled) { onFriendClick(user.id) }
  ) {
      UserRowView(...)
  }
  ```

**Файлы для изменения:**
- `app/src/main/java/com/swparks/ui/screens/profile/MyFriendsScreen.kt`

### 3.2. Обновление навигации в RootScreen

- [ ] Передать `currentUser?.id` из `RootScreen` в `MyFriendsScreen`
- [ ] `currentUser` уже доступен в RootScreen (строка ~99)

**Файлы для изменения:**
- `app/src/main/java/com/swparks/ui/screens/RootScreen.kt`

### 3.3. Аудит остальных экранов

- [ ] Проверить `DialogsListScreen` - список диалогов (пользователей)
- [ ] Применить тот же паттерн, если диалоги содержат возможность перехода на профиль пользователя

---

## Этап 4: Тестирование

### 4.1. Ручное тестирование

- [ ] Открыть `SearchUserScreen` и найти себя в поиске - вьюха должна быть disabled
- [ ] Открыть `UserFriendsScreen` и проверить, что свой профиль в списке друзей disabled
- [ ] Проверить, что клик по disabled вьюхе не вызывает навигацию
- [ ] Проверить визуальное отображение (alpha = 0.5)

### 4.2. UI тесты (опционально)

- [ ] Добавить тест для `SearchUserScreenTest`: проверка disabled состояния для своего профиля
- [ ] Добавить тест для `UserFriendsScreenTest`: проверка disabled состояния для своего профиля
- [ ] Проверить, что `onNodeWithTag(...).assert(isNotClickable())`

---

## Критерии завершения

- [ ] `SearchUserScreen`: вьюха своего профиля disabled (alpha = 0.5f)
- [ ] `UserFriendsScreen`: вьюха своего профиля disabled (alpha = 0.5f)
- [ ] `MyFriendsScreen`: вьюха своего профиля в списке друзей disabled (alpha = 0.5f)
- [ ] Клик по disabled вьюхе не вызывает навигацию
- [ ] `currentUserId: Long?` корректно передается из `RootScreen.currentUser`
- [ ] Код соответствует style guide проекта (ktlint, detekt)
- [ ] После изменений выполнена команда `make format`

---

## Технические заметки

### Получение currentUserId в RootScreen

```kotlin
// В RootScreen (строка ~99) currentUser уже доступен
val currentUser by profileViewModel.currentUser.collectAsState()
```

### Передача currentUserId в экраны

```kotlin
// В RootScreen при вызове SearchUserScreen
SearchUserScreen(
    modifier = Modifier.fillMaxSize(),
    viewModel = viewModel,
    onBackClick = { appState.navController.popBackStack() },
    onUserClick = { userId ->
        appState.navController.navigate(
            Screen.OtherUserProfile.createRoute(userId, source)
        )
    },
    parentPaddingValues = paddingValues,
    currentUserId = currentUser?.id  // <-- добавить этот параметр
)
```

### Визуальное выделение disabled состояния

```kotlin
val isEnabled = user.id != currentUserId
Box(
    modifier = Modifier
        .graphicsLayer {
            alpha = if (isEnabled) 1f else 0.5f
        }
        .clickable(
            enabled = isEnabled,
            onClick = { onUserClick(user.id) }
        )
) {
    UserRowView(
        modifier = Modifier,
        imageStringURL = user.image,
        name = user.name,
        address = null
    )
}
```

### Важные примечания

1. **Тип параметра:** `currentUserId: Long?` (не `String`), так как `user.id` имеет тип `Long`
2. **MyFriendsScreen особенность:** уже есть параметр `enabled` для блокировки при обработке запросов, поэтому нужно комбинировать: `val isEnabled = user.id != currentUserId && enabled`
3. **UserRowView НЕ обновлять:** этот компонент из дизайн-системы не нужно менять. Блокировку делать через `Box` с `clickable` и `graphicsLayer` на уровне вызова

---

## Связанная документация

- `docs/screens/plan-search-user-screen.md` - документация SearchUserScreen
- `docs/screens/plan-other-user-profile-screen.md` - документация OtherUserProfileScreen (имеет защиту через `onNavigateToOwnProfile()`)
- `docs/plan-development.md` - общий план разработки
- `docs/navigation/doc-appstate-auth-state.md` - документация по AppState и авторизации
