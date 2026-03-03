# Экран Messages (Список диалогов)

## Статус: ✅ ЗАВЕРШЁН

---

## Описание

Экран списка диалогов с возможностью удаления, отметки прочитанными и интеграцией авторизации.

**Связанные экраны:**
- [ChatScreen](doc-chat-screen.md) — экран чата с пользователем

---

## Реализованный функционал

### Загрузка списка диалогов

**Архитектура:** MVVM + Fallback cache (сервер → Room → UI, офлайн-режим)

**Data Layer:** `DialogEntity`, `DialogDao`, `MessagesRepository`, миграция БД v2

**UI Layer:** `DialogsViewModel` с `DialogsUiState`, `MessagesRootScreen`, `DialogRowView`

**Тесты:** 11 unit-тестов ViewModel, 5 unit-тестов репозитория

**Исправленные баги:** logout cleanup, форматирование даты, центрирование empty/error states, pull-to-refresh на пустом списке, повторная авторизация, loading overlay, пустой ответ сервера, EmptyStateView после авторизации.

### Удаление диалогов

**UX:** Long Press → DropdownMenu → AlertDialog → API → LoadingOverlayView

**Ключевое решение:** DropdownMenu рендерится на уровне `DialogsContent` (State Hoisting), не внутри `DialogRowView`. Это корректно работает с `positionInRoot()`.

**Реализация:**
- `DialogRowData.onLongClick: (localOffset, itemPosition) -> Unit` — делегирует позицию
- `FormCardContainer.onLongClickWithOffset` — `pointerInput` + `detectTapGestures`
- `SWRepository.deleteDialog` — API + удаление из БД для реактивного обновления UI
- `DialogsViewModel.deleteDialog()` с флагом `isDeleting`

### Mark as Read

**UX:** Long Press → DropdownMenu → "Mark as read" (только если unreadCount > 0) → API → LoadingOverlayView

**Реализация:**
- `DialogDao.updateUnreadCount(dialogId)` — SQL UPDATE для сброса счётчика
- `SWRepository.markDialogAsRead(dialogId, userId)` — API + DAO update
- `DialogsViewModel.markDialogAsRead()` с флагом `isMarkingAsRead`
- `isUpdating = combine(isDeleting, isMarkingAsRead)` — общий индикатор загрузки
- Условное отображение пункта меню: `if ((dialog.unreadCount ?: 0) > 0)`

### Интеграция авторизации

**UX:**
- Для неавторизованных: `IncognitoProfileView` с кнопкой "Authorize"
- Для авторизованных: список диалогов или заглушка "Тут будут ваши сообщения"

**Реализация:**
- Параметры `appState: AppState` и `onShowLoginSheet: () -> Unit` в `MessagesRootScreen`
- Переиспользование общего `LoginSheetHost` для авторизации
- Кросс-экранная синхронизация состояния авторизации

---

## Ключевые файлы

```
# Data Layer
data/database/entity/DialogEntity.kt
data/database/dao/DialogDao.kt              # deleteById(), updateUnreadCount()
data/repository/SWRepository.kt             # deleteDialog(), markDialogAsRead()
domain/repository/MessagesRepository.kt
data/repository/MessagesRepositoryImpl.kt
data/model/DialogResponse.kt

# UI Layer
ui/viewmodel/DialogsViewModel.kt            # deleteDialog(), markDialogAsRead(), isUpdating
ui/viewmodel/IDialogsViewModel.kt
ui/screens/messages/MessagesRootScreen.kt   # DropdownMenu + AlertDialog + Auth
ui/ds/DialogRowView.kt                      # positionInRoot + onLongClick
ui/ds/FormCardContainer.kt                  # onLongClickWithOffset
ui/screens/RootScreen.kt                    # LoginSheetHost интеграция

# Resources
res/values/strings.xml                      # delete_dialog_*, mark_as_read, messages_placeholder
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
- `LoadingOverlayView` при загрузке и операциях

### Реактивность

- UI обновляется через Flow из Room
- `isUpdating = combine(isDeleting, isMarkingAsRead)` — общий индикатор

### Безопасные зоны

- `parentPaddingValues` от `RootScreen`

---

## Исправленные баги

### Баг: Race condition при авторизации — показывался IncognitoProfileView вместо диалогов ✅

**Симптомы:**
- После авторизации пользователь видит пустой экран с заголовком "Messages"
- Диалоги загружены с сервера (логи: `Получено 1 диалогов с сервера`)
- Логи показывают: `isLoadingDialogs=false, isAuthorized=false, uiState=Success, dialogsCount=1`

**Причина:**
Race condition между `isLoadingDialogs` и `appState.isAuthorized`. После успешного логина:
1. `isLoadingDialogs = true` — начало загрузки диалогов
2. Сервер возвращает диалоги → `uiState = Success`, `isLoadingDialogs = false`
3. Но `appState.isAuthorized` ещё `false` (обновляется позже, после загрузки профиля)

В результате условие `!appState.isAuthorized` истинно, и показывается `IncognitoProfileView` вместо `DialogsContent`.

**Решение:**
Добавлена проверка `hasLoadedDialogs = uiState is DialogsUiState.Success`. Теперь условие:
```kotlin
} else if (!appState.isAuthorized && !hasLoadedDialogs) {
```
Если диалоги уже загружены (Success), IncognitoProfileView не показывается даже если `isAuthorized` ещё `false`.

### Баг: EmptyStateView не отображался после авторизации ✅

**Симптомы:**
- После авторизации на экране диалогов виден только заголовок "Messages"
- EmptyStateView не отображался, хотя список диалогов пуст
- API возвращал данные (логи показывали `Получено 1 диалогов с сервера`)
- Логи показывали: `isLoadingDialogs=false, isAuthorized=true, uiState=Success`

**Причина:**
В `loadDialogsInternal()` было условие `if (currentState is DialogsUiState.Loading)`, которое не выполнялось, если uiState уже был Success (например, после предыдущей загрузки). В результате после повторной авторизации с пустым списком диалогов показывался старый Success вместо EmptyStateView.

**Решение:**
Убрано условие `if (currentState is DialogsUiState.Loading)`. Теперь после успешной загрузки с сервера всегда читаются данные из БД и устанавливается корректный uiState, независимо от текущего состояния.

**Тесты:**
- `loadDialogsAfterAuth_withPreviouslyCachedDialogs_andEmptyServerResponse_showsEmptyState` - проверяет повторную авторизацию с пустым списком
- `loadDialogsAfterAuth_whenFlowEmitsWithDelay_showsSuccessEmptyState` - проверяет случай с задержкой Flow

### Баг: DialogsContent не отображался — не передавался modifier ✅

**Симптомы:**
- После навигации на экран Messages виден только заголовок "Messages"
- Логи показывают корректные данные: `uiState=Success, dialogsCount=1`
- `DialogRowView` рендерится (логи: `Рендерим диалог: Ninenineone`)
- Но контент не виден на экране

**Причина:**
В `MessagesRootScreen` при вызове `DialogsContent` не передавался `modifier`:
```kotlin
// BUG: modifier не передан
DialogsContent(
    params = DialogsContentParams(...)
)
```

При этом `LoadingOverlayView` и `IncognitoProfileView` использовали `modifier` корректно.
Modifier из `NavHost` содержит `fillMaxSize()` и `padding(paddingValues)`, необходимые для отображения контента.

**Решение:**
Передать `modifier` в `DialogsContent`:
```kotlin
DialogsContent(
    modifier = modifier,
    params = DialogsContentParams(...)
)
```
