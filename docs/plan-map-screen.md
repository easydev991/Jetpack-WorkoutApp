# План реализации экрана карты с MapLibre

## Обзор задачи

Нужно заменить заглушку `map_placeholder` в `ParksRootScreen.kt` на рабочий экран карты с:
- кластеризацией 9000+ точек;
- подсказкой по тапу на точку;
- кнопкой перехода в уже существующий `ParkDetailScreen`;
- кнопкой геолокации.

## Зафиксированный технический путь

В этом плане используется один стек:
- `AndroidView` + `MapView` из MapLibre Native Android SDK;
- Compose используется только для overlay-элементов поверх карты (`ParkInfoCard`, FAB, empty/error state).

Официальный MapLibre Compose SDK уже существует, но находится в активной разработке и пока не даёт нам выигрыша, который оправдывает дополнительные риски API-нестабильности. Для текущего проекта основной путь фиксируем через native SDK.

## Сценарий взаимодействия

- Тап по одиночной метке не делает навигацию сразу.
- Тап по одиночной метке обновляет `selectedParkId`.
- По `selectedParkId` показывается `ParkInfoCard`.
- Кнопка "Подробнее" в `ParkInfoCard` вызывает существующий UI-callback навигации в `ParkDetailScreen`.
- Тап по кластеру увеличивает карту до `expansionZoom`.
- Тап по пустому месту карты закрывает `ParkInfoCard`.

Это важно: состояние выбранного парка обязательно сохраняется, иначе tooltip / карточка не сможет жить консистентно.

## Текущее состояние после этапа 7

### Уже реализовано

- Этап 1: подключены `MapLibre Native Android SDK` и `play-services-location`.
- Этап 2: введены `UiCoordinates`, `MapUiState`, `MapCameraPosition`, `MapEvent`.
- Этап 3: `ParksRootViewModel` хранит `mapState`, очищает `selectedParkId` при изменении фильтра, использует `UserNotifier`.
- Этап 4: `ParkMapView` полностью реализован:
  - `GeoJsonSource` с native clustering API;
  - SymbolLayer для отрисовки parks, selected park и clusters;
  - bitmap-based иконки для кластеров со встроенным числом (не зависят от glyph endpoint);
  - безопасная обработка tap: кластер определяется по `point_count`, tap по feature без id деградирует в `ClearSelection`;
  - `fit bounds` для выбранного города при наличии 2+ уникальных координат;
  - диагностический лог координат: total/valid/unique, min/max lat/lon;
  - lifecycle MapView через `LifecycleEventObserver` + `ComponentCallbacks2`.
- Этап 5: добавлены `ParkInfoCard` и `MyLocationFab`.
- Этап 6: `MAP` tab интегрирован в `ParksRootScreen`, навигация в `ParkDetailScreen` идёт через UI-callback.
- `ParkMapView` переведён на единый `onMapEvent`, поэтому `ParksRootScreen` больше не разворачивает map events в набор отдельных callback-параметров.
- overlay-UI дочищен:
  - `ParkInfoCard` шириной max 420dp;
  - `CreateParkFab` автоматически поднимается над открытой карточкой по её реальной высоте;
  - появление/скрытие карточки и смена позиции FAB анимированы.
- Unit-тесты: `MapUiStateTest`, `ParksRootViewModelTest` (776 строк), `ParkMapGeometryTest`, `ParkMapMarkerSizingTest`.

### Требует завершения

- Этап 7: one-shot поведение `MyLocationFab` реализовано в `ParksRootViewModel`, требуется финальная верификация на реальном устройстве.
- Этап 8: OpenFreeMap glyph 404 не блокирует отображение parks/clusters, но label-слои базовой карты могут быть неполными.
- Этап 9: отсутствуют Android instrumented tests для `ParkMapView`, `ParkInfoCard`, `MyLocationFab`.

### Принятое решение по оставшимся багам

- финальный viewport выбранного города больше не строится только по `city center + zoom=11`;
- если у выбранного города есть `2+` валидные parks и `2+` уникальные координаты, карта делает `fit bounds(filteredParks city)` с padding;
- если валидная координата фактически одна, `fit bounds` не выполняется и используется fallback на одиночную точку или центр города;
- `userLocation` хранится в `MapUiState`, но не считается скрытым источником camera updates:
  - `MyLocationFab` создаёт только разовый `cameraPosition`;
  - дальше viewport двигают только явные map event'ы и пользовательский `OnCameraIdle`;
- диагностика распределения координат становится обязательной частью фикса:
  - логируем `total/valid/unique coordinates`;
  - логируем `min/max lat/lon`;
  - отдельно логируем, когда сработал `fit bounds`, а когда выбран fallback;
- `glyph 404` по OpenFreeMap пока классифицируется как отдельная style-level проблема:
  - он ломает текстовые подписи в style-level `SymbolLayer`, но не считается причиной отсутствия самих parks/clusters;
  - сначала закрываем source/filter/bounds/viewport, и только потом принимаем решение по glyph endpoint.
