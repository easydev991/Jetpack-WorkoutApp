# JournalSettingsDialog - Диалог настроек дневника

## Описание

Диалог для редактирования настроек дневника пользователя, аналогичный `JournalSettingsScreen` в iOS-приложении. Диалог показывается при нажатии на действие "Настроить" (JournalAction.SETUP) в меню дневника на экране JournalsListScreen.

## Ссылки на референсы

- **iOS-реализация**: `/Users/Oleg991/Documents/GitHub/SwiftUI-WorkoutApp/SwiftUI-WorkoutApp/Screens/Profile/Journals/JournalSettingsScreen.swift`
- **nowinandroid SettingsDialog**: Использовать как референс для AlertDialog с RadioButton опциями
- **Android JournalAccess enum**: `app/src/main/java/com/swparks/ui/model/JournalAccess.kt`
- **Android EditJournalSettingsRequest**: `app/src/main/java/com/swparks/ui/model/EditJournalSettingsRequest.kt`

## Функциональность

Диалог должен содержать следующие элементы:

1. **Текстовое поле для названия дневника**
   - Placeholder: `R.string.journal_title_placeholder` ("Название дневника")
   - При открытии диалога подставляется текущее название дневника
   - Пользователь может изменять название

2. **Настройка "Кто видит записи"**
   - Заголовок: `R.string.read_access` ("Кто видит записи")
   - Три варианта (RadioButton):
     - `R.string.everybody_access` ("Все") → `JournalAccess.ALL`
     - `R.string.friends_access` ("Друзья") → `JournalAccess.FRIENDS`
     - `R.string.only_me_access` ("Только я") → `JournalAccess.NOBODY`
   - При открытии диалога подставляется текущее значение `viewAccess`

3. **Настройка "Кто может оставлять комментарии"**
   - Заголовок: `R.string.comment_access` ("Кто может оставлять комментарии")
   - Три варианта (RadioButton):
     - `R.string.everybody_access` ("Все") → `JournalAccess.ALL`
     - `R.string.friends_access` ("Друзья") → `JournalAccess.FRIENDS`
     - `R.string.only_me_access` ("Только я") → `JournalAccess.NOBODY`
   - При открытии диалога подставляется текущее значение `commentAccess`

4. **Кнопка сохранения**
   - Текст: `R.string.save` ("Сохранить") - **УЖЕ ЕСТЬ** в strings.xml
   - Активна только если:
     - Название не пустое
     - Есть изменения по сравнению с исходным состоянием (title, viewAccess или commentAccess изменились)
   - При нажатии отправляет запрос на сервер через ViewModel
   - Во время запроса кнопка блокируется и показывается индикатор загрузки

5. **Кнопка закрытия**
   - Кнопка с иконкой крестика в правом верхнем углу заголовка диалога
   - Текст `R.string.close` - **УЖЕ ЕСТЬ** в strings.xml
   - При нажатии закрывает диалог без сохранения

## Архитектурные слои

### 1. Domain Layer

#### Модели данных

- `JournalAccess` enum - **УЖЕ ЕСТЬ** (все три варианта: ALL, FRIENDS, NOBODY)
- `Journal` domain model - **УЖЕ ЕСТЬ**
- Extension `JournalAccess.canCreateEntry()` - **УЖЕ ЕСТЬ** (для проверки прав на создание записей)

### 2. Data Layer

#### API и Repository

- `SWApi.editJournalSettings()` - **УЖЕ ЕСТЬ**
- `SWRepository.editJournalSettings()` - **УЖЕ ЕСТЬ**
- `EditJournalSettingsRequest` - **УЖЕ ЕСТЬ**

### 3. Presentation Layer

#### ViewModel

##### Изменения в `IJournalsViewModel.kt`

**Добавить событие в `JournalsEvent` sealed class:**

```kotlin
sealed class JournalsEvent {
    // ... существующие события
    data class JournalSettingsSaved(val journal: Journal) : JournalsEvent()
}
```

