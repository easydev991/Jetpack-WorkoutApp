# План разработки экрана PhotoDetailScreen

## Текущий статус: 0% завершено

### Цель

Разработать полноэкранный модальный экран для просмотра отдельной фотографии с возможностью удаления (для автора мероприятия) или жалобы (для других авторизованных пользователей).

---

## Обзор

**PhotoDetailScreen** — модальный экран (ModalBottomSheet), который открывается при клике на фотографию в EventDetailScreen. В первой итерации отображает одну фотографию на весь экран с zoom-жестами.

### Функциональность

**TopAppBar:**
- **Слева:** Кнопка закрытия (иконка крестика)
- **Справа:**
  - **Автор мероприятия:** Кнопка удаления (иконка корзины) → подтверждение через AlertDialog → логирование
  - **Другие авторизованные:** Кнопка "Пожаловаться" (иконка восклицания) → отправка жалобы через email
  - **Неавторизованные:** Нет кнопки

**Контент:**
- Полноэкранное отображение фотографии с pinch-to-zoom и double-tap
- Фотография масштабируется и панорамируется

---

## Политика авторизации

| Роль | Действия |
|------|----------|
| **Неавторизованный** | Только просмотр, нет кнопок действий |
| **Авторизованный (не автор)** | Просмотр + кнопка "Пожаловаться" |
| **Автор мероприятия** | Просмотр + кнопка удаления с подтверждением |

---

## Этап 1: Domain Layer — Модели и состояние UI

### Задачи

- [ ] Создать `PhotoDetailUIState` — состояние экрана

  ```kotlin
  data class PhotoDetailUIState(
      val photo: Photo,
      val eventTitle: String,
      val isEventAuthor: Boolean,
      val isLoading: Boolean = false,
      val error: String? = null
  )
  ```

- [ ] Создать `IPhotoDetailViewModel` — интерфейс ViewModel

  ```kotlin
  interface IPhotoDetailViewModel {
      val uiState: StateFlow<PhotoDetailUIState>
      val events: SharedFlow<PhotoDetailEvent>
      val isAuthorized: StateFlow<Boolean>
      fun onCloseClick()
      fun onDeleteClick()
      fun onDeleteConfirm()
      fun onDeleteDismiss()
      fun onReportClick()
  }
  ```

- [ ] Создать `PhotoDetailEvent` — sealed class для одноразовых событий

  ```kotlin
  sealed class PhotoDetailEvent {
      data object CloseScreen : PhotoDetailEvent()
      data object ShowDeleteConfirmDialog : PhotoDetailEvent()
      data class SendPhotoComplaint(val complaint: Complaint.EventPhoto) : PhotoDetailEvent()
  }
  ```

### Файлы

| Файл                                                                  | Действие |
|-----------------------------------------------------------------------|----------|
| `app/src/main/java/com/swparks/ui/state/PhotoDetailUIState.kt`        | Создать  |
| `app/src/main/java/com/swparks/ui/viewmodel/IPhotoDetailViewModel.kt` | Создать  |

---

## Этап 2: Domain Layer — ViewModel

### Задачи

- [ ] Создать `PhotoDetailViewModel`
  - Принимает `photoId`, `eventId`, `eventTitle` через SavedStateHandle
  - Загружает информацию о фото (пока просто использует переданные данные)
  - Определяет `isEventAuthor` на основе `currentUserId` и `eventAuthorId`
  - Реализует методы интерфейса `IPhotoDetailViewModel`
  - Реализует удаление фото (пока логирует, API интеграция позже)
  - Реализует отправку жалобы через `SendPhotoComplaint` event

- [ ] Добавить Factory для ViewModel

  ```kotlin
  companion object {
      val Factory = viewModelFactory {
          initializer {
              val savedStateHandle = createSavedStateHandle()
              // Dependency injection через Application container
              PhotoDetailViewModel(savedStateHandle, appContainer)
          }
      }
  }
  ```

### Файлы

| Файл                                                                 | Действие |
|----------------------------------------------------------------------|----------|
| `app/src/main/java/com/swparks/ui/viewmodel/PhotoDetailViewModel.kt` | Создать  |

---

## Этап 3: UI Layer — PhotoDetailScreen

### Задачи

- [ ] Создать `PhotoDetailScreen` composable
  - Scaffold с TopAppBar и контентом
  - TopAppBar:
    - Левая иконка: `Icons.Default.Close` с `onCloseClick`
    - Правая иконка (условно):
      - Если `isEventAuthor` → `Icons.Default.Delete` с `onDeleteClick`
      - Если `isAuthorized && !isEventAuthor` → `Icons.Default.Warning` с `onReportClick`
      - Иначе → нет кнопки
  - Контент: `ZoomablePhotoView` с фото на весь экран

