---

# План разработки экрана EventDetailScreen

## Текущий статус: ~75% завершено

### ✅ Выполнено
* Этап 0: Подготовка архитектуры
* Этап 1: UI State + ViewModel + реальная загрузка
* Этап 2: LocationInfoView + MapUriSet + DateFormatter
* Этап 6: Навигация (EventsScreen → EventDetailScreen → OtherUserProfile)
* Factory метод в EventDetailViewModel с createSavedStateHandle()
* RootScreen использует viewModel() с factory вместо remember()
* Тесты: MapUriSetTest, DateFormatterTest
* Локализация: основные строки (event_title, event_open_map, event_build_route, event_add_to_calendar, when, where, participants, address, delete, cancel, back)

### ⏳ В работе / Не начато
* **Этап 3:** PhotoSectionView (компонент не создан)
* **Этап 4:** EventDetailScreen (дописать: PhotoSectionView, описание, toggle "Пойду", add comment, edit/share)
* **Этап 5:** Локализация (дописать недостающие строки: event_will_attend, event_description, event_add_comment, event_edit, event_share)
* **Этап 7:** EventDetailViewModelTest (файл не создан)

### 🔧 Исправленные баги
* **Март 2026:** Исправлен баг с пересозданием EventDetailViewModel при навигации после смены темы
  * Проблема: ViewModel пересоздавалась при возврате на экран после смены темы на ThemeIconScreen
  * Причина: Использование remember(navBackStackEntry, appContainer) - при restoreState создавался новый NavBackStackEntry, что приводило к смене ключа remember и созданию нового экземпляра ViewModel
  * Решение: Использование viewModel() с factory методом, использующим createSavedStateHandle(), что позволяет Navigation Compose правильно управлять жизненным циклом ViewModel

---

## Обзор

Разработка экрана детальной информации о мероприятии для Android-приложения по аналогии с iOS-версией (`EventDetailsScreen.swift`), но с адаптацией под Android-ограничения по картам:

* **без платных map snapshot сервисов**
* **без зависимости от Google Static Maps API**
* **без нестабильных публичных static map endpoint-ов**
* **с открытием карты/маршрута через внешний map app или browser**

### Ссылки на референсы

* **iOS:** `SwiftUI-WorkoutApp/SwiftUI-WorkoutApp/Screens/Events/EventDetailsScreen.swift`
* **iOS фото-секция:** `SwiftUI-WorkoutApp/SwiftUI-WorkoutApp/Screens/Common/PhotoSection/PhotoSectionView.swift`
* **iOS фото full screen:** `SwiftUI-WorkoutApp/SwiftUI-WorkoutApp/Screens/Common/PhotoSection/PhotoDetailScreen.swift`
* **Android маршруты:** `app/src/main/java/com/swparks/navigation/Destinations.kt` (`Screen.EventDetail` уже определён)
* **Android корневая навигация:** `RootScreen.kt` (интеграция нового экрана должна выполняться здесь)
* **Референс по адресам:** `EventsScreen` / `EventsViewModel` (логика получения адреса через `CountriesRepository`)
* **Референс по авторизации:** `ProfileRootScreen` / `UserPreferencesRepository.currentUserId`

---

## Структура экрана (сверху вниз)

| Секция               | Описание                                   | Компонент                     | Авторизация                        |
|----------------------|--------------------------------------------|-------------------------------|------------------------------------|
| Заголовок            | Локализованный текст "Event"               | `Text`                        | Все                                |
| Название             | Крупным шрифтом title мероприятия          | `Text`                        | Все                                |
| Когда                | Форматированная дата                       | `Row` + `Text` + `Spacer`     | Все                                |
| Где                  | Место (страна, город)                      | `Row` + `Text` + `Spacer`     | Все                                |
| Адрес                | Адрес площадки                             | `Row` + `Text` + `Spacer`     | Все                                |
| Локация площадки     | Адрес + действия "Открыть карту"/"Маршрут" | **НОВЫЙ:** `LocationInfoView` | Все                                |
| Календарь            | Кнопка "Добавить в календарь" для предстоящего события | `Button`            | Все, только для `isCurrent`        |
| Участники            | Количество участников + toggle "Пойду"     | `SectionView` + `FormRowView` | **Только авторизованные**          |
| Фотографии           | Сетка фотографий                           | **НОВЫЙ:** `PhotoSectionView` | Все                                |
| Описание             | Текст описания (без HTML)                  | `Text`                        | Все                                |
| Автор                | Информация об организаторе                 | `UserRowView`                 | Все (клик — только авторизованные) |
| Комментарии          | Список комментариев                        | `CommentRowView` + доработка click по автору | Все                    |
| Добавить комментарий | Кнопка добавления                          | `Button`                      | **Только авторизованные**          |

---

## Авторизация и различия функционала

### Что СКРЫВАЕТСЯ для неавторизованных (UI не отображается)

