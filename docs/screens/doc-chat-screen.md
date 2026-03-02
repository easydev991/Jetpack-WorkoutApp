# Экран диалога (ChatScreen)

Экран чата для обмена сообщениями с другим пользователем.

## Файлы

- **UI:** `app/src/main/java/com/swparks/ui/screens/messages/ChatScreen.kt`
- **ViewModel:** `app/src/main/java/com/swparks/ui/viewmodel/ChatViewModel.kt`
- **Интерфейс ViewModel:** `app/src/main/java/com/swparks/ui/viewmodel/IChatViewModel.kt`
- **Состояние UI:** `app/src/main/java/com/swparks/ui/state/ChatUiState.kt`
- **События:** `app/src/main/java/com/swparks/ui/state/ChatEvent.kt`
- **Тесты:** `app/src/test/java/com/swparks/ui/viewmodel/ChatViewModelTest.kt`

## Навигация

Маршрут: `Screen.Chat(dialogId, userName, userImage, otherUserId)`

Параметры передаются из `DialogsListScreen` при нажатии на диалог.

## Логика работы

### Загрузка сообщений

1. `loadMessages(dialogId)` вызывается при открытии экрана
2. Сообщения загружаются через `SWApi.getMessages()` и сортируются хронологически
3. Состояние: `Loading` → `Success(messages)` или `Error`

### Отправка сообщения

1. Пользователь вводит текст в `ChatInputBar`
2. При нажатии отправки вызывается `sendMessage(userId)`
3. После успешной отправки эмитится событие `ChatEvent.MessageSent`
4. Событие используется для обновления списка диалогов на родительском экране

### MarkAsRead

1. При прокрутке к последнему сообщению проверяется видимость
2. Если последнее входящее сообщение видно — вызывается `markAsRead(userId)`
3. Используется `SWRepository.markDialogAsRead()` — обновляет и сервер, и локальную БД
4. Room Flow автоматически обновляет бейдж непрочитанных на `MessagesRootScreen`

### UI-компоненты

- **TopAppBar:** аватар собеседника (кликабельный), имя, кнопки назад и refresh
- **LazyColumn:** список сообщений с автопрокруткой к последнему
- **ChatInputBar:** поле ввода с кнопкой отправки
- **LoadingOverlay:** блокирует UI при загрузке/отправке

## Состояния

- `ChatUiState.Loading` — начальная загрузка
- `ChatUiState.Success(messages)` — сообщения загружены
- `ChatUiState.Error(message)` — ошибка (отображается через `UserNotifier`)

## Зависимости ViewModel

- `SWApi` — сетевые запросы
- `SWRepository` — обновление локальной БД (markAsRead)
- `UserNotifier` — отображение ошибок в UI
