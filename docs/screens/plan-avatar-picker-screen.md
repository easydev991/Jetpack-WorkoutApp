# План: Выбор фотографии профиля (AvatarPicker)

## Обзор

Реализация функционала выбора фотографии профиля из галереи устройства для экрана `EditProfileScreen`.

### Референсы

- **Android**: `EditProfileScreen.kt` - AvatarSection
- **API**: `SWApi.editUser()` - уже поддерживает параметр `image: MultipartBody.Part?`
- **Repository**: `SWRepository.editUser()` - уже принимает `image: ByteArray?`
- **Utils**: `NetworkUtils.createOptionalImagePart()` - уже реализован

---

## Архитектура решения

### Best Practice для Android (2024+)

Для выбора изображений из галереи используется **Photo Picker** (`ActivityResultContracts.PickVisualMedia`).

**Примечание о совместимости:**
- Нативный Photo Picker доступен на Android 13+ (API 33)
- Бэкпорт через Play Services работает на Android 4.4+ (API 19+)
- Fallback на `GetContent()` нужен для устройств без Play Services (некоторые китайские устройства, эмуляторы без GMS)

**Преимущества Photo Picker:**
- Не требует разрешений `READ_MEDIA_IMAGES`
- Системный UI для выбора изображений
- Автоматическое управление правами доступа
- Поддержка облачных хранилищ

### Компоненты решения

```
UI Layer (Compose)
├── AvatarSection - отображение аватара + кнопка выбора
├── rememberLauncherForActivityResult - запуск Photo Picker
└── EditProfileScreen - координация

ViewModel Layer
├── EditProfileViewModel.onAvatarSelected(uri: Uri?)
├── EditProfileUiState.selectedAvatarUri: Uri? - временное хранение выбранного фото
├── EditProfileUiState.avatarError: String? - ошибка при обработке фото
└── EditProfileUiState.isUploadingAvatar: Boolean - состояние загрузки

Utils Layer (новое)
├── ImageUtils - валидация и сжатие изображений
└── UriUtils - конвертация Uri → ByteArray с обработкой ошибок

Data Layer (уже реализовано)
├── SWRepository.editUser(image: ByteArray?)
├── SWApi.editUser(image: MultipartBody.Part?)
└── NetworkUtils.createOptionalImagePart()
```

---

## Этапы реализации

### Этап 1: Создание утилит для работы с изображениями

**Задача:** Создать утилиты для валидации, сжатия и конвертации изображений.

**Файлы:**
- `app/src/main/java/com/swparks/util/UriUtils.kt` (создать)
- `app/src/main/java/com/swparks/util/ImageUtils.kt` (создать)

**UriUtils.kt:**

```kotlin
object UriUtils {
    /**
     * Конвертирует Uri в ByteArray с обработкой ошибок
     * @return Result.success(ByteArray) или Result.failure с описанием ошибки
     */
    fun uriToByteArray(context: Context, uri: Uri): Result<ByteArray> {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                Result.success(inputStream.readBytes())
            } ?: Result.failure(IOException("Cannot open input stream for uri: $uri"))
        } catch (e: SecurityException) {
            Result.failure(SecurityException("No permission to read uri: $uri"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**ImageUtils.kt:**

```kotlin
object ImageUtils {
    const val MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024 // 5 MB
    val SUPPORTED_MIME_TYPES = listOf("image/jpeg", "image/png", "image/webp")

    /**
     * Проверяет MIME-тип изображения
     */
    fun isSupportedMimeType(context: Context, uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType in SUPPORTED_MIME_TYPES
    }

