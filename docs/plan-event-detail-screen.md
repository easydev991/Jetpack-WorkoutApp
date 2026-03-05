# План разработки экрана EventDetailScreen

## Обзор

Разработка экрана детальной информации о мероприятии для Android-приложения по аналогии с iOS-версией (`EventDetailsScreen.swift`).

### Ссылки на референсы

- **iOS:** `SwiftUI-WorkoutApp/SwiftUI-WorkoutApp/Screens/Events/EventDetailsScreen.swift`
- **Android навигация:** `app/src/main/java/com/swparks/navigation/Destinations.kt` (Screen.EventDetail уже определён)

### Структура экрана (сверху вниз)

| Секция | Описание | Компонент |
|--------|----------|-----------|
| Заголовок | Локализованный текст "Event" | `Text` с `MaterialTheme.typography.labelMedium` |
| Название | Крупным шрифтом title мероприятия | `Text` с `MaterialTheme.typography.headlineMedium` |
| Дата проведения | Форматированная дата | `DateFormatter.formatDate()` |
| Место проведения | Адрес (страна, город) | `Text` |
| Локация площадки | Карта + адрес + кнопка маршрута | **НОВЫЙ:** `ParkLocationInfoView` |
| Участники | Количество участников + toggle "Пойду" | `SectionView` + `FormRowView` (toggle) |
| Фотографии | Сетка фотографий | **НОВЫЙ:** `PhotoSectionView` |
| Описание | Текст описания (без HTML) | `Text` с `event.parsedDescription` |
| Автор | Информация об организаторе | ✅ `UserRowView` (существует) |
| Комментарии | Список комментариев | ✅ `CommentRowView` (существует) |

---

## Этап 1: Новые компоненты дизайн-системы

### 1.1. Компонент ParkLocationInfoView

**Файл:** `app/src/main/java/com/swparks/ui/ds/ParkLocationInfoView.kt`

**Описание:** Компонент для отображения локации площадки со статичным снапшотом карты, адресом и кнопкой построения маршрута.

**Параметры:**

```kotlin
data class ParkLocationInfoConfig(
    val latitude: String,
    val longitude: String,
    val address: String,
    val onRouteClick: () -> Unit
)
```

**Структура:**
- Снапшот карты (статичное изображение) - использовать Google Maps Static API или OpenStreetMap
- Текстовый адрес
- Кнопка "Построить маршрут" → открывает Google Maps через Intent

**Зависимости:**
- Coil для загрузки изображения карты
- Android Intent для открытия Google Maps навигации

**Пример Google Maps Static API:**

```
https://maps.googleapis.com/maps/api/staticmap?center=$lat,$lon&zoom=15&size=400x200&markers=$lat,$lon&key=API_KEY
```

**Альтернатива без API ключа (OpenStreetMap):**

```
https://staticmap.openstreetmap.de/staticmap.php?center=$lat,$lon&zoom=15&size=400x200&markers=$lat,$lon
```

**Критерии завершения:**
- [ ] Компонент отображает снапшот карты
- [ ] Отображается адрес
- [ ] Кнопка открывает Google Maps с координатами
- [ ] Поддержка темной темы
- [ ] Preview функции для светлой/темной темы

---

### 1.2. Компонент PhotoSectionView

**Файл:** `app/src/main/java/com/swparks/ui/ds/PhotoSectionView.kt`

**Описание:** Адаптивная сетка фотографий с поддержкой полноэкранного просмотра.

**Параметры:**

```kotlin
data class PhotoSectionConfig(
    val photos: List<Photo>,
    val canDelete: Boolean,
    val onPhotoClick: (Photo) -> Unit,
    val onDeleteClick: ((Photo) -> Unit)? = null,
    val onReportClick: (() -> Unit)? = null
)
```

**Структура:**
- Адаптивная сетка:
  - 1 фото → 1 столбец (на всю ширину)
  - 2 фото → 2 столбца
  - 3+ фото → 3 столбца
- Использовать `LazyVerticalGrid` или `Row` с `Modifier.weight()`
- Каждый элемент - `SWAsyncImage` с закругленными углами
- При клике → переход на экран галереи (EventGallery)

**Критерии завершения:**
- [ ] Адаптивная сетка 1/2/3 столбца
- [ ] Корректная загрузка изображений через Coil
- [ ] Обработка клика на фото (логирование)
- [ ] Поддержка темной темы
- [ ] Preview функции

---

## Этап 2: UI State и ViewModel

### 2.1. UI State

**Файл:** `app/src/main/java/com/swparks/ui/state/EventDetailUIState.kt`

**Структура:**