- [ ] Создать `ZoomablePhotoView` composable (или использовать существующий zoom компонент)
  - Pinch-to-zoom gesture
  - Double-tap для zoom in/out
  - Pan gesture для перемещения
  - Использует `SWAsyncImage` для загрузки

- [ ] Добавить обработку events в `LaunchedEffect`
  - `CloseScreen` → вызов `onDismiss` callback
  - `ShowDeleteConfirmDialog` → показать `showDeleteDialog = true`
  - `SendPhotoComplaint` → вызвать `sendComplaint()`

- [ ] Добавить AlertDialog для подтверждения удаления
  - Title: `R.string.event_delete_photo_confirm_title`
  - Message: "Are you sure you want to delete this photo?" (добавить в strings.xml)
  - Confirm button → `viewModel.onDeleteConfirm()`
  - Dismiss button → `viewModel.onDeleteDismiss()`

### Файлы

| Файл                                                                   | Действие |
|------------------------------------------------------------------------|----------|
| `app/src/main/java/com/swparks/ui/screens/photos/PhotoDetailScreen.kt` | Создать  |
| `app/src/main/java/com/swparks/ui/screens/photos/ZoomablePhotoView.kt` | Создать  |

---

## Этап 4: UI Layer — PhotoDetailSheetHost

### Задачи

- [ ] Создать `PhotoDetailSheetHost` composable
  - ModalBottomSheet на весь экран (`skipPartiallyExpanded = true`)
  - Блокирует dismiss по тапу вне области и системной кнопке "назад"
  - Закрытие только по кнопке крестика или после успешного действия
  - Создает `PhotoDetailViewModel` через factory
  - Передает параметры: `photoId`, `eventId`, `eventTitle`, `isEventAuthor`

- [ ] Параметры SheetHost:

  ```kotlin
  @Composable
  fun PhotoDetailSheetHost(
      show: Boolean,
      photoId: Long,
      eventId: Long,
      eventTitle: String,
      isEventAuthor: Boolean,
      onDismissed: () -> Unit
  )
  ```

### Файлы

| Файл                                                                      | Действие |
|---------------------------------------------------------------------------|----------|
| `app/src/main/java/com/swparks/ui/screens/photos/PhotoDetailSheetHost.kt` | Создать  |

---

## Этап 5: Интеграция с EventDetailScreen

### Задачи

- [ ] Добавить `NavigateToPhotoDetail` event в `EventDetailEvent`

  ```kotlin
  data class NavigateToPhotoDetail(
      val photo: Photo,
      val eventId: Long,
      val eventTitle: String,
      val isEventAuthor: Boolean
  ) : EventDetailEvent()
  ```

- [ ] Обновить `IEventDetailViewModel.onPhotoClick(photo: Photo)`
  - Текущая реализация логирует
  - Новая реализация: emit `NavigateToPhotoDetail` event

- [ ] Добавить state в EventDetailScreen для PhotoDetailSheetHost

  ```kotlin
  var showPhotoDetailSheet by remember { mutableStateOf(false) }
  var selectedPhoto by remember { mutableStateOf<Photo?>(null) }
  ```

- [ ] Обработать `NavigateToPhotoDetail` event в LaunchedEffect

  ```kotlin
  is EventDetailEvent.NavigateToPhotoDetail -> {
      selectedPhoto = event.photo
      showPhotoDetailSheet = true
  }
  ```

- [ ] Добавить `PhotoDetailSheetHost` в EventDetailScreen (после Scaffold)

  ```kotlin
  selectedPhoto?.let { photo ->
      PhotoDetailSheetHost(
          show = showPhotoDetailSheet,
          photoId = photo.id,
          eventId = viewModel.eventId,
          eventTitle = viewModel.uiState.value.event.title,
          isEventAuthor = isEventAuthor,
          onDismissed = {
              showPhotoDetailSheet = false
              selectedPhoto = null
          }
      )
  }
  ```

### Файлы

| Файл                                                                   | Действие |
|------------------------------------------------------------------------|----------|
| `app/src/main/java/com/swparks/ui/viewmodel/EventDetailViewModel.kt`   | Изменить |
| `app/src/main/java/com/swparks/ui/screens/events/EventDetailScreen.kt` | Изменить |

---

## Этап 6: Локализация

### Задачи

- [ ] Добавить строковые ресурсы в `strings.xml`
  - `photo_delete_confirm_message`: "Are you sure you want to delete this photo?"
  - `photo_deleted`: "Photo deleted" (для future use)
  - `photo_delete_error`: "Failed to delete photo" (для future use)
  - `photo_report`: "Report photo" (для accessibility)

