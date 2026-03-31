# План миграции bottom-nav на persistent tabs (аналог nowinandroid)

## Цель

Снизить лаги/фризы при переключении top-level вкладок (`parks/events/messages/profile/more`) за счёт архитектуры persistent tabs:

- каждая top-level вкладка имеет собственный сохранённый стек;
- при смене bottom-nav вкладка не пересоздаётся целиком;
- тяжёлые экраны (в первую очередь карта) не проходят полный re-init на каждом возврате.

## Scope изменений (что затрагиваем)

- `app/src/main/java/com/swparks/ui/screens/RootScreen.kt` (root-host, top-level контейнеры, `TopAppBar`, `shouldShowBottomBar`, текущий граф 30+ маршрутов);
- `app/src/main/java/com/swparks/navigation/AppState.kt` (state/контракты top-level навигации);
- `app/src/main/java/com/swparks/navigation/Navigation.kt` (BottomNavigationBar click/select logic);
- `app/src/main/java/com/swparks/navigation/Destinations.kt` (маршруты и parent/source mapping);
- `app/src/main/java/com/swparks/navigation/*NavigationCoordinator*.kt` (Park/Event/UserParks и др.);
- `app/src/main/java/com/swparks/ui/viewmodel/*NavArgsViewModel*.kt` (savedStateHandle и привязка к конкретному NavHostController);
- `app/src/test/java/com/swparks/navigation/*` (unit-тесты state и route-контрактов);
- `app/src/androidTest/java/com/swparks/ui/screens/RootScreen*.kt` и `.../parks/ParksRootScreenTest.kt` (интеграционные сценарии).

## Термины

- `source-driven navigation`: дочерние экраны получают `source` (`parks/events/messages/profile/more`) и по нему определяется «родительская» вкладка, которая должна оставаться выбранной в bottom-nav.
- `coordinator-объекты`: тонкие helper-функции/координаторы навигации в `com.swparks.navigation` (например, `ParkNavigationCoordinator`, `EventNavigationCoordinator`, `UserParksNavigationCoordinator`), которые строят route и передают аргументы.
- `persistent tabs`: отдельные substacks per top-level tab, где переключение вкладки не уничтожает состояние вкладки.
- `navigation3`: новый стек AndroidX (`navigation3-runtime/ui`) с моделью `NavKey + NavDisplay`; в отличие от текущего `navigation-compose` (`route string + NavHostController`) легче выражает multi-stack top-level навигацию.

## Текущее состояние (база)

- Сейчас top-level вкладки живут в одном `NavHost` (`RootScreen`).
- Используются `saveState/restoreState` в `AppState.navigateToTopLevelDestination(...)`.
- Это сохраняет back stack/route state, но не гарантирует «живой» UI-контейнер тяжёлых экранов.
- Для `parks` это приводит к полному re-init карты при `parks -> more -> parks`.
- В проекте уже есть навигационные coordinators, завязанные на `Screen.*` и `source`.

## Целевая архитектура

Аналогично nowinandroid по сути (не обязательно 1:1 по библиотеке):

- отдельный state-holder top-level навигации (top-level stack + substack per tab);
- `Navigator`/координатор, который управляет переходами между top-level и вложенными экранами;
- сохранение состояния и `ViewModelStore` для каждого top-level контейнера;
- переключение bottom-nav как show/hide/activate вкладки, а не полное пересоздание destination.

Источник истины после миграции:
- `AppState` остаётся единым публичным state-holder для UI (`currentTopLevelDestination`, `isCurrentRouteTopLevel`, top/bottom bar состояние).
- `TopLevelNavigationState` вводится как внутренняя модель внутри `AppState`, а не второй независимый state рядом.
- `TopLevelNavigator` вызывается из `AppState` и не публикуется напрямую в feature-экраны.
- Dual-state не допускается: внешние экраны продолжают работать только через `AppState`.

