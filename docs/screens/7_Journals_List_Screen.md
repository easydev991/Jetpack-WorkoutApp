# План реализации JournalsListScreen (первая итерация)

## Прогресс выполнения

**Выполнено: 100%** ✅

- ✅ Этап 1: Модели данных (Domain Layer) - полностью выполнен
- ✅ Этап 2: API клиент и репозиторий (Data Layer) - полностью выполнен
- ✅ Этап 3: Use Cases (Domain Layer) - полностью выполнен
- ✅ Этап 4: ViewModel (Presentation Layer) - полностью выполнен
- ✅ Этап 5: UI компоненты (UI Layer) - полностью выполнен
- ✅ Этап 6: Тестирование - полностью выполнен

## Описание экрана

Экран списка дневников пользователя, который:
- Принимает идентификатор пользователя (`userId`) через аргументы навигации
- Загружает список дневников с сервера через API endpoint
- Сохраняет дневники в локальную базу данных (кеширование для всех пользователей)
- Отображает список дневников через компонент `JournalRowView`
- Показывает заглушку с кнопкой "Создать дневник" (`create_journal`) при пустом списке
- Поддерживает Pull-to-Refresh для обновления данных (без блокировки контента)
- При первой загрузке показывает полный экран загрузки
- Корректно обрабатывает безопасные зоны экрана (как в MyBlacklistScreen)
- На первой итерации все действия (кроме "Назад") логируются в консоль

## Референсы

- iOS-версия: `SwiftUI-WorkoutApp` - экран `JournalsListScreen`
- Безопасные зоны: `MyBlacklistScreen.kt`
- UI компонент: `JournalRowView.kt`
- Навигация: маршрут `JournalsList` уже определен в `Destinations.kt`
- API endpoint: `SWApi.getJournals(userId: Long)` возвращает `List<JournalResponse>`
- Модель данных: `JournalResponse.kt` уже существует

---

## Этап 1: Модели данных (Domain Layer) ✅ ВЫПОЛНЕНО

- ✅ Доменная модель `Journal`, enum `JournalAccess`, маппер `JournalResponse.toDomain()`

---

## Этап 2: API клиент и репозиторий (Data Layer) ✅ ВЫПОЛНЕНО

- ✅ `JournalEntity`, `JournalDao` (getJournalsByUserId, insertAll, deleteByUserId), мапперы (String ↔ Long timestamp)
- ✅ `JournalsRepository` (interface + implementation) с `observeJournals(userId): Flow<List<Journal>>` и `refreshJournals(userId): Result<Unit>`
- ✅ `AppContainer.kt` обновлен с factory методом `journalsRepository()`

---

## Этап 3: Use Cases (Domain Layer) ✅ ВЫПОЛНЕНО

- ✅ `GetJournalsUseCase` и `SyncJournalsUseCase` с делегированием в репозиторий
- ✅ `AppContainer.kt` обновлен с factory методами для Use Cases

---

## Этап 4: ViewModel (Presentation Layer) ✅ ВЫПОЛНЕНО

- ✅ `JournalsUiState` (sealed class: InitialLoading, Content, Error)
- ✅ `JournalsViewModel` с подпиской на Flow (SSOT), методами `loadJournals()` и `retry()`
- ✅ `AppContainer.kt` обновлен с factory методом `journalsViewModelFactory(userId)`

---

## Этап 5: UI компоненты (UI Layer) ✅ ВЫПОЛНЕНО

### 5.1 Локализация - ВЫПОЛНЕНО ✅

- ✅ `journals_list_title` (RU: "Дневники", EN: "Journals")
- ✅ `journals_empty` (RU: "Дневников пока нет", EN: "No journals here yet")

### 5.2 JournalsListScreen - ВЫПОЛНЕНО ✅

- ✅ Экран с AppBar, Pull-to-Refresh, безопасными зонами (`parentPaddingValues`, `innerPadding`)
- ✅ Состояния UI: `InitialLoading` → `LoadingOverlayView`, `Error` → `ErrorContentView`, `Content` → список/заглушка
- ✅ Блокировка UI при `isRefreshing` (кнопки и элементы списка недоступны, индикатор поверх контента)
- ✅ Логирование действий в консоль
- ✅ Приватные функции: `ContentScreen`, `JournalsList`, `EmptyStateView`

### 5.3 Навигация и интерфейс - ВЫПОЛНЕНО ✅

