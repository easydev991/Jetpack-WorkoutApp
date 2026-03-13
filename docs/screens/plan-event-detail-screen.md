---

# План разработки экрана EventDetailScreen

## Текущий статус: ~96% завершено

### ✅ Выполнено
* Этапы 0-4, 6, 7 завершены
* Реализованы базовые сценарии экрана: загрузка/refresh, карта/маршрут/календарь, участники, удаление события/фото
* Комментарии: `REPORT`, `CREATE/EDIT/DELETE`, `TextEntrySheetHost`, confirm-alert, обновление через refresh
* API комментариев приведён к iOS-контракту (`comment` + корректная обработка non-2xx)
* Покрытие: `MapUriSetTest`, `DateFormatterTest`, `EventDetailViewModelTest`, `SWRepositoryCommentsTest`

### ⏳ В работе / Не начато
* **Этап 5:** EventDetailScreen (дописать: edit/share actions в TopAppBar)
* **Этап 8:** EventDetailViewModelTest (расширить покрытие beyond `REPORT`)

### 🎯 Следующие шаги (приоритет)
1. **Edit/Share actions** — добавить пункты меню в TopAppBar (ViewModel уже готов)
2. **PhotoDetailScreen** — добавить детальный экран фото и перенести туда удаление фото с confirm alert/dialog
3. **EventDetailViewModelTest** — расширить набор unit-тестов ViewModel (сеть/ошибки/factory/navigation)

---

## Обзор

Разработка экрана мероприятия по аналогии с iOS-версией (`EventDetailsScreen.swift`), с адаптацией под Android:

* **Карты:** без платных snapshot сервисов, открытие карты/маршрута через внешний map app/browser
* **Референсы:** iOS `EventDetailsScreen.swift`, `PhotoSectionView.swift`, `PhotoDetailScreen.swift`
* **Интеграция:** через `RootScreen.kt`
* **Авторизация:** через `UserPreferencesRepository.currentUserId`

**Структура экрана:** title, date, address, `LocationInfoView`, calendar, participants, `SwitchFormRowView`, `PhotoSectionView`, description, author, comments, add comment.

**Политика авторизации:**
* неавторизованные — скрыт participants section, add comment disabled
* авторизованные — полный доступ
* автор мероприятия — edit/delete actions, удаление фото

**Первая рабочая версия:**
* реальная загрузка event, адреса, refresh
* открытие карты/маршрута/календаря через Intent
* удаление мероприятия/фото через confirm dialog
* часть действий логируется (edit, share, toggle, comments)

**Не переносим в первой версии:**
* жалоба на фото/комментарий к площадке
* полноэкранный просмотр фото (переносится на `PhotoDetailScreen`)

---

## Этап 0: Подготовка архитектуры [ГОТОВО]

* [x] Унифицированы ID (`Long`) и получение `eventId` из `SavedStateHandle`

---

## Этап 1: UI State и ViewModel с реальной загрузкой [ГОТОВО]

* [x] Реализованы `EventDetailUIState`, `IEventDetailViewModel`, `EventDetailViewModel` (удаление фото, factory)

---

## Этап 2: Компоненты LocationInfoView, MapUriSet, DateFormatter [ГОТОВО]

* [x] Реализованы `LocationInfoView`, `MapUriSet`, `DateFormatter` и блоки `Когда/Где/Адрес` с unit-тестами

---

## Этап 3: SwitchFormRowView для toggle "Пойду" [ГОТОВО]

* [x] `SwitchFormRowView` создан и интегрирован (`isAuthorized`, `isCurrent`, темы, Preview)

---

## Этап 3.1: Доработки секций EventDetailScreen [ГОТОВО]

* [x] Доработаны секции описания/автора, `byMainUser` в комментариях, локализация `event_description`/`event_author`

---

## Этап 3.2: Блокировка кнопок LocationInfoView при refresh [ГОТОВО]

* [x] Добавлен `enabled` в `LocationInfoConfig`/`LocationInfoView`; передаётся `!isRefreshing`