Выбранный подход декомпозиции `RootScreen`:
- Сценарий A (принят): отдельный `TabHost` composable на каждую top-level вкладку (`ParksTabHost`, `EventsTabHost`, `MessagesTabHost`, `ProfileTabHost`, `MoreTabHost`), у каждого свой substack-host/NavController.
- Общие правила:
- top-level и tab-local дочерние экраны живут в соответствующем `TabHost`;
- глобальные/кросс-tab операции маршрутизируются через `AppState` facade (`switchTab`, `switchTabAndOpen`, `reselectTab`);
- прямые вызовы `appState.navController.navigate(...)` в feature-экранах постепенно заменяются на методы `AppState`, чтобы не протекали детали конкретного NavController.
- Следствие для `NavArgsViewModel`:
- `savedStateHandle` и `navBackStackEntry` становятся substack-local; это фиксируется отдельным аудитом и адаптерами в этапе 4.

## Дизайн cross-tab навигации (обязательно до UI-интеграции)

- Тип 1: `switchTab(tab)` — активирует целевой top-level tab, восстанавливает его текущий substack.
- Тип 2: `switchTabAndOpen(tab, route)` — активирует tab, затем добавляет route в substack целевой вкладки.
- Тип 3: `reselectTab(tab)` — очищает только substack выбранной вкладки до её корня.
- Для сценария `Events -> Parks` сохраняется текущий контракт `appState.navigateToTopLevelDestination(PARKS)` и маппится на `switchTab(PARKS)`.
- Для будущих cross-tab deep-link сценариев используется `switchTabAndOpen(...)` с явной маршрутизацией в substack нужной вкладки.

Обязательная матрица cross-tab сценариев:
- `Events -> Parks` (`EventsScreen.onNavigateToParks`) с восстановлением текущего `parks` substack.
- `Any source -> NavigateToOwnProfile` из `OtherUserProfile` (без глобального reset всего графа, только `profile` substack reset + activate).
- `Messages -> UserSearch(source=messages) -> OtherUserProfile -> NavigateToOwnProfile` (многошаговая цепочка через две вкладки).
- `Parks -> CreatePark(source=parks)` (in-tab flow) с корректной логикой `shouldShowBottomBar` при активном/неактивном `parks` контейнере.

## Риски и как их закрыть

- Риск: поломка source-driven выбора активной вкладки.
- Митигировать: сохранить текущую логику parent/source в адаптере, покрыть integration-тестами.
- Риск: поломка back stack в дочерних экранах.
- Митигировать: unit-тесты на state machine + e2e-инструментальные цепочки.
- Риск: утечки памяти из-за долгоживущих контейнеров.
- Митигировать: явные lifecycle-правила для тяжёлых экранов, профилирование памяти.
- Риск: нестабильность из-за большой миграции за один PR.
- Митигировать: feature flag + инкрементальные PR + быстрый rollback.
- Риск: миграция на `navigation3` усложнит текущие тестовые harness-ы.
- Митигировать: сначала совместимый слой на `navigation-compose`, затем отдельное решение по `navigation3`.

## Стратегия миграции (рекомендуемая)

Оценка общей длительности этапов 0-4 (без буфера): 13-18 рабочих дней.
Оценка критического пути с частичным параллелизмом этапов 0-1: 12-17 рабочих дней.
Этап 5 (`navigation3`) вынесен за пределы базового плана и оценивается отдельно после RFC.

### Этап 0. Бейзлайн и контракт инвариантов

Срок: 1 рабочий день.

Ответственные:
- Owner: Android TL/ведущий разработчик.
- Reviewer: QA lead + владелец карты (`parks`).