    /**
     * Сжимает изображение если оно превышает максимальный размер
     * @return Сжатый ByteArray или исходный если размер в норме
     */
    fun compressIfNeeded(data: ByteArray, maxSizeBytes: Int = MAX_IMAGE_SIZE_BYTES): ByteArray {
        if (data.size <= maxSizeBytes) return data

        // Использовать BitmapFactory для сжатия
        // Уменьшать качество JPEG до достижения нужного размера
        // ...
    }
}
```

**Изменения:**
- [ ] Создать `UriUtils.kt` с функцией `uriToByteArray()`
- [ ] Создать `ImageUtils.kt` с константами и функциями валидации
- [ ] Добавить функцию сжатия изображения

**Критерии завершения:**
- Uri корректно конвертируется в ByteArray
- Ошибки обрабатываются через Result
- MIME-тип проверяется перед обработкой
- Большие изображения сжимаются

---

### Этап 2: Обновление UI State

**Задача:** Добавить состояние для хранения выбранного фото и ошибок.

**Файлы:**
- `app/src/main/java/com/swparks/ui/state/EditProfileUiState.kt`

**Изменения:**
- [ ] Добавить поле `selectedAvatarUri: Uri?` - URI выбранного фото
- [ ] Добавить поле `avatarError: String? = null` - ошибка при обработке фото
- [ ] Добавить поле `isUploadingAvatar: Boolean = false` - состояние загрузки
- [ ] Обновить `hasChanges` для учета изменения аватара

**Логика hasChanges:**

```kotlin
val hasChanges: Boolean
    get() = userForm != initialForm || selectedAvatarUri != null
```

**Критерии завершения:**
- State содержит новые поля
- `hasChanges` корректно учитывает изменение аватара
- Есть поле для хранения ошибки

---

### Этап 3: Обновление ViewModel

**Задача:** Добавить метод обработки выбора фото с валидацией и обновить сохранение.

**Файлы:**
- `app/src/main/java/com/swparks/ui/viewmodel/IEditProfileViewModel.kt`
- `app/src/main/java/com/swparks/ui/viewmodel/EditProfileViewModel.kt`

**Изменения в интерфейсе:**
- [ ] Добавить метод `onAvatarSelected(uri: Uri?)` - обработка выбранного фото (nullable для отмены)

**Изменения в реализации:**
- [ ] Реализовать `onAvatarSelected(uri: Uri?)`:
  - Если `uri == null` — пользователь отменил выбор, ничего не делаем (логируем)
  - Проверить MIME-тип через `ImageUtils.isSupportedMimeType()`
  - Если тип не поддерживается — показать ошибку через `avatarError`
  - Сохранить валидный URI в state
- [ ] Обновить `onSaveClick()`:
  - Установить `isUploadingAvatar = true`
  - Конвертировать Uri → ByteArray через `UriUtils.uriToByteArray()`
  - Обработать ошибку конвертации
  - Отправить фото на сервер через repository
  - Сбросить `isUploadingAvatar` после завершения

**Обработка отмены выбора:**

```kotlin
fun onAvatarSelected(uri: Uri?) {
    if (uri == null) {
        // Пользователь отменил выбор — ничего не делаем
        logger.d(TAG, "Avatar selection cancelled by user")
        return
    }

    // Валидация и сохранение...
}
```

**Критерии завершения:**
- ViewModel принимает nullable Uri
- Отмена выбора обрабатывается корректно (без ошибок)
- MIME-тип валидируется перед сохранением
- Uri конвертируется в ByteArray с обработкой ошибок
- Фото отправляется на сервер при сохранении
- Ошибки отображаются пользователю

---

### Этап 4: Интеграция Photo Picker в UI

**Задача:** Добавить Photo Picker в EditProfileScreen с fallback.

**Файлы:**
- `app/src/main/java/com/swparks/ui/screens/profile/EditProfileScreen.kt`

**Изменения:**
- [ ] Добавить `rememberLauncherForActivityResult` с `PickVisualMedia()` контракт
- [ ] Передать `uri` (может быть null) в ViewModel: `viewModel.onAvatarSelected(uri)`
- [ ] Добавить fallback для устройств без Play Services с `GetContent()` контрактом
- [ ] Определить какой picker использовать на основе доступности Play Services

**Photo Picker контракты:**

```kotlin
// Основной вариант - Photo Picker
val photoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
) { uri ->
    viewModel.onAvatarSelected(uri) // uri может быть null при отмене
}

