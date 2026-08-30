# Оптимизация UI-тестов (androidTest)

> **ARCHIVED 2026-09-26** — план закрыт, A/B подтверждён.
> Сжатая история решений и осознанных отказов.
> Долгоживущие правила — в [`AGENTS.md`](../AGENTS.md).
> Развёрнутые шаги — в `git log` (сквош-коммит «Исправление и оптимизация UI-тестов»).

## Итог

Полный прогон `make android-test` (`./gradlew :app:connectedDebugAndroidTest`,
Pixel 9 Pro arm64): **8m 40s → 2m 33s (−70.6%, −6 мин 8 с)**, без
регрессий.

## Что сделано

- **Этап 1 — защита от зависания**: abstract `TimeoutTest` (29 UI: 60 с,
  2 не-UI Keystore/Room: 180 с) в `app/src/androidTest/.../testing/`. Починен
  `FakeParkDetailViewModel` (см. «Находка» п.1). Прогон больше не виснет навсегда.
- **Этап 2 — `make emulator-fast`**: цели `emulator-fast`/`emulator-slow` в
  `Makefile` (3 `settings put global *_animation_scale`, 0/1) + общий
  `EMULATOR_SERIAL`. A/B на полном suite дал −70.6%.
- **Этап 3 — сужение `make android-test`**: `./gradlew connectedDebugAndroidTest`
  → `./gradlew :app:connectedDebugAndroidTest`. Screengrab-модуль
  `:screenshot-tests` отсечён из QA-цикла; `make screenshots` (fastlane) не
  затронут.
- **Этап 4 — документация**: `AGENTS.md` — `emulator-fast` в Build,
  фильтр-флаг `--tests` → `-Pandroid.testInstrumentationRunnerArguments.class=`
  в Focused tests, канон «фильтр в dev / полный прогон перед коммитом», timeout
  rule и нестабильная сеть AVD (`wpa_supplicant: BEACON_LOSS`) в Testing.

## Находка

1. **`FakeParkDetailViewModel` deadlock** — `MutableSharedFlow()` без буфера →
   `runBlocking { emitEvent }` блокирует до подписки `LaunchedEffect` →
   deadlock. Фикс: `extraBufferCapacity = 1` + не-suspend `tryEmit`. **Это и был
   виновник** зависания suite >14 мин. Прод `ParkDetailViewModel.kt:150` не
   трогаем.
2. **`--tests "<FQN>"` НЕ работает на AGP 9** (Gradle 9.7.1 → `Unknown
   command-line option`). Рабочий фильтр —
   `-Pandroid.testInstrumentationRunnerArguments.class=<FQN>`. Зафиксировано в
   `AGENTS.md`.
3. **`connectedDebugAndroidTest` без `:app:`** запускает оба модуля
   (`:app:` + `:screenshot-tests:`). Screengrab-тесты не должны гоняться вне
   Screengrab-пайплайна.

## Не делаем (осознанно)

- **Шардирование на 2 AVD** — один AVD в распоряжении, после Этапа 2 wall
  2:33 — в ~4× ниже порога 10 мин, возврат не нужен.
- **Test Orchestrator / `clearPackageData`** — даёт чистоту состояния, но
  замедляет прогон; противоречит цели.
- **`testOptions { animationsDisabled = true }`** — влияет только на
  Animator-based, не на Compose; дешевле `settings put`.
- **Параллельный executor внутри одного AVD** — гонки ломают детерминизм Compose.
- **`org.gradle.workers.max` вручную** — не наш рычаг для androidTest.
- **Удаление/группировка тестов ради скорости** — потеря покрытия запрещена.
- **`TestAppContainer`** — UI-тесты экранов работают с реальным
  `MainActivity` + реальным контейнером приложения. Изоляция — через 9
  `Fake*ViewModel` в `app/src/androidTest/ui/viewmodel/` на уровне экрана (DI
  через Compose-параметры).
- **`-Pandroid.testInstrumentationRunnerArguments.timeout_msec`** — параметр
  `Deprecated` на `androidx.test:runner 1.7.0`, требует другой runner.
  Идём через JUnit-rule (`TimeoutTest`).

## Файлы изменены

- `Makefile` — `emulator-fast`/`emulator-slow`, `EMULATOR_SERIAL`,
  `:app:connectedDebugAndroidTest` (Этапы 2–3).
- `app/build.gradle.kts` — без `androidTestImplementation(libs.junit)`
  (транзитивно через `androidx.test.ext:junit:1.3.0`).
- `app/src/androidTest/java/com/swparks/testing/TimeoutTest.kt` — abstract
  base, конструкторский параметр `seconds: Long = 60`.
- 31 androidTest-класс — `: TimeoutTest()` или `: TimeoutTest(180)`.
- `app/src/androidTest/.../ParkDetailScreenTest.kt:955` —
  `MutableSharedFlow(extraBufferCapacity = 1)`, `tryEmit` без `runBlocking`.
- `AGENTS.md` — 4 правки (см. Этап 4).
