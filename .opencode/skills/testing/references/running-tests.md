# Running tests

## Unit: `make test`

```bash
make test
```

Запускает `./gradlew test --console=plain` + отчёт
`scripts/test_report.py`. **Отчёт строится только при exit 0** (gradle
зелёный); при exit 1 `Makefile` прерывает выполнение до python-скрипта
и печатает `[FAIL]` + диагностику gradle. Диагностика падений на
упавшем прогоне — XML `app/build/test-results/test*/*.xml` или HTML
`app/build/reports/tests/testDebugUnitTest/index.html`.

### Фильтр по одному классу

```bash
./gradlew :app:testDebugUnitTest \
    --tests "com.swparks.domain.usecase.LoginUseCaseTest"
```

### Фильтр по одному методу

```bash
./gradlew :app:testDebugUnitTest \
    --tests "com.swparks.domain.usecase.LoginUseCaseTest.invoke_whenValidCredentials_thenSavesTokenAndCallsLogin"
```

`--tests` работает для unit-тестов.

## androidTest: `make android-test`

```bash
make emulator-fast    # после старта эмулятора, откат: make emulator-slow
make android-test
```

Запускает `./gradlew :app:connectedDebugAndroidTest` (только `:app:`,
без `:screenshot-tests:`) + `scripts/android_test_report.py` — отчёт
по классам (аналогично `scripts/test_report.py` для unit). Полный
прогон ~440–470 `@Test` на 2026-09-26 (число меняется, см. `grep '@Test' app/src/androidTest/`).
Длительность: ~2:33 после `make emulator-fast`, ~8:40 без.

### Масштабы анимаций: авто-защита от дрейфа

`make android-test` **сам** проверяет и чинит масштабы анимаций перед
каждым прогоном (prereq `_ensure_animations_off`) — вызывать
`make emulator-fast` руками больше не обязательно для корректности,
только для скорости первого прогона. Зачем так: масштабы умеют
откатываться уже **после** записи — 2026-10-08
`animator_duration_scale` сам вернулся в `null` (= 1.0) через минуты
после `make emulator-fast`, и UI-тесты шли втрое дольше. Причины сброса
ключа на стороне системы эмулятора; полагаться на память
(«не забудь emulator-fast») нельзя — точка проверки = точка
использования.

Три уровня защиты в `Makefile`:

| Цель | Что делает |
|---|---|
| `_emulator_animations` (emulator-fast/slow) | пишет 3 масштаба и **сверяет read-back**; расхождение → exit 1 |
| `_ensure_animations_off` (prereq `android-test`) | обёртка `_emulator_animations SCALE=0` (read-back сверен); эмулятора нет → no-op |
| `android-test` | гоняет тесты только после успешной защиты |

Симптом-диагностика (если защита обошли стороной — прямой gradle-вызов):
UI-тесты втрое дольше обычного или `qemu-system-aarch64` под сотнями
процентов дольше ~минуты → `adb shell settings get global
animator_duration_scale` (и соседние). Сам по себе краткий всплеск CPU
эмулятора во время UI-теста — норма (VM: рендер + оркестратор + GPU-эмуляция).

### Фильтр по одному классу

**`--tests "<FQN>"` НЕ работает на AGP 9** (Gradle 9.7.1 →
`Unknown command-line option '--tests'`). Рабочий фильтр:

```bash
./gradlew :app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=com.swparks.ui.screens.more.MoreScreenTest
```

### Отчёты

- **`make android-test`** при exit 0 → `python3 scripts/android_test_report.py`
  (статистика по классам, сортировка по числу падений, failed-методы).
  При exit 1 отчёт не строится (см. ниже).
- **HTML** при любом исходе: `make android-test-report` → открывает
  `app/build/reports/androidTests/connected/debug/index.html`.
- **XML** при любом исходе: `app/build/outputs/androidTest-results/connected/debug/*.xml`.
- **Per-test timeout failures** приходят в XML с именем класса
  наследника `TimeoutTest`.

## Канон запуска

→ `AGENTS.md`, секция «Focused tests».

## Сеть AVD

Нестабильна (детали и решение — `references/compose-ui-testing.md`,
раздел «Сетевые тесты»). `make emulator-fast` влияет только на
анимации, не на сеть. Лечить сетью или добавлять network stubs в тест.

## Связанные документы

- `AGENTS.md` — секции «Focused tests», «Testing», «Build, lint, test»
  (долговечные правила: `TimeoutTest`, `emulator-fast`, `--tests`).
- `docs/ui-tests-optimization.md` — **история** оптимизации (архивировано
  2026-09-26; долговечные правила переехали в `AGENTS.md`, здесь
  хранится только «как было»).
- `Makefile` — определение целей. `make test-all` тесты не запускает —
  только печатает пути к отчётам; полные прогоны — `make test` +
  `make android-test`.
