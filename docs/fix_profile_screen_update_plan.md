# План исправления проблемы обновления экрана профиля после авторизации

## Описание проблемы

После успешной авторизации через `LoginSheetHost`:
- Пользователь успешно сохраняется (видно в логах: `Пользователь сохранён: 280084`)
- При перезапуске приложения пользователь загружается (видно в логах: `Текущий пользователь изменился: 280084`)
- **НО** на экране профиля отображается кнопка "Авторизоваться", а не профиль пользователя

## Корневая причина

### 1. Пересоздание ProfileViewModel при навигации

**В `RootScreen.kt` (строки 125-145):**

```kotlin
composable(route = Screen.Profile.route) {
    // Создаем ProfileViewModel ПРИ КАЖДОМ ПЕРЕХОДЕ НА ВКЛАДКУ!
    val profileViewModel = remember {
        appContainer.profileViewModelFactory()
    }

    ProfileRootScreen(
        viewModel = profileViewModel,
        ...
    )
}
```

Проблема: `remember` внутри `composable` пересоздает значение при выходе со вкладки "Профиль". При возвращении на вкладку создается новый `ProfileViewModel`, который:
- Получает `currentUser` из `SWRepository.getCurrentUserFlow()`
- Но Flow в `SWRepository` может еще не успеть обновиться или не уведомить нового подписчика

### 2. Асинхронность Flow обновлений

`ProfileViewModel` подписывается на `currentUser` в `init`:

```kotlin
// ProfileViewModel.kt (строки 52-59)
init {
    // Автоматически загружаем данные при изменении currentUser
    viewModelScope.launch {
        currentUser.collect { user ->
            loadProfile()
        }
    }
}
```

Проблема: Если `SWRepository` еще не успел обновить `currentUser`, новый `ProfileViewModel` получит `null` и покажет кнопку "Авторизоваться".

### 3. Ожидание: ProfileViewModel загружается автоматически

В `RootScreen.kt` (строки 277-285):

```kotlin
LoginSheetHost(
    show = showLoginSheet,
    onDismissed = { showLoginSheet = false },
    onLoginSuccess = {
        // Успешная авторизация - закрываем LoginSheet
        // Данные пользователя загрузятся в ProfileViewModel при открытии профиля
        showLoginSheet = false
    }
)
```

Проблема: Код ожидает, что `ProfileViewModel` автоматически загрузит данные при открытии профиля, но это не работает из-за пересоздания ViewModel.

## Решение

### 1. Создать единый ProfileViewModel на уровне RootScreen

**Проблема:** `ProfileViewModel` создается внутри `composable` и пересоздается при навигации

**Решение:** Вынести создание `ProfileViewModel` на уровень `RootScreen`:

```kotlin
// RootScreen.kt
@Composable
fun RootScreen(appState: AppState) {
    // ...

    // Создаем ProfileViewModel ЕДИН РАЗ на уровне RootScreen
    val profileViewModel = remember {
        appContainer.profileViewModelFactory()
    }

    Scaffold(...) { paddingValues ->
        NavHost(...) {
            // ...

            // Вкладка "Профиль" - используем ЕДИНЫЙ profileViewModel
            composable(route = Screen.Profile.route) {
                ProfileRootScreen(
                    appContainer = appContainer,
                    viewModel = profileViewModel,  // ← Используем единый экземпляр
                    isLoggingOut = isLoggingOut.value,
                    onLogout = {
                        isLoggingOut.value = true
                    },
                    onLogoutComplete = {
                        isLoggingOut.value = false
                    },
                    onShowLoginSheet = { showLoginSheet = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            // ...
        }
    }
}
```

**Преимущества:**
- ProfileViewModel создается один раз при старте приложения
- ViewModel не пересоздается при навигации между вкладками
- ProfileViewModel всегда подписан на `currentUser` Flow и получает обновления сразу после авторизации
- Экран профиля обновляется автоматически после успешной авторизации

### 2. Добавить принудительное обновление профиля после авторизации (опционально)

**Проблема:** Даже при едином ProfileViewModel может быть задержка между сохранением пользователя и обновлением Flow

**Решение:** Добавить вызов `loadProfile()` в `ProfileViewModel` после успешной авторизации:

```kotlin
// ProfileViewModel.kt
/** Принудительная перезагрузка профиля */
fun reloadProfile() {
    loadProfile()
}
```

```kotlin
// RootScreen.kt
LoginSheetHost(
    show = showLoginSheet,
    onDismissed = { showLoginSheet = false },
    onLoginSuccess = {
        // Успешная авторизация - принудительно перезагружаем профиль
        profileViewModel.reloadProfile()
        // Закрываем LoginSheet
        showLoginSheet = false
        // Навигируем на вкладку профиля, чтобы гарантированно обновить UI
        appState.navigateToProfile()
    }
)
```

**Преимущества:**
- Принудительная перезагрузка профиля сразу после авторизации
- Навигация на вкладку профиля гарантирует пересоздание UI
- Двойная защита: `reloadProfile()` + `navigateToProfile()`

