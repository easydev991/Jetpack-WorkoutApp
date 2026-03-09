---

# План разработки экрана EventDetailScreen

## Текущий статус: ~90% завершено

### ✅ Выполнено
* Этап 0: Подготовка архитектуры
* Этап 1: UI State + ViewModel + реальная загрузка (все методы реализованы)
* Этап 2: LocationInfoView + MapUriSet + DateFormatter
* Этап 3: SwitchFormRowView (toggle "Пойду" для авторизованных, только если isCurrent)
* Этап 3.1: Доработки секций (EventDescriptionSection, EventAuthorSection в SectionView, byMainUser для комментариев)
* Этап 7: Навигация (EventsScreen → EventDetailScreen → OtherUserProfile)
* Тесты: MapUriSetTest, DateFormatterTest
* Локализация: основные строки (event_title, event_description, event_author, event_open_map, event_build_route, event_add_to_calendar, when, where, participants, address, delete, cancel, back, participate_too, add_comment)
* **Март 2026:** onClickParticipants подключен к ViewModel, логирует нажатие
* **Март 2026:** Удаление мероприятия и фото реализовано с confirm dialogs
* **Март 2026:** Календарь, карта и маршрут работают через system intents

### ⏳ В работе / Не начато
* **Этап 4:** PhotoSectionView (компонент не создан) — следующий приоритет
* **Этап 5:** EventDetailScreen (дописать: PhotoSectionView, add comment button, edit/share actions в TopAppBar)
* **Этап 6:** Локализация (дописать недостающие строки: event_photos, event_edit, event_share)
* **Этап 8:** EventDetailViewModelTest (файл не создан)

### 🎯 Следующие шаги (приоритет)
1. **PhotoSectionView** — создать компонент для отображения сетки фотографий мероприятия
2. **Add comment button** — добавить кнопку "Добавить комментарий" в UI (ViewModel уже готов)
3. **Edit/Share actions** — добавить пункты меню в TopAppBar (ViewModel уже готов)
4. **Локализация** — добавить строки event_photos, event_edit, event_share
5. **EventDetailViewModelTest** — написать unit-тесты для ViewModel

### 🔧 Исправленные баги
* **Март 2026:** Исправлен баг с пересозданием EventDetailViewModel при навигации после смены темы
  * Проблема: ViewModel пересоздавалась при возврате на экран после смены темы на ThemeIconScreen
  * Причина: Использование `remember(navBackStackEntry, appContainer)` — при `restoreState` создавался новый `NavBackStackEntry`
  * Решение: Использование `viewModel()` с factory методом, использующим `createSavedStateHandle()`

* **Март 2026:** onClickParticipants подключен к ViewModel
  * Проблема: Клик по количеству участников логировался прямо в Composable, не проходя через ViewModel
  * Решение: Добавлен параметр `onClickParticipants` в `EventParticipantsSection`, передан `viewModel::onParticipantsCountClick`
  * Результат: Логирование теперь происходит в `EventDetailViewModel.onParticipantsCountClick()`

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

| Секция               | Описание                                               | Компонент                                    | Авторизация                        |
|----------------------|--------------------------------------------------------|----------------------------------------------|------------------------------------|
| Заголовок            | Локализованный текст "Event"                           | `Text`                                       | Все                                |
| Название             | Крупным шрифтом title мероприятия                      | `Text`                                       | Все                                |
| Когда                | Форматированная дата                                   | `Row` + `Text` + `Spacer`                    | Все                                |
| Где                  | Место (страна, город)                                  | `Row` + `Text` + `Spacer`                    | Все                                |
| Адрес                | Адрес площадки                                         | `Row` + `Text` + `Spacer`                    | Все                                |
| Локация площадки     | Адрес + действия "Открыть карту"/"Маршрут"             | **НОВЫЙ:** `LocationInfoView`                | Все                                |
| Календарь            | Кнопка "Добавить в календарь" для предстоящего события | `Button`                                     | Все, только для `isCurrent`        |
| Участники            | Количество участников + toggle "Пойду"                 | `SectionView` + `FormRowView`                | **Только авторизованные**          |
| Фотографии           | Сетка фотографий                                       | **НОВЫЙ:** `PhotoSectionView`                | Все                                |
| Описание             | Текст описания (без HTML)                              | `Text`                                       | Все                                |
| Автор                | Информация об организаторе                             | `UserRowView`                                | Все (клик — только авторизованные) |
| Комментарии          | Список комментариев                                    | `CommentRowView` + доработка click по автору | Все                                |
| Добавить комментарий | Кнопка добавления                                      | `Button`                                     | **Только авторизованные**          |

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
* ~~клик по количеству участников~~ ✅ (подключен к ViewModel, логирует)
* клик по автору мероприятия
* клик по автору комментария
* клик по фото
* клик "Поделиться" — ViewModel метод готов
* клик "Редактировать" — ViewModel метод готов
* клик "Добавить комментарий" — ViewModel метод готов

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

