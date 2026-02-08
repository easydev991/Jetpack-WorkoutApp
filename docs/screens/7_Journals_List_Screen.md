# План реализации JournalsListScreen

## Прогресс выполнения

**Первая итерация: 100%** ✅
**Вторая итерация: 100%** ✅

---

## Описание экрана

Экран списка дневников пользователя, который:
- Принимает `userId` через навигацию
- Загружает и кеширует список дневников через API
- Отображает дневники через `JournalRowView`
- Поддерживает Pull-to-Refresh, удаление с диалогом подтверждения
- Обрабатывает состояния загрузки, ошибки, пустого списка
- Корректно обрабатывает безопасные зоны экрана

## Референсы

- iOS-версия: `SwiftUI-WorkoutApp` - экран `JournalsListScreen`
- Безопасные зоны: `MyBlacklistScreen.kt`
- UI компонент: `JournalRowView.kt`
- Удаление записей: `JournalEntriesScreen.kt`

---

## Первая итерация: базовый экран ✅

### Реализованный функционал

**Domain Layer:**
- Доменная модель `Journal`, enum `JournalAccess`, маппер `toDomain()`

**Data Layer:**
- `JournalEntity`, `JournalDao` (getJournalsByUserId, insertAll, deleteByUserId)
- `JournalsRepository` с `observeJournals()`, `refreshJournals()`

**Domain Layer (Use Cases):**
- `GetJournalsUseCase`, `SyncJournalsUseCase`

**Presentation Layer:**
- `JournalsUiState` (InitialLoading, Content, Error)
- `JournalsViewModel` с Flow подпиской, методами `loadJournals()`, `retry()`

**UI Layer:**
- Экран с AppBar, Pull-to-Refresh, безопасными зонами
- Состояния UI с `LoadingOverlayView`, `ErrorContentView`, заглушкой при пустом списке
- Локализация (RU/EN): `journals_list_title`, `journals_empty`

**Тестирование:**
- 11 UI тестов с `FakeJournalsViewModel`
- 12 unit тестов для ViewModel с `MockK`

### Измененные файлы

- `app/src/main/java/com/swparks/ui/screens/journals/JournalsListScreen.kt`
- `app/src/main/java/com/swparks/ui/viewmodel/IJournalsViewModel.kt`
- `app/src/main/java/com/swparks/ui/viewmodel/FakeJournalsViewModel.kt`
- `app/src/androidTest/java/com/swparks/ui/screens/journals/JournalsListScreenTest.kt`
- `app/src/test/java/com/swparks/ui/viewmodel/JournalsViewModelTest.kt`
- `app/src/main/java/com/swparks/data/AppContainer.kt`
- `app/src/main/java/com/swparks/ui/screens/RootScreen.kt`
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-ru/strings.xml`

---

## Вторая итерация: удаление дневника ✅

### Реализованный функционал

**Domain Layer:**
- `DeleteJournalUseCase` с интерфейсом и реализацией

**Presentation Layer:**
- `JournalsViewModel`: метод `deleteJournal()`, флаг `isDeleting`, события для Snackbar

**UI Layer:**
- Диалог подтверждения удаления `DeleteConfirmationDialog`
- Обработка действия `JournalAction.DELETE`
- Индикатор загрузки при удалении, блокировка UI

**Локализация:**
- `delete_journal_title`, `delete_journal_message`, `journal_deleted`, `error_delete_journal` (RU/EN)

**Data Layer:**
- `SWRepository.deleteJournal()` синхронизирует локальную БД

**Тестирование:**
- 5 unit тестов для удаления в `JournalsViewModelTest.kt`
- Базовые UI тесты (уже покрыты в первой итерации)

### Качество кода

- ✅ Все 698 тестов проходят успешно
- ✅ Билд успешный

### Измененные файлы

**Созданные:**
- `app/src/main/java/com/swparks/domain/usecase/IDeleteJournalUseCase.kt`
- `app/src/main/java/com/swparks/domain/usecase/DeleteJournalUseCase.kt`
- `app/src/test/java/com/swparks/domain/usecase/DeleteJournalUseCaseTest.kt`

**Измененные:**
- `app/src/main/java/com/swparks/ui/screens/journals/JournalsListScreen.kt`
- `app/src/main/java/com/swparks/ui/viewmodel/IJournalsViewModel.kt`
- `app/src/main/java/com/swparks/ui/viewmodel/JournalsViewModel.kt`
- `app/src/main/java/com/swparks/ui/viewmodel/FakeJournalsViewModel.kt`
- `app/src/main/java/com/swparks/data/repository/SWRepository.kt`
- `app/src/main/java/com/swparks/data/AppContainer.kt`
- `app/src/test/java/com/swparks/ui/viewmodel/JournalsViewModelTest.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-ru/strings.xml`

---

## Следующие итерации (для справки)

**Третья итерация:**
- Реализовать переход на экран создания дневника (`CreateJournalScreen`)
- Реализовать реальные действия для кнопки "Создать дневник"
- Реализовать переходы при клике на элементы списка
- Добавить обработку других действий меню (редактирование, настройки доступа)

**Четвертая итерация:**
- Добавить фильтрацию и сортировку дневников
- Добавить индикатор количества записей в дневнике
