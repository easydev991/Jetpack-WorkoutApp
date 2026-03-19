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

## Этап 1: Модели данных ✅

### 1.1 PickedImageItem sealed class ✅

Файл: `app/src/main/java/com/swparks/ui/model/PickedImageItem.kt`

- [x] Создать sealed class `PickedImageItem`
- [x] `Image` - содержит Uri и уникальный id
- [x] `AddButton` - кнопка добавления

### 1.2 PickedImagesState ✅

Файл: `app/src/main/java/com/swparks/ui/model/PickedImagesState.kt`

- [x] Создать data class для состояния:
  - `images: List<Uri>`
  - `selectionLimit: Int`
  - `canAddMore: Boolean` (computed property)
  - `remainingSlots: Int` (computed property)

---

## Этап 2: UI компоненты ✅

### 2.1 PickedImageCell ✅

Файл: `app/src/main/java/com/swparks/ui/ds/PickedImageCell.kt`

Ячейка для отображения одной фотографии или кнопки добавления.

**Для Image:**
- Асинхронная загрузка через Coil
- Нажатие → контекстное меню (просмотр, удаление)
- Скругленные углы

**Для AddButton:**
- Закрашенная поверхность с плюсом
- Material иконка `Icons.Outlined.Add`
- Скругленные углы

- [x] Создать composable `PickedImageCell`
- [x] Реализовать отображение Image с AsyncImage
- [x] Реализовать отображение AddButton
- [x] Добавить контекстное меню для Image
- [x] Использовать стандартные Material иконки

### 2.2 PickedImagesGrid ✅

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

- [x] Создать composable `PickedImagesGrid`
- [x] Реализовать сетку 3 колонки с aspectRatio(1f)
- [x] Добавить подзаголовок с лимитом
- [x] Сформировать список items (images + AddButton если canAddMore)
- [x] Обработать клики и долгие нажатия

### 2.3 Стилизация ✅

- [x] Использовать Material3 цвета и типографику
- [x] Скругление углов: 8.dp
- [x] Отступы между ячейками: 12.dp
- [x] Цвет фона кнопки добавления: `MaterialTheme.colorScheme.surfaceVariant`

---

## Этап 3: Интеграция с Photo Picker ✅

### 3.1 PickedImagesController ✅

Файл: `app/src/main/java/com/swparks/ui/ds/PickedImagesController.kt`

Вспомогательный composable для управления выбором фото.

```kotlin
@Composable
fun rememberPickedImagesController(
    currentImageCount: Int,
    selectionLimit: Int,
    onImagesSelected: (List<Uri>) -> Unit
): PickedImagesController
```

- [x] Создать controller с использованием `PickMultipleVisualMedia`
- [x] Ограничить количество через `maxItems` (или обрезать результат)
- [x] Фильтр только изображений

### 3.2 Пример использования в экране ✅

- [x] Пример интеграции в план документации:
  - rememberPickedImagesController
  - Photo Picker launch через controller.launch()
  - Обработка результата через onImagesSelected callback

---

## Этап 4: Просмотр фото на весь экран ✅

### 4.1 ImagePreviewDialog ✅

Файл: `app/src/main/java/com/swparks/ui/ds/ImagePreviewDialog.kt`

Диалог для просмотра фото с возможностью удаления.

**Переиспользование:**
- ✅ `ZoomablePhotoView` (`app/src/main/java/com/swparks/ui/screens/photos/ZoomablePhotoView.kt`) - уже реализует zoom/pan/double-tap
- ❌ `PhotoDetailScreen` НЕ подходит - привязан к специфичным `PhotoDetailUIState` и `PhotoDetailAction`, логика удаления зависит от `state.isEventAuthor`

**Функционал:**
- Полноэкранный просмотр через `ZoomablePhotoView(imageUrl = uri.toString())`
- TopAppBar с кнопками: Close (всегда) + Delete (всегда видна)
- В отличие от PhotoDetailScreen, кнопка удаления ВСЕГДА видна