// Fallback для устройств без Play Services
val contentPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
) { uri ->
    viewModel.onAvatarSelected(uri) // uri может быть null при отмене
}
```

**Запуск picker:**

```kotlin
// Использовать PickVisualMediaRequest для ограничения только изображениями
photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
```

**Критерии завершения:**
- Photo Picker открывается по клику на кнопку
- Работает на Android 13+ и устройствах с Play Services
- Fallback работает на устройствах без Play Services
- Отмена выбора не вызывает ошибок

---

### Этап 5: Обновление AvatarSection

**Задача:** Отображать выбранное фото превью перед сохранением и ошибки.

**Файлы:**
- `app/src/main/java/com/swparks/ui/screens/profile/EditProfileScreen.kt`

**Изменения:**
- [ ] Добавить параметр `selectedAvatarUri: Uri?` в AvatarSection
- [ ] Добавить параметр `avatarError: String?` в AvatarSection
- [ ] Обновить AsyncImage для отображения выбранного фото или текущего аватара
- [ ] Добавить индикатор загрузки при `isUploadingAvatar`
- [ ] Показывать ошибку через Snackbar или текст под аватаром

**Логика отображения:**

```kotlin
AsyncImage(
    model = selectedAvatarUri ?: avatarUrl, // приоритет выбранному URI
    // ...
)
```

**Критерии завершения:**
- AvatarSection отображает выбранное фото превью
- Индикатор загрузки показывается во время загрузки
- Ошибки отображаются пользователю
- После сохранения отображается новый аватар с сервера

---

### Этап 6: Тестирование

**Задача:** Написать тесты для новой функциональности.

**Файлы:**
- `app/src/test/java/com/swparks/util/UriUtilsTest.kt` (создать)
- `app/src/test/java/com/swparks/util/ImageUtilsTest.kt` (создать)
- `app/src/test/java/com/swparks/ui/viewmodel/EditProfileViewModelTest.kt` (обновить)

**Тесты UriUtils:**
- [ ] `uriToByteArray_success` - успешная конвертация
- [ ] `uriToByteArray_nullInputStream` - ошибка при null InputStream
- [ ] `uriToByteArray_securityException` - ошибка прав доступа

**Тесты ImageUtils:**
- [ ] `isSupportedMimeType_jpeg` - JPEG поддерживается
- [ ] `isSupportedMimeType_png` - PNG поддерживается
- [ ] `isSupportedMimeType_unsupported` - неподдерживаемый тип
- [ ] `compressIfNeeded_smallImage` - маленькое изображение не сжимается
- [ ] `compressIfNeeded_largeImage` - большое изображение сжимается

**Тесты ViewModel:**
- [ ] `onAvatarSelected_updatesState` - проверка сохранения URI
- [ ] `onAvatarSelected_nullUri_doesNothing` - отмена выбора не меняет state
- [ ] `onAvatarSelected_unsupportedMimeType_showsError` - неверный тип файла
- [ ] `hasChanges_returnsTrue_whenAvatarSelected` - проверка hasChanges
- [ ] `onSaveClick_sendsImageToRepository` - проверка отправки фото

**Моки:**
- Использовать MockK для мокирования SWRepository
- Использовать MockK для мокирования Context и ContentResolver в утилитах
- Для UriUtils можно использовать Robolectric или мокировать через wrapper-интерфейс

**Критерии завершения:**
- Все тесты проходят
- Покрытие кода > 80% для новой логики

---

### Этап 7: Локализация

**Задача:** Добавить строковые ресурсы для новых сообщений об ошибках.

**Файлы:**
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-ru/strings.xml`

**Строки:**
- [ ] `avatar_error_unsupported_type` - "Неподдерживаемый формат изображения"
- [ ] `avatar_error_too_large` - "Изображение слишком большое"
- [ ] `avatar_error_read_failed` - "Не удалось прочитать изображение"
- [ ] `avatar_upload_error` - "Ошибка при загрузке изображения"

