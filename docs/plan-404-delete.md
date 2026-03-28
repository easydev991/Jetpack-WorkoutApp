# План: Удаление park/event при 404 ошибке

## Контекст задачи

При загрузке детальной информации о park/event, если сервер возвращает 404 (ресурс не найден), необходимо:
1. Удалить объект из локального хранилища (только представляющие его записи)
2. Показать ошибку пользователю
3. Вернуться на предыдущий экран

**Порядок действий**: сначала отправить ошибку в `UserNotifier`, затем эмитить навигационное событие назад. Поскольку уведомления централизованно показывает `RootScreen`, сообщение переживёт возврат на предыдущий экран.

**Принцип**: списки обновляются реактивно через Flow/Room. UseCase очищает только repository/local storage cache. Временные in-memory кэши конкретных ViewModel не трогаем.

---

## Этап 1: Добавить domain-исключение NotFoundException

- [ ] Создать `app/src/main/java/com/swparks/domain/exception/NotFoundException.kt`
- [ ] sealed class с конкретными типами (НЕ resourceType):

```kotlin
sealed class NotFoundException : Exception() {
    abstract val resourceId: Long

    data class ParkNotFound(override val resourceId: Long) : NotFoundException()
    data class EventNotFound(override val resourceId: Long) : NotFoundException()
}
```

- [ ] Использовать `NotFoundException.ParkNotFound(resourceId = parkId)` и `NotFoundException.EventNotFound(resourceId = eventId)` везде в плане

---

## Этап 2: Добавить идемпотентные UseCase для удаления

### 2.1 DeleteParkUseCase

- [ ] Создать `app/src/main/java/com/swparks/domain/usecase/DeleteParkUseCase.kt`
- [ ] Принимает `parkId: Long`
- [ ] Очищает park из локального хранилища через repository
- [ ] Конкретная реализация удаления (например, обновление `UserEntity.addedParks` через `SWRepository.removeParkFromUser()`) скрыта внутри repository
- [ ] **НЕ трогает кэши ViewModel** — только repository-level хранилище
- [ ] **Идемпотентность**: если park уже удалён — `Result.success(Unit)`
- [ ] Возвращает `Result<Unit>`

### 2.2 DeleteEventUseCase

- [ ] Создать `app/src/main/java/com/swparks/domain/usecase/DeleteEventUseCase.kt`
- [ ] Принимает `eventId: Long`
- [ ] Очищает event из локального хранилища через repository
- [ ] Конкретная реализация удаления (например, `eventDao.deleteById(eventId)`) скрыта внутри repository
- [ ] **НЕ трогает кэши ViewModel** — только repository-level хранилище
- [ ] **Идемпотентность**: если event уже удалён — `Result.success(Unit)`
- [ ] Возвращает `Result<Unit>`

### 2.3 Перед реализацией (важно!)

- [ ] Проверить фактические локальные источники park в repository/DAO
- [ ] Если park присутствует в других repository-level кэшах — удалить и оттуда
- [ ] Не добавлять удаление "других кэшей" без подтверждения конкретным кодом

---

## Этап 3: Единое место маппинга HTTP 404 → NotFoundException

### 3.1 Проверить APIError

- [ ] Изучить `app/src/main/java/com/swparks/data/APIError.kt`
- [ ] Определить, как текущий код работает с HTTP 404

### 3.2 Обновить SWRepository

- [ ] В методах `getPark(id: Long)` и `getEvent(id: Long)`:
  - В том месте, где сейчас интерпретируется HTTP/API ошибка, добавить маппинг 404 → `NotFoundException`
  - **Только здесь** происходит маппинг 404 → domain exception
  - ViewModel не знает про HTTP/APIError

---

## Этап 4: Добавить локализуемую ошибку AppError.ResourceNotFound

### 4.1 Проверить AppError

- [ ] Изучить `app/src/main/java/com/swparks/util/AppError.kt`
- [ ] Проверить, есть ли `AppError.NotFound` или нужно добавить

### 4.2 Добавить AppError.ResourceNotFound

- [ ] Добавить `AppError.ResourceNotFound(resourceType: AppError.ResourceType)` с локализованной строкой
- [ ] Добавить enum `AppError.ResourceType` со значениями `PARK`, `EVENT`
- [ ] Использовать **только** `AppError.ResourceNotFound` в этапах 5 и 6 (НЕ `AppError.ParkNotFound`)

---

## Этап 5: Обновить ParkDetailViewModel

### 5.1 DI

- [ ] Добавить `DeleteParkUseCase` в конструктор

### 5.2 Обработка результата loadPark()

- [ ] Если result содержит exception типа `NotFoundException.ParkNotFound`:
  - Получить `parkId` из `exception.resourceId`
  - Вызвать `DeleteParkUseCase(parkId)`
  - Вызвать `userNotifier.handleError(AppError.ResourceNotFound(AppError.ResourceType.PARK))`
  - Эмитировать `ParkDetailEvent.NavigateBack`
