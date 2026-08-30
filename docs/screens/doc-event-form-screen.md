# EventFormScreen (создание и редактирование мероприятия)

## Обзор

`EventFormScreen` обслуживает полный сценарий создания и редактирования мероприятия. Это один экран с общей формой и тремя режимами работы:

| Режим | Route | Точка входа | Особенности |
|------|-------|-------------|-------------|
| `RegularCreate` | `Screen.CreateEvent` | FAB на `EventsScreen` | Пользователь сам выбирает площадку |
| `EditExisting` | `Screen.EditEvent` | меню автора на `EventDetailScreen` | Форма инициализируется данными существующего мероприятия |
| `CreateForSelected` | `Screen.CreateEventForPark` | `ParkDetailScreen` | Площадка предустановлена и недоступна для смены |

Экран реализован в [EventFormScreen.kt](app/src/main/java/com/swparks/ui/screens/events/EventFormScreen.kt), состояние и бизнес-логика находятся в [EventFormViewModel.kt](app/src/main/java/com/swparks/ui/viewmodel/EventFormViewModel.kt).

## Архитектура

### Основные модели

| Сущность | Файл | Назначение |
|---------|------|------------|
| `EventForm` | [EventForm.kt](app/src/main/java/com/swparks/ui/model/EventForm.kt) | DTO формы для create/edit API |
| `EventFormMode` | [EventFormMode.kt](app/src/main/java/com/swparks/ui/model/EventFormMode.kt) | Режим работы экрана и заголовок навигации |
| `EventFormUiState` | [EventFormUiState.kt](app/src/main/java/com/swparks/ui/state/EventFormUiState.kt) | Полное состояние UI, производные флаги и лимиты |
| `EventFormEvent` | [EventFormUiState.kt](app/src/main/java/com/swparks/ui/state/EventFormUiState.kt) | Одноразовые события: сохранение, навигация, пикер фото |

### Поток данных

```text
EventFormScreen
  -> EventFormViewModel
  -> CreateEventUseCase / EditEventUseCase
  -> ParksEventsRepository.saveEvent()
  -> SWApi.createEvent() / SWApi.editEvent()
```

### Навигационная интеграция

- `RootScreen` создаёт `EventFormViewModel` с нужным `EventFormMode`.
- Для `EditExisting` объект `Event` передаётся через `savedStateHandle` как JSON, а `eventId` идёт в route.
- После успешного create экран возвращает созданное мероприятие в `EventsViewModel.addCreatedEvent(...)`.
- После успешного edit экран кладёт JSON обновлённого мероприятия в `previousBackStackEntry.savedStateHandle["updatedEvent"]`.

Связанные точки входа:

- [RootScreen.kt](app/src/main/java/com/swparks/ui/screens/RootScreen.kt)
- [EventNavigationCoordinator.kt](app/src/main/java/com/swparks/navigation/EventNavigationCoordinator.kt)
- [EventNavArgs.kt](app/src/main/java/com/swparks/navigation/EventNavArgs.kt)

## Реализованный функционал

### Поля формы

| Поле | Источник | Обязательность | Примечание |
|------|----------|----------------|------------|
| `title` | `SWTextField` | обязательно | Без названия сохранение недоступно |
| `description` | `SWTextEditor` | необязательно | Свободный текст |
| `parkId` / `parkName` | `ListRowView` в `FormCardContainer` | обязательно | Выбор площадки через отдельный экран, кроме `CreateForSelected` |
| `date` | `SWDateTimePicker` | обязательно | Хранится как `ISO_LOCAL_DATE_TIME` без timezone |
| `selectedPhotos` | `PickedImagesGrid` | необязательно | Только новые фото, выбранные в текущей сессии |

### Логика доступности Save

- В create-режимах кнопка активна только если заполнены `title`, `parkId`, `date` и есть изменения относительно `initialForm`.
- В edit-режиме сохранение доступно, если изменено хотя бы одно поле формы или добавлены новые фото.
- Во время `isSaving=true` форма блокируется, кнопка Save отключается, поверх контента показывается `LoadingOverlayView`.

### Работа с площадкой

- В `RegularCreate` и `EditExisting` нажатие на строку площадки открывает `SelectParkForEvent`.
- В `CreateForSelected` площадка фиксирована, chevron скрыт, переход отключён.
- После возврата из `SelectParkForEvent` выбранный парк применяется через `consumeSelectedParkResult()`.

### Работа с датой и временем

- Для новых мероприятий дата по умолчанию устанавливается в `LocalDateTime.now()` с обнулёнными секундами и наносекундами.
- При редактировании `beginDate` нормализуется в локальную дату/время устройства.
- Если сервер прислал `Z` или offset, `EventFormViewModel` конвертирует значение в `ZoneId.systemDefault()` и убирает timezone перед сохранением.
- В UI `SWDateTimePicker` ограничен диапазоном лет `currentYear..currentYear+1`.

