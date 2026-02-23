# План исправления: EmptyStateView на JournalsListScreen

## Проблема

При открытии чужого дневника (userId != currentUser.id) на экране JournalsListScreen при первой загрузке на заднем плане виден EmptyStateView. Это происходит потому что:

1. Flow из `getJournalsUseCase` сразу возвращает данные из локальной БД (кэш)
2. Для чужого дневника кэш пустой → Flow эмитит пустой список
3. ViewModel устанавливает `Content(journals = emptyList())`
4. UI в `ContentScreen` показывает EmptyStateView при `journals.isEmpty()`

## Решение

EmptyStateView показывается **ТОЛЬКО** при выполнении всех условий:
1. `journals.isEmpty()` — список пуст
2. `isOwner` — это собственный дневник пользователя (для чужих дневников EmptyStateView не показывается вообще, так как нельзя создавать чужие дневники)
3. `!isRefreshing` — загрузка завершена

### Условие показа EmptyStateView

```kotlin
if (journals.isEmpty()) {
    // EmptyStateView показывается только для владельца после завершения загрузки
    if (isOwner && !isRefreshing) {
        EmptyStateView(
            text = stringResource(R.string.journals_empty),
            buttonTitle = stringResource(R.string.create_journal),
            enabled = !isRefreshing && !isDeleting,
            onButtonClick = onCreateJournalClick
        )
    }
}
```

## Этапы реализации

### Этап 1: UI Layer — Модификация JournalsListScreen

**Файл:** `app/src/main/java/com/swparks/ui/screens/journals/JournalsListScreen.kt`

- [x] Модифицировать условие показа EmptyStateView: `isOwner && !isRefreshing`

### Этап 2: Тестирование

**Файл:** `app/src/androidTest/java/com/swparks/ui/screens/journals/JournalsListScreenTest.kt`

- [x] Тест: EmptyStateView НЕ показывается для чужого дневника во время загрузки
- [x] Тест: EmptyStateView НЕ показывается для чужого дневника после загрузки (для чужих нельзя создавать дневники)
- [x] Тест: EmptyStateView НЕ показывается для владельца во время загрузки
- [x] Тест: EmptyStateView показывается для владельца после загрузки

### Этап 3: Ручное тестирование

- [ ] Запустить приложение
- [ ] Открыть свой дневник без записей → убедиться что EmptyStateView показывается после загрузки
- [ ] Открыть чужой дневник → убедиться что EmptyStateView не мелькает и не показывается вообще

## Критерии завершения

- [x] EmptyStateView не мелькает при открытии чужого дневника
- [x] EmptyStateView НЕ показывается для чужого дневника вообще
- [x] EmptyStateView показывается для владельца только после завершения загрузки
- [x] Все тесты проходят
- [x] `make format` проходит без ошибок

## Результат реализации

**Измененные файлы:**
1. `app/src/main/java/com/swparks/ui/screens/journals/JournalsListScreen.kt`
   - Изменено условие показа EmptyStateView: `isOwner && !isRefreshing`

2. `app/src/androidTest/java/com/swparks/ui/screens/journals/JournalsListScreenTest.kt`
   - Тест `journalsListScreen_emptyStateNotShown_forOtherUserWhenRefreshing` — EmptyStateView НЕ показывается для чужого при загрузке
   - Тест `journalsListScreen_emptyStateNotShown_forOtherUserWhenNotRefreshing` — EmptyStateView НЕ показывается для чужого после загрузки
   - Тест `journalsListScreen_emptyStateNotShown_forOwnerWhenRefreshing` — EmptyStateView НЕ показывается для владельца при загрузке
   - Тест `journalsListScreen_emptyStateShown_forOwnerWhenNotRefreshing` — EmptyStateView показывается для владельца после загрузки
