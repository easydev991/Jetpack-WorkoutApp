# План: Замена `fallbackToDestructiveMigration` на нормальные миграции Room

## Контекст

В `app/src/main/java/com/swparks/data/AppContainer.kt:228` БД собирается так:

```kotlin
Room.databaseBuilder(...)
    .fallbackToDestructiveMigration(dropAllTables = true)
    .build()
```

Это значит: **при любом расхождении схемы Room стирает все пользовательские данные** (диалоги, избранные площадки, дневники, локальный кэш пользователей). Для приложения, которое уже в продакшене, это скрытый риск потери доверия пользователей.

Фактическое состояние БД по тегам релизов (`git show <tag>:app/src/main/java/com/swparks/data/database/SWDatabase.kt`):

| Тег        | `version` | entity |
| ---------- | --------- | ------ |
| `1.0` … `1.3.4` | 2    | 8      |

Схема 8 таблиц между `1.0` и `1.3.4` не менялась (`git diff 1.0 1.3.4 -- app/src/main/java/com/swparks/data/database/` затрагивает только DAO). То есть **все базы в проде — версии 2, и они совпадают с текущим кодом**.

**Цель:** убрать `fallbackToDestructiveMigration`, включить `exportSchema = true` для аудита изменений и зафиксировать правило миграций на будущее.

---

## Этап 1: Включить schema export

- [x] **1.1** В `SWDatabase.kt` заменить `exportSchema = false` на `exportSchema = true`
- [x] **1.2** В `app/build.gradle.kts` добавить путь экспорта схемы (проект использует KSP, не kapt):

  ```kotlin
  ksp {
      arg("room.schemaLocation", "$projectDir/schemas")
  }
  ```

- [x] **1.3** Собрать проект и закоммитить экспортированную схему в `app/schemas/com.swparks.data.database.SWDatabase/2.json` — Room раскладывает JSON по полному имени класса БД, путь создаётся сам. Это эталон для аудита изменений и для `MigrationTestHelper` в тестах миграций (Этап 3). В `.gitignore` ничего не добавлять

> Версию БД **не поднимаем**: `2.json` описывает ровно то, что лежит у пользователей. Bump до 3 без `MIGRATION_2_3` уронил бы базу каждому при обновлении.
>
> Короткий комментарий в коде: зачем нужен schema export — чтобы между версиями видеть diff таблиц и писать миграции точно по изменениям, а не «на глаз».

---

## Этап 2: Убрать fallback

- [x] **2.1** В `AppContainer.kt:228` удалить `.fallbackToDestructiveMigration(dropAllTables = true)`
- [x] **2.2** Собрать debug и release: Room не должен ругаться — версия БД остаётся `2`, схема не менялась, миграция не требуется
- [x] **2.3** `make build` — debug APK собирается (проверки стиля — отдельно в 4.2 `make lint`; импорты трогать не нужно: `fallbackToDestructiveMigration` — метод билдера, отдельного импорта у него нет)

`MIGRATION_1_2` не восстанавливаем: ни один релиз не выходил с `version = 1`, а схема «v1» в истории неоднозначна (1 entity в `82e59e77`, 2 в `3ae5a8d8`, 3 в `ac265bf4`) — миграция была бы недостижимым мёртвым кодом.

---

## Правило на будущее: миграции

Fallback больше нет, поэтому **первое же изменение схемы без миграции уронит базу**. Рецепт на каждое изменение `@Database(version = N)`:

1. bump `version` в `SWDatabase.kt`
2. `MIGRATION_{N-1}_N` с явным SQL в companion-объекте `SWDatabase` (`ALTER TABLE` / `CREATE TABLE` / `CREATE INDEX`, при удалении entity — `DROP TABLE`, при переносе данных — `UPDATE`/`INSERT`). DDL берётся дословно из `createSql` в экспортированном `N.json`, чтобы не расходиться с Room по DEFAULT/типам
3. подключить в `AppContainer.kt` через `.addMigrations(...)`
4. закоммитить новый `N.json` в `app/schemas/com.swparks.data.database.SWDatabase/` — SQL пишется по diff `(N-1).json` vs `N.json`
5. добавить тест в `MigrationTest.kt` в том же коммите (Этап 3) — миграция без теста не принимается

`@AutoMigration` для простых случаев (добавление колонки с `DEFAULT`) допустим; для переноса данных — только ручной SQL.

---

## Этап 3: Подключить `room-testing` и закрыть схему тестом

Подключаем **сейчас**, а не вместе с первой миграцией: уже есть что проверять. `room-testing` даёт работающие сегодня тесты — смоук связки export → assets (3.3) и отклонение downgrade (3.5). Защиту от «правка entity без bump'а версии» тест не даёт в принципе: тот же билд молча перезаписывает `2.json`, и тест сравнивал бы код с его же выводом — эта защита `git diff app/schemas` в `.githooks/pre-commit`.

Опыт взят из соседнего проекта `Jetpack-MyWorkouts` (тот же Room 2.8.5, `MigrationTest.kt` + `app/schemas/…/1.json…4.json`).

- [x] **3.1** В `gradle/libs.versions.toml` рядом с `androidx-room-ktx` добавить по образцу соседнего проекта:

  ```toml
  androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
  ```

  и в `app/build.gradle.kts` → `androidTestImplementation(libs.androidx.room.testing)`