- для числа в кластере и устойчивого отображения markers выбран путь без зависимости от glyph endpoint:
  - одиночный park рисуется `SymbolLayer` с bitmap-иконкой;
  - selected park рисуется отдельной bitmap-иконкой;
  - кластер рисуется `SymbolLayer` с bitmap-иконкой, в которую уже встроено число `point_count`;
  - `SymbolLayer` использует `iconImage`, а не `textField`, поэтому cluster count не зависит от загрузки glyphs;
  - итог: кластеры и точки отображаются даже при сохраняющемся `glyph 404` у OpenFreeMap.

### Как именно были починены кластеры и точки

- причина №1: `park.cityID` оказался не глобально уникальным ключом для city-filter;
  - для Москвы фильтр по одному `cityID` подтягивал дальние точки с координатами далеко за пределами города;
  - исправление: после базового фильтра по `cityID` `ParksRootViewModel` нормализует набор parks по расстоянию до центра выбранного города;
  - если набор локальный, он остаётся как есть;
  - если набор слишком широкий, дальние ложные совпадения отбрасываются;
- причина №2: отображение cluster count через текстовый `SymbolLayer` зависело от glyph endpoint внешнего style;
  - `OpenFreeMap` возвращал `404`/`timeout` по glyphs;
  - исправление: cluster count перенесён в bitmap-иконки, а не в `textField`;
- причина №3: после перехода на bitmap-маркеры `selected`-слой временно фильтровался как `CircleLayer`, а не как `SymbolLayer`;
  - из-за этого selected marker рисовался поверх всех объектов;
  - исправление: фильтр `selectedParkId` переведён на `SymbolLayer`;
- итоговое решение в `ParkMapView`:
  - `GeoJsonSource` по-прежнему строится через native clustering API;
  - одиночные parks рисуются зелёной bitmap-иконкой;
  - selected park рисуется отдельной увеличенной bitmap-иконкой;
  - кластеры рисуются оранжевой bitmap-иконкой со встроенным числом;
  - размер cluster icon и размер шрифта подобраны отдельно, чтобы значения `2`, `19`, `104`, `402`, `1388` читались на карте.

### Анализ по логам после доработок

- карта больше не падает при открытии;
- `GeoJsonSource` действительно обновляется данными:
  - без city-filter: `8391 features`;
  - Москва: `1531 features`;
  - Анапа: `13 features`;
- Абакан: `23 features`;
- один парк с координатами `(-180, -180)` корректно отфильтровывается как невалидный;
- стартовая камера без выбранного города теперь уходит в безопасный регион `63.9999 / 94.0 / zoom=3.5`, а не в `zoom ~ 0.1`;
- `MyLocationFab` уже умеет центрировать карту: после нажатия карта приходит в `37.4219983 / -122.084 / zoom=15`;
- при выборе города камера уже действительно меняется:
  - Анапа: `OnCameraIdle(... latitude=44.9597, longitude=37.2274, zoom=9.7068 ...)`;
  - Абакан: `OnCameraIdle(... latitude=53.71556, longitude=91.42917, zoom=11.0 ...)`;
- Москва после нормализации набора parks и `fit bounds` приходит к локальному viewport (`zoom ~ 7.8` на старте, затем увеличивается по cluster click / zoom gesture), а кластеры реально отображаются;
- Анапа и другие небольшие города тоже показывают точки/кластеры в ожидаемом viewport;
- дефект приоритета камеры при смене города закрыт;
- обнаружено дополнительное правило для этапа 7:
  - центрирование на пользователе должно быть one-shot действием;
  - успешный recenter не должен включать follow-mode и не должен мешать следующему выбору города;
- найден и исправлен crash по tap на point:
  - `queryRenderedFeatures(...)` может вернуть feature без безопасного `cluster:Boolean`;
  - кластер нужно определять по наличию `point_count`, а tap по feature без `id` должен деградировать в `ClearSelection`, а не в crash;
- найден и исправлен crash по диагностике viewport:
  - `queryRenderedFeaturesForBox(...)` внутри native MapLibre падал в `libmaplibre.so`;
  - небезопасный diagnostic query по всему viewport удалён;
- после `onClearCityFilter` источник снова становится `8391 features`, но дальше нужно отдельно подтвердить ожидаемую стратегию viewport при снятии city-filter;
- `404` по glyphs для `Open Sans Regular, Arial Unicode MS Regular` остаётся;
- текстовый `SymbolLayer` для cluster count исключён из критического пути рендера, чтобы `glyph 404` не ломал отображение кластеров;
- кластеры и одиночные parks теперь отображаются стабильно:
  - cluster count виден прямо внутри cluster bitmap;
  - одиночные parks отображаются отдельными зелёными bitmap-маркерами;
- множество `Request failed due to a permanent error: Canceled` после резкого перелёта камеры выглядят ожидаемыми отменами prefetched tile requests, а не отдельным багом приложения;
- `MapLibreSurfaceView Warning, !readyToDraw()` при возврате с выбора города пока классифицируется как побочный renderer warning, не как корневая причина отсутствия parks.

### Уточнённый root cause после дополнительных логов

- проблема с Москвой показала, что `park.cityID` нельзя считать глобально уникальным ключом для city-filter;
- при `selectedCityId=1` в Android-приложение попадали parks с координатами в диапазоне:
  - `lat=[36.65, 56.87]`;
  - `lon=[18.87, 131.87]`;
  - это несовместимо с реальным viewport Москвы и объясняет уход камеры в почти глобальный `fit bounds`;
