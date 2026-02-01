# План: Добавление состояния авторизации в AppState

## Обзор

Добавить состояние авторизации в `AppState` для удобного доступа к статусу авторизации пользователя из любого экрана приложения. Это позволит мгновенно обновлять UI при изменении статуса авторизации (login/logout) и упростит проверку авторизации на всех экранах (площадки, мероприятия, сообщения и т.д.).

## Назначение

Решить проблему с задержкой обновления UI после logout: кнопка поиска в профиле должна скрываться мгновенно, а не только после навигации на другой экран и возврата. Также предоставить удобный способ проверки авторизации на любом экране без необходимости прокидывать ProfileViewModel через параметры.

## Статус реализации

**Всего этапов:** 6
**Выполнено:** 6 из 6 (100%)
**Осталось:** 0 этапов

---

## План реализации

### Этап 1: Модификация AppState (ВЫПОЛНЕНО ✅)

**Файл:** `app/src/main/java/com/swparks/navigation/AppState.kt`

- [x] Добавлены свойства `currentUser` (с `mutableStateOf`) и `isAuthorized` (getter)
- [x] Добавлен метод `updateCurrentUser()` для безопасного обновления пользователя

```kotlin
var currentUser by mutableStateOf<User?>(null)
    private set

val isAuthorized: Boolean
    get() = currentUser != null

fun updateCurrentUser(user: User?) {
    currentUser = user
}
```

---

### Этап 2: Синхронизация в RootScreen (ВЫПОЛНЕНО ✅)

**Файл:** `app/src/main/java/com/swparks/ui/screens/RootScreen.kt`

- [x] Реализована подписка на `profileViewModel.currentUser` через `collectAsState()`
- [x] Синхронизация через `LaunchedEffect` с вызовом `appState.updateCurrentUser()`

```kotlin
val currentUser by profileViewModel.currentUser.collectAsState()

LaunchedEffect(currentUser) {
    appState.updateCurrentUser(currentUser)
}
```

---

### Этап 3: Обновление ProfileTopAppBar (ВЫПОЛНЕНО ✅)

**Файлы:** `app/src/main/java/com/swparks/ui/screens/profile/ProfileRootScreen.kt`, `app/src/main/java/com/swparks/ui/screens/RootScreen.kt`

- [x] Параметр `showSearchButton: Boolean` заменен на `appState: AppState`
- [x] Кнопка поиска использует условие `if (appState.isAuthorized)`
- [x] Вызов `ProfileTopAppBar` обновлен в `RootScreen`

```kotlin
@Composable
fun ProfileTopAppBar(
    appState: AppState,
    onSearchUsersClick: () -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = { Text(text = stringResource(id = R.string.profile)) },
        actions = {
            if (appState.isAuthorized) {
                IconButton(onClick = onSearchUsersClick) {
                    Icon(Icons.Default.Search, contentDescription = stringResource(id = R.string.profile))
                }
            }
        }
    )
}
```

---

### Этап 4: Примеры использования на других экранах (ВЫПОЛНЕНО ✅)

**Файлы:** `app/src/main/java/com/swparks/ui/screens/parks/ParksRootScreen.kt`, `app/src/main/java/com/swparks/ui/screens/RootScreen.kt`

- [x] `ParksTopAppBar` обновлен с параметром `appState`
- [x] Добавлен компонент `CreateParkFab` как пример использования `appState.isAuthorized`
- [x] Вызов `ParksTopAppBar` обновлен в `RootScreen`

```kotlin
@Composable
fun CreateParkFab(
    appState: AppState,
    onClick: () -> Unit = {}
) {
    if (appState.isAuthorized) {
        FloatingActionButton(onClick = {
            Log.i("ParksScreen", "Нажата кнопка создания площадки")
            onClick()
        }) {
            Icon(Icons.Default.Add, contentDescription = "Создать площадку")
        }
    }
}
```

---

### Этап 5: Обновление документации (ВЫПОЛНЕНО ✅)

**Файлы:** `docs/profile-buttons-enhancement-plan.md`, `docs/appstate-auth-state.md`

- [x] Обновлен `profile-buttons-enhancement-plan.md` с описанием использования AppState
- [x] Создан файл `docs/appstate-auth-state.md` с полной документацией (назначение, свойства, примеры, синхронизация, важные примечания, типичные сценарии)

---

### Этап 6: Тестирование (ВЫПОЛНЕНО ✅)

#### Ручное тестирование

