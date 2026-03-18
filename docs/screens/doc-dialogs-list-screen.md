# Экран Messages (Список диалогов)

## Описание

Экран списка диалогов с возможностью удаления, отметки прочитанными, запуском нового диалога и интеграцией авторизации.

**Связанные экраны:**
- [ChatScreen](doc-chat-screen.md) — экран чата с пользователем
- `MessagesFriendsPickerScreen` — выбор друга для старта нового диалога

---

## Реализованный функционал

### Загрузка списка диалогов

**Архитектура:** MVVM + Fallback cache (сервер → Room → UI, офлайн-режим)

**Компоненты:** `DialogEntity`, `DialogDao`, `MessagesRepository`, `DialogsViewModel` с `DialogsUiState`, `MessagesRootScreen`, `DialogRowView`

**Тесты:** покрыто unit-тестами ViewModel и репозитория (`DialogsViewModelTest`, `MessagesRepositoryTest`, `SWRepositoryMessagesTest`)

### Запуск нового диалога (FAB / Empty state CTA)

**UX:**
- в `DialogsUiState.Success` при непустом списке показывается `NewDialogFAB`;
- если у пользователя есть друзья: переход в `Screen.FriendsForDialog`;
- если друзей нет: переход в `Screen.UserSearch` (source=`messages`);
- в пустом состоянии используется тот же выбор действия через CTA.

**Реализация:** `MessagesNavigationAction.NavigateToFriends` / `NavigateToSearchUsers` в `MessagesRootScreen`, обработка маршрутов в `RootScreen`.

### Удаление диалогов

**UX:** Long Press → DropdownMenu → AlertDialog → API → LoadingOverlayView

**Реализация:** DropdownMenu рендерится на уровне `DialogsContent` (State Hoisting). `DialogRowData.onLongClick` делегирует позицию, `FormCardContainer.onLongClickWithOffset` использует `pointerInput` + `detectTapGestures`. `SWRepository.deleteDialog` выполняет API-запрос и удаляет из БД.

### Mark as Read

**UX:** Long Press → DropdownMenu → "Mark as read" (только если unreadCount > 0) → API → LoadingOverlayView

**Реализация:** `DialogDao.updateUnreadCount()` сбрасывает счётчик в БД. `DialogsViewModel.markDialogAsRead()` с флагом `isMarkingAsRead`. Общий индикатор `isUpdating = combine(isDeleting, isMarkingAsRead)`.

### Интеграция авторизации

**UX:** Для неавторизованных — `IncognitoProfileView` с кнопками "Authorize" и "Register". Для авторизованных — список диалогов или заглушка "Тут будут ваши сообщения". После logout — корректное отображение `IncognitoProfileView`.

**Реализация:**
- Параметры `appState: AppState` и `onAction: (MessagesNavigationAction) -> Unit` в `MessagesRootScreen`
- Проверка `!appState.isAuthorized` для отображения `IncognitoProfileView`
- Race condition при авторизации обрабатывается через `isLoadingDialogs`: показывается `LoadingOverlayView` во время загрузки диалогов после логина
- Переиспользование `LoginSheetHost` и `RegisterSheetHost` из `RootScreen`

### Автообновление после отправки сообщения

**UX:** после успешной отправки сообщения из `TextEntrySheetHost` список диалогов обновляется автоматически без ручного pull-to-refresh.

**Реализация:** `TextEntryUseCase` эмитит событие в `MessageSentNotifier`, `DialogsViewModel` подписывается на `messageSent` и вызывает `refresh()`.

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
ui/screens/messages/MessagesFriendsPickerScreen.kt
ui/ds/DialogRowView.kt
ui/ds/FormCardContainer.kt
ui/screens/RootScreen.kt
domain/event/MessageSentNotifier.kt
domain/usecase/TextEntryUseCase.kt

# Resources
res/values/strings.xml
res/values-ru/strings.xml
```

---

## Технические детали

- **Позиционирование DropdownMenu:** `positionInRoot()` относительно composition root, рендерится как sibling после Box на уровне `DialogsContent`
- **Форматирование:** `DateFormatter.formatDate()` для дат, Material3 `PullToRefreshBox`, `LoadingOverlayView`
- **Реактивность:** Flow из Room, `isUpdating = combine(isDeleting, isMarkingAsRead)`
- **Межэкранные события:** `MessageSentNotifier` (`SharedFlow`) для авто-refresh списка диалогов
- **Безопасные зоны:** `parentPaddingValues` от `RootScreen`
- **Навигация нового диалога:** route `friends_for_dialog` (`Screen.FriendsForDialog`) с скрытием нижней навигации