- следовательно, отсутствие parks/кластеров в части сценариев было не только renderer-проблемой, но и следствием некорректного состава `filteredParks`;
- обязательный фикс:
  - после первичного фильтра по `cityID` нормализовать parks по расстоянию до центра выбранного города;
  - если набор выглядит локальным, не трогать его;
  - если набор подозрительно широкий, отбрасывать дальние точки как ложные совпадения по неуникальному `city_id`;
- этот же механизм должен применяться и при обычном `onCitySelected(...)`, и при восстановлении фильтра из `DataStore`.
- для счётчика кластера и устойчивости отображения:
  - отказаться от зависимости на внешние glyphs для cluster count;
  - генерировать bitmap-иконки для park marker, selected marker и cluster marker;
  - для кластеров использовать иконку со встроенным числом, чтобы даже при `glyph 404` пользователь видел размер кластера.

### Анализ bottom-navigation и полного re-init карты

- внутри `parks` уже выполнена локальная оптимизация:
  - `ParkMapView` больше не пересоздаётся при переключении внутренних табов `LIST/MAP`;
  - карта остаётся в композиции экрана `parks`, а переключение происходит через show/hide logic;
- при переходе через `bottomNavigation` (`parks -> more -> parks`) полный re-init карты всё ещё происходит;
- это видно по фактическим логам:
  - снова приходят `OnMapReady`, `Style diagnostics`, `Обновляем source ...`, `Диагностика координат ...`;
  - снова стартует style load и новая волна tile/prefetch запросов;
- по коду причина ожидаемая:
  - top-level вкладки находятся внутри одного `NavHost`;
  - `AppState.navigateToTopLevelDestination(...)` использует `saveState=true` и `restoreState=true`;
  - это помогает back stack и route state, но не гарантирует, что `Composable` экрана `parks` и вложенный `MapView` останутся живыми между top-level destinations;
  - `ParksRootViewModel` живёт на уровне `RootScreen`, поэтому state карты сохраняется;
  - но сам `ParksRootScreen`/`ParkMapView` при возврате на `parks` всё равно пересоздаётся и повторно инициализирует `MapView` и style;
- вывод:
  - текущие логи при возврате через `bottomNavigation` являются ожидаемыми для текущей архитектуры;
  - это не баг данных или MapLibre;
  - это архитектурная особенность top-level navigation и lifecycle Compose destination.

### Можно ли удержать `parks` живым между bottom-nav вкладками без большого рефакторинга

- короткий ответ: полностью и надёжно удержать именно живой `MapView` между top-level вкладками без заметного рефакторинга навигации, скорее всего, нельзя;
- малой правкой это не решается, потому что проблема не в `ParkMapView`, а в том, что top-level destination `parks` выходит из композиции при переходе на другую вкладку;
- что уже есть сейчас:
  - `saveState/restoreState` в `AppState` — это правильная базовая практика и она уже используется;
  - `ParksRootViewModel` создан на уровне `RootScreen`, поэтому map state переживает возврат;
- чего этого не хватает:
  - `saveState/restoreState` не удерживает тяжёлый `AndroidView(MapView)` живым как объект рендера;
  - для этого нужно либо держать top-level screens одновременно в композиции, либо вынести карту в отдельный persistent host.

### Оценка практики

- для обычных bottom-nav экранов повторный re-init при возврате считается нормальным;
- для тяжёлых экранов вроде карты, камеры, webview или сложного feed удержание экрана живым между вкладками считается хорошей практикой, если:
  - это не приводит к утечкам памяти;
  - приемлема дополнительная память;
  - выигрыш в UX важнее стоимости удержания ресурса;
- для данного экрана карты это разумная цель оптимизации, но не обязательный baseline.

### Пошаговый план исправлений для разных кейсов

#### Кейс A. Минимальный безопасный путь без большого рефакторинга

- оставить текущую top-level navigation как есть;
- принять re-init карты при переходе `parks -> other tab -> parks` как архитектурно ожидаемое поведение;
- реализованные дешёвые оптимизации:
  - повторяющийся warning по одному и тому же невалидному park заменён на агрегированный лог вида `count + sampleIds`, который печатается один раз на уникальный набор проблемных parks;
  - `ParksRootScreen` больше не шлёт `OnLocationPermissionResult(...)`, если permission state уже совпадает с `uiState.mapState.locationPermissionGranted`;
  - `MyLocationFab` не дублирует `OnLocationPermissionResult(true)`, если permission уже синхронизирован;
  - `ParksRootViewModel` игнорирует повторный `OnLocationPermissionResult(...)` с тем же значением и не логирует его вторично;
  - за счёт этого убраны лишние state-updates и часть дублирующих логов при возврате на экран `parks`;
- что ещё оставляем сознательно:
  - debug-логи `OnMapReady`, `Style diagnostics`, `Обновляем source ...`, `Диагностика координат ...` пока сохраняются как полезная диагностика карты;
  - профиль времени первого кадра после возврата на `parks` остаётся ручной задачей для отдельной проверки;