- [x] Создать Dialog composable с DialogProperty(fullscreen)
- [x] Использовать `ZoomablePhotoView` для отображения (передавать `uri.toString()`)
- [x] Добавить TopAppBar с Close и Delete кнопками
- [x] Delete кнопка всегда видна (не условно как в PhotoDetailScreen)

---

## Этап 5: Локализация ✅

### 5.1 Строковые ресурсы ✅

Файл: `app/src/main/res/values/strings.xml`

- [x] `photoSectionHeader` (plurals) - "1 photo" / "%d photos"
- [x] `photos_add_subtitle_empty` - "Add photos, max %1$d"
- [x] `photos_add_subtitle_more` - "Can add %1$d more"
- [x] `photos_max_reached` - "Maximum number of photos added"
- [x] `photos_view_fullscreen` - "View fullscreen"
- [x] `photos_delete` → используется общая `delete`
- [x] `photos_error_load` → не нужна (AsyncImage использует placeholder)

### 5.2 Русская локализация ✅

Файл: `app/src/main/res/values-ru/strings.xml`
- [x] Добавить переводы

---

## Этап 6: Тестирование ✅

### 6.1 Unit-тесты ✅

Файл: `app/src/test/java/com/swparks/ui/model/PickedImagesStateTest.kt`

- [x] Тест canAddMore логики (пустой, неполный, полный, превышен лимит)
- [x] Тест remainingSlots вычисления
- [x] Тест default values

### 6.2 UI-тесты ✅

Файл: `app/src/androidTest/java/com/swparks/ui/ds/PickedImagesGridTest.kt`

- [x] Тест отображения пустой сетки с кнопкой добавления
- [x] Тест subtitle для разных состояний (empty, more, max_reached)
- [x] Тест отображения заголовка с количеством фото (plurals)
- [x] Тест скрытия кнопки добавления при полном лимите
- [x] Тест enabled/disabled состояния кнопки добавления
- [x] Тест клика на добавление

---

## Зависимости

### Уже существуют в проекте

- ✅ Coil для загрузки изображений
- ✅ Material3 иконки
- ✅ ActivityResultContracts

### Нужно добавить (опционально)

- [ ] Accompanist Pager для просмотра фото (или собственная реализация)

---

## Out of Scope (Phase 2)

- Съемка с камеры
- Сжатие изображений перед отправкой
- Drag & drop для изменения порядка
- Редактирование фото (обрезка)

---

## Критерии завершения

- [x] Компонент отображает сетку фотографий 3xN
- [x] Кнопка добавления работает через Photo Picker
- [x] Удаление фото работает
- [x] Просмотр на весь экран работает
- [x] Лимит фотографий соблюдается
- [x] Подсказки о лимите корректны
- [x] `make lint` без ошибок
- [x] Unit и UI тесты проходят

---

## Актуализация после ревью коммита `9785ab6b4b82f08cae29cf4a402d46f08e932c05` ✅

**Статус: Все проблемы исправлены**

Ниже зафиксированы проблемы, найденные при проверке реализации относительно плана и iOS-референса.

### Проблемы

- [x] `PickedImagesController`: риск падения при `remainingSlots <= 1` из-за `PickMultipleVisualMedia(maxItems)` (требует `maxItems > 1`)
- [x] `PickedImagesGrid`: некорректный subtitle в состоянии полного лимита (`Can add 0 more` вместо `photos_max_reached`)
- [x] `PickedImageItem`/`PickedImagesGrid`: нестабильные ключи в `LazyVerticalGrid` из-за `UUID.randomUUID()` при каждом recomposition
- [x] ~~`PickedImageCell`: несоответствие плану по UX/иконке~~ — иконка исправлена на `Icons.Outlined.Add`; tap для меню соответствует iOS-референсу
- [x] `PickedImagesGridTest`: ненадёжная проверка `assert(addClicked)` вместо явной JUnit-проверки

### Пошаговый план исправления

#### Шаг 1. Исправить контракт Photo Picker в `PickedImagesController` ✅

- [x] Для `remainingSlots == 1` использовать `ActivityResultContracts.PickVisualMedia` (single picker)
- [x] Для `remainingSlots > 1` использовать `ActivityResultContracts.PickMultipleVisualMedia(remainingSlots)`
- [x] Для `remainingSlots == 0` не запускать picker и возвращать no-op `launch`
- [x] Унифицировать callback `onImagesSelected` для single/multi сценариев