- [ ] Зафиксировать baseline метрики:
- [ ] `parks_return_tti_ms_p95`: время `more -> parks` до first-interactive (цель baseline, затем сравнение).
- [ ] `parks_map_reinit_count_per_10_switches`: число `OnMapReady` на 10 переключений (baseline).
- [ ] `parks_switch_jank_ratio`: доля jank-кадров при переключении вкладок (через JankStats/Macrobenchmark).
- [ ] `persistent_tabs_rss_delta_mb`: рост RSS после 10 циклов переключения всех вкладок (baseline).
- [ ] `cold_start_to_first_interactive_ms_p95`: холодный старт до first-interactive (baseline).
- [ ] Зафиксировать навигационные инварианты:
- [ ] reselect текущей вкладки сбрасывает substack до корня;
- [ ] переход на другую вкладку восстанавливает её последний экран;
- [ ] back работает предсказуемо между top-level/substack;
- [ ] source-driven поведение текущих экранов сохраняется.

Критерий завершения:
- baseline и инварианты формально утверждены Owner + Reviewer.

Примечание по планированию:
- Этапы 0 и 1 могут частично выполняться параллельно: QA фиксирует baseline, разработчик начинает RED-тесты.

### Этап 1. Тесты (RED) для нового поведения top-level tabs

Срок: 2 рабочих дня.

Ответственные:
- Owner: Android разработчик, выполняющий миграцию.
- Reviewer: владелец тестовой инфраструктуры.

- [ ] Добавить unit-тесты для нового `TopLevelNavigationState`:
- [ ] `switchTab_preservesSubStack`;
- [ ] `reselectTab_clearsToRoot`;
- [ ] `backFromSubscreen_returnsToPreviousInSubStack`;
- [ ] `backFromTopLevelRoot_returnsToPreviousTopLevel`.
- [ ] Добавить отдельный класс `TopLevelNavigationStateTest` (не смешивать в один файл с legacy-проверками).
- [ ] Явно сохранить зелёными существующие тесты `AppStateTest` (текущие сценарии source-driven, ~683 строки на момент планирования).
- [ ] Добавить integration/UI-тесты для `RootScreen`:
- [ ] `parks(map) -> more -> parks` не создаёт новый map-host (через счётчик/лог-индикатор);
- [ ] выбранная вкладка bottom-nav корректна во всех текущих source-flow сценариях;
- [ ] внутренние переходы в рамках вкладки сохраняют анимации/поведение стека.

Критерий завершения:
- новые тесты падают на текущей реализации и фиксируют ожидаемое поведение.

### Этап 2. Модель и бизнес-логика навигации (GREEN)

Срок: 2-3 рабочих дня.

Ответственные:
- Owner: Android разработчик.
- Reviewer: Android TL.

- [ ] Ввести новые сущности:
- [ ] `TopLevelNavKey`/`TopLevelRoute`;
- [ ] `TopLevelNavigationState` (top-level stack + substacks);
- [ ] `TopLevelNavigator` с чистой логикой переходов.
- [ ] Сохранить совместимость текущего API:
- [ ] адаптер над текущим `AppState` (`navigateToTopLevelDestination`, `currentTopLevelDestination`);
- [ ] без массовой смены вызовов во feature-модулях на этом этапе.
- [ ] Зафиксировать матрицу cross-tab сценариев:
- [ ] `Events -> Parks` (switch only);
- [ ] `Profile -> OwnProfile` (очистка только profile-substack);
- [ ] source-driven дочерние маршруты в пределах активной вкладки.
- [ ] `Any source -> NavigateToOwnProfile` из `OtherUserProfile`;
- [ ] `Messages -> UserSearch(messages) -> OtherUserProfile -> OwnProfile`.

Критерий завершения:
- unit-тесты состояния/навигатора зелёные, старые контракты `AppState` сохранены.

### Этап 3. UI-интеграция persistent контейнеров top-level (GREEN)

Срок: 5-7 рабочих дней.

Ответственные:
- Owner: Android разработчик.
- Reviewer: Android TL + QA lead.

