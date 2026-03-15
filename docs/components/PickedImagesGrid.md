# План: PickedImagesGrid - Компонент выбора фотографий

## Обзор

**Задача:** Реализовать переиспользуемый компонент для выбора и отображения фотографий в форме.

**Референсы iOS:**
- `SwiftUI-WorkoutApp/Screens/Common/ImagePicker/PickedImagesGrid.swift` - основной компонент
- `SwiftUI-WorkoutApp/Screens/Common/ImagePicker/ModernPickedImagesGrid.swift` - реализация с PhotosPicker
- `SwiftUI-WorkoutApp/Screens/Common/ImagePicker/ImagePickerViews.swift` - вспомогательные view
- `SwiftUI-WorkoutApp/Screens/Common/ImagePicker/PickedPhotoView.swift` - ячейка фотографии

**Используется в:**
- EventFormScreen (создание/редактирование мероприятий)
- ParkFormScreen (создание/редактирование площадок) - в будущем

---

## Функциональные требования

### Основные возможности
1. Отображение сетки фотографий (3 колонки)
2. Кнопка добавления новых фотографий
3. Просмотр фото на весь экран
4. Удаление фото
5. Лимит на количество фотографий
6. Подсказка о лимите

### Источники фото (Android)
- Photo Picker (ActivityResultContracts.PickMultipleVisualMedia) - основной способ
- Камера (опционально, Phase 2)

### Ограничения
- Максимальное количество фото: 15 (настраиваемо)
- Форматы: JPEG, PNG, WebP
- Поддержка стандартных Material иконок (без кастомных)

---

## Этап 1: Модели данных

### 1.1 PickedImageItem sealed class
Файл: `app/src/main/java/com/swparks/ui/model/PickedImageItem.kt`

```kotlin
sealed class PickedImageItem {
    data class Image(val uri: Uri, val id: String = UUID.randomUUID().toString()) : PickedImageItem()
    data object AddButton : PickedImageItem()
}
```

- [ ] Создать sealed class `PickedImageItem`
- [ ] `Image` - содержит Uri и уникальный id
- [ ] `AddButton` - кнопка добавления

### 1.2 PickedImagesState
- [ ] Создать data class для состояния:
  - `images: List<Uri>`
  - `selectionLimit: Int`
  - `canAddMore: Boolean`

---

## Этап 2: UI компоненты

### 2.1 PickedImageCell
Файл: `app/src/main/java/com/swparks/ui/ds/PickedImageCell.kt`

Ячейка для отображения одной фотографии или кнопки добавления.

**Для Image:**
- Асинхронная загрузка через Coil
- Долгое нажатие → контекстное меню (просмотр, удаление)
- Скругленные углы

**Для AddButton:**
- Закрашенная поверхность с плюсом
- Material иконка `Icons.Outlined.Add`
- Скругленные углы

- [ ] Создать composable `PickedImageCell`
- [ ] Реализовать отображение Image с AsyncImage
- [ ] Реализовать отображение AddButton
- [ ] Добавить контекстное меню для Image
- [ ] Использовать стандартные Material иконки

### 2.2 PickedImagesGrid
Файл: `app/src/main/java/com/swparks/ui/ds/PickedImagesGrid.kt`

Основной компонент - сетка фотографий.

**Структура:**
```
PickedImagesGrid
├── Header (опционально)
├── Subtitle (подсказка о лимите)
└── LazyVerticalGrid
    └── PickedImageCell items
```

**Параметры:**
- `images: List<Uri>` - список выбранных фото
- `selectionLimit: Int` - максимальное количество
- `onAddClick: () -> Unit` - callback добавления
- `onRemoveClick: (index: Int) -> Unit` - callback удаления
- `onImageClick: (uri: Uri) -> Unit` - callback просмотра
- `enabled: Boolean` - блокировка взаимодействия
- `modifier: Modifier`

- [ ] Создать composable `PickedImagesGrid`
- [ ] Реализовать сетку 3 колонки с aspectRatio(1f)
- [ ] Добавить подзаголовок с лимитом
- [ ] Сформировать список items (images + AddButton если canAddMore)
- [ ] Обработать клики и долгие нажатия

### 2.3 Стилизация
- [ ] Использовать Material3 цвета и типографику
- [ ] Скругление углов: 8.dp
- [ ] Отступы между ячейками: 12.dp
- [ ] Цвет фона кнопки добавления: `MaterialTheme.colorScheme.surfaceVariant`

---

## Этап 3: Интеграция с Photo Picker

### 3.1 PickedImagesController
Файл: `app/src/main/java/com/swparks/ui/ds/PickedImagesController.kt`

Вспомогательный composable для управления выбором фото.