- критерий успеха:
  - возврат остаётся с re-init, но без лишнего лог-шума и без заметного UI-jank сверх сетевой загрузки style/tiles.

#### Кейс B. Средний путь с частичным улучшением UX без перестройки всей навигации

- исследовать, можно ли удерживать визуальный контейнер `parks` поверх `NavHost` или рядом с ним, не трогая дочернюю навигацию остальных вкладок;
- целевой результат:
  - `ParksRootScreen` или хотя бы `ParkMapView` не выходит из композиции при переключении bottom-nav;
  - экран `more` и другие вкладки накладываются/скрываются отдельно;
- риск:
  - возрастает сложность `RootScreen`;
  - нужно аккуратно развести top bar, bottom bar и системные back-press сценарии;
- применять только если profiling покажет, что re-init карты действительно портит UX.

#### Кейс C. Правильный архитектурный путь для truly persistent top-level tabs

- перевести top-level вкладки на persistent container:
  - каждая top-level вкладка держится в памяти отдельно;
  - у каждой вкладки собственный сохранённый back stack / nested nav host;
  - переключение bottom-nav скрывает/показывает уже живой контейнер вместо пересоздания destination;
- для `parks` это даст:
  - живой `MapView` между `parks <-> more <-> ...`;
  - отсутствие повторного `OnMapReady` и повторного style load на каждом возврате;
  - более быстрый и плавный возврат на карту;
- цена:
  - это уже не маленькая правка;
  - придётся заметно переработать top-level navigation слой.

### План по лагам при исчезновении/появлении карты

- симптом:
  - при уходе с `parks` через bottom-nav карта исчезает не мгновенно;
  - при возврате экран “медленно проявляется”, как будто падает FPS или перегружается CPU/GPU;
- наиболее вероятные причины по текущему коду:
  - повторная инициализация `MapView` и MapLibre renderer;
  - повторный style load;
  - повторная генерация marker bitmaps и заполнение source на главном потоке;
  - новая волна tile/prefetch запросов при старте карты;
- пошаговый план диагностики и исправления:
  1. Зафиксировать baseline:
     - снять время между входом на `parks` и первым `OnMapReady`;
     - снять время до первого `OnCameraIdle`;
     - проверить субъективную плавность на эмуляторе и реальном устройстве.
  2. Убрать дешёвую лишнюю работу:
     - задедуплировать лог невалидного park;
     - убрать лишние `onMapEvent: OnLocationPermissionResult(granted=true)` на возврате;
     - минимизировать лишние recomposition-trigger side effects.
  3. Проверить стоимость marker pipeline:
     - если bitmap-иконки пересоздаются на каждом возврате, рассмотреть их кэширование;
     - если `FeatureCollection` пересобирается слишком часто, рассмотреть memoization по входному списку parks.
  4. Если lag остаётся:
     - переходить к кейсу B или C и удерживать `parks` живым между bottom-nav вкладками.

### Рекомендуемое решение на текущий момент

- краткосрочно:
  - реализовать кейс A;
  - убрать дешёвую лишнюю работу и уменьшить лог-шум;
  - оставить текущую bottom-nav архитектуру без большого рефакторинга;
- среднесрочно:
  - если после этого возврат на карту всё ещё субъективно “тяжёлый”, планировать кейс C как отдельную архитектурную задачу;
- причина такой рекомендации:
  - сама функциональность карты уже работает корректно;
  - полный persistent keep-alive для top-level `parks` потребует заметно больше изменений, чем локальная доработка `ParkMapView`.

## Этап 1: Зависимости и импорты ✅

- Подключены `org.maplibre.gl:android-sdk` и `play-services-location`.
- Зафиксированы native Android import'ы `org.maplibre.android.*`.

## Этап 2: Состояние карты ✅

- Реализованы `UiCoordinates`, `MapUiState`, `MapCameraPosition`, `MapEvent`.
- State развязан от MapLibre SDK; SDK-типы остаются внутри `ParkMapView`.
- Ошибки карты для v1 идут через `UserNotifier`, без persistent error-state.
- Базовые unit-тесты для map-state добавлены.

## Этап 3: Интеграция с `ParksRootViewModel` ✅

- `ParksRootUiState` расширен `mapState`.
- ViewModel управляет только состоянием и системными one-shot event'ами; навигация остаётся в UI.
- Выбор park синхронизирован с фильтром: невалидный `selectedParkId` автоматически сбрасывается.
- Добавлены/обновлены unit-тесты ViewModel для выбора park, камеры, геолокации и city-filter.

## Этап 4: `ParkMapView` ✅ реализация завершена

### 4.0 Текущий статус

Реализовано:
- `ParkMapView` создан (620 строк);
- `GeoJsonSource` и слои добавляются после загрузки style;
- `ParkMapView` наружу отдаёт единый `MapEvent`, а не набор разрозненных callback'ов;
- `OnCameraIdle` синхронизирует камеру обратно в `MapUiState`.
- кластеры и одиночные parks отображаются через bitmap-based `SymbolLayer`;
- cluster count больше не зависит от glyph endpoint и отображается внутри cluster bitmap;
- city-filter нормализуется по расстоянию до центра выбранного города (через `selectedCityBoundsCameraUpdate` в `ParkMapGeometry`);
- `ParkInfoCard` и `CreateParkFab` больше не конфликтуют по layout на карте;
- диагностика координат: total/valid/unique, min/max lat/lon, distance to city center.