#### Шаг 2. Исправить текст subtitle в полном лимите ✅

- [x] В `SubtitleText` переключить условие на `state.remainingSlots == 0`
- [x] Оставить ветки:
  - пустой список → `photos_add_subtitle_empty`
  - есть свободные слоты → `photos_add_subtitle_more`
  - слоты закончились → `photos_max_reached`
- [x] Добавить/обновить тест на case `images.size == selectionLimit`

#### Шаг 3. Сделать стабильные ключи элементов грида ✅

- [x] Убрать генерацию случайного `UUID` для UI-ключа у `PickedImageItem.Image`
- [x] Использовать стабильный key `"${uri}-$index"` (поддерживает дубликаты uri)
- [x] Проверить, что тесты проходят после изменений

#### Шаг 4. Привести `PickedImageCell` к требованиям плана ✅

- [x] ~~Открывать контекстное меню по долгому нажатию~~ — свёрено с iOS: меню открывается по tap, текущая реализация корректна
- [x] Привести иконку add-кнопки к `Icons.Outlined.Add`
- [x] Сверить поведение меню (fullscreen/delete) с iOS-референсом

#### Шаг 5. Усилить тесты ✅

- [x] В `PickedImagesGridTest` заменить `assert(addClicked)` на `assertTrue(addClicked)`
- [x] ~~Добавить UI-тест для корректного subtitle при полном лимите~~ (уже есть `whenFull_showsMaxReachedSubtitle`)
- [x] Добавить тест(ы) на работу контроллера при `remainingSlots = 0/1/2`

#### Шаг 6. Финальная валидация ✅

- [x] Прогнать `make format`
- [x] Прогнать `make lint`
- [x] Прогнать `make test`
- [x] ~~Прогнать `connectedDebugAndroidTest` на эмуляторе для UI-части~~ (пользователь подтвердил успешное прохождение)

---

## Пример использования

```kotlin
@Composable
fun EventFormScreen(viewModel: EventFormViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var previewUri by remember { mutableStateOf<Uri?>(null) }
    
    // Photo Picker controller
    val photoPickerController = rememberPickedImagesController(
        currentImageCount = uiState.photos.size,
        selectionLimit = 15,
        onImagesSelected = viewModel::onPhotosSelected
    )
    
    // В форме
    PickedImagesGrid(
        images = uiState.photos,
        selectionLimit = 15,
        onAddClick = { photoPickerController.launch() },
        onRemoveClick = viewModel::onPhotoRemoved,
        onImageClick = { uri, _ -> previewUri = uri },
        enabled = !uiState.isLoading
    )
    
    // Fullscreen preview dialog
    previewUri?.let { uri ->
        ImagePreviewDialog(
            uri = uri,
            onDismiss = { previewUri = null },
            onDelete = {
                viewModel.onPhotoRemoved(uri)
                previewUri = null
            }
        )
    }
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

---

## Известные баги

### BUG-001: FlowRow не помещает 3 элемента в строку ✅ ИСПРАВЛЕНО

**Описание:** При добавлении 2 фотографий кнопка "+" оказывалась на второй строке вместо продолжения первой строки. Ожидается, что в одной строке будет 3 элемента (2 фото + кнопка).

**Причины:**
1. Дробное деление Dp значений приводило к округлению вверх и превышению ширины
2. `IconButton` имел intrinsic minimum size (48dp), который конфликтовал с заданным размером

**Решение:**
1. Использовать `LocalDensity` для точного расчёта ширины в пикселях с целочисленным делением
2. Заменить `IconButton` на `Box` с `clickable` для полного контроля над размером

**Файл:** `app/src/main/java/com/swparks/ui/ds/PickedImagesGrid.kt`, `app/src/main/java/com/swparks/ui/ds/PickedImageCell.kt`

**Статус:** Исправлено

---

## Статус: Завершено ✅ (с известными багами)

Все этапы реализации выполнены. Компонент готов к использованию с учётом известных багов.
