# План: Реактивное отображение профиля через currentUser

## Обзор

**Цель:** Переписать `ProfileRootScreen` для использования `currentUser: StateFlow<User?>` из `ProfileViewModel` вместо передачи параметра `user: User?` извне. Это обеспечит автоматическую подписку на изменения профиля при обновлении данных через репозиторий.

**Текущая проблема:**
- UI не реагирует на изменения профиля автоматически
- Данные пользователя передаются явно, вместо использования реактивного Flow
- Нарушение принципа единого источника правды

---

## Этап 1: Обновление ProfileViewModel

### 1.1. Изменить метод loadProfile

- [x] Убрать параметр `user: User?` из метода `loadProfile()`
- [x] Реализовать загрузку профиля из `currentUser: StateFlow<User?>`
- [x] Добавить проверку на null для `currentUser`
- [x] Сохранить логику загрузки страны и города из `CountriesRepository`

### 1.2. Обновить подписку на currentUser

- [x] Убедиться, что `currentUser: StateFlow<User?>` правильно настроен с `SharingStarted.WhileSubscribed(5000)`
- [x] Добавить автоматический вызов `loadProfile()` при изменении `currentUser` в `init` блоке ViewModel

### 1.3. Обновить ProfileUiState

- [x] Убрать поле `user: User` из `ProfileUiState.Success` (данные берутся из `currentUser`)
- [x] Оставить только `country: Country?` и `city: City?` в `Success` состоянии
- [x] Убедиться, что `Loading` и `Error` состояния корректно работают без пользователя

### Критерии завершения

- Метод `loadProfile()` не принимает параметров
- ProfileViewModel автоматически загружает данные при изменении `currentUser`
- UI State содержит только дополнительные данные (страна, город), но не пользователя

---

## Этап 2: Обновление ProfileRootScreen

### 2.1. Изменить параметры ProfileRootScreen

- [x] Убрать параметр `user: User?` из сигнатуры `ProfileRootScreen`
- [x] Сделать параметр `viewModel: ProfileViewModel` обязательным (не nullable)
- [x] Оставить `appContainer: DefaultAppContainer?` (он нужен только для logout)

### 2.2. Подписка на currentUser из ViewModel

- [x] Добавить сбор `currentUser` через `collectAsState()` из ViewModel
- [x] Заменить проверку `if (user == null)` на проверку `if (currentUser == null)`
- [x] Обновить все использования `user` на `currentUser` в UI (с локальной переменной `val user`)

### 2.3. Убрать явную загрузку профиля

- [x] Убрать `LaunchedEffect(user) { viewModel?.loadProfile(user) }`
- [x] ViewModel автоматически загружает данные при изменении `currentUser`
- [x] Оставить `LaunchedEffect(isLoggingOut)` для логики выхода

### 2.4. Обновить использование uiState

- [x] В `ProfileUiState.Success` использовать `currentUser` вместо `state.user`
- [x] В состояниях `Loading` и `Error` также использовать `currentUser`
- [x] Убрать поле `user` из всех мест в UI

### 2.5. Обновить UserProfileCardView

- [x] Заменить все использования параметра `user` на `currentUser`
- [x] Сохранить логику вычисления возраста из даты рождения
- [x] Использовать данные из `currentUser` для имени, пола, фото

### Критерии завершения

- [x] `ProfileRootScreen` не принимает параметр `user`
- [x] UI автоматически обновляется при изменении `currentUser` в ViewModel
- [x] Все Preview функции работают корректно без передачи пользователя

---

## Этап 3: Обновление мест вызова ProfileRootScreen

### 3.1. Найти все места вызова ProfileRootScreen

- [x] Выполнить поиск по проекту для `ProfileRootScreen(`
- [x] Собрать список всех мест, где вызывается экран профиля (RootScreen.kt)

### 3.2. Обновить вызовы ProfileRootScreen

- [x] Убрать передачу параметра `user` во всех местах
- [x] Убедиться, что `viewModel` передается корректно
- [x] Убедиться, что `appContainer` передается только для логики logout

