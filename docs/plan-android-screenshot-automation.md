# План автоматизации Android-скриншотов для Jetpack-WorkoutApp

## Цель

Сделать в `Jetpack-WorkoutApp` быструю и воспроизводимую автоматизацию маркетинговых скриншотов через `fastlane` по образцу `JetpackDays`, но с важным отличием:

- скриншоты должны запускаться не через текущий общий `androidTest`;
- для скриншотов нужен отдельный Android UI-test target;
- fastlane должен собирать и запускать только screenshot-target, чтобы генерация была заметно быстрее полного `make android-test`.

## Что взято за эталон

### JetpackDays

Эталонная Android-схема уже есть в `JetpackDays`:

- `Makefile` содержит команды `screenshots`, `screenshots-ru`;
- `fastlane/Fastfile` вызывает `capture_android_screenshots(...)`;
- `fastlane/Screengrabfile` фильтрует только пакет `com.dayscounter.screenshots`;
- есть отдельный UI-тест `ScreenshotsTest`, который делает серию снимков через `Screengrab.screenshot(...)`;
- подключена зависимость `tools.fastlane:screengrab`;
- в debug manifest добавлены права для локали, разблокировки экрана и сохранения скриншотов.

### SwiftUI-WorkoutApp

Эталонный пользовательский сценарий уже описан в iOS UI-тесте `WorkoutAppUITests.testMakeScreenshots()`:

- `0-parksMap`
- `1-parksList`
- `2-parkDetails`
- `3-pastEvents`
- `4-eventDetails`
- `5-profile`

Структурно это один orchestrator-тест, разбитый на маленькие шаги-хелперы.

## Что уже есть в Jetpack-WorkoutApp

### Уже настроено

- В `Makefile` уже есть команды `screenshots`, `screenshots-ru`.
- В `fastlane/Fastfile` уже есть lane-ы `screenshots`, `screenshots_ru`.
- В `fastlane/Screengrabfile` уже прописан пакет `com.swparks.screenshots`.
- В документации уже описан ожидаемый сценарий использования `make screenshots`.

### Чего не хватает сейчас

- В проекте нет пакета `com.swparks.screenshots` и нет screenshot UI-теста.
- В `app/build.gradle.kts` не подключен `tools.fastlane:screengrab`.
- У приложения нет отдельного manifest/source set для screenshot-разрешений.
- Текущая команда `_build_screenshots_apk` собирает `assembleDebug assembleDebugAndroidTest`, то есть опирается на общий `app/src/androidTest`.
- В `app/src/androidTest` уже много интеграционных и UI-тестов, поэтому текущий `android-test` слишком тяжелый для сценария генерации скриншотов.

## Рекомендуемое архитектурное решение

Рекомендуемый путь: **отдельный screenshot-target как отдельный Gradle-модуль для Android UI tests**, а не новая папка внутри существующего `app/src/androidTest`.

Почему это лучший вариант:

- не нужно смешивать маркетинговые скриншоты с регрессионным `androidTest`;
- fastlane будет собирать отдельный test APK;
- screenshot automation станет независимой от общего набора инструментальных тестов;
- проще держать отдельный `Application`, `Runner`, фейковые данные и локальные permissions;
- ниже риск случайно замедлить `make screenshots` при росте обычного `androidTest`.

### Предлагаемая структура

- `:app` остается основным приложением.
- Новый модуль, например `:screenshots`, создается как отдельный Android test target.
- Для модуля `:screenshots` использовать `com.android.test` plugin.
- В `build.gradle.kts` модуля `:screenshots` явно указать `targetProjectPath = ":app"`, чтобы instrumentation tests запускались против APK основного приложения `com.swparks`.
- В модуле `:screenshots` лежат:
  - свой instrumentation runner;
  - свой test manifest;
  - screenshot-only зависимости;
  - пакет `com.swparks.screenshots`;
  - один основной orchestrator-тест `testMakeScreenshots`.

### Требования к эмулятору

- screenshot-flow должен запускаться на заранее поднятом эмуляторе, как и в `JetpackDays`; fastlane/screengrab использует уже доступное устройство через `adb`;
- нужен AVD с портретной ориентацией и экраном, подходящим под store-скриншоты;
- минимально нужно зафиксировать API level, resolution и locale setup в документации и в отладочном checklist;
- `use_adb_root true` нужно отдельно проверить на выбранном API level и образе эмулятора, а не считать универсально рабочим для любого AVD.

## Принцип работы screenshot-target

### Базовая идея

Скриншоты должны сниматься не на реальных сетевых данных и не через живой логин, а на **детерминированном screenshot-container** с подготовленным состоянием приложения.

