# План доработок: наблюдаемость ошибок и автоматизация контроля утечек

> Последнее обновление: 2026-10-10.

## Цель

- [x] Построен единый контур отлова ошибок (fatal + non-fatal).
- [ ] Гарантировать, что ошибки из `ViewModel` и `Data` слоев доходят до `Logger` и Firebase Crashlytics. **Частично сделано** (см. статус ниже).
- [ ] Внедрить простую, регулярную и максимально автоматизированную проверку утечек памяти (LeakCanary + автосценарии), чтобы ручные проверки были только исключением.

## Контекст и исходные проблемы (по текущему состоянию)

- [x] Для `ChatViewModel` прямые `Log.e` заменены на единый маршрут `Logger + CrashReporter`. `markAsRead` — намеренно только `logger.e` (см. примечание к Этапу 3).
- [ ] В части остального кода еще используется `Log.*` напрямую, без единого маршрута в `Logger`/`CrashReporter` (сверено с кодом 2026-10-10). Канонический список — таблица в Этапе 4 (use case и data/util) и список в Этапе 3 (ViewModel). Кратко:
  - ViewModel: `JournalsViewModel`, `JournalEntriesViewModel`, `ThemeIconViewModel` (см. Этап 3);
  - feature/навигация: `FeedbackSender`, `AppState`, `ParkNavigationCoordinator`, `Navigation`, `RootScreen` (только `Log.d` для трассировки навигации), `ParkMapView` (только `Log.e` для `UnsatisfiedLinkError` MapLibre);
  - use case и data/util: см. таблицу в Этапе 4.
- [x] В release-режиме `NoOpLogger` снижает шум в логах, при этом non-fatal ошибки продолжают отправляться в Crashlytics через `CrashReporter.logException` (выход за рамки NoOp).
- [x] Инфраструктура `Logger`, `CrashReporter`, `UserNotifier`, `AppError` (с вариантами Network/Validation/Server/Generic/LocationFailed/LocationDisabled/GeocodingFailed/ResourceNotFound) реализована и подключена в `AppContainer`.
- [ ] Для утечек нет ни LeakCanary в зависимостях (`app/build.gradle.kts`, `gradle/libs.versions.toml`), ни `lint.xml` с baseline для `StaticFieldLeak`, ни `memory-check` в Makefile — стадия 6 фактически не начата.
- [ ] `lintDebug` пока не запускается как обязательный gate в Makefile (`make check` ограничен `ktlintCheck` + `app:detekt` + `test`).

## Принципы реализации

- [x] TDD-first: сначала тесты/сценарии, потом изменения.
- [x] Единая точка ошибок: любой catch-блок должен иметь предсказуемый маршрут ошибки.
- [ ] Минимум ручной рутины: одна локальная команда `make memory-check` и понятный отчет. CI в проекте нет — вся автоматизация выполняется на ноутбуке разработчика.
- [x] Без переусложнения: решение под маленькую аудиторию и бесплатное приложение.

## Архитектурное решение

- [x] Policy зафиксирована и применяется в `ChatViewModel` + `messagesRepository.markDialogAsRead`; для остальных ViewModel/feature-кода миграция не завершена.
- [x] Ошибка из ViewModel → `UserNotifier`/`Logger` (ChatViewModel).
- [x] Техническая ошибка → `CrashReporter.logException` для `IOException`/`HttpException` в `ChatViewModel.sendMessage`/`refresh`/`loadMessages`.
- [ ] В data/domain слоях оставить текущие `Result<T>` и усилить единообразие логирования/репортинга. **Контракт сложился** (repos возвращают `Result.failure` для ожидаемых ошибок, `crashReporter.logException` через `handleIOException`/`handleHttpException` для технических сбоев; см. `MessagesRepositoryImpl` и `MessagesRepositoryImplTest`), но многие use case и низкоуровневые data-классы всё ещё логируют через `android.util.Log` без инжекта `Logger`.
- [ ] Для утечек использовать `LeakCanary` в debug-сборке (product flavors в проекте нет, вариант `internal` не нужен) и инструментальный тестовый прогон, который валит `make memory-check` при подтвержденных leak-детекциях.

## Этап 0: Definition of Done и чек-лист

- [x] DoD для ошибки согласован:
- [x] `catch` не должен заканчивать путь только `Log.e` (соблюдено в `ChatViewModel`/`MessagesRepositoryImpl`).
- [x] Ожидаемые ошибки: `Result.failure` + UI уведомление.
- [x] Неожиданные: `logger.e` + `crashReporter.logException`.
- [ ] Распространение DoD на data/domain — открыто, см. Этап 4.
- [ ] DoD для утечек (черновик):
- [ ] `make memory-check` падает при новых leak-регрессиях в обязательных сценариях.
- [ ] Для релиза есть отчет по утечкам (автоматический артефакт).

