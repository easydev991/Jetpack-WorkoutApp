# План: Загрузка списка диалогов на экране Messages

## Статус: ✅ ВЫПОЛНЕНО

**Дата завершения:** 13 февраля 2026

---

## Реализация

**Архитектура:** MVVM + Fallback cache (сервер → Room → UI, офлайн-режим)

**Data Layer:** `DialogEntity`, `DialogDao`, `MessagesRepository`, миграция БД v2

**UI Layer:** `DialogsViewModel` с `DialogsUiState`, `MessagesRootScreen`, `DialogRowView`

**Тесты:** 11 unit-тестов ViewModel, 5 unit-тестов репозитория

---

## Исправленные баги (8)

| # | Проблема | Решение |
|---|----------|---------|
| 1 | Диалоги не удаляются при logout | `dialogDao.deleteAll()` в `clearUserData()` |
| 2 | Дата не форматируется | `DateFormatter.formatDate()` |
| 3 | EmptyStateView не по центру | `Box` + `contentAlignment = Center` |
| 4 | Error state не по центру | `Box` + `contentAlignment = Center` |
| 5 | Pull to refresh не работает при пустом списке | `verticalScroll(rememberScrollState())` |
| 6 | Диалоги не загружаются при повторной авторизации | `LaunchedEffect(appState.isAuthorized)` |
| 7 | Не показывается LoadingOverlayView после авторизации | `isLoadingProfile`/`isLoadingDialogs` флаги + `loadDialogsAfterAuth()` |
| 8 | При 0 диалогах от сервера показывается Error вместо EmptyStateViewForDialogs | Явная установка `Success(emptyList())` при успехе в `loadDialogsInternal()` |

---

## Ключевые файлы

```
data/database/entity/DialogEntity.kt
data/database/dao/DialogDao.kt
domain/repository/MessagesRepository.kt
data/repository/MessagesRepositoryImpl.kt
data/model/DialogResponse.kt
ui/viewmodel/DialogsViewModel.kt
ui/screens/messages/MessagesRootScreen.kt
ui/ds/DialogRowView.kt
```

---

## Примечания

- **Pull-to-refresh:** Material3 `PullToRefreshBox`
- **Безопасные зоны:** `parentPaddingValues` от `RootScreen`

---

# План: Удаление диалогов (Long Press + DropdownMenu)

## Статус: 🔲 НЕ НАЧАТО

## UX Flow

1. Пользователь удерживает палец на диалоге (**Long Press**)
2. Появляется **DropdownMenu** рядом с пальцем с пунктом "Удалить"
3. При нажатии на "Удалить" — **диалог подтверждения** (AlertDialog)
4. Подтверждение → вызов API → обновление списка

---

## Архитектурные решения

### State Hoisting (Важно!)

**❌ НЕ ДЕЛАТЬ:** Не добавлять `showDeleteMenu` в `DialogRowData` и не прокидывать через ViewModel.

**✅ ПРАВИЛЬНО:** Состояние меню — **локальное** внутри Composable. ViewModel не нужно знать, открыта ли менюшка.

```kotlin
// DialogRowView.kt — локальный стейт
var showMenu by remember { mutableStateOf(false) }

Box {
    Row(
        modifier = Modifier.combinedClickable(
            onClick = { onClick() },
            onLongClick = { showMenu = true }
        )
    ) { ... }

    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("Удалить") },
            onClick = {
                showMenu = false
                onDeleteClick() // Колбэк → Screen → ViewModel
            }
        )
    }
}
```

**Причина:** Избежать лишних перерисовок всего списка и unnecessary state hoisting.

---

## Задачи

### 1. DialogRowView — добавить Long Press + DropdownMenu

**Файл:** `ui/ds/DialogRowView.kt`

- Добавить параметр `onDeleteClick: () -> Unit` в `DialogRowData`
- Использовать `combinedClickable` с `onLongClick`
- Добавить локальный `var showMenu by remember { mutableStateOf(false) }`
- Обернуть Row в Box, добавить `DropdownMenu`
- **Важно:** Добавить `@OptIn(ExperimentalFoundationApi::class)` для `combinedClickable`

**Референс:** `JournalRowView.kt` — меню с `DropdownMenuItem`

```kotlin
data class DialogRowData(
    // ... existing fields
    val onDeleteClick: () -> Unit = {} // Только колбэк, без состояния меню!
)
```

### 2. MessagesRootScreen — добавить диалог подтверждения

**Файл:** `ui/screens/messages/MessagesRootScreen.kt`

- Добавить состояние для AlertDialog:

  ```kotlin
  var showDeleteDialog by remember { mutableStateOf(false) }
  var dialogToDelete by remember { mutableStateOf<DialogEntity?>(null) }
  ```