Это даст:

- быстрый прогон;
- одинаковый результат на каждой машине;
- отсутствие зависимости от API, интернета, логина, push-alert и внешних данных;
- возможность точно повторить iOS-сценарий, но быстрее и стабильнее.

### Что должен подменять screenshot-container

- список парков для карты и списка;
- данные для `ParkDetail`;
- список прошедших мероприятий;
- данные для `EventDetail`;
- авторизованного пользователя для экрана профиля;
- поиск пользователя, если он нужен для финального скриншота;
- локацию и геокодинг, если экран зависит от позиции пользователя.

### Разделение ответственностей внутри screenshot-окружения

Нужно явно разделить две ответственности, чтобы не смешивать их в одной абстракции без необходимости:

- `ScreenshotAppContainer` отвечает за подмену зависимостей приложения и за выдачу детерминированных demo-данных;
- отдельный navigation state provider или screenshot scenario state отвечает за стартовое навигационное состояние, если для части экранов будет выбран не UI-переход, а предустановленный вход в detail screen.

Если на практике окажется удобно объединить это в одном screenshot-окружении, это допустимо, но в плане реализации считать эти ответственности разными и проектировать их отдельно.

## Этапы реализации

### Этап 1. Инфраструктура отдельного screenshot-target

- [x] Добавить в `settings.gradle.kts` новый модуль для screenshot UI tests.
- [x] Выбрать формат target:
  - основной вариант: отдельный модуль Android test;
  - резервный вариант: отдельный variant/build type, только если test-модуль окажется технически неудобен.
- [x] Подключить в модуле `:screenshots` `com.android.test` plugin и указать `targetProjectPath = ":app"`.
- [x] Зафиксировать точные Gradle task names для сборки app APK и screenshot test APK.
- [x] Настроить отдельный package namespace для screenshot-тестов.
- [x] Создать отдельный `AndroidManifest.xml` для screenshot-target с правами:
  - разблокировка экрана;
  - wake lock;
  - смена локали;
  - доступ, необходимый `screengrab`.
- [ ] Зафиксировать требования к эмулятору для локальной отладки:
  - API level;
  - resolution/aspect ratio;
  - портретная ориентация;
  - готовность `adb` и совместимость с `use_adb_root true`.
- [x] Подключить `tools.fastlane:screengrab` как зависимость именно для screenshot-target.
- [x] Добавить smoke-проверку сборки screenshot-target в виде команды Gradle, которую можно гонять без полного `androidTest`.

**Результат этапа 1:**
- Модуль `:screenshots` создан с `com.android.test` plugin и `targetProjectPath = ":app"`.
- Подтверждена успешная сборка `:screenshots:assembleDebug`.
- Подтверждена успешная сборка существующего `:app:assembleDebugAndroidTest` без регрессий.
- Для fastlane-интеграции в Этапе 6 нужно дополнительно зафиксировать финальный набор task names и итоговые пути к app APK и screenshot APK.
- Namespace: `com.swparks.screenshots`.
- AndroidManifest.xml включает `WAKE_LOCK`, `DISABLE_KEYGUARD`, `CHANGE_CONFIGURATION`, `WRITE_EXTERNAL_STORAGE`, `READ_EXTERNAL_STORAGE`.
- `tools.fastlane:screengrab:2.1.1` подключена только в `:screenshots`.

### Критерий завершения этапа

- можно отдельно собрать APK приложения и APK screenshot-target без запуска общего `app/src/androidTest`.

### Этап 2. Screenshot runner и test application

- [x] Создать отдельный `ScreenshotTestRunner`.
- [x] Создать `ScreenshotTestApplication`, наследующий `JetpackWorkoutApplication`.
- [x] Зафиксировать это как обязательное архитектурное решение: `ScreenshotTestApplication` не должен быть standalone `Application`, потому что в `RootScreen` и связанных хостах используется жёсткий cast к `JetpackWorkoutApplication` для доступа к `container`.
- [x] Не задавать `android:name` для `<application>` в manifest test-APK; подмена `Application` должна идти только через `ScreenshotTestRunner.newApplication(...)`.
- [x] Через `ScreenshotTestRunner` и тестовый manifest убедиться, что instrumentation поднимает именно `ScreenshotTestApplication`, а не обычный `JetpackWorkoutApplication`, и не вызывает краш `com.swparks.screenshots` вне instrumentation-контекста.
- [x] Подменить `AppContainer`, чтобы `MainActivity` и `RootScreen` работали на screenshot-данных.
- [x] Убедиться, что подмена не влияет на обычный запуск приложения и обычные тесты.
- [x] Добавить unit/smoke-проверки на то, что screenshot-container отдает нужные данные для экранов.
- [x] Добавить smoke-проверку, что после внедрения screenshot-container текущий `make android-test` и сборка `assembleDebugAndroidTest` по-прежнему работают.