**Добавить метод (не suspend, без callback):**

```kotlin
/**
 * Редактировать настройки дневника.
 *
 * После успешного обновления эмитится событие [JournalsEvent.JournalSettingsSaved].
 * При ошибке эмитится событие [JournalsEvent.ShowSnackbar].
 *
 * @param journalId Идентификатор дневника
 * @param title Новое название дневника
 * @param viewAccess Новый уровень доступа для просмотра
 * @param commentAccess Новый уровень доступа для комментариев
 */
fun editJournalSettings(
    journalId: Long,
    title: String,
    viewAccess: JournalAccess,
    commentAccess: JournalAccess
)
```

**Добавить состояние загрузки в `JournalsUiState.Content`:**

```kotlin
data class Content(
    val journals: List<Journal>,
    val isRefreshing: Boolean = false,
    val isSavingJournalSettings: Boolean = false // <-- Новое поле
) : JournalsUiState()
```

##### Изменения в `JournalsViewModel.kt`

**Реализовать метод:**

```kotlin
override fun editJournalSettings(
    journalId: Long,
    title: String,
    viewAccess: JournalAccess,
    commentAccess: JournalAccess
) {
    viewModelScope.launch {
        val userId = _currentUser.value?.id
        if (userId == null) {
            events.emit(JournalsEvent.ShowSnackbar(
                message = context.getString(R.string.error_user_not_found)
            ))
            return@launch
        }

        // Установить флаг загрузки только для состояния Content
        _uiState.update { state ->
            if (state is JournalsUiState.Content) {
                state.copy(isSavingJournalSettings = true)
            } else {
                state
            }
        }

        try {
            val result = swRepository.editJournalSettings(
                journalId = journalId,
                title = title,
                userId = userId,
                viewAccess = viewAccess,
                commentAccess = commentAccess
            )

            result.fold(
                onSuccess = { updatedJournal ->
                    // Обновить список дневников с новыми данными от сервера
                    _uiState.update { state ->
                        if (state is JournalsUiState.Content) {
                            val updatedJournals = state.journals.map { journal ->
                                if (journal.id == updatedJournal.id) updatedJournal else journal
                            }
                            state.copy(
                                journals = updatedJournals,
                                isSavingJournalSettings = false
                            )
                        } else {
                            state
                        }
                    }

                    // Эмитить событие об успешном сохранении
                    events.emit(JournalsEvent.JournalSettingsSaved(updatedJournal))

                    events.emit(JournalsEvent.ShowSnackbar(
                        message = context.getString(R.string.journal_settings_saved)
                    ))
                },
                onFailure = { error ->
                    // Сбросить флаг загрузки
                    _uiState.update { state ->
                        if (state is JournalsUiState.Content) {
                            state.copy(isSavingJournalSettings = false)
                        } else {
                            state
                        }
                    }

                    events.emit(JournalsEvent.ShowSnackbar(
                        message = error.message ?: context.getString(R.string.error_save_journal_settings)
                    ))
                }
            )
        } catch (e: Exception) {
            // Сбросить флаг загрузки
            _uiState.update { state ->
                if (state is JournalsUiState.Content) {
                    state.copy(isSavingJournalSettings = false)
                } else {
                    state
                }
            }

            events.emit(JournalsEvent.ShowSnackbar(
                message = context.getString(R.string.error_save_journal_settings)
            ))
        }
    }
}
```

#### UI Components

##### Создать `JournalSettingsDialog.kt`

**Расположение:** `app/src/main/java/com/swparks/ui/screens/journals/JournalSettingsDialog.kt`

**Функционал:**

1. **Основная Composable функция**:

```kotlin
@Composable
fun JournalSettingsDialog(
    journal: Journal,
    onDismiss: () -> Unit,
    viewModel: IJournalsViewModel,
    uiState: JournalsUiState
)
```

