# Документация: Удаление park/event при 404 ошибке

## Обзор

При загрузке детальной информации о park/event, если сервер возвращает 404 (ресурс не найден), необходимо:
1. Удалить объект из локального хранилища
2. Показать ошибку пользователю
3. Вернуться на предыдущий экран

**Правило (канон)**: удаление при 404 допустимо **только** если тело ответа — JSON с полями `errors`/`name` (реальный API-404; проверено живыми запросами к проду: оба формата 404 — `errors`-массив от контроллеров и `name`+`message`+`status` от Yii2 error handler — несут эти поля, пустых тел сервер не отдаёт). Инфраструктурный 404 (HTML-страница от nginx/прокси, пустое или битое тело, чужой JSON без `errors`/`name`) удаление **не** инициирует: он мапится в `ServerException(code = 503)` → `AppError.Server(code = 503)` → «Сервер недоступен. Попробуйте позже.» (`R.string.error_server_unavailable`). Это единственный путь обработки инфра-404 — см. раздел «Определение реального API-404».

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

Алгоритм применяется **только** для реального API-404 (см. следующий раздел). При инфра-404 ни один из шагов не выполняется: пользователь видит «Сервер недоступен. Попробуйте позже.», локальный кэш сохраняется.

---

## Определение реального API-404

Классификация выполняется **по содержимому тела ответа**, Content-Type не проверяется. Помощник: `isApiNotFoundBody(body: ResponseBody?, json: Json): ErrorResponse?` (`BaseRepository.kt`, top-level internal fun).

```
HTTP 404 получен
    ↓
Парсим тело как ErrorResponse
(Json: ignoreUnknownKeys, isLenient;
null/blank/битое тело — парсинг не удался)
    ↓
Парсинг успешен И есть поля API
(errors / name; чужой message —
не признак нашего API)? ── да ──→ API-404
    ↓ нет                  (ErrorResponse(realMessage = ...) — с серверным сообщением)
Инфра-404 (пустое тело, HTML/XML/текст
от прокси или валидный JSON без полей API —
чужой прокси, Spring Boot и т.п.)
    → ServerException(message, code = 503)
    → AppError.Server(code = 503)
    → «Сервер недоступен. Попробуйте позже.»
```

**Контракт `isApiNotFoundBody`**: `null` — не API-404 (инфра-404); `ErrorResponse` — API-404, серверное сообщение в `realMessage` (может быть `null`, если заполнено только `name`). `body` — peek-копия (`okhttp3.Response.peekBody`, 64KB), функцией потребляется, исходное `errorBody` не затрагивается.

**Коды `ServerException.code`**: настоящий API-404 → `code = 404` (ViewModel могут ветвиться: например, профиль — экран «Пользователь не найден»); инфра-404 → `code = 503` («Сервер недоступен», деструктивные действия запрещены); прочие ошибки → `code = null`. Инфра-404 дополнительно пишет breadcrumb `crashReporter.log("[infra_html_404] ...")`.

**Следствие для деструктивных операций**: удаление park/event из локального кэша и silent delete журналов/записей разрешены только при `isApiNotFoundBody != null`.

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

- `loadPark_whenParkNotFound_thenDeletesLocallyNotifiesAndNavigatesBack` (`ParkDetailViewModelTest`)
- `loadPark_whenParkNotFound_thenUiStateShowsError` (`ParkDetailViewModelTest`)
- `loadEvent_whenEventNotFound_thenDeletesLocallyNotifiesAndNavigatesBack` (`EventDetailViewModelTest`)
- `loadEvent_whenEventNotFound_thenUiStateShowsError` (`EventDetailViewModelTest`)

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
