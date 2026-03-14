---

# План разработки экрана EventDetailScreen

## Текущий статус: ~99% завершено

### ✅ Выполнено
* Этапы 0-7: архитектура, UI State/ViewModel, компоненты (LocationInfoView, MapUriSet, DateFormatter, SwitchFormRowView), секции, локализация, навигация
* Этап 5.1-5.4: EventDetailScreen, Toggle "Пойду" (optimistic update), dropdown-меню автора (edit/delete), стилизация кнопок, доработки секций
* Комментарии: CREATE/EDIT/DELETE/REPORT через TextEntrySheetHost + email
* Тесты: MapUriSetTest, DateFormatterTest, EventDetailViewModelTest (12 тестов), SWRepositoryCommentsTest

### ⏳ В работе / Не начато
* **Этап 8:** EventDetailViewModelTest (расширить покрытие)

### 🎯 Следующие шаги (приоритет)
1. **PhotoDetailScreen** — детальный экран фото с удалением
2. **EventDetailViewModelTest** — расширить unit-тесты

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

* [x] Реализованы `EventDetailUIState`, `IEventDetailViewModel`, `EventDetailViewModel` (удаление фото, factory)

---

## Этап 2: Компоненты [ГОТОВО]

* [x] `LocationInfoView`, `MapUriSet`, `DateFormatter` с unit-тестами

---

## Этап 3: Секции и комментарии [ГОТОВО]

* [x] `SwitchFormRowView`, секции (описание, автор, комментарии), REPORT через email
* [x] `TextEntrySheetHost` для create/edit, удаление с confirm-alert

---

## Этап 4: PhotoSectionView [ГОТОВО]

* [x] Адаптивная сетка (1/2/3 столбца), `SWAsyncImage`, темы, Preview

---

## Этап 5: UI Screen [ГОТОВО]

### 5.1. EventDetailScreen

**Файл:** `app/src/main/java/com/swparks/ui/screens/events/EventDetailScreen.kt`

* [x] `Scaffold` с `TopAppBar`, `PullToRefreshBox`, секции (title, date, address, LocationInfoView, calendar, participants, SwitchFormRowView, PhotoSectionView, description, author, comments)
* [x] Состояния: `InitialLoading`, `Error`, `Content`
* [x] Меню удаления для автора, навигация на профиль, REPORT через email
* [x] Удаление мероприятия/фото/комментария через confirm-dialog
* [ ] Меню редактирования — UI не добавлен (ViewModel метод готов)
* [ ] Меню "Поделиться" — UI не добавлен (ViewModel метод готов)

### 5.4. Dropdown-меню в TopAppBar [ГОТОВО]

* [x] Dropdown с опциями "Изменить" (Edit) и "Удалить" (Delete)
* [x] Меню блокируется при `isRefreshing`, показывается только если `isAuthorized && isEventAuthor`

---

## Этап 5.1: Toggle "Пойду" [ГОТОВО]

* [x] Optimistic update с локальным обновлением `trainingUsers`, откат при ошибке
* [ ] Unit-тесты на успех/ошибка/откат

---

## Этап 5.2-5.3: Стилизация и доработки [ГОТОВО]

* [x] Кнопки удаления через `MaterialTheme.colorScheme.error`
* [x] `EventAuthorSection` — блокировка клика для автора
* [x] `EventParticipantsSection` — скрытие при `trainingUsersCount == null` или `0`

---

## Этап 6: Локализация [ГОТОВО]

* [x] Основные строки локализованы, добавлены `event_photos`, `event_edit`, `event_share`
* [ ] Новые ключи подключены в UI (зависит от edit/share)

---

## Этап 7: Интеграция в навигацию [ГОТОВО]

* [x] Экран интегрирован в `RootScreen` с factory и навигацией

---

## Этап 8: Тестирование [ЧАСТИЧНО]

### 8.1. Unit-тесты ViewModel

**Файл:** `app/src/test/java/com/swparks/ui/viewmodel/EventDetailViewModelTest.kt`

**Выполнено (12 тестов):**
* [x] `REPORT/CREATE/EDIT/DELETE` комментарий (4 теста)
* [x] Ошибки загрузки/удаления события и фото (3 теста)
* [x] `onCommentDeleteConfirm` — успешное удаление (1 тест)
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

## Порядок реализации

1. Этапы 0-7 ✅
2. Этап 3.2-3.6, 4-5.3 ✅
3. Этап 5.4 → dropdown-меню для автора ✅
4. Этап 8 → Тестирование (расширить покрытие)

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