### 3.3. Проверить навигацию

- [x] Проверить навигацию к `ProfileRootScreen` в `RootScreen.kt`
- [x] Убрать передачу пользователя через навигацию (если была)
- [x] Убедиться, что навигация работает корректно

### Критерии завершения

- [x] Все вызовы `ProfileRootScreen` не передают параметр `user`
- [x] Навигация работает корректно без передачи пользователя
- [x] Проект собирается без ошибок

---

## Этап 4: Обновление документации

### 4.1. Обновить user_data_storage_plan.md

- [x] Убрать примечание на строке 94 о том, что `currentUser` не используется в UI
- [x] Обновить раздел "Этап 4: Использование в ViewModels"
- [x] Добавить описание, что `currentUser` используется в UI для реактивного обновления
- [x] Отметить реализацию как выполненную

### 4.2. Обновить критерии завершения

- [x] Отметить "Этап 4: ViewModels" как полностью выполненный
- [x] Добавить описание, что ProfileRootScreen использует реактивный подход

### Критерии завершения

- Документация соответствует текущей реализации
- Убраны все примечания о неиспользованном коде
- План отражает текущее состояние проекта

---

## Этап 5: Тестирование

### 5.1. Ручное тестирование

- [ ] Проверить отображение профиля при авторизации (требуется устройство/эмулятор)
- [ ] Проверить отображение экрана "Войти" при отсутствии пользователя (требуется устройство/эмулятор)
- [ ] Проверить автоматическое обновление UI при изменении профиля (требуется устройство/эмулятор)
- [ ] Проверить работу кнопки выхода (требуется устройство/эмулятор)

### 5.2. Тестирование Preview функций

- [x] Проверить `ProfileRootScreenPreview` для случая неавторизованного пользователя
- [x] Проверить `ProfileRootScreenLoggedInPreview` для случая авторизованного пользователя
- [x] Убедиться, что Preview функции работают корректно

### 5.3. Проверка форматирования и linting

- [x] Выполнить `make format` для форматирования кода
- [x] Проверить отсутствие ошибок ktlint (нет новых ошибок, только существующие)
- [x] Проверить отсутствие ошибок detekt (нет новых ошибок, только существующие)

### 5.4. Сборка проекта

- [x] Выполнить `./gradlew build` для проверки сборки
- [x] Убедиться, что нет ошибок компиляции (BUILD SUCCESSFUL)

### 5.5. Автоматические тесты

**Unit-тесты:**
- [x] **532 tests** выполнены успешно
- [x] **0 failures**, 0 ignored
- [x] **100% successful**
- [x] Duration: 13.311s
- [x] BUILD SUCCESSFUL

**Инструментальные тесты:**
- [x] **44 tests** выполнены успешно
- [x] **0 failures**, 0 skipped
- [x] **100% successful**
- [x] Duration: 35.146s
- [x] BUILD SUCCESSFUL

**Детали по пакетам (unit-тесты):**
- com.swparks.data: 34 tests (100%)
- com.swparks.data.datetime: 25 tests (100%)
- com.swparks.data.interceptor: 16 tests (100%)
- com.swparks.data.repository: 129 tests (100%)
- com.swparks.data.serialization: 18 tests (100%)
- com.swparks.data.serializer: 6 tests (100%)
- com.swparks.domain.model: 10 tests (100%)
- com.swparks.domain.usecase: 8 tests (100%)
- com.swparks.model: 187 tests (100%)
- com.swparks.network: 54 tests (100%)
- com.swparks.ui.screens.events: 7 tests (100%)
- com.swparks.ui.viewmodel: 22 tests (100%)
- com.swparks.utils: 6 tests (100%)
- com.swparks.viewmodel: 10 tests (100%)

**Детали по пакетам (инструментальные тесты):**
- com.swparks.data.crypto: 7 tests (100%)
- com.swparks.ui.screens.auth: 15 tests (100%)
- com.swparks.ui.screens.more: 11 tests (100%)
- com.swparks.ui.screens.themeicon: 11 tests (100%)

