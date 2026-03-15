# План разработки экрана EventDetailScreen

## Текущий статус: 100% завершено

### ✅ Выполнено

| Этап | Описание                                                                                        |
|------|-------------------------------------------------------------------------------------------------|
| 0    | Архитектура: ID (`Long`), `eventId` из `SavedStateHandle`                                       |
| 1    | `EventDetailUIState`, `IEventDetailViewModel`, `EventDetailViewModel`                           |
| 2    | `LocationInfoView`, `MapUriSet`, `DateFormatter` + тесты                                        |
| 3    | Секции, `SwitchFormRowView`, комментарии (CREATE/EDIT/DELETE/REPORT), `TextEntrySheetHost`      |
| 4    | `PhotoSectionView`: адаптивная сетка, `SWAsyncImage`, темы                                      |
| 5    | `EventDetailScreen`: Scaffold, states, Toggle "Пойду" (optimistic), dropdown автора, стилизация |
| 6    | Локализация: `event_photos`, `event_edit`, `event_share`                                        |
| 7    | Интеграция в `RootScreen` с factory                                                             |
| 8    | 20 unit-тестов в `EventDetailViewModelTest`                                                     |
| 9    | `EventShareButton`, `EventAuthorActionsButton` в TopAppBar                                      |
| 10   | Flow-based `EventsViewModel`: `getFutureEventsFlow()`, `syncFutureEvents()`                     |
| 11   | `PhotoDetailScreen`: полноэкранный просмотр фото с zoom, удаление (API), жалоба, 17 тестов      |

### ⏳ В работе / Не начато

*Нет активных задач*

---

## Обзор

Разработка экрана мероприятия по аналогии с iOS-версией (`EventDetailsScreen.swift`), с адаптацией под Android.

**Структура экрана:** title, date, address, `LocationInfoView`, calendar, participants, `SwitchFormRowView`, `PhotoSectionView`, description, author, comments, add comment.

**Политика авторизации:**
* неавторизованные — скрыт participants section, add comment disabled
* авторизованные — полный доступ
* автор мероприятия — edit/delete actions, удаление фото

---

> **Детали Этапа 11 (PhotoDetailScreen):** См. [plan-photo-detail-screen.md](./plan-photo-detail-screen.md)

---

## Существующие компоненты (переиспользование)

| Компонент                                                                                                                                                      | Статус   |
|----------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| `SectionView`, `UserRowView`, `CommentRowView`, `LoadingOverlayView`, `FormCardContainer`, `FormRowView`, `SWAsyncImage`, `ErrorContentView`, `EmptyStateView` | ✅ Готовы |

---

## Технический долг и улучшения

* **Фото:** оптимизация загрузки больших изображений, кэширование
* **Комментарии:** пагинация, ответы, optimistic UI
* **Производительность:** кэширование адресов, оптимизация refresh
* **UX:** skeleton loading, анимации

---

## Следующие итерации

### Итерация 2 — Навигация

* Редактирование мероприятия, реальный share flow
