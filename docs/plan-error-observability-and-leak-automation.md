# План доработок: наблюдаемость ошибок и автоматизация контроля утечек

> Последнее обновление: 2026-10-10.

## Цель

- [x] Построен единый контур отлова ошибок (fatal + non-fatal).
- [ ] Гарантировать, что ошибки из `ViewModel` и `Data` слоев доходят до `Logger` и Firebase Crashlytics. **Частично сделано** (см. статус ниже).
- [x] Внедрена автоматизированная проверка утечек: LeakCanary в debug + `make memory-check`, правило запуска зафиксировано в `AGENTS.md` (Этапы 6-7; не покрыты auth-гейтед сценарии — см. Этап 7).

## Контекст и исходные проблемы (по текущему состоянию)

- [x] Единый контур ошибок работает: `Logger`/`CrashReporter`/`UserNotifier`/`AppError` подключены в `AppContainer`, `NoOpLogger` в release не глушит non-fatal в Crashlytics, `ChatViewModel` переведен на единый маршрут (детали — Этапы 2-3).
- [ ] В части остального кода еще используется `Log.*` напрямую, без единого маршрута в `Logger`/`CrashReporter` (пересве��ено с кодом 2026-10-10: все 3 ViewModel из Этапа 3 и все 16 файлов из таблицы Этапа 4 по-прежнему без `Logger`, ни одна миграция не начата). Канонический список — таблица в Этапе 4 (use case и data/util) и список в Этапе 3 (ViewModel). Кратко:
  - ViewModel: `JournalsViewModel` (17 `Log.*`), `JournalEntriesViewModel` (23), `ThemeIconViewModel` (7) (см. Этап 3);
  - feature/навигация: `FeedbackSender`, `AppState`, `ParkNavigationCoordinator`, `Navigation`, `RootScreen` (только `Log.d` для трассировки навигации), `ParkMapView` (только `Log.e` для `UnsatisfiedLinkError` MapLibre);
  - use case и data/util: см. таблицу в Этапе 4.
- [x] LeakCanary 2.14 в debug-сборке (в release-класспайте 0 совпадений, init-код не нужен), отчеты автоматически в logcat/файлы/экран. Ограничение на API 36: экран отчетов рисуется под системной навигацией — баг библиотеки, workaround невозможен, оставлено как есть.
- [ ] `lintDebug` не запускается как обязательный gate: `make lint` ограничен `ktlintCheck` + `app:detekt` + `markdownlint`, а `lint.xml` с baseline для `StaticFieldLeak` отсутствует. Это Этап 8.

## Принципы реализации

- [x] TDD-first: сначала тесты/сценарии, потом изменения.
- [x] Единая точка ошибок: любой catch-блок должен иметь предсказуемый маршрут ошибки.
- [x] Минимум ручной рутины: одна локальная команда `make memory-check` и понятный отчет. CI в проекте нет — вся автоматизация выполняется на ноутбуке разработчика.
- [x] Без переусложнения: решение под маленькую аудиторию и бесплатное приложение.

## Архитектурное решение

- [x] Policy зафиксирована и применена в `ChatViewModel`: ошибка из ViewModel → `UserNotifier`/`Logger`, техническая (`IOException`/`HttpException`) → `CrashReporter.logException`; остальной feature-код не мигрирован.
- [ ] В data/domain слоях оставить текущие `Result<T>` и усилить единообразие логирования/репортинга. **Контракт сложился** (repos возвращают `Result.failure` для ожидаемых ошибок, `crashReporter.logException` через `handleIOException`/`handleHttpException` для технических сбоев; см. `MessagesRepositoryImpl` и `MessagesRepositoryImplTest`), но многие use case и низкоуровневые data-классы всё ещё логируют через `android.util.Log` без инжекта `Logger`.

## Этап 0: Definition of Done и чек-лист

- [x] DoD для ошибки согласован и соблюден в `ChatViewModel`/`MessagesRepositoryImpl`: `catch` не заканчивается только `Log.e`, ожидаемые ошибки — `Result.failure` + UI-уведомление, неожиданные — `logger.e` + `crashReporter.logException`.
- [ ] Распространение DoD на data/domain — открыто, см. Этап 4.
- [x] DoD для утечек выполнен: `make memory-check` валит прогон при новых утечках (проверено: ловит реальную утечку, чистый прогон 2/2 зелено), отчет — в `app/build/reports/androidTests/connected/debug/`.

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
- [x] `IOException`/`HttpException` разделены через `handleIOException`/`handleHttpException` (`BaseRepository`); неожиданные `Exception` → Crashlytics в `ChatViewModel`/`MessagesRepositoryImpl`.
- [x] `CancellationException` rethrow точечно в 13 файлах (в т.ч. `InitializeParksUseCase`); единого хелпера нет — нормализация в Этапе 4.
- [x] Стандарт ViewModel: `logger.e` + `userNotifier.handleError` + `crashReporter.logException`; матрица «тип ошибки → действие» — в `AppError` и `UserNotifier.handleError` (8 вариантов).

