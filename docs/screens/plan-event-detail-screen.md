# План разработки экрана EventDetailScreen

## Обзор

Разработка экрана детальной информации о мероприятии для Android-приложения по аналогии с iOS-версией (`EventDetailsScreen.swift`), но с адаптацией под Android-ограничения по картам:

* **без платных map snapshot сервисов**
* **без зависимости от Google Static Maps API**
* **без нестабильных публичных static map endpoint-ов**
* **с открытием карты/маршрута через внешний map app или browser**

### Ссылки на референсы

* **iOS:** `SwiftUI-WorkoutApp/SwiftUI-WorkoutApp/Screens/Events/EventDetailsScreen.swift`
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
| Дата проведения      | Форматированная дата                       | `DateFormatter.formatDate()`  | Все                                |
| Место проведения     | Адрес (страна, город)                      | `Text`                        | Все                                |
| Локация площадки     | Адрес + действия "Открыть карту"/"Маршрут" | **НОВЫЙ:** `LocationInfoView` | Все                                |
| Участники            | Количество участников + toggle "Пойду"     | `SectionView` + `FormRowView` | **Только авторизованные**          |
| Фотографии           | Сетка фотографий                           | **НОВЫЙ:** `PhotoSectionView` | Все                                |
| Описание             | Текст описания (без HTML)                  | `Text`                        | Все                                |
| Автор                | Информация об организаторе                 | `UserRowView` + click wrapper | Все (клик — только авторизованные) |
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

### Что БЛОКИРУЕТСЯ для неавторизованных (UI виден, действие недоступно)

| Действие                              | Реализация в первой рабочей версии       |
|---------------------------------------|------------------------------------------|
| Переход на профиль автора мероприятия | Клик логируется, навигация не вызывается |
| Переход на профиль автора комментария | Клик логируется, навигация не вызывается |

### Что ДОСТУПНО всем (включая неавторизованных)

* Просмотр заголовка, описания, даты, места
* Просмотр адреса и координат
* Открытие карты
* Построение маршрута через внешний map app/browser
* Просмотр фотографий
* Просмотр организатора (без перехода на профиль)
* Просмотр комментариев
* Поделиться мероприятием

### Логика `isEventAuthor`

```kotlin
val isEventAuthor: StateFlow<Boolean>
```

Вычисляется во ViewModel на основе:

* текущего пользователя из `UserPreferencesRepository.currentUserId`
* `event.author.id`

**Важно:** все идентификаторы пользователя и мероприятия использовать в одном типе: **`Long`**.

---

## Принцип реализации карты на Android

### Что НЕ используем

* Google Maps Static API
* платные map snapshot сервисы
* публичные нестабильные static map endpoint-ы как production-основу

### Что используем в первой версии

* текстовый адрес
* при необходимости координаты
* кнопку **"Открыть на карте"**
* кнопку **"Построить маршрут"**
* открытие внешнего приложения карты или browser через Intent / map URL

### Почему так

На iOS аналогичный сценарий удобно закрывается через `MapKit`, но на Android нет равноценного встроенного бесплатного системного решения для embedded snapshot-карты. Поэтому на первой итерации оптимальный и устойчивый путь — не встраивать snapshot, а использовать внешний map client.

---

## Этап 0: Подготовка архитектуры

### 0.1. Уточнение модели и контрактов

**Задачи:**

* Зафиксировать, что `eventId`, `authorId`, `currentUserId`, `commentAuthorId` имеют тип `Long`
* Убрать лишнее дублирование состояния
* Оставить back navigation в UI/navigation layer, а не во ViewModel
* Переименовать новый location-компонент в общий, не привязанный к park:

    * `LocationInfoView`
* Зафиксировать, как обрабатывается клик по автору для существующих `UserRowView` и `CommentRowView`

**Критерии завершения:**

* [ ] Типы ID унифицированы
* [ ] Архитектурные решения по карте зафиксированы
* [ ] Компонент называется нейтрально и может переиспользоваться
* [ ] `onBackClick()` не закладывается в интерфейс ViewModel
* [ ] Понятно, нужен ли wrapper или расширение API для клика по автору

---

## Этап 1: UI State и ViewModel с реальной загрузкой

### 1.1. UI State

**Файл:** `app/src/main/java/com/swparks/ui/state/EventDetailUIState.kt`

**Структура:**