---

## Этап 3.3: Секция комментариев и кнопка Add Comment [ГОТОВО]

* [x] Секция комментариев вынесена в `EventDetailSections.kt`
* [x] Кнопка `Добавить комментарий` всегда видима, `enabled = isAuthorized && !isRefreshing`

---

## Этап 3.4: Интеграция жалобы на чужой комментарий [ГОТОВО]

**Цель:** подключить `CommentAction.REPORT` к отправке email через `Complaint.EventComment`.

**Реализация:**
* [x] `REPORT` подключён через `EventDetailEvent` + `sendComplaint(...)`
* [x] Жалоба формируется в ViewModel без зависимости от `Context`
* [x] Добавлены unit-тесты на базовый и fallback-сценарии

**Критерии завершения:**
* [x] При нажатии `REPORT` открывается почтовый клиент с предзаполненными темой/телом
* [x] Тема/тело письма соответствуют формату iOS `Complaint.eventComment`
* [x] ViewModel не зависит от `Context`
* [x] Сценарий не ломает `EDIT/DELETE` для своих комментариев

---

## Этап 3.5: Интеграция TextEntrySheetHost для комментариев [ГОТОВО]

**Цель:** открыть экран ввода текста для создания/редактирования комментария прямо из EventDetail.

**Реализация:**
* [x] Подключён `TextEntrySheetHost` для `CREATE/EDIT` (`NewForEvent`/`EditEvent`)
* [x] Для `EDIT` передаётся исходный текст комментария
* [x] После успешной отправки выполняется `refresh()` с `isRefreshing`

**Критерии завершения:**
* [x] Создание комментария инициируется через `onAddCommentClick`
* [x] Редактирование комментария инициируется через `onCommentActionClick`
* [x] Используются существующие эндпоинты `addCommentToEvent` и `editEventComment` (через `TextEntryUseCase`)
* [x] После отправки комментария экран обновляется с индикатором refresh

---

## Этап 3.6: Удаление комментария с подтверждением [ГОТОВО]

**Цель:** добавить удаление своего комментария (`CommentAction.DELETE`) по аналогии с дневником, через confirm/cancel alert.

**Реализация:**
* [x] `DELETE` для своего комментария открывает confirm/cancel alert
* [x] Подтверждение вызывает `deleteEventComment` через `SWRepository.deleteComment(...)`
* [x] На время удаления включается `isRefreshing`, после успеха данные обновляются

**Критерии завершения:**
* [x] Есть подтверждение/отмена удаления
* [x] Удаление доступно только для своего комментария
* [x] Запрос удаления соответствует iOS API-контракту
* [x] При ожидании ответа клики по фото/действиям блокируются через `isRefreshing`

---

## Этап 4: Новый компонент PhotoSectionView

### 4.1. Компонент PhotoSectionView

**Файл:** `app/src/main/java/com/swparks/ui/ds/PhotoSectionView.kt`

**Параметры:**

```kotlin
data class PhotoSectionConfig(
    val photos: List<Photo>,
    val enabled: Boolean,
    val onPhotoClick: (Photo) -> Unit
)
```

**Реализация:**
* [x] Адаптивная сетка: 1 фото → 1 столбец, 2 фото → 2 столбца, 3+ фото → 3 столбца
* [x] `SWAsyncImage`, поддержка тем и Preview

**Важно:** удаление фото переносится на `PhotoDetailScreen`.

**Критерии завершения:**
* [x] Реализована адаптивная сетка, загрузка изображений и клик по фото

---

## Этап 5: UI Screen

### 5.1. EventDetailScreen

**Файл:** `app/src/main/java/com/swparks/ui/screens/events/EventDetailScreen.kt`

**Структура:**

```kotlin
@Composable
fun EventDetailScreen(
    modifier: Modifier = Modifier,
    viewModel: IEventDetailViewModel,
    onBack: () -> Unit,
    onNavigateToUserProfile: (Long) -> Unit
)
```