## Этап 3: Миграция feature-кода (ChatViewModel и далее)

- [x] `ChatViewModel` мигрирован на единый маршрут и проверен: `markAsRead`, `sendMessage`, `refresh` больше не «тихие», дублей в UI/Crashlytics нет.
- [ ] По аналогии пройтись по остальным ViewModel с прямыми `Log.*`. **Не сделано**:
  - `JournalsViewModel.editJournalSettings`, `loadJournals`, `retry`, `deleteJournal` — используют `Log.e`/`Log.i` вместо `logger.e`/`logger.i`; `logger` пока не инжектится.
  - `JournalEntriesViewModel.editJournalSettings`, `loadJournalAfterSettingsUpdate` — используют `Log.e`/`Log.i`; `logger`/`crashReporter` не инжектированы.
  - `ThemeIconViewModel.updateTheme` — `Log.e(TAG, "Ошибка сохранения темы", e)` без `crashReporter.logException`.
> `markAsRead` намеренно выведен из crash-маршрута — остался только `logger.e` («единый маршрут» относится к `sendMessage`, `refresh` и загрузке).

## Этап 4: Укрепление data/domain слоя

- [x] Контракт репозитория: `Result.failure` для ожидаемых ошибок, `logger.e` + `crashReporter.logException` для технических сбоев (через `handleIOException`/`handleHttpException`/`handleResponseError`); покрыто `MessagesRepositoryImplTest` (`verify(exactly = 0) { crashReporter.logException }`).
- [ ] Пройти оставшиеся места с прямым `android.util.Log` и перевести на `logger` (+ `crashReporter.logException` для технических сбоев). Каноническая таблица миграции (пересве��ена с кодом 2026-10-10: **ни одна из 16 строк не мигрирована**, `Logger` не инжектится ни в один файл; в колонке «Сейчас» — фактическое число `Log.*`-вызовов). **Не сделано**:

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
- [ ] Нормализовать обработку `CancellationException` через общий хелпер `runCancellableCatching { }` в core — единая точка: `CancellationException` rethrow, остальные исключения — в `Result.failure`. Хелпера в коде нет; сейчас rethrow точечный в 13 файлах (см. Этап 2), а `catch (e: Exception)` без rethrow остается, например, в `IconManager`. Порядок: хелпер → перевод по одному use case, каждый с unit-тестом.
- [ ] Проверить `handleResponseError` и парсинг ошибок: `Result.failure` с человекочитаемым `message` уже формируется в `BaseRepository`, контекст ошибки теряется — нужны custom keys (набор — пунктом ниже).
- [ ] Добавить custom keys для Crashlytics — минимальный набор: `http_code` (фильтр по 5xx) и `feature` (группировка по фиче); остальные (`endpoint`, `action`, `auth state`) — по необходимости. **Пересве��ено 2026-10-10**: `setCustomKey` реализован в `CrashReporter`/`FirebaseCrashReporter` (включая авто-ключ `error_message`), но **ни один data/use case его не вызывает** — работа сводится к добавлению вызовов, сама инфраструктура готова.
- [ ] Проверить privacy:
- [x] PII не отправляется: токен шифруется (`SecureTokenRepository`), в `AuthInterceptor` логируется только `path`.

## Этап 5: Тестирование контура ошибок (TDD)

- [x] `ChatViewModel` покрыт unit-тестами error handling (`ChatViewModelTest`): `IOException`/`HttpException` → `logger.e` + `crashReporter.logException` + `userNotifier.handleError`; `markAsRead` не зовёт `userNotifier`/`crashReporter`.
- [x] Контрактные тесты репозиториев (`MessagesRepositoryImplTest`, `JournalEntriesRepositoryImplTest`): корректная обработка `IOException`/`HttpException`, `logger.e` для 401/403 и неожиданных исключений, `crashReporter.logException` НЕ вызывается для `Result.failure`.
- [ ] *Опционально (nice-to-have, не блокирует этап):* smoke-тест "искусственный non-fatal" в debug-сборке для быстрой валидации Firebase. **Не сделано.**

## Этап 6: LeakCanary (базовое внедрение)