2. **Структура UI** (аналогично SettingsDialog из nowinandroid):

```kotlin
// Локальное состояние диалога
var title by rememberSaveable { mutableStateOf(journal.title) }
var viewAccess by rememberSaveable { mutableStateOf(journal.viewAccess) }
var commentAccess by rememberSaveable { mutableStateOf(journal.commentAccess) }
var titleAttemptedSave by remember { mutableStateOf(false) }

// Проверка, есть ли изменения
val hasChanges = remember(title, viewAccess, commentAccess, journal) {
    title != journal.title ||
    viewAccess != journal.viewAccess ||
    commentAccess != journal.commentAccess
}

// Кнопка "Сохранить" активна только если название не пустое и есть изменения
val isSaveButtonEnabled = title.isNotBlank() && hasChanges
val isSaving = (uiState as? JournalsUiState.Content)?.isSavingJournalSettings ?: false

val configuration = LocalConfiguration.current

AlertDialog(
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier.widthIn(max = configuration.screenWidthDp.dp - 80.dp),
    onDismissRequest = onDismiss,
    title = {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.journal_settings),
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close)
                )
            }
        }
    },
    text = {
        HorizontalDivider()
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            // Текстовое поле для названия
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.journal_title_placeholder)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                isError = title.isBlank() && titleAttemptedSave
            )

            // Кто видит записи
            SettingsDialogSectionTitle(
                text = stringResource(R.string.read_access),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            JournalAccessGroup(
                options = listOf(
                    JournalAccessOption(
                        access = JournalAccess.ALL,
                        textRes = R.string.everybody_access
                    ),
                    JournalAccessOption(
                        access = JournalAccess.FRIENDS,
                        textRes = R.string.friends_access
                    ),
                    JournalAccessOption(
                        access = JournalAccess.NOBODY,
                        textRes = R.string.only_me_access
                    )
                ),
                selected = viewAccess,
                onSelected = { viewAccess = it }
            )

            // Кто может оставлять комментарии
            SettingsDialogSectionTitle(
                text = stringResource(R.string.comment_access),
                modifier = Modifier.padding(horizontal = 16.dp, top = 16.dp)
            )
            JournalAccessGroup(
                options = listOf(
                    JournalAccessOption(
                        access = JournalAccess.ALL,
                        textRes = R.string.everybody_access
                    ),
                    JournalAccessOption(
                        access = JournalAccess.FRIENDS,
                        textRes = R.string.friends_access
                    ),
                    JournalAccessOption(
                        access = JournalAccess.NOBODY,
                        textRes = R.string.only_me_access
                    )
                ),
                selected = commentAccess,
                onSelected = { commentAccess = it }
            )
            HorizontalDivider(Modifier.padding(top = 8.dp))
        }
    },
    confirmButton = {
        Button(
            onClick = {
                if (title.isNotBlank()) {
                    viewModel.editJournalSettings(
                        journalId = journal.id,
                        title = title,
                        viewAccess = viewAccess,
                        commentAccess = commentAccess
                    )
                    // Диалог закроется по событию JournalSettingsSaved
                } else {
                    titleAttemptedSave = true
                }
            },
            enabled = isSaveButtonEnabled && !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(stringResource(R.string.save))
            }
        }
    }
)
```

3. **Вспомогательные типы и функции**:

```kotlin
private data class JournalAccessOption(
    val access: JournalAccess,
    @StringRes val textRes: Int
)

@Composable
private fun SettingsDialogSectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun JournalAccessGroup(
    options: List<JournalAccessOption>,
    selected: JournalAccess,
    onSelected: (JournalAccess) -> Unit
) {
    Column(Modifier.selectableGroup().padding(horizontal = 16.dp)) {
        options.forEach { option ->
            JournalAccessRow(
                text = stringResource(option.textRes),
                selected = selected == option.access,
                onClick = { onSelected(option.access) }
            )
        }
    }
}

@Composable
private fun JournalAccessRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}
```