### Файлы

| Файл                                     | Действие |
|------------------------------------------|----------|
| `app/src/main/res/values/strings.xml`    | Изменить |
| `app/src/main/res/values-ru/strings.xml` | Изменить |

---

## Этап 7: Unit-тесты

### Задачи

- [ ] Создать `PhotoDetailViewModelTest`
  - Тест: `init_whenCreated_thenLoadsPhotoData`
  - Тест: `onCloseClick_whenCalled_thenEmitsCloseScreen`
  - Тест: `onDeleteClick_whenAuthor_thenEmitsShowDeleteConfirmDialog`
  - Тест: `onDeleteConfirm_whenAuthor_thenLogsAndEmitsPhotoDeleted`
  - Тест: `onReportClick_whenNotAuthor_thenEmitsSendPhotoComplaint`
  - Тест: `onReportClick_whenNotAuthorized_thenDoesNothing`

### Файлы

| Файл                                                                     | Действие |
|--------------------------------------------------------------------------|----------|
| `app/src/test/java/com/swparks/ui/viewmodel/PhotoDetailViewModelTest.kt` | Создать  |

---

## Этап 8: UI Preview

### Задачи

- [ ] Добавить Preview для `PhotoDetailScreen`
  - Preview для автора (с кнопкой удаления)
  - Preview для авторизованного пользователя (с кнопкой report)
  - Preview для неавторизованного (без кнопок)
  - Preview для темной темы

### Файлы

| Файл                                                                   | Действие |
|------------------------------------------------------------------------|----------|
| `app/src/main/java/com/swparks/ui/screens/photos/PhotoDetailScreen.kt` | Изменить |

---

## Зависимости

### Существующие компоненты

| Компонент              | Статус      | Использование                   |
|------------------------|-------------|---------------------------------|
| `Photo` model          | ✅ Готов     | Передается из EventDetailScreen |
| `Complaint.EventPhoto` | ✅ Готов     | Используется для жалобы         |
| `sendComplaint()`      | ✅ Готов     | Вызывается при report           |
| `SWAsyncImage`         | ✅ Готов     | Загрузка изображения            |
| `ModalBottomSheet`     | ✅ Готов     | Используется в SheetHost        |
| String resources       | ⚠️ Частично | Нужно добавить новые            |

### Новые зависимости

Нет внешних зависимостей. Все компоненты реализуются на базе существующих библиотек проекта.

---

## Технический долг и улучшения (Future Iterations)

### Итерация 2 — Галерея

- [ ] Поддержка коллекции фотографий (swipe для переключения)
- [ ] Индикатор текущей фотографии (1/5)
- [ ] Анимации переходов между фото

### Итерация 3 — API интеграция

- [ ] Реальное удаление фото через API
- [ ] Optimistic UI при удалении
- [ ] Обработка ошибок сети
- [ ] Обновление списка фото в EventDetailScreen после удаления

### Итерация 4 — UX улучшения

- [ ] Share фото
- [ ] Save фото в галерею устройства
- [ ] Exif информация (если доступна)

---

## Критерии завершения

### Обязательные

- [ ] Фото открывается в полноэкранном модальном окне
- [ ] Крестик закрывает экран
- [ ] Автор видит кнопку удаления с подтверждением
- [ ] Авторизованные пользователи видят кнопку report
- [ ] Неавторизованные не видят кнопок действий
- [ ] Жалоба отправляется через email клиент
- [ ] Unit-тесты проходят
- [ ] Локализация добавлена
- [ ] Preview работают

### Опциональные (для этой итерации)

- [ ] Pinch-to-zoom работает плавно
- [ ] Double-tap zoom работает
- [ ] Анимации открытия/закрытия

---

## Риски и вопросы

1. **Вопрос:** Нужен ли zoom для первой итерации или можно отложить?
   - **Решение:** Реализовать базовый zoom, так как это стандартное поведение для фото-просмотра

2. **Вопрос:** Обновлять ли список фото в EventDetailScreen после удаления?
   - **Решение:** В первой итерации — нет. Пользователь может обновить вручную через pull-to-refresh. Реальное обновление в Итерации 3.

3. **Вопрос:** Как передавать данные о фото в PhotoDetailViewModel?
   - **Решение:** Через SavedStateHandle при создании ViewModel в SheetHost

---

## Метрики успеха

- Время открытия фото < 300ms
- Нет crashes при открытии/закрытии
- Unit-тесты покрывают > 80% логики ViewModel
- Все строковые ресурсы локализованы
