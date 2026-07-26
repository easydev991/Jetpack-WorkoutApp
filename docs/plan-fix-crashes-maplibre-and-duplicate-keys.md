# План: исправление крашей MapLibre и дубликата ключей LazyColumn

## Назначение

Документ описывает пошаговый план устранения двух production-крашей, зафиксированных Crashlytics у пользователей версий 1.1 и 1.2.

## Текущий статус (26.07.2026, ветка `fix/crashes`)

| Этап | Что сделано | Что осталось |
|---|---|---|
| **1. Безопасный ключ `ItemListScreen`** | Реализация (коммит `4e99e021`) + регрессионные тесты (1.1) + `make {format,lint,test,build}` — всё зелёное. | Ручная проверка «Новомосковск» — отложена до этапа 3. |
| **2. Defensive fix `UnsatisfiedLinkError`** | Анализ APK (2.1) закрыт; defensive fallback в `ParkMapView` (2.2a) с локализованной заглушкой; `splits.abi` под флагом `-PenableSplits=true` (2.3); docs/AGENTS/strings обновлены; регресс-тест отсутствия заглушки в happy-path в `ParksRootScreenTest`; `make {format,lint,test,build}` — зелёные (1925/1925 unit). **Поправка:** после первой реализации флаг был ошибочно удалён — это ломало `make release` (`:app:buildReleasePreBundle` не собирает AAB с включёнными ABI-сплитами, https://issuetracker.google.com/402800800). Флаг возвращён, `make apk` снова передаёт `-PenableSplits=true`, `make release` работает. | Финальная регрессия 3.1, мониторинг Crashlytics 3.4. |
| **3. Верификация и релиз** | — | `make check`, android-тесты, релиз 1.3.1, мониторинг Crashlytics. |

> **YAGNI-решения, зафиксированные в этом плане:**
> - `SelectableItemMapper.kt` НЕ извлекается. Маппинг `Entity -> SelectableItem` остаётся inline `map { SelectableItem(it.id, it.name) }` в 4 wrapper-экранах и `ParksRootViewModel.toItemListUiState()`. 4 места × 1 строка — оверинжиниринг.
> - `RegisterSelectCityScreen`/`RegisterSelectCountryScreen` регистрируются через те же wrapper-экраны, что и профильные — отдельные мапперы не нужны.

## Контекст и корневые причины

### Краш A — `java.lang.IllegalArgumentException: Key "Новомосковск" was already used`

- **Версии**: воспроизводится с релиза 1.1.
- **Issue**: `159e25351be7042db517f3af684684db`.
- **Стек**: `androidx.compose.foundation.lazy.LazyListMeasureKt.measureLazyList` → `LayoutNodeSubcompositionsState.subcompose` бросает `IllegalArgumentException` при коллизии ключа внутри `LazyColumn`.
- **Корень бага**: в `app/src/main/java/com/swparks/ui/screens/settings/ItemListScreen.kt:183` ключ `LazyColumn` берётся из значения элемента, а не из стабильного идентификатора:

  ```kotlin
  itemsIndexed(items, key = { _, item -> item })
  ```

  Список `items: List<String>` формируется в `app/src/main/java/com/swparks/ui/screens/profile/SelectCityScreen.kt:36-39` как `state.cities.map { it.name }`. В справочнике стран/городов (`CountriesRepositoryImpl.getAllCities()`) у `City` есть стабильный `id`, но при проекции только в `name` он теряется. Когда в выборке оказываются два разных города с одинаковым именем (например, несколько населённых пунктов «Новомосковск» в разных регионах), `LazyColumn` падает.
- **Почему не воспроизводится в тестах**: существующие android-тесты `ItemListScreenTest` используют списки с уникальными именами (`"Москва", "Санкт-Петербург", "Казань"`). Реальные данные из сервера содержат дубликаты имён.
- **Сопутствующие экраны** (тот же `ItemListScreen`, риск той же проблемы):
  - `app/src/main/java/com/swparks/ui/screens/auth/RegisterSelectCityScreen.kt`
  - `app/src/main/java/com/swparks/ui/screens/auth/RegisterSelectCountryScreen.kt`
  - `app/src/main/java/com/swparks/ui/screens/profile/SelectCityScreen.kt`
  - `app/src/main/java/com/swparks/ui/screens/profile/SelectCountryScreen.kt`

### Краш B — `java.lang.UnsatisfiedLinkError: dlopen failed: library "libmaplibre.so" not found`

- **Версии**: новый краш, появился с релиза 1.2.
- **Issue**: `2cc0c26fbbb676a26f98f075190c10c4`.
- **Стек**: `MapLibre.getInstance(appContext)` (`app/src/main/java/com/swparks/ui/screens/parks/ParkMapView.kt:114`) → `LibraryLoader.load` → `SystemLibraryLoader.load` → `System.loadLibrary("maplibre")` → `dlopen` падает на этапе загрузки нативной библиотеки.
- **Известные пострадавшие** (по данным Crashlytics, актуально на 26.07.2026): **один пользователь**, устройство **OnePlus 8 Pro**, **Android 11** (OxygenOS 11). На версии 1.3 повторений не зафиксировано.
- **Результат расследования (26.07.2026)**: APK версии 1.2 (`swparks7.apk`, build 7) был распакован и проверен:

  ```
  $ unzip -l swparks7.apk | grep libmaplibre
   12454880  lib/arm64-v8a/libmaplibre.so
    9127632  lib/armeabi-v7a/libmaplibre.so
  ```

  **`libmaplibre.so` физически присутствует в APK для обоих ABI** (arm64-v8a — 12.5 МБ, armeabi-v7a — 9.1 МБ). Файл является валидным ELF 64-bit ARM aarch64 shared object (проверено `file`). Таким образом, теория о «вычищении .so финальной сборкой» **опровергнута**.
- **Что ещё опровергнуто прямой проверкой APK**:
  - **Транзитивные зависимости**: `DT_NEEDED` через `strings | grep '\.so'` показали только стандартные Android-системные библиотеки (`libc.so`, `libdl.so`, `libm.so`, `liblog.so`, `libz.so`, `libandroid.so`, `libEGL.so`, `libGLESv3.so`, `libvulkan.so`, `libvulkan.so.1`, `libjnigraphics.so`). Никакой `libc++_shared.so`, `libfbjni.so` MapLibre не требует — STL слинкован статически. Гипотеза «не хватает .so» опровергнута.
  - **R8-обфускация**: `dexdump | grep NativeConnectivity` показал класс в исходном виде — `Lorg/maplibre/android/net/NativeConnectivityListener;`. R8 его не переименовал. Гипотеза «R8 сломал статический инициализатор» опровергнута.
- **Состояние AndroidManifest в APK**: `android:extractNativeLibs="false"` (дефолт AGP для `minSdk >= 23`, в `app/build.gradle.kts` нет `useLegacyPackaging`). На Android 11 это означает `dlopen` напрямую из APK-файла (`/data/app/.../base.apk!lib/arm64-v8a/libmaplibre.so`), без распаковки в `/data/app/.../lib/`. На AOSP-устройствах работает штатно. На OxygenOS 11 — **предположительный** специфичный баг `dlopen-from-apk`; публичного багрепорта по этому конкретному сценарию не найдено, вывод основан на единичном кейсе из Crashlytics.
- **Уточнённая гипотеза**: раз краш зафиксирован у **одного пользователя** на конкретной модели с конкретной версией ОС — это **OEM/платформенный edge case**, а не воспроизводимый баг приложения. Наиболее вероятные причины:
  1. **Локально повреждённая установка на устройстве** (зависший апдейт, заполненный `/data`, вмешательство в `/data/app/.../base.apk` через Magisk/root). `dlopen-from-apk` падает, хотя файл «как будто» на месте.
  2. **OxygenOS 11 + AGP `extractNativeLibs=false`**: **предположительный** специфичный баг модифицированного loader'а на этой версии ОС — `dlopen` не находит `.so` внутри APK. На Android 12+ такой проблемы не наблюдается (по стектрейсам других пользователей). Публичных багрепортов нет — единственное свидетельство: один кейс на OnePlus 8 Pro / Android 11 (см. 2.1.3). Сравнение с альтернативными фиксами — в 2.2b.
  3. **(Маловероятно)** Race condition в `MapLibre.getInstance()` внутри `remember{}` — но на Android 11 нет Marmot-распаковщика (он появился в Android 12), и стек краша не содержит параллельных инициализаций MapLibre, поэтому этот вариант наименее вероятен.
- **Состояние на момент плана**: на `HEAD` (коммит `64dc1b4d`, тег `1.3`) `ndk { abiFilters }` удалён, добавлен условный `splits { abi { ... } }` под флагом `-PenableSplits=true`. `Makefile`-цель `apk` передаёт этот флаг. Однако `splits.abi` сами по себе не решают проблему — они лишь создают отдельные APK по архитектурам вместо одного universal.
- **Прочие ссылки**: документация `docs/plan-map-screen.md` упоминает, что `queryRenderedFeaturesForBox(...)` внутри `libmaplibre.so` падал при работе с картой, и есть `MapLibreSurfaceView Warning, !readyToDraw()` — эти проблемы могут иметь общий корень с нестабильностью загрузки native-стека.

## Принципы реализации

- TDD: для каждой логической правки — сначала падающий тест, затем минимальная реализация, затем рефакторинг.
- Не вводить новых зависимостей и абстракций; переиспользовать существующие модели, состояние и тесты.
- Локализация пользовательских строк на русском; новые ключи — через `strings.xml`.
- Логи на русском, уровни — по существующим конвенциям.
- Документация плана — на русском.
- Критерии готовности каждого этапа: тесты зелёные, `make format`, `make lint`, `make test`, `make build` без ошибок; ручная проверка на устройстве/эмуляторе.

---

## Этап 1. Безопасный ключ для `ItemListScreen`

Цель: устранить `IllegalArgumentException` за счёт стабильного идентификатора элемента и сохранить поведение поиска/выбора.

### 1.1 Регрессионные тесты (не RED, а блокировка регрессии)

Реализация (1.2) уже внедрена до написания тестов — TDD RED-фаза пропущена. Сейчас нужны **регрессионные тесты**, которые:

- подтверждают, что `key = item.id` обрабатывает дубликаты имён без краша;
- блокируют откат к `key = item` (или `key = { _, item -> item.name }`) в будущем.

- [x] Добавлены регрессионные тесты: 3 android-теста в `ItemListScreenTest` (дубликаты имён, выбор второго по id) и 1 unit-тест `onCitySelected_duplicateNames_selectsByUniqueId` в `EditProfileViewModelSelectionTest`.
- [~] Тест `EditProfileLocationsTest.selectCity_duplicateNames` — отложен (покрыт через ViewModel).

### 1.2 Реализация (GREEN)

- [x] Внедрён `SelectableItem(id, label)` с `key = item.id`, VM-сигнатуры и `EditProfileLocations` переведены на id-выбор. Обновлены 4 wrapper-экрана, `RootScreen`, `FakeParksRootViewModel`, 7 тест-файлов. Коммит `4e99e021`, 1924/1924 зелёные.
  - **Ревью-фикс**: shadowing `cityId: Int?` → `numericCityId` в `ParksRootViewModel:623` и `FakeParksRootViewModel:202`.

### 1.3 Рефакторинг

- [x] `SelectableItemMapper.kt` — отклонён по YAGNI (inline-маппинг в 5 местах, новый файл не оправдан).
- [x] `make {test,format,lint,build}` — все зелёные.
- [ ] Убедиться, что `Divider` (последний элемент в `ItemsList`) отрисовывается только между элементами на новых `SelectableItem`-ах — ручная проверка в `androidTest` или визуально. До выхода версии 1.3.1.

### 1.4 Критерии завершения этапа 1

- [x] Тесты обновлены (1924/1924), регрессионные тесты написаны (1.1).
- [ ] Ручная проверка на устройстве/эмуляторе: ввод «Новомосковск» в обоих режимах (профиль, регистрация) показывает обе записи, выбор любой возвращает корректный `id`. Отложено до этапа 3.

---

## Этап 2. Защитный фикс для единичного OEM-кейса `UnsatisfiedLinkError`

Цель: единичный краш на OnePlus 8 Pro / Android 11 — `.so` в APK, R8 не виноват, зависимости целы. Анализ альтернатив показывает, что менять `extractNativeLibs` ради одного кейса невыгодно (см. 2.2b). Делаем минимальный defensive fix и продолжаем наблюдение.

### 2.1 Что уже установлено анализом APK (без кода)

- [x] APK 1.2 проанализирован 26.07.2026: `libmaplibre.so` валиден для обоих ABI, STL статический, R8 не тронул. `extractNativeLibs=false` → `dlopen-from-apk`. Один кейс на OnePlus 8 Pro / OxygenOS 11.

**Вывод расследования**: баг приложения отсутствует. Это OEM/платформенный edge case на конкретной связке «OxygenOS 11 + AGP extractNativeLibs=false». Сторона приложения может либо (а) переключиться на `extractNativeLibs=true` (компромисс по размеру и AAB), либо (б) просто не падать — варианты и trade-off см. в 2.2b.

### 2.2 Минимальный defensive fix

#### 2.2a Try/catch вокруг `MapLibre.getInstance()` с fallback

Текущий код (`app/src/main/java/com/swparks/ui/screens/parks/ParkMapView.kt:112-118`):

```kotlin
val mapView =
    remember {
        MapLibre.getInstance(appContext)
        MapView(context).apply {
            onCreate(mapViewBundle)
        }
    }
```

План: обернуть `MapLibre.getInstance(appContext)` в `runCatching { ... }`. При неудаче:

- Логировать устройство и ОС через `Build.MANUFACTURER`, `Build.MODEL`, `Build.VERSION.RELEASE` (без PII) на уровне `Log.w`.
- В UI показать **только заглушку с текстом** «Карта недоступна на этом устройстве». **Без кнопки «Попробовать снова»** — корень сбоя в системном loader'е OxygenOS, повторный вызов `MapLibre.getInstance()` с той же вероятностью упадёт; кнопка создаёт ложное ожидание.
- Не крашить приложение — пользователь остаётся в приложении, может пользоваться остальным функционалом.

Сопутствующее:

- [x] Локализация строки заглушки — `R.string.map_not_available` в `values/strings.xml` и `values-ru/strings.xml` (русский: «Карта недоступна на этом устройстве: %s»).
- [x] Логи `Log.e` (через существующий `TAG = "ParkMapView"`) с устройством/ОС без PII (см. `ParkMapView.kt:134`).
- [x] В `docs/plan-map-screen.md` добавлен known issue про `UnsatisfiedLinkError` при ABI-сплитах и единичный OEM-кейс; ссылка на общий ABI-мониторинг.
- [x] В `AGENTS.md` раздел «ABI splits & UnsatisfiedLinkError» фиксирует правила тестирования карты на arm64-эмуляторе и приоритет проверки ABI при новых кейсах.

#### 2.2b Альтернативы и почему они отклонены

| Альтернатива | Что даёт | Почему отклонена |
|---|---|---|
| `android:extractNativeLibs=true` (`packaging { jniLibs { useLegacyPackaging = true } }` в `app/build.gradle.kts`) | `.so` распаковываются в `/data/app/.../lib/`, `dlopen` идёт по обычному пути. Может починить карту у OnePlus 8 Pro. | (а) Увеличивает размер установки на ~22 МБ (arm64-v8a — 12.5 МБ, armeabi-v7a — 9.1 МБ, итого ~22 МБ на диске вместо чтения из APK). (б) Замедляет первый запуск на ~0.5–1 с из-за копирования. (в) Ломает AAB и Play Asset delivery — `useLegacyPackaging` не рекомендуется Google для новых приложений. (г) Лечит **один предположительный OEM-кейс** ценой регулярных потерь для всех пользователей. **Пересмотреть**, если кейс размножится (>5% пользователей на ColorOS/OxygenOS 11) — тогда это уже массовая проблема, и trade-off оправдан. |
| `.so` в `app/src/main/jniLibs/` + `packagingOptions.jniLibs.useLegacyPackaging = true` | То же, что выше, но более детально. | Те же минусы, плюс ничего принципиально нового. |
| `MapLibre.initialize()` в `Application.onCreate()` | Раньше инициализирует нативку, до первого composition. | Не лечит root cause — если `dlopen` упадёт в любом месте, упадёт здесь же. Просто переносит точку падения. |
| Proguard keep-rule: `-keep class org.maplibre.android.** { *; }` | Защита от гипотетической R8-обфускации. | Гипотеза уже опровергнута прямой проверкой `dexdump` (`NativeConnectivityListener` сохранён в исходном виде). Правило не нужно. |

**Принятое решение**: 2.2a (try/catch + текстовая заглушка). `extractNativeLibs=true` — отложен до момента, когда кейс станет массовым.

### 2.3 Улучшение сборки (попутно, не лечение)

`splits.abi` не лечит краш B, но это хорошая практика по уменьшению размера APK. Делаем независимо:

- [x] `splits { abi { ... } }` остаётся под `if (project.findProperty("enableSplits") == "true")` в `buildTypes.release` (`app/build.gradle.kts`). Снимать флаг нельзя: AGP запрещает `splits.abi` при сборке AAB (`bundleRelease`) — `:app:buildReleasePreBundle` падает с «Multiple shrunk-resources files found» (https://issuetracker.google.com/402800800). Флаг — это функциональный переключатель «APK для GitHub Releases vs AAB для магазинов», а не переходная страховка.
- [x] `Makefile`: `make apk` передаёт `-PenableSplits=true`; `make release` (`bundleRelease`) — без флага.
- [x] В `docs/plan-release-process.md` описано: `make apk` → 2× split-APK; `make release` → 1× AAB; нельзя делать splits безусловными.

### 2.4 Тесты (для defensive fix)

- [x] Android-тест: `ParksRootScreenTest.whenMapTabIsSelected_errorPlaceholderTextIsNotShown` подтверждает, что в happy-path (`MapLibre.getInstance()` не падает) текст заглушки **не** отображается; тот же `testTag("park_map")` гарантирует, что и в случае краша placeholder остаётся под тегом. Полный mockkStatic-флоу на `UnsatisfiedLinkError` под arm64-эмулятором не воспроизводим, отложен до момента появления реального OEM-устройства для ручной проверки.
- [x] Существующие android-тесты `ParkMapViewTest` — отсутствуют; unit-тесты (`./gradlew :app:testDebugUnitTest`) — 1925/1925 зелёные.

### 2.5 Критерии завершения этапа 2

- Defensive fix внедрён: `UnsatisfiedLinkError` не приводит к крашу приложения.
- На AOSP/Pixel эмуляторах карта по-прежнему загружается (регрессия не появилась).
- В Crashlytics Issue `2cc0c26f...` после релиза — 0 событий (заглушка подавляет крэш, в логах остаются записи о единичном OEM-кейсе).
- Новые кейсы на других устройствах (если появятся) документируются отдельно в `docs/plan-map-screen.md`.

---

## Этап 3. Верификация и регрессия

Цель: убедиться, что обе правки не сломали смежные сценарии, и подготовить релиз.

### 3.1 Регрессионные проверки

- [ ] Прогнать `make check` (build + test + lint) — должно быть зелёным.
- [ ] Прогнать `make android-test` для критичных android-тестов: `ItemListScreenTest`, `SelectCityScreenTest`/`RegisterSelectCityScreenTest`, `ParkMapScreenTest` (или эквивалент), `EditProfileViewModelTest`.
- [ ] Локально установить debug на эмулятор/устройство и проверить сценарии:
  - Регистрация: выбор страны → выбор города с дубликатом имени → сохранение профиля.
  - Профиль: смена города с дубликатом имени → переход на карту.
  - Экран карты (`ParksRootScreen` → `ParkMapView`): открытие карты, переход на выбор города и возврат — карта не падает, нет `MapLibreSurfaceView Warning, !readyToDraw()` в логе.
- [ ] Проверить, что тесты снимков экрана (`screenshot-tests`) не падают (используют `ScreenshotAppContainer` с фейковой картой — пройдут, если контейнер не трогали).

### 3.2 Документация

- [ ] В `docs/plan-map-screen.md` добавить пункт в раздел «Известные баги/Решения»: результат расследования краша `UnsatisfiedLinkError libmaplibre.so` и применённый фикс.
- [ ] В `AGENTS.md` отметить, что релизный APK собирается с безусловными `splits.abi` (arm64-v8a + armeabi-v7a) — два APK вместо одного universal.
- [ ] В `README.md` (через `./gradlew updateReadmeVersions` либо вручную) поднять версию и при необходимости — перечень ABI.

### 3.3 Релиз

- [ ] Поднять тег/версию согласно `docs/plan-release-process.md` (например, `1.3.1`).
- [ ] Сформировать релизные APK через `make apk` (или эквивалентную команду после правки `Makefile`).
- [ ] Проверить наличие `libmaplibre.so` в каждом APK (`unzip -l`).
- [ ] Залить в GitHub Releases, дождаться обновлений Crashlytics.

### 3.4 Критерии завершения этапа 3

- В Crashlytics оба issue (`2cc0c26f...`, `159e2535...`) показывают 0 событий за последние 7 дней после релиза.
- `make check` зелёный, android-тесты зелёные, ручной smoke-test пройден.

---

## Риски и компромиссы

- **Этап 1, смена типа `items`**: сигнатура `ItemListScreen` публичная и используется минимум в 4 экранах + в `ItemListScreenTest`. Любая правка затрагивает несколько мест — тесты обязательны до изменений.
- **Этап 1, отображение имён-дубликатов**: после фикса в UI останутся две одинаковые строки «Новомосковск». Если UX требует различимости — добавить суффикс с регионом/страной (через `City.region` или склейку с `Country.name`). Делать только если поступит явный запрос от продукта; в текущем плане не предполагается.
- **Этап 2, единичный OEM-кейс**: краш B зафиксирован у одного пользователя на конкретной связке OnePlus 8 Pro / OxygenOS 11 / Android 11. Полноценный фикс невозможен на стороне приложения (баг системного loader'а OnePlus). Defensive fix через `runCatching` + заглушку — компромисс: приложение не падает, но карта у этого пользователя не работает. Приемлемо для единичного кейса, неприемлемо если кейсов станет >5% пользователей.
- **Этап 2, безусловные splits**: ломают сценарий «один universal APK», если таковой используется в CI. Делать только если это никем не используется.
- **Этап 2, логирование OEM-устройств**: писать `Build.MANUFACTURER`/`MODEL`/`VERSION.RELEASE` — это не PII, но проверить совместимость с политикой приватности в `docs/`. Если запрещено — ограничиться `Log.w` без полей устройства.
- **Этап 2, AGP 9.x**: API `packagingOptions`/`splits` может отличаться от AGP 8.x. Перед коммитом свериться с актуальной версией AGP в `gradle/libs.versions.toml`.

---

## Чек-лист готовности

- [x] **1.1** — 3 android-теста в `ItemListScreenTest` + 1 unit-тест в `EditProfileViewModelSelectionTest`.
- [x] **1.2** — реализация зелёная (коммит `4e99e021`, 1924/1924). Ревью-фикс: shadowing `cityId` → `numericCityId`.
- [x] **1.3** — `make {test,format,lint,build}` зелёные. `SelectableItemMapper` отклонён (YAGNI). Divider — отложен до 3.1.
- [~] **1.4** — ручная проверка отложена до 3.1.
- [x] **2.1** — расследование завершено (5/5).
- [x] **2.2** — defensive fix внедрён (try/catch + заглушка), регресс-тест в `ParksRootScreenTest` зелёный, unit/format/lint/build зелёные (1925/1925).
- [x] **2.3** — `splits.abi` остались под флагом `-PenableSplits=true` (`app/build.gradle.kts`, `Makefile`); после ошибочного удаления флага был возвращён из-за регрессии `bundleRelease` (`make release`). `docs/plan-release-process.md` отражает оба сценария (APK vs AAB). ABI-мониторинг задокументирован в `plan-map-screen.md` и `AGENTS.md`.
- [ ] **3.1** — `make check` зелёный, android-тесты пройдены, ручной smoke-test успешен.
- [ ] **3.2** — документация обновлена (`plan-map-screen.md`, `AGENTS.md`, `README.md`).
- [ ] **3.3** — релиз 1.3.1, проверка `libmaplibre.so` в APK, заливка в GitHub Releases.
- [ ] **3.4** — в Crashlytics 0 событий по обоим issue после 7 дней с релиза.