**Реализовано:**
* `Scaffold` с `TopAppBar` (назад, заголовок, меню для автора)
* `PullToRefreshBox` с `viewModel.refresh()`
* Секции: title, date, address, `LocationInfoView`, calendar, participants, `SwitchFormRowView`, `PhotoSectionView`, description, author, comments, add comment
* Состояния: `InitialLoading` → `LoadingOverlayView`, `Error` → `ErrorContentView`, `Content` → основной контент
* Меню удаления видно только автору, подтверждение через alert/dialog
* Навигация на профиль автора/комментатора для авторизованных
* `REPORT` комментария открывает email-клиент

**Условия отображения (iOS-референс):**
* participants section — только для авторизованных
* toggle "Пойду" и кнопка calendar — только для предстоящих (`isCurrent`) событий
* photo section — только если есть фото
* description — только если не пустое
* add comment button — всегда видима, `enabled = isAuthorized && !isRefreshing`

**Критерии завершения:**
* [x] Экран показывает основные секции, pull-to-refresh, состояния загрузки/ошибки
* [x] Секция участников скрыта для неавторизованных
* [x] Кнопка "Добавить комментарий" добавлена
* [x] Меню удаления видно только автору
* [ ] Меню редактирования — UI не добавлен (ViewModel метод готов)
* [ ] Меню "Поделиться" — UI не добавлен (ViewModel метод готов)
* [x] Клики обрабатываются через ViewModel, тёмная тема поддерживается
* [x] `REPORT` открывает email-клиент с `Complaint.EventComment`
* [x] Кнопка "Добавить в календарь" открывает системный календарь
* [x] `PhotoSectionView` подключен
* [x] Удаление мероприятия и фото подтверждается через alert/dialog
* [x] Удаление комментария подтверждается через alert/dialog
* [x] Навигация на профиль работает

**Не реализовано:**
* Edit action в меню TopAppBar — UI не добавлен (ViewModel метод готов)
* Share action в меню TopAppBar — UI не добавлен (ViewModel метод готов)

---

## Этап 6: Локализация [ГОТОВО]

### 6.1. Строковые ресурсы

**Файл:** `app/src/main/res/values/strings.xml`, `app/src/main/res/values-ru/strings.xml`

**Критерии завершения:**
* [x] Основные строки локализованы
* [x] Добавлены `event_photos`, `event_edit`, `event_share`
* [ ] Новые ключи подключены в UI (зависит от этапа 5)

**Примечание:** `add_comment` уже существует.
**Обновлено:** добавлены строки для confirm-алерта удаления комментария.

---

## Этап 7: Интеграция в навигацию [ГОТОВО]

* [x] Экран интегрирован в `RootScreen` с `viewModel()`/factory и навигацией

---

## Этап 8: Тестирование [ЧАСТИЧНО]

### 8.0. Выполненные тесты

* [x] **MapUriSetTest** — форматы URI (`geo`, `browser`, `navigation`, `browserRoute`), edge-cases координат
* [x] **DateFormatterTest** — `parseIsoDateToMillis` для valid/invalid ISO

### 8.1. Unit-тесты ViewModel [ЧАСТИЧНО]

**Файл:** `app/src/test/java/com/swparks/ui/viewmodel/EventDetailViewModelTest.kt`

**Выполнено:**
* [x] Покрыты сценарии `REPORT`, `CREATE`, `EDIT`, `DELETE` комментариев
* [x] Проверены открытие `TextEntry`, confirm удаления и обновление данных

**Осталось:**
* [ ] Успешная загрузка, ошибки сети/сервера
* [ ] Pull-to-refresh, адрес через `CountriesRepository`
* [ ] `isAuthorized`, `isEventAuthor`
* [ ] Действия пользователя, `onOpenMapClick()`, `onRouteClick()`
* [ ] Отсутствие `eventId` → `Error` без запроса
* [ ] Удаление мероприятия/фото (с подтверждением и без)
* [ ] Factory метод с `SavedStateHandle`, сохранение состояния

### 8.2. UI / Preview [ЧАСТИЧНО]