| Элемент                       | Условие скрытия  |
|-------------------------------|------------------|
| Секция участников целиком     | `!isAuthorized`  |
| Toggle "Пойду на мероприятие" | `!isAuthorized`  |
| Кнопка "Добавить комментарий" | `!isAuthorized`  |
| Меню редактирования           | `!isEventAuthor` |
| Меню удаления мероприятия     | `!isEventAuthor` |
| Действие удаления фото        | `!isEventAuthor` |

### Что БЛОКИРУЕТСЯ для неавторизованных (UI виден, действие недоступно)

| Действие                              | Реализация в первой рабочей версии       |
|---------------------------------------|------------------------------------------|
| Переход на профиль автора мероприятия | Клик логируется, навигация не вызывается |
| Переход на профиль автора комментария | Клик логируется, навигация не вызывается |

### Что ДОСТУПНО всем (включая неавторизованных)

* Просмотр заголовка, описания, даты, места
* Просмотр адреса
* Открытие карты
* Построение маршрута через внешний map app/browser
* Просмотр фотографий
* Просмотр организатора (без перехода на профиль)
* Просмотр комментариев
* Поделиться мероприятием

### Политика интеракций первой рабочей версии

В первой рабочей версии экран уже должен:

* реально загружать данные мероприятия
* реально обновляться через pull-to-refresh
* реально открывать карту и маршрут через внешний map app/browser
* реально открывать системный экран добавления события в календарь через Android Intent для предстоящего события
* реально удалять мероприятие для автора после подтверждения в alert/dialog
* реально удалять фото мероприятия для автора после подтверждения в alert/dialog

При этом остальные пользовательские действия на экране пока только логируются через `Logger`:

* toggle "Пойду на мероприятие" — только для авторизованных пользователей
* клик по количеству участников
* клик по автору мероприятия
* клик по автору комментария
* клик по фото
* клик "Поделиться"
* клик "Редактировать"
* клик "Добавить комментарий"

### Логика `isEventAuthor`

```kotlin
val isEventAuthor: StateFlow<Boolean>
```

Вычисляется во ViewModel на основе:

* текущего пользователя из `UserPreferencesRepository.currentUserId`
* `event.author.id`

**Важно:** все идентификаторы пользователя и мероприятия использовать в одном типе: **`Long`**.

### Правило авторских действий

Действия, изменяющие мероприятие, доступны только авторизованному пользователю, который является автором мероприятия (`isEventAuthor == true`):

* редактирование мероприятия
* удаление мероприятия
* удаление фото мероприятия

### Что НЕ переносим из iOS в первую Android-версию

Из iOS-референса берём общую верстку, порядок секций и условия отображения, но не переносим платформенные или пока не реализованные сценарии:

* жалоба на фото
* жалоба на комментарий
* полноэкранный просмотр фото как обязательную часть первой версии

---

## Принцип реализации карты на Android

### Что НЕ используем

* Google Maps Static API
* платные map snapshot сервисы
* публичные нестабильные static map endpoint-ы как production-основу

### Что используем в первой версии

* текстовый адрес
* координаты используются только для открытия карты/маршрута (не отображаются в UI)
* кнопку **"Открыть на карте"**
* кнопку **"Построить маршрут"**
* открытие внешнего приложения карты или browser через Intent / map URL

### Почему так

На iOS аналогичный сценарий удобно закрывается через `MapKit`, но на Android нет равноценного встроенного бесплатного системного решения для embedded snapshot-карты. Поэтому на первой итерации оптимальный и устойчивый путь — не встраивать snapshot, а использовать внешний map client.

---

## Этап 0: Подготовка архитектуры [ГОТОВО]

* [x] Типы ID унифицированы (`Long`)
* [x] Архитектурные решения по карте зафиксированы (без map snapshot)
* [x] Компонент переименован в `LocationInfoView`
* [x] `onBackClick()` не в интерфейсе ViewModel
* [x] Контракт удаления фото описан до repository
* [x] Источник `eventId` — `SavedStateHandle` из маршрута
* [x] Компоненты с `onClick`: `UserRowView` (enabled/onClick), `CommentRowView` (onAuthorClick), `FormRowView` (onClick)

---

## Этап 1: UI State и ViewModel с реальной загрузкой [ГОТОВО]

### Реализовано

* [x] `EventDetailUIState` (sealed class: InitialLoading, Content, Error)
* [x] `IEventDetailViewModel` — интерфейс с 21 методом
* [x] `EventDetailViewModel` — полная реализация:
  * Загрузка мероприятия через `SWRepository.getEvent()`
  * Pull-to-refresh с реальным обновлением данных
  * Адрес через `CountriesRepository`
  * `isAuthorized` и `isEventAuthor` через `UserPreferencesRepository`
  * Удаление мероприятия с confirm dialog
  * Удаление фото с confirm dialog
  * Добавление в календарь через Android Intent
  * Обработка ошибок через `UserNotifier`
  * **Factory метод с `createSavedStateHandle()`** — позволяет Navigation Compose правильно управлять жизненным циклом ViewModel