- [ ] Перестроить `RootScreen`:
- [ ] выделить persistent контейнер для каждой top-level вкладки;
- [ ] переключение bottom-nav = активация контейнера, а не полная пересборка экрана.
- [ ] Для каждой вкладки:
- [ ] хранить отдельный `NavHostController` или эквивалентный substack-хост;
- [ ] обеспечить корректную работу `ViewModelStoreOwner` и `SavedState`.
- [ ] Мигрировать `TopAppBar`/видимость bottom bar:
- [ ] `shouldShowBottomBar()` работает от route активного substack, а не от единственного `navController.currentBackStackEntryAsState()`;
- [ ] `isCurrentRouteTopLevel` вычисляется через активный top-level host.
- [ ] Проверить тяжёлые экраны:
- [ ] `ParkMapView` не пересоздаётся при `parks -> other -> parks`.

Критерий завершения:
- integration-тесты из этапа 1 проходят, карта не проходит полный re-init при tab-switch.

### Этап 4. Переходный слой и совместимость с текущими маршрутами

Срок: 3-5 рабочих дней.

Ответственные:
- Owner: Android разработчик.
- Reviewer: владелец navigation contracts.

- [ ] Оставить `Screen.*` маршруты и текущие coordinator-объекты как публичный контракт.
- [ ] Под капотом маршрутизировать вызовы через новый top-level state.
- [ ] Свести изменения в feature-экранах к минимуму.
- [ ] Отдельно закрыть сценарий `ProfileNavigationAction.NavigateToOwnProfile`:
- [ ] убрать глобальный `popUpTo(0) { inclusive = true }`;
- [ ] заменить на безопасный reset только `profile` substack + активация `Screen.Profile`.
- [ ] Провести аудит `*NavArgsViewModel*` и `consume*Args()/consume*Result()` helper-функций:
- [ ] проверить привязку к корректному `navBackStackEntry` внутри substack-local контроллеров;
- [ ] при необходимости добавить адаптер слоя для совместимой работы `SavedStateHandle` при multi-host.

Критерий завершения:
- существующие тесты на nav args/coordinator/source продолжают проходить без массовой переписи.

### Этап 5. Рефактор и опциональный переход на navigation3

Срок: оценивается отдельно в RFC (этап не входит в базовый roadmap 0-4).

Ответственные:
- Owner: Android TL.
- Reviewer: команда платформы/архитектуры.

- [ ] После стабилизации решить отдельно:
- [ ] оставаться на `navigation-compose` с persistent-контейнерами;
- [ ] либо мигрировать на `navigation3` (как в nowinandroid) через отдельный RFC.
- [ ] Если выбран `navigation3`, делать отдельными PR:
- [ ] сначала слой state/navigator;
- [ ] затем UI-host (`NavDisplay`) с адаптером route-аргументов.

Критерий завершения:
- принято осознанное решение на основе метрик, стоимости сопровождения и трудозатрат на тесты.

Критерий старта этапа 5 (что считаем «стабилизацией»):
- [ ] В течение 7 календарных дней после rollout этапов 0-4 нет критичных регрессий навигации (P0/P1).
- [ ] QA даёт формальное `OK` по regression checklist.
- [ ] Все обязательные nav/unit/androidTest suites зелёные в CI.

## План тестирования (обязательный)

- [ ] `make test` + таргетные unit-тесты `navigation/*`.
- [ ] Инструментальные тесты:
- [ ] `RootScreenBottomNavSourceFlowTest`;
- [ ] `RootScreenTest`;
- [ ] `ParksRootScreenTest` (регрессии карты и tab behavior).
- [ ] Добавить новые testsuite для persistent-tabs контрактов.
- [ ] Smoke на реальном девайсе:
- [ ] `parks(map) -> more -> parks`;
- [ ] длинные цепочки внутри `profile/messages/events`;
- [ ] back/reselect сценарии.

## Rollout и rollback

- [ ] Ввести runtime feature flag `persistentTopLevelTabsEnabled` в `AppSettingsDataStore` (локальный Preferences DataStore, `app_settings`).
- [ ] Сначала включить в debug/internal build.
- [ ] После прохождения тестов и smoke включить по умолчанию.
- [ ] Постепенное включение: debug -> internal -> production.
- [ ] В проекте нет обязательной зависимости на remote config для этой миграции; первичный rollout делаем через локальный DataStore флаг.
- [ ] При runtime-смене флага обеспечить пересоздание root-host:
- [ ] базовый путь: `key(persistentTopLevelTabsEnabled)` вокруг root navigation host;
- [ ] fallback: `Activity.recreate()` при переключении флага в debug/internal режиме.