## Этап 1: Инвентаризация ошибок и точек потерь (Red)

- [x] Собрать список всех мест с `Log.e`/`Log.w` в `ViewModel` и `UI`, где нет маршрута в `Logger`/`CrashReporter` (см. раздел «Контекст»).
- [ ] Сгруппировать по критичности:
- [ ] P0: авторизация, сообщения, профиль, создание/редактирование контента.
- [ ] P1: вторичные экраны и вспомогательные операции.
- [ ] Подготовить таблицу миграции: файл, тип ошибки, текущий путь, целевой путь.
- [ ] Зафиксировать baseline метрики Crashlytics (до изменений):
- [ ] Количество fatal/non-fatal за 7 дней.
- [ ] Crash-free users/sessions.

## Этап 2: Контракт ошибок (Green)

- [x] Guideline зафиксирован в коде:
- [x] `IOException`/`HttpException` разделены через `handleIOException`/`handleHttpException` (`BaseRepository`).
- [x] `CancellationException` rethrow — частично (JournalsViewModel/JournalEntriesViewModel.editJournalSettings); в use case не реализовано.
- [x] Неожиданные `Exception` → Crashlytics. Частично (ChatViewModel/MessagesRepositoryImpl); в use case/data-низкоуровневом коде нет.
- [x] Стандарт ViewModel утверждён: `logger.e` + `userNotifier.handleError` + `crashReporter.logException`.
- [x] Матрица "тип ошибки → действие" зафиксирована в `AppError` и `UserNotifier.handleError` (Network/Validation/Server/Generic/LocationFailed/LocationDisabled/GeocodingFailed/ResourceNotFound).

## Этап 3: Миграция feature-кода (ChatViewModel и далее)

- [x] `ChatViewModel` мигрирован на единый маршрут (детали — в «Архитектурном решении» выше).
- [ ] По аналогии пройтись по остальным ViewModel с прямыми `Log.*`. **Не сделано**:
  - `JournalsViewModel.editJournalSettings`, `loadJournals`, `retry`, `deleteJournal` — используют `Log.e`/`Log.i` вместо `logger.e`/`logger.i`; `logger` пока не инжектится.
  - `JournalEntriesViewModel.editJournalSettings`, `loadJournalAfterSettingsUpdate` — используют `Log.e`/`Log.i`; `logger`/`crashReporter` не инжектированы.
  - `ThemeIconViewModel.updateTheme` — `Log.e(TAG, "Ошибка сохранения темы", e)` без `crashReporter.logException`.
- [x] Проверено: `markAsRead`, `sendMessage`, `refresh` больше не "тихие", дубли в UI/Crashlytics отсутствуют.

> Обновление 2026-10 (фикс markDialogAsRead): `ChatViewModel.markAsRead` выведен из crash-маршрута — остался только `logger.e`, `crashReporter.logException` удалён. «Единый маршрут `Logger + CrashReporter`» выше относится к `sendMessage`, `refresh` и загрузке.

## Этап 4: Укрепление data/domain слоя

- [x] Контракт репозитория: `Result.failure` для ожидаемых ошибок, `logger.e` + `crashReporter.logException` для технических сбоев (через `handleIOException`/`handleHttpException`/`handleResponseError`); покрыто `MessagesRepositoryImplTest` (`verify(exactly = 0) { crashReporter.logException }`).
- [ ] Пройти оставшиеся места с прямым `android.util.Log` и перевести на `logger` (+ `crashReporter.logException` для технических сбоев). Каноническая таблица миграции (сверена с кодом 2026-10-10). **Не сделано**:

| Файл | Сейчас | Целевой путь | Приоритет |
|---|---|---|---|
| `SecureTokenRepository` | `Log.*` | `logger` | высокий (auth) |
| `AuthInterceptor` | `Log` (HTTP-401 path) | `logger` | высокий (auth) |
| `SyncJournalsUseCase` | `Log.i` старт + `Log.e` при `onFailure` | `logger.i` / `logger.e` | средний |
| `SyncJournalEntriesUseCase` | `Log.i` старт + `Log.e` при `onFailure` | `logger.i` / `logger.e` | средний |
| `TextEntryUseCase` | 8 `Log.i` на успех операций | `logger.i` | средний |
| `DeleteUserUseCase` | `Log.i` старт/успех, `Log.e` при ошибке | `logger.i` / `logger.e` | средний |
| `CryptoManagerImpl` | `Log.*` | `logger` | средний |
| `EncryptedStringSerializer` | `Log.*` | `logger` | средний |
| `UserPreferencesRepository` | `Log.*` | `logger` | средний |
| `IconManager` | 5 `Log.d`/`Log.e` | `logger.d` / `logger.e` | средний |
| `GetJournalEntriesUseCase` | `Log.i` старт | `logger.i` | низкий |
| `LogoutUseCase` | `Log.i` (есть `crashReporter.setUserId`) | `logger.i` | низкий |
| `Converters` | `Log.e` (десериализация) | `logger.e` | низкий |
| `DateFormatter` | `Log.w` (форматирование даты) | `logger.w` | низкий |
| `UriUtils` | `Log.w` / `Log.e` / `Log.i` (чтение uri) | `logger` | низкий |
| `User` | `Log.w` (парсинг даты рождения) | `logger.w` | низкий |
| `LoggingInterceptor` | `Log.d`/`Log.e` (диагностика OkHttp) | `logger` (опционально) | низкий |

> Room-сущности `JournalEntity` и `JournalEntryEntity` из миграции **вычеркнуты**: инжект `Logger` в data-класс — лишняя связность; `Log.e` в mapper-функциях приемлем, при реальной проблеме — проброс исключения в репозиторий.
- [ ] Нормализовать обработку `CancellationException` (rethrow) через общий хелпер `runCancellableCatching { }` в core — единая точка: `CancellationException` rethrow, остальные исключения — в `Result.failure`. Затронуто: `TextEntryUseCase`, `DeleteUserUseCase`, `SyncJournalEntriesUseCase` и т.п. (сейчас `try/catch (e: Exception)` без rethrow).
- [ ] Проверить `handleResponseError` и парсинг ошибок: `Result.failure` с человекочитаемым `message` уже формируется в `BaseRepository`, контекст ошибки теряется — нужны custom keys (набор — пунктом ниже).
- [ ] Добавить custom keys для Crashlytics — минимальный набор: `http_code` (фильтр по 5xx) и `feature` (группировка по фиче); остальные (`endpoint`, `action`, `auth state`) — по необходимости. **Частично** — `FirebaseCrashReporter.setCustomKey` поддерживает, но никто из data/use case не вызывает.
- [ ] Проверить privacy:
- [x] PII не отправляется: токен шифруется (`SecureTokenRepository`), в `AuthInterceptor` логируется только `path`.

## Этап 5: Тестирование контура ошибок (TDD)

- [x] `ChatViewModel` покрыт unit-тестами error handling в `ChatViewModelTest`:
  - `loadMessages`/`refreshMessages`/`sendMessage` — `IOException` и `HttpException` → `logger.e` + `crashReporter.logException` + `userNotifier.handleError`;
  - `markAsRead_onError_logsButDoesNotNotifyUser` — `markAsRead` не зовёт `userNotifier`/`crashReporter` (после фикса markDialogAsRead).
- [x] Контрактные тесты репозиториев:
  - `IOException`/`HttpException` обрабатываются корректно (`MessagesRepositoryImplTest`, `JournalEntriesRepositoryImplTest`);
  - `JournalEntriesRepositoryImplTest` проверяет `logger.e` для 401/403 и неожиданных исключений;
  - `MessagesRepositoryImplTest` явно проверяет, что `crashReporter.logException` НЕ вызывается для `Result.failure` (3 `verify(exactly = 0)`).
- [ ] *Опционально (nice-to-have, не блокирует этап):* smoke-тест "искусственный non-fatal" в debug-сборке для быстрой валидации Firebase. **Не сделано.**

## Этап 6: LeakCanary (базовое внедрение)

- [ ] Подключить LeakCanary только для debug-сборки через `debugImplementation` (product flavors в проекте нет, вариант `internal` не нужен). **Не начато** — нет зависимости в `app/build.gradle.kts` и `gradle/libs.versions.toml`.
- [ ] Включить автоматический экспорт результата анализа утечек в локальный отчет (лог/файл).
- [ ] Настроить игнор-лист известных framework false-positive утечек.
- [ ] Зафиксировать критичные сценарии для проверки:
- [ ] `parks -> map -> other tab -> back`.
- [ ] `messages -> chat -> back` циклы.
- [ ] `profile/edit screens` циклы открытия/закрытия.

## Этап 7: Автоматизация утечек локально

Автоматизация локальная (см. Принципы): вместо nightly job — одна удобная команда. `make memory-check` делает всё сама на подключённом эмуляторе, от человека требуется только вызвать её перед релизом (см. Этап 10: 100% без ручных действий недостижимо и локально).

