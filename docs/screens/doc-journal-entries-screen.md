# План реализации JournalEntriesScreen

## Выполненные итерации ✅

| Итерация | Функционал |
|----------|------------|
| Первая | Экран записей с кэшированием (offline-first), Pull-to-Refresh, навигация |
| Вторая | Удаление через API с обновлением кэша (идемпотентность: 200/204/404) |
| Доработки | EmptyStateView, userNotifier, запрет удаления первой записи |
| Третья | Интеграция с TextEntryScreen (Bottom Sheet), FAB, canCreateEntry/canEditEntry |
| Четвертая | Багфиксы: FAB, пустое поле при редактировании, обновление списка, свайпы |
| Пятая | Кнопка настроек в TopAppBar, JournalSettingsDialog, IJournalSettingsViewModel |
| Шестая | Багфиксы настроек: видимость кнопки, обновление заголовка, проверка кэша |
| Седьмая | Доработка FAB по iOS-логике: убрано дублирование isOwner, отключение при обновлении |
| Восьмая | Редактирование/удаление своих записей в чужих дневниках |

## Статистика тестов

| Итерация | Тестов | Примечание |
|----------|--------|------------|
| Первая | 29 | Базовый функционал |
| Вторая | 19 | Удаление |
| Доработки | 9 | EmptyStateView, запрет удаления |
| Третья | 13 | TextEntryScreen |
| Четвертая | 10 | + 116 обновлено |
| Пятая | 10 | Настройки дневника |
| Шестая | 2 | Багфиксы настроек |
| Седьмая | 2 | Доработка FAB по iOS-логике |
| Восьмая | TBD | Редактирование/удаление своих записей в чужих дневниках |

---

## Шестая итерация: Исправление багов настроек дневника ✅

### Точки входа в настройки

- **JournalEntriesScreen**: иконка в TopAppBar (условие: `currentUser?.id == journalOwnerId`)
- **JournalsListScreen**: пункт меню "Настроить" (условие: `currentUser?.id == journal?.ownerId`)

Оба используют `JournalSettingsDialog` с интерфейсом `IJournalSettingsViewModel`.

**Важно:** JournalEntriesScreen использует `journalOwnerId` из навигационных параметров для мгновенной проверки без ожидания загрузки journal.

### Исправленные баги

1. **Кнопка настроек не отображается при первом открытии** — используется `journalOwnerId` из навигации вместо `currentJournal?.ownerId`
2. **Заголовок не обновляется после редактирования** — TopAppBar использует `currentJournal?.title` с fallback на навигационный параметр
3. **Лишняя загрузка дневника с сервера** — `loadJournal()` проверяет кэш перед запросом

### Unit-тесты

- `testObserveEntries_preservesJournal` — journal сохраняется при обновлении entries
- `testLoadJournal_skipsWhenCached` — не загружает с сервера если есть в кэше

---

## Седьмая итерация: Доработка FAB согласно iOS-логике ✅

### Проблема

В Android было несколько проблем с логикой FAB:

1. **Дублирование `isOwner`** — FAB проверял `isOwner && canCreateEntry`, но `canCreateEntry` должен полностью инкапсулировать логику прав доступа
2. **Отсутствие отключения при обновлении** — iOS отключает кнопку при `isLoading`, а Android нет
3. **Неверная логика для `FRIENDS`** — в Android владелец мог создавать записи при `FRIENDS` без добавления себя в друзья

### iOS-реализация (эталон)

```swift
public static func canCreateEntry(
    journalOwnerId: Int,
    journalCommentAccess: Self,
    mainUserId: Int?,
    mainUserFriendsIds: [Int]
) -> Bool {
    let isOwner = journalOwnerId == mainUserId
    let isFriend = mainUserFriendsIds.contains(journalOwnerId)
    switch journalCommentAccess {
    case .all:
        return mainUserId != nil
    case .friends:
        return isFriend
    case .nobody:
        return isOwner
    }
}
```

### Исправления в коде

**1. JournalAccess.kt** — исправлена логика для FRIENDS:

```kotlin
// Было: return isFriend || isOwner // владелец всегда может создавать записи
// Стало:
return when (this) {
    JournalAccess.ALL -> true
    JournalAccess.FRIENDS -> isFriend  // только друзья (без исключения для владельца)
    JournalAccess.NOBODY -> isOwner
}
```

**2. JournalEntriesScreen.kt** — убрано дублирование `isOwner`:

```kotlin
// Было:
if (isOwner && contentState.canCreateEntry && !isDeleting) {

// Стало:
if (contentState.canCreateEntry && !isDeleting && !isRefreshing) {
```

### Unit-тесты

Исправленные тесты в `JournalAccessTest`:
- ✅ `canCreateEntry with FRIENDS and owner returns false` — владелец не может при FRIENDS без себя в друзьях
- ✅ `canCreateEntry with NOBODY and owner returns true` — владелец может при NOBODY

Исправленные тесты в `JournalEntriesViewModelTest`:
- ✅ `testCanCreateEntry_FRIENDS_isOwner_returnsFalse` — изменён с `assertTrue` на `assertFalse`

### Проверка

```bash
./gradlew :app:test  # Все тесты проходят
```

---

## Восьмая итерация: Редактирование/удаление своих записей в чужих дневниках

### Проблема

Сейчас пользователь может создавать записи в чужих дневниках (если разрешено доступом), но не может их редактировать или удалять. Логика `canEditEntry` и `canDeleteEntry` проверяет только `isOwner` (владелец дневника), но не учитывает `authorId` записи.

### Требуемое поведение

Если `entry.authorId == currentUser.id`, пользователь должен иметь возможность:
- **EDIT** — редактировать свою запись
- **DELETE** — удалять свою запись (кроме первой записи в дневнике)

### Текущий код (JournalEntriesScreen.kt)

```kotlin
canEditEntry = { entry -> entry.authorId != null && isOwner },
canDeleteEntry = isOwner,
```

### План доработки

1. **Добавить проверку автора записи** — изменить `canEditEntry` и `canDeleteEntry`:
   - `canEditEntry = { entry -> entry.authorId != null && (isOwner || entry.authorId == currentUser?.id) }`
   - `canDeleteEntry = isOwner || entry.authorId == currentUser?.id`

2. **Обновить логику формирования действий** — убедиться, что для своих записей добавляются оба действия:
   - `EDIT` — если `canEditEntry(entry)`
   - `DELETE` — если `canDeleteEntry && entry.id != firstEntryId`

### Примеры сценариев

| Сценарий | canEditEntry | canDeleteEntry |
|----------|--------------|----------------|
| Своя запись в своём дневнике | ✅ | ✅ |
| Чужая запись в своём дневнике | ❌ | ❌ (только владелец дневника) |
| Своя запись в чужом дневнике | ✅ | ✅ |
| Чужая запись в чужом дневнике | ❌ | ❌ |

### Связанные документы

- [План JournalsListScreen](./doc-journals-list-screen.md)
- [План TextEntryScreen](./doc-text-entry-screen.md)
- [JournalSettingsDialog](./doc-journal-settings-dialog.md)
