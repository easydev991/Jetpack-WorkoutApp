# План: Реактивное отображение профиля через currentUser

## Обзор

**Цель:** Переписать `ProfileRootScreen` для использования `currentUser: StateFlow<User?>` из `ProfileViewModel` вместо передачи параметра `user: User?` извне. Это обеспечит автоматическую подписку на изменения профиля при обновлении данных через репозиторий.

**Текущая проблема:**
- UI не реагирует на изменения профиля автоматически
- Данные пользователя передаются явно, вместо использования реактивного Flow
- Нарушение принципа единого источника правды

---

## Этап 1-4: Реализация реактивного профиля ✅

### Выполнено

**ProfileViewModel:**
- [x] Метод `loadProfile()` не принимает параметров, использует `currentUser: StateFlow<User?>`
- [x] Автоматическая загрузка данных при изменении `currentUser` через `init`
- [x] `ProfileUiState.Success` содержит только `country` и `city` (без `user`)

**ProfileRootScreen:**
- [x] Не принимает параметр `user: User?`, использует `viewModel: ProfileViewModel`
- [x] Подписка на `currentUser` через `collectAsState()` из ViewModel
- [x] Убрана явная загрузка профиля через `LaunchedEffect(user)`
- [x] Все компоненты используют `currentUser` вместо параметра `user`

**Интеграция:**
- [x] Все вызовы `ProfileRootScreen` обновлены (убрана передача `user`)
- [x] Навигация работает корректно без передачи пользователя

**Документация:**
- [x] Обновлен `user_data_storage_plan.md` (убраны примечания о неиспользовании)
- [x] Отмечено использование `currentUser` в UI для реактивного обновления

### Критерии завершения

- [x] UI автоматически обновляется при изменении `currentUser` в ViewModel
- [x] Все Preview функции работают корректно
- [x] Проект собирается без ошибок

---

## Этап 5: Тестирование

### Выполнено

**Автоматические тесты:**
- [x] Unit-тесты: 532 tests, 100% successful, 13.311s
- [x] Инструментальные тесты: 44 tests, 100% successful, 35.146s
- [x] **Всего: 576 tests, 100% successful**

**Форматирование и сборка:**
- [x] `make format` выполнен успешно
- [x] `./gradlew build` - BUILD SUCCESSFUL
- [x] Нет новых ошибок ktlint и detekt

**Preview функции:**
- [x] `ProfileRootScreenPreview` (неавторизованный пользователь)
- [x] `ProfileRootScreenLoggedInPreview` (авторизованный пользователь)

### Ручное тестирование (требуется устройство/эмулятор)

- [ ] Проверить отображение профиля при авторизации
- [ ] Проверить отображение экрана "Войти" при отсутствии пользователя
- [ ] Проверить автоматическое обновление UI при изменении профиля
- [ ] Проверить работу кнопки выхода

### Критерии завершения

- [x] Все автоматические тесты проходят успешно (576 tests)
- [x] Проект собирается без ошибок
- [ ] Ручные тесты выполняются пользователем

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

## Этап 6: Исправление проблемы с обновлением UI после авторизации ✅

### Проблема

После авторизации UI не обновляется автоматически, требуется перезапуск приложения.

**Причины:**
- Локальное состояние `currentUser` в `RootScreen` (нарушение реактивности)
- Нет реактивной связи между `UserPreferencesRepository` и репозиторием
- Два контейнера создавали разные экземпляры Room БД

### Выполнено

**Data Layer:**
- [x] `UserPreferencesRepository.currentUserId` возвращает `Flow<Long?>` с эмитом изменений
- [x] `SWRepository.getCurrentUserFlow()` использует `flatMapLatest` для реактивного обновления `userId` → `User?`
- [x] При изменении `userId` загружается из `UserDao.getUserByIdFlow()`, при `null` → `flowOf(null)`
- [x] Логирование: "Текущий пользователь изменился: $userId" / "Текущий пользователь отсутствует"
- [x] `UserDao` имеет `@Insert(REPLACE)` и `getUserByIdFlow(userId)`

**Domain Layer:**
- [x] `LoginUseCase` сохраняет `userId` в `UserPreferencesRepository`
- [x] Полный объект `User` сохраняется в БД через `getSocialUpdates()`
- [x] `LogoutUseCase` очищает `userId` через `clearCurrentUserId()`

**UI Layer:**
- [x] `RootScreen`: убрано локальное состояние `currentUser`
- [x] Используется единый контейнер: `(context.applicationContext as JetpackWorkoutApplication).container`
- [x] `AppContainer`: добавлен метод `profileViewModelFactory()`
- [x] `ProfileRootScreen`: параметр `appContainer` приведен к `AppContainer?`
- [x] `ProfileViewModel.currentUser` через `swRepository.getCurrentUserFlow().stateIn(...)` с `SharingStarted.WhileSubscribed(5000)`

### Критерии завершения

- [x] UI профиля обновляется без перезапуска приложения
- [x] При выходе Flow эмитит `null`, UI показывает экран входа
- [x] Изменения схемы БД не требуются
- [x] Проект собирается без ошибок

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

- [x] Логика `isBusy` вынесена в одно место (LoginUiState)
- [x] Дублирующий код удален из View компонентов
- [x] Все автоматические тесты проходят (586 tests)
- [x] Код соответствует принципу DRY

---

## Итоговая статистика тестов

### Автоматические тесты (итого)

- **Unit-тесты:** 542 tests, 100% successful, 13.621s
- **Инструментальные тесты:** 44 tests, 100% successful, 60.313s
- **Всего:** 586 tests, 100% successful

**Изменения:**
- Добавлен новый тестовый файл: `LoginUiStateTest.kt` (13 тестов)
- Добавлен новый тестовый пакет: `com.swparks.ui.state`
- Все тесты успешно проходят
- Рефакторинг улучшил качество кода (убрано дублирование)