Фиксируем выбранную реализацию:
- `MapView` создаётся на `Activity/Compose context`, а `MapLibre.getInstance(...)` и `ComponentCallbacks2` используют `applicationContext`;
- при `selectedCityCenter != null` и камере вида `city center + zoom=11` `ParkMapView` один раз пытается заменить такой запрос на `fit bounds(valid city parks)`;
- если `unique coordinates < 2`, `fit bounds` не выполняется и сохраняется fallback-камера;
- после пользовательского pan/zoom именно `OnCameraIdle` становится источником истины для следующего восстановления viewport.

Ожидаемая ручная проверка:
- финальная верификация `fit bounds` на нескольких городах и реальном устройстве;
- проверка отображения clusters/parks для городов с разным количеством парков.

### 4.1 Создать `ParkMapView`

**Файл:** `app/src/main/java/com/swparks/ui/screens/parks/ParkMapView.kt`

```kotlin
@Composable
fun ParkMapView(
    parks: List<Park>,
    selectedParkId: Long?,
    selectedCityCenter: UiCoordinates?,
    cameraPosition: MapCameraPosition?,
    userLocation: UiCoordinates?,
    onMapEvent: (MapEvent) -> Unit,
    modifier: Modifier = Modifier
)
```

Внутри `ParkMapView` выполняется локальная конвертация `UiCoordinates` в `LatLng`.
Наружу SDK-типы не выносятся.

### 4.2 Реализация через `AndroidView + MapView`

Ключевые решения:

- `MapView` создаётся один раз через `remember`.
- карта не пересоздаётся на каждый recomposition;
- overlay-UI (`ParkInfoCard`, FAB, empty state) рисуется Compose-слоем поверх `AndroidView`;
- `MapLibre.getInstance(...)` вызывается до создания `MapView`.

### 4.3 Кластеризация только через native API

Источник создаётся после загрузки style:

```kotlin
val source = GeoJsonSource(
    SOURCE_ID,
    featureCollection,
    GeoJsonOptions()
        .withCluster(true)
        .withClusterMaxZoom(14)
        .withClusterRadius(50)
)

style.addSource(source)
```

Это важнее псевдокода: в плане не использовать named arguments вида `cluster = true`, потому что реальный Android API работает через `GeoJsonOptions`.

### 4.4 Конвертация `Park` -> GeoJSON ✅

```kotlin
private fun List<Park>.toFeatureCollection(): FeatureCollection {
    val features = mapNotNull { park ->
        val latitude = park.latitude.toDoubleOrNull() ?: return@mapNotNull null
        val longitude = park.longitude.toDoubleOrNull() ?: return@mapNotNull null

        Feature.fromGeometry(
            Point.fromLngLat(longitude, latitude)
        ).apply {
            addNumberProperty("id", park.id.toDouble())
            addStringProperty("name", park.name)
        }
    }

    return FeatureCollection.fromFeatures(features)
}
```

**Реализация:** `ParkMapView.kt:247-261`
**Примечание:** `park.id` оборачивается в `.toDouble()`, т.к. `Long` не приводится к `Number` автоматически в этой перегрузке `addNumberProperty`.

### 4.5 Обработка кликов

Для native Android SDK:

```kotlin
maplibreMap.addOnMapClickListener { latLng ->
    val screenPoint = maplibreMap.projection.toScreenLocation(latLng)
    val features = maplibreMap.queryRenderedFeatures(
        screenPoint,
        CLUSTERS_LAYER_ID,
        UNCLUSTERED_LAYER_ID
    )

    when {
        features.any { it.getBooleanProperty("cluster") } -> {
            val clusterFeature = features.first { it.getBooleanProperty("cluster") }
            val expansionZoom = source.getClusterExpansionZoom(clusterFeature)
            val point = clusterFeature.geometry() as Point
            onMapEvent(
                MapEvent.ClusterClick(
                    target = UiCoordinates(
                        latitude = point.latitude(),
                        longitude = point.longitude()
                    ),
                    expansionZoom = expansionZoom
                )
            )
        }

        features.isNotEmpty() -> {
            val parkId = features.first().getNumberProperty("id").toLong()
            onMapEvent(MapEvent.SelectPark(parkId))
        }

        else -> onMapEvent(MapEvent.ClearSelection)
    }

    true
}
```

Важно:
- использовать `org.maplibre.android.geometry.LatLng`;
- `getClusterExpansionZoom(...)` возвращает `Int`, не nullable;
- при клике по кластеру наружу передаются и `target`, и `expansionZoom`, чтобы ViewModel могла однозначно обновить `cameraPosition`;
- не полагаться на `feature.getBooleanProperty("cluster")` как на non-null контракт; безопаснее определять кластер по `point_count` и деградировать в `onClearSelection()`, если feature не содержит ожидаемый `id`;
- источник и слои добавляются только после `style` load.
- выбранный контракт для экрана:
  - `ParkMapView` эмитит только `MapEvent`;
  - `ParksRootScreen` передаёт `viewModel::onMapEvent`;
  - `CenterOnUser` остаётся внешним UI-action и не генерируется самим `ParkMapView`.

