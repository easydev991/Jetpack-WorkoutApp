# Рефакторинг навигации: декомпозиция `RootScreen` через coordinators + typed args

## Summary

Цель: распространить успешный паттерн из `UserParks` (navigation coordinator + parser аргументов) на другие навигационные сценарии, где в `RootScreen` сейчас смешаны route-building, `SavedStateHandle`, JSON encode/decode и cleanup.  
Результат: `RootScreen` остается “тонким” (создание VM + рендер), а вся низкоуровневая навигационная логика переносится в `navigation` слой и покрывается unit-тестами.

## Этап 1: Event payload flows (приоритет 1) ✅

- [x] Создать coordinator для перехода в участников события:
  - `navigateToEventParticipants(eventId, source, users)`
  - внутри: route + encode users + запись в `SavedStateHandle`.
- [x] Создать parser аргументов участников:
  - `consumeEventParticipantsArgs()` (decode users + fallback на `emptyList()`).
- [x] Создать coordinator для редактирования события:
  - `navigateToEditEvent(event, source)`
  - внутри: route + encode event + запись в `SavedStateHandle`.
- [x] Создать parser аргументов EditEvent:
  - `consumeEditEventArgs()` (eventId + decoded event, nullable-safe).
- [x] Вынести result-аргументы выбора парка:
  - `setSelectedParkResult(parkId, parkName)` для sender,
  - `consumeSelectedParkResult()` для receiver (Create/Edit Event), с cleanup ключей.
- [x] Обновить `RootScreen`: заменить текущие блоки encode/decode/cleanup на вызовы новых функций.

## Этап 2: Унификация userId/source аргументов (приоритет 2) ✅

- [x] Добавить typed parser для маршрутов с `userId/source`:
  - `consumeUserIdSourceArgs(defaultSource = "...")`.
- [x] Применить parser в `RootScreen` для веток:
  - `UserTrainingParks`, `UserFriends`, `JournalsList`, `OtherUserProfile` (где применимо).
- [x] Для кликов из экранов (переходы на user-based экраны) использовать координаторы/extension-функции вместо ручного `createRoute(...)`, если есть повторяющийся шаблон.

## Этап 3: JournalEntries args parser (приоритет 3)

- [x] Добавить typed parser:
  - `consumeJournalEntriesArgs()` для `journalId`, `journalOwnerId`, `journalTitle`, `viewAccess`, `commentAccess`, `source`.
  - внутри: decode title, дефолты по доступам, null-safe поведение.
- [x] В `RootScreen` заменить ручной разбор query-параметров на parser.
- [x] Зафиксировать единый fallback-подход при битых/неполных аргументах (экран не падает, ветка просто не рендерится или показывает текущий безопасный state по существующему поведению).

## Этап 4: Тесты и критерии завершения

- [x] Unit-тесты navigation coordinators (этап 1):
  - route формируется корректно,
  - payload пишется в `SavedStateHandle` корректно.
- [x] Unit-тесты parsers (этап 1):
  - успешный decode,
  - malformed payload,
  - cleanup ключей после consume.
- [x] Unit-тесты parsers (этап 2-3):
  - дефолты `source`/access,
- [x] Smoke-регрессия сценариев:
  - EventDetail -> Participants,
  - EventDetail -> EditEvent -> BackWithCreatedEvent,
  - SelectPark -> Create/EditEvent result,
  - JournalsList -> JournalEntries.
  - Примечание: подтверждено автопрогонами таргетных unit/navigation/viewmodel тестов; отдельных сквозных androidTest для всей цепочки пока нет.
- [x] Прогон (этап 1):
  - таргетные unit-тесты навигации,
  - `:app:compileDebugKotlin`,
  - `:app:ktlintCheck`.

## Важные изменения интерфейсов

- Новый navigation API (extensions/coordinators + typed args parsers), например:
  - `NavController.navigateToEventParticipants(...)`
  - `NavController.navigateToEditEvent(...)`
  - `NavBackStackEntry.consumeEventParticipantsArgs()`
  - `NavBackStackEntry.consumeEditEventArgs()`
  - `NavBackStackEntry.consumeSelectedParkResult()`
  - `NavBackStackEntry.consumeUserIdSourceArgs()`
  - `NavBackStackEntry.consumeJournalEntriesArgs()`
- `RootScreen` перестает напрямую знать ключи `SavedStateHandle` и JSON-поля этих сценариев.

## Assumptions

- Поведение UX и схемы маршрутов не меняются; меняется только распределение ответственности.
- Существующие ключи `SavedStateHandle` сохраняются ради обратной совместимости внутри текущей ветки.
- Рефактор выполняется последовательно по приоритетам 1 -> 2 -> 3, каждый этап мерджится после прохождения своих тестов.
