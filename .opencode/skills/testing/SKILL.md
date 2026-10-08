---
name: testing
description: >
  Экспертное руководство по тестированию в Android-проекте SW Parks
  (Jetpack-WorkoutApp): unit-тесты в app/src/test/ (JUnit 4, MockK,
  kotlinx-coroutines-test, Turbine, Robolectric) и инструментированные
  тесты в app/src/androidTest/ (JUnit 4, Compose Testing, Room in-memory,
  Fake*ViewModel, TimeoutTest для per-test таймаута). Использовать при
  написании новых тестов, рефакторинге существующих, отладке flaky-тестов
  и улучшении покрытия в проекте.
---

# Testing — testing skill

## Overview

Два типа тестов, разные директории и стеки:

- **Unit-тесты в `app/src/test/`** — JVM, без устройства. Стек:
  **JUnit 4** (`org.junit.*`), **MockK** (`io.mockk.*`),
  **kotlinx-coroutines-test** (`runTest`, `UnconfinedTestDispatcher`
  в `MainDispatcherRule`), **Turbine** для Flow/StateFlow, **Robolectric**
  для Android `Context`/`Resources`. Цель — быстрые изолированные
  тесты бизнес-логики (Use Cases, ViewModel, репозитории, мапперы,
  валидаторы). Запуск: `make test`.
- **Инструментированные тесты в `app/src/androidTest/`** — требуют
  устройство/эмулятор. Стек: **JUnit 4**, **Compose Testing** (`createComposeRule()` — тип
  правила в `references/compose-ui-testing.md`, «Два типа Compose-правил»),
  **Room in-memory**, **Fake*ViewModel** в
  `ui/viewmodel/`, **`TimeoutTest(N)`** для
  per-test таймаута. Цель — критичные UI-сценарии (ScreenTest +
  `Fake*ViewModel`) и DAO с реальным Room. Запуск: `make android-test`.

> **TDD-порядок**: tests → logic → UI → подробнее в
> `references/fundamentals.md` (раздел «Порядок работы»).

## Agent behavior contract (следуй этим правилам)

> Контракт — это **указатель на `references/`**, не пересказ. Чеклист —
> раздел «Verification checklist» внизу.
>
> Межфайловые ссылки из `references/*.md` пишутся с префиксом
> `references/` (например, `references/fundamentals.md`) — они
> резолвятся от корня навыка `.opencode/skills/testing/`. Пути без
> префикса `references/` (`AGENTS.md`, `Makefile`, `docs/…`)
> резолвятся от корня репозитория.

### Общие (unit + androidTest)

1. **JUnit 4 во всём проекте** → `references/fundamentals.md`.
2. **Имена тестов**: `functionName_whenCondition_thenExpectedResult()`;
   класс оканчивается на `Test` (или `IntegrationTest`) →
   `references/fundamentals.md`.
3. **Given / When / Then** + сообщения `assert` на русском →
   `references/fundamentals.md`.
4. **Никакого `!!`** → `references/fundamentals.md`.
5. **Файлы зеркалят структуру `app/src/main/`** → `references/fundamentals.md`.

### Unit (app/src/test/)

6. **ViewModel — только unit-тест.** Integration-тесты ViewModel в
   `androidTest/` запрещены → `references/viewmodel-testing.md`.
7. **MockK vs Fake** (stateless — MockK, репозитории с Flow — Fake) →
   `references/fakes-and-stateflow.md` (Bottom line).
8. **`coEvery`/`coVerify` — suspend; `every`/`verify` — не-suspend,
   включая геттеры Flow-свойств; Flow-методы репозитория — по правилу 7
   (Fake) → `references/mocking-mockk.md`.
9. **`MainDispatcherRule`** в каждом ViewModel-тесте +
   `advanceUntilIdle()` после каждого действия →
   `references/viewmodel-testing.md`.
10. **Turbine** для `StateFlow`/`SharedFlow` — только в
    `app/src/test/` (в androidTest 0 файлов) →
    `references/viewmodel-testing.md`.
11. **Use Case возвращают `Result<T>`** → `references/use-cases.md`.
12. **Robolectric** для unit-тестов с Android `Context`/`Resources` →
    `references/fundamentals.md` (общий вид) + `references/room-testing.md`
    (DAO-каркас «Каркас 2», `@Config(sdk=[33])` + `allowMainThreadQueries`).