* [x] Типы ID унифицированы (`Long`), архитектурные решения зафиксированы
* [x] `eventId` из `SavedStateHandle`, компоненты с `onClick` подготовлены

---

## Этап 1: UI State и ViewModel с реальной загрузкой [ГОТОВО]

* [x] Реализованы `EventDetailUIState`, `IEventDetailViewModel` (21 метод), `EventDetailViewModel` с загрузкой, refresh, адресом, авторизацией, удалением, календарём
* [x] `deleteEventPhoto(eventId, photoId)` добавлен в `SWRepository`
* [x] Factory метод с `createSavedStateHandle()` для корректного lifecycle management

---

## Этап 2: Компоненты LocationInfoView, MapUriSet, DateFormatter [ГОТОВО]

* [x] `LocationInfoView` — кнопки "Открыть на карте" и "Построить маршрут" через Intent с fallback на browser, поддержка темной темы, Preview
* [x] Структура блока даты/места/адреса — 3 строки `Когда` / `Где` / `Адрес` (LabeledValueRow)
* [x] `MapUriSet` data class — инкапсуляция логики формирования map URI, unit-тесты
* [x] Рефакторинг парсинга даты — `parseIsoDateToMillis()` в `DateFormatter`, unit-тесты

---

## Этап 3: SwitchFormRowView для toggle "Пойду" [ГОТОВО]

* [x] Компонент создан и интегрирован в EventDetailScreen под секцией участников
* [x] Виден только для авторизованных пользователей и только для предстоящих мероприятий (`isCurrent`)
* [x] Клик по toggle логируется, поддержка темной темы, Preview функции

---

## Этап 3.1: Доработки секций EventDetailScreen [ГОТОВО]

* [x] `EventDescriptionSection` — секция с описанием через `parseHtmlOrNull`, обёрнута в `SectionView`, скрыта при отсутствии описания
* [x] `EventAuthorSection` — секция автора обёрнута в `SectionView` с локализованным заголовком `event_author`
* [x] `byMainUser` в комментариях — вычисляется по `currentUserId` из ViewModel, передаётся в `EventCommentItem`
* [x] Локализация — строки `event_description` и `event_author` добавлены в оба файла

---

## Этап 4: Новый компонент PhotoSectionView

### 4.1. Компонент PhotoSectionView

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

## Этап 5: UI Screen

### 5.1. EventDetailScreen

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
    * `SwitchFormRowView` — toggle "Пойду" (`if (isAuthorized && event.isCurrent)`)
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
* [ ] Кнопка "Добавить комментарий" — UI не добавлен (ViewModel метод готов)
* [x] Меню удаления видно только автору
* [ ] Меню редактирования — UI не добавлен (ViewModel метод готов)
* [ ] Меню "Поделиться" — UI не добавлен (ViewModel метод готов)
* [x] Клики корректно обрабатываются через ViewModel
* [x] Поддержка темной темы (все компоненты поддерживают)
* [x] На Android не отображаются жалобы на фото/комментарии
* [x] Кнопка "Добавить в календарь" открывает системный календарь
* [ ] PhotoSectionView не создан — требует реализации
* [x] Удаление мероприятия требует явного подтверждения в alert/dialog
* [x] Удаление фото требует явного подтверждения в alert/dialog (логика готова, UI требует PhotoSectionView)
* [x] Навигация на профиль автора/комментатора работает (через `onNavigateToUserProfile`)

