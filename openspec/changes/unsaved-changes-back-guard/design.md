# Design

## Context

Состояние на момент задачи (проверено по коду):

- `BackHandler` из `androidx.activity.compose` в `app/src/main/java` не используется ни разу; confirm-диалог подключён только к кнопке TopBar: `ParkFormScreen.kt:120–126` (`if (uiState.hasChanges) showConfirmDialog = true else Back`) и `EventFormScreen.kt:121–127` — дословно одинаковые ветки.
- `EditProfileScreen.kt:145` — `onBackClick = { onAction(EditProfileNavigationAction.Back) }` без проверки; при этом `EditProfileUiState.hasChanges` существует (`ui/state/EditProfileUiState.kt:54`, там же `:62` — `canSave` уже через него) и сам ViewModel его использует (`ui/viewmodel/EditProfileViewModel.kt:277,481`).
- `ConfirmCloseDialog` продублирован приватными composable в `ParkFormScreen.kt:435–462` и `EventFormScreen.kt:442–469` — тела идентичны (те же строки `event_form_confirm_close_title/message` + `close`/`cancel`, `strings.xml:440–441,241,201`), различается только способ управления видимостью: ParkForm гоняет его через локальный sealed `ParkFormDialogAction` (`:69–78`, `ParkFormDialogs` :413–433), EventForm держит локальный `showConfirmDialog` (`:101` объявление, call site `:164–172`).
- `JournalSettingsDialog.kt:76` и `ParksFilterDialog.kt:57` — `onDismissRequest = onDismiss`; крестик в заголовке (`JournalSettingsDialog.kt:142`, `ParksFilterDialog.kt:197–202` — `IconButton` в приватном `DialogTitle`) тоже зовёт `onDismiss` напрямую. Признак правок локальный: `JournalSettingsDialogState.hasChanges` (:110–114) и `ParksFilterDialogState.canApply` (:146 — правки относительно состояния на момент открытия; `isEdited` :143 сравнивает с дефолтами — это признак кнопки «Сбросить», не guard'а).
- Эталон настройки возврата у листа — `PhotoDetailSheetHost.kt` (`shouldDismissOnBackPress = true`: возврат закрывает лист сразу, без подтверждения; задача `sheet-back-gesture`); у `AlertDialog` каналы другие — `onDismissRequest`.

Требования к поведению — в `specs/unsaved-changes-guard/spec.md`.

## Goals / Non-Goals

**Goals:**
- Единый точечный паттерн «BackHandler(enabled = hasChanges) → confirm → реальный выход», применённый в пяти местах без общей прослойки.
- Одна реализация `ConfirmCloseDialog` в `ui/ds/` вместо двух копипаст.
- Правка целиком в UI-слое: ViewModel, use case и навигация не меняются.

**Non-Goals:**
- Черновики/автосохранение ввода (меняет семантику данных, отдельная тема).
- Глобальный перехват возврата в `RootScreen`/навигации — см. решение 1.
- Правка листов `PhotoDetailSheetHost`, `TextEntrySheetHost`, auth-листов (задача `sheet-back-gesture`): поведение `TextEntrySheetHost`/auth-листов зафиксировано спеками `text-entry`/`auth-sheets`, у `PhotoDetailSheetHost` — только кодом, спеки нет.
- `BackHandler` для листов не нужен и отвергнут (`openspec/changes/archive/2026-10-08-sheet-back-gesture/design.md:43` — дублирует встроенный канал `ModalBottomSheet`); для `AlertDialog` он тоже не нужен — канал есть: возврат перехватывает окно диалога и приходит в `onDismissRequest`, который маршрутизируется в guard (решение 4).
- Рефакторинг управления confirm-диалогом в ParkForm с `ParkFormDialogAction` на локальное состояние (как в EventForm) — работает, не трогаем.
- Унификация с `DeleteConfirmDialog` (`ImagePreviewDialog.kt`) и `DeleteProfileDialog` (`EditProfileScreen.kt`) — те же структурно `AlertDialog`, отличаются строками; консолидация только close-диалогов, delete-диалоги — отдельная задача.

## Decisions

### 1. `BackHandler(enabled = hasChanges)` на каждом экране — без общей прослойки

В каждом из трёх экранов форм добавляется `BackHandler(enabled = uiState.hasChanges) { showConfirmDialog = true }` рядом с существующим `Scaffold`; после подтверждения выполняется существующий путь выхода (`ParkFormNavigationAction.Back` / `EventFormNavigationAction.Back` / `EditProfileNavigationAction.Back`). Пока confirm-диалог открыт, повторный возврат попадает в его `onDismissRequest` (= отмена) — диалог подтверждения закрывается, форма остаётся.

Альтернативы:
- **Глобальный перехват в `RootScreen`** — корень не знает про `hasChanges` конкретного destination: пришлось бы тащить состояние форм наверх (глобальный реестр «грязных» экранов) и городить условную логику для пяти разных мест ради одного паттерна из трёх строк. Отвергнуто.
- **Автосохранение вместо подтверждения** — меняет поведение сохранения (частичные/невалидные данные), выходит за рамки «починить потерю ввода». Отвергнуто.
- **Обёртка-компонент `GuardedBack` над BackHandler+диалогом** — BackHandler и показ диалога разнесены по иерархии (диалоги ParkForm живут в `ParkFormDialogs`), обёртка разъезжается с существующей разводкой состояний; три строки на месте вызова дешевле абстракции. Отвергнуто.

### 2. Общий `ui/ds/ConfirmCloseDialog.kt` вместо копипасты

Публичный composable `ConfirmCloseDialog(onDismiss, onConfirm)` с теми же строками переезжает в `app/src/main/java/com/swparks/ui/ds/` (рядом с `ImagePreviewDialog.kt`, `SWDateTimePicker.kt` — там уже живут диалоговые общие компоненты). Приватные копии в Park/EventForm удаляются, вызовы перенаправляются. `onDismissRequest = onDismiss` сохраняется — «тап вне/возврат по confirm-диалогу = отмена». Строки при этом переименовываются: `event_form_confirm_close_title`/`_message` → `confirm_close_title`/`confirm_close_message` (en `strings.xml:440–441`, ru `values-ru/strings.xml:456–457`) — после консолидации их используют пять мест, префикс `event_form_` ошибочен; обновляются ссылки в новом `ConfirmCloseDialog.kt` и в androidTest-ах (`ParkFormScreenTest`, `EventFormScreenTest`).

### 3. EditProfile: TopBar выравнивается + BackHandler

`onBackClick` получает ту же ветку `if (uiState.hasChanges) showCloseConfirm = true else Back` (новый локальный `showCloseConfirm`), BackHandler ставит тот же флаг. Confirm-диалог рендерится рядом с существующим `showDeleteDialog` (:172–180). Оба пути (кнопка и системный возврат) сходятся в один флаг — спека требует одинакового поведения.

### 4. Диалоги: единый `requestClose()` в `onDismissRequest` — без `BackHandler`

Возврат в `AlertDialog` без своего `BackHandler` перехватывает окно диалога (`dismissOnBackPress` включён по умолчанию) и приходит в `onDismissRequest`; тап вне области и крестик заголовка идут туда же. При `hasChanges` показываем confirm-поверх (второй `AlertDialog` поверх — штатный паттерн Dialog-над-Dialog). Локальный `var showCloseConfirm by remember`, и единый обработчик (пример JournalSettingsDialog; у фильтра признак — `state.canApply`):

```
val requestClose = { if (!isSaving) { if (state.hasChanges) showCloseConfirm = true else onDismiss() } }
```

через него маршрутятся `onDismissRequest` (возврат и тап вне) и крестик заголовка — все пути закрытия сходятся в единственную проверку `hasChanges` внутри `requestClose` (без правок он зовёт `onDismiss` — тот же результат, что нативный dismiss). При идущем сохранении (`isSaving`) `requestClose` выходит рано — возврат не показывает confirm поверх запроса и не закрывает диалог (прецедент: спеки `auth-sheets`/`text-entry`); крестик и тап вне маршрутятся через тот же `requestClose`, поэтому во время сохранения они тоже ничего не делают — следствие раннего выхода, осознанно; `enabled` для этого не вводится — единственная точка решения остаётся внутри `requestClose`. Сам confirm-диалог: `onConfirm = { showCloseConfirm = false; onDismiss() }` — подтверждение закрывает оба слоя; его `onDismissRequest = { showCloseConfirm = false }` — возврат/тап вне по confirm только сбрасывает флаг. Тап вне основного диалога при наличии правок тоже попадает в `onDismissRequest → requestClose` — правки не теряются незаметно (в ParkForm/EventForm закрываемой тап-вне поверхности нет — там ничего дополнительно не нужно). При отсутствии правок поведение идентично текущему: `requestClose` → `onDismiss`.

### 5. Видимость confirm-диалога

Новое локальное состояние (`showCloseConfirm`, `remember { mutableStateOf(false) }`) — только в EditProfile и двух диалогах (см. решения 3 и 4); ParkForm/EventForm используют существующие флаги (см. решение 1). Никаких новых состояний в ViewModel.

## Risks / Trade-offs

- **Predictive back (API 34+, AVD API 36)**: активный `BackHandler` отключает системную анимацию предиктивного возврата на защищаемых экранах — осознанный размен: без него возврат закрывал бы форму до того, как guard успевает показать диалог. На безизменённых формах `enabled = false` и анимация остаётся. В диалогах `BackHandler` нет: возврат поглощает окно диалога и приходит в `onDismissRequest → requestClose` — предиктивная анимация диалога не затронута, guard при этом работает.
- **UI-тесты `pressBack` на диалогах** могут быть нестабильны на эмуляторе (опыт `sheet-back-gesture`, риск зафиксирован там же) → при нестабильности сценарий переводится в наблюдаемую ручную проверку — шаблон задач это допускает.
- **`FakeEditProfileViewModel`** пишется с нуля — по образцу `FakeParkFormViewModel`; риск только механический.
- **Крестик диалогов меняет поведение** (раньше закрывал сразу, теперь при правках спрашивает) — это и есть цель спеки («не существует пути мгновенной потери правок»), но в задачу включена ручная проверка обоих диалогов.

## Migration Plan

Не требуется: миграций данных и обратной совместимости нет. Откат — revert коммита; новый файл `ui/ds/ConfirmCloseDialog.kt` и правки пяти экранов самодостаточны.

## Open Questions

Нет.
