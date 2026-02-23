# План исправления: EmptyStateView на JournalsListScreen для чужих дневников

## Проблема

При открытии чужого дневника (userId != currentUser.id) на экране JournalsListScreen при первой загрузке на заднем плане виден EmptyStateView. Это происходит потому что:

1. Flow из `getJournalsUseCase` сразу возвращает данные из локальной БД (кэш)
2. Для чужого дневника кэш пустой → Flow эмитит пустой список
3. ViewModel устанавливает `Content(journals = emptyList())`
4. UI в `ContentScreen` показывает EmptyStateView при `journals.isEmpty()`

EmptyStateView должен показываться при первой загрузке только для дневников авторизованного пользователя (isOwner == true), так как у него может не быть дневников. Для чужих дневников EmptyStateView должен показываться только после завершения загрузки с сервера.

## Анализ текущего поведения

### UI State Flow

1. `InitialLoading` — начальное состояние (показывается LoadingOverlayView)
2. `Content(journals = [], isRefreshing = true)` — пустой кэш, синхронизация идет
3. После синхронизации:
   - Успех: `Content(journals = [данные])` или `Content(journals = [])` если на сервере пусто
   - Ошибка: `Error(message)` только если список пуст

### Текущая логика ContentScreen (строки 338-345)

```kotlin
if (journals.isEmpty()) {
    EmptyStateView(...) // Показывается для любого пустого списка
}
```

## Решение

Модифицировать условие показа EmptyStateView в `ContentScreen`:
- **Для владельца (isOwner == true)**: показывать EmptyStateView при любом пустом списке (текущее поведение)
- **Для чужого дневника (isOwner == false)**: показывать EmptyStateView только когда `isRefreshing == false` (загрузка завершена)

## Этапы реализации

### Этап 1: UI Layer — Модификация JournalsListScreen

**Файл:** `app/src/main/java/com/swparks/ui/screens/journals/JournalsListScreen.kt`

- [ ] Модифицировать функцию `ContentScreen`:
  - Добавить параметр `isOwner: Boolean` (уже есть, передается снаружи)
  - Изменить условие показа EmptyStateView
  
- [ ] Изменить логику внутри `PullToRefreshBox` (строки ~338-345):

  ```kotlin
  if (journals.isEmpty()) {
      // Для чужого дневника показывать EmptyStateView только после завершения загрузки
      if (isOwner || !isRefreshing) {
          EmptyStateView(...)
      }
      // Для чужого дневника при isRefreshing == true показывать пустой Box
      // или можно показать LoadingOverlayView вместо EmptyStateView
  }
  ```

- [ ] Обновить Preview функции если нужно (добавить isOwner в параметры)

### Этап 2: Тестирование

**Файл:** `app/src/androidTest/java/com/swparks/ui/screens/journals/JournalsListScreenTest.kt`

- [ ] Добавить тест: при открытии чужого дневника с пустым кэшем EmptyStateView НЕ отображается пока идет загрузка
- [ ] Добавить тест: при открытии чужого дневника с пустым кэшем после завершения загрузки EmptyStateView отображается
- [ ] Убедиться что тесты для собственного дневника не сломаны (EmptyStateView должен показываться сразу)

**Файл:** `app/src/test/java/com/swparks/ui/viewmodel/JournalsViewModelTest.kt`

- [ ] Проверить что ViewModel корректно устанавливает isRefreshing

### Этап 3: Ручное тестирование

- [ ] Запустить приложение
- [ ] Открыть дневник авторизованного пользователя → убедиться что EmptyStateView показывается если нет дневников
- [ ] Открыть чужой дневник → убедиться что EmptyStateView не мелькает при загрузке
- [ ] Открыть чужой дневник без записей → убедиться что EmptyStateView показывается после загрузки

## Технические детали

### Параметры ContentScreen

- `journals: List<Journal>` — список дневников
- `isRefreshing: Boolean` — признак загрузки/обновления
- `isOwner: Boolean` — признак владельца (true = авторизованный пользователь)

### Условие показа EmptyStateView

```kotlin
when {
    // Для владельца: показывать всегда при пустом списке
    isOwner && journals.isEmpty() -> show EmptyStateView
    
    // Для чужого: показывать только после завершения загрузки
    !isOwner && journals.isEmpty() && !isRefreshing -> show EmptyStateView
    
    // В остальных случаях: не показывать EmptyStateView
    else -> show empty Box или LoadingOverlayView
}
```

## Критерии завершения

- [ ] EmptyStateView не мелькает при открытии чужого дневника
- [ ] EmptyStateView по-прежнему показывается для собственного дневника при отсутствии записей
- [ ] EmptyStateView показывается для чужого дневника если на сервере действительно нет дневников
- [ ] Все существующие тесты проходят
- [ ] Новые тесты добавлены и проходят
- [ ] `make lint` проходит без ошибок
- [ ] `make test` проходит без ошибок