* [x] `LocationInfoView` — светлая/тёмная тема
* [x] `PhotoSectionView` (1, 2, 3+ фото)
* [ ] `EventDetailScreen` (`loading`, `content`, `error`)

### 8.3. UI-сценарии [ПРОВЕРЕНО]

* [x] Неавторизованный не видит `participants section`
* [x] Кнопка add comment видна неавторизованному, но disabled
* [x] Авторизованный видит `participants section`
* [ ] Автор видит edit action
* [x] Автор видит delete action
* [x] При refresh действия корректно блокируются (включая секцию фото)
* [x] При отсутствии map app срабатывает fallback
* [x] Ошибка загрузки показывает `ErrorContentView`
* [x] Toggle "Пойду" и кнопка calendar — только для предстоящих событий
* [x] `REPORT` открывает email-клиент
* [x] Удаление мероприятия/фото через alert/dialog
* [x] Удаление комментария через alert/dialog

---

## Порядок реализации

1. **Этап 0** → Подготовка архитектуры ✅
2. **Этап 1** → UI State + ViewModel ✅
3. **Этап 2** → `LocationInfoView` ✅
4. **Этап 7** → Навигация ✅
5. **Исправление бага** → Factory с `createSavedStateHandle()` ✅ (Март 2026)
6. **Этап 3** → `SwitchFormRowView` ✅
7. **Доработка** → onClickParticipants ✅ (Март 2026)
8. **Этап 3.2** → блокировка `LocationInfoView` при refresh ✅
9. **Этап 3.3** → секция комментариев + Add comment ✅
10. **Этап 3.4** → интеграция `REPORT` через email ✅
11. **Этап 3.5** → интеграция `TextEntrySheetHost` для create/edit комментария ✅
12. **Этап 3.6** → удаление комментария с confirm-alert ✅
13. **Этап 4** → `PhotoSectionView` ✅
14. **Этап 5** → `EventDetailScreen` (дописать: edit/share actions)
15. **Этап 6** → Локализация ✅
16. **Этап 8** → Тестирование (EventDetailViewModelTest)

---

## Существующие компоненты (переиспользование)

| Компонент | Статус |
|-----------|--------|
| `SectionView`, `UserRowView`, `CommentRowView`, `LoadingOverlayView`, `FormCardContainer`, `FormRowView`, `SWAsyncImage`, `ErrorContentView`, `EmptyStateView` | ✅ Готовы |

---

## Примечания

1. **Карты:** без map snapshot, использовать адрес + внешний map app/browser через Intent.
2. **Реальная загрузка:** event, адрес, refresh работают; ключевые действия логируются.
3. **Авторизация:**
   * неавторизованные — скрыт participants section, add comment disabled
   * авторизованные — полный доступ
   * автор — edit/delete actions, удаление фото
4. **Фото:** удаление переносится на `PhotoDetailScreen`.
5. **Комментарии:** `REPORT` через email; `CREATE/EDIT/DELETE` через backend API.
6. **Календарь:** системный Intent для предстоящих событий.
7. **Навигация:** через `RootScreen`.
8. **ViewModel:** `viewModel()` с factory и `createSavedStateHandle()`.
9. **Временная заглушка:** удалена (fake-комментарии больше не используются).

---

## Технический долг и улучшения

* **Фото:** полноэкранный просмотр, жесты, оптимизация загрузки
* **Комментарии:** пагинация, ответы, optimistic UI
* **Производительность:** кэширование адресов, оптимизация refresh
* **UX:** skeleton loading, анимации, Material 3 pull-to-refresh

---

## Следующие итерации

### Итерация 2 — Навигация

* Переход на экран галереи
* Переход на редактирование мероприятия
* Реальный share flow

### Итерация 3 — API интеграция

* Toggle "Пойду" с optimistic update
* Комментарии: optimistic update без полного refresh
* Расширенные действия над комментариями

### Итерация 4 — Расширенный функционал

* Дополнительные action sheet / menu сценарии
* R&D по встроенному preview карты
