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
| Восьмая ✅ | Редактирование/удаление своих записей в чужих дневниках |
| Девятая ✅ | Синхронизация друзей: очистка флагов при обновлении |
| Десятая 📋 | EmptyStateView: не показывать при загрузке и без прав создания |

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
| Восьмая | 10 | Редактирование/удаление своих записей в чужих дневниках |
| Девятая | 4 | Синхронизация друзей |
| Десятая | — | EmptyStateView (TODO) |

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

## Восьмая итерация: Редактирование/удаление своих записей в чужих дневниках ✅

### Проблема

Сейчас пользователь может создавать записи в чужих дневниках (если разрешено доступом), но не может их редактировать или удалять. Логика `canEditEntry` и `canDeleteEntry` проверяла только `isOwner` (владелец дневника), но не учитывала `authorId` записи.

### Реализация

Логика вынесена в ViewModel для возможности unit-тестирования.

**1. IJournalEntriesViewModel.kt** — добавлен новый метод:

```kotlin
fun canDeleteEntry(entry: JournalEntry): Boolean
```

**2. JournalEntriesViewModel.kt** — обновлена логика:

```kotlin
// canEditEntry
override fun canEditEntry(entry: JournalEntry): Boolean {
    val currentUserId = _currentUserId.value ?: return false
    if (entry.authorId == null) return false
    val isOwner = journalOwnerId == currentUserId
    return isOwner || entry.authorId == currentUserId
}

// canDeleteEntry
override fun canDeleteEntry(entry: JournalEntry): Boolean {
    val currentUserId = _currentUserId.value ?: return false
    val isOwner = journalOwnerId == currentUserId
    return isOwner || entry.authorId == currentUserId
}
```

**3. JournalEntriesScreen.kt** — передача функций из ViewModel:

```kotlin
val canEditEntry: (JournalEntry) -> Boolean = { entry -> viewModel.canEditEntry(entry) }
val canDeleteEntry: (JournalEntry) -> Boolean = { entry -> viewModel.canDeleteEntry(entry) }
```

### Сценарии

| Сценарий | canEditEntry | canDeleteEntry |
|----------|--------------|----------------|
| Своя запись в своём дневнике | ✅ | ✅ |
| Чужая запись в своём дневнике | ❌ | ❌ |
| Своя запись в чужом дневнике | ✅ | ✅ |
| Чужая запись в чужом дневнике | ❌ | ❌ |
| Не авторизован | ❌ | ❌ |

### Unit-тесты

Добавлено 10 тестов в `JournalEntriesViewModelTest`:
- `canEditEntry_journalOwner_returnsTrue`
- `canEditEntry_authorInForeignJournal_returnsTrue`
- `canEditEntry_notLoggedIn_returnsFalse`
- `canEditEntry_entryWithoutAuthor_returnsFalse`
- `canDeleteEntry_author_returnsTrue`
- `canDeleteEntry_journalOwner_returnsTrue`
- `canDeleteEntry_foreignEntryInForeignJournal_returnsFalse`
- `canDeleteEntry_notLoggedIn_returnsFalse`
- Обновлены `testCanEditEntry_author_returnsTrue` и `testCanEditEntry_notAuthor_returnsFalse`

### Проверка

```bash
./gradlew test  # Все тесты проходят
./gradlew ktlintCheck  # Линтер проходит
```

---

## Девятая итерация: Отладка FAB для FRIENDS-доступа 🔍

### Проблема

На iOS FAB не отображается в чужом дневнике с FRIENDS-доступом, если владелец дневника не находится в списке друзей текущего пользователя. На Android FAB отображается (и сервер возвращает 403 при попытке создать запись).

Предполагаемые причины:
1. **Stale friends cache** — локальный список друзей может содержать устаревшие данные
2. **Несимметричная проверка дружбы** — `isFriend` проверяет "владелец в моём списке друзей", а не "я в списке друзей владельца"

### Добавленное логирование

Добавлено логирование в следующие функции:

1. **`computeCanCreateEntry()`** — логирует:
   - `currentUserId` — ID авторизованного пользователя
   - `journalOwnerId` — ID владельца дневника
   - `commentAccess` — уровень доступа (ALL/FRIENDS/NOBODY)
   - `friendsIds` — список ID друзей текущего пользователя
   - `isFriend` — результат проверки `friendsIds.contains(journalOwnerId)`
   - `canCreateEntry result` — итоговый результат

2. **`canEditEntry(entry)`** — логирует:
   - `currentUserId`
   - `journalOwnerId`
   - `commentAccess`
   - `entry.authorId`, `entry.id`

3. **`canDeleteEntry(entry)`** — логирует:
   - `currentUserId`
   - `journalOwnerId`
   - `commentAccess`
   - `entry.authorId`, `entry.id`

### Ожидаемые логи

При открытии чужого дневника с FRIENDS-доступом, где владелец НЕ в друзьях:

```
D JournalEntriesVM: === computeCanCreateEntry DEBUG ===
D JournalEntriesVM: currentUserId=10367, journalOwnerId=22748
D JournalEntriesVM: commentAccess=FRIENDS, friendsIds=[...]
D JournalEntriesVM: isFriend=false
D JournalEntriesVM: ==================
D JournalEntriesVM: canCreateEntry result: false
```

### Реальные логи (проблема найдена!)

