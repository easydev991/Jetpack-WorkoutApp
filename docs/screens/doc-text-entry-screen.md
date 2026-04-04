# TextEntryScreen

## Статус

`TextEntryScreen` и `TextEntrySheetHost` реализованы и используются в нескольких сценариях приложения. Это уже не план, а актуальное описание текущего поведения.

## Назначение

`TextEntryScreen` — универсальный экран ввода текста, который открывается как `ModalBottomSheet` через `TextEntrySheetHost`.

Экран используется для:

- создания комментариев;
- редактирования комментариев;
- создания дневников и записей в дневнике;
- редактирования записей;
- отправки личного сообщения пользователю.

## Поддерживаемые режимы `TextEntryMode`

Сейчас в коде поддерживаются 8 режимов:

- `NewForPark`
- `NewForEvent`
- `NewForJournal`
- `NewJournal`
- `EditPark`
- `EditEvent`
- `EditJournalEntry`
- `Message`

## Как открывается экран

`TextEntryScreen` не имеет собственного navigation route. Он показывается локально внутри экранов-хостов через `TextEntrySheetHost`.

Текущее подтверждённое использование:

- `ParkDetailScreen`
- `EventDetailScreen`
- `JournalEntriesScreen`
- `JournalsListScreen`
- `OtherUserProfileScreen`

## Поведение `TextEntrySheetHost`

- используется `ModalBottomSheet`;
- закрытие по свайпу, тапу вне sheet и системной кнопке/жесту назад запрещено;
- закрытие по кнопке X разрешено только когда `uiState.isLoading == false`;
- после успешной отправки sheet автоматически скрывается и вызывает `onSendSuccess()`;
- ошибки идут через `UserNotifier`, а показ сообщения остаётся на уровне `RootScreen`.

## Архитектура

```text
TextEntrySheetHost
    -> TextEntryScreen
    -> TextEntryViewModel
    -> TextEntryUseCase
    -> SWRepository
```

Ключевые части реализации:

- `TextEntryMode` описывает сценарий и влияет на заголовок, placeholder и правила валидации;
- `TextEntryViewModel` управляет `TextEntryUiState` и эмитит `TextEntryEvent`;
- `TextEntrySheetHost` создаёт ViewModel через `AppContainer` и управляет жизненным циклом sheet;
- `TextEntryScreen` отвечает только за UI.

## Валидация и UX

- запрос перед отправкой берётся из состояния ViewModel;
- для новых записей и комментариев нельзя отправить пустой текст;
- для edit-сценариев текст должен отличаться от исходного значения;
- во время отправки UI блокируется;
- заголовок и placeholder зависят от `TextEntryMode`.

## Тесты

Подтверждённые тестовые файлы:

- `app/src/test/java/com/swparks/ui/viewmodel/TextEntryViewModelTest.kt`
- `app/src/androidTest/java/com/swparks/ui/screens/common/TextEntryScreenTest.kt`

## Связанные документы

- `docs/screens/doc-journals-list-screen.md`
- `docs/screens/doc-journal-entries-screen.md`
- `docs/screens/doc-park-detail-screen.md`
- `docs/screens/doc-event-detail-screen.md`
- `docs/screens/doc-other-user-profile-screen.md`
