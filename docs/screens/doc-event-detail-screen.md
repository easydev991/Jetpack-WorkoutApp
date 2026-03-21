# EventDetailScreen

Экран просмотра мероприятия.

## Структура

| Секция | Описание |
|--------|----------|
| Header | Название, дата, адрес |
| LocationInfoView | Карта с меткой локации |
| Calendar | Добавление в календарь |
| Participants | Список участников |
| SwitchFormRowView | Toggle "Пойду/Не пойду" |
| PhotoSectionView | Адаптивная сетка фото |
| Description | Описание мероприятия |
| Author | Информация об авторе |
| Comments | Комментарии, добавление |

## Политика авторизации

| Роль | Доступ |
|------|--------|
| Гость | Скрыт participants, add comment disabled |
| Авторизованный | Полный доступ |
| Автор мероприятия | Edit/delete, удаление фото |

## Архитектура

- **ID**: `Long` (аргумент навигации)
- **eventId**: из `SavedStateHandle`
- **State**: `EventDetailUIState` (sealed)
- **ViewModel**: `EventDetailViewModel` реализует `IEventDetailViewModel`

## Компоненты

| Компонент | Назначение |
|-----------|------------|
| `LocationInfoView` | Отображение локации на карте |
| `SwitchFormRowView` | Toggle для участия |
| `PhotoSectionView` | Сетка фото с кликом для fullscreen |
| `EventShareButton` | Кнопка "Поделиться" в TopAppBar |
| `EventAuthorActionsButton` | Dropdown с edit/delete для автора |
| `TextEntrySheetHost` | Ввод текста комментариев |

## Комментарии

- CREATE/EDIT/DELETE/REPORT
- Optimistic UI для toggle участия

## Тесты

`EventDetailViewModelTest` — 20 unit-тестов.

## Связанные экраны

- [PhotoDetailScreen](./doc-photo-detail-screen.md) — полноэкранный просмотр фото с zoom, удаление, жалоба

## Технический долг

- Оптимизация загрузки больших фото
- Пагинация комментариев
- Кэширование адресов
- Skeleton loading
