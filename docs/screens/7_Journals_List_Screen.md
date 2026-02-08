# План реализации JournalsListScreen (первая итерация)

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

## Этап 1: Модели данных (Domain Layer)

### 1.1 Создать доменную модель Journal

- Создать `app/src/main/java/com/swparks/domain/model/Journal.kt` с полями:
  - `id: Long`
  - `title: String?`
  - `lastMessageImage: String?`
  - `createDate: String?`
  - `modifyDate: String?`
  - `lastMessageDate: String?`
  - `lastMessageText: String?`
  - `entriesCount: Int?` (количество записей в дневнике)
  - `ownerId: Long?`
  - `viewAccess: JournalAccess?`
  - `commentAccess: JournalAccess?`

### 1.2 Создать маппер JournalResponse → Journal

- Добавить extension функцию `JournalResponse.toDomain(): Journal` в `JournalResponse.kt`
- Маппер должен преобразовывать все поля из `JournalResponse` в `Journal`

### 1.3 Обновить enum JournalAccess (если необходимо)

- Проверить наличие `JournalAccess` в модели (уже должен существовать)
- Убедиться, что метод `from(value: Int)` работает корректно

---

## Этап 2: API клиент и репозиторий (Data Layer)

### 2.1 Создать DAO для таблицы Journal

- Создать `app/src/main/java/com/swparks/data/database/dao/JournalDao.kt`:
  - `@Query("SELECT * FROM journals WHERE owner_id = :userId ORDER BY modify_date DESC") fun getJournalsByUserId(userId: Long): Flow<List<JournalEntity>>`
  - `@Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(journals: List<JournalEntity>)`
  - `@Query("DELETE FROM journals WHERE owner_id = :userId") suspend fun deleteByUserId(userId: Long)`

### 2.2 Создать Entity для таблицы Journal

- Создать `app/src/main/java/com/swparks/data/database/entity/JournalEntity.kt`:
  - Поля должны соответствовать структуре базы данных
  - `@PrimaryKey(autoGenerate = true) val id: Long = 0`
  - Другие поля из `JournalResponse`

### 2.3 Создать маппер Journal → JournalEntity

- Добавить extension функцию `Journal.toEntity(): JournalEntity` в `JournalEntity.kt`:
  - Конвертировать `modifyDate` из String в Long (timestamp) для корректной сортировки в БД
  - Использовать утилиту для парсинга даты (проверить существующие дата-конвертеры в проекте)

### 2.4 Обновить базу данных SWDatabase

- Добавить таблицу `journals` в `SWDatabase.kt`:
  - Дата модификации хранится как `Long` (timestamp) для корректной сортировки
- Создать `abstract fun journalDao(): JournalDao`

### 2.5 Создать JournalsRepository

- Создать интерфейс `app/src/main/java/com/swparks/domain/repository/JournalsRepository.kt`:
  - `fun observeJournals(userId: Long): Flow<List<Journal>>` - наблюдение за дневниками (SSOT)
  - `suspend fun refreshJournals(userId: Long): Result<Unit>` - обновление данных с сервера

- Создать реализацию `app/src/main/java/com/swparks/data/repository/JournalsRepositoryImpl.kt`:
  - Инжектить `SWApi`, `JournalDao`
  - В методе `observeJournals` возвращать `journalDao.getJournalsByUserId(userId).map { entities -> entities.map { it.toDomain() } }`
  - В методе `refreshJournals`:
    - Вызывать `SWApi.getJournals(userId)`
    - Мапить `JournalResponse` → `Journal` → `JournalEntity`
    - **Важно**: Использовать транзакцию для очистки старых данных перед вставкой:

      ```kotlin
      journalDao.deleteByUserId(userId)
      journalDao.insertAll(entities)
      ```

    - Кэшировать дневники **всех** пользователей (не только текущего) для оффлайн-доступа
    - Возвращать `Result.success(Unit)` при успехе или `Result.failure(...)` при ошибке

### 2.6 Обновить AppContainer

- Добавить factory метод `journalsRepository(): JournalsRepository` в `AppContainer.kt`

---

## Этап 2.7: Use Cases (Domain Layer)

### 2.7.1 Создать GetJournalsUseCase