### 4.6 Управление камерой

Весь документ использует один путь: нативное управление камерой.

```kotlin
maplibreMap.animateCamera(
    CameraUpdateFactory.newLatLngZoom(
        LatLng(64.0, 94.0),
        3.5
    )
)
```

Не использовать в этом плане `CameraPositionState` и другие API из compose-wrapper.

Дополнение к реализации:
- `LaunchedEffect` в `ParkMapView` не должен принимать решение по камере только на основании факта наличия `userLocation`;
- `userLocation` допустимо использовать как данные для UI и для результата one-shot геолокации, но не как скрытый fallback camera-source;
- это правило обязательно, чтобы после успешного recenter по FAB следующий выбор города или восстановление экрана не возвращали карту назад к пользователю.

Текущее наблюдение по логам:
- дефолтный `fit all parks` уже убран;
- city-focus работает и `OnCameraIdle` приходит с координатами выбранного города;
- рендер parks частично работает:
  - в маленьком городе одна точка видна;
  - в большом городе clusters не появляются даже при `1531 features`;
  - в городе с `15 features` визуально видна только одна точка;
- это означает, что нужно проверить и рендер слоёв, и сами данные координат.

План доработки:
- сохранить текущее правило приоритета камеры:
  - `userLocation` не должен быть постоянным драйвером камеры и не должен автоматически возвращать карту к пользователю после style reload / recomposition;
  - `MyLocationFab` делает разовый recenter через запись новой `cameraPosition`;
  - `ClusterClick` и другие явные программные переходы имеют следующий приоритет;
  - при смене города ViewModel обновляет `cameraPosition` под новый город;
  - при снятии city-filter карта сохраняет текущий viewport, пока пользователь сам не изменит его;
- следующий фокус:
  - доказать, что layer stack и filters действительно рендерят circles;
  - измерить, сколько parks реально попадает в текущий viewport после city-focus;
  - вывести min/max bounds координат по `filteredParks` выбранного города;
  - посчитать количество уникальных `lat/lon` пар, чтобы исключить массовое наложение точек в одну координату;
  - при необходимости временно логировать наличие слоёв и rendered features;
  - при необходимости поднять пользовательские слои выше reference layer base style через `addLayerAbove(...)`;
  - если данные города географически шире/грязнее, чем предполагает `zoom=11`, перейти на `fit bounds(filteredParks city)` вместо фиксированного zoom;
- добавить временный диагностический лог:
  - какое решение принято для камеры (`restore-state`, `selected-city`, `user-location`, `cluster-click`);
  - какой `target/zoom` реально применён после city change.

После уточнения реализации фиксируем финальный алгоритм camera resolution:
- `restore-state`: если есть валидный `cameraPosition` из state, сначала используем его;
- `selected-city + default city zoom`: если этот `cameraPosition` соответствует только что выбранному городу и у города есть `2+` уникальные координаты парков, заменяем его на `fit bounds`;
- `cluster-click`: всегда применяет переданный `target + expansionZoom`, без попытки `fit bounds`;
- `user-location`: камера меняется только когда ViewModel явно записала новый `cameraPosition` после успешного one-shot запроса геолокации;
- `initial fallback`: без города используем безопасный регион, а не `fit all parks`.

### 4.7 Lifecycle `MapView` ⚠️ доработать ownership `MapView`

**Файл:** `app/src/main/java/com/swparks/ui/screens/parks/ParkMapView.kt:77-112`

Полная реализация через `LifecycleEventObserver` + `ComponentCallbacks2`:

```kotlin
val lifecycleOwner = LocalLifecycleOwner.current

DisposableEffect(Unit) {
    MapLibre.getInstance(context)
    val map = MapView(context)
    mapView = map
    map.onCreate(null)

    val observer = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> map.onStart()
            Lifecycle.Event.ON_RESUME -> map.onResume()
            Lifecycle.Event.ON_PAUSE -> map.onPause()
            Lifecycle.Event.ON_STOP -> map.onStop()
            Lifecycle.Event.ON_DESTROY -> map.onDestroy()
            else -> {}
        }
    }
    lifecycleOwner.lifecycle.addObserver(observer)

    val componentCallbacks = object : android.content.ComponentCallbacks2 {
        override fun onLowMemory() = map.onLowMemory()
        override fun onTrimMemory(level: Int) {}
        override fun onConfigurationChanged(config: android.content.res.Configuration) {}
    }
    context.registerComponentCallbacks(componentCallbacks)

    onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer)
        context.unregisterComponentCallbacks(componentCallbacks)
        map.onDestroy()
        mapView = null
        mapLibreMap = null
    }
}
```

Принятые решения:
- `ON_LOW_MEMORY` не является `Lifecycle.Event` — обрабатывается через `ComponentCallbacks2`;
- `onTrimMemory`-заглушка (пустое тело), т.к. `MapView` не имеет соответствующего метода;
- Регистрация/удаление `ComponentCallbacks` в `DisposableEffect` гарантирует очистку при unmount;
- v1: полный native `onSaveInstanceState(...)` не реализован, восстановление через `MapUiState.cameraPosition`.

