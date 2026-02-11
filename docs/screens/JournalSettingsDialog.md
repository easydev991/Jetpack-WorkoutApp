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

1. **Текстовое поле для названия дневника** - с валидацией на непустоту
2. **Настройка "Кто видит записи"** - RadioButton с вариантами: Все, Друзья, Только я
3. **Настройка "Кто может оставлять комментарии"** - RadioButton с теми же вариантами
4. **Кнопка сохранения** - активна только при наличии изменений и непустом названии, с индикатором загрузки
5. **Кнопка закрытия** - с иконкой крестика в правом верхнем углу заголовка

## Архитектурные слои

### 1. Domain Layer

- ✅ `JournalAccess` enum (ALL, FRIENDS, NOBODY) - уже существует
- ✅ `Journal` domain model - уже существует
- ✅ `IEditJournalSettingsUseCase` и `EditJournalSettingsUseCase` - созданы

### 2. Data Layer

- ✅ `SWApi.editJournalSettings()` - уже существует
- ✅ `SWRepository.editJournalSettings()` - уже существует
- ✅ `EditJournalSettingsRequest` - уже существует

### 3. Presentation Layer

#### ViewModel

- Добавить событие `JournalSettingsSaved` в `JournalsEvent` sealed class
- Добавить метод `editJournalSettings()` в `IJournalsViewModel.kt` и реализовать в `JournalsViewModel.kt`
- Добавить флаг `isSavingJournalSettings` в `JournalsUiState.Content`

#### UI Components

- Создать `JournalSettingsDialog.kt` в `app/src/main/java/com/swparks/ui/screens/journals/`
- Реализовать основную Composable функцию с локальным состоянием (`TextFieldValue` для title)
- Создать вспомогательные компоненты: `JournalAccessOption`, `SettingsDialogSectionTitle`, `JournalAccessGroup`, `JournalAccessRow`
- Добавить валидацию названия и логику активации кнопки сохранения
- Показывать индикатор загрузки на кнопке во время сохранения

#### JournalsListScreen Integration

- Добавить состояние `journalToEditSettings` с `remember`
- Добавить обработчик для действия `JournalAction.SETUP`
- Добавить отображение диалога
- Добавить обработку события `JournalSettingsSaved` в основном коллекторе событий

## Локализация

Добавить строковые ресурсы в `values/strings.xml` и `values-ru/strings.xml`:
- `journal_settings_saved` / "Настройки дневника сохранены"
- `error_save_journal_settings` / "Ошибка сохранения настроек дневника"
- `error_user_not_found` / "Пользователь не найден"

## Порядок реализации

### Этап 1: Domain Layer

- ✅ **ПРОПУЩЕН** - Модели и Use Case уже существуют

### Этап 2: Data Layer

- ✅ **ПРОПУЩЕН** - API и Repository уже существуют

### Этап 3: Presentation Layer - ViewModel

- ✅ Добавить событие `JournalSettingsSaved` в `JournalsEvent`
- ✅ Создать Use Case `IEditJournalSettingsUseCase` и `EditJournalSettingsUseCase`
- ✅ Добавить флаг `isSavingJournalSettings` в `JournalsUiState.Content`
- ✅ Реализовать метод `editJournalSettings()` в `IJournalsViewModel.kt` и `JournalsViewModel.kt`
- ✅ Добавить обработку ошибок и отправку событий

### Этап 4: Presentation Layer - UI Components

- ✅ Создать файл `JournalSettingsDialog.kt`
- ✅ Реализовать основную Composable функцию `JournalSettingsDialog()`
- ✅ Реализовать вспомогательные типы и функции
- ✅ Добавить состояние диалога с `remember` (использовано TextFieldValue для title)
- ✅ Добавить валидацию названия (не пустое)
- ✅ Реализовать логику активации кнопки сохранения

### Этап 5: Интеграция в JournalsListScreen

- ✅ Добавить состояние для диалога настроек (`journalToEditSettings`)
- ✅ Добавить обработчик для действия `JournalAction.SETUP`
- ✅ Добавить отображение диалога настроек
- ✅ Добавить обработку события `JournalSettingsSaved` для закрытия диалога

