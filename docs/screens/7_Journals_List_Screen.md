# План реализации JournalsListScreen

## Прогресс выполнения

**Первая итерация: 100%** ✅
**Вторая итерация: 100%** ✅
**Третья итерация: 100%** ✅

---

## Описание экрана

Экран списка дневников пользователя, который:
- Принимает `userId` через навигацию
- Загружает и кеширует список дневников через API
- Отображает дневники через `JournalRowView`
- Поддерживает Pull-to-Refresh, удаление с диалогом подтверждения, создание через FAB
- Обрабатывает состояния загрузки, ошибки, пустого списка
- Корректно обрабатывает безопасные зоны экрана

## Референсы

- iOS-версия: `SwiftUI-WorkoutApp` - экран `JournalsListScreen`
- Безопасные зоны: `MyBlacklistScreen.kt`
- UI компонент: `JournalRowView.kt`
- Удаление записей: `JournalEntriesScreen.kt`

---

## Первая итерация: базовый экран ✅

**Реализован функционал:**
- Domain Layer: модель `Journal`, enum `JournalAccess`, маппер
- Data Layer: `JournalEntity`, `JournalDao`, `JournalsRepository`
- Use Cases: `GetJournalsUseCase`, `SyncJournalsUseCase`
- Presentation: `JournalsUiState`, `JournalsViewModel`
- UI: экран с AppBar, Pull-to-Refresh, состояниями UI
- Локализация: `journals_list_title`, `journals_empty`

**Тестирование:** 11 UI тестов, 12 unit тестов для ViewModel

**Измененные файлы:**
- `JournalsListScreen.kt`, `IJournalsViewModel.kt`, `FakeJournalsViewModel.kt`
- `JournalsListScreenTest.kt`, `JournalsViewModelTest.kt`
- `AppContainer.kt`, `RootScreen.kt`
- `strings.xml`, `strings-ru/strings.xml`

---

## Вторая итерация: удаление дневника ✅

**Реализован функционал:**
- Domain Layer: `DeleteJournalUseCase`
- Presentation: метод `deleteJournal()`, флаг `isDeleting`, события для Snackbar
- UI: диалог подтверждения `DeleteConfirmationDialog`, обработка `DELETE` действия
- Data Layer: `SWRepository.deleteJournal()` синхронизирует локальную БД
- Локализация: `delete_journal_title`, `delete_journal_message`, `journal_deleted`, `error_delete_journal`

**Тестирование:** 5 unit тестов для удаления

**Измененные файлы:**
- **Созданные:** `IDeleteJournalUseCase.kt`, `DeleteJournalUseCase.kt`, `DeleteJournalUseCaseTest.kt`
- **Измененные:** `JournalsListScreen.kt`, `IJournalsViewModel.kt`, `JournalsViewModel.kt`, `FakeJournalsViewModel.kt`, `SWRepository.kt`, `AppContainer.kt`, `JournalsViewModelTest.kt`, `strings.xml`, `strings-ru/strings.xml`

---

## Третья итерация: создание дневника ✅

**Реализован функционал:**
- Domain Layer: `CreateJournalUseCase`, модель запроса `CreateJournalRequest`
- Presentation: новый режим `NewJournal` в `TextEntryMode`, обновление `TextEntryViewModel`
- UI: FAB для создания дневника, интеграция с `TextEntrySheetHost`, callback `onSendSuccess`
- API: исправлен метод `createJournal` - использует `@Body CreateJournalRequest` вместо `@Body title: String`
- FAB отображается только если `appState.currentUser != null && userId == appState.currentUser.id`
- Локализация: `new_journal_placeholder`, `journal_created`, `error_create_journal`, `error_empty_title`, `fab_create_journal_description`

**Тестирование:**
- 6 unit тестов для `CreateJournalUseCase`
- 4 новых UI теста для FAB (авторизованный/неавторизованный пользователь, свой/чужой профиль, удаление)

**Измененные файлы:**
- **Созданные:** `CreateJournalRequest.kt`, `ICreateJournalUseCase.kt`, `CreateJournalUseCase.kt`, `CreateJournalUseCaseTest.kt`, `TextEntryUseCaseTest.kt`
- **Измененные:** `SWApi.kt`, `MockSWApi.kt`, `SWRepository.kt`, `TextEntryMode.kt`, `ITextEntryUseCase.kt`, `TextEntryUseCase.kt`, `TextEntryViewModel.kt`, `JournalsListScreen.kt`, `AppContainer.kt`, `RootScreen.kt`, `strings.xml`, `strings-ru/strings.xml`

---

## Связанные документы

- [План JournalEntriesScreen](./7.1_JournalEntriesScreen.md)
- [План TextEntryScreen](./7.2_TextEntryScreen.md) - экран ввода текста с Mode enum и Bottom Sheet

---

## Четвертая итерация: унификация уведомлений ✅

**Проблема:**
- В JournalsListScreen и JournalEntriesScreen есть локальные SnackbarHost и события ShowSnackbar
- Это дублирует глобальный механизм через userNotifier.errorFlow в RootScreen
- FIXME в коде: `// Показать Snackbar (FIXME: добавить SnackbarHost)`

**Решение:**
- Расширить UserNotifier для поддержки информационных сообщений (не только ошибки)
- Убрать локальные SnackbarHost из экранов дневников
- Убрать события ShowSnackbar из ViewModel

**Реализованный функционал:**
1. Создан `AppNotification` sealed class для информационных сообщений
2. Расширен `UserNotifier` - добавлен `notificationFlow` и метод `showInfo()`
3. Обновлен `UserNotifierImpl` - реализация `showInfo()` с логированием
4. Обновлен `RootScreen` - подписка на `notificationFlow` для показа Snackbar
5. Убран `ShowSnackbar` из `JournalsEvent` и `JournalEntriesEvent`
6. ViewModels используют `userNotifier.showInfo()` вместо emit событий
7. Убраны локальные SnackbarHost и обработка ShowSnackbar в экранах

**Измененные файлы:**
- **Созданные:** `AppNotification.kt`
- **Измененные:** `UserNotifier.kt`, `UserNotifierImpl.kt`, `AppError.kt`, `RootScreen.kt`, `IJournalsViewModel.kt`, `JournalsViewModel.kt`, `IJournalEntriesViewModel.kt`, `JournalEntriesViewModel.kt`, `JournalsListScreen.kt`, `JournalEntriesScreen.kt`, `strings.xml` (добавлена строка `info`), `strings-ru/strings.xml`
- **Тесты:** `UserNotifierImplTest.kt`, `JournalsViewModelTest.kt`, `JournalEntriesViewModelTest.kt`

---

## Пятая итерация: TODO

- Добавить фильтрацию и сортировку дневников
- Добавить индикатор количества записей в дневнике
