# План: Экран Messages (Список диалогов)

## Статус: ✅ ЗАВЕРШЁН

**Выполнено:** 15 февраля 2026 (загрузка диалогов, удаление, mark as read)

---

## Реализованный функционал

### Загрузка списка диалогов (13.02.2026)

**Архитектура:** MVVM + Fallback cache (сервер → Room → UI, офлайн-режим)

**Data Layer:** `DialogEntity`, `DialogDao`, `MessagesRepository`, миграция БД v2

**UI Layer:** `DialogsViewModel` с `DialogsUiState`, `MessagesRootScreen`, `DialogRowView`

**Тесты:** 11 unit-тестов ViewModel, 5 unit-тестов репозитория

**Исправленные баги (8):** logout cleanup, форматирование даты, центрирование empty/error states, pull-to-refresh на пустом списке, повторная авторизация, loading overlay, пустой ответ сервера.

### Удаление диалогов (15.02.2026)

**UX:** Long Press → DropdownMenu → AlertDialog → API → LoadingOverlayView

**Ключевое решение:** DropdownMenu рендерится на уровне `DialogsContent` (State Hoisting), не внутри `DialogRowView`. Это корректно работает с `positionInRoot()`.

**Реализация:**
- `DialogRowData.onLongClick: (localOffset, itemPosition) -> Unit` — делегирует позицию
- `FormCardContainer.onLongClickWithOffset` — `pointerInput` + `detectTapGestures`
- `SWRepository.deleteDialog` — API + удаление из БД для реактивного обновления UI
- `DialogsViewModel.deleteDialog()` с флагом `isDeleting`

**Тесты:** 4 unit-теста на deleteDialog

### Mark as Read (15.02.2026)

**UX:** Long Press → DropdownMenu → "Mark as read" (только если unreadCount > 0) → API → LoadingOverlayView

**Реализация:**
- `DialogDao.updateUnreadCount(dialogId)` — SQL UPDATE для сброса счётчика
- `SWRepository.markDialogAsRead(dialogId, userId)` — API + DAO update
- `DialogsViewModel.markDialogAsRead()` с флагом `isMarkingAsRead`
- `isUpdating = combine(isDeleting, isMarkingAsRead)` — общий индикатор загрузки
- Условное отображение пункта меню: `if ((dialog.unreadCount ?: 0) > 0)`

**Тесты:** 5 unit-тестов на markDialogAsRead

---

## Ключевые файлы

```
# Data Layer
data/database/entity/DialogEntity.kt
data/database/dao/DialogDao.kt              # deleteById()
data/repository/SWRepository.kt             # deleteDialog() — API + БД
domain/repository/MessagesRepository.kt
data/repository/MessagesRepositoryImpl.kt
data/model/DialogResponse.kt

# UI Layer
ui/viewmodel/DialogsViewModel.kt            # deleteDialog(), isDeleting
ui/viewmodel/IDialogsViewModel.kt
ui/screens/messages/MessagesRootScreen.kt   # DropdownMenu + AlertDialog
ui/ds/DialogRowView.kt                      # positionInRoot + onLongClick
ui/ds/FormCardContainer.kt                  # onLongClickWithOffset

# Resources
res/values/strings.xml                      # delete_dialog_title, delete_dialog_message
res/values-ru/strings.xml
```

---

## Технические нюансы

### Позиционирование DropdownMenu

- `positionInRoot()` — позиция относительно composition root (без TopAppBar)
- DropdownMenu offset работает относительно window
- **Решение:** Рендерить DropdownMenu как sibling после Box на уровне `DialogsContent`

### Форматирование

- `DateFormatter.formatDate()` для дат сообщений
- Material3 `PullToRefreshBox` для обновления
- `LoadingOverlayView` при загрузке и удалении

---

## Примечания

- **Реактивность:** UI обновляется через Flow из Room
- **ExperimentalFoundationApi:** НЕ используется — `pointerInput` + `detectTapGestures`
- **State Hoisting:** Состояние меню локальное в Composable
- **Безопасные зоны:** `parentPaddingValues` от `RootScreen`

---

# План: Mark as Read в контекстном меню диалога

## Статус: ✅ ЗАВЕРШЁНО

## UX Flow

1. Пользователь делает **Long Press** на диалоге с непрочитанными сообщениями
2. Появляется **DropdownMenu** с двумя пунктами: "Mark as read" и "Delete"
3. При нажатии на "Mark as read" → API запрос → **LoadingOverlayView**
4. После успешного ответа → обновление `unreadCount = 0` в БД → UI обновляется реактивно

---

## Ревью и решения

