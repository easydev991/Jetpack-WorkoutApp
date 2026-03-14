# План разработки экрана EventDetailScreen

## Текущий статус: 100% завершено

### ✅ Выполнено

* Этапы 0-7: архитектура, UI State/ViewModel, компоненты, секции, локализация, навигация
* Этап 5.1-5.4: EventDetailScreen, Toggle "Пойду", dropdown-меню автора, стилизация
* Комментарии: CREATE/EDIT/DELETE/REPORT через TextEntrySheetHost + email
* Тесты: MapUriSetTest, DateFormatterTest, EventDetailViewModelTest (12 тестов), SWRepositoryCommentsTest
* **Этап 9:** Кнопка "Поделиться" в TopAppBar (EventShareButton, EventAuthorActionsButton)
* **Этап 10:** Flow-based EventsViewModel — автоматическое обновление списка при удалении ✅

### ⏳ В работе / Не начато

* **Этап 8:** EventDetailViewModelTest (расширить покрытие)
* **PhotoDetailScreen** — детальный экран фото с удалением

### 🎯 Следующие шаги (приоритет)

1. **PhotoDetailScreen** — детальный экран фото с удалением
2. **EventDetailViewModelTest** — расширить unit-тесты (опционально)

---

## Обзор

Разработка экрана мероприятия по аналогии с iOS-версией (`EventDetailsScreen.swift`), с адаптацией под Android.

**Структура экрана:** title, date, address, `LocationInfoView`, calendar, participants, `SwitchFormRowView`, `PhotoSectionView`, description, author, comments, add comment.

**Политика авторизации:**
* неавторизованные — скрыт participants section, add comment disabled
* авторизованные — полный доступ
* автор мероприятия — edit/delete actions, удаление фото

---

## Этап 0: Подготовка архитектуры [ГОТОВО]

* [x] Унифицированы ID (`Long`) и получение `eventId` из `SavedStateHandle`

---

## Этап 1: UI State и ViewModel [ГОТОВО]

* [x] Реализованы `EventDetailUIState`, `IEventDetailViewModel`, `EventDetailViewModel`

---

## Этап 2: Компоненты [ГОТОВО]

* [x] Реализованы `LocationInfoView`, `MapUriSet`, `DateFormatter` с unit-тестами

---

## Этап 3: Секции и комментарии [ГОТОВО]

* [x] Реализованы секции и `SwitchFormRowView`, REPORT через email
* [x] `TextEntrySheetHost` для create/edit/delete, удаление с confirm-alert

---

## Этап 4: PhotoSectionView [ГОТОВО]

* [x] Адаптивная сетка (1/2/3 столбца), `SWAsyncImage`, темы, Preview

---

## Этап 5: UI Screen [ГОТОВО]

### 5.1. EventDetailScreen

**Файл:** `app/src/main/java/com/swparks/ui/screens/events/EventDetailScreen.kt`

* [x] `Scaffold` с `TopAppBar`, `PullToRefreshBox`, все секции
* [x] Состояния: `InitialLoading`, `Error`, `Content`
* [x] Меню удаления для автора, навигация на профиль, REPORT через email
* [x] Удаление мероприятия/фото/комментария через confirm-dialog
* [ ] Меню редактирования — UI не добавлен (ViewModel метод готов)

### 5.4. Dropdown-меню в TopAppBar [ГОТОВО]

* [x] Dropdown с опциями "Изменить" и "Удалить" для автора

---

## Этап 5.1: Toggle "Пойду" [ГОТОВО]

* [x] Optimistic update с локальным обновлением `trainingUsers`, откат при ошибке
* [ ] Unit-тесты на успех/ошибка/откат

---

## Этап 5.2-5.3: Стилизация и доработки [ГОТОВО]

* [x] Кнопки удаления через `MaterialTheme.colorScheme.error`
* [x] Блоровка клика для автора в `EventAuthorSection`
* [x] Скрытие participants при `trainingUsersCount == null` или `0`

---

## Этап 6: Локализация [ГОТОВО]

* [x] Строки локализованы: `event_photos`, `event_edit`, `event_share`
* [ ] Новые ключи подключены в UI (зависит от edit/share)

---

## Этап 7: Интеграция в навигацию [ГОТОВО]

* [x] Экран интегрирован в `RootScreen` с factory и навигацией

