# Design

## Context

Мотивация — в proposal.md (Why). Текущее состояние кода:

- `OtherUserProfileScaffold` (`app/src/main/java/com/swparks/ui/screens/profile/OtherUserProfileScreen.kt`)
  безусловно рендерит `OtherUserProfileTopAppBar`, у которого слот `actions` всегда
  содержит `IconButton` блокировки (иконки `Block`/`CheckCircle`).
- Состояния экрана: `OtherUserProfileUiState.Loading / UserNotFound / BlockedByUser /
  Success / Error`. `viewedUser` загружен только к моменту `Success`.
- `OtherUserProfileViewModel.performBlacklistAction()` при `viewedUser == null`
  завершается no-op — в остальных состояниях кнопка не работает.
- Тесты: `OtherUserProfileScreenTest` (androidTest, `TimeoutTest`, `createComposeRule`)
  тестирует компоненты экрана изолированно; фейк для `IOtherUserProfileViewModel` ещё
  не создан. Полноэкранный паттерн с фейковой VM есть в `ProfileRootScreenTest`; фейк
  определён отдельным файлом:
  `app/src/androidTest/java/com/swparks/ui/viewmodel/FakeProfileViewModel.kt`.
- Спека требования — `specs/other-user-profile/spec.md` (см. Scenario-ы).

## Goals / Non-Goals

**Goals:**

- Поведение верхней панели — по нормам `specs/other-user-profile/spec.md` (оба
  требования), здесь не пересказывается.
- Поведение зафиксировано instrumented-тестами уровня экрана (TDD: red → green).

**Non-Goals:**

- Изменения ViewModel, репозиториев, диалога блокировки и unblock-иконки.
- Скрытие кнопок «написать»/«дружба» — они и так существуют только в Success-контенте.

## Decisions

1. **Видимость вычисляется в `OtherUserProfileScaffold` и передаётся параметром в
   `OtherUserProfileTopAppBar`** (`params.uiState is Success` — `uiState` уже является
   полем `OtherUserProfileScaffoldParams`, нового проброса не требуется).
   Альтернативы: (а) передавать `uiState` в топбар и проверять там — топбар получал бы
   весь стейт и знал лишнее; (б) гейт во ViewModel — это чисто UI-решение, стейт уже в
   экране. Параметр-признак — минимальный дифф, топбар остаётся чистой функцией
   аргументов.
2. **Тесты на уровне экрана с `FakeIOtherUserProfileViewModel`** (по образцу
   `FakeProfileViewModel` из
   `app/src/androidTest/java/com/swparks/ui/viewmodel/FakeProfileViewModel.kt`,
   используемого в `ProfileRootScreenTest`); новый фейк объявляется inline в
   `OtherUserProfileScreenTest.kt` (не отдельным `FakeIOtherUserProfileViewModel.kt`) —
   задача узкая, 4 метода-заглушки, отдельный файл не оправдан.
   Альтернатива: сделать `OtherUserProfileTopAppBar` internal и тестировать параметр —
   такой тест проверяет только проброс булева флага, а не пользовательское требование
   «в ошибке кнопки нет». Фейк ~8 flow-полей + 4 метода-заглушки, окупается двумя
   поведенческими тестами.
3. **TDD-порядок**: сначала тест Error (падает) и тест Success (проходит, контроль), затем
   реализация. Прогон точечный — через
   `-Pandroid.testInstrumentationRunnerArguments.class=...OtherUserProfileScreenTest`
   (`--tests` на AGP 9 для connected-тасков не работает, см. AGENTS.md).

## Risks / Trade-offs

- [Guard «свой профиль» в `OtherUserProfileScreen` (навигация при
  `viewedUser.id == currentUser.id`)] → в тесте Success задавать `viewedUser` с id,
  отличным от `currentUser.id` (достаточно `currentUser = null`).
- [`relations.isInBlacklist` в не-Success состояниях всегда `false` (нет `viewedUser`)] →
  тест проверяет `contentDescription = R.string.block`, а не `unblock`, — стабильно.
- [Точечный прогон не покрывает остальные UI-тесты] → перед коммитом полный прогон
  `make android-test` — канон проекта из AGENTS.md.
- [Спека требует отсутствия кнопки в 4 не-Success состояниях, тест покрывает только
  Error] → Error выбран как представитель всех не-Success состояний
  (Loading/UserNotFound/BlockedByUser): реализация единая — `uiState is Success`,
  отдельные тесты на каждое состояние избыточны.
- [Flaky-сеть эмулятора] → тесты полностью оффлайн (фейковая VM), сетевых зависимостей нет.

Миграция не требуется: изменение UI-локально, откат — revert коммита.