```kotlin
@Composable
fun rememberPickedImagesController(
    selectionLimit: Int,
    onImagesSelected: (List<Uri>) -> Unit
): PickedImagesController
```

- [ ] Создать controller с использованием `PickMultipleVisualMedia`
- [ ] Ограничить количество через `maxItems` (или обрезать результат)
- [ ] Фильтр только изображений

### 3.2 Пример использования в экране
- [ ] Показать пример интеграции в EventFormScreen:
  - rememberLauncherForActivityResult
  - Photo Picker launch
  - Обработка результата

---

## Этап 4: Просмотр фото на весь экран

### 4.1 ImagePreviewDialog
Файл: `app/src/main/java/com/swparks/ui/ds/ImagePreviewDialog.kt`

Диалог для просмотра фото с возможностью удаления.

**Переиспользование:**
- ✅ `ZoomablePhotoView` (`app/src/main/java/com/swparks/ui/screens/photos/ZoomablePhotoView.kt`) - уже реализует zoom/pan/double-tap
- ❌ `PhotoDetailScreen` НЕ подходит - привязан к специфичным `PhotoDetailUIState` и `PhotoDetailAction`, логика удаления зависит от `state.isEventAuthor`

**Функционал:**
- Полноэкранный просмотр через `ZoomablePhotoView(imageUrl = uri.toString())`
- TopAppBar с кнопками: Close (всегда) + Delete (всегда видна)
- В отличие от PhotoDetailScreen, кнопка удаления ВСЕГДА видна

- [ ] Создать Dialog composable с DialogProperty(fullscreen)
- [ ] Использовать `ZoomablePhotoView` для отображения (передавать `uri.toString()`)
- [ ] Добавить TopAppBar с Close и Delete кнопками
- [ ] Delete кнопка всегда видна (не условно как в PhotoDetailScreen)

---

## Этап 5: Локализация

### 5.1 Строковые ресурсы
Файл: `app/src/main/res/values/strings.xml`

- [ ] `photos_section_header` - "Фотографии (%d)"
- [ ] `photos_add_subtitle_empty` - "Добавьте фото, максимум %d"
- [ ] `photos_add_subtitle_more` - "Можно добавить ещё %d"
- [ ] `photos_max_reached` - "Добавлено максимальное количество фотографий"
- [ ] `photos_view_fullscreen` - "На весь экран"
- [ ] `photos_delete` - "Удалить"
- [ ] `photos_error_load` - "Не удалось загрузить фото"

### 5.2 Русская локализация
Файл: `app/src/main/res/values-ru/strings.xml`
- [ ] Добавить переводы

---

## Этап 6: Тестирование

### 6.1 Unit-тесты
- [ ] Тест формирования списка items (images + AddButton)
- [ ] Тест canAddMore логики
- [ ] Тест лимита выбора

### 6.2 UI-тесты
- [ ] Тест отображения пустой сетки
- [ ] Тест отображения с фотографиями
- [ ] Тест клика на добавление
- [ ] Тест удаления фото
- [ ] Тест блокировки при disabled

---

## Зависимости

### Уже существуют в проекте:
- ✅ Coil для загрузки изображений
- ✅ Material3 иконки
- ✅ ActivityResultContracts

### Нужно добавить (опционально):
- [ ] Accompanist Pager для просмотра фото (или собственная реализация)

---

## Out of Scope (Phase 2)

- Съемка с камеры
- Сжатие изображений перед отправкой
- Drag & drop для изменения порядка
- Редактирование фото (обрезка)

---

## Критерии завершения

- [ ] Компонент отображает сетку фотографий 3xN
- [ ] Кнопка добавления работает через Photo Picker
- [ ] Удаление фото работает
- [ ] Просмотр на весь экран работает
- [ ] Лимит фотографий соблюдается
- [ ] Подсказки о лимите корректны
- [ ] `make lint` без ошибок
- [ ] Unit и UI тесты проходят

---

## Пример использования

```kotlin
@Composable
fun EventFormScreen(viewModel: EventFormViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Photo Picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(15 - uiState.photos.size)
    ) { uris ->
        viewModel.onPhotosSelected(uris)
    }
    
    // В форме
    PickedImagesGrid(
        images = uiState.photos,
        selectionLimit = 15,
        onAddClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ImageOnly)) },
        onRemoveClick = viewModel::onPhotoRemoved,
        onImageClick = { uri -> /* показать диалог */ },
        enabled = !uiState.isLoading
    )
}
```

---

## Оценка времени

| Этап | Время |
|------|-------|
| Модели данных | 0.5ч |
| UI компоненты | 3ч |
| Photo Picker интеграция | 1ч |
| Просмотр фото | 2ч |
| Локализация | 0.5ч |
| Тесты | 2ч |
| **Итого** | ~9ч |