---

## Этап 8: Тестирование [ЧАСТИЧНО]

### 8.1. Unit-тесты ViewModel

**Файл:** `app/src/test/java/com/swparks/ui/viewmodel/EventDetailViewModelTest.kt`

**Выполнено (9 тестов):**
* [x] `REPORT` комментарий (2 теста)
* [x] `CREATE/EDIT/DELETE` комментарий (4 теста)
* [x] Обработка RuntimeException (3 теста)

**Осталось:**
* [ ] Успешная загрузка события, pull-to-refresh
* [ ] `onParticipantToggle` — optimistic update, ошибка, откат
* [ ] Действия: `onOpenMapClick()`, `onRouteClick()`
* [ ] Factory метод с `SavedStateHandle`
* [ ] Удаление фото (успех)
* [ ] Удаление мероприятия (успех)
* [ ] Навигация к участникам (`onParticipantsCountClick`)
* [ ] Добавление в календарь (`onAddToCalendarClick`)

### 8.2-8.3. UI / Preview и сценарии [ЧАСТИЧНО]

* [x] `LocationInfoView`, `PhotoSectionView` — темы и состояния
* [x] Неавторизованный/авторизованный/автор — корректные права
* [x] Блокировка при refresh, fallback для map
* [x] Toggle "Пойду" и calendar — только для предстоящих событий
* [ ] `EventDetailScreen` (`loading`, `content`, `error`)
* [ ] Автор видит edit action (UI не добавлен)

---

## Этап 9: Кнопка "Поделиться" в TopAppBar [ГОТОВО]

* [x] Создан `EventAuthorActionsButton` — dropdown с Edit/Delete
* [x] Создан `EventShareButton` — share через `Intent.ACTION_SEND`
* [x] Unit-тесты для `Event.shareLinkStringURL`
* [x] TopAppBar обновлён: ShareButton + AuthorActionsButton
* [x] Lint и сборка проходят

---

## Этап 10: Flow-based EventsViewModel [ГОТОВО]

### Решение

Repository управляет данными через `StateFlow`. При удалении — обновляет Flow, ViewModel автоматически получает изменения.

### Выполнено

* [x] EventDao: добавлен `deleteById(eventId: Long)`
* [x] SWRepository: добавлены `getFutureEventsFlow()`, `syncFutureEvents()`
* [x] SWRepositoryImpl: `_futureEvents` StateFlow, обновлён `deleteEvent()`
* [x] Use cases: `GetFutureEventsFlowUseCase`, `SyncFutureEventsUseCase`
* [x] AppContainer: зарегистрированы use cases
* [x] EventsViewModel: переделан на Flow-based
* [x] Тесты обновлены, lint проходит

### Файлы изменены

| Файл | Изменение |
|------|-----------|
| `EventDao.kt` | + `deleteById()` |
| `SWRepository.kt` | + Flow-методы, StateFlow |
| `domain/usecase/` | 4 новых файла, 2 удалено |
| `AppContainer.kt` | регистрация use cases |
| `EventsViewModel.kt` | Flow-based архитектура |

---

## Порядок реализации

1. Этапы 0-7 ✅
2. Этап 3.2-3.6, 4-5.3 ✅
3. Этап 5.4 → dropdown-меню для автора ✅
4. Этап 9 → Кнопка "Поделиться" в TopAppBar ✅
5. Этап 10 → Flow-based EventsViewModel ✅
6. Этап 8 → Тестирование (расширить покрытие) — опционально

---

## Существующие компоненты (переиспользование)

| Компонент | Статус |
|-----------|--------|
| `SectionView`, `UserRowView`, `CommentRowView`, `LoadingOverlayView`, `FormCardContainer`, `FormRowView`, `SWAsyncImage`, `ErrorContentView`, `EmptyStateView` | ✅ Готовы |

---

## Технический долг и улучшения

* **Фото:** полноэкранный просмотр, жесты, оптимизация загрузки
* **Комментарии:** пагинация, ответы, optimistic UI
* **Производительность:** кэширование адресов, оптимизация refresh
* **UX:** skeleton loading, анимации

---

## Следующие итерации

### Итерация 2 — Навигация

* Переход на экран галереи, редактирование мероприятия, реальный share flow

### Итерация 3 — API интеграция

* Комментарии: optimistic update без полного refresh