- Добавить `AlertDialog` для подтверждения (референс: `JournalsListScreen.kt:206-218`)
- Передать `onDeleteClick` в `DialogsList` → `DialogRowView`:

  ```kotlin
  onDeleteClick = {
      dialogToDelete = dialog
      showDeleteDialog = true
  }
  ```

### 3. DialogsViewModel — добавить удаление

**Файл:** `ui/viewmodel/DialogsViewModel.kt`

```kotlin
private val _isDeleting = MutableStateFlow(false)
val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

fun deleteDialog(dialogId: Long) {
    viewModelScope.launch {
        _isDeleting.value = true
        val result = swRepository.deleteDialog(dialogId) // SWRepository, не MessagesRepository!
        _isDeleting.value = false
        if (result.isFailure) {
            _syncError.value = "Ошибка удаления диалога"
        }
        // При успехе Flow из Room обновит список автоматически (после удаления из БД)
    }
}
```

### 4. IDialogsViewModel — добавить интерфейс

**Файл:** `ui/viewmodel/IDialogsViewModel.kt`

```kotlin
val isDeleting: StateFlow<Boolean>
fun deleteDialog(dialogId: Long)
```

### 5. SWRepository.deleteDialog — обновить (КРИТИЧНО!)

**Файл:** `data/repository/SWRepository.kt:769`

**Текущая реализация** вызывает только API:

```kotlin
override suspend fun deleteDialog(dialogId: Long): Result<Unit> =
    safeApiCall { swApi.deleteDialog(dialogId) }
```

**Нужно добавить удаление из БД после успешного API:**

```kotlin
override suspend fun deleteDialog(dialogId: Long): Result<Unit> {
    val result = safeApiCall { swApi.deleteDialog(dialogId) }
    if (result.isSuccess) {
        dialogDao.deleteById(dialogId) // ← Это запустит реактивное обновление UI
    }
    return result
}
```

**Причина:** Без удаления из Room диалог останется на экране до следующего pull-to-refresh.

### 6. DialogDao — добавить удаление

**Файл:** `data/database/dao/DialogDao.kt`

```kotlin
@Query("DELETE FROM dialogs WHERE id = :dialogId")
suspend fun deleteById(dialogId: Long)
```

### 7. Ресурсы — строки

**Файл:** `res/values/strings.xml`

```xml
<string name="delete_dialog_title">Удалить диалог?</string>
<string name="delete_dialog_message">Диалог будет удалён без возможности восстановления.</string>
```

**Файл:** `res/values-ru/strings.xml` (если нужен русский перевод)

---

## Порядок реализации

1. ✅ Endpoint уже реализован (`SWApi.deleteDialog`)
2. 🔲 Добавить `deleteById` в `DialogDao`
3. 🔲 Обновить `SWRepository.deleteDialog` — удалять из БД после успешного API
4. 🔲 Добавить `isDeleting` и `deleteDialog` в `IDialogsViewModel` + `DialogsViewModel`
5. 🔲 Обновить `DialogRowData` — добавить `onDeleteClick` (БЕЗ `showDeleteMenu`)
6. 🔲 Обновить `DialogRowView` — локальный `showMenu` + `DropdownMenu` + `combinedClickable`
7. 🔲 Обновить `MessagesRootScreen` — состояние AlertDialog + передача колбэка
8. 🔲 Добавить строки в `strings.xml`
9. 🔲 Написать тесты

---

## Тесты

### Unit-тесты ViewModel

**Файл:** `test/.../DialogsViewModelTest.kt`

```kotlin
@Test
fun deleteDialog_whenSuccess_thenRemovesFromList() = runTest

@Test
fun deleteDialog_whenFailure_thenShowsError() = runTest

@Test
fun isDeleting_whenDeleting_thenReturnsTrue() = runTest
```

### Unit-тесты Repository

**Файл:** `test/.../SWRepositoryMessagesTest.kt` (обновить существующий)

```kotlin
@Test
fun deleteDialog_whenApiSuccess_thenDeletesFromDb() = runTest {
    // Проверить, что dialogDao.deleteById() вызывается после успешного API
}
```

---

## Ключевые файлы

```
ui/ds/DialogRowView.kt                    # Long press + DropdownMenu (локальный showMenu)
ui/screens/messages/MessagesRootScreen.kt # AlertDialog + состояние
ui/viewmodel/DialogsViewModel.kt          # deleteDialog(), isDeleting
ui/viewmodel/IDialogsViewModel.kt         # interface
data/database/dao/DialogDao.kt            # deleteById()
data/repository/SWRepository.kt           # update deleteDialog() — добавить удаление из БД
res/values/strings.xml                    # strings
```

---

## Примечания

- **ExperimentalFoundationApi:** Не забыть `@OptIn(ExperimentalFoundationApi::class)` для `combinedClickable`
- **Реактивность:** UI обновится автоматически через Flow из Room после удаления из БД
