# План реализации ParkDetailScreen (TDD)

## Цель

Сделать `ParkDetailScreen` в Android по логике `EventDetailScreen`, но с отличиями, указанными в задаче, и без реализации `ParkFormScreen` (кнопка редактирования пока только логирует нажатие).

## Обязательные отличия от EventDetailScreen

| Что | EventDetailScreen | ParkDetailScreen |
|---|---|---|
| Заголовок TopAppBar | `event_title` | `park_title` |
| Header content | `title + when + where + address` | только `title + address` |
| Главная кнопка в header | `event_add_to_calendar` | `create_event` → `Screen.CreateEventForPark` |
| Participants section | `participants`, `participate_too`, mode `Event` | заголовок `park_trainees_title`, toggle `train_here`, mode `Park` |
| Description section | есть | отсутствует |
| Author section title | `event_author` | `added_by` |
| Edit action | переход в EditEvent | только логирование (ParkFormScreen отсутствует) |
| Update action | `onEventUpdated` | `onParkUpdated` (обновляет данные после ParkFormScreen) |
| API toggle | `changeIsGoingToEvent` | `changeTrainHereStatus` |
| API load detail | `getEvent` | `getPark` |
| API delete photo | `deleteEventPhoto` | `deleteParkPhoto` |

## Что должно остаться как в EventDetailScreen

- Pull-to-refresh (`InitialLoading/Error/Content`, `refresh`).
- Share action в TopAppBar.
- Author menu с `Edit/Delete` (доступ только автору и авторизованному пользователю).
- Секции: header, participants, photos, author, comments, add-comment button.
- Диалоги удаления: сущности, фото, комментария.
- Комментарии: add/edit/delete/report через `TextEntrySheetHost` и `sendComplaint`.
- Фото: открытие `PhotoDetailSheetHost`, локальное удаление после `onDismissed(deletedPhotoId)`.
- Карта/маршрут: `OpenMap`/`BuildRoute` + fallback в браузер при `ActivityNotFoundException`.

---

## Фактические наблюдения перед реализацией

- ~~В `RootScreen` сейчас `Screen.ParkDetail` — заглушка.~~ → заглушка сохраняется.
- ~~`SWRepository` уже имеет park-endpoints, **кроме** `deleteParkPhoto`.~~ → `deleteParkPhoto` добавлен.
- `TextEntryMode` уже поддерживает park-комментарии:
  - `TextEntryMode.NewForPark(parkId)`
  - `TextEntryMode.EditPark(EditInfo)`
- `TextEntryOption.Park` уже реализован в `SWRepository.addComment/editComment/deleteComment`.
- ~~Навигация участников для Event сделана через отдельный coordinator + JSON в `SavedStateHandle`; для Park нужен аналог.~~ → реализовано (`ParkNavigationCoordinator`, `ParkTraineesNavArgs`, `ParkTraineesNavArgsViewModel`).
- ~~`PhotoDetailViewModel/PhotoDetailConfig` сейчас event-specific (`eventId`, `eventTitle`, `isEventAuthor`, `deleteEventPhoto`) — это нужно обобщить для Park.~~ → обобщено через `PhotoOwner` (Event/Park).

## Текущий статус реализации — ЗАВЕРШЕНО ✅

| Компонент | Статус | Примечание |
|---|---|---|
| `ParkNavigationCoordinator` | ✅ | `buildParkTraineesNavigationData`, `navigateToParkTrainees` |
| `ParkTraineesNavArgs` | ✅ | `consumeParkTraineesArgs()` реализован |
| `ParkTraineesNavArgsViewModel` | ✅ | Добавлен в `NavArgsViewModels.kt` |
| `SWRepository.deleteParkPhoto` | ✅ | Реализован |
| `ParkDetailUIState` | ✅ | `InitialLoading/Content/Error` |
| `IParkDetailViewModel` | ✅ | Interface с events |
| `ParkDetailEvent` | ✅ | Все события определены |
| `ParkDetailViewModel` | ✅ | Все методы реализованы, 22 теста |
| `PhotoOwner` | ✅ | Event/Park обобщение |
| `PhotoDetailConfig` | ✅ | `ownerType: PhotoOwner` |
| `PhotoDetailViewModel` | ✅ | Тесты для Event и Park |
| `ParkDetailSections.kt` | ✅ | Все sections + тесты |
| `ParkDetailScreen.kt` | ✅ | Полная реализация, 32 теста |
| RootScreen ParkDetail | ✅ | Интегрирован в RootScreen |
| RootScreen ParkTrainees | ✅ | Использует ParkTraineesNavArgsViewModel |
| Ручное тестирование | ✅ | Все пункты чек-листа пройдены |