**Примечание:** Основные строки уже существуют (`change_photo`, `photo`)

---

## Диаграмма потока данных

```
User Click "Изменить фото"
        │
        ▼
┌─────────────────────────┐
│  Photo Picker Launch    │
│  (PickVisualMedia)      │
└───────────┬─────────────┘
            │
            ▼
    User selects image (or cancels)
            │
            ├─────────────────────────────┐
            │                             │
            ▼ (selected)                  ▼ (cancelled)
┌─────────────────────────┐    ┌─────────────────────────┐
│  onAvatarSelected(uri)  │    │  onAvatarSelected(null) │
│  Validate MIME type     │    │  Log & return           │
└───────────┬─────────────┘    └─────────────────────────┘
            │
            ▼
┌─────────────────────────┐
│  Update UiState         │
│  selectedAvatarUri      │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│  AvatarSection shows    │
│  preview (selected URI) │
└───────────┬─────────────┘
            │
            ▼
    User clicks "Сохранить"
            │
            ▼
┌─────────────────────────┐
│  onSaveClick()          │
│  isUploadingAvatar=true │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│  UriUtils.uriToByteArray│
│  (with error handling)  │
└───────────┬─────────────┘
            │
            ├─────────────────────────────┐
            │                             │
            ▼ (success)                   ▼ (error)
┌─────────────────────────┐    ┌─────────────────────────┐
│  ImageUtils.compress    │    │  Show error to user     │
│  if needed              │    │  avatarError = message  │
└───────────┬─────────────┘    └─────────────────────────┘
            │
            ▼
┌─────────────────────────┐
│  SWRepository.editUser  │
│  (image: ByteArray?)    │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│  SWApi.editUser         │
│  (MultipartBody.Part)   │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│  Server saves image     │
│  Returns updated User   │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│  Update local cache     │
│  Navigate back          │
└─────────────────────────┘
```

---

## Важные замечания

### Безопасность

- ✅ Photo Picker не требует разрешений
- ✅ Для fallback `GetContent()` тоже не требуются разрешения
- ✅ Разрешение `READ_MEDIA_IMAGES` в манифесте не используется для этой функциональности

### Обработка ошибок

- ✅ Обработка отмены выбора фото (uri = null)
- ✅ Обработка ошибок чтения Uri через `Result`
- ✅ Валидация MIME-типа перед обработкой
- ✅ Сжатие больших изображений
- ✅ Обработка ошибок загрузки на сервер (через UserNotifier)
- ✅ Отображение ошибок пользователю через `avatarError`

### UX

- Показывать превью выбранного фото до сохранения
- Блокировать UI во время загрузки (`isUploadingAvatar`)
- Отображать индикатор загрузки
- Показывать понятные сообщения об ошибках

### Совместимость

- Минимальная версия SDK: 26 (Android 8.0)
- Photo Picker нативно на Android 13+ (API 33)
- Photo Picker через Play Services на Android 4.4+ (API 19+)
- Fallback на `GetContent()` для устройств без Play Services

---

## Итого

| Этап | Сложность | Оценка времени |
|------|-----------|----------------|
| 1. Утилиты (UriUtils + ImageUtils) | Средняя | 45 мин |
| 2. UI State | Низкая | 15 мин |
| 3. ViewModel | Средняя | 45 мин |
| 4. Photo Picker | Средняя | 30 мин |
| 5. AvatarSection | Низкая | 20 мин |
| 6. Тестирование | Средняя | 60 мин |
| 7. Локализация | Низкая | 10 мин |
| **Итого** | | **~3.5 часа** |

---

## Будущие улучшения (не в текущей итерации)

- Кэширование выбранного фото в `savedStateHandle` для сохранения при навигации
- Обрезка фото (crop) после выбора
- Выбор источника: камера или галерея
- Отображение прогресса загрузки в процентах
