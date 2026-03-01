# План: Экран диалога (ChatScreen)

## Описание

Экран для отображения переписки с пользователем. Содержит:
- Список сообщений с автоматической прокруткой к последнему
- Поле ввода сообщения
- Кнопку отправки сообщения

## Референс

- **iOS:** `SwiftUI-WorkoutApp/Screens/Messages/DialogScreen.swift`
- **iOS компоненты:**
  - `ChatBubbleRowView.swift` - сообщение в чате
  - `SendChatMessageButton.swift` - кнопка отправки (круглая с иконкой стрелки вверх)

## Текущее состояние Android

### Уже реализовано ✅

- `MessageBubbleView.kt` - компонент для отображения сообщения (аналог ChatBubbleRowView)
- `MessageResponse.kt` - модель ответа сообщения
- `DialogEntity.kt` - модель диалога в БД
- `Screen.Chat` - маршрут навигации
- `MessagesRootScreen.kt` - список диалогов
- API методы:
  - `getMessages(dialogId: Long)` - получение сообщений диалога
  - `sendMessageTo(userId: Int, message: String)` - отправка сообщения
  - `markAsRead(userId: Int)` - пометить как прочитанное

### Требуется реализовать ❌

1. `SendChatMessageButton` - кнопка отправки (круглая с иконкой стрелки вверх)
2. `ChatInputBar` - компонент ввода сообщения (TextField + кнопка отправки)
3. `ChatViewModel` - ViewModel для экрана
4. `ChatUiState` - состояние UI
5. `ChatScreen.kt` - экран диалога
6. Навигация из MessagesRootScreen на ChatScreen

---

## Этап 1: UI Компоненты (без логики)

### 1.1 SendChatMessageButton

**Файл:** `app/src/main/java/com/swparks/ui/ds/SendChatMessageButton.kt`

**Описание:** Круглая кнопка с иконкой стрелки вверх для отправки сообщения.

**Референс iOS:**

```swift
// SendChatMessageButton.swift
Button(action: action) {
    Circle()
        .fill(isEnabled ? Color.swAccent : Color.swDisabledButton)
        .frame(width: 39, height: 39)
        .overlay {
            Icons.Regular.arrowUp.view
                .foregroundStyle(Color.swBackground)
        }
}
```

**Реализация Android:**
- CircleShape с размером 39.dp
- Цвет: `primary` (enabled) или `surfaceVariant` (disabled)
- Иконка: `Icons.AutoMirrored.Filled.ArrowUp` (или `Icons.Filled.KeyboardArrowUp`)
- Цвет иконки: `onPrimary` (enabled) или `onSurfaceVariant` (disabled)
- Анимация при изменении enabled состояния

### 1.2 ChatInputBar

**Файл:** `app/src/main/java/com/swparks/ui/ds/ChatInputBar.kt`

**Описание:** Компонент с полем ввода сообщения и кнопкой отправки.

**Использует:** `SendChatMessageButton` из пункта 1.1

**Референс iOS:**

```swift
// sendMessageBar
HStack(spacing: 10) {
    newMessageTextField
        .frame(height: 42)
        .padding(.horizontal, 8)
        .overlay(RoundedRectangle...)
    SendChatMessageButton { sendMessage() }
        .disabled(isSendButtonDisabled)
}
.padding()
```

**Реализация Android:**
- Row с TextField и `SendChatMessageButton` (из пункта 1.1)
- TextField: BasicTextField или OutlinedTextField с min height 42.dp
- Состояние фокуса для изменения border color
- Передача `enabled = text.isNotEmpty() && !isLoading` в SendChatMessageButton

---

## Этап 2: Domain Layer (интерфейсы и модели)

### 2.1 ChatUiState

**Файл:** `app/src/main/java/com/swparks/ui/state/ChatUiState.kt`

**Описание:** Sealed class для состояний экрана чата.