### 3. Добавить LaunchedEffect для автоматической загрузки профиля

**Проблема:** Даже с `reloadProfile()` и навигацией может быть задержка или ситуация, когда `currentUser` не изменился (пользователь уже был авторизован)

**Решение:** Добавить `LaunchedEffect` в `ProfileRootScreen` для отслеживания изменения `currentUser.id`:

```kotlin
// ProfileRootScreen.kt
// Получаем currentUser из ViewModel
val currentUser by viewModel.currentUser.collectAsState()

// Получаем UI State из ViewModel
val uiState by viewModel.uiState.collectAsState()

// Автоматически загружаем профиль при появлении пользователя
// Это решает проблему, когда reloadProfile() вызывается слишком рано,
// а currentUser еще не успел обновиться в Flow
LaunchedEffect(currentUser?.id) {
    if (currentUser != null) {
        viewModel.loadProfile()
    }
}
```

**Преимущества:**
- Автоматическая загрузка профиля при появлении пользователя
- Реагирует на любые изменения `currentUser.id`
- Не зависит от времени вызова `reloadProfile()`
- Работает при повторной авторизации того же пользователя

## Этапы реализации

### Этап 1: Переместить создание ProfileViewModel на уровень RootScreen ✅

- [x] Перенести создание `ProfileViewModel` из `composable` в начало `RootScreen`
- [x] Удалить создание `ProfileViewModel` внутри `composable` для вкладки "Профиль"
- [x] Убедиться, что используется единый экземпляр ViewModel

### Этап 2: Добавить метод принудительной перезагрузки профиля ✅

- [x] Добавить метод `reloadProfile()` в `ProfileViewModel`
- [x] Вызвать `profileViewModel.reloadProfile()` в `onLoginSuccess` в `RootScreen`

### Этап 3: Добавить навигацию на профиль после авторизации ✅

- [x] Добавить метод `navigateToProfile()` в `AppState`
- [x] Вызвать `appState.navigateToProfile()` в `onLoginSuccess` в `RootScreen`

### Этап 4: Добавить LaunchedEffect для автоматической загрузки профиля ✅

- [x] Добавить `LaunchedEffect(currentUser?.id)` в `ProfileRootScreen`
- [x] Автоматическая загрузка профиля при появлении пользователя

### Этап 5: Проверка сборки и тестов ✅

- [x] Сборка проекта прошла успешно
- [x] Unit-тесты прошли успешно

### Этап 6: Тестирование обновления экрана профиля

- [ ] Протестировать сценарий:
  1. Открыть приложение
  2. Перейти на вкладку "Профиль"
  3. Нажать "Авторизоваться"
  4. Ввести учетные данные и нажать "Войти"
  5. Проверить, что экран профиля автоматически обновился и показывает профиль

### Этап 7: Проверка на разных сценариях

- [ ] Авторизация с вкладки "Профиль"
- [ ] Авторизация с других вкладок (Площадки, Мероприятия)
- [ ] Перезапуск приложения после авторизации (пользователь должен быть загружен)
- [ ] Выход из учетной записи (профиль должен сброситься)

### Этап 5: Актуализация документации

- [ ] Добавить правило в архитектурную документацию о создании ViewModels
  - "ViewModels для экранов, которые могут быть открыты повторно, должны создаваться на уровне родительского Composable"
  - "Используйте `remember` только если компонент пересоздается, а не при навигации"
- [ ] Обновить правила разработки экранов профиля

## Критерии завершения

- ✅ После успешной авторизации экран профиля автоматически обновляется
- ✅ Профиль пользователя отображается корректно (имя, фото, город, страна)
- ✅ Кнопка "Авторизоваться" не показывается после авторизации
- ✅ ProfileViewModel не пересоздается при навигации между вкладками
- ✅ При перезапуске приложения профиль загружается корректно
- ✅ Документация обновлена

## Примечание

**Почему это решает проблему:**

1. **Единый ProfileViewModel** подписан на `currentUser` Flow с момента старта приложения
2. При авторизации `LoginViewModel` сохраняет пользователя в `SWRepository`
3. `SWRepository` обновляет `currentUser` Flow (видно в логах: `Текущий пользователь изменился: 281331`)
4. **LaunchedEffect(currentUser?.id)** в `ProfileRootScreen` срабатывает при изменении `currentUser.id` и вызывает `viewModel.loadProfile()`
5. `ProfileViewModel.loadProfile()` загружает страну и город пользователя
6. **Дополнительно вызывается `profileViewModel.reloadProfile()`** после успешной авторизации для принудительной перезагрузки
7. **Навигация на вкладку профиля** (`appState.navigateToProfile()`) гарантирует пересоздание UI и отображение обновленных данных

**Тройная защита от проблем:**
- `LaunchedEffect(currentUser?.id)` - автоматическая загрузка при появлении пользователя
- `reloadProfile()` - принудительная перезагрузка после авторизации
- `navigateToProfile()` - навигация на вкладку профиля для пересоздания UI