- Создать `app/src/main/java/com/swparks/domain/usecase/GetJournalsUseCase.kt`:
  - Параметр конструктора: `journalsRepository: JournalsRepository`
  - Оператор `invoke` принимает `userId: Long` и возвращает `Flow<List<Journal>>`
  - Реализация: делегирует вызов `journalsRepository.observeJournals(userId)`

### 2.7.2 Создать SyncJournalsUseCase

- Создать `app/src/main/java/com/swparks/domain/usecase/SyncJournalsUseCase.kt`:
  - Параметр конструктора: `journalsRepository: JournalsRepository`
  - Оператор `invoke` принимает `userId: Long` и возвращает `Result<Unit>`
  - Реализация: делегирует вызов `journalsRepository.refreshJournals(userId)`

### 2.7.3 Обновить AppContainer для Use Cases

- Добавить factory методы в `AppContainer.kt`:
  - `getJournalsUseCase(): GetJournalsUseCase`
  - `syncJournalsUseCase(): SyncJournalsUseCase`

---

## Этап 3: ViewModel (Presentation Layer)

### 3.1 Создать UI State

- Создать `app/src/main/java/com/swparks/ui/state/JournalsUiState.kt`:
  - Sealed class:
    - `InitialLoading` - первая загрузка (показывается полный экран загрузки)
    - `Content(journals: List<Journal>, isRefreshing: Boolean)` - контент + статус обновления
    - `Error(message: String)` - ошибка с возможностью повтора

### 3.2 Создать ViewModel

- Создать `app/src/main/java/com/swparks/ui/viewmodel/JournalsViewModel.kt`:
  - Параметры конструктора: `userId: Long`, `getJournalsUseCase: GetJournalsUseCase`, `syncJournalsUseCase: SyncJournalsUseCase`
  - Состояние:
    - `private val _uiState = MutableStateFlow<JournalsUiState>(JournalsUiState.InitialLoading)`
    - `val uiState: StateFlow<JournalsUiState> = _uiState.asStateFlow()`
    - `private val _isRefreshing = MutableStateFlow(false)`
    - `val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()`
  - **Single Source of Truth (SSOT)**:
    - Подписываться на `getJournalsUseCase(userId)` в `init`
    - При получении данных из Flow обновлять `_uiState` в `Content(journals, isRefreshing.value)`
    - При ошибке (пустой список после первой загрузки) обновлять `_uiState` в `Error(message)`
  - Методы:
    - `init { observeJournals(); loadJournals() }` - подписка + первая загрузка
    - `private fun observeJournals()`: подписаться на Flow из `getJournalsUseCase`
    - `fun loadJournals()`: триггерить `syncJournalsUseCase`, установить `_isRefreshing.value = true`, затем `false` при завершении
    - `fun retry()`: повторить загрузку при ошибке (аналогично `loadJournals`)
    - Логировать все действия в консоль (`Log.i("JournalsViewModel", "...")`)

### 3.3 Добавить factory метод в AppContainer

- Добавить метод `journalsViewModel(userId: Long): JournalsViewModel` в `AppContainer.kt`
  - Создавать ViewModel с инжектированными Use Cases

---

## Этап 4: UI компоненты (UI Layer)

### 4.1 Добавить строки локализации

- В `app/src/main/res/values/strings.xml` добавить:
  - `journals_empty` (если отсутствует)
  - `journals_list_title` (если отсутствует)
  - Проверить наличие `create_journal` (уже существует)

- В `app/src/main/res/values-ru/strings.xml` добавить русские переводы

### 4.2 Создать JournalsListScreen

- Создать `app/src/main/java/com/swparks/ui/screens/journals/JournalsListScreen.kt`:

**Структура экрана:**
- `@Composable fun JournalsListScreen(...)`:
  - Параметры: `userId: Long`, `viewModel: JournalsViewModel`, `onBackClick: () -> Unit`, `parentPaddingValues: PaddingValues`
  - Использовать `CenterAlignedTopAppBar` с заголовком и кнопкой "Назад"
  - Использовать `Scaffold` с `contentWindowInsets = WindowInsets(0, 0, 0, 0)` для безопасных зон
  - Поддерживать Pull-to-Refresh через `PullToRefreshBox` (экспериментальный API)
  - **Важно**: Использовать `LaunchedEffect(userId)` для перезапуска загрузки при смене пользователя
  - Логировать нажатия элементов в консоль (`Log.i("JournalsListScreen", "...")`)