```kotlin
sealed class ChatUiState {
    data object Loading : ChatUiState()
    data class Success(val messages: List<MessageResponse>) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}
```

### 2.2 IChatViewModel (интерфейс)

**Файл:** `app/src/main/java/com/swparks/ui/viewmodel/IChatViewModel.kt`

**Методы:**
- `val uiState: StateFlow<ChatUiState>`
- `val isLoading: StateFlow<Boolean>`
- `val messageText: MutableState<String>`
- `fun loadMessages(dialogId: Long)` - загрузка сообщений при открытии экрана
- `fun refreshMessages()` - обновление списка сообщений (вызывается после успешной отправки и по кнопке обновления)
- `fun sendMessage(userId: Int)` - отправка сообщения, после успеха вызывает `refreshMessages()`
- `fun markAsRead(userId: Int)` - пометить диалог как прочитанный (вызывается при появлении последнего сообщения на экране)

---

## Этап 3: Unit-тесты ChatViewModel (TDD - Red)

**⚠️ TDD: Сначала пишем тесты, потом реализацию!**

### 3.1 ChatViewModelTest

**Файл:** `app/src/test/java/com/swparks/ui/viewmodel/ChatViewModelTest.kt`

**Моки:**
- `SWApi` - для API запросов
- `UserPreferencesRepository` - для userId
- `UserNotifier` - для проверки вызова `handleError()`

**Сценарии:**
- Initial state is Loading
- loadMessages updates uiState to Success
- loadMessages handles errors and calls userNotifier.handleError
- sendMessage clears messageText and calls refreshMessages on success
- sendMessage handles empty message (not sent)
- sendMessage handles error and calls userNotifier.handleError
- refreshMessages called on demand
- refreshMessages handles error and calls userNotifier.handleError
- markAsRead not called before messages loaded
- markAsRead called when triggered after successful load

**Подход:**
- Использовать MockK для мокирования SWApi и UserNotifier
- Использовать `MainDispatcherRule` для тестирования корутин
- Проверять вызов `userNotifier.handleError()` при ошибках
- Тесты должны падать (Red) до реализации ViewModel

---

## Этап 4: Data Layer - ChatViewModel (TDD - Green)

### 4.1 ChatViewModel

**Файл:** `app/src/main/java/com/swparks/ui/viewmodel/ChatViewModel.kt`

**Зависимости:**
- `SWApi` - для API запросов
- `UserPreferencesRepository` - для получения текущего userId
- `UserNotifier` - для отправки ошибок в UI-слой (отображение тоста на корневом экране)

**Логика:**

**loadMessages(dialogId):**
- Загрузка сообщений при инициализации экрана
- Обновление `uiState` (Loading → Success/Error)
- При ошибке: `userNotifier.handleError(AppError.Generic(message, error))`

**refreshMessages():**
- Принудительное обновление списка сообщений с сервера
- Вызывается:
  1. После успешной отправки сообщения (`sendMessage`)
  2. По нажатию на кнопку обновления в TopAppBar
- При ошибке: `userNotifier.handleError(AppError.Generic(message, error))`

**sendMessage(userId):**
- Отправка сообщения на сервер
- При успехе: очистка `messageText`, вызов `refreshMessages()`
- При ошибке: `userNotifier.handleError(AppError.Generic(message, error))`
- **Важно:** Ошибки отправляются через `userNotifier.handleError()` для отображения тоста на корневом экране

**markAsRead(userId):**
- Вызывается **только** при появлении последнего сообщения на экране (событие видимости)
- Не вызывать раньше, чем успешно загрузится список сообщений
- Использовать `LazyListState` или `LaunchedEffect` для отслеживания видимости последнего элемента
- При ошибке: логировать в консоль, не беспокоить пользователя

**Цель:** Сделать все тесты из Этапа 3 зелеными (Green)

---

## Этап 5: UI Screen

### 5.1 ChatScreen