```kotlin
sealed class EventDetailUIState {
    data object InitialLoading : EventDetailUIState()
    data class Content(
        val event: Event,
        val address: String,
        val isLoading: Boolean = false,
        val isParticipating: Boolean = false
    ) : EventDetailUIState()
    data class Error(val message: String?) : EventDetailUIState()
}
```

**Критерии завершения:**
- [ ] Sealed class с состояниями InitialLoading, Content, Error
- [ ] Content содержит все необходимые данные для отображения

---

### 2.2. ViewModel Interface

**Файл:** `app/src/main/java/com/swparks/ui/viewmodel/IEventDetailViewModel.kt`

**Методы:**

```kotlin
interface IEventDetailViewModel {
    val uiState: StateFlow<EventDetailUIState>
    val isRefreshing: StateFlow<Boolean>
    val isAuthorized: StateFlow<Boolean>
    
    fun refresh()
    fun onBackClick()
    fun onEditClick()
    fun onShareClick()
    fun onParticipantToggle()
    fun onParticipantsCountClick()
    fun onAuthorClick()
    fun onRouteClick()
    fun onPhotoClick(photo: Photo)
    fun onAddCommentClick()
    fun onCommentActionClick(commentId: Long, action: CommentAction)
}
```

**Критерии завершения:**
- [ ] Интерфейс определён
- [ ] Все необходимые методы описаны

---

### 2.3. ViewModel Implementation

**Файл:** `app/src/main/java/com/swparks/ui/viewmodel/EventDetailViewModel.kt`

**Зависимости:**
- `SWRepository` - для `getEvent(id: Long)`
- `CountriesRepository` - для получения адреса
- `UserPreferencesRepository` - для проверки авторизации
- `Logger` - для логирования
- `UserNotifier` - для обработки ошибок

**Инициализация:**
- Получить `eventId` из SavedStateHandle
- Загрузить данные мероприятия через `repository.getEvent(id)`
- Загрузить адрес через `countriesRepository`

**Factory:**

```kotlin
companion object {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as JetpackWorkoutApplication
            val savedStateHandle = createSavedStateHandle()
            val eventId = savedStateHandle.get<Long>("eventId") ?: 0L
            // ... создание ViewModel
        }
    }
}
```

**Логирование:**
- Все действия логируются через `logger.d(TAG, "...")`
- При нажатии на любой элемент - логировать событие

**Критерии завершения:**
- [ ] ViewModel загружает данные мероприятия
- [ ] Обработка состояний загрузки/ошибки
- [ ] Factory с получением eventId из SavedStateHandle
- [ ] Все методы логируют действия в консоль

---

## Этап 3: UI Screen

### 3.1. EventDetailScreen

**Файл:** `app/src/main/java/com/swparks/ui/screens/events/EventDetailScreen.kt`

**Структура Composable:**

```kotlin
@Composable
fun EventDetailScreen(
    modifier: Modifier = Modifier,
    viewModel: IEventDetailViewModel = viewModel<EventDetailViewModel>(factory = EventDetailViewModel.Factory),
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onShare: (String) -> Unit,
    onParticipants: (Long) -> Unit,
    onAuthor: (Int) -> Unit,
    onGallery: (Long) -> Unit,
    onAddComment: (Long) -> Unit
)
```

**Основные компоненты:**

1. **Scaffold с TopAppBar**
   - Кнопка "Назад"
   - Название "Event" (локализованное)
   - Меню с действиями (редактировать, поделиться) - если автор

2. **PullToRefreshBox** для обновления данных

3. **Секции (в LazyColumn):**
   - Заголовок мероприятия (title) - крупный шрифт
   - Дата проведения
   - Место проведения (адрес)
   - ParkLocationInfoView (карта + кнопка маршрута)
   - Секция участников (SectionView + количество + toggle "Пойду")
   - PhotoSectionView (если есть фото)
   - Описание (если есть)
   - Автор (UserRowView)
   - Комментарии (SectionView + список CommentRowView)
   - Кнопка "Добавить комментарий"

4. **LoadingOverlayView** для первичной загрузки

**Критерии завершения:**
- [ ] Экран отображает все секции
- [ ] Pull-to-refresh работает
- [ ] LoadingOverlayView показывается при первичной загрузке
- [ ] Все клики логируются
- [ ] Поддержка темной темы

---

## Этап 4: Локализация

### 4.1. Строковые ресурсы

**Файл:** `app/src/main/res/values/strings.xml` (английский)
**Файл:** `app/src/main/res/values-ru/strings.xml` (русский)

**Необходимые строки:**

