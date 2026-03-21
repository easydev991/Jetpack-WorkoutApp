## Документ плана (черновик для `docs/screens/plan-bottom-nav-source-stability.md`)

# План: стабильность BottomNavigation для source-driven навигации (TDD)

## Цель

Гарантировать инвариант навигации: выбранная вкладка `BottomNavigation` не меняется на дочерних экранах без явной команды переключения вкладки.  
Контекст вкладки определяется через `source` и должен сохраняться сквозь переходы.

## Инвариант

- Если переход выполнен на дочерний экран (`parentTab != null`), активная вкладка должна соответствовать `source` текущего navigation-контекста.
- Смена вкладки допустима только при явном переходе на top-level маршрут (`parks/events/messages/profile/more`) через действие bottom bar.

---

## Этап 1 (GREEN ✅): unit-тесты AppState на сохранение вкладки

- [x] Добавить тесты в `AppStateTest`:
  - [x] `onDestinationChanged_whenUserParksFromProfile_thenProfileTabIsSelected`
  - [x] `onDestinationChanged_whenUserParksFromParks_thenParksTabIsSelected`
  - [x] `onDestinationChanged_whenUserTrainingParksFromProfile_thenProfileTabIsSelected`
  - [x] `onDestinationChanged_whenJournalsListFromProfile_thenProfileTabIsSelected`
  - [x] `onDestinationChanged_whenJournalsListFromEvents_thenEventsTabIsSelected`
  - [x] `onDestinationChanged_whenJournalEntriesFromProfile_thenProfileTabIsSelected`
  - [x] `onDestinationChanged_whenJournalEntriesFromMessages_thenMessagesTabIsSelected`
- [x] Добавить цепочные сценарии:
  - [x] `profile -> user_training_parks(profile) -> park_detail(profile) -> park_trainees(profile) -> other_user_profile(profile)` сохраняет `PROFILE`
  - [x] `parks -> park_detail(parks) -> park_trainees(parks) -> other_user_profile(parks)` сохраняет `PARKS`

Критерий завершения: ✅
- Все тесты проходят (инвариант уже реализован в `Screen.findParentTab()` и `AppState.onDestinationChanged()`).

---

## Этап 2 (GREEN ✅): unit-тесты source→parentTab/route для целевых экранов

- [x] Расширить `DestinationsTest` проверками для:
  - [x] `user_training_parks`
  - [x] `user_parks` (ParksAddedByUser flow)
  - [x] `journals_list`
  - [x] `journal_entries`
  - [x] `other_user_profile`
- [x] Добавить негативные кейсы:
  - [x] неизвестный `source` => корректный `default` для каждого из экранов
- [x] В `UserParksNavigationCoordinatorTest` добавить проверку:
  - [x] route всегда содержит исходный `source` без подмены/нормализации

Критерий завершения: ✅
- Маппинг `source` консистентен на уровне маршрутов/координаторов.

---

## Этап 3 (GREEN ✅): минимальные правки логики навигации

- [x] ~~Внести минимальные изменения в навигационные entry points~~ — не требуется
  - Инфраструктура `Screen.findParentTab()` и `getScreenBySource()` уже корректно обрабатывает source.
  - `AppState.onDestinationChanged()` уже принимает arguments и использует их для определения parentTab.

Критерий завершения: ✅
- Все unit-тесты из этапов 1–2 проходят.

---

## Этап 4 (GREEN ✅): integration-тесты цепочек RootScreen

- [x] Добавить инструментальные тесты (новый класс `RootScreenBottomNavSourceFlowTest`):
  - [x] `profileFlow_whenNavigatingToUserParks_thenProfileTabIsSelected`
  - [x] `profileFlow_whenNavigatingToParkDetail_thenProfileTabIsSelected`
  - [x] `profileFlow_whenNavigatingChain_thenProfileTabIsPreserved`
  - [x] `eventsFlow_whenNavigatingToJournalsList_thenEventsTabIsSelected`
  - [x] `eventsFlow_whenNavigatingToJournalEntries_thenEventsTabIsSelected`
  - [x] `eventsFlow_whenNavigatingJournalsChain_thenEventsTabIsPreserved`
  - [x] `parksFlow_whenNavigatingToParkDetail_thenParksTabIsSelected`
  - [x] `messagesFlow_whenNavigatingToOtherUserProfile_thenMessagesTabIsSelected`
  - [x] `complexChain_profileToUserParksToParkDetailToOtherUserProfile_preservesProfileTab`
- [x] Проверка смотрит на реально выбранный item BottomNavigation через `assertIsSelected()`.

Критерий завершения: ✅
- 9 инструментальных тестов проходят, доказывая инвариант для реальных переходов.

---

## Этап 5 (REFACTOR): стабилизация тестовой инфраструктуры

- [ ] Вынести helper'ы:
  - [ ] `bundleWithSource(source)`
  - [ ] helper для цепочного `onDestinationChanged` + assert выбранной вкладки
- [ ] Убрать дубли в тестах, оставить читаемые сценарии.

Критерий завершения:
- Тесты компактны, без дублирования, легко расширяются новыми маршрутами.

---

## Запуск проверок

- [x] `./gradlew :app:testDebugUnitTest --tests "com.swparks.navigation.AppStateTest" --tests "com.swparks.navigation.DestinationsTest" --tests "com.swparks.navigation.UserParksNavigationCoordinatorTest"` ✅
- [x] `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.swparks.ui.screens.RootScreenBottomNavSourceFlowTest` ✅ (9/9 tests passed)

---

## Допущения

- `source` — это контекст происхождения вкладки, а не тип сущности экрана.
- Legacy-значения (например `park`) обрабатываются как совместимость, но канонические значения остаются `parks/events/messages/profile/more`.
- Явной командой смены вкладки считается только действие top-level навигации (bottom bar).

---

## Статус

**Этапы 1-4 завершены.** Unit-тесты и инструментальные тесты подтверждают корректность source-driven навигации.

**Далее:** Этап 5 (рефакторинг тестовой инфраструктуры) - опционально.
