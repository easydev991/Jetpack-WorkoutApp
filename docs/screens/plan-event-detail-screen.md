---

# План разработки экрана EventDetailScreen

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
| Дата проведения      | Форматированная дата                       | `DateFormatter.formatDate()`  | Все                                |
| Место проведения     | Адрес (страна, город)                      | `Text`                        | Все                                |
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
* Просмотр адреса и координат
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
* при необходимости координаты
* кнопку **"Открыть на карте"**
* кнопку **"Построить маршрут"**
* открытие внешнего приложения карты или browser через Intent / map URL

### Почему так

На iOS аналогичный сценарий удобно закрывается через `MapKit`, но на Android нет равноценного встроенного бесплатного системного решения для embedded snapshot-карты. Поэтому на первой итерации оптимальный и устойчивый путь — не встраивать snapshot, а использовать внешний map client.

---

## Этап 0: Подготовка архитектуры [ГОТОВО]

- [x] Типы ID унифицированы (`Long`)
- [x] Архитектурные решения по карте зафиксированы (без map snapshot)
- [x] Компонент переименован в `LocationInfoView`
- [x] `onBackClick()` не в интерфейсе ViewModel
- [x] Контракт удаления фото описан до repository
- [x] Источник `eventId` — `SavedStateHandle` из маршрута
- [ ] Понятно, какие компоненты уже поддерживают `onClick` (закрыть при реализации `EventDetailScreen`)

---

## Этап 1: UI State и ViewModel с реальной загрузкой [ГОТОВО]

### Реализовано

- [x] `EventDetailUIState` (sealed class: InitialLoading, Content, Error)
- [x] `IEventDetailViewModel` — интерфейс с 21 методом
- [x] `EventDetailViewModel` — полная реализация:
  - Загрузка мероприятия через `SWRepository.getEvent()`
  - Pull-to-refresh с реальным обновлением данных
  - Адрес через `CountriesRepository`
  - `isAuthorized` и `isEventAuthor` через `UserPreferencesRepository`
  - Удаление мероприятия с confirm dialog
  - Удаление фото с confirm dialog
  - Добавление в календарь через Android Intent
  - Обработка ошибок через `UserNotifier`
- [x] `deleteEventPhoto(eventId, photoId)` добавлен в `SWRepository`
- [x] Factory метод в `AppContainer` с `@Suppress("TooManyFunctions")`

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

* [ ] Экран отображает все основные секции
* [x] Pull-to-refresh работает с реальными данными
* [x] `LoadingOverlayView` показывается на первичной загрузке
* [x] `ErrorContentView` отображается при ошибке
* [x] Секция участников скрыта для неавторизованных
* [ ] Кнопка "Добавить комментарий" скрыта для неавторизованных
* [x] Меню удаления видно только автору
* [ ] Меню редактирования и удаления видно только автору
* [x] Клики корректно обрабатываются через ViewModel
* [ ] Поддержка темной темы
* [x] На Android не отображаются жалобы на фото/комментарии
* [x] Кнопка "Добавить в календарь" открывает системный календарь
* [ ] Удаление фото доступно только при `isEventAuthor == true`
* [x] Удаление мероприятия требует явного подтверждения в alert/dialog
* [x] Удаление фото требует явного подтверждения в alert/dialog

**Что уже есть по факту:**

* экран показывает `title`, дату, адрес, действия карты, кнопку календаря, автора, комментарии, подтверждение удаления мероприятия и подтверждение удаления фото
* `CommentRowView` уже интегрирован с `onAuthorClick`
* пока отсутствуют `LocationInfoView`, `PhotoSectionView`, описание, кнопка добавления комментария, toggle/секция участников и edit/share UI

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
| `event_add_to_calendar`| Add to calendar| Добавить в календарь |
| `event_edit`           | Edit event     | Редактировать        |
| `event_delete`         | Delete event   | Удалить мероприятие  |
| `event_delete_photo`   | Delete photo   | Удалить фото         |
| `event_delete_confirm_title` | Delete? | Удалить? |
| `event_delete_photo_confirm_title` | Delete photo? | Удалить фото? |
| `common_delete`        | Delete         | Удалить              |
| `common_cancel`        | Cancel         | Отмена               |
| `event_share`          | Share          | Поделиться           |
| `event_no_description` | No description | Нет описания         |
| `event_location`       | Location       | Локация              |

**Критерии завершения:**

* [ ] Все строки локализованы
* [ ] Использованы в UI

**Что уже есть по факту:**

* добавлены и используются `event_open_map`, `event_build_route`, `event_add_to_calendar`
* остальные строки из списка этапа ещё не подтверждены как реализованные в текущем UI

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
        onDelete = { id -> /* закрыть экран после успешного удаления */ },
        onShare = { /* следующая итерация */ },
        onParticipants = { /* следующая итерация */ },
        onAuthor = { /* следующая итерация */ },
        onGallery = { /* следующая итерация */ },
        onAddComment = { /* следующая итерация */ },
        onAddToCalendar = { event -> /* открыть Android календарь через Intent */ }
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
* [x] `eventId` передаётся корректно
* [x] Кнопка "Назад" работает
* [ ] Экран открывается из списка мероприятий

**Что уже есть по факту:**

* `Screen.EventDetail` описан в `Destinations.kt`
* `composable` для `Screen.EventDetail` добавлен в `RootScreen`
* `SavedStateHandle` для `EventDetailViewModel` создаётся из `navBackStackEntry`
* переход из `EventsScreen` пока не реализован: `EventsViewModel.onEventClick()` только логирует

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
* [ ] При отсутствии `eventId` ViewModel уходит в `Error` без сетевого запроса
* [ ] Удаление мероприятия вызывает repository endpoint
* [ ] Удаление фото вызывает repository endpoint
* [ ] Удаление мероприятия не вызывает repository до подтверждения в alert/dialog
* [ ] Удаление фото не вызывает repository до подтверждения в alert/dialog

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
| `UserRowView`        | `ui/ds/UserRowView.kt`        | ✅ Готов, поддерживает `enabled` и `onClick` |
| `CommentRowView`     | `ui/ds/CommentRowView.kt`     | ✅ Готов, click по автору уже интегрирован |
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
    * **автора мероприятия** — видны edit/delete action и доступно удаление фото

4. **Фото:** переход в полноценную галерею или photo detail можно перенести на следующую итерацию. В первой рабочей версии клик по фото может только логироваться, но удаление фото для автора должно быть поддержано через confirm alert/dialog.

5. **Комментарии и social actions:** в первой рабочей версии допустимо оставить без API-интеграции, но UI и точки расширения должны быть готовы. Жалобы на фото и комментарии в Android пока не выводить.

6. **Календарь:** если событие предстоящее (`isCurrent == true`), нужно показать кнопку добавления в календарь и открывать системный календарь через стандартный Android Intent без сторонних зависимостей.

7. **Навигация:** интеграцию делать через `RootScreen`, а не через `Navigation.kt`, так как `Navigation.kt` относится к нижней навигации.

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

### Итерация 4 — Расширенный функционал

* Дополнительные action sheet / menu сценарии
* Отдельное R&D по встроенному preview карты без платных сервисов