4. **Предпросмотры**:

```kotlin
// Для preview создать простой mock
private val mockViewModel = object : IJournalsViewModel {
    override fun editJournalSettings(
        journalId: Long,
        title: String,
        viewAccess: JournalAccess,
        commentAccess: JournalAccess
    ) {
        // no-op
    }
    // ... остальные методы интерфейса
}

@Preview
@Composable
private fun PreviewJournalSettingsDialog() {
    JetpackWorkoutAppTheme {
        val sampleJournal = Journal(
            id = 1L,
            title = "Мой дневник",
            lastMessageImage = null,
            createDate = "2023-01-01",
            modifyDate = null,
            lastMessageDate = null,
            lastMessageText = null,
            entriesCount = 10,
            ownerId = 1L,
            viewAccess = JournalAccess.FRIENDS,
            commentAccess = JournalAccess.ALL
        )
        val mockUiState = JournalsUiState.Content(
            journals = listOf(sampleJournal),
            isRefreshing = false,
            isSavingJournalSettings = false
        )

        JournalSettingsDialog(
            journal = sampleJournal,
            onDismiss = {},
            viewModel = mockViewModel,
            uiState = mockUiState
        )
    }
}
```

##### Изменения в `JournalsListScreen.kt`

**Добавить состояние для диалога:**

```kotlin
// Состояние диалога настроек дневника
var journalToEditSettings by remember { mutableStateOf<Journal?>(null) }
```

**Добавить обработчик для JournalAction.SETUP:**

```kotlin
onClickAction = { action ->
    when (action) {
        JournalAction.DELETE -> {
            journalToDelete = journal
            showDeleteDialog = true
        }
        JournalAction.SETUP -> {
            journalToEditSettings = journal
        }
        else -> {}
    }
}
```

**Добавить отображение диалога:**

```kotlin
// Диалог настроек дневника
journalToEditSettings?.let { journal ->
    JournalSettingsDialog(
        journal = journal,
        onDismiss = { journalToEditSettings = null },
        viewModel = viewModel,
        uiState = uiState
    )
}
```

**Добавить обработку события `JournalSettingsSaved` в основной коллектор событий:**

> **Важно:** Это должно быть частью единственного основного коллектора событий экрана (не создавать отдельный `LaunchedEffect`)

```kotlin
// В основном LaunchedEffect экрана, где собираются события
LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
        when (event) {
            is JournalsEvent.JournalSettingsSaved -> {
                // Закрыть диалог только если это был наш journal
                if (journalToEditSettings?.id == event.journal.id) {
                    journalToEditSettings = null
                }
            }
            // ... остальные события
        }
    }
}
```

## Локализация

**Важно:** Перед добавлением проверить, что строки ещё не существуют в `strings.xml`

Добавить строковые ресурсы:

### `app/src/main/res/values/strings.xml`

```xml
<!-- save и close уже есть, переиспользуем -->
<string name="journal_settings_saved">Journal settings saved</string>
<string name="error_save_journal_settings">Error saving journal settings</string>
<string name="error_user_not_found">User not found</string>
```

### `app/src/main/res/values-ru/strings.xml`

```xml
<!-- save и close уже есть, переиспользуем -->
<string name="journal_settings_saved">Настройки дневника сохранены</string>
<string name="error_save_journal_settings">Ошибка сохранения настроек дневника</string>
<string name="error_user_not_found">Пользователь не найден</string>
```

## Порядок реализации

### Этап 1: Domain Layer

- ✅ **ПРОПУЩЕН** - Модели уже существуют (JournalAccess, Journal)

### Этап 2: Data Layer

- ✅ **ПРОПУЩЕН** - API и Repository уже существуют

### Этап 3: Presentation Layer - ViewModel

