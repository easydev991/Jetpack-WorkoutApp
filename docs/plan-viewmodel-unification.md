# Унификация создания ViewModel в проекте

## Контекст и цель

Документ описывает **точечную унификацию в `RootScreen`**:

- убрать создание ViewModel через `remember { appContainer.xxxViewModelFactory(...) }`;
- перейти на `viewModel(...)` + `ViewModelProvider.Factory`;
- сохранить текущую семантику shared ViewModel и навигационных сценариев.

Цель: привести жизненный цикл ViewModel к стандартному для Jetpack и устранить риск потери состояния при `config change`.

---

## Текущая ситуация (факт)

В `app/src/main/java/com/swparks/ui/screens/RootScreen.kt` используются **три** подхода:

| Подход | Пример | Config change |
|---|---|---|
| `remember { appContainer.xxxViewModelFactory(...) }` с возвратом готового VM | `ProfileViewModel`, `DialogsViewModel`, `UserFriendsViewModel` | ❌ не гарантирует сохранение как ViewModelOwner-scoped VM |
| `viewModel(factory = ...)` | `EventsViewModel` ✅, `EventDetailViewModel`, `EventFormViewModel` | ✅ стандартный lifecycle ViewModel |
| Прямой `ViewModelProvider(ViewModelStore(), factory)` | `ThemeIconViewModel` | ⚠️ отдельный `ViewModelStore`, вне общей стратегии |

> **Примечание:** `EventsViewModel` — пример корректного подхода, который уже используется в `RootScreen` (строка 170). Эта VM создаётся через `viewModel<EventsViewModel>(factory = EventsViewModel.Factory)`.

### Подтвержденный баг

В `RootScreen` есть бессмысленное создание `BlacklistViewModel` без использования результата:

```kotlin
remember {
    appContainer.blacklistViewModelFactory()
}
```

Это лишняя инициализация (объект создается и сразу теряется).

---

## Важное уточнение по TextEntryViewModel

`TextEntryViewModel` **не является багом в контексте этой задачи**.

- `TextEntrySheetHost` вызывается только при `show && mode != null` из экранов-сценариев.
- При закрытии sheet host удаляется из composition, и при следующем открытии VM создается заново.
- Дополнительно при открытии вызывается `viewModel.resetState()`.

Следовательно, текущая модель (новая VM на новый сценарий) сохранена и менять её в рамках этого плана не нужно.

---

## Scope миграции

### In scope

Только ViewModel, создаваемые в `RootScreen` через `remember { appContainer... }`:

- `ProfileViewModel`
- `DialogsViewModel`
- `EditProfileViewModel`
- `ChatViewModel`
- `SearchUserViewModel`
- `FriendsListViewModel`
- `UserTrainingParksViewModel`
- `UserFriendsViewModel`
- `BlacklistViewModel`
- `OtherUserProfileViewModel`
- `JournalsViewModel`
- `JournalEntriesViewModel`
- `ChangePasswordViewModel`

### Out of scope

- `TextEntryViewModel` и `TextEntrySheetHost`.
- Полная унификация всего приложения за пределами `RootScreen`.
- Рефакторинг навигации, не требуемый для миграции factory.

---

## План изменений

## Этап 1. Подготовить factory-API без поломки `AppContainer`

### 1.1 ViewModel без runtime-параметров

Для VM без аргументов добавить/использовать `ViewModelProvider.Factory`:

```kotlin
companion object {
    fun factory(appContainer: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            ProfileViewModel(
                countriesRepository = appContainer.countriesRepository,
                swRepository = appContainer.swRepository,
                logger = appContainer.logger,
                userNotifier = appContainer.userNotifier
            )
        }
    }
}
```

### 1.2 ViewModel с runtime-параметрами

Оставить фабрики в виде `fun ...factory(arg1, arg2, ...)`, которые возвращают `ViewModelProvider.Factory`.

Пример:

```kotlin
companion object {
    fun factory(appContainer: AppContainer, userId: Long): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            UserFriendsViewModel(
                userId = userId,
                swRepository = appContainer.swRepository,
                logger = appContainer.logger,
                userNotifier = appContainer.userNotifier
            )
        }
    }
}
```

### 1.3 Не переводить все factory-методы `AppContainer` в `val`

Нельзя глобально заменить `fun ...Factory(arg)` на `val`, потому что часть VM требует runtime-аргументы (`userId`, `journalId`, `mode`).

Рекомендуемый вариант:

- либо не трогать `AppContainer` на этом этапе, а вызывать VM companion-factory напрямую;
- либо добавить в `AppContainer` новые методы, возвращающие `ViewModelProvider.Factory`, сохранив параметризованные методы как функции.

---

## Этап 2. Миграция `RootScreen` на `viewModel(...)`

### 2.1 Без параметров

Заменить:

```kotlin
val profileViewModel = remember { appContainer.profileViewModelFactory() }
```

на:

```kotlin
val profileViewModel: ProfileViewModel = viewModel(
    factory = ProfileViewModel.factory(appContainer)
)
```

Аналогично для:

- `DialogsViewModel`
- `EditProfileViewModel`
- `ChatViewModel`
- `SearchUserViewModel`
- `FriendsListViewModel`
- `BlacklistViewModel`
- `ChangePasswordViewModel`

