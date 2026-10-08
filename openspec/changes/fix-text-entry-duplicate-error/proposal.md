# Proposal

## Why

До изменения sheet-back-gesture пользователь видел ошибку отправки один раз — глобальным snackbar'ом `RootScreen` (вызов `userNotifier.handleError` в `TextEntryViewModel.handleResult()` существует давно). Изменение добавило второй потребитель — snackbar внутри `TextEntrySheetHost` (`TextEntryEvent.Error`), и тот же отказ стал показываться дважды. Дублирование текста ошибки — баг UX, а не задуманное поведение. Изменение продолжает отложенное решение из `archive/2026-10-08-sheet-back-gesture/design.md:84` (там дубль был зафиксирован как известное ограничение с пометкой «вынесено отдельной задачей»).

## What Changes

- В `TextEntryViewModel.handleResult()` в ветке `onFailure` удалить вызов `userNotifier.handleError(AppError.Generic(...))` вместе со ставшим лишним построением `AppError.Generic`. Ошибка отправки уходит только в `_events` (`TextEntryEvent.Error`) и показывается один раз — в snackbar'е листа.
- Обновить KDoc `TextEntrySheetHost`: удалить фразу «Глобальные ошибки через [userNotifier] в ViewModel тоже остаются (Snackbar отображается автоматически в любом экране)» — включая битую KDoc-ссылку `[userNotifier]`; оставить описание snackbar'а листа (ошибки валидации — `uiState.error`, ошибки отправки — `TextEntryEvent.Error`).
- Канал успешного уведомления не меняется: `userNotifier.showInfo(...)` при успехе (только `TextEntryMode.Message`) остаётся как есть.
- Другие экраны/ViewModel, использующие `userNotifier.handleError`, не затрагиваются.

## Capabilities

### New Capabilities

(нет)

### Modified Capabilities

- `text-entry`: требование «Ошибки валидации и отправки отображаются внутри листа» уточняется — уведомление об ошибке отправки показывается ровно один раз (только поверх листа), без дублирующего глобального уведомления.

## Impact

- Код: `app/src/main/java/com/swparks/ui/viewmodel/TextEntryViewModel.kt` (только ветка `onFailure` в `handleResult`), `app/src/main/java/com/swparks/ui/screens/common/TextEntrySheetHost.kt` (KDoc).
- Тесты: `TextEntrySheetHostTest` (androidTest) — существующий тест показа ошибки отправки в snackbar'е листа остаётся зелёным (проверяет листовую сторону через fake-ViewModel); покрытия ветки `onFailure` в `TextEntryViewModelTest` (unit) нет — её проверяет новая задача 2.3.
- Поведенческая зависимость: глобальный `RootScreen` больше не показывает snackbar для ошибок отправки текста — ошибки поверх листа остаются единственным источником (зафиксировано спекой `text-entry`).
- Побочный эффект (принят осознанно): отказ отправки больше не логируется — логирование жило внутри `UserNotifier.handleError` (`logger.e` с throwable), своего логгера у `TextEntryViewModel` нет; отдельный логгер в ViewModel не добавляется.
