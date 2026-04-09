# Android Screenshot Automation

## Назначение

Этот документ описывает фактически реализованную автоматизацию маркетинговых скриншотов Android в проекте `Jetpack-WorkoutApp`.

Автоматизация построена на отдельном instrumentation-модуле `:screenshot-tests` и `fastlane screengrab`, без запуска полного набора `app/src/androidTest`.

## Текущая архитектура

### Модули и роли

- `:app` — основное приложение.
- `:screenshot-tests` — отдельный Android test-модуль для screenshot-сценария.

### Конфигурация Gradle

- В [settings.gradle.kts](/Users/Oleg991/Documents/GitHub/Jetpack-WorkoutApp/settings.gradle.kts) подключен модуль:
  - `include(":screenshot-tests")`
- В [screenshot-tests/build.gradle.kts](/Users/Oleg991/Documents/GitHub/Jetpack-WorkoutApp/screenshot-tests/build.gradle.kts):
  - `targetProjectPath = ":app"`
  - `namespace = "com.swparks.screenshots"`
  - `testInstrumentationRunner = "com.swparks.screenshots.ScreenshotTestRunner"`
  - зависимость `tools.fastlane:screengrab:2.1.1`

### Screenshot runtime

- Runner: [ScreenshotTestRunner.kt](/Users/Oleg991/Documents/GitHub/Jetpack-WorkoutApp/screenshot-tests/src/main/java/com/swparks/screenshots/ScreenshotTestRunner.kt)
- Test Application: [ScreenshotTestApplication.kt](/Users/Oleg991/Documents/GitHub/Jetpack-WorkoutApp/screenshot-tests/src/main/java/com/swparks/screenshots/ScreenshotTestApplication.kt)
- Container с demo-данными: [ScreenshotAppContainer.kt](/Users/Oleg991/Documents/GitHub/Jetpack-WorkoutApp/screenshot-tests/src/main/java/com/swparks/screenshots/ScreenshotAppContainer.kt)
- Сценарное состояние: [ScreenshotScenarioState.kt](/Users/Oleg991/Documents/GitHub/Jetpack-WorkoutApp/screenshot-tests/src/main/java/com/swparks/screenshots/ScreenshotScenarioState.kt)

## Screenshot-сценарий

Основной orchestrator-тест:

- [WorkoutAppScreenshotsTest.kt](/Users/Oleg991/Documents/GitHub/Jetpack-WorkoutApp/screenshot-tests/src/main/java/com/swparks/screenshots/WorkoutAppScreenshotsTest.kt)

Порядок снимков (текущая реализация):

1. `1-parksMap`
2. `2-parksList`
3. `3-parkDetails`
4. `4-pastEvents`
5. `5-eventDetails`
6. `6-profile`

Локаль screenshot-flow: `ru-RU`.

## Fastlane

### Lane

В [fastlane/Fastfile](/Users/Oleg991/Documents/GitHub/Jetpack-WorkoutApp/fastlane/Fastfile) используется lane:

- `screenshots`

### Пути APK

- `app_apk_path`: `app/build/outputs/apk/debug/app-debug.apk`
- `tests_apk_path`: `screenshot-tests/build/outputs/apk/debug/screenshot-tests-debug.apk`

### Screengrabfile

В [fastlane/Screengrabfile](/Users/Oleg991/Documents/GitHub/Jetpack-WorkoutApp/fastlane/Screengrabfile):

- `tests_package_name 'com.swparks.screenshots'`
- `test_instrumentation_runner 'com.swparks.screenshots.ScreenshotTestRunner'`
- `use_tests_in_packages ['com.swparks.screenshots']`
- `locales ['ru-RU']`
- `output_directory '.../fastlane/metadata/android'`

## Makefile

В [Makefile](/Users/Oleg991/Documents/GitHub/Jetpack-WorkoutApp/Makefile):

- цель `screenshots` запускает fastlane lane `screenshots`;
- `_build_screenshots_apk` собирает:
  - `:app:assembleDebug`
  - `:screenshot-tests:assembleDebug`
- очистка APK артефактов идет для:
  - `app/build/outputs/apk`
  - `screenshot-tests/build/outputs/apk`

## Локальный запуск

### Полная генерация скриншотов

```bash
make screenshots
```

### Проверка сборки screenshot-модуля

```bash
./gradlew :screenshot-tests:assembleDebug
```

## Артефакты

- Итоговые PNG: `fastlane/metadata/android/**`
- Test APK screenshot-модуля: `screenshot-tests/build/outputs/apk/debug/screenshot-tests-debug.apk`

## Примечания

- В `.gitignore` игнорируется только `screenshot-tests/build/`.
- Папка `screenshot-tests/` полностью остается в репозитории.
- Исторический документ-план был преобразован в этот документ фактической реализации.