**Не реализовано:**

* `PhotoSectionView` — компонент не создан (следующий приоритет)
* Кнопка "Добавить комментарий" — UI не добавлен (ViewModel метод готов)
* Edit action в меню TopAppBar — UI не добавлен (ViewModel метод готов)
* Share action в меню TopAppBar — UI не добавлен (ViewModel метод готов)

---

## Этап 6: Локализация [ЧАСТИЧНО]

### 6.1. Строковые ресурсы

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
| `participate_too`                  | I am participating | Пойду на мероприятие | ✅   |

**Отсутствующие строки (потребуются для оставшихся секций):**

| Ключ                | EN            | RU                   | Приоритет                      | Статус         |
|---------------------|---------------|----------------------|--------------------------------|----------------|
| `event_photos`      | Photos        | Фотографии           | Высокий (для PhotoSectionView) | ❌ Не добавлен |
| `event_edit`        | Edit event    | Редактировать        | Средний (для меню)             | ❌ Не добавлен |
| `event_share`       | Share         | Поделиться           | Средний (для меню)             | ❌ Не добавлен |

**Примечание:** Строка `add_comment` уже существует (используется в парках), можно переиспользовать или создать `event_add_comment` для контекста мероприятий.

---

## Этап 7: Интеграция в навигацию [ГОТОВО]

* [x] Интеграция в `RootScreen` — `composable` для `Screen.EventDetail` с `viewModel()` и factory методом
* [x] Получение `eventId` и `source` из аргументов через `SavedStateHandle`
* [x] Callback-и: `onBack`, `onNavigateToUserProfile`
* [x] Навигация работает: `EventsScreen` → `EventDetailScreen`, `EventDetailScreen` → `OtherUserProfileScreen`, `EventDetailScreen` → Back

---

## Этап 8: Тестирование [ЧАСТИЧНО]

### 8.0. Выполненные тесты

**MapUriSetTest** ✅ (`app/src/test/java/com/swparks/ui/model/MapUriSetTest.kt`)
* [x] Все URI форматируются корректно (geo, browser, navigation, browserRoute)
* [x] Тесты с отрицательными и нулевыми координатами

**DateFormatterTest** ✅ (`app/src/test/java/com/swparks/util/DateFormatterTest.kt`)
* [x] `parseIsoDateToMillis` для всех ISO форматов и invalid cases

### 8.1. Unit-тесты ViewModel [НЕ НАЧАТО]

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

### 8.2. UI / Preview [ЧАСТИЧНО]

**Добавить Preview для:**

* [x] `LocationInfoView` — есть для светлой/тёмной темы
* [ ] `PhotoSectionView` (1, 2, 3+ фото)
* [ ] `EventDetailScreen` (`loading`, `content`, `error`)

### 8.3. UI-сценарии

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
2. **Этап 1** → UI State + ViewModel + реальная загрузка ✅ (все методы реализованы)
3. **Этап 2** → `LocationInfoView` ✅
4. **Этап 7** → Навигация ✅
5. **Исправление бага** → Factory метод с `createSavedStateHandle()` ✅ (Март 2026)
6. **Этап 3** → `SwitchFormRowView` (toggle "Пойду") ✅
7. **Доработка** → onClickParticipants подключен к ViewModel ✅ (Март 2026)
8. **Этап 4** → `PhotoSectionView` ⏳ (следующий приоритет)
9. **Этап 5** → `EventDetailScreen` (дописать: PhotoSectionView, add comment button, edit/share actions)
10. **Этап 6** → Локализация (дописать: event_photos, event_edit, event_share)
11. **Этап 8** → Тестирование (EventDetailViewModelTest)

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
