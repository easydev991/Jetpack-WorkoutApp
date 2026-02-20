# AGENTS.md

Guidelines for AI coding agents working in this repository.

## Project Overview

Android application "SW Parks" (Street Workout Parks) built with Kotlin and Jetpack Compose. A port of the iOS version (SwiftUI-WorkoutApp).

- **Package**: `com.swparks`
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35
- **Kotlin**: 2.3.10

## Build/Lint/Test Commands

### Build

```bash
make build              # Build debug APK
./gradlew assembleDebug # Direct gradle
make clean              # Clean build cache
```

### Lint

```bash
make lint               # Run ktlint + detekt + markdownlint
./gradlew ktlintCheck   # ktlint only
./gradlew app:detekt    # detekt only
```

### Format

```bash
make format             # Auto-fix ktlint + detekt + markdown
./gradlew ktlintFormat  # ktlint only
```

### Tests

```bash
make test                          # All unit tests
./gradlew test                     # All unit tests (direct)

# Single test class
./gradlew test --tests "com.swparks.domain.usecase.LoginUseCaseTest"

# Single test method
./gradlew test --tests "com.swparks.domain.usecase.LoginUseCaseTest.invoke_whenValidCredentials_thenSavesTokenAndCallsLogin"

# Android instrumented tests
make android-test
./gradlew connectedDebugAndroidTest
```

### Full Check

```bash
make check              # Build + tests + linters
make all                # check + install on device
```

## Code Style

### Imports Order

1. AndroidX imports
2. Kotlin imports
3. Third-party libraries (alphabetical)
4. Project imports (`com.swparks.*`)

Separate groups with blank lines. Remove unused imports.

### Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| Classes | PascalCase | `LoginViewModel` |
| Functions | camelCase | `loginUser()` |
| Variables | camelCase | `currentUser` |
| Constants | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Packages | lowercase | `com.swparks.domain.usecase` |
| Composable functions | PascalCase | `LoginScreen()` |

### Types

- **Data classes** for models: `data class User(val id: Long, val name: String)`
- **Sealed classes** for UI states and results:

  ```kotlin
  sealed class LoginUiState {
      data object Idle : LoginUiState()
      data object Loading : LoginUiState()
      data class Error(val message: String, val cause: Throwable?) : LoginUiState()
  }
  ```

- **Sealed classes** for navigation routes
- **Extension functions** for readability

### Null Safety

Never use `!!`. Use safe alternatives:

```kotlin
// Preferred approaches:
val itemId = checkNotNull(savedStateHandle["itemId"]) { "ItemId is required" }
user?.let { processUser(it) }
val name = user?.name ?: "Unknown"

// NEVER do this:
val itemId = savedStateHandle["itemId"]!!  // Forbidden
```

### Error Handling

**In Use Cases** — return `Result<T>`:

```kotlin
suspend operator fun invoke(): Result<Data> = try {
    val data = repository.getData()
    Result.success(data)
} catch (e: IOException) {
    Result.failure(NetworkException("Network error: ${e.message}", e))
}
```

**In ViewModels** — use sealed UI states:

```kotlin
sealed class ScreenState {
    data object Loading : ScreenState()
    data class Success(val data: Data) : ScreenState()
    data class Error(val message: String) : ScreenState()
}
```

**One-off events** (navigation, toasts) — use `Channel`:

```kotlin
private val _events = Channel<Event>(Channel.BUFFERED)
val events = _events.receiveAsFlow()

// Send event
_events.send(Event.Success)
```

### Comments

- KDoc for public APIs
- Explain "why", not "what"
- Logs in Russian: `Log.e("Tag", "Ошибка загрузки: ${e.message}")`

## Architecture

### Three Layers

```
UI Layer (Compose + ViewModels)
    ↓
Domain Layer (Use Cases)
    ↓
Data Layer (Repositories + API + Room + DataStore)
```

### Key Patterns

- **MVVM**: ViewModel manages UI state, Compose renders
- **Unidirectional Data Flow**: State flows down, events flow up
- **Repository Pattern**: Single API for data access
- **Manual DI**: Factory methods in `AppContainer` (no Hilt)

### AppState

Global state for navigation and auth:

```kotlin
class AppState {
    var currentUser by mutableStateOf<User?>(null)
        private set
    
    val isAuthorized: Boolean
        get() = currentUser != null
}
```

### Data Strategies

| Data Type | Strategy |
|-----------|----------|
| Parks, Cities | Cache-first with server sync |
| Events | Online-first, future events not cached |
| Journals | Offline-first with sync |
| Messages | Online-first with fallback cache |
| Auth | Online-only |

## Testing

### TDD Order

**Tests → Logic → UI**

1. Write failing test first
2. Implement minimal code to pass
3. Refactor while keeping tests green

### Test Naming

```kotlin
@Test
fun functionName_whenCondition_thenExpectedResult() {
    // Given: setup
    // When: action
    // Then: assertions
}
```

### Test Libraries

- **JUnit 4** — test framework
- **MockK** — mocking (`mockk`, `coEvery`, `coVerify`)
- **kotlinx.coroutines.test** — `runTest`, `advanceUntilIdle`
- **Turbine** — Flow testing
- **Robolectric** — Android unit tests

### Test Locations

```
app/src/test/java/com/swparks/          # Unit tests
app/src/androidTest/java/com/swparks/   # Instrumented tests
```

### Example Test

```kotlin
@Test
fun login_whenValidCredentials_thenReturnsSuccess() = runTest {
    // Given
    val credentials = LoginCredentials("user@test.com", "password")
    coEvery { loginUseCase(credentials) } returns Result.success(loginSuccess)

    // When
    viewModel.login()
    advanceUntilIdle()

    // Then
    assertTrue(viewModel.uiState.value is LoginUiState.Idle)
}
```

## Key Files

| File | Purpose |
|------|---------|
| `app/build.gradle.kts` | Dependencies, build config |
| `config/detekt/detekt.yml` | Detekt rules |
| `Makefile` | Build commands |
| `.cursor/rules/*.mdc` | Cursor AI rules |
| `docs/plan-development.md` | Development roadmap |

## Pre-Commit Checklist

- [ ] `make format` passes
- [ ] `make lint` passes
- [ ] `make test` passes
- [ ] No crashes on app launch
- [ ] No deprecated API usage