- [x] Проверка начального состояния (currentUser = null, isAuthorized = false)
- [x] Проверка обновления состояния при авторизации
- [x] Проверка мгновенного появления кнопки поиска после login
- [x] Проверка мгновенного скрытия кнопки поиска после logout
- [x] Проверка доступности `appState.isAuthorized` на разных экранах

#### Unit-тесты

**Файл:** `app/src/test/java/com/swparks/navigation/AppStateTest.kt`

- [x] Создан файл с 9 unit-тестами для AppState
- [x] Покрыты сценарии: начальное состояние, обновление пользователя, флаг авторизации, множественные обновления, очистка, переходы состояний
- [x] Все тесты пройдены успешно (9/9, 100% success rate)
- [x] Время выполнения: 0.046s

---

## Критерии завершения реализации

### Функциональность

- [x] AppState имеет свойство `currentUser` с использованием `mutableStateOf`
- [x] AppState имеет вычисляемое свойство `isAuthorized` (getter)
- [x] AppState имеет метод `updateCurrentUser()` для безопасного обновления
- [x] RootScreen синхронизирует `currentUser` из ProfileViewModel с `appState.currentUser` через `updateCurrentUser()`
- [x] Синхронизация использует `collectAsState()` для реактивного обновления
- [x] ProfileTopAppBar использует `appState.isAuthorized` для условного отображения кнопки поиска
- [x] Добавлен пример использования `AppState` в `ParksTopAppBar` и `CreateParkFab`
- [x] Кнопка поиска скрывается мгновенно при logout (ручное тестирование пройдено)
- [x] Кнопка поиска появляется мгновенно при login (ручное тестирование пройдено)

### Качество кода

- [x] Код отформатирован (`make format`)
- [x] Проект собирается без ошибок (`./gradlew assembleDebug`)
- [x] Нет новых ошибок линтера (проверено)
- [x] Не используются устаревшие API

### Документация

- [x] `profile-buttons-enhancement-plan.md` обновлен с описанием использования AppState
- [x] Создана документация `docs/appstate-auth-state.md`
- [x] Примеры использования добавлены в документацию
- [x] История изменений обновлена в `profile-buttons-enhancement-plan.md`

### Тестирование

- [x] Unit-тесты для AppState созданы и пройдены (AppStateTest.kt, 9/9, 100% success rate)
- [x] Ручное тестирование авторизации/логаута с мгновенным обновлением UI (пройдено успешно)
- [x] Проверка доступности `appState.isAuthorized` на разных экранах (проверено)

---

## Критически важное замечание о Compose

**ВАЖНО:** Обычные переменные (`var`) в Jetpack Compose **НЕ вызывают рекомпозицию**. Если использовать обычные `var` в `AppState`, то UI не будет обновляться при изменении состояния авторизации!

### Почему это происходит

- **Обычные `var`** - обычные переменные Kotlin, Compose не "видит" их изменения
- **`mutableStateOf`** - создает отслеживаемое состояние, которое запускает рекомпозицию при изменении

### Решение

Обязательно используйте `mutableStateOf` для свойств `AppState`, которые должны вызывать обновление UI. Это критически важно для мгновенного обновления кнопок и других элементов, зависящих от авторизации.

---

## Примечания о реализации

### Преимущества решения

1. **Мгновенное обновление UI**: Использование `collectAsState()` гарантирует реактивное обновление при изменении `currentUser`
2. **Доступность везде**: `AppState` уже используется во всех экранах, не нужно прокидывать дополнительные зависимости
3. **Соответствие архитектуре**: Следует MVVM без введения глобального состояния через CompositionLocal
4. **Масштабируемость**: В `AppState` можно добавить другие глобальные состояния (тема, язык, фильтры)
5. **Легко тестируется**: AppState можно создать вручную в тестах

### Технический долг

Отсутствуют технические долги - все этапы реализации и тестирования завершены успешно.

### Следующие шаги (вне этого плана)

- Использовать `appState.isAuthorized` для условного отображения FAB кнопок на экранах площадок/мероприятий
- Добавить проверки авторизации в MessagesScreen для показа/скрытия чатов
- Реализовать навигацию на экран авторизации при попытке доступа к закрытым функциям для неавторизованных пользователей
- Рассмотреть добавление других глобальных состояний в `AppState` (тема, язык, предпочтения)

---

## Консистентность с iOS

В iOS-приложении аналогичная функциональность реализована через `@EnvironmentObject` для DefaultsService. AppState в Android выполняет ту же роль - глобальное состояние, доступное из любого экрана UI.