* [x] `deleteEventPhoto(eventId, photoId)` добавлен в `SWRepository`
* [x] Factory метод в `AppContainer` с `@Suppress("TooManyFunctions")`

---

## Этап 2: Новый компонент LocationInfoView [ГОТОВО]

**Файл:** `app/src/main/java/com/swparks/ui/ds/LocationInfoView.kt`

**Описание:** Компонент для отображения действий, связанных с локацией мероприятия, без встроенного snapshot карты. Адрес отображается отдельно в родительском компоненте.

**Параметры:**

```kotlin
data class LocationInfoConfig(
    val latitude: String?,
    val longitude: String?,
    val address: String,
    val onOpenMapClick: () -> Unit,
    val onRouteClick: () -> Unit
)
```

**Структура:**

* Кнопка "Открыть на карте"
* Кнопка "Построить маршрут"

**Реализация действий:**

* Открытие внешнего map app/browser через Intent
* При отсутствии подходящего приложения — fallback на browser
* В первой итерации допустимо логирование результата открытия

**Критерии завершения:**

* [x] Кнопка "Открыть на карте" работает
* [x] Кнопка "Построить маршрут" работает
* [x] Есть fallback при отсутствии map app
* [x] Поддержка темной темы
* [x] Preview для светлой/темной темы

### 2.2. Уточнение структуры блока даты/места/адреса (по iOS референсу) [ГОТОВО]

**Файл:** `app/src/main/java/com/swparks/ui/screens/events/EventDetailScreen.kt`

**Требование по верстке:**

* После заголовка мероприятия использовать вертикальный блок из горизонтальных строк (`Row`)
* Строка 1: слева **"Когда"** (жирный), справа форматированная дата
* Строка 2: слева **"Где"** (жирный), справа место в формате страна/город (например, "Россия, Москва")
* Строка 3: слева **"Адрес"** (жирный), справа текст адреса площадки (например, "Парк Победы, Поклонная гора")
* Во всех строках использовать `Spacer(modifier = Modifier.weight(1f))` между левым и правым текстом
* Координаты пользователю не показывать
* Строка "Адрес" отображается только если `event.address` не пустой

**Критерии завершения:**

* [x] Добавлены 3 строки `Когда` / `Где` / `Адрес` в `EventDetailScreen`
* [x] Левый текст в каждой строке отображается жирным (`FontWeight.Bold`)
* [x] В каждой строке используется `Spacer(weight = 1f)` для разнесения левого и правого текста
* [x] Строка `Адрес` выводит `event.address` (условное отображение при наличии адреса)

### 2.3. Рефакторинг: вынести формирование URI в отдельный data class [ГОТОВО]

**Задача:** Создать data class для инкапсуляции логики формирования map URI.

**Файл:** `app/src/main/java/com/swparks/ui/model/MapUriSet.kt`

**Целевая реализация:**

```kotlin
package com.swparks.ui.model

import android.net.Uri

/**
 * Набор URI для работы с картой.
 * Создаётся на основе координат и предоставляет готовые URI для разных сценариев.
 */
data class MapUriSet(
    latitude: Double,
    longitude: Double
) {
    /**
     * geo: URI для открытия в нативном приложении карты.
     * Формат: geo:lat,lng?q=lat,lng
     */
    val geoUri: Uri = "geo:$latitude,$longitude?q=$latitude,$longitude".toUri()

    /**
     * HTTPS URI для открытия в браузере (поиск точки).
     * Формат: https://maps.google.com/?q=lat,lng
     */
    val browserUri: Uri = "https://maps.google.com/?q=$latitude,$longitude".toUri()

    /**
     * google.navigation: URI для запуска навигации.
     * Формат: google.navigation:q=lat,lng
     */
    val navigationUri: Uri = "google.navigation:q=$latitude,$longitude".toUri()

    /**
     * HTTPS URI для построения маршрута в браузере.
     * Формат: https://maps.google.com/?daddr=lat,lng
     */
    val browserRouteUri: Uri = "https://maps.google.com/?daddr=$latitude,$longitude".toUri()
}
```

**Использование в ViewModel:**

```kotlin
// EventDetailViewModel
val mapUriSet: MapUriSet?
    get() = uiState.valueOrNull?.event?.let {
        MapUriSet(it.latitude, it.longitude)
    }
```

**Использование в UI:**

```kotlin
val mapUriSet = viewModel.mapUriSet
if (mapUriSet != null) {
    // Используем mapUriSet.geoUri, mapUriSet.navigationUri и т.д.
}
```

**Преимущества:**
* Инкапсуляция логики формирования URI в одном месте
* Простое тестирование через unit-тесты
* Убираем дублирование кода
* UI становится чище и декларативнее

**Unit-тесты:**

**Файл:** `app/src/test/java/com/swparks/ui/model/MapUriSetTest.kt`

