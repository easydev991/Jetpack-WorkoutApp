# AGENTS.md

Guidelines for AI coding agents working in this repository.

## Project Overview

Android application "SW Parks" (Street Workout Parks), built with Kotlin and Jetpack Compose. A port of the iOS version (SwiftUI-WorkoutApp).

- Package: `com.swparks`
- Min SDK: 26
- Target SDK: 35
- Kotlin: 2.3.20

## AI Workflow

- Serena MCP is available in this repository.
- Use Serena first for symbol search, references, and structure.
- Prefer symbol-level navigation over full-file reads.
- Read only the minimal relevant code needed for the task.
- Avoid scanning many large files sequentially.
- Use broad reads only for final verification or very small files.

Preferred order:

1. Find relevant symbols/files with Serena
2. Read only needed sections
3. Make changes
4. Re-check affected usages
5. Run relevant tests/lint

Prefer Serena especially for:

- refactoring
- finding usages/references
- ViewModel / UseCase / Repository relationships
- navigation changes
- impact analysis

## Build / Lint / Test

```bash
make build
./gradlew assembleDebug
make clean

make lint
./gradlew ktlintCheck
./gradlew app:detekt

make format
./gradlew ktlintFormat
```

## Test

```bash
make test
./gradlew :app:testDebugUnitTest

# Single test class
./gradlew :app:testDebugUnitTest --tests "com.swparks.domain.usecase.LoginUseCaseTest"

# Single test method
./gradlew :app:testDebugUnitTest --tests "com.swparks.domain.usecase.LoginUseCaseTest.invoke_whenValidCredentials_thenSavesTokenAndCallsLogin"

# Android instrumented tests
make android-test
./gradlew connectedDebugAndroidTest
```

## Code Style

### Imports

Order imports as:

1. AndroidX
2. Kotlin
3. Third-party libraries
4. com.swparks.*

Separate groups with blank lines. Remove unused imports.

### Naming

- Classes: PascalCase
- Functions/variables: camelCase
- Constants: UPPER_SNAKE_CASE
- Packages: lowercase
- Composables: PascalCase

### Types

- Use data classes for models
- Use sealed classes for UI state and navigation
- Use extension functions when they improve readability

### Null Safety

Never use `!!`.

Prefer:

```kotlin
val itemId = checkNotNull(savedStateHandle["itemId"]) { "ItemId is required" }
user?.let { processUser(it) }
val name = user?.name ?: "Unknown"
```

### Error Handling

- In UseCases, return `Result<T>`
- In ViewModels, use sealed UI state
- For one-off events (navigation, toast), use `Channel`

### Comments and Logs

- Add KDoc for public APIs
- Explain why, not what
- Write logs in Russian

## Architecture

### Layers

```
UI Layer (Compose + ViewModels)
    ↓
Domain Layer (Use Cases)
    ↓
Data Layer (Repositories + API + Room + DataStore)
```

### Patterns

- MVVM
- Unidirectional Data Flow
- Repository Pattern
- Manual DI via AppContainer (no Hilt)

### AppState

- Global state for navigation and auth
- `isAuthorized = currentUser != null`

### Data Strategy

- Parks, Cities: cache-first with server sync
- Events: online-first
- Journals: offline-first with sync
- Messages: online-first with fallback cache
- Auth: online-only

## Testing

- TDD order: Tests → Logic → UI

### Test naming

```kotlin
@Test
fun functionName_whenCondition_thenExpectedResult()
```

### Libraries

- JUnit 4
- MockK
- kotlinx.coroutines.test
- Turbine
- Robolectric

### Locations

- `app/src/test/java/com/swparks/`
- `app/src/androidTest/java/com/swparks/`

## Key Files

- `app/build.gradle.kts`
- `config/detekt/detekt.yml`
- `Makefile`
- `.cursor/rules/*.mdc`
- `docs/plan-development.md`

## Pre-Commit Checklist

- `make format`
- `make lint`
- `make test`
- No crashes on app launch
- No deprecated API usage