**Состояния UI:**
- `InitialLoading`: показать `CircularProgressIndicator` по центру экрана (полный экран загрузки)
- `Error`: показать текст ошибки по центру с кнопкой "Повторить" (вызывает `viewModel.retry()`)
- `Content(journals, isRefreshing)`:
  - Если список не пустой: отобразить `LazyColumn` с элементами через `JournalRowView`
  - Если список пустой: показать заглушку с текстом `journals_empty` и кнопкой `create_journal`

**Pull-to-Refresh:**
- Использовать `PullToRefreshBox` с `isRefreshing` из `viewModel.isRefreshing` (отдельный StateFlow)
- Метод обновления: `viewModel.loadJournals()`
- Индикатор по центру сверху с отступом `spacing_regular`
- **Не блокировать контент** при pull-to-refresh (пользователь видит старые данные под индикатором)

**Безопасные зоны:**
- Учесть `parentPaddingValues` для нижней панели навигации
- Использовать `innerPadding` от `Scaffold`

### 4.3 Обновить навигацию

- В `Navigation.kt` добавить composable для `JournalsList`:
  - Извлекать `userId` из аргументов навигации
  - Получать `viewModel` через `appContainer.journalsViewModel(userId)`
  - Передавать `onBackClick: { navController.popBackStack() }`

### 4.4 Добавить UI тесты (опционально, если время позволяет)

- Создать `app/src/androidTest/java/com/swparks/ui/screens/journals/JournalsListScreenTest.kt`:
  - Тест отображения экрана
  - Тест загрузки данных
  - Тест пустого списка с кнопкой создания
  - Тест pull-to-refresh

---

## Критерии завершения первой итерации

### Функциональные требования

- ✅ Экран загружает список дневников для указанного `userId`
- ✅ Список сохраняется в локальной базе данных **для всех пользователей** (кеширование)
- ✅ При обновлении старые записи удаляются перед вставкой (delete → insert)
- ✅ При пустом списке показывается заглушка с кнопкой "Создать дневник"
- ✅ Pull-to-Refresh обновляет список дневников без блокировки контента
- ✅ Первичная загрузка показывает полный экран загрузки (`InitialLoading`)
- ✅ При ошибке показывается сообщение с кнопкой "Повторить"
- ✅ Безопасные зоны обрабатываются корректно
- ✅ Кнопка "Назад" работает и возвращает на предыдущий экран
- ✅ Все действия (кроме "Назад") логируются в консоль

### Технические требования

- ✅ Код соответствует правилам из `.cursor/rules/`
- ✅ Используется безопасное разворачивание опционалов (без `!!`)
- ✅ Локализация добавлена для всех строк (RU и EN)
- ✅ После изменений выполнена команда `make format`
- ✅ Проект собирается без ошибок (`./gradlew build`)
- ✅ Дата модификации конвертируется из String в Long timestamp в маппере `toEntity` для корректной сортировки
- ✅ Repository интерфейс находится в `domain/repository/`, реализация в `data/repository/`
- ✅ Use Cases созданы в `domain/usecase/` для бизнес-логики
- ✅ ViewModel использует Use Cases вместо прямого вызова Repository
- ✅ ViewModel подписывается на Flow из Use Case (SSOT) для получения актуальных данных из БД
- ✅ Используется `LaunchedEffect(userId)` для перезапуска загрузки при смене пользователя

### Примечания

- На первой итерации кнопка "Создать дневник" только логирует нажатие в консоль (не создает дневник)
- На первой итерации клики по элементам списка дневников логируются в консоль (не открывают детальный экран)
- На первой итерации действия меню в `JournalRowView` логируются в консоль

---

## Следующие итерации (для справки)

**Вторая итерация:**
- Реализовать переход на экран создания дневника (`CreateJournalScreen`)
- Реализовать переход на детальный экран дневника (`JournalDetailScreen`)

**Третья итерация:**
- Добавить обработку действий меню (редактирование, удаление, настройки доступа)
- Добавить фильтрацию и сортировку дневников