### 2.2 С параметрами

Для экранов с параметрами использовать `viewModel(..., factory = XxxViewModel.factory(appContainer, args...))`:

- `UserTrainingParksViewModel(userId)` — **используется в двух маршрутах:** `Screen.SelectParkForEvent` и `Screen.UserTrainingParks`. Миграция идентична для обоих.
- `UserFriendsViewModel(userId)`
- `OtherUserProfileViewModel(userId)`
- `JournalsViewModel(userId)` — `remember(appContainer)` не включает `userId` в ключи; в текущей навигации это обычно безопасно из-за отдельного `NavBackStackEntry`, но при миграции на `viewModel()` будет явная и более надежная привязка к owner/аргументам.
- `JournalEntriesViewModel(journalOwnerId, journalId, savedStateHandle)` — требует `SavedStateHandle`. При миграции нужно передавать `navBackStackEntry` как `viewModelStoreOwner`, чтобы `SavedStateHandle` инжектировался корректно, либо использовать `SavedStateHandle` из `navBackStackEntry.savedStateHandle` как параметр factory.

#### Test-gate для P2 (`JournalsViewModel`)

Перед обязательным рефакторингом `JournalsViewModel` зафиксировать поведение тестами:

1. Integration: `JournalsList(userId=A)` → выход → `JournalsList(userId=B)`; проверить, что VM работает с `userId=B`, а не с предыдущим значением.
2. Integration/Robolectric: `JournalsList(userId=A)` + recreate (config change); проверить, что после пересоздания сохраняется корректный `userId=A`.
3. Integration: повторный вход `JournalsList(userId=A)`; зафиксировать ожидаемый контракт (reuse/refresh) и проверить его.

Правило принятия решения:

- если тесты зелёные, считать P2 потенциальным улучшением, а не подтвержденным багом;
- если есть красный сценарий (reuse VM с неправильным `userId`), переводить доработку в обязательную.

### 2.3 Удалить лишний вызов `blacklistViewModelFactory()`

Удалить ранний `remember { appContainer.blacklistViewModelFactory() }`, оставить только реальное создание VM в маршруте `Screen.Blacklist`.

---

## Этап 3. Scoped/shared поведение

Для shared-сценариев зафиксировать owner явно и одинаково для связанных экранов:

- `ProfileViewModel` — используется в `ProfileScreen` и других экранах; должен сохранять состояние при навигации между вкладками.
- `EditProfileViewModel` — shared между `EditProfileScreen`, `SelectCountryScreen`, `SelectCityScreen`.
- `DialogsViewModel` — shared, т.к. иногда нужно обновить список диалогов с другого экрана (например, после отправки сообщения в `ChatScreen`).

Важно: не делать безусловный переход на activity-scope. Scope должен соответствовать текущему UX и маршрутизации (обычно root graph / нужный back stack entry).

---

## Этап 4. Проверка и тесты

### 4.1 Unit

- Проверка factory (создание VM, передача зависимостей).
- Проверка параметризованных factory (`userId`, `journalId`, `mode`).

### 4.2 Интеграционные/ручные

- `Profile`/`EditProfile`/`SelectCountry`/`SelectCity`: поворот экрана, проверка сохранения состояния и shared-поведения `EditProfileViewModel`.
- `Dialogs` → `Chat` → возврат: проверка обновления списка диалогов (shared `DialogsViewModel`).
- `Chat`, `Journals`, `JournalEntries`, `OtherUserProfile`: поворот и возврат в стек.
- `Blacklist`: отсутствие лишней ранней инициализации, корректная работа экрана.
- P2 gate (`JournalsViewModel`): выполнить 3 сценария из раздела Test-gate и принять решение по рефакторингу только по результатам тестов.

### 4.3 Регрессии

- `make lint`
- `make test`
- smoke run приложения.

---

## Критерии завершения

- В `RootScreen` не осталось `remember { appContainer.xxxViewModelFactory(...) }` для ViewModel.
- Лишний вызов `blacklistViewModelFactory()` удален.
- Все мигрированные VM создаются через `viewModel(...)` + `ViewModelProvider.Factory`.
- Shared VM (`ProfileViewModel`, `EditProfileViewModel`, `DialogsViewModel`) используют согласованный scope.
- Для `JournalsViewModel` зафиксирована корректная привязка к owner и аргументу `userId` (без неоднозначности `remember`-ключей).
- `JournalEntriesViewModel` получает `SavedStateHandle` корректно.
- `TextEntryViewModel` остается сценарной (эфемерной) и не включается в эту миграцию.
- `make lint` и `make test` проходят.

---

## Риски и митигация

| Риск | Митигация |
|---|---|
| Изменение scope shared VM сломает UX | Явно выбрать owner для shared VM и проверить переходы между связанными экранами |
| Ошибка в параметризованных factory (не тот `userId`/`journalId`) | Добавить тесты на создание VM с аргументами и smoke-проверки навигации |
| `JournalEntriesViewModel` требует `SavedStateHandle` | При миграции использовать `navBackStackEntry` как `viewModelStoreOwner` или передавать `savedStateHandle` явно в factory |
| Частичная миграция оставит несогласованные паттерны | Ограничить scope этим документом (`RootScreen`) и зафиксировать follow-up для остальных экранов |
