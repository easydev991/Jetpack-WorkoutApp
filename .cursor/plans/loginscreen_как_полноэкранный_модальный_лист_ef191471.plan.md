---
name: LoginScreen как полноэкранный модальный лист
overview: Переделать LoginScreen из обычного экрана в NavHost в полноэкранный модальный лист с запретом случайного закрытия
todos:
  - id: create-login-sheet-host
    content: Создать компонент LoginSheetHost с ModalBottomSheet для полноэкранного показа LoginScreen
    status: pending
  - id: update-rootscreen
    content: Обновить RootScreen для использования LoginSheetHost и управления состоянием showLoginSheet
    status: pending
  - id: update-profilerootscreen
    content: Обновить ProfileRootScreen для открытия LoginSheet через колбэк onShowLoginSheet вместо навигации
    status: pending
isProject: false
---

# План реализации LoginScreen как полноэкранного модального листа

## Текущая ситуация

- LoginScreen открывается как обычный экран в NavHost через `navController.navigate(Screen.Login.route)` в RootScreen.kt:206-224
- Закрытие происходит через `navController.popBackStack()`
- В LoginScreen уже есть крестик в TopAppBar (LoginModalAppBar), который передаёт `onDismiss` и блокируется при `isLoading`

## Основные задачи

### 1. Создать компонент LoginSheetHost

**Файл:** `app/src/main/java/com/swparks/ui/screens/auth/LoginSheetHost.kt`

**Реализация:**

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginSheetHost(
    show: Boolean,
    isLoading: Boolean,
    onDismissed: () -> Unit,
    onLoginSuccess: (Result<SocialUpdates>) -> Unit
) {
    var allowHide by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            if (newValue == SheetValue.Hidden) allowHide else true
        }
    )

    if (show) {
        ModalBottomSheet(
            onDismissRequest = { /* Игнорируем тап вне sheet */ },
            sheetState = sheetState,
            windowInsets = WindowInsets(0) // Полноэкранный режим
        ) {
            LoginScreen(
                onDismiss = {
                    if (isLoading) return@LoginScreen
                    scope.launch {
                        allowHide = true
                        sheetState.hide()
                        allowHide = false
                        onDismissed()
                    }
                },
                onLoginSuccess = { result ->
                    scope.launch {
                        allowHide = true
                        sheetState.hide()
                        allowHide = false
                        onLoginSuccess(result)
                    }
                }
            )
        }
    }
}
```

### 2. Изменить RootScreen для использования LoginSheetHost

**Файл:** `app/src/main/java/com/swparks/ui/screens/RootScreen.kt`

**Изменения:**

- Добавить состояние `showLoginSheet` в RootScreen
- Заменить `composable(route = Screen.Login.route)` на `LoginSheetHost` в UI (не в NavHost)
- Удалить или оставить пустой composable для Login.route (для обратной совместимости)

**Код изменений:**

```kotlin
@Composable
fun RootScreen(appState: AppState) {
    // ... существующий код ...
    
    // Добавляем состояние для LoginSheet
    var showLoginSheet by remember { mutableStateOf(false) }
    
    Scaffold(
        bottomBar = { BottomNavigationBar(appState = appState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavHost(
                navController = appState.navController,
                startDestination = Screen.Parks.route,
                modifier = Modifier.fillMaxSize(),
            ) {
                // ... все существующие маршруты ...
                
                // Оставляем пустой composable для обратной совместимости
                composable(route = Screen.Login.route) {
                    // LoginScreen теперь показывается через LoginSheetHost
                }
            }
            
            // LoginSheetHost поверх NavHost
            LoginSheetHost(
                show = showLoginSheet,
                isLoading = false, // Передаем реальный статус из LoginViewModel
                onDismissed = { showLoginSheet = false },
                onLoginSuccess = { result ->
                    result.onSuccess { socialUpdates ->
                        currentUser.value = socialUpdates.user
                        showLoginSheet = false
                    }
                }
            )
        }
    }
}
```

### 3. Изменить ProfileRootScreen для открытия LoginSheet

**Файл:** `app/src/main/java/com/swparks/ui/screens/profile/ProfileRootScreen.kt`

**Изменения:**

- Добавить параметр `onShowLoginSheet` в ProfileRootScreen
- Изменить навигацию с `navController?.navigate(Screen.Login.route)` на вызов `onShowLoginSheet()`

**Код изменений:**

```kotlin
@Composable
fun ProfileRootScreen(
    modifier: Modifier = Modifier,
    user: User?,
    appContainer: com.swparks.data.DefaultAppContainer? = null,
    navController: NavController? = null,
    isLoggingOut: Boolean = false,
    onLogout: () -> Unit = {},
    onLogoutComplete: () -> Unit = {},
    onShowLoginSheet: () -> Unit = {} // Новый параметр
) {
    // ... существующий код ...
    
    IncognitoProfileView(
        modifier = modifier
            .fillMaxWidth()
            .padding(paddingValues)
            .padding(
                start = dimensionResource(R.dimen.spacing_regular),
                end = dimensionResource(R.dimen.spacing_regular)
            ),
        onClickAuth = onShowLoginSheet // Изменено с навигации
    )
}
```

### 4. Обновить RootScreen для передачи onShowLoginSheet в ProfileRootScreen

**Файл:** `app/src/main/java/com/swparks/ui/screens/RootScreen.kt`

**Изменения:**

- Передать `onShowLoginSheet = { showLoginSheet = true }` в ProfileRootScreen

**Код:**

```kotlin
// Вкладка "Профиль"
composable(route = Screen.Profile.route) {
    ProfileRootScreen(
        user = currentUser.value,
        appContainer = appContainer,
        navController = appState.navController,
        isLoggingOut = isLoggingOut.value,
        onLogout = { isLoggingOut.value = true },
        onLogoutComplete = {
            currentUser.value = null
            isLoggingOut.value = false
        },
        onShowLoginSheet = { showLoginSheet = true } // Новый параметр
    )
}
```

### 5. Добавить необходимые импорты

**Файл:** `app/src/main/java/com/swparks/ui/screens/RootScreen.kt`

**Добавить импорты:**

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import kotlinx.coroutines.launch
```

## Проверка требований

### Acceptance Criteria

- ✅ LoginScreen открывается как полноэкранный модальный sheet поверх текущего экрана
- ✅ Тап по фону, swipe-down не закрывают sheet (через `confirmValueChange` и `allowHide`)
- ✅ Back press не закрывает sheet (если доступно `shouldDismissOnBackPress`)
- ✅ Крестик закрывает sheet только когда `isLoading == false`
- ✅ При `isLoading == true` крестик визуально disabled (уже реализовано в LoginScreen.kt:180-182)
- ✅ После успешной авторизации sheet закрывается программно и пользователь возвращается в профиль

## Порядок реализации

1. Создать `LoginSheetHost.kt`
2. Обновить `RootScreen.kt` для использования LoginSheetHost
3. Обновить `ProfileRootScreen.kt` для открытия LoginSheet через колбэк
4. Протестировать в эмуляторе (выполнит пользователь)

## Файлы для изменения

1. Создать: `app/src/main/java/com/swparks/ui/screens/auth/LoginSheetHost.kt`
2. Изменить: `app/src/main/java/com/swparks/ui/screens/RootScreen.kt`
3. Изменить: `app/src/main/java/com/swparks/ui/screens/profile/ProfileRootScreen.kt`