После pull-to-refresh профиля:
- Сервер возвращает пустой список друзей: `Body: []`
- НО локальный кэш всё ещё содержит `friendsIds=[10367]`

**Вывод:** Локальная база данных НЕ обновляется при получении пустого списка друзей с сервера.

### Корень проблемы

1. **Проблема не в canCreateEntry** — логика правильная
2. **Проблема в синхронизации друзей** — при обновлении профиля API возвращает пустой массив `[]`, но Room-таблица не очищается для удалённых друзей
3. **Метод `getSocialUpdates()`** получает `friends = []` с сервера, но не удаляет старых друзей из БД

### Исправление

**1. UserDao.kt** — добавлены методы для сброса флагов:

```kotlin
@Query("UPDATE users SET isFriend = 0 WHERE isFriend = 1")
suspend fun clearAllFriendFlags()

@Query("UPDATE users SET isFriendRequest = 0 WHERE isFriendRequest = 1")
suspend fun clearAllFriendRequestFlags()

@Query("UPDATE users SET isBlacklisted = 0 WHERE isBlacklisted = 1")
suspend fun clearAllBlacklistFlags()
```

**2. SWRepository.kt** — `getSocialUpdates()` теперь очищает старые данные перед вставкой новых:

```kotlin
// Сбрасываем все флаги перед обновлением
// Это критически важно: если сервер возвращает пустой список,
// старые записи должны быть очищены
userDao.clearAllFriendFlags()
userDao.clearAllFriendRequestFlags()
userDao.clearAllBlacklistFlags()

// Затем вставляем актуальные данные
userDao.insertAll(friends.map { it.toEntity(isFriend = true) })
```

### Unit-тесты

Добавлено 4 теста в `SWRepositoryProfileTest`:
- `getSocialUpdates_clearsOldFriendsBeforeInsertingNew` — проверяет очистку флагов перед вставкой
- `getSocialUpdates_whenApiReturnsEmptyFriendsList_clearsOldFriends` — проверяет очистку при пустом списке
- `getSocialUpdates_whenApiReturnsEmptyFriendRequests_clearsOldRequests` — проверяет очистку заявок
- `getSocialUpdates_whenApiReturnsEmptyBlacklist_clearsOldBlacklist` — проверяет очистку черного списка

---

## Десятая итерация: EmptyStateView для чужих дневников 📋

### Проблема

При открытии чужого дневника без записей EmptyStateView кратковременно показывается во время загрузки (пока `isRefreshing=true`). Это создаёт визуальный артефакт, аналогичный багу в JournalsListScreen (коммит c7d9ee7).

### Текущее поведение

```kotlin
if (entries.isEmpty()) {
    if (canCreateEntry) {
        EmptyStateView(
            text = stringResource(R.string.entries_empty),
            buttonTitle = stringResource(R.string.create_entry),
            enabled = !isRefreshing && !isDeleting,
            onButtonClick = onAddEntryClick
        )
    }
}
```

**Проблемы:**
1. EmptyStateView показывается во время загрузки (`isRefreshing=true`)
2. FAB не показывается во время загрузки, но EmptyStateView с кнопкой "Создать" — показывается

### Требуемое поведение

EmptyStateView показывается только при выполнении **всех** условий:
- `entries.isEmpty()` — список записей пуст
- `!isRefreshing` — загрузка завершена
- `canCreateEntry` — пользователь имеет право создавать записи

Для чужих дневников где `canCreateEntry=false` (например, `NOBODY`):
- EmptyStateView не показывается вообще
- Пользователь видит пустой экран

### Реализация

**1. ContentScreen в JournalEntriesScreen.kt:**

```kotlin
if (entries.isEmpty()) {
    // EmptyStateView показывается только после завершения загрузки
    // и если пользователь имеет право создавать записи
    if (!isRefreshing && canCreateEntry) {
        EmptyStateView(
            text = stringResource(R.string.entries_empty),
            buttonTitle = stringResource(R.string.create_entry),
            enabled = !isDeleting,
            onButtonClick = onAddEntryClick
        )
    }
}
```

### Сценарии

| Сценарий | EmptyStateView | Примечание |
|----------|----------------|------------|
| Свой дневник, пуст, загрузка | ❌ | Ждём завершения загрузки |
| Свой дневник, пуст, загружен | ✅ | Можно создать запись |
| Чужой дневник, пуст, ALL-доступ | ✅ | Можно создать запись |
| Чужой дневник, пуст, NOBODY-доступ | ❌ | Нельзя создавать записи |
| Чужой дневник, пуст, FRIENDS-доступ (не друг) | ❌ | Нельзя создавать записи |

### Unit-тесты

Добавить тесты в `JournalEntriesScreenTest.kt`:
- `emptyStateNotShown_whenRefreshing` — EmptyStateView не показывается во время загрузки
- `emptyStateShown_forOwnerWhenNotRefreshing` — EmptyStateView показывается для владельца после загрузки
- `emptyStateShown_forForeignJournalWithAllAccess` — EmptyStateView показывается при ALL-доступе
- `emptyStateNotShown_forForeignJournalWithNobodyAccess` — EmptyStateView не показывается при NOBODY-доступе

### Связанные документы

- [План JournalsListScreen](./doc-journals-list-screen.md) — аналогичный фикс в коммите c7d9ee7
- [План TextEntryScreen](./doc-text-entry-screen.md)
- [JournalSettingsDialog](./doc-journal-settings-dialog.md)
