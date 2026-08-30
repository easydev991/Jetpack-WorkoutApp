# Документация: Удаление park/event при 404 ошибке

## Обзор

При загрузке детальной информации о park/event, если сервер возвращает 404 (ресурс не найден), необходимо:
1. Удалить объект из локального хранилища
2. Показать ошибку пользователю
3. Вернуться на предыдущий экран

**Порядок действий**: сначала отправить ошибку в `UserNotifier`, затем эмитить навигационное событие назад. Поскольку уведомления централизованно показывает `RootScreen`, сообщение переживёт возврат на предыдущий экран.

---

## Архитектура

```
Repository (HTTP 404 → NotFoundException)
    ↓
UseCase (инициирует локальную очистку)
    ↓
ViewModel (координирует: delete → handleError → navigateBack)
    ↓
UserNotifier (показывает Snackbar)
```

**Принцип**: списки обновляются реактивно через Flow/Room. UseCase очищает только repository/local storage cache. Временные in-memory кэши конкретных ViewModel не трогаем.

---

## Ключевые компоненты

### Domain-исключение `NotFoundException`

```kotlin
sealed class NotFoundException : Exception() {
    abstract val resourceId: Long
    data class ParkNotFound(override val resourceId: Long) : NotFoundException()
    data class EventNotFound(override val resourceId: Long) : NotFoundException()
}
```

### AppError.ResourceNotFound

```kotlin
AppError.ResourceNotFound(message: String, resourceType: ResourceType)
enum class ResourceType { PARK, EVENT }
```

### DeleteUseCase

- `DeleteParkUseCase(parkId: Long)` — делегирует в `ParksEventsRepository.removeParkLocally(parkId)`, возвращает `Result<Unit>`
- `DeleteEventUseCase(eventId: Long)` — делегирует в `ParksEventsRepository.removeEventLocally(eventId)`, возвращает `Result<Unit>`

### Navigation Event

```kotlin
data object NavigateBack : ParkDetailEvent()
data object NavigateBack : EventDetailEvent()
```

---

## Алгоритм при 404

1. `DeleteUseCase(parkId/eventId)` — удаляет из локального хранилища
2. `userNotifier.handleError(AppError.ResourceNotFound(...))` — показывает Snackbar
3. `emit(NavigateBack)` — возврат на предыдущий экран

---

## Реализованные файлы

| Файл | Назначение |
|------|------------|
| `domain/exception/NotFoundException.kt` | sealed class с ParkNotFound/EventNotFound |
| `domain/usecase/DeleteParkUseCase.kt` | удаление park из repository |
| `domain/usecase/DeleteEventUseCase.kt` | удаление event из repository |
| `util/AppError.kt` | добавлен ResourceNotFound + ResourceType enum |
| `util/AppErrorExt.kt` | toUiText() для ResourceNotFound |
| `data/repository/ParksEventsRepository.kt` | маппинг HTTP 404 → NotFoundException (getPark, getEvent) |
| `ui/viewmodel/ParkDetailViewModel.kt` | 404 handling + DeleteParkUseCase DI |
| `ui/viewmodel/EventDetailViewModel.kt` | 404 handling + DeleteEventUseCase DI |
| `ui/viewmodel/ParkDetailEvent.kt` | NavigateBack event |
| `ui/viewmodel/EventDetailViewModel.kt` | NavigateBack event (sealed class EventDetailEvent объявлен внутри файла) |

---

## Тесты

### Repository тесты

- `getPark_whenApiReturns404_thenReturnsParkNotFound` (`ParksEventsRepositoryParksTest`)
- `getEvent_whenApiReturns404_thenReturnsEventNotFound` (`ParksEventsRepositoryEventsTest`)

### UseCase тесты

`DeleteParkUseCaseTest`:

- `invoke_success_callsRemoveParkLocally`
- `invoke_failure_returnsFailure`
- `invoke_passesCorrectParkId`

`DeleteEventUseCaseTest`:

- `invoke_success_callsRemoveEventLocally`
- `invoke_failure_returnsFailure`
- `invoke_passesCorrectEventId`

### ViewModel тесты

- `loadPark_whenParkNotFound_thenDeletesLocallyNotifiesAndNavigatesBack`
- `loadPark_whenParkNotFound_thenUiStateShowsError`
- `loadEvent_whenEventNotFound_thenDeletesLocallyNotifiesAndNavigatesBack`
- `loadEvent_whenEventNotFound_thenUiStateShowsError`

### Presentation Integration тесты

- `Integration404Test.parkDetail_404_whenParkNotFound_deletesAndUpdatesParksList`
- `Integration404Test.eventDetail_404_whenEventNotFound_deletesAndUpdatesEventsList`

---

## Границы удаления

**Удаляем только**:
- Запись park/event в repository-level локальном хранилище
- Конкретные механизмы (обновление `UserEntity.addedParks` или `eventDao.deleteById`) скрыты внутри repository

**НЕ удаляем**:
- Комментарии, фотки, связи
- In-memory кэши ViewModel
- Глобальные данные пользователя

---

## Проверка

- [x] `make lint` — проходит
- [x] `make test` — все unit тесты проходят
- [x] Presentation integration tests — проходят
- [ ] Ручное тестирование: открыть детали park/event, удалённый на сервере
