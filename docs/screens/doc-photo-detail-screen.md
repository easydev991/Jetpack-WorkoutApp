# План разработки экрана PhotoDetailScreen

## Текущий статус: 100% завершено

### Цель

Полноэкранный модальный экран для просмотра фото с удалением (автор) или жалобой (авторизованные).

---

## Выполненные этапы

| Этап         | Описание                                                                                                                            |
|--------------|-------------------------------------------------------------------------------------------------------------------------------------|
| Domain Layer | `PhotoDetailUIState`, `PhotoDetailConfig`, `PhotoDetailAction`, `PhotoDetailEvent`, `IPhotoDetailViewModel`, `PhotoDetailViewModel` |
| UI Layer     | `PhotoDetailScreen`, `ZoomablePhotoView` (pinch-to-zoom, double-tap), `DeleteConfirmDialog`, `PhotoDetailSheetHost`                 |
| Интеграция   | Связь с `EventDetailScreen` через `NavigateToPhotoDetail`, callback `onPhotoDeleted`                                                |
| Локализация  | Строки en/ru для диалога удаления                                                                                                   |
| Тестирование | 18 unit-тестов `PhotoDetailViewModelTest`, 4 Preview                                                                                |
| API          | Удаление фото через `swRepository.deleteEventPhoto()`, LoadingOverlay, `UserNotifier.handleError()`                                 |
| Bugfix       | `key = "photo_${config.photoId}"` для корректного открытия выбранного фото                                                          |

---

## Доработки

*Нет активных доработок*

---

## Технический долг и улучшения (Future Iterations)

### Итерация 2 — Галерея (следующая)

**Задача:** Открытие выбранной фотографии с горизонтальной коллекцией миниатюр внизу.

**Изменения в PhotoDetailConfig:**

```kotlin
data class PhotoDetailConfig(
    val photos: List<Photo>,        // было: single Photo
    val selectedPhotoId: String,    // ID фото для начального отображения
    val authorId: String,
    val canDelete: Boolean,
)
```

**UI изменения:**
- [ ] `HorizontalPager` для свайпа между фото
- [ ] Нижняя панель с `LazyRow` миниатюр (`ThumbnailBar`)
- [ ] Подсветка активной миниатюры
- [ ] Клик по миниатюре → переход к фото
- [ ] Синхронизация pager position ↔ thumbnail selection

**Navigation изменения:**
- [ ] Обновить `NavigateToPhotoDetail` — передача `List<Photo>` + `selectedPhotoId`
- [ ] Обновить вызовы из `EventDetailScreen`

**Тестирование:**
- [ ] Unit-тесты: навигация по коллекции, синхронизация pager/thumbnails
- [ ] Preview: различное количество фото (1, 3, 10+)
- [ ] UI-тест: клик по миниатюре, свайп pager

**Оценка:** 4-6 часов

### Итерация 3 — UX улучшения

- [ ] Share фото
- [ ] Save фото в галерею устройства
- [ ] Exif информация (если доступна)

### Опциональные

- [ ] Анимации открытия/закрытия
- [ ] `photo_report`: "Report photo" (для accessibility)

---

## Метрики успеха

- ✅ Время открытия фото < 300ms
- ✅ Нет crashes при открытии/закрытии
- ✅ Unit-тесты покрывают > 80% логики ViewModel
- ✅ Все строковые ресурсы локализованы
- ✅ Удаление фото работает корректно (API + локальное обновление UI)