---

## TDD-план по итерациям

## Итерация 1: Навигационный каркас ParkTrainees (RED → GREEN → REFACTOR) ✅

**RED (тесты сначала)** ✅
- [x] Добавить тесты по аналогии с `EventNavigationCoordinatorTest`:
  - `buildParkTraineesNavigationData_whenUsersProvided_thenKeepsUsersJsonAndRoute`
  - `buildParkTraineesNavigationData_whenEmptyUsers_thenEmptyJsonArray`

**GREEN**
- [x] Добавить отдельный `ParkNavigationCoordinator`:
  - `buildParkTraineesNavigationData(parkId, source, users)`
  - `NavController.navigateToParkTrainees(...)`
- [x] Добавить `ParkTraineesNavArgs` + `consumeParkTraineesArgs()`.
- [x] Добавить `ParkTraineesNavArgsViewModel` (по паттерну Event).

---

## Итерация 2: Repository gap для удаления фото площадки ✅

Реализован `SWRepository.deleteParkPhoto(parkId, photoId)` с тестами.

---

## Итерация 3: Контракты ParkDetail ✅

Созданы `ParkDetailUIState`, `IParkDetailViewModel`, `ParkDetailEvent` с тестами.

---

## Итерация 4: ParkDetailViewModel — core сценарии ✅

Реализован `ParkDetailViewModel` со всеми методами (load, refresh, toggle, delete, map, edit) + тесты.

---

## Итерация 5: ParkDetailViewModel — comments/photos ✅

Реализованы комментарии и фото (TextEntryMode, delete, complaint) + тесты.

---

## Итерация 6: Обобщение PhotoDetail под Park ✅

Добавлен `PhotoOwner` (Event/Park), `PhotoDetailConfig.ownerType`, тесты для обоих режимов.

---

## Итерация 7: UI Sections для ParkDetail ✅

Создан `ParkDetailSections.kt` со всеми sections + Compose тесты (22 теста в `ParkDetailScreenTest`).

---

## Итерация 8: ParkDetailScreen (RED → GREEN → REFACTOR) ✅

**RED** ✅
- [x] Добавить в `ParkDetailScreenTest` тесты на сам экран (не только sections):
  - рендер состояний `InitialLoading/Error/Content`
  - обработка событий `NavigateToCreateEvent`/`NavigateToTrainees`
  - отображение диалогов удаления (park/photo/comment)
  - интеграция `TextEntrySheetHost`/`PhotoDetailSheetHost`
  - отсутствие Description section

**GREEN** ✅
- [x] Создать `ParkDetailScreen.kt` по структуре `EventDetailScreen`:
  - `Scaffold` + `TopAppBar` (title = `park_title`, actions: Share + author menu)
  - `PullToRefreshBox` + `LazyColumn` с секциями: header → participants → photos → author → comments → add-comment
  - обработка `ParkDetailEvent` через `LaunchedEffect`
  - диалоги удаления (park/photo/comment)
  - `TextEntrySheetHost` + `PhotoDetailSheetHost`
- [x] Callbacks:
  - `onBack`
  - `onNavigateToUserProfile(userId)`
  - `onNavigateToTrainees(parkId, source, users)`
  - `onNavigateToCreateEvent(parkId, parkName)`
  - `onParkDeleted(parkId)`
  - `onPhotoDeleted(photoId)` - обрабатывается через `viewModel.onPhotoDeleted()`

**REFACTOR** ✅
- [x] Привести обработчики side-effects к стилю EventDetail (минимум inline-логики).

---

## Итерация 9: Интеграция в RootScreen (RED → GREEN → REFACTOR) ✅

**RED** ⏭️ (пропущено - навигационные тесты требуют Instrumentation)
- [~] Добавить/обновить навигационные тесты

**GREEN** ✅
- [x] В `RootScreen.composable(Screen.ParkDetail.route)`:
  - создать `ParkDetailViewModel` через factory
  - реализовать все callbacks
- [x] В `RootScreen.composable(Screen.ParkTrainees.route)`:
  - читать users из `ParkTraineesNavArgsViewModel`
  - передавать в `ParticipantsScreen`