- [ ] Добавить событие `JournalSettingsSaved` в `JournalsEvent`
- [ ] Добавить флаг `isSavingJournalSettings` в `JournalsUiState.Content`
- [ ] Добавить метод `editJournalSettings()` в интерфейс `IJournalsViewModel.kt`
- [ ] Реализовать метод `editJournalSettings()` в `JournalsViewModel.kt`
- [ ] Добавить обработку ошибок и отправку событий

### Этап 4: Presentation Layer - UI Components

- [ ] Создать файл `JournalSettingsDialog.kt`
- [ ] Реализовать основную Composable функцию `JournalSettingsDialog()`
- [ ] Реализовать вспомогательные типы и функции (`JournalAccessOption`, `SettingsDialogSectionTitle`, `JournalAccessGroup`, `JournalAccessRow`)
- [ ] Добавить состояние диалога с `rememberSaveable`
- [ ] Добавить валидацию названия (не пустое)
- [ ] Реализовать логику активации кнопки сохранения
- [ ] Добавить предпросмотры

### Этап 5: Интеграция в JournalsListScreen

- [ ] Добавить состояние для диалога настроек
- [ ] Добавить обработчик для действия `JournalAction.SETUP`
- [ ] Добавить отображение диалога настроек
- [ ] Добавить обработку события `JournalSettingsSaved` для закрытия диалога в основном коллекторе событий экрана

### Этап 6: Локализация

- [ ] Добавить строковые ресурсы в `values/strings.xml`
- [ ] Добавить строковые ресурсы в `values-ru/strings.xml`

### Этап 7: Тестирование

- [ ] Написать unit-тесты для ViewModel метода `editJournalSettings()`
- [ ] Написать UI тесты для диалога настроек
- [ ] Проверить работу диалога на устройстве/эмуляторе

## Критерии завершения

- [ ] Диалог открывается при нажатии на "Настроить" в меню дневника
- [ ] Текстовое поле показывает текущее название дневника
- [ ] RadioButton'ы показывают текущие значения viewAccess и commentAccess
- [ ] Кнопка "Сохранить" активна только при наличии изменений и непустом названии
- [ ] При нажатии "Сохранить" отправляется запрос через ViewModel
- [ ] Кнопка блокируется и показывается индикатор загрузки во время запроса
- [ ] После успешного сохранения диалог закрывается по событию `JournalSettingsSaved`
- [ ] При ошибке показывается Snackbar с сообщением об ошибке
- [ ] Кнопка с крестиком закрывает диалог без сохранения
- [ ] Написаны тесты для ViewModel и UI компонента
- [ ] Проект собирается без ошибок

## Тестирование

### Unit-тесты для ViewModel (`JournalsViewModelTest`)

**Настройки:**
- Использовать MockK для мокирования `SWRepository`
- Использовать `runTest` для тестирования корутин
- Использовать `Turbine` для проверки событий

**Сценарии:**

1. **Успешное сохранение настроек:**
   - `when`: вызывается `editJournalSettings()` с валидными параметрами
   - `then`: репозиторий вызван с правильными параметрами
   - `then`: `isSavingJournalSettings` устанавливается в `true` перед запросом и в `false` после
   - `then`: в `uiState.journals` обновлён именно нужный journal с данными от сервера (через `map`)
   - `then`: эмитится событие `JournalSettingsSaved` с обновленным журналом от сервера
   - `then`: эмитится событие `ShowSnackbar` с сообщением "Настройки дневника сохранены"

2. **Ошибка сети/сервера:**
   - `when`: репозиторий возвращает `Result.failure()`
   - `then`: `isSavingJournalSettings` сбрасывается в `false`
   - `then`: эмитится событие `ShowSnackbar` с сообщением об ошибке

3. **Неожиданная ошибка (исключение):**
   - `when`: происходит непредвиденное исключение
   - `then`: `isSavingJournalSettings` сбрасывается в `false`
   - `then`: эмитится событие `ShowSnackbar` с сообщением об ошибке

