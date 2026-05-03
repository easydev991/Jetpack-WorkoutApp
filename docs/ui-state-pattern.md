# Паттерн управления UI-состоянием

**Цель:** Единый архитектурный подход к управлению UI-состоянием во всех экранах приложения.
Паттерн обеспечивает иммутабельность, предсказуемость и тестируемость.

Источник: коммит `dd7b305ce5b552b74b4a916c925f5f7b830bd3d5` в `JetpackDays`.

---

## Основные принципы

1. **Data-классы без `MutableState`.** Все UI-state модели — обычные `data class` / `sealed class`
   без полей типа `MutableState<T>`. Состояние иммутабельно и изменяется только через `copy()`.
2. **`StateFlow` в ViewModel.** ViewModel хранит состояние в `StateFlow<T>` и предоставляет
   его Compose-слою. ViewModel не экспортирует Compose state в контракте и не хранит
   `mutableStateOf` как форму.
3. **Compose-локальное состояние.** Для transient UI-флагов (меню, dialog, sheet, анимации)
   используется `remember` / `rememberSaveable`, если состояние не является частью
   бизнес-логики.
4. **Unidirectional data flow.** Состояние течёт вниз (в Composable через параметры),
   события — вверх (через callback'и или `Flow`).

## Иерархия состояний

```
ViewModel (StateFlow) ──► Compose Screen
                                │
                     ┌──────────┴──────────┐
                     │                     │
             ViewModel-управляемые    Локальные состояния
             состояния               (remember/rememberSaveable)
             (список сообщений,       ─ диалоги подтверждения
              детали события,         ─ showDatePicker
              форма логина)           ─ text entry sheet
                                      ─ анимационные флаги
```

## Когда что использовать

| Уровень | Механизм | Примеры |
|---------|----------|---------|
| ViewModel → UI | `StateFlow<T>` | `ChatUiState`, `LoginUiState`, список парков |
| Composable (сохраняемое) | `rememberSaveable` | Флаги sheet/dialog при повороте |
| Composable (временное) | `remember { mutableStateOf(...) }` | `expandedSection`, `showDatePicker` |
| UI-события | Callback'и (лямбды) | `onDeleteClick`, `onSave`, `onBack` |
| События ViewModel → UI | `SharedFlow<T>` / `Channel` | Snackbar, навигация, toast |

## Где разрешён `mutableStateOf`

- **Внутри Composable** / локального state-holder экрана (например,
  `remember { mutableStateOf(DetailOverlayState()) }`).
- **Внутри `AppState`** как отдельного навигационного Compose state-holder.
- **НЕ в ViewModel** (замена на `MutableStateFlow`).
- **НЕ в `ui/state` моделях** (остаются plain Kotlin классами).

> **Исключение:** `AppState` использует `mutableStateOf` для `currentUser`,
> `currentTopLevelDestination` и `bottomNavVisualEpoch`. Это осознанное решение:
> `AppState` является глобальным state-holder'ом для навигации и авторизации,
> его механический перенос на `StateFlow` не требуется.

## Новые чтения `StateFlow` в Compose

Новые экраны и изменяемые при рефакторе экраны по умолчанию собирают `StateFlow`
через `collectAsStateWithLifecycle()`:

```kotlin
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SomeScreen(viewModel: SomeViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // ...
}
```

Зависимость `androidx.lifecycle:lifecycle-runtime-compose` уже подключена.
Метод `collectAsState()` без Lifecycle-aware остаётся только в обоснованных случаях
с локальным объяснением причины. Однократные события не превращать в State:
для них использовать `LaunchedEffect` и сбор `Flow` / `SharedFlow`.

## Пример (Chat)

```kotlin
// 1. ViewModel — StateFlow, без MutableState
interface IChatViewModel {
    val uiState: StateFlow<ChatUiState>
    val messageText: StateFlow<String>
    fun onMessageTextChange(value: String)
    fun sendMessage()
}

class ChatViewModel(...) : IChatViewModel {
    private val _messageText = MutableStateFlow("")
    override val messageText: StateFlow<String> = _messageText.asStateFlow()

    override fun onMessageTextChange(value: String) {
        _messageText.value = value
    }

    override fun sendMessage() {
        val text = _messageText.value.trim()
        // ... use case, очистка
        _messageText.value = ""
    }
}

// 2. Composable — collectAsStateWithLifecycle + plain значения
@Composable
fun ChatScreen(viewModel: IChatViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messageText by viewModel.messageText.collectAsStateWithLifecycle()

    ChatContent(
        state = uiState,
        messageText = messageText,
        onMessageTextChange = viewModel::onMessageTextChange,
        onSendClick = viewModel::sendMessage
    )
}

// 3. Дочерний Composable — plain значения и callback'и
@Composable
fun ChatContent(
    state: ChatUiState,
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    // ...
}
```

## Проверка при code review

- [ ] В `ui/viewmodel/` нет `import androidx.compose.runtime.MutableState` / `mutableStateOf`
      для формовых данных.
- [ ] ViewModel экспортирует состояние как `StateFlow`, однократные события — как `Flow` / `SharedFlow`.
- [ ] Новые чтения `StateFlow` в экранах используют `collectAsStateWithLifecycle()`,
      а не `collectAsState()`.
- [ ] Дочерние Composable принимают plain-значения и callback'и, а не `MutableState<T>`.
- [ ] В `ui/state/` моделях нет `import` `getValue`/`setValue`.
- [ ] Dialog/sheet состояние закрывается и на confirm, и на dismiss
      (нет висящих полуоткрытых состояний).
- [ ] `AppState` не затронут без отдельного архитектурного решения.

## Навигация

Параметры навигации (itemId) передаются через `SavedStateHandle` с `checkNotNull`:

```kotlin
private val itemId: Long = checkNotNull(savedStateHandle["itemId"]) {
    "itemId parameter is required"
}
```

## Рекомендуемый PR-порядок

1. **PR 1:** проектный reference-документ + `ChatViewModel` migration.
2. **PR 2:** `LoginViewModel` form state migration; удалить compatibility getter `credentials`.
3. **PR 3 (опционально):** точечный cleanup overlay state в `ParkDetailScreen` / `EventDetailScreen`.
4. **PR 4 (опционально):** journals/root overlay cleanup при наличии сценарного риска.
