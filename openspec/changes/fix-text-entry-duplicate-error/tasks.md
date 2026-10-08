# Tasks

## 1. Устранение дублирования ошибки

- [x] 1.1 В `app/src/main/java/com/swparks/ui/viewmodel/TextEntryViewModel.kt` в ветке `onFailure` функции `handleResult()` удалить вызов `userNotifier.handleError(appError)`, построение `AppError.Generic` и ставший неиспользуемым импорт `com.swparks.util.AppError` (иначе падает `make lint`: detekt `UnusedImports` + ktlint `no-unused-imports`); отправка `TextEntryEvent.Error(errorMessage)` в `_events` остаётся. Проверка: `./gradlew :app:compileDebugKotlin` (единственность отправки события проверяет задача 2.3).

- [x] 1.2 В KDoc `TextEntrySheetHost` (`app/src/main/java/com/swparks/ui/screens/common/TextEntrySheetHost.kt`, строки 53–54) удалить фразу «Глобальные ошибки через [userNotifier] в ViewModel тоже остаются (Snackbar отображается автоматически в любом экране)» — включая битую KDoc-ссылку `[userNotifier]` (такого символа в области `TextEntrySheetHost` нет); оставить описание snackbar'а листа: ошибки валидации — `uiState.error`, ошибки отправки — `TextEntryEvent.Error`. Проверка: текст KDoc не упоминает глобальные ошибки.

## 2. Тесты и проверка

- [x] 2.1 Прогнать `TextEntryViewModelTest` (unit) — существующие 21 тест не покрывают ветку `onFailure` и должны остаться зелёными (регрессий от правки 1.1 нет); поведение самой ветки проверяет задача 2.3. Проверка: `./gradlew :app:testDebugUnitTest --tests "com.swparks.ui.viewmodel.TextEntryViewModelTest"`.

- [x] 2.2 Прогнать `TextEntrySheetHostTest` (androidTest) — существующий тест `textEntrySheetHost_onSendError_thenShowsSnackbarAndKeepsSheetAndText` (snackbar листа при ошибке отправки) остаётся зелёным. Проверка: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.swparks.ui.screens.common.TextEntrySheetHostTest`.

- [x] 2.3 Добавить unit-тест в `TextEntryViewModelTest` на ветку `onFailure` (сценарий «Ошибка отправки видна ровно один раз»): застабить `textEntryUseCase` → `Result.failure(...)`, вызвать `onSend()`, проверить `events.first() is TextEntryEvent.Error` и `verify(exactly = 0) { userNotifier.handleError(any()) }` (моки уже есть в тест-классе, заготовка строки ошибки — в setup). Проверка: `./gradlew :app:testDebugUnitTest --tests "com.swparks.ui.viewmodel.TextEntryViewModelTest"`.

- [x] 2.4 Полная проверка перед коммитом (чеклист AGENTS.md): `make format && make lint && make test && make build` зелёные, новые предупреждения Detekt отсутствуют; перед коммитом — полный прогон `make android-test` (после старта AVD выполнить `make emulator-fast`). Проверка: команды завершаются без ошибок.