- [ ] При других ошибках — обычная обработка через `handleError()`
- [ ] **НЕ дергать списковые ViewModel напрямую**

### 5.3 Навигация назад — использовать sealed interface Event

- [ ] Использовать паттерн как в LoginScreen:

```kotlin
sealed interface ParkDetailEvent {
    data object NavigateBack : ParkDetailEvent
}

private val _events = Channel<ParkDetailEvent>()
val events: Flow<ParkDetailEvent> = _events.receiveAsFlow()
```

---

## Этап 6: Обновить EventDetailViewModel

### 6.1 DI

- [ ] Добавить `DeleteEventUseCase` в конструктор

### 6.2 Обработка результата loadEvent()

- [ ] Если result содержит exception типа `NotFoundException.EventNotFound`:
  - Получить `eventId` из `exception.resourceId`
  - Вызвать `DeleteEventUseCase(eventId)`
  - Вызвать `userNotifier.handleError(AppError.ResourceNotFound(AppError.ResourceType.EVENT))`
  - Эмитировать `EventDetailEvent.NavigateBack`
- [ ] При других ошибках — обычная обработка через `handleError()`

### 6.3 Навигация назад

- [ ] Аналогично ParkDetailViewModel: `sealed interface EventDetailEvent { data object NavigateBack }`

---

## Этап 7: Проверить реактивное обновление списков

### 7.1 Для Events

- [ ] Проверить, что `EventsScreen` читает данные из Flow/Room, а не из in-memory кэша
- [ ] Если `EventsViewModel` использует in-memory кэши (`futureEventsCache`, `pastEventsCache`) — это presentation-layer кэш, **не очищаем** в UseCase
- [ ] Если Flow из локального хранилища обновляется автоматически после удаления event через repository — всё работает реактивно

### 7.2 Для Parks

- [ ] Проверить, как обновляется список в `UserAddedParksScreen` / `UserTrainingParksScreen`
- [ ] Если подписаны на Flow от UserEntity — Flow сам эмитит обновление
- [ ] Если используют in-memory state — это presentation-layer, **не очищаем** в UseCase

### 7.3 Итог

- [ ] Если списки реактивные — обновятся автоматически
- [ ] Если есть presentation-layer кэши — это задача отдельной оптимизации, не часть этого плана
- [ ] Ручные методы `removeParkById()` / `removeEventById()` в списковых ViewModel **НЕ добавляем**

---

## Этап 8: Написать тесты

### 8.1 Repository тесты

- [ ] `getPark_whenServerReturns404_thenResultFailureParkNotFound`
- [ ] `getEvent_whenServerReturns404_thenResultFailureEventNotFound`

### 8.2 UseCase тесты

- [ ] `DeleteParkUseCase_whenParkExists_thenDeletesAndReturnsSuccess`
- [ ] `DeleteParkUseCase_whenParkAlreadyDeleted_thenReturnsSuccess` (идемпотентность)
- [ ] `DeleteEventUseCase_whenEventExists_thenDeletesAndReturnsSuccess`
- [ ] `DeleteEventUseCase_whenEventAlreadyDeleted_thenReturnsSuccess` (идемпотентность)

### 8.3 ViewModel тесты

- [ ] `ParkDetailViewModel_whenLoadParkReturnsNotFound_thenDeletesAndNavigatesBack`
- [ ] `EventDetailViewModel_whenLoadEventReturnsNotFound_thenDeletesAndNavigatesBack`
- [ ] Проверить: delete use case вызван, `userNotifier.handleError` вызван с `AppError.ResourceNotFound`, navigate event эмитится

### 8.4 Integration тесты

- [ ] Открыть detail → сервер 404 → вернуться на список → элемент исчезает
- [ ] Это **обязательный** тест (НЕ опциональный), так как проверяет сквозной сценарий

---

## Заметки

### Архитектурные принципы

- Repository — единственное место, где HTTP 404 маппится в `NotFoundException`
- UseCase — инициирует локальную очистку через repository. **Не трогает presentation-layer кэши**
- ViewModel — только инициирует сценарий и обновляет свой UI state/события
- Списки — обновляются реактивно через Flow
- `AppError.ResourceNotFound` — единый тип ошибки для userNotifier

### Границы удаления

Удаляем **только**:
- Запись park/event в repository-level локальном хранилище
- Конкретные механизмы удаления (например, обновление `UserEntity.addedParks` или удаление через DAO) скрыты внутри repository

**НЕ удаляем**:
- Комментарии, фотки, связи — только если они больше не достижимы и это следует стратегии repository
- In-memory кэши ViewModel
- Глобальные данные пользователя

### Порядок действий при 404

```
1. DeleteUseCase(parkId/eventId)
2. userNotifier.handleError(AppError.ResourceNotFound(...))  ← показывает сообщение
3. emit NavigateBack event                                   ← возврат на предыдущий экран
```

### Проверка

- [ ] `make lint`
- [ ] `make test`
- [ ] Тесты из этапа 8
- [ ] Ручное тестирование: открыть детали park/event, удалённый на сервере
