# AGENTS.md

Concise guidelines for AI agents working on SW Parks.

## Project

- Android app "SW Parks" (Street Workout Parks), Kotlin + Jetpack Compose, port of the iOS SwiftUI-WorkoutApp.
- Package `com.swparks`, modules `:app` and `:screenshot-tests`.
- SDK and toolchain versions are in `README.md` badges and `gradle/libs.versions.toml`.

## Build, lint, test

```bash
make build          # assembleDebug
make clean          # gradle clean
make lint           # ktlintCheck + app:detekt + markdownlint (if installed)
make format         # ktlintFormat + app:detekt autoCorrect + markdownlint --fix
make test           # unit tests + Python report
make android-test   # :app:connectedDebugAndroidTest (только :app:, без :screenshot-tests)
make check          # build + test + lint
make install        # installDebug
```

- `make lint` only warns if `markdownlint-cli` is missing; install it via `npm install -g markdownlint-cli`.
- **`make emulator-fast`** — отключает анимации эмулятора (ускоряет `make android-test` в ~3.5×). Вызывать **после каждого старта эмулятора**. Откат: `make emulator-slow`.
- Detekt: `config/detekt/detekt.yml`. `maxIssues: 35`, `TooManyFunctions` threshold 20, `LargeClass` excludes `*ViewModel.kt`. Add `@Suppress("TooManyFunctions")` to interfaces when they grow past 20 methods.
- EditorConfig: `.editorconfig`. No wildcard imports, no trailing commas, 4-space indent.

## Focused tests

```bash
./gradlew :app:testDebugUnitTest --tests "com.swparks.domain.usecase.LoginUseCaseTest"
./gradlew :app:testDebugUnitTest --tests "com.swparks.domain.usecase.LoginUseCaseTest.invoke_whenValidCredentials_thenSavesTokenAndCallsLogin"
./gradlew :app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=com.swparks.ui.screens.more.MoreScreenTest
```

- **`--tests "<FQN>"` не работает на AGP 9** для таска `connectedDebugAndroidTest` (Gradle 9.7.1 возвращает `Unknown command-line option '--tests'`). Рабочий фильтр — `-Pandroid.testInstrumentationRunnerArguments.class=<FQN>` (см. пример выше).
- **Канон**: в итерации — фильтр по FQN (как выше); перед коммитом или релизом — полный прогон через `make android-test` (после `make emulator-fast`).

## Architecture

- MVVM, unidirectional data flow, repository pattern, manual DI via `AppContainer` (no Hilt).
- Entry points: `JetpackWorkoutApplication` → `MainActivity` → `AppContainer` → `RootScreen` + `Destinations`.
- Data strategy: Parks/Cities cache-first; Events online-first; Journals offline-first; Messages online-first with fallback; Auth online-only.

## Code conventions

- Import order: AndroidX, Kotlin, third-party, `com.swparks.*`.
- No `!!`; use `checkNotNull`, `?.let`, `?:`.
- Use cases return `Result<T>`. ViewModels use sealed UI state. One-off events use `Channel`.
- Composables and classes are PascalCase; functions/variables camelCase; constants UPPER_SNAKE_CASE.
- Logs and user-facing text in Russian.
- KDoc for public APIs; explain why, not what.

## Testing

Подробности — в навыке `testing` (`.opencode/skills/testing/SKILL.md`).
Ключевое:
- Тесты: `app/src/test/` (unit, JUnit 4) и `app/src/androidTest/` (UI/integration).
- Имена: `functionName_whenCondition_thenExpectedResult()`.
- Android-тесты наследуют `TimeoutTest(N)` (UI: 60 с, Keystore/Room: 180 с) — зависший `@Test` не блокирует прогон.
- Сеть AVD нестабильна — flaky сеть-зависимые тесты лечить сетью или stubs.

## Key files

- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `config/detekt/detekt.yml`
- `.editorconfig`
- `Makefile`
- `docs/plan-development.md`
- `app/src/main/java/com/swparks/JetpackWorkoutApplication.kt`
- `app/src/main/java/com/swparks/MainActivity.kt`
- `app/src/main/java/com/swparks/data/AppContainer.kt`
- `app/src/main/java/com/swparks/navigation/Destinations.kt`

## Repo-local OpenCode skills

`.opencode/skills/` has project-specific skills for Compose tasks: `android-dimens`, `jetpack-compose-safe-areas`, `loading-overlay`, `localization`, `pull-to-refresh`, `testing`.

## Git hooks

- `.githooks/pre-commit` runs `./gradlew updateReadmeVersions` and auto-stages `README.md`.
- Enable with `make setup` (sets `git config core.hooksPath .githooks`).
- If not configured, run `./gradlew updateReadmeVersions` manually before committing.

## ABI splits & UnsatisfiedLinkError

- `splits.abi` включается через флаг `-PenableSplits=true` (передаётся `make apk`); `make release` (`bundleRelease`) запускается без флага, иначе AGP падает на `:app:buildReleasePreBundle` (<https://issuetracker.google.com/402800800>). При включённом флаге релизные APK содержат только `arm64-v8a` и `armeabi-v7a` — x86/x86_64 исключены.
- Debug-сборки (`make build`) универсальны и работают на любой архитектуре.
- MapLibre (`libmaplibre.so`) — основной кандидат на `UnsatisfiedLinkError` при установке релизного APK на эмулятор x86_64.
- Обязательное правило: карту (`ParkMapView`) тестировать только через `make build`/`make install` на arm64-эмуляторе (например, `Pixel 9 Pro API 36`). Релизные APK предназначены для arm64-устройств.
- При появлении crash по `UnsatisfiedLinkError` из любой native-библиотеки — первым делом проверять ABI-состав APK.

## Pre-commit checklist

- `make format`
- `make lint`
- `make test`
- `make build`
- No deprecated API usage
- No crashes on app launch