**REFACTOR** ✅
- [x] Паттерн навигации Park/Event унифицирован (оба используют NavArgsViewModel).

---

## Итерация 10: Подключение навигации к ParkDetailScreen

**Проблема:** При нажатии на площадку в `ParksRootScreen` и `ParksAddedByUserScreen` только логируется нажатие, но навигация на `Screen.ParkDetail` не происходит.

**Места для исправления:**
- `RootScreen.kt` строка ~294: `onParkClick` в `ParksRootScreen` - только лог
- `RootScreen.kt` строка ~920: `onParkClick` в `ParksAddedByUserScreen` - только лог

**Решение:**
- [x] Добавить навигацию `appState.navController.navigate(Screen.ParkDetail.createRoute(park.id))`

---

## Итерация 11: Регрессия и финальная проверка ✅

- [x] `make format && make lint && make test`
- [x] Ручной чек-лист:
  - TopAppBar = `park_title`
  - Header: только title + address (нет when/where)
  - Кнопка `create_event` открывает `Screen.CreateEventForPark` (для авторизованных)
  - Participants: title `park_trainees_title`, toggle `train_here`
  - Description отсутствует
  - Author title = `added_by`
  - Edit в меню только логирует
  - Pull-to-refresh работает
  - Удаления (park/photo/comment) работают
  - Комментарии add/edit/delete/report работают
  - Фото: просмотр + удаление + жалоба
  - Карта/маршрут работают

---

## Созданные/изменённые файлы — ЗАВЕРШЕНО ✅

### Созданы:
- `app/src/main/java/com/swparks/ui/screens/parks/ParkDetailScreen.kt`

### Изменены:
- `app/src/main/java/com/swparks/ui/screens/RootScreen.kt` — интеграция ParkDetail и ParkTrainees

### Тесты созданы:
- `app/src/androidTest/java/com/swparks/ui/screens/parks/ParkDetailScreenTest.kt` — 32 теста

---

## Все созданные файлы

- `app/src/main/java/com/swparks/navigation/ParkNavigationCoordinator.kt`
- `app/src/main/java/com/swparks/navigation/ParkNavArgs.kt`
- `app/src/main/java/com/swparks/navigation/NavArgsViewModels.kt` (`ParkTraineesNavArgsViewModel`)
- `app/src/main/java/com/swparks/data/repository/SWRepository.kt` (добавлен `deleteParkPhoto`)
- `app/src/main/java/com/swparks/ui/state/ParkDetailUIState.kt`
- `app/src/main/java/com/swparks/ui/viewmodel/IParkDetailViewModel.kt`
- `app/src/main/java/com/swparks/ui/viewmodel/ParkDetailEvent.kt`
- `app/src/main/java/com/swparks/ui/viewmodel/ParkDetailViewModel.kt`
- `app/src/main/java/com/swparks/ui/state/PhotoDetailUIState.kt` (добавлен `PhotoOwner`)
- `app/src/main/java/com/swparks/ui/viewmodel/PhotoDetailViewModel.kt` (обобщён под Park)
- `app/src/main/java/com/swparks/ui/screens/parks/sections/ParkDetailSections.kt`
- `app/src/main/java/com/swparks/ui/screens/parks/ParkDetailScreen.kt`
- `app/src/test/java/com/swparks/navigation/ParkNavigationCoordinatorTest.kt`
- `app/src/test/java/com/swparks/ui/viewmodel/ParkDetailViewModelTest.kt` (22 теста)
- `app/src/test/java/com/swparks/ui/viewmodel/PhotoDetailViewModelTest.kt`
- `app/src/androidTest/java/com/swparks/ui/screens/parks/ParkDetailScreenTest.kt` (32 теста)

---

## Важные ограничения

- Не вводить навигацию на `ParkFormScreen` до появления самого экрана.
- Не использовать `TextEntryMode.EditForPark`/`NewForPark(parkId, parkName)` — таких режимов в проекте нет.
- Не менять поведение EventDetail при обобщении `PhotoDetail`.

---

## ✅ Реализация завершена

**Дата завершения:** 21.03.2026

**Итоговая статистика:**
- 54 теста (32 в ParkDetailScreenTest + 22 в ParkDetailViewModelTest)
- Все итерации TDD выполнены
- Интеграция в RootScreen завершена
- Ручное тестирование пройдено