**Файл:** `app/src/main/java/com/swparks/ui/screens/messages/ChatScreen.kt`

**Компоненты:**
- TopAppBar:
  - Имя собеседника в title
  - Аватар собеседника справа (клик → переход в профиль)
  - Кнопка обновления слева → вызывает `viewModel.refreshMessages()`
- LazyColumn с сообщениями (MessageBubbleView)
- ChatInputBar внизу экрана
- LoadingOverlayView при загрузке
- Автопрокрутка к последнему сообщению

**Референс iOS:**

```swift
VStack {
    makeScrollView(with: proxy)
        .scrollDismissesKeyboard(.interactively)
    sendMessageBar
}
```

**Особенности:**
- Использовать `LaunchedEffect` для прокрутки к последнему сообщению
- Использовать `rememberLazyListState` для управления прокруткой
- Использовать `focusRequester` для управления фокусом поля ввода
- **markAsRead вызов:**
  - Отслеживать появление последнего сообщения на экране через `snapshotFlow { lazyListState.firstVisibleItemIndex }` или `LaunchedEffect`
  - Вызывать `viewModel.markAsRead(userId)` только когда последнее сообщение стало видимым
  - Не вызывать markAsRead до успешной загрузки сообщений

---

## Этап 6: Навигация

### 6.1 Интеграция с Navigation.kt

**Изменения:**
- Добавить composable для `Screen.Chat`
- Передавать dialogId и другую информацию через аргументы
- Добавить переход из DialogRowView в ChatScreen

### 6.2 DialogsViewModel

**Изменения:**
- Реализовать `onDialogClick` для навигации на ChatScreen

---

## Этап 7: UI-тесты ChatScreen

### 7.1 ChatScreenTest

**Файл:** `app/src/androidTest/java/com/swparks/ui/screens/messages/ChatScreenTest.kt`

**Сценарии:**
- Displays loading state
- Displays messages in success state
- Displays error state
- Send button disabled when message is empty
- Send button enabled when message is not empty
- Message sent on button click
- Refresh button in TopAppBar triggers refreshMessages
- Auto-scroll to last message works

---

## Зависимости между этапами (TDD)

```
Этап 1: UI Компоненты (без логики)
    ↓
Этап 2: Domain Layer (интерфейсы, модели)
    ↓
Этап 3: Unit-тесты ChatViewModel (TDD - Red) ⚠️
    ↓
Этап 4: ChatViewModel реализация (TDD - Green) ⚠️
    ↓
Этап 5: UI Screen (ChatScreen)
    ↓
Этап 6: Навигация
    ↓
Этап 7: UI-тесты ChatScreen
```

---

## Критерии завершения

- [ ] SendChatMessageButton реализован с Material3 стилем
- [ ] ChatInputBar реализован с фокус-состоянием
- [ ] ChatUiState и IChatViewModel интерфейс созданы
- [ ] Unit-тесты ChatViewModel написаны (TDD - Red)
- [ ] ChatViewModel реализован, тесты проходят (TDD - Green)
- [ ] Ошибки обрабатываются через `userNotifier.handleError()` для отображения тостов
- [ ] ChatScreen отображает список сообщений
- [ ] Автопрокрутка к последнему сообщению работает
- [ ] Отправка сообщений работает
- [ ] Навигация из списка диалогов работает
- [ ] UI-тесты проходят
- [ ] `make format` без ошибок
- [ ] `make lint` без ошибок

---

## Оценка времени

- Этап 1 (UI компоненты): 1-2 часа
- Этап 2 (Domain - интерфейсы): 30 мин
- Этап 3 (Unit-тесты - TDD Red): 1-2 часа
- Этап 4 (ChatViewModel - TDD Green): 1-2 часа
- Этап 5 (UI Screen): 2-3 часа
- Этап 6 (Навигация): 30 мин
- Этап 7 (UI-тесты): 1-2 часа

**Итого:** 8-12 часов