- [x] **3.2** **Подключить схемы к assets androidTest** — без этого `MigrationTestHelper` не найдёт JSON и тест упадёт с «no such schema». В `app/build.gradle.kts`:

  ```kotlin
  sourceSets {
      getByName("androidTest").assets.srcDir("$projectDir/schemas")
  }
  ```

  > Про `kotlinx-serialization`: `room-testing` требует `>= 1.8.1`, иначе `AbstractMethodError` в `MigrationTestHelper`. В `Jetpack-MyWorkouts` пришлось добавлять `constraints`-блок, потому что там версия резолвилась в 1.7.3. У нас `kotlinxSerializationJson = "1.11.0"` в `libs.versions.toml` — заведомо выше, **constraints не нужен**. Если тест упадёт с `AbstractMethodError` — сначала проверь версию, потом добавляй constraints.

- [x] **3.3** Создать `app/src/androidTest/java/com/swparks/data/database/MigrationTest.kt` с тестом: `helper.createDatabase(TEST_DB, 2)` создаёт базу по `2.json`, затем её открывает настоящий `SWDatabase`. Тест доказывает связку export → assets и что закоммиченный JSON соответствует коду на момент генерации; от «изменил entity, забыв поднять `version`» он НЕ защищает (2.json перезаписывается тем же билдом) — guard это `git diff app/schemas` в `.githooks/pre-commit`. Класс наследует `TimeoutTest(180)` (конвенция проекта для Room-тестов, см. `JournalEntryDaoTest`)
- [ ] **3.4** На каждую будущую миграцию — тест по рецепту соседнего проекта: `helper.createDatabase(TEST_DB, N-1)` с данными → `helper.runMigrationsAndValidate(TEST_DB, N, true, MIGRATION_{N-1}_N)` → проверки, что пользовательские таблицы (`users`, `dialogs`, `journal_entries`) целы, `db.version == N`. Третий аргумент `true` валидирует и удалённые таблицы
- [x] **3.5** Тест на «некорректный downgrade»: открыть БД текущей версии как младшей (`MigrationTestHelper` с меньшим таргетом) и ожидать `IllegalStateException` — понижение версии Room отклоняет сам

> Отклонение в реализации 3.5: вариант «меньший таргет» (`runMigrationsAndValidate(..., 1, ...)`) требует несуществующий `1.json` — v1 в релизы не выходила, эталона нет. Реализовано эквивалентно: файл v2 из `2.json` с поднятым `user_version = 3`, настоящий `SWDatabase` (v2) открывает его → ожидаем `IllegalStateException` (downgrade без деструктивного fallback отклоняется, данные не стираются молча).
>
> Практическое следствие: откат сборки (поставить старый APK поверх новой БД) теперь = **краш** при первом открытии БД, а не старт с пустой базой, как было со старым fallback. Чинить локально: `adb shell pm clear com.swparks`.

---

## Этап 4: Проверка и safety net

- [x] **4.1** На эмуляторе: собрать и поставить APK с тега `1.3.4` (схема БД совпадает с продакшеном), создать тестовые данные (диалог, запись дневника), затем поставить APK из текущей ветки — данные должны сохраниться

> **Результат прогона 4.1** (эмулятор AVD API 36): APK `1.3.4` → первый запуск (seed 9300 парков в Room) → seed строк напрямую в `sw_database` (1 журнал, 1 запись дневника, 1 диалог — UI без логина их не создаёт, вставка через sqlite3) → `adb install -r` APK текущей ветки → запуск без краша, все счётчики до и после совпадают (1/1/1/9300), `user_version=2`, UI списка площадок идентичен. Скриншоты: `room-migration-4-1/`. Дифф-подтверждение: `git diff 1.3.4 HEAD -- app/src/main/java/com/swparks/data/database/` пуст, кроме `exportSchema = true` — обновлению нечему расходиться. In-memory `JournalEntryDaoTest` доказательством не является: он не открывает файловую БД и не проходит upgrade-путь.
- [x] **4.2** `make lint` — нет новых предупреждений
- [x] **4.3** `make test` — все тесты зелёные
- [x] **4.4** `make android-test` — инструментационные тесты зелёные (выполнено целевым прогоном задетых экранов вместо полного: journal-экраны `JournalEntriesScreenTest`/`JournalsListScreenTest`/`JournalSettingsDialogTest` + `JournalEntryDaoTest` + `MigrationTest` — 36 тестов, 0 failures)
- [x] **4.5** Откатывать удаление fallback только если **данные не сохранились** или Room бросил исключение при открытии. Если приложение не стартует по другой причине (нативный краш, ABI-сплиты релизной сборки) — это не про миграции: чинить сборку и повторять 4.1. Коммит с `exportSchema = true` в любом случае оставить (схему надо аудировать)

---

## Чек-лист внедрения

- [x] Этап 1 — schema export включён, `2.json` в репозитории, версия БД осталась `2`
- [x] Этап 2 — `fallbackToDestructiveMigration` удалён из `AppContainer.kt`, сборка чистая
- [x] Этап 3 — `room-testing` подключён, схемы прокинуты в assets androidTest, `MigrationTest.kt` с тестом регрессии схемы зелёный
- [x] Этап 4 — ручная проверка обновления с сохранением данных: 1.3.4 → HEAD на эмуляторе, журналы/записи/диалоги/парки целы, сборки и тесты зелёные

---

## Что НЕ нужно менять

- DAO и entity-классы — миграции работают на уровне SQL, entity остаются прежними
- Репозитории и ViewModels — не зависят от способа сборки БД
- `docs/plan-user-data-storage.md` — стратегия кэширования не меняется, только надёжность обновления схемы
- `version` в `SWDatabase.kt` — остаётся `2`