- [ ] Создать инструментальный regression-сценарий (тест в `app/src/androidTest/`), который:
- [ ] Проходит 10-20 циклов по критичным экранам (список — Этап 6).
- [ ] Триггерит GC.
- [ ] Проверяет, что LeakCanary не нашел новых подтвержденных утечек, и валит прогон при их обнаружении.
- [ ] Добавить удобную команду `make memory-check` по образцу существующей `android-test`:
- [ ] запуск на подключённом эмуляторе (`EMULATOR_SERIAL`) с понятной ошибкой, если устройства нет;
- [ ] отключение анимаций через `_ensure_animations_off`;
- [ ] фильтр только leak-тестов (класс-фильтр инструментального прогона);
- [ ] цветной FAIL-блок при падении.
- [ ] **Не сделано** (в `Makefile` нет `memory-check`).
- [ ] Сохранять отчет как локальный артефакт с коротким summary (по образцу `scripts/android_test_report.py`).
- [ ] Локальный релизный гейт: `make memory-check` — обязательный шаг перед `make release` (зафиксировать в релизном чек-листе), повторный прогон после правок жизненного цикла Activity/ViewModel/навигации.

## Этап 8: Локальный релизный gate

- [x] `testDebugUnitTest` — гоняется через `make test`.
- [ ] `lintDebug` (с исправлением текущих lint errors). `make lint` сейчас вызывает только `ktlintCheck` + `app:detekt` + `markdownlint`; `lintDebug` отдельно не зафиксирован в обязательном gate.
- [x] `bundleRelease` — гоняется через `make release`.
- [x] `uploadCrashlyticsMappingFileRelease` — выполняется автоматически плагином `firebase.crashlytics` при `bundleRelease` (плагин подключён в `app/build.gradle.kts`); отдельный шаг в `Makefile` не требуется.
- [ ] Добавить автоматический post-build smoke:
- [ ] Проверка, что mapping загружен.
- [ ] Проверка, что test non-fatal виден в Firebase (для internal канала).

## Этап 9: Наблюдаемость после релиза

- [ ] Настроить минимальный runbook (1 страница; содержимое фиксируется в момент создания — экраны/URL Firebase Console, алерты, фичефлаги):
- [ ] Куда смотреть в Firebase в первые 24/72 часа.
- [ ] Какие алерты считать критичными.
- [ ] Как быстро отключить проблемный флоу (если возможно конфигом/фичефлагом).
- [ ] Установить простые SLO для маленькой аудитории (источник метрик — Firebase Crashlytics → Dashboard, раздел Stability):
  - [ ] Crash-free users >= 99.5%.
  - [ ] Новые fatal issues P0 закрываются за 24 часа.
  - [ ] Повторяющиеся non-fatal P1 — в ближайшем минорном релизе.

## Этап 10: Ответ на вопрос про "полностью без ручной проверки"

- [x] Вывод зафиксирован: 100% автоматизация недостижима; цель — 90-95% автопроверок + ручной triage по красным кейсам.

## Контрольные артефакты по завершению

- [ ] Обновленный документ policy по обработке ошибок.
- [x] Таблица миграции `Log.e -> Logger + CrashReporter` зафиксирована в Этапе 4 (миграция файлов не выполнена).
- [x] Unit/integration тесты по error-handling для `ChatViewModel` и репозиториев.
- [ ] Подключенный LeakCanary в debug-сборке.
- [ ] Команда `memory-check` с локальным артефактом отчета.
- [ ] Короткий post-release runbook.

## Порядок выполнения (рекомендуемый)

- [x] Неделя 1: Этапы 0-3 — **частично** (policy + `ChatViewModel` готовы; `JournalsViewModel`/`JournalEntriesViewModel`/`ThemeIconViewModel` — нет).
- [ ] Неделя 2: Этапы 4-6 (data/domain унификация + LeakCanary baseline).
- [ ] Неделя 3: Этапы 7-9 (локальная автоматизация утечек + релизный gate + runbook).
- [ ] Неделя 4: стабилизация, чистка ложных срабатываний, финальная приемка.

## Риски и смягчение

- [ ] Риск: шум non-fatal в Crashlytics.
- [ ] Смягчение: обязательные custom keys и дедупликация контекста.
- [ ] Риск: flaky leak-тесты на эмуляторе.
- [ ] Смягчение: фиксированный эмулятор-профиль, прогрев, повторный прогон при флаке.
- [ ] Риск: `make memory-check` забыли запустить перед релизом.
- [ ] Смягчение: прогон вписан в релизный чек-лист; опционально — вызывать `memory-check` из `make release` при подключённом эмуляторе.