### Этап 6: Локализация

- ✅ Добавить строковые ресурсы в `values/strings.xml`
- ✅ Добавить строковые ресурсы в `values-ru/strings.xml`

### Этап 7: Тестирование

- ✅ Проверить работу диалога на устройстве/эмуляторе
- [ ] Написать специализированные unit-тесты для `JournalsViewModel.editJournalSettings()`
- [ ] Написать UI тесты для диалога настроек (`JournalSettingsDialogTest`)

## Известные проблемы и замечания

1. **Отсутствие специализированных тестов для новых функций** ⚠️
   - В `JournalsViewModelTest.kt` `editJournalSettingsUseCase` создается как mock, но нет тестов, которые проверяют успешное сохранение, обработку ошибок, корректность изменения флага `isSavingJournalSettings`
   - Отсутствует файл `EditJournalSettingsUseCaseTest.kt`
   - **Статус:** Планируется добавить в разделе тестирования

2. **Использование `!!` в существующем коде** (не часть нового функционала) ⚠️
   - В `JournalsListScreen.kt` строка 365: `journal.ownerId!!` - потенциально небезопасно
   - Строка 221: `textEntryMode!!` - безопасно, так как выше есть проверка
   - **Статус:** Не является критичным для текущей задачи, не требует исправления в рамках этого плана

## Критерии завершения

- [ ] Написаны тесты для ViewModel и UI компонента

## Тестирование

### Unit-тесты для ViewModel (`JournalsViewModelTest`)

**Настройки:**
- Использовать MockK для мокирования `IEditJournalSettingsUseCase`, `ISyncJournalsUseCase`
- Использовать `runTest` для тестирования корутин
- Использовать `Turbine` для проверки событий

**Сценарии (ДОБАВИТЬ):**

1. **Успешное сохранение настроек:**
   - use case вызван с правильными параметрами
   - `isSavingJournalSettings` устанавливается в `true` перед запросом и в `false` после
   - вызывается `syncJournalsUseCase` для перезагрузки данных
   - эмитится событие `JournalSettingsSaved` с журналом из текущего состояния
   - эмитится событие `ShowSnackbar` с сообщением "Настройки дневника сохранены"

2. **Ошибка сети/сервера при сохранении:**
   - `isSavingJournalSettings` сбрасывается в `false`
   - эмитится событие `ShowSnackbar` с сообщением об ошибке

3. **Неожиданная ошибка (исключение) при сохранении:**
   - `isSavingJournalSettings` сбрасывается в `false`
   - эмитится событие `ShowSnackbar` с сообщением об ошибке по умолчанию

4. **Редактирование при состоянии не Content:**
   - обновления `isSavingJournalSettings` игнорируются (state остаётся как есть)
   - если запрос успешен, событие `JournalSettingsSaved` всё равно эмитится (для закрытия диалога)

### UI тесты для Dialog (`JournalSettingsDialogTest`)

**Настройки:**
- Использовать Compose Testing
- Создать mock ViewModel и Journal
- Проверять состояние UI через assertions

**Сценарии:**

1. **Кнопка "Сохранить" заблокирована при пустом названии**
2. **Кнопка "Сохранить" заблокирована при отсутствии изменений**
3. **Кнопка "Сохранить" активна при наличии изменений**
4. **RadioButton меняет выбранное значение**
5. **Семантика RadioButton работает корректно** (правильная роль, selectableGroup modifier)
6. **Индикатор загрузки при сохранении** (CircularProgressIndicator на кнопке, кнопка заблокирована)
7. **Ошибка валидации названия** (isError = true при попытке сохранить с пустым названием)
8. **Диалог с uiState не Content** (isSaving вычисляется как false, кнопка работает нормально)

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
10. **Обновление данных**: После успешного сохранения вызывается `syncJournalsUseCase` для перезагрузки данных с сервера, а затем эмитится событие `JournalSettingsSaved` с журналом из текущего состояния UI для закрытия диалога
11. **Переиспользование кода**: Компонент `JournalAccessGroup` переиспользуется для обеих групп доступов
12. **Сохранение состояния**: Использовать `remember` для локального состояния (с `TextFieldValue` для title)