Дополнительно для стабилизации:
- убедиться, что `MapView`, переданный в `AndroidView`, и `MapView`, на который навешан lifecycle, совпадают;
- исключить сценарий с созданием второго `MapView` вне lifecycle-контроля;
- исключить двойной вызов `onDestroy()` для разных экземпляров.

Статус:
- после последних правок карта больше не падает на открытии, значит этот блок близок к закрытию;
- окончательно считать этап закрытым только после того, как:
  - при выборе города viewport реально меняется;
  - на карте становятся видны parks/кластеры этого города без ручного центрирования на геолокацию.

## Этап 5: Overlay-компоненты

- Реализованы `ParkInfoCard` и `MyLocationFab`.
- `ParkInfoCard` открывает существующую навигацию в `ParkDetailScreen`.
- FAB размещён отдельно от `CreateParkFab` и отражает состояние геолокации.

## Этап 6: Интеграция в `ParksRootScreen`

- Заглушка заменена на `ParkMapView` с overlay-компонентами.
- MAP tab использует тот же `filteredParks`, что и LIST tab.
- Навигация в `ParkDetailScreen` подключена через существующий `onParkClick(park)`.

## Этап 7: Геолокация и настройки устройства ✅

### 7.0 Статус

Реализовано в `ParksRootViewModel.handleCenterOnUser()`:
- one-shot центрирование карты на текущей геолокации;
- проверка permission state перед запросом геолокации;
- `SettingsClient.checkLocationSettings` для проверки device settings;
- `ResolvableApiException` обрабатывается через one-shot event + UI callback;
- `isLoadingLocation` сбрасывается во всех ветках: success, failure, cancel, exception;
- `userLocation` не является скрытым источником camera updates.

Требуется финальная верификация:
- UX сценарий: FAB -> resolution dialog -> accept -> recenter;
- UX сценарий: FAB -> resolution dialog -> cancel -> loading off;
- после успешного recenter следующий `onCitySelected` меняет viewport на город и не откатывается назад к `userLocation`.

### 7.1 Переиспользовать существующую инфраструктуру проекта

В проекте уже есть:
- permission flow в `ParksRootScreen`;
- `LocationService` в `AppContainer`;
- глобальный `UserNotifier`.

Карту нужно строить поверх этой инфраструктуры, а не заводить параллельные локальные решения.

### 7.2 Логика кнопки "центрировать на мне"

Требуемое поведение:
- `MyLocationFab` выполняет только разовое центрирование карты на текущей геолокации;
- после успешного центрирования не включается follow-mode и не устанавливается постоянный приоритет `userLocation` над дальнейшими camera updates;
- сначала пробуем быстро получить last known location;
- если её нет, запрашиваем одноразовую актуальную геолокацию;
- если координаты всё равно недоступны, показываем сообщение через `UserNotifier`.

Реализация должна жить внутри `LocationService` или его расширения, а не напрямую внутри composable.

### 7.3 Проверка location settings

Перед запросом геолокации проверять настройки устройства через `SettingsClient.checkLocationSettings(...)`.

Если настройки можно исправить стандартным системным диалогом:
- обрабатывать `ResolvableApiException`;
- эмитить one-shot event из ViewModel;
- UI запускает `IntentSenderRequest` через `ActivityResultContracts.StartIntentSenderForResult`.

Только если сценарий неразрешим через системный dialog:
- предлагать переход в `Settings.ACTION_LOCATION_SOURCE_SETTINGS`.

В тексте UI использовать формулировку "геолокация устройства", а не только "GPS".

### 7.4 Ошибки геолокации

Через `UserNotifier` показывать:
- отсутствие разрешения;
- выключенную геолокацию устройства;
- невозможность получить координаты;
- ошибку системного resolution flow.

### 7.5 Ручная проверка геолокации

Финальная верификация на реальном устройстве:
- FAB -> permission dialog -> accept -> recenter;
- FAB -> permission dialog -> deny -> error notification;
- FAB -> resolution dialog -> accept -> recenter;
- FAB -> resolution dialog -> cancel -> loading off;
- `MyLocationFab` -> карта центрируется на пользователе;
- затем выбор города снова переводит карту на город, без возврата к `userLocation`;
- возврат с экрана выбора города без фактической смены фильтра не меняет viewport.

## Этап 8: Стиль карты и кэш

### 8.1 OpenFreeMap style

Текущая реализация:
```kotlin
val styleUri = "https://tiles.openfreemap.org/styles/liberty"
```

Статус glyph 404:
- `glyph 404` для `/fonts/Open Sans Regular,Arial Unicode MS Regular/0-255.pbf` не блокирует отображение parks/clusters;
- parks и clusters рисуются через bitmap-иконки (`iconImage`), а не через `textField`, поэтому cluster count отображается корректно;
- glyph issue влияет только на label-слои базовой карты (названия улиц и т.д.);
- это приемлемо для v1 карты.

Ожидаемые non-blocking логи:
- `Request failed due to a permanent error: Canceled` — отмены prefetch tile requests при перелёте камеры;
- `Invalid geometry in line layer` — артефакт внешнего style/tile content.

