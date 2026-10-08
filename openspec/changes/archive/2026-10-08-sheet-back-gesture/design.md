# Design

## Context

Текущее состояние `TextEntrySheetHost.kt` (единственный хост экрана ввода текста; используется из 6 мест):

- `onDismissRequest = {}` и `ModalBottomSheetProperties(shouldDismissOnBackPress = false)` — возврат и жест «назад» не делают ничего; закрытие возможно только программно (крестик или событие `Success`).
- `confirmValueChange` пропускает `SheetValue.Hidden` только под флагом `allowHide`, который выставляется на время программного `hide()` — свайп листа вниз закрыть не может.
- `Modifier.disableAllGestures()` на `TextEntryScreen` съедает перетаскивания по контенту.
- `TextEntryEvent.Error` уже отправляется ViewModel'ом, но в хосте обрабатывается пустым веткой `is TextEntryEvent.Error -> {}`. `events` — `Channel<TextEntryEvent>(Channel.BUFFERED)` (`receiveAsFlow`), то есть у события ровно один получатель.
- `TextEntryUiState.error` заполняется при ошибке валидации, но нигде не отображается; `ITextEntryViewModel.onDismissError()` существует и не вызывается.
- Глобальный snackbar (`SnackbarHostState` в `RootScreen.kt:190`, хост — `snackbarHost` корневого `Scaffold`) недостижим из листа: `ModalBottomSheet` отрисовывается в отдельном окне поверх окна приложения, поэтому snackbar оказывается под листом и под затемнением. Ошибка отправки уходит в этот глобальный хост через `userNotifier.handleError` (`TextEntryViewModel.handleResult`) — пользователь её не видит.
- Готовый прецедент в проекте: `PhotoDetailSheetHost.kt` (коммит `ec35157c`, «Возврат назад с экрана просмотра фото (#15)») — тот же класс бага, исправлен через `onDismissRequest` + `shouldDismissOnBackPress = true` + `shouldDismissOnClickOutside = false` + `sheetGesturesEnabled = false`.

Та же проблема в auth-листах:

- `LoginSheetHost.kt:90-100` и `RegisterSheetHost.kt:106-113` — та же проблема (пустой `onDismissRequest`, возврат не закрывает), но конфигурация разная: у `LoginSheetHost` задан только `shouldDismissOnBackPress = false` (`:98`); у `RegisterSheetHost` уже стоят `shouldDismissOnBackPress = false` и `shouldDismissOnClickOutside = false` (`:111-112`) — там баг только в back press. В проекте это единственные оставшиеся `ModalBottomSheet` с таким поведением.
- Guard на выполнение запроса есть только в крестике: `LoginSheetHost.kt:103-106` и `RegisterSheetHost.kt:160` проверяют `uiState.isBusy`; `onDismissRequest` этот guard обходит — при включении возврата guard нужно добавить в новый путь закрытия (`onDismissRequest`), не перенося его в общий `dismiss()`.
- Snackbar-проблемы в auth-листах нет: у регистрации свой `SnackbarHost` внутри листа (`RegisterSheetHost.kt:65` — `snackbarHostState`, отрисовка `166-169`), ошибки логина намеренно показываются инлайн под полем пароля (`LoginViewModel.kt:116`, в `userNotifier` не отправляются).
- VM у хостов неотделима: `LoginSheetHost` берёт её сам через `viewModel<LoginViewModel>(factory)` (хотя `LoginScreen` принимает `ILoginViewModel` и `FakeLoginViewModel` существует), `RegisterSheetHost` — через `appContainer.registerViewModelFactory()` при обязательном параметре `appContainer: DefaultAppContainer`. Интерфейс `IRegisterViewModel` существует (`RegisterViewModel` реализует его, экраны регистрации типизированы им), но фейка `FakeRegisterViewModel` нет.
- `Modifier.disableAllGestures()` стоит на контенте всех трёх хостов.

Требования к поведению — в `specs/text-entry/spec.md` и `specs/auth-sheets/spec.md`.

## Goals / Non-Goals

**Goals:**
- Правка целиком в UI-слое (`TextEntrySheetHost.kt`, `TextEntryScreen.kt`, `LoginSheetHost.kt`, `RegisterSheetHost.kt`), без изменения ViewModel и интерфейсов.
- Тестируемость через существующие `FakeTextEntryViewModel` и `FakeLoginViewModel`, плюс новый `FakeRegisterViewModel` (не через реальный `AppContainer`).

**Non-Goals:**
- Замена модальных листов на полноценные экраны навигации: это меняет способ возврата во всех местах вызова и выходит за рамки «починить баг».
- Изменение текстов ошибок, локализации и аналитики отправки.
- Правка `PhotoDetailSheetHost` — там возврат уже работает (эталон паттерна).

## Decisions

### 1. Жест «назад» — тот же приём, что в `PhotoDetailSheetHost`

`onDismissRequest` вызывает общий обработчик закрытия; в `ModalBottomSheetProperties` — `shouldDismissOnBackPress = true`, `shouldDismissOnClickOutside = false`; отдельный параметр самого `ModalBottomSheet` — `sheetGesturesEnabled = false`. Тап по затемнённой области гейтится исключительно `shouldDismissOnClickOutside` (скрим в material3 — отдельный механизм, на него `sheetGesturesEnabled` не влияет); `sheetGesturesEnabled = false` отключает только перетаскивание листа. Без `shouldDismissOnClickOutside = false` (дефолт `true`) лист закрывался бы тапом вне, что запрещено спекой.

Альтернативы:
- **Ручной `BackHandler`** — дублирует встроенный канал `ModalBottomSheet` и добавляет импорт/lifecycle-обвязку без выигрыша.
- **Оставить свайп листа вниз как способ закрытия** — конфликтует с прокруткой многострочного поля ввода и создаёт второй конфликтующий путь закрытия; отвергнуто, поведение закрытия закреплено сценарием спеки.
- **Отдельный nav-экран** — см. Non-Goals.

### 2. Одна функция закрытия вместо копипасты

Общий `dismiss(onComplete: () -> Unit)` (по образцу уже существующей `dismissSheet(onComplete)` в auth-хостах) делает `focusManager.clearFocus(force = true)` и `keyboardController?.hide()` (как в образце — `LoginSheetHost.kt:79-80`; у text-entry при закрытии возвратом фокус обычно в многострочном поле), затем `allowHide` вокруг `sheetState.hide()` и вызов `onComplete`; `onComplete` у путей разный: возврат и крестик — `onDismissed`, ветка `Success` — `onSendSuccess` / `onLoginSuccess` / `onRegisterSuccess`. Guard занятости (`uiState.isLoading` / `uiState.isBusy`) живёт на путях пользовательского закрытия (`onDismissRequest` и крестик), а не внутри общего `dismiss`: успех и закрытие — разные сценарии и не должны конкурировать за один guard — штатное авто-закрытие по событию Success (обе auth-VM шлют его и переводят uiState в Idle: `LoginViewModel.kt:106→108`, `RegisterViewModel.kt:285→286`) проходит безусловно, без проверки занятости. Для text-entry порядок безопасен (`TextEntryViewModel.kt:71` снимает `isLoading` до `handleResult`), guard оставлен на местах единообразно. Сейчас в `TextEntrySheetHost` этот блок продублирован дважды, а guard в auth-хостах есть только в крестике — без общего `dismiss(onComplete)` возврат обходил бы проверку занятости.

### 3. Snackbar живёт внутри `TextEntryScreen`, а не рядом с листом

`TextEntryScreen` получает необязательный параметр `snackbarHostState: SnackbarHostState?` (по умолчанию `null` → своё `remember`-состояние, поэтому превью и существующие тесты не требуют правок) и прокидывает `SnackbarHost` в свой `Scaffold`.

Альтернативы:
- **Прокинуть глобальный `SnackbarHostState` из `RootScreen`** — невозможно, см. Context.
- **`SnackbarHost` сиблингом внутри `ModalBottomSheet`** — не участвует в `Scaffold` экрана, позицию и insets пришлось бы подбирать вручную; хост в `Scaffold` уже настроен проектом под safe areas.
- **Перенести отображение ошибки в отдельный composable** — лишняя абстракция.

### 4. Ошибка отправки принимается в хосте листа, а не вторым коллектором

`TextEntrySheetHost` остаётся единственным получателем `events` (`Channel`), в ветке `TextEntryEvent.Error` показывает `event.message` в переданном состоянии.

Альтернатива — собирать `events` ещё и в `TextEntryScreen`: `Channel` отдаст событие ровно одному из коллекторов, и `Success` перестанет доходить до хоста (регрессия автоматического закрытия).

### 5. Ошибка валидации берётся из `uiState.error`, без правок ViewModel

`LaunchedEffect(uiState.error)` показывает текст и вызывает уже существующий `onDismissError()`, который сбрасывает поле ошибки (`uiState.error`), не трогая введённый текст. ViewModel не меняется: обе нужные величины (`error`, `onDismissError`) в нём уже есть.

### 6. Хосты `TextEntrySheetHost`, `LoginSheetHost` и `RegisterSheetHost` принимают ViewModel необязательным параметром

Добавляется параметр `viewModel: ITextEntryViewModel? = null` / `viewModel: ILoginViewModel? = null` / `viewModel: IRegisterViewModel? = null`; при `null` используется текущая конструкция (`appContainer.textEntryViewModelFactory(mode)` / `viewModel<LoginViewModel>(factory = LoginViewModel.Factory)` / `appContainer.registerViewModelFactory()`). Конвенция проекта уже такая: `OtherUserProfileScreen` и `ProfileRootScreen` принимают ViewModel параметром, а хосты листов — исключение. Это позволяет прогнать UI-тесты правила возврата на фейковых VM, не поднимая реальный `AppContainer` в тесте.

Особенность `RegisterSheetHost`: интерфейс `IRegisterViewModel` уже существует (реализован в `RegisterViewModel`, экраны типизированы им), но `FakeRegisterViewModel` создаётся в рамках этой задачи, а `appContainer` становится необязательным (`= null`) — иначе тесту нечего передать в обязательный параметр.

Альтернатива — тестировать только экраны без хостов: конфигурация `ModalBottomSheet` (то, что и сломано) при этом осталась бы непроверенной.

### 7. `Modifier.disableAllGestures()` снимается во всех трёх хостах

После `sheetGesturesEnabled = false` модификатор избыточен (лист не реагирует на перетаскивание) и блокирует прокрутку/взаимодействие жестами с контентом (многострочное поле ввода, поля форм). Удаление, а не сохранение.

## Risks / Trade-offs

- **Сообщение об ошибке может показаться дважды** — ViewModel продолжает сообщать об ошибке через `userNotifier.handleError()` (`TextEntryViewModel.kt:152`), откуда она попадает в глобальный `errorFlow`. Пока лист открыт, snackbar `RootScreen` под листом и уходит по таймауту (~4 с); если закрыть лист быстрее, сообщение появится повторно после закрытия. → Принять как есть: это существующее поведение, а устранение требует убрать `userNotifier.handleError` из ветки отказа `TextEntryViewModel.handleResult`, а `handleError` ещё и логирует ошибку — потеря централизованного логирования. Вынесено отдельной задачей при необходимости.
- **Снятие `disableAllGestures`** меняет поведение контента (прокрутка/выделение текста жестами) → проверить вручную, что многострочное поле ввода и поля auth-форм прокручиваются, а свайп листа вниз не закрывает его.
- **Возврат во время запроса игнорируется** — пользователь может не заметить причину; поведение крестика то же, консистентно.
- **UI-тест на `pressBack` для `ModalBottomSheet`** (`TextEntrySheetHostTest`, `LoginSheetHostTest`, `RegisterSheetHostTest`) может быть нестабилен на эмуляторе → при нестабильности проверку перевести в наблюдаемое ручное поведение (шаблон задач это допускает).
- **Запросы не отменяются при закрытии листа** — все три ViewModel запускают их в `viewModelScope` без сохранённого `Job` и cancel-API. Отмена не нужна: guard занятости блокирует все пути закрытия во время запроса (возврат и крестик — через guard на путях пользовательского закрытия, тап вне и свайп вниз — отключены параметрами листа), поэтому отменять при закрытии нечего. Запрос дорабатывает в фоне только при уничтожении владельца (уход с экрана, смерть процесса) — стандартное поведение Android, вне рамок этого изменения.

## Migration Plan

Не требуется: миграций данных и обратной совместимости нет. Откат — revert коммита, файлы самодостаточны.

## Open Questions

Нет. Дублирование сообщения об ошибке сознательно зафиксировано в рисках как известное ограничение.