### androidTest (app/src/androidTest/)

13. **`TimeoutTest(N)` — базовый класс** (UI: 60 с, Keystore/Room:
    180 с) → `references/room-testing.md` (раздел «Каркас 1»).
14. **Все androidTest-классы несут `@RunWith(AndroidJUnit4::class)`**
    (включая компонентные). Исключение обосновать в коде.
15. **ComposeTestRule** + один `setContent` на тест (повторный
    `setContent` бросает `IllegalStateException`; смена экрана —
    через `mutableStateOf` внутри первого `setContent`) →
    `references/compose-ui-testing.md`.
16. **`Fake*ViewModel`** в `ui/viewmodel/` для изоляции экрана (при
    изменении реального VM — синхронизируй Fake) →
    `references/fakes-and-stateflow.md` (раздел «Fake*ViewModel в androidTest»).
    Выбор Mock vs Fake — `Bottom line` там же.
17. **Room in-memory** для DAO → `references/room-testing.md`.
18. **`waitUntil`/`waitForIdle` вместо `Thread.sleep`** →
    `references/compose-ui-testing.md`.
19. **Строки UI через ресурсы** (`context.getString(R.string.xxx)`)
    → `references/compose-ui-testing.md`.
20. **Поиск нод в merged-контейнерах** — `useUnmergedTree = true` →
    `references/compose-ui-testing.md`.

## First 60 seconds (triage template)

Прежде чем писать код, собери факты:

- **Цель**: новые unit-тесты / androidTest, flaky failures, новый
  ViewModel, новый Use Case, новый экран, миграция с другого подхода?
- **Факты**:
  - Слой: domain model / Use Case / ViewModel / Repository / DAO /
    Provider / Mapper?
  - Flow / StateFlow / SharedFlow / suspend? Меняет диспетчер и выбор
    Fake vs Mock.
  - I/O: Room DAO, DataStore, Android Context, Clock, system time?
    Меняет Fake vs Mock vs Robolectric vs Room in-memory.
  - Unit-уровень или нужен реальный Compose-стек? Если Compose —
    выбор правила: `references/compose-ui-testing.md` («Два типа Compose-правил»).
  - TDD (пишем до логики) или покрытие существующего кода?
  - Есть ли `Fake*ViewModel` для целевого экрана в
    `app/src/androidTest/.../ui/viewmodel/`? Если нет — нужно создать.

## Common pitfalls → next best move

| Проблема | Решение |
|---|---|
| Unit-тест ViewModel зависает навсегда | Не `runBlocking` с `viewModelScope.launch`. `runTest` + `MainDispatcherRule` + `advanceUntilIdle()` |
| `coEvery { repo.method() }` не компилируется | Метод не suspend — нужен `every` |
| `flowOf(...)` от `mockk()` не доезжает до UI state | Заменить `mockk()` для репозитория на Fake на `MutableStateFlow` |
| `StateFlow` с `WhileSubscribed`: без подписчика upstream не стартует — тест не видит эмиссий | Подписаться до `advanceUntilIdle()`: `backgroundScope.launch { flow.collect { } }` — `references/viewmodel-testing.md` («StateFlow с WhileSubscribed») |
| `composeTestRule.onNodeWithText("…")` не находит | Используй `context.getString(R.string.xxx)` — строки в ресурсах; проверь `setContent` (тема — если компонент её требует) |
| Данные вставил, а UI их не видит | `composeTestRule.waitForIdle()` после `runBlocking { db.insert(...) }` — `references/compose-ui-testing.md` («Ожидание данных») |
| `onAllNodesWithText` без проверки количества | → `references/compose-ui-testing.md` (Селекторы: обязательно `assertCountEquals(n)`) |
| `performClick` на disabled-ноде падает | → `references/compose-ui-testing.md` (Действия: `assertIsNotEnabled`) |
| Второй `setContent` бросает `IllegalStateException` | → `references/compose-ui-testing.md` («Два типа Compose-правил») |
| `assertTrue(x is Y)` без проверки sealed | Сначала `assertTrue(state is DetailScreenState.Success)`, потом `as` — иначе ClassCastException |
| `MutableSharedFlow()` без буфера + suspend `emit()` до подписки → deadlock | Либо `extraBufferCapacity = 1` / `replay = 1`, либо не-suspend `tryEmit`. Подробности — `references/fakes-and-stateflow.md` (раздел «Буфер MutableSharedFlow») |
| Room-тест падает «cannot access database on the main thread» | DAO вызывать через `runTest` (или `runBlocking` в Robolectric). Флаг `.allowMainThreadQueries()` нужен только в Robolectric-DAO в `test/.../database/dao/` (ParkDaoTest, UserTrainingParkDaoTest); в androidTest `JournalEntryDaoTest` работает без флага |
| Mockito в импортах | Только MockK |
| Прогон `make android-test` зависает >10 мин или идёт втрое дольше обычного | Масштабы анимаций слетели: `android-test` чинит их сам (prereq `_ensure_animations_off`), при прямом gradle-вызове — сначала `make emulator-fast` (сам сверяет read-back). Диагностика и механизм — `references/running-tests.md` («Масштабы анимаций: авто-защита от дрейфа»). Если и с ними висит — проверь `TimeoutTest(N)` (правило 13) |
| `--tests "<FQN>"` для androidTest: `Unknown command-line option` | AGP 9 не поддерживает — `references/running-tests.md` (раздел «Фильтр по одному классу») |
| Тест flaky из-за сети | → `references/compose-ui-testing.md` («Сетевые тесты») |