**Результат этапа 2:**
- Добавлены `ScreenshotTestRunner` и `ScreenshotTestApplication` в `:screenshots`.
- `ScreenshotTestApplication` наследуется от `JetpackWorkoutApplication`, а `JetpackWorkoutApplication` объявлен как `open` для корректной подмены в instrumentation.
- В manifest screenshot-модуля оставлен только runner; `Application` подменяется в `ScreenshotTestRunner.newApplication(...)`, чтобы избежать ClassNotFound-crash в процессе `com.swparks.screenshots`.
- Добавлен smoke instrumentation test `ScreenshotAppBootstrapTest` (проверка, что поднят именно `ScreenshotTestApplication`).
- `ScreenshotAppContainer` добавлен как точка подмены контейнера и пока делегирует в `DefaultAppContainer`; детерминированные demo-данные будут реализованы в Этапе 3.
- Сборки подтверждены: `:screenshots:assembleDebug` и `:app:assembleDebugAndroidTest`.

### Критерий завершения этапа

- screenshot-target поднимает приложение в изолированном режиме через `ScreenshotTestApplication`; детерминированные screenshot-данные подключаются в Этапе 3.

### Этап 3. Подготовка данных для скриншотов

- [x] Использовать `parks` для screenshot-flow напрямую из `app/src/main/assets/parks.json` без ручных правок набора.
- [ ] Создать фиксированный набор demo-данных под Android-сценарий.
- [ ] Явно определить форму demo-данных:
  - либо через screenshot-specific fake repositories/use cases внутри `:screenshots`;
  - либо через переиспользуемые тестовые doubles, если их можно безопасно вынести из текущего `app/src/androidTest`.
- [ ] Синхронизировать набор экранов с iOS-сценарием:
  - карта парков;
  - список парков;
  - детали площадки;
  - прошедшие мероприятия;
  - детали мероприятия;
  - профиль.
- [x] Зафиксировать single-locale режим для `ru-RU` (публикация только в RuStore) и убрать обязательность `en-US` в screenshot automation.
- [x] Разделить пользователей screenshot-flow:
  - `demoAuthorizedUser` для логина и состояния авторизованной сессии;
  - `demoSearchUser`/`demoUser` для финального `OtherUserProfile` c iOS-совместимыми данными `UserResponse.preview` (`id = 24798`, `userName = Workouter`, реальные `workout.su` URL).
- [x] Для прошедших мероприятий использовать iOS-совместимый demo-набор на основе `EventResponse.previewList` с реальными URL изображений `workout.su`.
- [x] Для `ParkDetail` использовать iOS-совместимую preview-модель парка (`id=3`) с 6 фотографиями и comment-заглушкой, даже если список парков берется из `assets/parks.json`.
- [x] Для profile-search flow использовать отдельного demo-пользователя для поиска и `OtherUserProfile`, чтобы финальный скриншот совпадал по сценарию с iOS.
- [x] Для `OtherUserProfile` использовать отображение имени с fallback `fullName.ifBlank { userName }`, чтобы на финальном скриншоте имя не пропадало при пустом `fullName`.
- [x] Убедиться, что все ключевые изображения и превью имеют стабильный fallback, если сеть выключена.
- [x] Добавить нормализацию URL для `coil` (кириллица и пробелы в ссылках из `parks.json`), чтобы реальные картинки из `workout.su` загружались стабильно.
- [x] Исключить реальные API-запросы в screenshot-flow: `login/search/other-profile/sync parks/sync countries` должны идти через `ScreenshotAppContainer` и demo-репозитории, а не через `DefaultAppContainer`.
- [ ] Отдельно определить источник стартового навигационного состояния для detail-экранов, если часть сценариев будет открываться не кликом, а через преднастроенный route/state.
- [ ] Проверить существующие `Fake*ViewModel` из `app/src/androidTest/java/com/swparks/ui/viewmodel/` и принять явное решение:
  - переиспользовать через вынос в общую test-only зону;
  - или не переиспользовать и создать screenshot-specific doubles в `:screenshots`, если текущие `Fake*ViewModel` слишком тесно привязаны к обычным `androidTest`.