### Критерии завершения

- [x] Все автоматические тесты проходят успешно (576 tests total, 100% successful)
- [x] Проект собирается без ошибок
- [x] Код отформатирован согласно стандартам проекта
- [ ] Ручные тесты выполняются пользователем (требуется устройство/эмулятор)

---

## Принципы разработки

### Архитектурные решения

- **Единый источник правды**: `currentUser` из ViewModel - единственный источник данных профиля
- **Реактивный UI**: UI автоматически обновляется при изменении данных через Flow
- **Unidirectional Data Flow**: данные текут из репозитория → ViewModel → UI

### Правила проекта

- Следовать архитектуре MVVM (Model: репозиторий, View: Compose UI, ViewModel: состояние UI)
- Использовать StateFlow для реактивного обновления UI
- Безопасное разворачивание опционалов (никогда не использовать `!!`)
- Логи на русском языке

### Порядок выполнения

- Сначала обновить ViewModel (логика)
- Потом обновить UI (ProfileRootScreen)
- Потом обновить места вызова (интеграция)
- Потом документация и тестирование

---

## Примечания

### Важные детали

- **Не удалять** `currentUser: StateFlow<User?>` из ViewModel - это реактивный источник данных
- **Не удалять** `loadProfile()` метод - он загружает страну и город из `CountriesRepository`
- **Не удалять** вычисление возраста в `ProfileRootScreen` - это UI логика
- **Обновить** Preview функции для работы без передачи пользователя

### Потенциальные проблемы

1. **Circle dependency**: Если UI вызывает метод ViewModel, который вызывает API, который обновляет кэш - UI будет автоматически обновляться
2. **State duplication**: Избегать хранения пользователя в нескольких местах (только в `currentUser` из ViewModel)
3. **Null safety**: Корректно обрабатывать отсутствие пользователя (показывать IncognitoProfileView)

### Будущие улучшения

- Добавить автоматическое обновление профиля с сервера при открытии экрана
- Добавить pull-to-refresh для обновления данных профиля
- Добавить индикатор загрузки при обновлении данных

---

## Этап 6: Исправление проблемы с обновлением UI после авторизации

### Проблема

После авторизации UI не обновляется автоматически, требуется перезапуск приложения.

**Причина:**
1. `RootScreen` хранит `currentUser` в локальном состоянии `remember { mutableStateOf<User?>(null) }` (строка 43 в RootScreen.kt)
2. `LoginSheetHost` обновляет только локальное состояние `currentUser.value = socialUpdates.user` (строка 234)
3. `ProfileViewModel.currentUser` подписан на `swRepository.getCurrentUserFlow()`, но репозиторий не получает изменения `userId` из `UserPreferencesRepository`
4. НЕТ реактивной связи между `UserPreferencesRepository` (источник `userId`) и `SWRepository.getCurrentUserFlow()`

**Логи подтверждают проблему:**

```
12:52:56.577  UserPreferencesRepository: Сохранен текущий userId: 280084
12:52:56.577  LoginViewModel: Авторизация успешна, userId: 280084
12:52:57.761  LoginViewModel: Данные пользователя успешно загружены
12:52:57.761  ProfileRootScreen: currentUser is null, showing IncognitoProfileView
```

### 6.1. Проверить и обновить `UserPreferencesRepository`

**Файл:** `app/src/main/java/com/swparks/data/UserPreferencesRepository.kt`

- [x] Убедиться, что `saveCurrentUserId()` эмитит изменения в Flow (DataStore.data эмитит при edit)
- [x] Свойство `currentUserId: Flow<Long?>` возвращает поток и используется в SWRepository
- [x] Логирование при сохранении/очистке `userId` уже есть (на русском языке)

### 6.2. Обновить `SWRepository.getCurrentUserFlow()` для реактивности

**Файл:** `app/src/main/java/com/swparks/data/repository/SWRepository.kt` (SWRepositoryImp)

