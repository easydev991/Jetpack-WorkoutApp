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

## Седьмая итерация: Доработка FAB согласно iOS-логике 🚧

### Проблема

В Android FAB отображается по условию `isOwner && canCreateEntry && !isDeleting`, что дублирует логику — `canCreateEntry` уже включает проверку владельца внутри себя (см. `JournalAccess.canCreateEntry`).

### iOS-реализация (эталон)

```swift
@ViewBuilder
var addEntryButtonIfNeeded: some View {
    let canCreateEntry = JournalAccess.canCreateEntry(
        journalOwnerId: userId,
        journalCommentAccess: currentJournal.commentAccessType,
        mainUserId: defaults.mainUserInfo?.id,
        mainUserFriendsIds: defaults.friendsIdsList
    )
    if canCreateEntry {
        Button { showCreateEntrySheet = true } label: {
            Icons.Regular.plus.view
                .symbolVariant(.circle)
        }
        .tint(.accent)
        .disabled(currentState.isLoading)  // ← отключение при загрузке
        .sheet(isPresented: $showCreateEntrySheet) { ... }
    }
}
```

### Требуемые изменения

1. **Убрать дублирование `isOwner`** в условии FAB — оставить только `canCreateEntry`
2. **Добавить отключение FAB при `isRefreshing`** (аналог `isLoading` в iOS)

### Изменения в коде

**JournalEntriesScreen.kt** (строки 177-202):

```kotlin
// Было:
if (isOwner && contentState.canCreateEntry && !isDeleting) {

// Должно быть:
if (contentState.canCreateEntry && !isDeleting && !isRefreshing) {
```

### Unit-тесты

- FAB виден при `canCreateEntry = true`, скрыт при `false`
- FAB отключен при `isRefreshing = true`
- FAB не зависит от `isOwner` напрямую (логика инкапсулирована в `canCreateEntry`)

---

## Связанные документы

- [План JournalsListScreen](./doc-journals-list-screen.md)
- [План TextEntryScreen](./doc-text-entry-screen.md)
- [JournalSettingsDialog](./doc-journal-settings-dialog.md)