## Запуск тестов

Краткий рецепт — `references/running-tests.md`; канон запуска
(фильтр в dev / полный прогон перед коммитом) — `AGENTS.md`, секция
«Focused tests».

## Routing map

- Анатомия теста, именование, Given/When/Then, `!!`-запрет,
  Robolectric, TDD-порядок → `references/fundamentals.md`
- MockK: `every`/`coEvery`/`verify`/`coVerify`, `relaxed`, `slot`,
  `throws`, `mockkStatic(Log)`/`unmockkAll()` → `references/mocking-mockk.md`
- Fake на `MutableStateFlow`, `Fake*ViewModel` в androidTest,
  `Fake*Repository` в unit, буфер `MutableSharedFlow` →
  `references/fakes-and-stateflow.md`
- ViewModel: `MainDispatcherRule`, `advanceUntilIdle`,
  `WhileSubscribed`, Turbine (только `test/`), `SavedStateHandle` →
  `references/viewmodel-testing.md`
- Use Case: `Result<T>`, паттерны assert, `Result.getOrNull()` →
  `references/use-cases.md`
- Room in-memory DAO (androidTest) + Robolectric DAO (test): каркас,
  Flow, `database.close()`, `TimeoutTest(180)` →
  `references/room-testing.md`
- Compose UI: селекторы, `setContent`, `waitForIdle`, merged-tree,
  `useUnmergedTree`, `getString(R.string.…)`, OkHttpInterceptor →
  `references/compose-ui-testing.md`
- Запуск `make test` / `make android-test`, фильтры, отчёты,
  `--tests` для unit, `-P...class=` для androidTest,
  `emulator-fast` → `references/running-tests.md`

## Verification checklist

Пройти правила 1–20 — таблица «правило → файл» ниже.

| Правила | Где смотреть |
|---|---|
| 1–5 (JUnit 4, имена, Given/When/Then, нет `!!`, зеркалирование) | `references/fundamentals.md` |
| 6 (`ViewModel` только unit), 9 (`MainDispatcherRule`), 10 (Turbine, только `test/`) | `references/viewmodel-testing.md` |
| 7 (MockK vs Fake), 16 (`Fake*ViewModel`, синхронизировать при изменении VM), правило буфера `MutableSharedFlow` | `references/fakes-and-stateflow.md` |
| 8 (`coEvery`/`coVerify`) | `references/mocking-mockk.md` |
| 11 (`Result<T>`) | `references/use-cases.md` |
| 12 (Robolectric при необходимости) | `references/fundamentals.md` (раздел «Robolectric») + `references/room-testing.md` («Каркас 2» для DAO) |
| 13 (`TimeoutTest(N)`), 17 (Room in-memory) | `references/room-testing.md` |
| 14 (`@RunWith(AndroidJUnit4::class)`) | `references/compose-ui-testing.md` (сниппет «Канонический компонентный тест»); исключение обосновать в коде |
| 15 (один `setContent` на тест), 18 (`waitForIdle` вместо `Thread.sleep`), 19 (`R.string.`), 20 (`useUnmergedTree`) | `references/compose-ui-testing.md` |