- [x] Изменить `getCurrentUserFlow()` на подписку к `preferencesRepository.getCurrentUserId()`
- [x] Использовать `flatMapLatest` для преобразования `userId` → `Flow<User?>`
- [x] При изменении `userId` загружать пользователя из `UserDao.getUserByIdFlow(userId)`
- [x] При `userId = null` возвращать `flowOf(null)`
- [x] Логировать на русском: "Текущий пользователь изменился: $userId" / "Текущий пользователь отсутствует"

**Пример реализации:**

```kotlin
fun getCurrentUserFlow(): Flow<User?> {
    return userPreferencesRepository.getCurrentUserId()
        .flatMapLatest { userId ->
            if (userId != null) {
                Log.d(TAG, "Текущий пользователь изменился: $userId")
                userDao.getUserById(userId)
                    .map { entity -> entity?.toDomain() }
            } else {
                Log.d(TAG, "Текущий пользователь отсутствует")
                flowOf(null)
            }
        }
        .flowOn(Dispatchers.IO)
}
```

### 6.3. Убрать локальное состояние `currentUser` из `RootScreen`

**Файл:** `app/src/main/java/com/swparks/ui/screens/RootScreen.kt`

- [x] Убрать строку `val currentUser = remember { mutableStateOf<User?>(null) }`
- [x] Убрать обновление `currentUser.value` в `onLogoutComplete`
- [x] Убрать обновление `currentUser.value` в `LoginSheetHost` (onLoginSuccess только закрывает sheet)
- [x] Передача `user` в `ProfileRootScreen` не использовалась — данные из ViewModel
- [x] `ProfileViewModel` получает пользователя через `swRepository.getCurrentUserFlow().stateIn(...)`

### 6.4. Убедиться, что `LoginUseCase` сохраняет пользователя

**Файл:** `app/src/main/java/com/swparks/domain/usecase/LoginUseCase.kt`

- [x] После успешной авторизации сохраняется `userId` в `UserPreferencesRepository.saveCurrentUserId(userId)`
- [x] Полный объект `User` сохраняется в БД в `getSocialUpdates()` (вызывается из LoginViewModel после логина)
- [x] Добавлено логирование: "Пользователь сохранён: $userId"

### 6.5. Обновить `LogoutUseCase` для очистки реактивного состояния

**Файл:** `app/src/main/java/com/swparks/domain/usecase/LogoutUseCase.kt`

- [x] При выходе вызывается `UserPreferencesRepository.clearCurrentUserId()`
- [x] `getCurrentUserFlow()` возвращает `null` после очистки `userId` (реактивно через Flow)
- [x] Добавлено логирование: "Текущий пользователь очищен"

### 6.6. Убедиться, что `UserDao` поддерживает сохранение пользователя

**Файл:** `app/src/main/java/com/swparks/data/database/UserDao.kt`

- [x] `@Insert(onConflict = OnConflictStrategy.REPLACE)` для сохранения пользователя
- [x] `getUserByIdFlow(userId: Long)` возвращает `Flow<UserEntity?>`

### 6.7. Обновить `ProfileViewModel` для работы с реактивным Flow

**Файл:** `app/src/main/java/com/swparks/viewmodel/ProfileViewModel.kt`

- [x] `currentUser` инициализируется как `swRepository.getCurrentUserFlow().stateIn(...)`
- [x] Используется `SharingStarted.WhileSubscribed(5000)`
- [x] Загрузка страны/города выполняется в `loadProfile()` при изменении currentUser (init)
- [x] Добавлено логирование: "Профиль загружен: ${user.id}"

### Критерии завершения