```kotlin
sealed class EventDetailUIState {
    data object InitialLoading : EventDetailUIState()

    data class Content(
        val event: Event,
        val address: String
    ) : EventDetailUIState()

    data class Error(
        val message: String?
    ) : EventDetailUIState()
}
```

**Отдельные StateFlow во ViewModel:**

* `isRefreshing: StateFlow<Boolean>`
* `isAuthorized: StateFlow<Boolean>`
* `isEventAuthor: StateFlow<Boolean>`

**Важно:**

* `currentUserId` не хранить внутри `Content`, если он нужен только для вычисления `isEventAuthor`
* `isLoading` внутри `Content` не дублировать, так как initial loading и refresh уже разведены по отдельным state

**Критерии завершения:**

* [ ] Sealed class с состояниями `InitialLoading`, `Content`, `Error`
* [ ] Нет дублирования loading-состояний
* [ ] Нет лишних полей в `Content`

---

### 1.2. ViewModel Interface

**Файл:** `app/src/main/java/com/swparks/ui/viewmodel/IEventDetailViewModel.kt`

**Свойства и методы:**

```kotlin
interface IEventDetailViewModel {
    val uiState: StateFlow<EventDetailUIState>
    val isRefreshing: StateFlow<Boolean>
    val isAuthorized: StateFlow<Boolean>
    val isEventAuthor: StateFlow<Boolean>

    fun onEditClick()
    fun onShareClick()

    fun onParticipantToggle()
    fun onParticipantsCountClick()

    fun onAuthorClick(authorId: Long)
    fun onCommentAuthorClick(authorId: Long)

    fun onOpenMapClick()
    fun onRouteClick()

    fun onPhotoClick(photo: Photo)
    fun onPhotoReportClick(photo: Photo)

    fun onAddCommentClick()
    fun onCommentActionClick(commentId: Long, action: CommentAction)

    fun refresh()
}
```

**Критерии завершения:**

* [ ] Интерфейс определён
* [ ] Нет `onBackClick()`
* [ ] Все пользовательские действия покрыты отдельными методами
* [ ] Действия карты разделены на `onOpenMapClick()` и `onRouteClick()`
* [ ] Методы клика по пользователю принимают `authorId`, а не `commentId`

---

### 1.3. ViewModel Implementation

**Файл:** `app/src/main/java/com/swparks/ui/viewmodel/EventDetailViewModel.kt`

**Зависимости:**

* `SWRepository` — для `getEvent(id: Long)`
* `CountriesRepository` — для получения адреса
* `UserPreferencesRepository` — для авторизации и `currentUserId`
* `Logger` — для логирования
* `UserNotifier` — для обработки ошибок

**Инициализация:**

* Получить `eventId` из `SavedStateHandle`
* Загрузить данные мероприятия через `repository.getEvent(id)`
* Построить адрес через `CountriesRepository`
* Получить `currentUserId` из preferences
* Вычислить `isAuthorized`
* Вычислить `isEventAuthor`

**Логика адреса:**

* Переиспользовать подход из `EventsScreen`
* Поддержать fallback при неполных country/city данных

**Логика загрузки:**

* initial loading → `InitialLoading`
* успешная загрузка → `Content`
* ошибка → `Error`
* pull-to-refresh обновляет данные реально, а не только логирует

**Критерии завершения:**

* [ ] ViewModel реально загружает мероприятие
* [ ] Pull-to-refresh реально обновляет данные
* [ ] Адрес строится через `CountriesRepository`
* [ ] `isAuthorized` и `isEventAuthor` корректно вычисляются
* [ ] Ошибки уходят через `UserNotifier` и/или `Error` state
* [ ] Логирование действий пользователей добавлено

---

## Этап 2: Новый компонент LocationInfoView

### 2.1. Компонент LocationInfoView

**Файл:** `app/src/main/java/com/swparks/ui/ds/LocationInfoView.kt`

**Описание:** Компонент для отображения адреса и действий, связанных с локацией мероприятия, без встроенного snapshot карты.

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

* Иконка/визуальный блок локации
* Текстовый адрес
* При необходимости строка с координатами
* Кнопка "Открыть на карте"
* Кнопка "Построить маршрут"

**Реализация действий:**

* Открытие внешнего map app/browser через Intent
* При отсутствии подходящего приложения — fallback на browser
* В первой итерации допустимо логирование результата открытия

**Критерии завершения:**