Rollback (быстрый откат):
- [ ] Установить `persistentTopLevelTabsEnabled=false` в `AppSettingsDataStore` (или локальный дефолт в app config).
- [ ] Убедиться, что `RootScreen` выбирает legacy-путь с текущим `NavHost` и текущим `AppState`.
- [ ] Перезапустить приложение (или форсировать пересоздание root-host в runtime при смене флага).
- [ ] Проверить smoke: `parks/events/messages/profile/more` + `RootScreenBottomNavSourceFlowTest`.

Файлы, контролирующие откат:
- `app/src/main/java/com/swparks/ui/screens/RootScreen.kt` (ветвление persistent/legacy host);
- `app/src/main/java/com/swparks/navigation/AppState.kt` (legacy поведение top-level navigate);
- `app/src/main/java/com/swparks/navigation/Navigation.kt` (нажатия bottom-nav и вызов navigator/appState).

## Post-rollout мониторинг

Период наблюдения: минимум 7 дней после включения в production.

Ответственные:
- Owner: QA lead (ежедневный мониторинг dashboard и smoke).
- Backup owner: Android TL (эскалация/решение по rollback).

- [ ] Собирать и сравнивать метрики:
- [ ] `parks_return_tti_ms_p95`;
- [ ] `parks_map_reinit_count_per_10_switches`;
- [ ] `parks_switch_jank_ratio`;
- [ ] `bottom_nav_switch_duration_ms_p95`.
- [ ] `persistent_tabs_rss_delta_mb`.
- [ ] `cold_start_to_first_interactive_ms_p95`.
- [ ] Логировать события навигации:
- [ ] `bottom_nav_switch_started`;
- [ ] `bottom_nav_switch_completed`;
- [ ] `parks_map_on_map_ready`;
- [ ] `parks_map_style_loaded`.
- [ ] Проверять ошибки/ANR:
- [ ] crash-free сессии по navigation stack;
- [ ] ANR при tab switch;
- [ ] spikes по OOM/renderer warnings в экране карты.

## Обновление документации

- [ ] Обновить `docs/doc-navigation.md` с новой схемой top-level/substack.
- [ ] Обновить `docs/plan-map-screen.md` раздел про `bottomNavigation` и re-init карты.
- [ ] Добавить ADR-файл `docs/adr/adr-persistent-top-level-navigation.md` с решением: почему выбран persistent-host и выбран/не выбран `navigation3`.
- [ ] Обновить тестовую документацию по запуску nav regression suite.

## Definition of Done (измеримый)

- [ ] `parks_map_reinit_count_per_10_switches`: не более `1` (было baseline, ожидаем улучшение).
- [ ] `parks_return_tti_ms_p95`: улучшение минимум на `30%` относительно baseline из этапа 0.
- [ ] `parks_switch_jank_ratio`: не более `5%` jank-кадров при tab switch.
- [ ] `persistent_tabs_rss_delta_mb`: рост RSS не более `+20 MB` относительно baseline из этапа 0 после 10 циклов переключения всех вкладок.
- [ ] `cold_start_to_first_interactive_ms_p95`: не хуже baseline из этапа 0 более чем на `5%`.
- [ ] Навигационные инварианты и source-flow инварианты сохранены.
- [ ] Текущие навигационные тесты зелёные.
- [ ] Новые тесты persistent-tabs зелёные.
- [ ] Регрессий по back/reselect/top bar/bottom bar не обнаружено.
- [ ] Проверка на утечки памяти пройдена: нет новых leak-сигналов для `RootScreen`/`ParkMapView` после 10+ циклов `parks -> other -> parks` (LeakCanary или эквивалентный инструмент).
