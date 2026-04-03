# EventDetailScreen

Экран просмотра мероприятия.

## Назначение

`EventDetailScreen` показывает полную информацию о мероприятии, поддерживает действия автора и участников, работу с фото и комментариями, а для прошедших мероприятий использует cache-first загрузку из `Room`.

## Структура

| Секция | Описание |
|--------|----------|
| Header | Название, дата, адрес |
| LocationInfoView | Карта с меткой локации |
| Calendar | Добавление в календарь для актуальных мероприятий |
| Participants | Список участников и переход к полному списку |
| SwitchFormRowView | Toggle "Пойду/Не пойду" |
| PhotoSectionView | Адаптивная сетка фото |
| Description | Описание мероприятия |
| Author | Информация об авторе |
| Comments | Комментарии, добавление, редактирование, удаление, жалоба |

## Политика авторизации

| Роль | Доступ |
|------|--------|
| Гость | Нет toggle участия, добавление комментария недоступно |
| Авторизованный | Участие, комментарии, просмотр участников |
| Автор мероприятия | Edit/delete мероприятия, удаление фото, действия автора |

## Архитектура

- **ID**: `Long` (аргумент навигации)
- **eventId**: берётся из `SavedStateHandle`
- **State**: `EventDetailUIState` (`InitialLoading`, `Content`, `Error`)
- **ViewModel**: `EventDetailViewModel` реализует `IEventDetailViewModel`
- **Источник данных**:
  - обычная загрузка: `swRepository.getEvent(eventId)`
  - cache-first для past event: `swRepository.getEventFromCache(eventId)` -> показ cached `Content` -> `refreshEventContentInBackground(eventId)`

## Cache-first для past event

| Сценарий | Поведение |
|----------|-----------|
| В `Room` есть полный snapshot past event | Экран сразу открывается из локального кэша |
| После показа локального кэша | Запускается фоновое обновление с сервера |
| Фоновое обновление успешно | UI обновляется, новый full snapshot сохраняется в `Room` |
| Фоновое обновление падает | Уже показанный `Content` сохраняется, ошибка уходит через `UserNotifier` |
| Кэша нет | Обычный flow `InitialLoading -> Content/Error` |

Полный кэш хранится в `events` через расширенный `EventEntity` с признаком `isFull = true`. Для списка прошедших мероприятий сохраняется частичный snapshot, для details сохраняется полный snapshot с `address`, `photos`, `trainingUsers`, `comments`, полным `author`, счётчиками и флагами экрана.

## Компоненты

| Компонент | Назначение |
|-----------|------------|
| `LocationInfoView` | Отображение локации на карте |
| `SwitchFormRowView` | Toggle для участия |
| `PhotoSectionView` | Сетка фото с кликом для fullscreen |
| `EventShareButton` | Кнопка "Поделиться" в TopAppBar |
| `EventAuthorActionsButton` | Dropdown с edit/delete для автора |
| `TextEntrySheetHost` | Ввод текста комментариев |

## Состояния и UX

- При первом открытии без кэша показывается `InitialLoading`.
- Если `eventId` некорректен, экран переходит в `Error`.
- При `refresh()` из состояния `Error` экран временно возвращается в `InitialLoading`.
- При `refresh()` из `Content` неуспешное обновление не уничтожает уже показанный контент.
- При `404` во время загрузки `ViewModel` удаляет локальную запись, уведомляет пользователя и инициирует `NavigateBack`.

## Комментарии и действия

- Комментарии поддерживают `CREATE`, `EDIT`, `DELETE`, `REPORT`.
- Toggle участия обновляется оптимистично.
- Календарь доступен только для актуальных мероприятий.
- После успешной загрузки full past event вызывается `saveEventFull(event)`.

## Тесты

- `EventDetailViewModelTest` — 32 unit-теста, включая cache-first сценарии past event.
- `EventDetailScreenTest` — UI/instrumentation сценарии для error/content состояний и cached past event.

## Связанные экраны

- [PhotoDetailScreen](./doc-photo-detail-screen.md) — полноэкранный просмотр фото с zoom, удаление, жалоба

## Ограничения и техдолг

- Полный device-run `EventDetailScreenTest` зависит от общей стабильности instrumentation окружения.
- Для full cache используется одна таблица `events`, поэтому важно сохранять merge-логику списка и не возвращаться к destructive `replaceAll`.
- Пагинация комментариев и отдельная оптимизация загрузки больших фото пока не реализованы.