- [ ] Предпочесть переиспользование существующих `Fake*ViewModel` только если это не привязывает screenshot-target к полному набору обычных `androidTest` и не усложняет сборку.
- [x] Зафиксировать `ScreenshotScenarioState` на данных из iOS-совместимого demo-набора (`PARK_DETAIL_ID = 3`, `EVENT_DETAIL_ID = 4699`).

### Критерий завершения этапа

- все шесть экранов можно получить локально и без сети.

### Этап 4. Screenshot-сценарий по мотивам iOS `testMakeScreenshots`

- [ ] Создать пакет `com.swparks.screenshots`.
- [ ] Создать один основной UI-тест, например `WorkoutAppScreenshotsTest`.
- [ ] Разбить его на шаги-хелперы по аналогии с iOS:
  - `checkMap()`
  - `checkParks()`
  - `checkEvents()`
  - `checkProfile()`
- [ ] Снимать скриншоты в том же порядке и с теми же именами:
  - `0-parksMap`
  - `1-parksList`
  - `2-parkDetails`
  - `3-pastEvents`
  - `4-eventDetails`
  - `5-profile`
- [ ] Для каждого шага использовать `Screengrab.screenshot(...)`.
- [x] В `checkProfile()` повторить iOS-flow: открыть профиль, авторизоваться, открыть поиск пользователей, выполнить поиск, открыть `OtherUserProfile` и только после этого снять `5-profile`.
- [x] В `checkMap()` центрировать карту по геолокации эмулятора (Москва), не выбирая город вручную через фильтр, и делать снимок только после стабилизации карты.
- [x] Для screenshot-режима привести масштаб карты к iOS-ориентиру (по визуальному масштабу `defaultCoordinateSpan`) через отдельный screenshot-only zoom (`~12.2`), чтобы Москва читалась как город, а не как квартал.
- [x] В screenshot-режиме открывать экран мероприятий сразу на `PAST`-вкладке (без клика по табу).
- [x] На `EventsTabRow` отключить keyboard focus для `Tab` через `focusProperties { canFocus = false }`, чтобы на `3-pastEvents` не появлялся ложный hover/focus-визуал на невыбранной вкладке `Планируемые`.
- [x] Для `3-pastEvents` добавить neutral-tap перед снимком и увеличить post-screenshot delay, чтобы исключить остаточный pressed-state/выделение первого элемента.
- [x] Отказаться от screenshot-only ветки в `EventsScreen` (без проверки `isScreenshotMode` по имени `Application`): проблема `3-pastEvents` закрыта нейтральным тапом и задержкой в UI-тесте, без расхождения продуктового UI между обычным и screenshot-режимом.
- [x] Убрать из screenshot-flow поддержку `en-US` (лейны и конфиги), оставить только `ru-RU` как целевую публикационную локаль.
- [ ] Зафиксировать механику навигации между экранами:
  - основной вариант: переходы через Compose UI-взаимодействия, как в iOS-сценарии;
  - fallback для detail-экранов: предустановленное состояние навигации screenshot-container, если UI-переход окажется слишком хрупким.
- [ ] Явно определить, как пользователь попадает на `ParkDetail` и `EventDetail`: через клик по карточкам списка или через стартовое состояние nav graph.
- [ ] Если выбран UI-путь, добавить стабильные селекторы для bottom navigation, списков и action-кнопок.
- [ ] Добавить локальные robot/helper функции для ожидания загрузки, навигации и подготовки UI.
- [ ] Не считать `@ScreenshotTest` обязательной частью решения: для текущего `screengrab` достаточно `Screengrab.screenshot(...)`, а любые аннотации оставить только как опциональный слой организации тестов.

### Критерий завершения этапа

- один тест последовательно проходит все шесть экранов и сохраняет все шесть скриншотов.

### Этап 5. Доступность элементов для screenshot automation

- [ ] Проверить, хватает ли текущих `testTag`, `contentDescription` и стабильных текстовых селекторов.
- [ ] Для bottom navigation явно проверить наличие стабильных селекторов, если навигация будет идти через UI.
- [ ] Там, где селекторы хрупкие, добавить явные `testTag` для screenshot-flow.
- [ ] Не использовать селекторы, завязанные на случайные данные сервера.
- [ ] Убедиться, что переходы на `ParkDetail` и `EventDetail` можно делать либо через UI, либо напрямую через стартовое состояние навигации screenshot-container.

### Критерий завершения этапа

- screenshot-тест не зависит от нестабильных строк, таймингов и сетевых ответов.

### Этап 6. Fastlane и Makefile

- [ ] Обновить `Makefile`, чтобы `_build_screenshots_apk` собирал не `assembleDebugAndroidTest`, а отдельный screenshot-target.
- [ ] Уточнить и зафиксировать реальные пути:
  - app APK;
  - screenshot test APK.