4. **userId отсутствует:**
   - `when`: `_currentUser.value?.id` равен `null`
   - `then`: не выполняется запрос к репозиторию
   - `then`: эмитится событие `ShowSnackbar` с сообщением "Пользователь не найден" (или аналогичным)
   - `then`: `isSavingJournalSettings` остаётся `false`

5. **Редактирование при состоянии не Content:**
   - `when`: `uiState` не является `JournalsUiState.Content` (например `Error` или `InitialLoading`)
   - `then`: обновления `isSavingJournalSettings` игнорируются (state остаётся как есть)
   - `then`: если запрос успешен, событие `JournalSettingsSaved` всё равно эмитится (для закрытия диалога)

### UI тесты для Dialog (`JournalSettingsDialogTest`)

**Настройки:**
- Использовать Compose Testing
- Создать mock ViewModel и Journal
- Проверять состояние UI через assertions

**Сценарии:**

1. **Кнопка "Сохранить" заблокирована при пустом названии:**
   - `given`: диалог открыт с пустым названием
   - `expect`: кнопка "Сохранить" отключена (enabled = false)

2. **Кнопка "Сохранить" заблокирована при отсутствии изменений:**
   - `given`: диалог открыт с текущими значениями без изменений
   - `expect`: кнопка "Сохранить" отключена (enabled = false)

3. **Кнопка "Сохранить" активна при наличии изменений:**
   - `given`: диалог открыт и пользователь изменил название
   - `expect`: кнопка "Сохранить" включена (enabled = true)

4. **RadioButton меняет выбранное значение:**
   - `when`: пользователь кликает на вариант "Друзья" для "Кто видит записи"
   - `then`: соответствующий RadioButton становится выбранным (selected = true)

5. **Семантика RadioButton работает корректно:**
   - `expect`: RadioButton имеют правильную роль `Role.RadioButton`
   - `expect`: Column имеет модификатор `selectableGroup`

6. **Индикатор загрузки при сохранении:**
   - `given`: `isSavingJournalSettings = true` в uiState
   - `expect`: на кнопке "Сохранить" показан `CircularProgressIndicator`
   - `expect`: кнопка заблокирована (enabled = false)

7. **Ошибка валидации названия:**
   - `when`: пользователь пытается сохранить с пустым названием
   - `then`: поле ввода показывает ошибку (isError = true)

8. **Диалог с uiState не Content:**
   - `given`: `uiState` = `Error` или `InitialLoading`
   - `expect`: `isSaving` вычисляется как `false` (безопасное разворачивание)
   - `expect`: кнопка "Сохранить" работает нормально (если есть изменения)

## Заметки

1. **Безопасное разворачивание опционалов**: Использовать `?.let`, `?:` и ранний выход, не использовать `!!`
2. **Обработка ошибок сети**: Использовать `try-catch` и показывать Snackbar с сообщением об ошибке
3. **Индикатор загрузки**: Показывать CircularProgressIndicator на кнопке сохранения во время запроса
4. **Валидация**: Название дневника не должно быть пустым
5. **Активация кнопки сохранения**: Использовать `remember` для кэширования результата сравнения
6. **Размер диалога**: Использовать `DialogProperties(usePlatformDefaultWidth = false)` и ограничить ширину как в nowinandroid
7. **Скролл**: Добавить `verticalScroll` для содержимого диалога
8. **Радио-кнопки**: Использовать `selectableGroup` modifier для правильной работы семантики
9. **Навигация**: Диалог закрывается при успешном сохранении по событию ViewModel
10. **Обновление данных**: После успешного сохранения журнал обновляется в `uiState.journals` данными от сервера, и эмитится событие `JournalSettingsSaved` для закрытия диалога
11. **Переиспользование кода**: Компонент `JournalAccessGroup` переиспользуется для обеих групп доступов
12. **Сохранение состояния**: Использовать `rememberSaveable` для сохранения состояния при повороте экрана