### Работа с фотографиями

- Используется `rememberPickedImagesController` и системный photo picker.
- Поддерживаются только MIME-типы, разрешённые `AvatarHelper.isSupportedMimeType(...)`.
- Все выбранные фото конвертируются в JPEG и при необходимости сжимаются перед отправкой.
- Общий лимит фотографий на мероприятие: `15`.
- В edit-режиме лимит новых фото считается как `15 - event.photos.size`.
- `PickedImagesGrid` показывает только новые фотографии, добавленные в текущей сессии.
- `ImagePreviewDialog` позволяет просмотреть и удалить только новые, ещё не сохранённые фото.

## Важные ограничения текущей реализации

- Экран не отображает уже сохранённые фотографии мероприятия в режиме редактирования. Они учитываются только в лимите `photosCount`.
- Удаление существующих серверных фото из `EventFormScreen` не поддерживается.
- При инициализации `EditExisting` имя площадки заполняется как `№{parkId}`, потому что в форму передаётся только `parkID`, без человекочитаемого названия площадки.
- Если route `Screen.EditEvent` открыт без объекта `event` в `savedStateHandle`, `RootScreen` переводит экран в `RegularCreate`. Для поддержки это важно: проблема будет выглядеть как "открылось создание вместо редактирования".

## Сохранение и сетевой контракт

### UseCase и repository

- `CreateEventUseCase` и `EditEventUseCase` являются тонкими обёртками над `ParksEventsRepository.saveEvent(...)`.
- `ParksEventsRepository.saveEvent(...)` использует один путь для create и edit, различая сценарии по `id == null`.

### Формирование multipart

- Фото отправляются как `photo1`, `photo2`, ... без подчёркиваний.
- Для каждой части используется `Content-Type: image/jpeg`.
- Имя файла формируется как `${partName}.jpg`.
- Текстовые поля отправляются как multipart parts: `title`, `description`, `date`, `area_id`.

Код:

- [ParksEventsRepository.kt](app/src/main/java/com/swparks/data/repository/ParksEventsRepository.kt)
- [NetworkUtils.kt](app/src/main/java/com/swparks/data/NetworkUtils.kt)

## UX и пользовательские сценарии

### Закрытие экрана

- Если `uiState.hasChanges == true`, на Back показывается подтверждение закрытия.
- Диалог предлагает `Close` и `Cancel`.
- Если изменений нет, экран закрывается сразу.

### Уведомление об ошибках

- Ошибки чтения фото и ошибки сохранения прокидываются через `UserNotifier`.
- ViewModel логирует ключевые действия и исключения через `Logger`.

### Что видит пользователь после успешного сохранения

- Форма сбрасывает `selectedPhotos`.
- `initialForm` обновляется до актуального состояния.
- Экран закрывается и возвращает сохранённый `Event` в вызывающий сценарий.

## Тестовое покрытие

| Файл | Тип | Покрывает |
|------|-----|-----------|
| [EventFormViewModelTest.kt](app/src/test/java/com/swparks/ui/viewmodel/EventFormViewModelTest.kt) | unit | инициализацию режимов, изменение полей, нормализацию дат, лимиты фото, сохранение |
| [EventFormScreenTest.kt](app/src/androidTest/java/com/swparks/ui/screens/events/EventFormScreenTest.kt) | androidTest | заголовки режимов, доступность Save, блокировки, confirm dialog, поведение выбора площадки |
| [ParksEventsRepositoryEventsTest.kt](app/src/test/java/com/swparks/data/repository/ParksEventsRepositoryEventsTest.kt) | unit | create/edit event API, multipart-части и обработку ошибок |
| [NetworkUtilsTest.kt](app/src/test/java/com/swparks/data/NetworkUtilsTest.kt) | unit | MIME, filename и multipart helper |

## Что учитывать при развитии фичи

- Если понадобится редактирование существующих фото, это придётся добавлять отдельно: сейчас `EventFormUiState` знает только количество существующих фото, но не управляет ими как сущностями UI.
- Если нужно показывать название площадки при редактировании без пере-выбора, надо расширять payload навигации или восстанавливать park name по `parkId`.
- Любые изменения формата даты нужно синхронизировать сразу в трёх местах: `EventFormViewModel.normalizeDateForServer(...)`, `parseFormDateTimeOrNow(...)` и `parseEventDateTime(...)`.
- Любые изменения photo upload контракта нужно проверять не только в UI/ViewModel, но и в [ParksEventsRepository.kt](app/src/main/java/com/swparks/data/repository/ParksEventsRepository.kt) и [NetworkUtils.kt](app/src/main/java/com/swparks/data/NetworkUtils.kt).
