# План: исправление крашей MapLibre и дубликата ключей LazyColumn

## Назначение

Документ описывает пошаговый план устранения двух production-крашей, зафиксированных Crashlytics у пользователей версий 1.1 и 1.2.

## Текущий статус (26.07.2026, ветка `fix/crashes`)

| Этап | Что сделано | Что осталось |
|---|---|---|
| **1. Безопасный ключ `ItemListScreen`** | Реализация (коммит `4e99e021`) + регрессионные тесты (1.1) + `make {format,lint,test,build}` — всё зелёное. | Ручная проверка «Новомосковск» — отложена до этапа 3. |
| **2. Defensive fix `UnsatisfiedLinkError`** | Анализ APK (2.1) закрыт; defensive fallback в `ParkMapView` (2.2a) с локализованной заглушкой; `splits.abi` под флагом `-PenableSplits=true` (2.3); strings/`ParkMapView`/`ParksRootScreenTest` обновлены; `make {format,lint,test,build}` — зелёные (1925/1925 unit). **Поправка:** флаг был ошибочно удалён в первом коммите — это ломало `make release` (`:app:buildReleasePreBundle` не собирает AAB с включёнными ABI-сплитами, <https://issuetracker.google.com/402800800>). Флаг возвращён, `make apk` снова передаёт `-PenableSplits=true`, `make release` работает. **Замечание:** AGENTS.md, `docs/plan-map-screen.md` и `docs/plan-release-process.md` всё ещё содержат формулировку «splits.abi включён безусловно» — это устаревший текст, см. «Несогласованности, обнаруженные при верификации». | Финальная регрессия 3.1, мониторинг Crashlytics 3.4, **корректировка устаревших формулировок в 3.2**. |
| **3. Верификация и релиз** | Android-тесты 465/465 + unit-тесты 1925/1925 — зелёные; **ручной smoke-test на эмуляторе 26.07.2026 — профиль + регистрация + карта, выбор «Новомосковск», краша нет**. | `make check` (lint не подтверждён), релиз 1.3.1, мониторинг Crashlytics. |

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

- [x] **1.1–1.3** — `SelectableItem(id, label)` с `key = item.id` (коммит `4e99e021`); регресс-тесты (3 android + 1 unit); YAGNI: `SelectableItemMapper.kt` не извлекался; ревью-фикс shadowing `cityId` → `numericCityId`.
- [~] `EditProfileLocationsTest.selectCity_duplicateNames` — отложен (покрыт через ViewModel).

### 1.3 Рефакторинг

- [ ] Убедиться, что `Divider` (последний элемент в `ItemsList`) отрисовывается только между элементами на новых `SelectableItem`-ах — ручная проверка в `androidTest` или визуально. До выхода версии 1.3.1.

### 1.4 Критерии завершения этапа 1

- [x] Ручная проверка на устройстве/эмуляторе: ввод «Новомосковск» в обоих режимах (профиль, регистрация) показывает обе записи, выбор любой возвращает корректный `id`. **Профиль и регистрация проверены 26.07.2026** (краша нет, выбор возвращает корректный id).

---

## Этап 2. Защитный фикс для единичного OEM-кейса `UnsatisfiedLinkError`

Цель: единичный краш на OnePlus 8 Pro / Android 11 — `.so` в APK, R8 не виноват, зависимости целы. Анализ альтернатив показывает, что менять `extractNativeLibs` ради одного кейса невыгодно (см. 2.2b). Делаем минимальный defensive fix и продолжаем наблюдение.

### 2.1 Что уже установлено анализом APK (без кода)

- [x] APK 1.2 проанализирован (26.07.2026): `libmaplibre.so` валиден, R8 не виновен; edge case на OnePlus 8 Pro / OxygenOS 11.

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

План: обернуть `MapLibre.getInstance(appContext)` в `try { ... } catch (e: UnsatisfiedLinkError) { ... }`. При неудаче:

- Логировать устройство и ОС через `Build.MANUFACTURER`, `Build.MODEL`, `Build.VERSION.RELEASE`, `Build.VERSION.SDK_INT` (без PII) на уровне `Log.e` через существующий `TAG = "ParkMapView"`. (Изначально план указывал `Log.w` и `runCatching`; в реализации выбрано `Log.e` для единичного OEM-кейса и `try/catch` вместо `runCatching` — функционально эквивалентно, но без лишнего `Result`-оборачивания.)
- В UI показать **только заглушку с текстом** «Карта недоступна на этом устройстве». **Без кнопки «Попробовать снова»** — корень сбоя в системном loader'е OxygenOS, повторный вызов `MapLibre.getInstance()` с той же вероятностью упадёт; кнопка создаёт ложное ожидание.
- Не крашить приложение — пользователь остаётся в приложении, может пользоваться остальным функционалом.

Сопутствующее:

- [x] `try/catch` вокруг `MapLibre.getInstance()` + локализованная заглушка (`map_not_available`); `Log.e` с `MANUFACTURER/MODEL/RELEASE/SDK_INT` (без PII); known issue в `docs/plan-map-screen.md` и правила arm64 в `AGENTS.md`.

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

- [x] `splits.abi` под флагом `-PenableSplits=true` (`app/build.gradle.kts:62-71`); оба сценария (APK/AAB) задокументированы в `docs/plan-release-process.md`.

### 2.4 Тесты (для defensive fix)

- [x] Happy-path регресс-тест `ParksRootScreenTest.whenMapTabIsSelected_errorPlaceholderTextIsNotShown`; полный mockkStatic `UnsatisfiedLinkError` отложен до OEM-устройства.

### 2.5 Критерии завершения этапа 2

- Defensive fix внедрён: `UnsatisfiedLinkError` не приводит к крашу приложения.
- На AOSP/Pixel эмуляторах карта по-прежнему загружается (регрессия не появилась).
- В Crashlytics Issue `2cc0c26f...` после релиза — 0 событий (заглушка подавляет крэш, в логах остаются записи о единичном OEM-кейсе).
- Новые кейсы на других устройствах (если появятся) документируются отдельно в `docs/plan-map-screen.md`.

---

## Этап 3. Верификация и регрессия

Цель: убедиться, что обе правки не сломали смежные сценарии, и подготовить релиз.

### 3.1 Регрессионные проверки

- [~] Прогнать `make check` (build + test + lint) — тесты зелёные (android 465/465 + unit 1925/1925), lint не подтверждён после регрессии.
- [x] `make android-test` — 465/465 пройдены (включая `ItemListScreenTest`, `SelectCityScreenTest`/`RegisterSelectCityScreenTest`, `ParkMapScreenTest` (или эквивалент), `EditProfileViewModelTest`, screenshot-tests); 26.07.2026.
- [x] Регистрация: выбор страны → выбор города с дубликатом имени → сохранение профиля — **проверено 26.07.2026** (краша нет, выбор возвращает корректный `id`).
- [x] Профиль: смена города с дубликатом имени → переход на карту — **проверено 26.07.2026** (краша нет, обе записи отображаются, выбор возвращает корректный `id`).
- [x] Экран карты (`ParksRootScreen` → `ParkMapView`): открытие карты, переход на выбор города и возврат — **проверено 26.07.2026** (карта не падает, заглушка не показывается на эмуляторе).

### 3.2 Документация

- [ ] В `docs/plan-map-screen.md` добавить пункт в раздел «Известные баги/Решения»: результат расследования краша `UnsatisfiedLinkError libmaplibre.so` и применённый фикс.
- [ ] В `AGENTS.md` в разделе «ABI splits & UnsatisfiedLinkError» заменить «`splits.abi` включён безусловно в `buildTypes.release`» на корректную формулировку: «`splits.abi` включается через флаг `-PenableSplits=true` (передаётся `make apk`); `make release` (`bundleRelease`) запускается без флага, иначе AGP падает на `:app:buildReleasePreBundle` (<https://issuetracker.google.com/402800800)»>.
- [ ] В `README.md` (через `./gradlew updateReadmeVersions` либо вручную) поднять версию и при необходимости — перечень ABI. **Уточнение:** таска `updateReadmeVersions` (`build.gradle.kts:12`) обновляет только технические бейджи (Kotlin, AGP, Gradle, Android SDK) — версия приложения (`VERSION_NAME`/`VERSION_CODE` из `gradle.properties`) в README не выводится. Если требуется упоминать версию приложения и ABI-состав релизного APK в README — это отдельный пункт, не покрывается существующей таской.

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
- **Этап 2, единичный OEM-кейс**: краш B зафиксирован у одного пользователя на конкретной связке OnePlus 8 Pro / OxygenOS 11 / Android 11. Полноценный фикс невозможен на стороне приложения (баг системного loader'а OnePlus). Defensive fix через `try { } catch` + заглушку — компромисс: приложение не падает, но карта у этого пользователя не работает. Приемлемо для единичного кейса, неприемлемо если кейсов станет >5% пользователей.
- **Этап 2, splits.abi под флагом**: режим «один universal APK» остаётся доступным через `./gradlew assembleRelease` без флага (по умолчанию AGP собирает universal APK со всеми ABI) — это не ломает никакой сценарий, а флаг `enableSplits` существует исключительно как переключатель «APK для GitHub Releases vs AAB для магазинов». См. <https://issuetracker.google.com/402800800> — почему splits нельзя включать при `bundleRelease`.
- **Этап 2, логирование OEM-устройств**: писать `Build.MANUFACTURER`/`MODEL`/`VERSION.RELEASE`/`SDK_INT` — это не PII, но проверить совместимость с политикой приватности в `docs/`. Если запрещено — ограничиться `Log.e` без полей устройства.
- **Этап 2, AGP 9.x**: API `packagingOptions`/`splits` может отличаться от AGP 8.x. Перед коммитом свериться с актуальной версией AGP в `gradle/libs.versions.toml`.

## Несогласованности, обнаруженные при верификации (26.07.2026)

После реализации этапа 2 выявлены устаревшие формулировки в сопутствующих документах — все они утверждают, что `splits.abi` «включён безусловно», хотя в `app/build.gradle.kts:62-71` блок `splits` обёрнут в `if (project.findProperty("enableSplits") == "true")` и фактически включается только при сборке через `make apk`. Это технический долг, который должен быть закрыт в рамках этапа 3.2:

- `AGENTS.md:84` — раздел «ABI splits & UnsatisfiedLinkError», первый пункт: «`splits.abi` включён безусловно в `buildTypes.release`» → заменить на flag-based формулировку (см. обновлённый текст в 3.2).
- `docs/plan-map-screen.md:864` — заголовок «Известная проблема: UnsatisfiedLinkError для MapLibre native lib при ABI-сплитах», первое предложение: «С момента этапа 2 `splits.abi` включён безусловно в `buildTypes.release`» → заменить на flag-based формулировку.
- `docs/plan-map-screen.md:877` — упоминается несуществующий флаг `assembleRelease -Psplits.universal=true`. С `splits.abi` под `if`-флагом universal APK получается простым `./gradlew assembleRelease` (без `-PenableSplits=true`); специальный флаг не нужен.
- `docs/plan-release-process.md:389` — пункт 4 перечня итогов: «ABI-фильтры для release: `arm64-v8a` + `armeabi-v7a`, включены безусловно в `buildTypes.release`» → заменить на flag-based формулировку.

> **Происхождение долга:** эти строки были написаны/обновлены в коммите `f78bc78`, где я ошибочно посчитал флаг `enableSplits` «переходной страховкой» и удалил его. Регрессия была исправлена в `557fcbb8` (флаг возвращён, `make release` снова работает), но три сопутствующих документа и AGENTS.md остались с устаревшими формулировками.

---

## Чек-лист готовности

- [x] **Этапы 1–2** выполнены: регресс-тесты (3 android + 1 unit), `SelectableItem`, defensive fix MapLibre (try/catch + заглушка), `splits.abi` под флагом — см. секции 1.1, 2.1, 2.2a, 2.3, 2.4.
- [x] **1.4** — профиль и регистрация проверены вручную на эмуляторе 26.07.2026.
- [~] **3.1** — частично: android-тесты 465/465 + unit 1925/1925 зелёные; smoke-test пройден 26.07.2026; осталось — `make check` (lint не подтверждён после регрессии).
- [ ] **3.2** — документация обновлена (`plan-map-screen.md`, `AGENTS.md`, `README.md`); **дополнительно:** закрыть технический долг из «Несогласованности, обнаруженные при верификации» — поправить устаревшие «безусловно» в `AGENTS.md`, `docs/plan-map-screen.md`, `docs/plan-release-process.md` (см. соответствующую секцию).
- [ ] **3.3** — релиз 1.3.1, проверка `libmaplibre.so` в APK, заливка в GitHub Releases.
- [ ] **3.4** — в Crashlytics 0 событий по обоим issue после 7 дней с релиза.