**Тест-кейсы:**

```kotlin
class MapUriSetTest {
    @Test
    fun geoUri_is_formatted_correctly() {
        val set = MapUriSet(latitude = 55.7558, longitude = 37.6173)
        assertEquals("geo:55.7558,37.6173?q=55.7558,37.6173", set.geoUri.toString())
    }

    @Test
    fun browserUri_is_formatted_correctly() {
        val set = MapUriSet(latitude = 55.7558, longitude = 37.6173)
        assertEquals("https://maps.google.com/?q=55.7558,37.6173", set.browserUri.toString())
    }

    @Test
    fun navigationUri_is_formatted_correctly() {
        val set = MapUriSet(latitude = 55.7558, longitude = 37.6173)
        assertEquals("google.navigation:q=55.7558,37.6173", set.navigationUri.toString())
    }

    @Test
    fun browserRouteUri_is_formatted_correctly() {
        val set = MapUriSet(latitude = 55.7558, longitude = 37.6173)
        assertEquals("https://maps.google.com/?daddr=55.7558,37.6173", set.browserRouteUri.toString())
    }

    @Test
    fun negative_coordinates_are_handled_correctly() {
        val set = MapUriSet(latitude = -33.8688, longitude = -151.2093)
        assertEquals("geo:-33.8688,-151.2093?q=-33.8688,-151.2093", set.geoUri.toString())
    }

    @Test
    fun zero_coordinates_are_handled_correctly() {
        val set = MapUriSet(latitude = 0.0, longitude = 0.0)
        assertEquals("geo:0.0,0.0?q=0.0,0.0", set.geoUri.toString())
    }
}
```

**Критерии завершения:**

* [x] Создан data class `MapUriSet` в `ui/model/`
* [x] ViewModel предоставляет `mapUriSet` как вычисляемое свойство
* [x] EventDetailScreen использует `MapUriSet` вместо прямого формирования URI
* [x] Unit-тесты для `MapUriSet` написаны и проходят
* [x] Сборка и линтинг проходят успешно

### 2.4. Рефакторинг: убрать парсинг даты из view-слоя [ГОТОВО]

**Проблема:** в `EventDetailScreen` есть локальная функция `parseEventDateToMillis()`, которая дублирует ISO-парсинг и нарушает разделение ответственности (UI не должен содержать date-parsing логику).

**Почему это важно:**
* в проекте уже есть общая утилита `DateFormatter.parseIsoDate()` с теми же поддерживаемыми форматами ISO
* при дублировании парсинга легко получить расхождение форматов между экранами
* тестируемость и сопровождаемость выше, когда парсинг сосредоточен в одном месте

**План рефакторинга:**
* [x] Удалить `parseEventDateToMillis()` из `EventDetailScreen`
* [x] Добавить в `DateFormatter` публичный метод для календарного сценария, например `parseIsoDateToMillis(dateString: String): Long?`
* [x] Использовать новый метод в обработке `EventDetailEvent.OpenCalendar`
* [x] Добавить unit-тесты в `DateFormatterTest` для `parseIsoDateToMillis()` на валидных и невалидных форматах
* [x] Убедиться, что `make format` и `make test` проходят после рефакторинга

---

## Этап 3: Новый компонент PhotoSectionView

### 3.1. Компонент PhotoSectionView

**Файл:** `app/src/main/java/com/swparks/ui/ds/PhotoSectionView.kt`

**Описание:** Адаптивная сетка фотографий, визуально ориентированная на iOS-референс, но без Android-сценариев жалоб и удаления фото в первой версии.

**Параметры:**

```kotlin
data class PhotoSectionConfig(
    val photos: List<Photo>,
    val canDelete: Boolean,
    val onPhotoClick: (Photo) -> Unit,
    val onDeleteClick: ((Photo) -> Unit)? = null
)
```

**Структура:**

* 1 фото → 1 столбец
* 2 фото → 2 столбца
* 3+ фото → 3 столбца
* `SWAsyncImage` с закруглениями
* клик по фото → пока логирование / позже навигация в галерею или photo detail

**Важно для первой версии:**

* не показывать действие "Пожаловаться на фото"
* удаление фото доступно только при `isEventAuthor == true`
* полноэкранный photo detail можно отложить на следующую итерацию, если удаление фото решается без него

**Критерии завершения:**

* [ ] Адаптивная сетка 1/2/3 столбца
* [ ] Корректная загрузка изображений
* [ ] Обработка клика на фото
* [ ] Поддержка темной темы
* [ ] Preview функции

---

## Этап 4: UI Screen

### 4.1. EventDetailScreen

**Файл:** `app/src/main/java/com/swparks/ui/screens/events/EventDetailScreen.kt`

**Структура Composable:**