- ✅ Комposable `JournalsList` в `RootScreen.kt` с извлечением `userId` из навигации
- ✅ `IJournalsViewModel` интерфейс для тестирования

---

## Этап 6: Тестирование - ВЫПОЛНЕНО ✅

### 6.1 UI тесты - ВЫПОЛНЕНО ✅

- ✅ 11 тестов: проверка AppBar, всех состояний UI, отображения дневников, кнопок, индикатора обновления, блокировки при `isRefreshing`
- ✅ `FakeJournalsViewModel` для изоляции бизнес-логики, `IJournalsViewModel` для типизации
- ✅ Проверка интерактивности кнопок (`assertIsEnabled`, `assertIsNotEnabled`)

### 6.2 Unit тесты ViewModel - ВЫПОЛНЕНО ✅

- ✅ 12 тестов: проверка состояний, переходов, обработки ошибок, флага `isRefreshing`, Flow
- ✅ `MockK` для Use Cases, мокирование статического `Log`
- ✅ Все тесты проходят успешно

---

## Критерии завершения первой итерации

### Функциональные требования

- ✅ Загрузка списка дневников для указанного `userId`, кеширование в БД (delete → insert)
- ✅ Pull-to-Refresh обновляет список без блокировки контента, индикатор поверх при `isRefreshing`
- ✅ Первичная загрузка: полный экран `LoadingOverlayView`, ошибка: `ErrorContentView`, пустой список: заглушка
- ✅ Безопасные зоны, кнопка "Назад", логирование всех действий в консоль

### Технические требования

- ✅ Код соответствует правилам (безопасные опционалы, локализация RU/EN, `make format`)
- ✅ Repository в `domain/repository/`, реализация в `data/repository/`, ViewModel → Use Cases → Repository
- ✅ ViewModel подписывается на Flow (SSOT), `LaunchedEffect(userId)` для перезапуска, `IJournalsViewModel` для тестирования

### Тестирование

- ✅ UI тесты (11) с `FakeJournalsViewModel`: все состояния UI, интерактивность кнопок, блокировка при `isRefreshing`
- ✅ Unit тесты (12) с `MockK`: состояния, переходы, ошибки, `isRefreshing`, Flow

### Примечания

- На первой итерации кнопка "Создать дневник" и клики по элементам списка только логируются в консоль
- Действия меню в `JournalRowView` логируются в консоль

---

## Резюме первой итерации

Первая итерация экрана JournalsListScreen **полностью завершена**.

### Реализованный функционал

- **Архитектура**: Domain (Journal, Use Cases) → Data (Entity, DAO, Repository, мапперы) → Presentation (ViewModel, UI)
- **Функциональность**: загрузка, кеширование в БД, Pull-to-Refresh с блокировкой UI, обработка ошибок, пустой список, логирование действий
- **Тестирование**: 11 UI тестов с `FakeJournalsViewModel`, 12 unit тестов для ViewModel с `MockK`

### Созданные и измененные файлы

**Созданные:**
- `app/src/main/java/com/swparks/ui/screens/journals/JournalsListScreen.kt`
- `app/src/main/java/com/swparks/ui/viewmodel/IJournalsViewModel.kt`
- `app/src/main/java/com/swparks/ui/viewmodel/FakeJournalsViewModel.kt`
- `app/src/androidTest/java/com/swparks/ui/screens/journals/JournalsListScreenTest.kt`
- `app/src/test/java/com/swparks/ui/viewmodel/JournalsViewModelTest.kt`

**Измененные:**
- `app/src/main/java/com/swparks/data/AppContainer.kt` - factory методы
- `app/src/main/java/com/swparks/ui/screens/RootScreen.kt` - composable для JournalsList
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-ru/strings.xml` - локализация

---

## Следующие итерации (для справки)

**Вторая итерация:**
- ✅ Детализировать план для экрана записей дневника (`JournalEntriesScreen`) - создан отдельный документ `7.1_JournalEntriesScreen.md`
- Реализовать переход на экран создания дневника (`CreateJournalScreen`)
- ~~Реализовать переход на детальный экран дневника (`JournalDetailScreen`)~~ → заменено на `JournalEntriesScreen`
- Реализовать реальные действия для кнопки "Создать дневник"
- Реализовать переходы при клике на элементы списка

**Третья итерация:**
- Добавить обработку действий меню (редактирование, удаление, настройки доступа)
- Добавить фильтрацию и сортировку дневников
- Добавить индикатор количества записей в дневнике
