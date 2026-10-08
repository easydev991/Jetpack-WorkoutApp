# Proposal

## Why

Экраны форм и диалоги с редактируемым состоянием теряют несохранённый ввод при закрытии системной кнопкой/жестом «назад»: confirm-диалог проверяется только на пути кнопки «назад» в TopBar (ParkForm, EventForm), на EditProfile проверки нет вовсе, а диалоги (JournalSettingsDialog, ParksFilterDialog) закрываются по возврату без подтверждения. `BackHandler` в проекте не используется ни разу — системный возврат обходит все существующие проверки.

## What Changes

- На трёх экранах форм (ParkForm, EventForm, EditProfile) системный возврат при наличии несохранённых изменений (`hasChanges`) перехватывается `androidx.activity.compose.BackHandler` и показывает существующий confirm-диалог («Закрыть?» / «Несохраненные изменения будут потеряны»); после подтверждения выполняется реальный выход. Без изменений возврат работает как раньше.
- `ConfirmCloseDialog` выносится из `ParkFormScreen.kt` и `EventFormScreen.kt` (дословная копипаста) в общий `app/src/main/java/com/swparks/ui/ds/` — одна реализация для всех пяти мест; EditProfile получает её бесплатно.
- EditProfile: кнопка «назад» в TopBar выравнивается с общим паттерном (сейчас не проверяет `hasChanges`) + перехват системного возврата.
- JournalSettingsDialog и ParksFilterDialog: перехват системного возврата тем же BackHandler-механизмом; в диалогах — без `enabled`, проверка внутри единого локального `requestClose()` (design, решение 4); кнопка закрытия (крестик) и `onDismissRequest` маршрутятся через этот обработчик, чтобы крестик тоже не терял ввод незаметно. Плюс фикс null-сравнения в локальном `hasChanges` (все три слагаемых нормализуются к значениям инициализации) — иначе при nullable-полях дневника guard требует подтверждения без правок.

ViewModel и use case не меняются: `hasChanges` уже существует во всех трёх UI-состояниях (`ui/state/ParkFormUiState.kt:16`, `ui/state/EventFormUiState.kt:15`, `ui/state/EditProfileUiState.kt:54`), для двух диалогов он локальный.

## Capabilities

### New Capabilities

- `unsaved-changes-guard`: защита несохранённых изменений при закрытии форм и диалогов системной кнопкой/жестом «назад» (и крестиком диалогов) — confirm-диалог перед потерей ввода.

### Modified Capabilities

- Нет. Листы из `auth-sheets`/`text-entry` и `PhotoDetailSheetHost` не затрагиваются (смежная задача `sheet-back-gesture` заархивирована — она учила листы закрываться по возврату; здесь обратная проблема: формы и диалоги закрываются возвратом без спроса).

## Impact

- Код: `app/src/main/java/com/swparks/ui/screens/parks/ParkFormScreen.kt`, `app/src/main/java/com/swparks/ui/screens/events/EventFormScreen.kt`, `app/src/main/java/com/swparks/ui/screens/profile/EditProfileScreen.kt`, `app/src/main/java/com/swparks/ui/screens/journals/JournalSettingsDialog.kt`, `app/src/main/java/com/swparks/ui/screens/parks/ParksFilterDialog.kt` (детали и якоря строк — design.md Context), новый общий `app/src/main/java/com/swparks/ui/ds/ConfirmCloseDialog.kt`.
- Тесты: существующие `ParkFormScreenTest`, `EventFormScreenTest`, `JournalSettingsDialogTest`, `ParksFilterDialogTest` (androidTest) расширяются сценариями возврата; новый `EditProfileScreenTest` + `FakeEditProfileViewModel` (UI-теста у экрана нет).
- Строковые ресурсы не добавляются: существующая пара `event_form_confirm_close_*` (`strings.xml:440–441`) переезжает в общий диалог и переименовывается в `confirm_close_title`/`confirm_close_message` (обоснование — design, решение 2).
- Публичные API, ViewModel, use case, навигация — без изменений.
