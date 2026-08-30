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
| Интеграция   | Связь с `EventDetailScreen` и `ParkDetailScreen` через `PhotoDetailSheetHost`, callback `onDismissed(deletedPhotoId: Long?)`          |
| Локализация  | Строки en/ru для диалога удаления                                                                                                   |
| Тестирование | unit-тесты `PhotoDetailViewModelTest`, unit-тесты `PhotoDetailSheetHostTest`, Preview                                          |
| API          | Удаление фото через `parksEventsRepository.deleteEventPhoto()`, LoadingOverlay, `UserNotifier.handleError()`                         |
| Bugfix       | Ключ ViewModel через `buildPhotoDetailViewModelKey`: `"photo_${ownerType.name}_${parentId}_${photoId}"` для корректного открытия выбранного фото (разные родители/типы не конфликтуют) |

---

## Доработки

*Нет активных доработок*

---

## Технический долг и улучшения (Future Iterations)

### Итерация 2 — Галерея (следующая)

**Задача:** Открытие выбранной фотографии с горизонтальной коллекцией миниатюр внизу. Не реализована — `PhotoDetailConfig` по-прежнему описывает одно фото.

**Текущий PhotoDetailConfig:**

```kotlin
data class PhotoDetailConfig(
    val photoId: Long,          // ID выбранного фото
    val parentId: Long,         // ID события/парка
    val parentTitle: String,
    val isAuthor: Boolean,
    val photoUrl: String,
    val ownerType: PhotoOwner   // PhotoOwner.Event | PhotoOwner.Park
)
```

**Целевые изменения в PhotoDetailConfig:**

```kotlin
data class PhotoDetailConfig(
    val photos: List<Photo>,        // было: одиночное фото (photoId + photoUrl)
    val selectedPhotoId: Long,      // ID фото для начального отображения
    val parentId: Long,
    val parentTitle: String,
    val isAuthor: Boolean,
    val ownerType: PhotoOwner
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