```kotlin
@Composable
fun EventDetailScreen(
    modifier: Modifier = Modifier,
    viewModel: IEventDetailViewModel = viewModel<EventDetailViewModel>(factory = EventDetailViewModel.Factory),
    onBack: () -> Unit,
    onEdit: (Long) -> Unit = {},
    onDelete: (Long) -> Unit = {},
    onShare: (String) -> Unit = {},
    onParticipants: (Long) -> Unit = {},
    onAuthor: (Long) -> Unit = {},
    onGallery: (Long) -> Unit = {},
    onAddComment: (Long) -> Unit = {},
    onAddToCalendar: (Event) -> Unit = {}
)
```

**Основные правила:**

* back обрабатывается через `onBack()`
* UI читает:

  * `uiState`
  * `isRefreshing`
  * `isAuthorized`
  * `isEventAuthor`

**Основные секции:**

1. `Scaffold` с `TopAppBar`

    * Назад
    * Заголовок `event_detail_title`
    * Меню:

        * редактировать — только для автора
        * удалить — только для автора
        * поделиться — для всех

2. `PullToRefreshBox`

    * реальный refresh через `viewModel.refresh()`

3. Содержимое:

    * title
    * date
    * address
    * `LocationInfoView`
    * add to calendar button (`if (event.isCurrent)`)
    * participants section (`if (isAuthorized)`)
    * `PhotoSectionView`
    * description
    * author (`UserRowView`)
    * comments (`CommentRowView`)
    * add comment button (`if (isAuthorized)`)

4. Состояния:

    * `InitialLoading` → `LoadingOverlayView`
    * `Error` → `ErrorContentView`
    * `Content` → основной контент

**Правила интеракций:**

* во время `isRefreshing` отключать кликабельные действия, где это уместно
* у автора показывать edit и delete actions
* для неавторизованного пользователя клик по автору/автору комментария только логируется
* для автора использовать встроенные `enabled`/`onClick` в `UserRowView`, а для `CommentRowView` предусмотреть расширение API или отдельную обёртку для клика по автору
* меню действий комментария в первой версии не показывать, чтобы не вывести неподдерживаемый `REPORT`
* удаление мероприятия и удаление фото должны идти через confirm alert/dialog по аналогии с подтверждением удаления в Journals flow

**Условия отображения секций по iOS-референсу:**

* секция участников отображается только для авторизованных
* строка с количеством участников отображается, если у события есть участники
* toggle "Пойду на мероприятие" отображается только для предстоящего (`isCurrent`) события и только для авторизованных
* кнопка "Добавить в календарь" отображается только для предстоящего (`isCurrent`) события
* секция фотографий отображается, только если фотографии есть
* секция описания отображается, только если описание не пустое
* блок автора и комментарии отображаются всегда

**Критерии завершения:**

* [x] Экран отображает основные секции (title, date, address, author, comments)
* [x] Pull-to-refresh работает с реальными данными
* [x] `LoadingOverlayView` показывается на первичной загрузке
* [x] `ErrorContentView` отображается при ошибке
* [x] Секция участников скрыта для неавторизованных
* [ ] Кнопка "Добавить комментарий" скрыта для неавторизованных
* [x] Меню удаления видно только автору
* [ ] Меню редактирования видно только автору
* [x] Клики корректно обрабатываются через ViewModel
* [ ] Поддержка темной темы (проверить)
* [x] На Android не отображаются жалобы на фото/комментарии
* [x] Кнопка "Добавить в календарь" открывает системный календарь
* [ ] Удаление фото доступно только при `isEventAuthor == true` (требует PhotoSectionView)
* [x] Удаление мероприятия требует явного подтверждения в alert/dialog
* [x] Удаление фото требует явного подтверждения в alert/dialog
* [x] Навигация на профиль автора/комментатора работает (через `onNavigateToUserProfile`)

**Текущее состояние реализации:**

**Реализовано:**
* `title` мероприятия
* Блок `Когда` / `Где` / `Адрес` (LabeledValueRow)
* `LocationInfoView` с кнопками "Открыть на карте" / "Построить маршрут"
* Кнопка "Добавить в календарь" (только для `isCurrent`)
* Секция участников (количество, только для авторизованных)
* Автор (`UserRowView`) с навигацией на профиль
* Комментарии (`CommentRowView`) с навигацией на профиль автора
* Pull-to-refresh
* Состояния `InitialLoading`, `Error`, `Content`
* Delete event dialog
* Delete photo dialog

**Не реализовано:**
* `PhotoSectionView` — компонент не создан
* Описание мероприятия
* Toggle "Пойду на мероприятие"
* Кнопка "Добавить комментарий"
* Edit action в меню TopAppBar
* Share action в меню TopAppBar

---

## Этап 5: Локализация [ЧАСТИЧНО]

### 5.1. Строковые ресурсы

**Файл:** `app/src/main/res/values/strings.xml`
**Файл:** `app/src/main/res/values-ru/strings.xml`

**Необходимые строки:**