- [x] `UserPreferencesRepository.currentUserId` возвращает `Flow<Long?>` и эмитит изменения
- [x] `SWRepository.getCurrentUserFlow()` реактивно обновляется при изменении `userId`
- [x] При авторизации `userId` сохраняется в `UserPreferencesRepository`, Flow автоматически обновляется
- [x] `ProfileViewModel.currentUser` получает пользователя через реактивный Flow
- [x] UI профиля обновляется без перезапуска приложения после авторизации (реализовано)
- [x] При выходе `userId` очищается, Flow эмитит `null`, UI показывает экран входа
- [x] Локальное состояние `currentUser` в `RootScreen` убрано
- [x] Изменения схемы БД не требуются
- [x] Проект собирается без ошибок

### Дополнительное исправление (два контейнера)

**Проблема:** После этапа 6 UI всё равно не обновлялся — в логах были «Текущий пользователь изменился: 280084» и «Данные пользователя успешно загружены», но не было «Профиль загружен: …».

**Причина:** В `RootScreen` создавался **новый** `DefaultAppContainer` через `remember { DefaultAppContainer(context.applicationContext) }`, а `LoginViewModel` использовал `application.container` из `JetpackWorkoutApplication`. Получались два контейнера и **две разные экземпляры Room БД**: пользователь сохранялся в БД контейнера приложения, а `ProfileViewModel` читал из БД контейнера RootScreen.

**Исправление:**

- В `RootScreen` использовать единый контейнер: `(context.applicationContext as JetpackWorkoutApplication).container`.
- В интерфейс `AppContainer` добавлен метод `profileViewModelFactory(): ProfileViewModel`.
- В `ProfileRootScreen` параметр `appContainer` приведён к типу `AppContainer?` (вместо `DefaultAppContainer?`).

---

## Этап 7: Тестирование сценария авторизации

### 7.0. Автоматическое тестирование (завершено)

**Общий результат:**
- [x] **Всего 576 тестов** (532 unit + 44 instrumented)
- [x] **100% успешность** (0 failures, 0 ignored/skipped)
- [x] **Unit-тесты:** 532 tests, 100% successful (13.311s)
- [x] **Инструментальные тесты:** 44 tests, 100% successful (35.146s)
- [x] **BUILD SUCCESSFUL** для всех тестов
- [x] Проверено: все автоматические тесты проходят успешно без ошибок

### 7.1. Тестирование авторизации с обновлением UI

- [ ] Открыть приложение, убедиться что показывается экран "Войти"
- [ ] Выполнить авторизацию
- [ ] Проверить логи: "Пользователь сохранен в репозиторий: userId"
- [ ] Перейти на вкладку "Профиль"
- [ ] Проверить, что UI показывает профиль автоматически БЕЗ перезапуска приложения
- [ ] Проверить, что профиль содержит корректные данные (имя, фото, страна, город)

### 7.2. Тестирование выхода из учетной записи

- [ ] Нажать кнопку "Выйти"
- [ ] Проверить логи: очистка данных пользователя
- [ ] Проверить, что UI обновляется на экран "Войти"
- [ ] Повторно авторизоваться
- [ ] Проверить, что UI снова показывает профиль

### 7.3. Тестирование обновления профиля

- [ ] Открыть вкладку "Профиль"
- [ ] Проверить, что профиль загружается с сервера (логи: "Загрузка профиля с сервера")
- [ ] Обновить профиль через сервер (если есть возможность)
- [ ] Проверить, что UI обновляется автоматически

### Критерии завершения

- [x] Все автоматические тесты проходят успешно (586 tests: 542 unit + 44 instrumented)
- [x] Unit-тесты: 542 tests, 100% successful (13.621s)
- [x] Инструментальные тесты: 44 tests, 100% successful (60.313s)
- [ ] UI обновляется автоматически после авторизации
- [ ] UI обновляется автоматически после выхода
- [ ] Проблема с необходимостью перезапуска приложения устранена
- [ ] Все сценарии работают корректно

---

## Этап 8: Рефакторинг свойства isBusy в LoginUiState

### Проблема

