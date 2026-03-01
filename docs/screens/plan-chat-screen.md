# План: Экран диалога (ChatScreen)

## Описание

Экран для отображения переписки с пользователем: список сообщений, поле ввода, кнопка отправки.

## Референс

- **iOS:** `SwiftUI-WorkoutApp/Screens/Messages/DialogScreen.swift`

## Текущее состояние

### Завершено ✅

- **UI компоненты:** `SendChatMessageButton`, `ChatInputBar`, `MessageBubbleView`
- **Data/Domain:** `MessageResponse`, `DialogEntity`, `ChatUiState`, `IChatViewModel`
- **ViewModel:** `ChatViewModel` + 11 unit-тестов
- **API:** `getMessages()`, `sendMessageTo()`, `markAsRead()`
- **ChatScreen.kt:** TopAppBar, LazyColumn с сообщениями, ChatInputBar, автопрокрутка, markAsRead, Preview-функции
- **Навигация:** Screen.Chat маршрут в RootScreen
- **Ресурсы:** `chat_empty_messages` (en/ru)

### Требуется ❌

1. `ChatScreenTest.kt` - UI-тесты

---

## Этап 5: UI-тесты ❌ ТЕКУЩИЙ

### 5.1 ChatScreenTest (UI-тесты)

**Файл:** `app/src/androidTest/java/com/swparks/ui/screens/messages/ChatScreenTest.kt`

**Подход:** Тестировать `ChatContent` в изоляции (как MessagesRootScreenTest), без реальной ViewModel.

**Сценарии:**
- Loading state - отображается LoadingOverlayView
- Success state - отображаются сообщения
- Пустой диалог - отображается текст "Сообщений пока нет"
- Поле ввода отображается
- Кнопка отправки disabled когда сообщение пустое
- Кнопка отправки enabled когда сообщение не пустое
- onSendMessage callback вызывается при клике
- onRefresh callback вызывается при клике на кнопку обновления
- onAvatarClick callback вызывается при клике на аватар

**НЕ тестируется в UI-тестах (покрыто unit-тестами):**
- Error state (ошибки через тосты на корневом экране)
- markAsRead логика
- Автопрокрутка (низкая ценность для UI-теста)

---

## Критерии завершения

- [ ] UI-тесты проходят
- [ ] `make format` без ошибок
- [ ] `make lint` без ошибок

---

## Оценка времени

**Осталось:** 1-1.5 часов (только UI-тесты)