| Ключ                               | EN              | RU                   |
|------------------------------------|-----------------|----------------------|
| `event_detail_title`               | Event           | Мероприятие          |
| `event_participants`               | Participants    | Участники            |
| `event_will_attend`                | I will attend   | Пойду                |
| `event_photos`                     | Photos          | Фотографии           |
| `event_description`                | Description     | Описание             |
| `event_author`                     | Organizer       | Организатор          |
| `event_comments`                   | Comments        | Комментарии          |
| `event_add_comment`                | Add comment     | Добавить комментарий |
| `event_open_map`                   | Open on map     | Открыть на карте     |
| `event_build_route`                | Build route     | Построить маршрут    |
| `event_add_to_calendar`            | Add to calendar | Добавить в календарь |
| `event_edit`                       | Edit event      | Редактировать        |
| `event_delete`                     | Delete event    | Удалить мероприятие  |
| `event_delete_photo`               | Delete photo    | Удалить фото         |
| `event_delete_confirm_title`       | Delete?         | Удалить?             |
| `event_delete_photo_confirm_title` | Delete photo?   | Удалить фото?        |
| `common_delete`                    | Delete          | Удалить              |
| `common_cancel`                    | Cancel          | Отмена               |
| `event_share`                      | Share           | Поделиться           |
| `event_no_description`             | No description  | Нет описания         |
| `event_location`                   | Location        | Локация              |

**Критерии завершения:**

* [x] Основные строки локализованы
* [x] Использованы в UI

**Реализованные строки:**

| Ключ                               | EN              | RU                   | Статус |
|------------------------------------|-----------------|----------------------|--------|
| `event_title`                      | Event           | Мероприятие          | ✅      |
| `event_open_map`                   | Open on map     | Открыть на карте     | ✅      |
| `event_build_route`                | Build route     | Построить маршрут    | ✅      |
| `event_add_to_calendar`            | Add to calendar | Добавить в календарь | ✅      |
| `event_delete_confirm_title`       | Delete event?   | Удалить мероприятие? | ✅      |
| `event_delete_photo_confirm_title` | Delete photo?   | Удалить фото?        | ✅      |
| `when`                             | When            | Когда                | ✅      |
| `where`                            | Where           | Где                  | ✅      |
| `participants`                     | Participants    | Участники            | ✅      |
| `address`                          | Address         | Адрес                | ✅      |
| `delete`                           | Delete          | Удалить              | ✅      |
| `cancel`                           | Cancel          | Отмена               | ✅      |
| `back`                             | Back            | Назад                | ✅      |

**Отсутствующие строки (потребуются для оставшихся секций):**

| Ключ                | EN            | RU                   | Приоритет                      |
|---------------------|---------------|----------------------|--------------------------------|
| `event_will_attend` | I will attend | Пойду                | Высокий (для toggle)           |
| `event_description` | Description   | Описание             | Высокий (для секции описания)  |
| `event_add_comment` | Add comment   | Добавить комментарий | Высокий (для кнопки)           |
| `event_edit`        | Edit event    | Редактировать        | Средний (для меню)             |
| `event_share`       | Share         | Поделиться           | Средний (для меню)             |
| `event_photos`      | Photos        | Фотографии           | Средний (для PhotoSectionView) |
| `event_author`      | Organizer     | Организатор          | Низкий (опционально)           |
| `event_comments`    | Comments      | Комментарии          | Низкий (опционально)           |

---

## Этап 6: Интеграция в навигацию [ГОТОВО]

### 6.1. Интеграция в RootScreen / основной NavHost

**Файл:** `RootScreen.kt`

**Реализовано:**

* [x] `composable` для `Screen.EventDetail`
* [x] получение `eventId` и `source` из аргументов через `SavedStateHandle`
* [x] передача callback-ов
* [x] **Использование `viewModel()` с factory вместо `remember()`** — обеспечивает корректное управление жизненным циклом ViewModel при навигации

**Текущая реализация:**

```kotlin
composable(
    route = Screen.EventDetail.route,
    arguments = listOf(
        navArgument("eventId") { type = NavType.LongType },
        navArgument("source") { type = NavType.StringType; defaultValue = "events" }
    )
) {
    val eventDetailViewModel = viewModel<EventDetailViewModel>(
        factory = EventDetailViewModel.factory(
            swRepository = appContainer.swRepository,
            countriesRepository = appContainer.countriesRepository,
            userPreferencesRepository = appContainer.userPreferencesRepository,
            userNotifier = appContainer.userNotifier,
            logger = appContainer.logger
        )
    )

    EventDetailScreen(
        viewModel = eventDetailViewModel,
        onBack = { navController.popBackStack() },
        onNavigateToUserProfile = { userId ->
            navController.navigate(Screen.OtherUserProfile.createRoute(userId, "events"))
        }
    )
}
```

### 6.2. Обновление источника перехода

**Файл:** `EventsScreen` / `EventsViewModel` / место клика по карточке мероприятия

**Добавить:**

* переход на `Screen.EventDetail`
* передача `eventId`
* передача `source`