* [ ] Компонент отображает адрес
* [ ] Кнопка "Открыть на карте" работает
* [ ] Кнопка "Построить маршрут" работает
* [ ] Есть fallback при отсутствии map app
* [ ] Поддержка темной темы
* [ ] Preview для светлой/темной темы

---

## Этап 3: Новый компонент PhotoSectionView

### 3.1. Компонент PhotoSectionView

**Файл:** `app/src/main/java/com/swparks/ui/ds/PhotoSectionView.kt`

**Описание:** Адаптивная сетка фотографий с поддержкой последующего перехода в галерею.

**Параметры:**

```kotlin
data class PhotoSectionConfig(
    val photos: List<Photo>,
    val canDelete: Boolean,
    val onPhotoClick: (Photo) -> Unit,
    val onDeleteClick: ((Photo) -> Unit)? = null,
    val onReportClick: ((Photo) -> Unit)? = null
)
```

**Структура:**

* 1 фото → 1 столбец
* 2 фото → 2 столбца
* 3+ фото → 3 столбца
* `SWAsyncImage` с закруглениями
* клик по фото → пока логирование / позже навигация в галерею

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
    onShare: (String) -> Unit = {},
    onParticipants: (Long) -> Unit = {},
    onAuthor: (Long) -> Unit = {},
    onGallery: (Long) -> Unit = {},
    onAddComment: (Long) -> Unit = {}
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
        * поделиться — для всех

2. `PullToRefreshBox`

    * реальный refresh через `viewModel.refresh()`

3. Содержимое:

    * title
    * date
    * address
    * `LocationInfoView`
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
* у автора показывать edit action
* для неавторизованного пользователя клик по автору/автору комментария только логируется
* для `UserRowView` использовать внешний clickable wrapper, а для `CommentRowView` предусмотреть расширение API или отдельную обёртку для клика по автору

**Критерии завершения:**

* [ ] Экран отображает все основные секции
* [ ] Pull-to-refresh работает с реальными данными
* [ ] `LoadingOverlayView` показывается на первичной загрузке
* [ ] `ErrorContentView` отображается при ошибке
* [ ] Секция участников скрыта для неавторизованных
* [ ] Кнопка "Добавить комментарий" скрыта для неавторизованных
* [ ] Меню редактирования видно только автору
* [ ] Клики корректно обрабатываются через ViewModel
* [ ] Поддержка темной темы

---

## Этап 5: Локализация

### 5.1. Строковые ресурсы

**Файл:** `app/src/main/res/values/strings.xml`
**Файл:** `app/src/main/res/values-ru/strings.xml`

**Необходимые строки:**

| Ключ                   | EN             | RU                   |
|------------------------|----------------|----------------------|
| `event_detail_title`   | Event          | Мероприятие          |
| `event_participants`   | Participants   | Участники            |
| `event_will_attend`    | I will attend  | Пойду                |
| `event_photos`         | Photos         | Фотографии           |
| `event_description`    | Description    | Описание             |
| `event_author`         | Organizer      | Организатор          |
| `event_comments`       | Comments       | Комментарии          |
| `event_add_comment`    | Add comment    | Добавить комментарий |
| `event_open_map`       | Open on map    | Открыть на карте     |
| `event_build_route`    | Build route    | Построить маршрут    |
| `event_edit`           | Edit event     | Редактировать        |
| `event_share`          | Share          | Поделиться           |
| `event_no_description` | No description | Нет описания         |
| `event_location`       | Location       | Локация              |

**Критерии завершения:**

* [ ] Все строки локализованы
* [ ] Использованы в UI

---

## Этап 6: Интеграция в навигацию

### 6.1. Интеграция в RootScreen / основной NavHost

**Файл:** `RootScreen.kt`

**Добавить:**

* `composable` для `Screen.EventDetail`
* получение `eventId` и `source` из аргументов
* передача callback-ов

**Пример:**