### ✅ API контракт проверен

- API endpoint: `POST /mark-read` с параметром `from_user_id`
- Сигнатура: `swApi.markAsRead(@Field("from_user_id") fromUserId: Long)`
- **Решение:** Метод помечает все сообщения от конкретного пользователя как прочитанные. Параметр `userId` корректен.

### ⚠️ Существующий метод markAsRead

- Текущий `SWRepository.markAsRead(userId)` делает **только API вызов**, не обновляет БД
- **Решение:** Создать новый метод `markDialogAsRead(dialogId, userId)` который делает API + DAO update

### ✅ Механизм показа ошибок

- Уже реализован: `syncError: StateFlow<String?>` в `DialogsViewModel`
- `dismissSyncError()` для сброса ошибки
- **Решение:** Использовать существующий механизм

### 📝 Optimistic Update (отложено)

- **Рекомендация:** Optimistic update (мгновенное обновление UI + откат при ошибке)
- **Решение:** Для первой реализации используем синхронный подход с LoadingOverlayView. Optimistic update можно добавить позже как улучшение UX.

---

## Пошаговая реализация

### Data Layer

| # | Задача | Файл | Описание |
|---|--------|------|----------|
| 1 | ✅ Добавить `updateUnreadCount` | `DialogDao.kt` | SQL: `UPDATE dialogs SET unreadCount = 0 WHERE id = :dialogId` |
| 2 | ✅ Добавить `markDialogAsRead` в interface | `SWRepository.kt` | `suspend fun markDialogAsRead(dialogId: Long, userId: Int): Result<Unit>` |
| 3 | ✅ Реализовать `markDialogAsRead` | `SWRepository.kt` | API `swApi.markAsRead(userId)` + DAO `dialogDao.updateUnreadCount(dialogId)` |

### ViewModel

| # | Задача | Файл | Описание |
|---|--------|------|----------|
| 4 | ✅ Добавить `ResourcesProvider` | `DialogsViewModel.kt` | Добавить `resources: ResourcesProvider` в конструктор |
| 5 | ✅ Рефакторинг `deleteDialog` | `DialogsViewModel.kt` | Заменить хардкод на `resources.getString(R.string.dialog_delete_error)` |
| 6 | ✅ Добавить `isMarkingAsRead` | `DialogsViewModel.kt` | `MutableStateFlow<Boolean>` + геттер |
| 7 | ✅ Добавить `markDialogAsRead` | `DialogsViewModel.kt` | Метод: `isMarkingAsRead = true` → repository → обработка ошибки через `_syncError` |
| 8 | ✅ Обновить `isUpdating` | `DialogsViewModel.kt` | `val isUpdating = combine(isDeleting, isMarkingAsRead) { a, b -> a || b }` |
| 9 | ✅ Добавить метод в interface | `IDialogsViewModel.kt` | `val isMarkingAsRead: StateFlow<Boolean>`, `fun markDialogAsRead(dialogId: Long, userId: Int)` |
| 10 | ✅ Обновить `AppContainer` | `AppContainer.kt` | Передать `resourcesProvider` в `DialogsViewModel` factory |

### Resources

| # | Задача | Файл | Описание |
|---|--------|------|----------|
| 11 | ✅ Добавить `dialog_delete_error` | `strings.xml` | `<string name="dialog_delete_error">Failed to delete dialog</string>` |
| 12 | ✅ Добавить перевод | `strings-ru.xml` | `<string name="dialog_delete_error">Ошибка удаления диалога</string>` |
| 13 | ✅ Добавить `mark_as_read` | `strings.xml` | `<string name="mark_as_read">Mark as read</string>` |
| 14 | ✅ Добавить перевод | `strings-ru.xml` | `<string name="mark_as_read">Отметить прочитанным</string>` |

### UI Layer

| # | Задача | Файл | Описание |
|---|--------|------|----------|
| 15 | ✅ Добавить пункт меню | `MessagesRootScreen.kt` | `DropdownMenuItem` с текстом "Mark as read" |
| 16 | ✅ **Условное отображение** | `MessagesRootScreen.kt` | ⚠️ **Критично:** Показывать "Mark as read" ТОЛЬКО если `dialog.unreadCount > 0`. Если непрочитанных нет — пункт скрыт. |
| 17 | ✅ Обновить LoadingOverlayView условие | `MessagesRootScreen.kt` | `if (viewModel.isUpdating.value)` вместо `isDeleting` |

---

## Тесты

### Unit-тесты ViewModel

**Файл:** `test/.../DialogsViewModelTest.kt`