| Ключ | EN | RU |
|------|----|----|
| `event_detail_title` | Event | Мероприятие |
| `event_participants` | Participants | Участники |
| `event_will_attend` | I will attend | Пойду |
| `event_photos` | Photos | Фотографии |
| `event_description` | Description | Описание |
| `event_author` | Organizer | Организатор |
| `event_comments` | Comments | Комментарии |
| `event_add_comment` | Add comment | Добавить комментарий |
| `event_build_route` | Build route | Построить маршрут |
| `event_edit` | Edit event | Редактировать |
| `event_share` | Share | Поделиться |
| `event_no_description` | No description | Нет описания |

**Критерии завершения:**
- [ ] Все строки локализованы на русский и английский
- [ ] Использованы в UI компонентах

---

## Этап 5: Навигация

### 5.1. Интеграция в Navigation.kt

**Файл:** `app/src/main/java/com/swparks/navigation/Navigation.kt`

**Добавить:**
- Composable для `Screen.EventDetail`
- Получение `eventId` и `source` из аргументов
- Передача callback-ов для навигации

**Пример:**

```kotlin
composable(
    route = Screen.EventDetail.route,
    arguments = listOf(
        navArgument("eventId") { type = NavType.LongType },
        navArgument("source") { type = NavType.StringType; defaultValue = "events" }
    )
) { backStackEntry ->
    val eventId = backStackEntry.arguments?.getLong("eventId") ?: 0L
    EventDetailScreen(
        onBack = { navController.popBackStack() },
        onEdit = { id -> navController.navigate(Screen.EditEvent.createRoute(id, source)) },
        // ... другие callback-и
    )
}
```

### 5.2. Обновление EventsViewModel

**Файл:** `app/src/main/java/com/swparks/ui/viewmodel/EventsViewModel.kt`

**Добавить:**
- Навигация на EventDetailScreen при клике на мероприятие
- Передача source параметра

**Критерии завершения:**
- [ ] Навигация работает из списка мероприятий
- [ ] Корректная передача eventId
- [ ] Кнопка "Назад" работает

---

## Этап 6: Тестирование

### 6.1. Unit-тесты ViewModel

**Файл:** `app/src/test/java/com/swparks/ui/viewmodel/EventDetailViewModelTest.kt`

**Тест-кейсы:**
- [ ] Успешная загрузка мероприятия
- [ ] Обработка ошибки сети
- [ ] Обработка ошибки сервера
- [ ] Pull-to-refresh обновляет данные
- [ ] Логирование действий

### 6.2. Preview компонентов

**Добавить Preview для:**
- [ ] ParkLocationInfoView
- [ ] PhotoSectionView (с 1, 2, 3+ фото)
- [ ] EventDetailScreen (состояния: loading, content, error)

---

## Порядок реализации

1. **Этап 1.1** → ParkLocationInfoView (новый компонент)
2. **Этап 1.2** → PhotoSectionView (новый компонент)
3. **Этап 2.1** → EventDetailUIState
4. **Этап 2.2** → IEventDetailViewModel
5. **Этап 2.3** → EventDetailViewModel
6. **Этап 4** → Локализация
7. **Этап 3** → EventDetailScreen
8. **Этап 5** → Навигация
9. **Этап 6** → Тестирование

---

## Существующие компоненты (переиспользование)

| Компонент | Файл | Статус |
|-----------|------|--------|
| `SectionView` | `ui/ds/SectionView.kt` | ✅ Готов |
| `UserRowView` | `ui/ds/UserRowView.kt` | ✅ Готов |
| `CommentRowView` | `ui/ds/CommentRowView.kt` | ✅ Готов |
| `LoadingOverlayView` | `ui/ds/LoadingOverlayView.kt` | ✅ Готов |
| `FormCardContainer` | `ui/ds/FormCardContainer.kt` | ✅ Готов |
| `FormRowView` | `ui/ds/FormRowView.kt` | ✅ Готов |
| `SWAsyncImage` | `ui/ds/SWAsyncImage.kt` | ✅ Готов |
| `ErrorContentView` | `ui/ds/ErrorContentView.kt` | ✅ Готов |
| `EmptyStateView` | `ui/ds/EmptyStateView.kt` | ✅ Готов |

---

## Примечания

1. **Карты:** Для снапшота карты можно использовать OpenStreetMap Static API (без API ключа) или Google Maps Static API (требует ключ).

2. **Toggle "Пойду":** В первой реализации только логируем действие. Оптимистичное обновление UI и API запрос добавляются позже.

3. **Комментарии:** Редактирование и удаление комментариев пока только логируются.

4. **Фото:** Полноэкранный просмотр (EventGallery) будет реализован отдельной задачей.

5. **Календарь:** Добавление в календарь не включено в первую итерацию (iOS имеет эту функцию).
