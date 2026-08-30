# Запрет открытия профиля авторизованного пользователя в списках пользователей

## Проблема

На экранах со списками пользователей пользователь может увидеть себя в списке и нажать на свою вьюху, что приводит к попытке открытия собственного профиля через `OtherUserProfileScreen`.

**Решение:** Блокировать вьюхи с `enabled = false`, если `userId == currentUserId`.

**Статус:** ✅ **Реализовано**

---

## Реализация

### Экраны

Блокировка выполняется через параметр `enabled` в `UserRowData` (компонент `UserRowView`): при `enabled = false` блокируются клики, визуальное disabled-состояние обрабатывает `FormCardContainer`.

- [x] **SearchUserScreen**: `currentUserId: Long?` передаётся через конфиг `SearchUserConfig`; в списке `UsersList` — `val isDisabled = user.id == currentUserId`, вьюха блокируется через `UserRowData(enabled = !isDisabled)`
- [x] **UserFriendsScreen**: `currentUserId: Long?` передаётся через конфиг `FriendsScreenConfig`; в приватном `FriendsList` — `val isDisabled = user.id == currentUserId`, вьюха блокируется через `UserRowData(enabled = !isDisabled)`
- [x] **MyFriendsScreen**: `currentUserId: Long?` передаётся через конфиг `FriendsScreenConfig`; блокировка в общем `FriendsListSection` (`com.swparks.ui.screens.common`): `val isDisabled = user.id == config.currentUserId || !config.enabled` (комбинируется с existing `enabled`)
- [x] **RootScreen**: передача `currentUser?.id` через конфиги в composable для маршрутов `user_search`, `my_friends`, `user_friends`
- [x] **Аудит DialogsListScreen**: не требует изменений (нет навигации на профиль)

### Утилиты

- `Modifier.disabledIf()` существует в `app/src/main/java/com/swparks/ui/utils/ModifierUtils.kt` (alpha 0.5 + блокировка кликов), но в списках пользователей **не используется** — применяется в `CommentRowView`
- Списки пользователей блокируются на уровне данных вьюхи (`UserRowData.enabled`), а не через modifier

### Тестирование

- [x] Ручное тестирование: все экраны блокируют свой профиль, клик не работает
- [x] Тесты: `BUILD SUCCESSFUL`
- [x] Сборка: `assembleDebug BUILD SUCCESSFUL`
- [x] `make format`: выполнено без ошибок

**Опционально:**
- [ ] UI тесты для проверки disabled состояния

---

## Паттерн для новых экранов

Для добавления блокировки на новые экраны:

### 1. Добавить currentUserId в конфиг экрана

```kotlin
data class YourScreenConfig(
    val parentPaddingValues: PaddingValues,
    val currentUserId: Long? = null  // <-- добавить
)
```

### 2. Передать из RootScreen

```kotlin
// В RootScreen (currentUser уже доступен через collectAsState)
YourScreen(
    config =
        YourScreenConfig(
            parentPaddingValues = paddingValues,
            currentUserId = currentUser?.id  // <-- передать
        )
)
```

### 3. Заблокировать вьюху в списке

```kotlin
items(users, key = { it.id }) { user ->
    val isDisabled = user.id == config.currentUserId
    UserRowView(
        data =
            UserRowData(
                modifier = Modifier,
                enabled = !isDisabled,  // <-- блокировка своего профиля
                imageStringURL = user.image,
                name = user.name,
                address = null,
                onClick = { onUserClick(user.id) }
            )
    )
}
```

### 4. Если есть existing `enabled`

```kotlin
// Комбинировать условия (как в FriendsListSection для MyFriendsScreen)
val isDisabled = user.id == config.currentUserId || !config.enabled

UserRowView(
    data =
        UserRowData(
            enabled = !isDisabled,
            // ...
        )
)
```

---

## Технические детали

### Типы данных

- `currentUserId: Long?` (nullable, так как `currentUser` может быть null)
- `user.id: Long` (не nullable)

### Компоненты

- **UserRowView**: компонент дизайн-системы, блокировка настраивается на уровне вызова через `UserRowData.enabled`
- **Блокировка**: `UserRowData(enabled = false)` блокирует клики; disabled-состояние визуально обрабатывает `FormCardContainer`

### Связанная документация

- `docs/screens/doc-search-user-screen.md`
- `docs/screens/doc-other-user-profile-screen.md`
- `docs/screens/doc-myfriends-screen.md`
- `docs/doc-searchuser-navigation.md`
- `docs/doc-appstate-auth-state.md`
