# Экран Messages (Список диалогов)

## Описание

Экран списка диалогов с возможностью удаления, отметки прочитанными и интеграцией авторизации.

**Связанные экраны:**
- [ChatScreen](doc-chat-screen.md) — экран чата с пользователем

---

## Реализованный функционал

### Загрузка списка диалогов

**Архитектура:** MVVM + Fallback cache (сервер → Room → UI, офлайн-режим)

**Компоненты:** `DialogEntity`, `DialogDao`, `MessagesRepository`, `DialogsViewModel` с `DialogsUiState`, `MessagesRootScreen`, `DialogRowView`

**Тесты:** 11 unit-тестов ViewModel, 5 unit-тестов репозитория

### Удаление диалогов

**UX:** Long Press → DropdownMenu → AlertDialog → API → LoadingOverlayView

**Реализация:** DropdownMenu рендерится на уровне `DialogsContent` (State Hoisting). `DialogRowData.onLongClick` делегирует позицию, `FormCardContainer.onLongClickWithOffset` использует `pointerInput` + `detectTapGestures`. `SWRepository.deleteDialog` выполняет API-запрос и удаляет из БД.

### Mark as Read

**UX:** Long Press → DropdownMenu → "Mark as read" (только если unreadCount > 0) → API → LoadingOverlayView

**Реализация:** `DialogDao.updateUnreadCount()` сбрасывает счётчик в БД. `DialogsViewModel.markDialogAsRead()` с флагом `isMarkingAsRead`. Общий индикатор `isUpdating = combine(isDeleting, isMarkingAsRead)`.

### Интеграция авторизации

**UX:** Для неавторизованных — `IncognitoProfileView` с кнопками "Authorize" и "Register". Для авторизованных — список диалогов или заглушка "Тут будут ваши сообщения". После logout — корректное отображение `IncognitoProfileView`.

**Реализация:**
- Параметры `appState: AppState` и `callbacks: MessagesNavigationCallbacks` в `MessagesRootScreen`
- Проверка `!appState.isAuthorized` для отображения `IncognitoProfileView`
- Race condition при авторизации обрабатывается через `isLoadingDialogs`: показывается `LoadingOverlayView` во время загрузки диалогов после логина
- Переиспользование `LoginSheetHost` и `RegisterSheetHost` из `RootScreen`

---

## Ключевые файлы

```
# Data Layer
data/database/entity/DialogEntity.kt
data/database/dao/DialogDao.kt
data/repository/SWRepository.kt
domain/repository/MessagesRepository.kt
data/repository/MessagesRepositoryImpl.kt
data/model/DialogResponse.kt

# UI Layer
ui/viewmodel/DialogsViewModel.kt
ui/viewmodel/IDialogsViewModel.kt
ui/screens/messages/MessagesRootScreen.kt
ui/ds/DialogRowView.kt
ui/ds/FormCardContainer.kt
ui/screens/RootScreen.kt

# Resources
res/values/strings.xml
res/values-ru/strings.xml
```

---

## Технические детали

- **Позиционирование DropdownMenu:** `positionInRoot()` относительно composition root, рендерится как sibling после Box на уровне `DialogsContent`
- **Форматирование:** `DateFormatter.formatDate()` для дат, Material3 `PullToRefreshBox`, `LoadingOverlayView`
- **Реактивность:** Flow из Room, `isUpdating = combine(isDeleting, isMarkingAsRead)`
- **Безопасные зоны:** `parentPaddingValues` от `RootScreen`