```kotlin
composable(
    route = Screen.EventDetail.route,
    arguments = listOf(
        navArgument("eventId") { type = NavType.LongType },
        navArgument("source") { type = NavType.StringType; defaultValue = "events" }
    )
) { backStackEntry ->
    EventDetailScreen(
        onBack = { navController.popBackStack() },
        onEdit = { id -> /* следующая итерация */ },
        onShare = { /* следующая итерация */ },
        onParticipants = { /* следующая итерация */ },
        onAuthor = { /* следующая итерация */ },
        onGallery = { /* следующая итерация */ },
        onAddComment = { /* следующая итерация */ }
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

* [ ] Навигация на EventDetail работает
* [ ] `eventId` передаётся корректно
* [ ] Кнопка "Назад" работает
* [ ] Экран открывается из списка мероприятий

---

## Этап 7: Тестирование

### 7.1. Unit-тесты ViewModel

**Файл:** `app/src/test/java/com/swparks/ui/viewmodel/EventDetailViewModelTest.kt`

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

### 7.2. UI / Preview

**Добавить Preview для:**

* [ ] `LocationInfoView`
* [ ] `PhotoSectionView` (1, 2, 3+ фото)
* [ ] `EventDetailScreen` (`loading`, `content`, `error`)

### 7.3. UI-сценарии

**Проверить:**

* [ ] Неавторизованный пользователь не видит participants section
* [ ] Неавторизованный пользователь не видит кнопку add comment
* [ ] Авторизованный пользователь видит participants section
* [ ] Автор мероприятия видит edit action
* [ ] При refresh действия корректно блокируются
* [ ] При отсутствии map app срабатывает fallback
* [ ] Ошибка загрузки показывает `ErrorContentView`

---

## Порядок реализации

1. **Этап 0** → Подготовка архитектуры
2. **Этап 1** → UI State + ViewModel + реальная загрузка
3. **Этап 2** → `LocationInfoView`
4. **Этап 3** → `PhotoSectionView`
5. **Этап 4** → `EventDetailScreen`
6. **Этап 5** → Локализация
7. **Этап 6** → Навигация
8. **Этап 7** → Тестирование

---

## Существующие компоненты (переиспользование)

| Компонент            | Файл                          | Статус  |
|----------------------|-------------------------------|---------|
| `SectionView`        | `ui/ds/SectionView.kt`        | ✅ Готов |
| `UserRowView`        | `ui/ds/UserRowView.kt`        | ⚠️ База готова, нужен clickable wrapper |
| `CommentRowView`     | `ui/ds/CommentRowView.kt`     | ⚠️ База готова, нужен click support по автору |
| `LoadingOverlayView` | `ui/ds/LoadingOverlayView.kt` | ✅ Готов |
| `FormCardContainer`  | `ui/ds/FormCardContainer.kt`  | ✅ Готов |
| `FormRowView`        | `ui/ds/FormRowView.kt`        | ✅ Готов |
| `SWAsyncImage`       | `ui/ds/SWAsyncImage.kt`       | ✅ Готов |
| `ErrorContentView`   | `ui/ds/ErrorContentView.kt`   | ✅ Готов |
| `EmptyStateView`     | `ui/ds/EmptyStateView.kt`     | ✅ Готов |

---

## Примечания

1. **Карты:** в первой версии не использовать map snapshot. Вместо этого показывать адрес и открывать внешний map app/browser через Intent.

2. **Реальная загрузка уже в первой рабочей версии:** получение event, адреса и refresh должны быть рабочими сразу. Только часть действий может оставаться на этапе логирования.

3. **Авторизация:** экран должен корректно работать для:

    * **неавторизованных пользователей** — скрыты participants section и add comment
    * **авторизованных пользователей** — доступен полный сценарий просмотра
    * **автора мероприятия** — видно edit action

4. **Фото:** переход в полноценную галерею можно перенести на следующую итерацию. В первой рабочей версии клик по фото может только логироваться.

5. **Комментарии и social actions:** в первой рабочей версии допустимо оставить без API-интеграции, но UI и точки расширения должны быть готовы.

6. **Навигация:** интеграцию делать через `RootScreen`, а не через `Navigation.kt`, так как `Navigation.kt` относится к нижней навигации.

---

## Следующие итерации (после первой рабочей версии)

### Итерация 2 — Реальная навигация

* Переход на профиль автора
* Переход на экран участников
* Переход на экран галереи
* Переход на редактирование мероприятия
* Реальный share flow

### Итерация 3 — API интеграция

* Toggle "Пойду" с optimistic update
* Добавление комментария
* Действия над комментариями
* Редактирование/удаление мероприятия для автора

### Итерация 4 — Расширенный функционал

* Добавление в календарь
* Жалобы на фото/комментарии
* Дополнительные action sheet / menu сценарии
* Отдельное R&D по встроенному preview карты без платных сервисов