Риск для production:
- публичный OpenFreeMap — бесплатный сервис без SLA;
- для production рекомендуется рассмотреть self-hosted style или коммерческий tile provider.

### 8.2 Кэш и offline

Для первого этапа не фиксировать в плане конкретный код низкоуровневой настройки offline/cache API, если он не перепроверен под точную версию SDK.

Фиксируем только безопасное решение:
- используем встроенный ambient cache SDK;
- не добавляем в документ потенциально устаревший псевдокод вроде `TileStoreOptions.getInstance().setCachePath(...)`;
- если понадобится тонкая настройка офлайна, отдельно перепроверяем актуальный API `OfflineManager` под выбранную версию MapLibre.

## Этап 9: Тестирование

### 9.1 Unit-тесты ✅

Существующие:
- `MapUiStateTest` — базовые тесты map state;
- `ParksRootViewModelTest` (776 строк) — тесты ViewModel для map events, city filter, геолокации;
- `ParkMapGeometryTest` — тесты геометрии камеры для city bounds;
- `ParkMapMarkerSizingTest` — тесты размеров bitmap-иконок для кластеров.

### 9.2 Android / UI тесты ⚠️

Существующие:
- `ParksRootScreenTest` — общий тест экрана parks.

Отсутствуют:
- `ParkMapViewTest` — тесты MapView, cluster rendering, tap handling;
- `ParkInfoCardTest` — тесты карточки парка;
- `MyLocationFabTest` — тесты FAB и геолокации.

### 9.3 Ручная проверка

Обязательная ручная проверка:
- Android 8.0 / API 26;
- современный эмулятор с Android 14+;
- реальное устройство;
- сценарий сворачивания/возврата в приложение;
- поворот экрана;
- первое открытие карты без сохранённого `cameraPosition`;
- подтверждение, что parks видны без нажатия `MyLocationFab`;
- клик по кластеру с фактическим перелётом камеры;
- выдача permission с нуля и отказ в permission;
- acceptance / cancel системного resolution dialog геолокации;
- плохая сеть / отсутствие сети;
- пустой результат фильтра.

### 9.4 Команды проверки

```bash
make lint
make test
make android-test
make test-all
```

`make test-all` и ручная проверка обязательны для DoD, потому что `MapView` и GL-рендеринг плохо проверяются только unit-тестами.

## Зависимости между этапами

```text
Этап 1: зависимости и импорты ✅
    ->
Этап 2: MapUiState ✅
    ->
Этап 3: ViewModel + events + UserNotifier ✅
    ->
Этап 4: ParkMapView + кластеризация + lifecycle ✅
     ->
Этап 5: ParkInfoCard + MyLocationFab ✅
     ->
Этап 6: интеграция в ParksRootScreen ✅
    ->
Этап 7: геолокация и device settings ✅
    ->
Этап 8: style + cache (glyph 404 не блокирующий)
    ->
Этап 9: тестирование
```

## Критерии завершения

| Этап | Критерий |
|------|----------|
| 1 | Проект собирается с `org.maplibre.gl:android-sdk` и правильными import'ами `org.maplibre.android.*` |
| 2 | `MapUiState` хранит `selectedParkId`, `cameraPosition` и location-state без зависимости от `org.maplibre.android.*` |
| 3 | ViewModel обновляет состояние карты, очищает `selectedParkId`, если парк исчез из `filteredParks`, и не управляет навигацией напрямую |
| 4 | Карта показывает одиночные точки и кластеры, использует один экземпляр `MapView`, стартует с полезной initial camera, меняет viewport при смене города/набора parks, корректно переживает lifecycle (ON_START/ON_RESUME/ON_PAUSE/ON_STOP/ON_LOW_MEMORY/ON_DESTROY) и восстанавливается после recreation из `MapUiState` |
| 5 | Tooltip / `ParkInfoCard` открывается по тапу на маркер и закрывается по dismiss / tap outside ✅ |
| 6 | MAP tab использует тот же отфильтрованный набор данных, что и LIST tab ✅ |
| 7 | FAB надёжно центрирует карту, корректно отрабатывает permission/settings/result flow, не оставляет бесконечный loading и не шумит error-логами на ожидаемом `RESOLUTION_REQUIRED` |
| 8 | Карта грузит стиль `https://tiles.openfreemap.org/styles/liberty` или выбранный fallback-style, glyph issue задокументирован и обработан, а странные style-level логи (`glyph 404`, `Canceled`, `Invalid geometry in line layer`) классифицированы на blocking / non-blocking |
| 9 | Проходят `make lint`, `make test`, `make android-test`, `make test-all`, плюс выполнена ручная проверка |

Дополнение к DoD по текущему блоку багфиксов:
- при выборе города с `2+` уникальными координатами parks карта использует `fit bounds`, а не только `city center + zoom=11`;
- при выборе города, где все parks реально совпадают в одной координате, пользователь видит ожидаемо одну точку или один кластер, и это подтверждается диагностическим логом `unique=1`;
- после успешного `CenterOnUser` следующий `onCitySelected(...)` меняет viewport на город и не откатывается назад к `userLocation` без нового нажатия FAB.