**Критерии завершения:**

* [x] Навигация на EventDetail работает
* [x] `eventId` передаётся корректно через `SavedStateHandle`
* [x] Кнопка "Назад" работает (`navController.popBackStack()`)
* [x] Экран открывается из списка мероприятий (`EventsScreen.onNavigateToEventDetail`)
* [x] Навигация на профиль пользователя работает (`onNavigateToUserProfile`)
* [x] `source` параметр передаётся (default: "events")

**Реализованная навигация:**

* `EventsScreen` → `EventDetailScreen` через `onNavigateToEventDetail(eventId)`
* `EventDetailScreen` → `OtherUserProfileScreen` через `onNavigateToUserProfile(userId)`
* `EventDetailScreen` → Back через `onBack()`

---

## Этап 7: Тестирование [ЧАСТИЧНО]

### 7.0. Выполненные тесты

**MapUriSetTest** ✅ (`app/src/test/java/com/swparks/ui/model/MapUriSetTest.kt`)
* [x] `geoUri_is_formatted_correctly`
* [x] `browserUri_is_formatted_correctly`
* [x] `navigationUri_is_formatted_correctly`
* [x] `browserRouteUri_is_formatted_correctly`
* [x] Тесты с отрицательными координатами
* [x] Тесты с нулевыми координатами

**DateFormatterTest** ✅ (`app/src/test/java/com/swparks/util/DateFormatterTest.kt`)
* [x] `parseIsoDateToMillis` для ISO 8601 с секундами
* [x] `parseIsoDateToMillis` для ISO 8601 с fractional seconds
* [x] `parseIsoDateToMillis` для ISO 8601 с Z
* [x] `parseIsoDateToMillis` для short date
* [x] `parseIsoDateToMillis` для server datetime without timezone
* [x] `parseIsoDateToMillis` для invalid string
* [x] `parseIsoDateToMillis` для empty string
* [x] `parseIsoDateToMillis` консистентность результатов

### 7.1. Unit-тесты ViewModel [НЕ НАЧАТО]

**Файл:** `app/src/test/java/com/swparks/ui/viewmodel/EventDetailViewModelTest.kt` (файл не создан)

**Тест-кейсы:**

* [ ] Успешная загрузка мероприятия
* [ ] Ошибка сети
* [ ] Ошибка сервера
* [ ] Pull-to-refresh повторно загружает данные
* [ ] Адрес собирается через `CountriesRepository`
* [ ] `isAuthorized` корректно вычисляется
* [ ] `isEventAuthor` корректно вычисляется
* [ ] Действия пользователя логируются
* [ ] `onOpenMapClick()` и `onRouteClick()` формируют корректные данные
* [ ] При отсутствии `eventId` ViewModel уходит в `Error` без сетевого запроса
* [ ] Удаление мероприятия вызывает repository endpoint
* [ ] Удаление фото вызывает repository endpoint
* [ ] Удаление мероприятия не вызывает repository до подтверждения в alert/dialog
* [ ] Удаление фото не вызывает repository до подтверждения в alert/dialog
* [ ] **Factory метод корректно создает ViewModel с SavedStateHandle**
* [ ] **ViewModel сохраняет состояние при изменении конфигурации**

### 7.2. UI / Preview [ЧАСТИЧНО]

**Добавить Preview для:**

* [x] `LocationInfoView` — есть для светлой/тёмной темы
* [ ] `PhotoSectionView` (1, 2, 3+ фото)
* [ ] `EventDetailScreen` (`loading`, `content`, `error`)

### 7.3. UI-сценарии

**Проверить:**

* [ ] Неавторизованный пользователь не видит participants section
* [ ] Неавторизованный пользователь не видит кнопку add comment
* [ ] Авторизованный пользователь видит participants section
* [ ] Автор мероприятия видит edit action
* [ ] Автор мероприятия видит delete action
* [ ] При refresh действия корректно блокируются
* [ ] При отсутствии map app срабатывает fallback
* [ ] Ошибка загрузки показывает `ErrorContentView`
* [ ] Toggle "Пойду" виден только для авторизованного пользователя и только для предстоящего события
* [ ] Кнопка "Добавить в календарь" видна только для предстоящего события
* [ ] Жалобы на фото и комментарии не отображаются в UI
* [ ] При удалении мероприятия сначала показывается alert/dialog с кнопками `Удалить` и `Отмена`
* [ ] При удалении фото сначала показывается alert/dialog с кнопками `Удалить` и `Отмена`

---

## Порядок реализации

1. **Этап 0** → Подготовка архитектуры ✅
2. **Этап 1** → UI State + ViewModel + реальная загрузка ✅
3. **Этап 2** → `LocationInfoView` ✅
4. **Этап 6** → Навигация ✅
5. **Исправление бага** → Factory метод с `createSavedStateHandle()` ✅ (Март 2026)
6. **Этап 3** → `PhotoSectionView` ⏳ (следующий приоритет)
7. **Этап 4** → `EventDetailScreen` (дописать оставшиеся секции)
8. **Этап 5** → Локализация (дописать отсутствующие строки)
9. **Этап 7** → Тестирование (EventDetailViewModelTest)