| # | Тест | Описание |
|---|------|----------|
| 1 | ✅ `markDialogAsRead_whenSuccess_callsRepositoryMarkAsRead` | Проверяет вызов repository |
| 2 | ✅ `markDialogAsRead_whenSuccess_updatesIsMarkingAsRead` | Проверяет флаг isMarkingAsRead |
| 3 | ✅ `markDialogAsRead_whenSuccess_callsWithCorrectParams` | Проверяет dialogId и userId |
| 4 | ✅ `markDialogAsRead_whenFailure_showsSyncError` | Проверяет `_syncError.value` через mock `resources.getString()` |
| 5 | ✅ `markDialogAsRead_whenFailure_logsError` | Проверяет логирование |

### Unit-тесты Repository

**Файл:** `test/.../SWRepositoryMessagesTest.kt`

| # | Тест | Описание |
|---|------|----------|
| 6 | ✅ `markDialogAsRead_whenApiSuccess_updatesDaoAndReturnsSuccess` | Проверяет вызов API + DAO update |
| 7 | ✅ `markDialogAsRead_whenApiFailure_returnsFailure` | Проверяет обработку ошибки |

---

## Технические детали

### DialogDao.updateUnreadCount

```kotlin
@Query("UPDATE dialogs SET unreadCount = 0 WHERE id = :dialogId")
suspend fun updateUnreadCount(dialogId: Long)
```

### SWRepository.markDialogAsRead (новый метод)

```kotlin
// В интерфейсе (строка ~119):
suspend fun markDialogAsRead(dialogId: Long, userId: Int): Result<Unit>

// Реализация (после markAsRead, строка ~768):
override suspend fun markDialogAsRead(dialogId: Long, userId: Int): Result<Unit> =
    try {
        swApi.markAsRead(userId.toLong())  // API: from_user_id
        dialogDao.updateUnreadCount(dialogId)  // DAO: реактивное обновление UI
        Result.success(Unit)
    } catch (e: IOException) {
        Result.failure(handleIOException(e, "отметке сообщений прочитанными"))
    } catch (e: HttpException) {
        Result.failure(handleHttpException(e, "отметке сообщений прочитанными"))
    }
```

### DialogsViewModel.isUpdating

```kotlin
private val _isMarkingAsRead = MutableStateFlow(false)
val isMarkingAsRead: StateFlow<Boolean> = _isMarkingAsRead.asStateFlow()

val isUpdating: StateFlow<Boolean> = combine(_isDeleting, _isMarkingAsRead) { deleting, marking ->
    deleting || marking
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
```

### DialogsViewModel — добавление ResourcesProvider

```kotlin
// Конструктор (добавить resources):
class DialogsViewModel(
    private val messagesRepository: MessagesRepository,
    private val swRepository: SWRepository,
    private val logger: Logger,
    private val resources: ResourcesProvider  // ← добавить
) : ViewModel(), IDialogsViewModel {
```

### DialogsViewModel.markDialogAsRead

```kotlin
override fun markDialogAsRead(dialogId: Long, userId: Int) {
    viewModelScope.launch {
        _isMarkingAsRead.value = true
        _syncError.value = null

        val result = swRepository.markDialogAsRead(dialogId, userId)
        _isMarkingAsRead.value = false

        if (result.isFailure) {
            logger.e(TAG, "markDialogAsRead failed: ${result.exceptionOrNull()?.message}")
            _syncError.value = resources.getString(R.string.sync_error_message)
        }
        // При успехе Flow из Room обновит unreadCount автоматически
    }
}
```

### Рефакторинг deleteDialog (использовать resources)

```kotlin
override fun deleteDialog(dialogId: Long) {
    viewModelScope.launch {
        _isDeleting.value = true
        val result = swRepository.deleteDialog(dialogId)
        _isDeleting.value = false

        if (result.isFailure) {
            _syncError.value = resources.getString(R.string.dialog_delete_error)
            logger.e(TAG, "Ошибка удаления диалога: ${result.exceptionOrNull()?.message}")
        }
    }
}
```

---

## Ключевые файлы

```
data/database/dao/DialogDao.kt            # updateUnreadCount()
data/repository/SWRepository.kt           # markDialogAsRead() — новый метод
ui/viewmodel/DialogsViewModel.kt          # markDialogAsRead(), isMarkingAsRead, isUpdating
ui/viewmodel/IDialogsViewModel.kt         # interface
ui/screens/messages/MessagesRootScreen.kt # DropdownMenuItem + LoadingOverlayView
res/values/strings.xml                    # mark_as_read
res/values-ru/strings.xml                 # перевод
```