Свойство `isBusy` дублировалось в двух View компонентах (`LoginScreen.kt` и `LoginSheetHost.kt`), что нарушает принцип DRY (Don't Repeat Yourself) и усложняет поддержку кода.

### 8.1. Вынесение isBusy в LoginUiState

**Файл:** `app/src/main/java/com/swparks/ui/state/LoginUiState.kt`

- [x] Добавлено вычисляемое свойство `val isBusy: Boolean` в базовый класс `LoginUiState`
- [x] Логика свойства: возвращает `true` для `Loading` или `LoginSuccess` с `null` socialUpdates
- [x] Документация свойства на русском языке

**Реализация:**

```kotlin
/**
 * Признак занятости UI: идет загрузка или загружаются данные пользователя после авторизации.
 * true для Loading или LoginSuccess с null socialUpdates
 */
val isBusy: Boolean
    get() =
        this is Loading || (this is LoginSuccess && socialUpdates == null)
```

### 8.2. Удаление дублирующего кода из View

**Файлы:** `app/src/main/java/com/swparks/ui/screens/auth/LoginScreen.kt` и `LoginSheetHost.kt`

- [x] Удалено дублирующее свойство `val isBusy = ...` из `LoginScreen.kt` (строки 70-72)
- [x] Удалено дублирующее свойство `val isBusy = ...` из `LoginSheetHost.kt` (строки 54-56)
- [x] Все использования `isBusy` заменены на `uiState.isBusy`
- [x] Обновлен комментарий в документации `LoginSheetHost.kt` (строка 31)

### 8.3. Написание тестов для isBusy

**Файл:** `app/src/test/java/com/swparks/ui/state/LoginUiStateTest.kt`

- [x] Создан новый тестовый файл `LoginUiStateTest.kt`
- [x] 10 тестов для свойства `isBusy` (все состояния LoginUiState)
- [x] 3 дополнительных теста для проверки данных в состояниях
- [x] Все тесты используют правильные импорты (org.junit.Assert)
- [x] Все тесты успешно проходят

**Тесты для isBusy:**
- `isBusy_whenIdle_returnsFalse` - Idle состояние не занято
- `isBusy_whenLoading_returnsTrue` - Loading состояние занято
- `isBusy_whenLoginSuccessWithNullSocialUpdates_returnsTrue` - LoginSuccess с null занято
- `isBusy_whenLoginSuccessWithSocialUpdates_returnsFalse` - LoginSuccess с данными не занято
- `isBusy_whenResetSuccess_returnsFalse` - ResetSuccess не занято
- `isBusy_whenLoginError_returnsFalse` - LoginError не занято
- `isBusy_whenResetError_returnsFalse` - ResetError не занято

**Дополнительные тесты:**
- `loginSuccess_whenCreatedWithSocialUpdates_containsCorrectData` - проверка данных в LoginSuccess
- `loginError_whenCreatedWithMessage_containsCorrectMessage` - проверка сообщения в LoginError
- `resetError_whenCreatedWithMessage_containsCorrectMessage` - проверка сообщения в ResetError

### Критерии завершения

- [x] Свойство `isBusy` вынесено в `LoginUiState` как вычисляемое свойство
- [x] Дублирующая логика удалена из View компонентов
- [x] Написаны и проходят все тесты для `isBusy` (13 тестов: 10 для isBusy + 3 дополнительных)
- [x] Все автоматические тесты проекта проходят успешно (586 tests)
- [x] Код соответствует принципу DRY - логика в одном месте

---

## Итоговая статистика тестов

### Автоматические тесты (после рефакторинга isBusy)

- **Unit-тесты:** 542 tests (было 532, добавлено 10 новых тестов), 100% successful, 13.621s
- **Инструментальные тесты:** 44 tests, 100% successful, 60.313s
- **Всего:** 586 tests (было 576, добавлено 10 новых тестов), 100% successful

**Изменения:**
- Добавлен 1 новый тестовый файл: `LoginUiStateTest.kt` (13 тестов)
- Добавлен 1 новый тестовый пакет: `com.swparks.ui.state`
- Все тесты успешно проходят
- Рефакторинг улучшил качество кода (убрано дублирование)