- [ ] Обновить `fastlane/Fastfile`, чтобы `tests_apk_path` указывал на APK отдельного screenshot-target.
- [x] Добавить шаг установки геолокации эмулятора на Москву перед запуском `capture_android_screenshots`.
- [x] Включить auto-grant runtime-permissions для screenshot-flow через `pm grant` в `WorkoutAppScreenshotsTest` (через `UiDevice`), чтобы центрирование по геолокации не зависело от ручного подтверждения permission-диалогов.
- [ ] Оставить lane-ы:
  - `screenshots`
  - `screenshots_ru`
- [ ] Обновить `fastlane/Screengrabfile`: при необходимости поменять `tests_package_name`, чтобы namespace screenshot test APK был согласован с `use_tests_in_packages ['com.swparks.screenshots']`.
- [ ] Сохранить фильтрацию по пакету `com.swparks.screenshots`.
- [ ] Обновить `_cleanup_screenshots_apk`, чтобы cleanup удалял артефакты не только `:app`, но и нового screenshot-модуля.

### Критерий завершения этапа

- `make screenshots` не запускает общий `android-test`, а работает только через отдельный screenshot-target.

### Этап 7. Документация и эксплуатация

- [ ] Обновить `docs/doc-deployment.md`, чтобы документация соответствовала реальной схеме сборки.
- [ ] При необходимости обновить `README.md` и `scripts/update_readme.py`, если изменится naming/pattern скриншотов.
- [x] Добавить в `README.md` таблицу Android-скриншотов по образцу JetpackDays (с заголовками колонок из iOS README).
- [x] Добавить `scripts/update_readme.py` для автоматической подстановки актуальных timestamp-файлов скриншотов в таблицу README.
- [ ] Добавить в документацию требования к эмулятору:
  - поддерживаемое разрешение;
  - портретная ориентация;
  - локаль `ru-RU`;
  - включенный ADB.
- [ ] Описать отдельную команду для локальной отладки screenshot-target без fastlane.

### Критерий завершения этапа

- новый разработчик может сгенерировать скриншоты по документации без изучения исходников.

## TDD-порядок для этой задачи

Для этой задачи TDD применяем в адаптированном виде:

1. Сначала проверка инфраструктуры screenshot-target и его сборки.
2. Затем тесты/проверки для screenshot-container и demo-данных.
3. Затем screenshot orchestrator-test.
4. Затем fastlane/make automation.
5. Затем документация и финальная верификация.

## Основные риски

- `RootScreen` и `MainActivity` сейчас ожидают реальный `AppContainer`, поэтому подмена должна быть аккуратной.
- Если screenshot-flow будет идти через реальный API, автоматизация останется медленной и нестабильной.
- Если использовать текущий `app/src/androidTest`, скорость снова ухудшится по мере роста набора тестов.
- Если не добавить стабильные `testTag`, тесты будут хрупкими даже в single-locale сценарии.
- Если оставить старые пути APK в fastlane, lane будет собирать не тот test APK.
- Если неправильно настроить `ScreenshotTestApplication` и manifest screenshot-модуля, жёсткий cast к `JetpackWorkoutApplication` в `RootScreen` сломает запуск instrumentation.

## Нефункциональные требования

- Генерация скриншотов не должна требовать запуска полного `make android-test`.
- Скриншоты должны воспроизводиться одинаково для `ru-RU`.
- Сценарий должен быть детерминированным и не зависеть от API backend (все данные экранов берутся из screenshot-demo слоя).
- Имена скриншотов должны совпадать с iOS-сценарием, чтобы было проще поддерживать обе платформы.
- После внедрения screenshot-target существующий `make android-test` должен продолжать работать без регрессий.

## Definition of Done

- [ ] Есть отдельный Android screenshot-target.
- [ ] Есть отдельный screenshot runner и screenshot application/container.
- [ ] Есть пакет `com.swparks.screenshots`.
- [ ] Есть orchestrator-тест со снимками `0-parksMap` ... `5-profile`.
- [ ] `make screenshots` использует отдельный screenshot-target, а не общий `androidTest`.
- [ ] `fastlane screenshots_ru` создаёт скриншоты в `fastlane/metadata/android`.
- [ ] Документация обновлена и отражает реальную схему.

## Рекомендация по реализации

На этапе реализации считать **отдельный screenshot-target обязательным решением**, а не опциональным улучшением. Для `Jetpack-WorkoutApp` это не просто оптимизация, а базовое условие, чтобы маркетинговые скриншоты не зависели от тяжёлого общего instrumentation suite.
