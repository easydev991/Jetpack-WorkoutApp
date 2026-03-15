# План разработки экрана PhotoDetailScreen

## Текущий статус: 100% завершено (11/11 этапов)

### Цель

Полноэкранный модальный экран для просмотра фото с удалением (автор) или жалобой (авторизованные).

---

## Выполненные этапы

| Этап | Описание | Файлы |
|------|----------|-------|
| 1. Domain Layer — Модели UI | `PhotoDetailUIState`, `PhotoDetailConfig`, `IPhotoDetailViewModel`, `PhotoDetailAction`, `PhotoDetailEvent` | `PhotoDetailUIState.kt`, `IPhotoDetailViewModel.kt` |
| 2. Domain Layer — ViewModel | `PhotoDetailViewModel` с SavedStateHandle, onAction, Factory | `PhotoDetailViewModel.kt` |
| 3. UI Layer — Screen | `PhotoDetailScreen`, `ZoomablePhotoView` (pinch-to-zoom, double-tap), `DeleteConfirmDialog` | `PhotoDetailScreen.kt`, `ZoomablePhotoView.kt` |
| 4. UI Layer — SheetHost | `PhotoDetailSheetHost` с ModalBottomSheet, блокировка dismiss, обработка events | `PhotoDetailSheetHost.kt` |
| 5. Интеграция | Интеграция с `EventDetailScreen` через `NavigateToPhotoDetail` event | `EventDetailViewModel.kt`, `EventDetailScreen.kt` |
| 6. Локализация | Строки для диалога удаления (en/ru) | `strings.xml` |
| 7. Unit-тесты | `PhotoDetailViewModelTest` (14 тестов) | `PhotoDetailViewModelTest.kt` |
| 8. UI Preview | 4 Preview: автор, авторизованный, неавторизованный, с диалогом | `PhotoDetailScreen.kt` |
| 9. Bugfix | Исправлен баг: всегда открывалась первая фотография вместо выбранной. Добавлен `key = "photo_${config.photoId}"` в viewModel() | `PhotoDetailSheetHost.kt:40` |
| 10. API интеграция | Реализовано удаление фото через API, локальное обновление UI, LoadingOverlay, обработка ошибок | `PhotoDetailViewModel.kt`, `PhotoDetailUIState.kt`, `PhotoDetailSheetHost.kt`, `EventDetailScreen.kt`, `EventDetailViewModel.kt`, `PhotoDetailScreen.kt`, `PhotoDetailViewModelTest.kt` |
| 11. Рефакторинг ошибок | Обработка ошибок через `UserNotifier.handleError()` вместо `errorMessage` в UI state | `PhotoDetailUIState.kt`, `PhotoDetailViewModel.kt`, `PhotoDetailSheetHost.kt`, `PhotoDetailViewModelTest.kt` |

---

## Доработки

*Нет активных доработок*

---

## Технический долг и улучшения (Future Iterations)

### Итерация 2 — Галерея

- [ ] Поддержка коллекции фотографий (swipe для переключения)
- [ ] Индикатор текущей фотографии (1/5)
- [ ] Анимации переходов между фото

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