---

## Существующие компоненты (переиспользование)

| Компонент            | Файл                          | Статус                                      |
|----------------------|-------------------------------|---------------------------------------------|
| `SectionView`        | `ui/ds/SectionView.kt`        | ✅ Готов                                     |
| `UserRowView`        | `ui/ds/UserRowView.kt`        | ✅ Готов, поддерживает `enabled` и `onClick` |
| `CommentRowView`     | `ui/ds/CommentRowView.kt`     | ✅ Готов, click по автору уже интегрирован   |
| `LoadingOverlayView` | `ui/ds/LoadingOverlayView.kt` | ✅ Готов                                     |
| `FormCardContainer`  | `ui/ds/FormCardContainer.kt`  | ✅ Готов                                     |
| `FormRowView`        | `ui/ds/FormRowView.kt`        | ✅ Готов                                     |
| `SWAsyncImage`       | `ui/ds/SWAsyncImage.kt`       | ✅ Готов                                     |
| `ErrorContentView`   | `ui/ds/ErrorContentView.kt`   | ✅ Готов                                     |
| `EmptyStateView`     | `ui/ds/EmptyStateView.kt`     | ✅ Готов                                     |

---

## Примечания

1. **Карты:** в первой версии не использовать map snapshot. Вместо этого показывать адрес и открывать внешний map app/browser через Intent.

2. **Реальная загрузка уже в первой рабочей версии:** получение event, адреса и refresh должны быть рабочими сразу. Только часть действий может оставаться на этапе логирования.

3. **Авторизация:** экран должен корректно работать для:

     * **неавторизованных пользователей** — скрыты participants section и add comment
     * **авторизованных пользователей** — доступен полный сценарий просмотра
     * **автора мероприятия** — видны edit/delete action и доступно удаление фото

4. **Фото:** переход в полноценную галерею или photo detail можно перенести на следующую итерацию. В первой рабочей версии клик по фото может только логироваться, но удаление фото для автора должно быть поддержано через confirm alert/dialog.

5. **Комментарии и social actions:** в первой рабочей версии допустимо оставить без API-интеграции, но UI и точки расширения должны быть готовы. Жалобы на фото и комментарии в Android пока не выводить.

6. **Календарь:** если событие предстоящее (`isCurrent == true`), нужно показать кнопку добавления в календарь и открывать системный календарь через стандартный Android Intent без сторонних зависимостей.

7. **Навигация:** интеграцию делать через `RootScreen`, а не через `Navigation.kt`, так как `Navigation.kt` относится к нижней навигации.

8. **ViewModel lifecycle:** Использовать `viewModel()` с factory методом, использующим `createSavedStateHandle()`, для корректного управления жизненным циклом ViewModel при навигации и изменении конфигурации. Это предотвращает пересоздание ViewModel при восстановлении состояния навигации.

---

## Технический долг и улучшения

### Исправленные проблемы

1. **Март 2026: Баг с пересозданием EventDetailViewModel**
   * **Проблема:** ViewModel пересоздавалась при возврате на экран после смены темы
   * **Причина:** Использование `remember(navBackStackEntry, appContainer)` - при `restoreState` создавался новый `NavBackStackEntry`, что приводило к смене ключа `remember`
   * **Решение:** Использование `viewModel()` с factory методом, использующим `createSavedStateHandle()`
   * **Файлы:** `EventDetailViewModel.kt`, `RootScreen.kt`

### Потенциальные улучшения

1. **Фото-секция:**
   * Добавить полноэкранный просмотр фото
   * Добавить жесты для пролистывания фото
   * Оптимизировать загрузку изображений

2. **Комментарии:**
   * Добавить пагинацию для большого количества комментариев
   * Добавить возможность ответа на комментарий
   * Добавить редактирование/удаление своих комментариев

3. **Производительность:**
   * Кэширование адресов в `CountriesRepository`
   * Оптимизация обновлений UI при refresh

4. **UX:**
   * Добавить skeleton loading вместо простого LoadingOverlayView
   * Добавить анимации переходов между секциями
   * Добавить pull-to-refresh indicator в стиле Material 3

---

## Следующие итерации (после первой рабочей версии)

### Итерация 2 — Реальная навигация

* ~~Переход на профиль автора~~ ✅ (реализовано через `onNavigateToUserProfile`)
* Переход на экран участников
* Переход на экран галереи
* Переход на редактирование мероприятия
* Реальный share flow

### Итерация 3 — API интеграция

* Toggle "Пойду" с optimistic update
* Добавление комментария
* Действия над комментариями

### Итерация 4 — Расширенный функционал

* Дополнительные action sheet / menu сценарии
* Отдельное R&D по встроенному preview карты без платных сервисов
