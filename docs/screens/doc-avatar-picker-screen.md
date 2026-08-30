# Документация: Выбор фотографии профиля (AvatarPicker)

## Обзор

Функционал выбора фотографии профиля из галереи устройства для экрана `EditProfileScreen`.

## Статус реализации

| Этап | Статус |
|------|--------|
| 1. Утилиты (UriUtils + AvatarHelper) | ✅ Завершён |
| 2. UI State | ✅ Завершён |
| 3. ViewModel | ✅ Завершён |
| 4. Photo Picker | ✅ Завершён |
| 5. AvatarSection | ✅ Завершён |
| 6. Тестирование | ✅ Завершён |
| 7. Локализация | ✅ Завершён |

---

## Архитектура

### Photo Picker

Для выбора изображений используется **Photo Picker** (`ActivityResultContracts.PickVisualMedia`):
- Не требует разрешений `READ_MEDIA_IMAGES`
- Нативно на Android 13+ (API 33), бэкпорт через Play Services на Android 4.4+
- Fallback не реализован (не требуется для целевых устройств)

### Компоненты

| Слой | Компоненты |
|------|------------|
| **UI** | AvatarSection, rememberLauncherForActivityResult (Photo Picker) |
| **ViewModel** | onAvatarSelected(), selectedAvatarUri, avatarError, isLoading |
| **Utils** | AvatarHelper (валидация MIME, Uri→ByteArray, сжатие до 5 MB; внутри UriUtils и ImageProcessor) |
| **Data** | UserProfileRepository.editUser(), SWApi.editUser() |

---

## Поток данных

1. Пользователь нажимает "Изменить фото" → запускается Photo Picker
2. При выборе изображения → валидация MIME-типа (JPEG/PNG/WebP)
3. Валидное фото → сохраняется URI в state, отображается превью
4. При сохранении → конвертация Uri→ByteArray, сжатие при необходимости
5. Отправка на сервер через repository, обновление локального кэша

---

## Обработка ошибок

- Отмена выбора фото (uri = null) — логируется, state не меняется
- Неподдерживаемый MIME-тип — отображается ошибка пользователю
- Ошибка чтения Uri — отображается ошибка пользователю
- Ошибка загрузки на сервер — обрабатывается через UserNotifier

## UX

- Превью выбранного фото до сохранения
- Индикатор загрузки во время отправки на сервер
- Блокировка UI при загрузке (`isLoading`)
- Понятные сообщения об ошибках (локализованные)

---

## Тестирование

| Файл |
|------|
| UriUtilsTest |
| AvatarHelper / ImageProcessor — покрыты в `EditProfileViewModelTest` |
| EditProfileViewModelTest |

---

## Будущие улучшения

- Кэширование выбранного фото в `savedStateHandle`
- Обрезка фото (crop) после выбора
- Выбор источника: камера или галерея
- Отображение прогресса загрузки в процентах
