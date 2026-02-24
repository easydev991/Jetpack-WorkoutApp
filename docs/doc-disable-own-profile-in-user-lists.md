# Запрет открытия профиля авторизованного пользователя в списках пользователей

## Проблема

На экранах со списками пользователей пользователь может увидеть себя в списке и нажать на свою вьюху, что приводит к попытке открытия собственного профиля через `OtherUserProfileScreen`.

**Решение:** Блокировать вьюхи с `enabled = false`, если `userId == currentUserId`.

**Статус:** ✅ **Реализовано**

---

## Реализация

### Экраны

- [x] **SearchUserScreen**: добавлен параметр `currentUserId: Long?`, вьюха блокируется через `disabledIf(user.id == currentUserId)`
- [x] **UserFriendsScreen**: добавлен параметр `currentUserId: Long?`, вьюха блокируется через `disabledIf(user.id == currentUserId)`
- [x] **MyFriendsScreen**: добавлен параметр `currentUserId: Long?`, вьюха блокируется через `disabledIf(user.id == currentUserId || !enabled)` (комбинируется с existing `enabled`)
- [x] **RootScreen**: передача `currentUser?.id` во все три экрана (строки 333, 389, 435)
- [x] **Аудит DialogsListScreen**: не требует изменений (нет навигации на профиль)

### Рефакторинг

- [x] **ModifierUtils.disabledIf()**: создана extension функция в `app/src/main/java/com/swparks/ui/utils/ModifierUtils.kt`
  - Визуальная блокировка (alpha = 0.5)
  - Блокировка кликов
  - Устранение дублирования кода в 3 экранах

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

### 1. Добавить параметр в экран

```kotlin
@Composable
fun YourScreen(
    // ...
    currentUserId: Long?,  // <-- добавить
    // ...
)
```

### 2. Передать из RootScreen

```kotlin
// В RootScreen (currentUser уже доступен через collectAsState)
YourScreen(
    // ...
    currentUserId = currentUser?.id,  // <-- передать
    // ...
)
```

### 3. Использовать disabledIf в списке

```kotlin
import com.swparks.ui.utils.disabledIf

items(users) { user ->
    Box(
        modifier = Modifier.disabledIf(
            disabled = user.id == currentUserId,
            onClick = { onUserClick(user.id) }
        )
    ) {
        UserRowView(...)
    }
}
```

### 4. Если есть existing `enabled`

```kotlin
// Комбинировать условия (как в MyFriendsScreen)
val isDisabled = user.id == currentUserId || !enabled

Box(
    modifier = Modifier.disabledIf(
        disabled = isDisabled,
        onClick = { onUserClick(user.id) }
    )
) {
    UserRowView(...)
}
```

---

## Технические детали

### Типы данных

- `currentUserId: Long?` (nullable, так как `currentUser` может быть null)
- `user.id: Long` (не nullable)

### Компоненты

- **UserRowView**: компонент дизайн-системы, НЕ изменять
- **Блокировка**: через `Box` с `disabledIf` на уровне вызова

### Связанная документация

- `docs/screens/plan-search-user-screen.md`
- `docs/screens/plan-other-user-profile-screen.md`
- `docs/navigation/doc-appstate-auth-state.md`
