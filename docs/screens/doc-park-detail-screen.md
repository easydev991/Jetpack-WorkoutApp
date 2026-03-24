# ParkDetailScreen

Экран просмотра площадки.

## Структура

| Секция            | Описание                                     |
|-------------------|----------------------------------------------|
| Header            | Название, адрес                              |
| LocationInfoView  | Карта с меткой локации                       |
| CreateEvent       | Кнопка создания мероприятия для площадки     |
| Participants      | Список тренирующихся (`park_trainees_title`) |
| SwitchFormRowView | Toggle "Тренируюсь здесь" (`train_here`)     |
| PhotoSectionView  | Адаптивная сетка фото                        |
| Author            | Информация об авторе (`added_by`)            |
| Comments          | Комментарии, добавление                      |

## Отличия от EventDetailScreen

| Что             | EventDetailScreen              | ParkDetailScreen                  |
|-----------------|--------------------------------|-----------------------------------|
| Header          | title + when + where + address | только title + address            |
| Description     | есть                           | отсутствует                       |
| Главная кнопка  | Добавить в календарь           | Создать мероприятие               |
| Participants    | Участники, "Пойду тоже"        | Тренирующиеся, "Тренируюсь здесь" |
| Author title    | `event_author`                 | `added_by`                        |
| TopAppBar title | `event_title`                  | `park_title`                      |

## Политика авторизации

| Роль           | Доступ                                              |
|----------------|-----------------------------------------------------|
| Гость          | Скрыт participants, add comment disabled            |
| Авторизованный | Полный доступ                                       |
| Автор площадки | Delete, удаление фото, Edit → ParkFormScreen        |

## Архитектура

- **ID**: `Long` (аргумент навигации)
- **parkId**: из `SavedStateHandle`
- **State**: `ParkDetailUIState` (sealed: `InitialLoading`/`Content`/`Error`)
- **ViewModel**: `ParkDetailViewModel` реализует `IParkDetailViewModel`
- **Events**: `ParkDetailEvent` (sealed class)

## Компоненты

| Компонент                 | Назначение                             |
|---------------------------|----------------------------------------|
| `LocationInfoView`        | Отображение локации на карте           |
| `SwitchFormRowView`       | Toggle для train_here                  |
| `PhotoSectionView`        | Сетка фото с кликом для fullscreen     |
| `ParkShareButton`         | Кнопка "Поделиться" в TopAppBar        |
| `ParkAuthorActionsButton` | Dropdown с delete для автора           |
| `TextEntrySheetHost`      | Ввод текста комментариев               |
| `PhotoDetailSheetHost`    | Просмотр фото с zoom, удаление, жалоба |

## Навигация

| Callback                                      | Назначение                             |
|-----------------------------------------------|----------------------------------------|
| `onNavigateToCreateEvent(parkId, parkName)`   | Переход на `Screen.CreateEventForPark` |
| `onNavigateToTrainees(parkId, source, users)` | Переход на `Screen.ParkTrainees`       |
| `onNavigateToUserProfile(userId)`             | Профиль пользователя                   |
| `onNavigateToEditPark(park)`                  | Переход на `Screen.EditPark`           |
| `onParkDeleted(parkId)`                       | После удаления площадки                |

### ParkTrainees навигация

- `ParkNavigationCoordinator.buildParkTraineesNavigationData()` — сериализация users в JSON
- `ParkTraineesNavArgsViewModel` — потребление аргументов из `SavedStateHandle`

## Комментарии

- CREATE/EDIT/DELETE/REPORT через `TextEntryMode`
- Режимы: `TextEntryMode.NewForPark(parkId)`, `TextEntryMode.EditPark(EditInfo)`

## API методы

| Метод                   | Назначение                 |
|-------------------------|----------------------------|
| `getPark`               | Загрузка данных площадки   |
| `changeTrainHereStatus` | Toggle train_here          |
| `deleteParkPhoto`       | Удаление фото              |
| `sendComplaint`         | Жалоба на фото/комментарий |

## Тесты

- `ParkDetailViewModelTest` — 22 unit-теста
- `ParkDetailScreenTest` — 32 instrumented-теста
- `PhotoDetailViewModelTest` — тесты для Event и Park режимов

## Связанные экраны

- [PhotoDetailScreen](./doc-photo-detail-screen.md) — полноэкранный просмотр фото с zoom, удаление, жалоба

## Edit Flow

Полный цикл редактирования площадки:

| Этап | Компонент                               | Описание                                                               |
|------|-----------------------------------------|------------------------------------------------------------------------|
| 1    | `ParkAuthorActionsButton`               | Меню с Edit для автора                                                 |
| 2    | `ParkDetailEvent.NavigateToEditPark`    | Событие навигации с данными парка                                      |
| 3    | `ParkDetailAction.OnNavigateToEditPark` | Action для coordinator                                                 |
| 4    | `RootScreen`                            | `navigateToEditPark()` → `Screen.EditPark`                             |
| 5    | `ParkFormMode.Edit`                     | Режим редактирования с `parkId` и `park`                               |
| 6    | `ParkFormScreen`                        | Экран редактирования (см. [ParkFormScreen](./doc-park-form-screen.md)) |

**Unit test:** `onEditClick_thenEmitsNavigateToEditPark()`

## Реализованные файлы

| Файл                                              | Описание            |
|---------------------------------------------------|---------------------|
| `ui/screens/parks/ParkDetailScreen.kt`            | Основной экран      |
| `ui/screens/parks/sections/ParkDetailSections.kt` | UI секции           |
| `ui/viewmodel/ParkDetailViewModel.kt`             | ViewModel           |
| `ui/state/ParkDetailUIState.kt`                   | Состояние UI        |
| `navigation/ParkNavigationCoordinator.kt`         | Навигация trainees  |
| `navigation/ParkNavArgs.kt`                       | Аргументы навигации |