- [x] LeakCanary 2.14 подключен через `debugImplementation`, init-код не нужен (автоустановка через manifest merge); в release-класспайте 0 совпадений.
- [x] Автоэкспорт отчетов работает без своего кода: logcat (тег `LeakCanary`), `databases/leaks.db` + hprof, экран отчетов в лаунчере. Приложение после подключения работает без изменений.
- [x] Игнор-лист FP: встроенный `AndroidReferenceMatchers` покрывает framework; кастомные `AppWatcher.config`-игноры не добавляются до реального FP (иначе можно заглушить настоящую утечку) — правило на каждый подтверждённый FP.
- [x] Зафиксированы критичные сценарии (покрытие — Этап 7): `parks -> map -> other tab -> back`, `messages -> chat -> back`, `profile/edit screens`.

## Этап 7: Автоматизация утечек локально

Автоматизация локальная (см. Принципы): вместо nightly job — одна удобная команда. `make memory-check` делает всё сама на подключённом эмуляторе, от человека требуется только вызвать её перед релизом (см. Этап 10: 100% без ручных действий недостижимо и локально).

- [x] `MemoryLeakRegressionTest` (`app/src/androidTest/.../leak/`): 15 циклов по нижним вкладкам + 15 циклов `parks -> park_detail -> back` через реальный `RootScreen`; GC и анализ heap делает `DetectLeaksAfterTestSuccess`, прогон валится при подтвержденных утечках (проверено: тест ловит удержание уничтоженной `Activity`, чистый прогон 2/2 зелено).
- [ ] `messages -> chat -> back` и `profile -> edit -> back` — **не сделано**: требуют авторизации, а стабы для нее нет. Добавлять отдельные auth-стабы ради этих двух сценариев — только когда появится реальный фикс утечки в них.
- [x] `make memory-check`: проверка эмулятора + `_ensure_animations_off` + фильтр только leak-тестов + цветной FAIL-блок; отчет — стандартный, как у `android-test` (переиспользован `scripts/android_test_report.py`, полный `HEAP ANALYSIS RESULT` попадает в XML).
- [x] Релизный гейт: правило запуска зафиксировано в `AGENTS.md` (Build/test, Testing, pre-commit checklist) — прогонять после задач по UI/навигации/ViewModel/жизненному циклу и всегда перед `make release`.

## Этап 8: Локальный релизный gate

- [x] Релизные гейты в Makefile: `testDebugUnitTest` (`make test`), `bundleRelease` (`make release`), маппинг Crashlytics — плагином `firebase.crashlytics` при `bundleRelease`, отдельный шаг не нужен.
- [ ] `lintDebug` (с исправлением текущих lint errors). `make lint` сейчас вызывает только `ktlintCheck` + `app:detekt` + `markdownlint`; `lintDebug` отдельно не зафиксирован в обязательном gate.
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

- [ ] Обновленный документ policy по обработке ошибок. **Частично есть** `docs/doc-universal-error-handling.md` (78 строк: `AppError`, `UserNotifier`, локализация, буферизация, аналитика) — но контракт `Logger` + `CrashReporter` (`handleIOException`/`handleHttpException`, матрица «тип ошибки → действие») там не описан. Нужно дописать этот раздел, а не заводить новый документ.
- [x] Таблица миграции `Log.e -> Logger + CrashReporter` зафиксирована в Этапе 4, unit/integration тесты error-handling для `ChatViewModel` и репозиториев написаны, LeakCanary и команда `memory-check` (+ правило в `AGENTS.md`) подключены.
- [ ] Короткий post-release runbook.

## Порядок выполнения (рекомендуемый)

- [x] Неделя 1: Этапы 0-3 — **частично** (policy + `ChatViewModel` готовы; `JournalsViewModel`/`JournalEntriesViewModel`/`ThemeIconViewModel` — нет).
- [ ] Неделя 2: Этапы 4-6 (data/domain унификация + LeakCanary baseline) — **частично** (Этап 6 закрыт; Этап 4 не начат).
- [ ] Неделя 3: Этапы 7-9 (локальная автоматизация утечек + релизный gate + runbook) — **частично** (Этап 7 закрыт; Этапы 8-9 открыты).
- [ ] Неделя 4: стабилизация, чистка ложных срабатываний, финальная приемка.

## Риски и смягчение

- [ ] Риск: шум non-fatal в Crashlytics.
- [ ] Смягчение: обязательные custom keys и дедупликация контекста.
- [ ] Риск: flaky leak-тесты на эмуляторе.
- [ ] Смягчение: фиксированный эмулятор-профиль, прогрев, повторный прогон при флаке.
- [ ] Риск: `make memory-check` забыли запустить перед релизом.
- [x] Смягчение: прогон вписан в `AGENTS.md` (pre-commit checklist + раздел Testing). Без CI гейт держится на дисциплине — при следующем релизе прогнать руками.
