# План разработки экрана EventDetailScreen

## Обзор

Разработка экрана детальной информации о мероприятии для Android-приложения по аналогии с iOS-версией (`EventDetailsScreen.swift`).

### Ссылки на референсы

- **iOS:** `SwiftUI-WorkoutApp/SwiftUI-WorkoutApp/Screens/Events/EventDetailsScreen.swift`
- **Android навигация:** `app/src/main/java/com/swparks/navigation/Destinations.kt` (Screen.EventDetail уже определён)

### Структура экрана (сверху вниз)

| Секция               | Описание                               | Компонент                         | Авторизация                        |
|----------------------|----------------------------------------|-----------------------------------|------------------------------------|
| Заголовок            | Локализованный текст "Event"           | `Text`                            | Все                                |
| Название             | Крупным шрифтом title мероприятия      | `Text`                            | Все                                |
| Дата проведения      | Форматированная дата                   | `DateFormatter.formatDate()`      | Все                                |
| Место проведения     | Адрес (страна, город)                  | `Text`                            | Все                                |
| Локация площадки     | Карта + адрес + кнопка маршрута        | **НОВЫЙ:** `ParkLocationInfoView` | Все                                |
| Участники            | Количество участников + toggle "Пойду" | `SectionView` + `FormRowView`     | **Только авторизованные**          |
| Фотографии           | Сетка фотографий                       | **НОВЫЙ:** `PhotoSectionView`     | Все                                |
| Описание             | Текст описания (без HTML)              | `Text`                            | Все                                |
| Автор                | Информация об организаторе             | ✅ `UserRowView`                   | Все (клик - только авторизованные) |
| Комментарии          | Список комментариев                    | ✅ `CommentRowView`                | Все                                |
| Добавить комментарий | Кнопка добавления                      | `Button`                          | **Только авторизованные**          |

---

## Авторизация и различия функционала

### Что СКРЫВАЕТСЯ для неавторизованных (UI не отображается)

| Элемент                       | Условие скрытия          |
|-------------------------------|--------------------------|
| Секция участников целиком     | `!isAuthorized`          |
| Toggle "Пойду на мероприятие" | Внутри секции участников |
| Кнопка "Добавить комментарий" | `!isAuthorized`          |
| Меню редактирования/удаления  | `!isEventAuthor`         |

### Что БЛОКИРУЕТСЯ для неавторизованных (UI виден, действие недоступно)

| Действие                              | Реализация                               |
|---------------------------------------|------------------------------------------|
| Переход на профиль автора мероприятия | Клик логируется, навигация не вызывается |
| Переход на профиль автора комментария | Клик логируется, навигация не вызывается |

### Что ДОСТУПНО всем (включая неавторизованных)

- Просмотр заголовка, описания, даты, места
- Просмотр карты и кнопка "Построить маршрут"
- Просмотр фотографий (открытие галереи)
- Просмотр организатора (без перехода на профиль)
- Просмотр комментариев
- Пожаловаться на фото/комментарий
- Поделиться мероприятием

### Логика isEventAuthor

```kotlin
// В ViewModel:
val isEventAuthor: Boolean
    get() = isAuthorized.value && (uiState.value as? EventDetailUIState.Content)
        ?.event?.author?.id == currentUserId
```

---

## Первая итерация: ТОЛЬКО логирование

**Важно:** В первой итерации все действия пользователей **только логируются** в консоль.

| Действие             | Реализация в первой итерации                                                     |
|----------------------|----------------------------------------------------------------------------------|
| Toggle "Пойду"       | `logger.d(TAG, "onParticipantToggle: userId=$userId, eventId=$eventId")`         |
| Клик на автора       | `logger.d(TAG, "onAuthorClick: authorId=$authorId, isAuthorized=$isAuthorized")` |
| Клик на комментарий  | `logger.d(TAG, "onCommentClick: commentId=$commentId")`                          |
| Клик на фото         | `logger.d(TAG, "onPhotoClick: photoId=$photoId")`                                |
| Клик "Маршрут"       | `logger.d(TAG, "onRouteClick: lat=$lat, lon=$lon")`                              |
| Клик "Поделиться"    | `logger.d(TAG, "onShareClick: eventId=$eventId")`                                |
| Клик "Редактировать" | `logger.d(TAG, "onEditClick: eventId=$eventId")`                                 |
| Добавить комментарий | `logger.d(TAG, "onAddCommentClick: eventId=$eventId")`                           |
| Пожаловаться         | `logger.d(TAG, "onReportClick: type=$type, id=$id")`                             |

**Навигация и API запросы добавляются в следующих итерациях.**

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
        val isParticipating: Boolean = false,
        val currentUserId: Int? = null  // ID текущего пользователя для проверки isEventAuthor
    ) : EventDetailUIState()
    data class Error(val message: String?) : EventDetailUIState()
}
```

**Критерии завершения:**
- [ ] Sealed class с состояниями InitialLoading, Content, Error
- [ ] Content содержит все необходимые данные для отображения
- [ ] Content содержит currentUserId для проверки авторства

---

### 2.2. ViewModel Interface

**Файл:** `app/src/main/java/com/swparks/ui/viewmodel/IEventDetailViewModel.kt`

**Свойства и методы:**

```kotlin
interface IEventDetailViewModel {
    val uiState: StateFlow<EventDetailUIState>
    val isRefreshing: StateFlow<Boolean>
    val isAuthorized: StateFlow<Boolean>
    val isEventAuthor: StateFlow<Boolean>  // Является ли текущий пользователь автором мероприятия

    // Навигация
    fun onBackClick()
    fun onEditClick()          // Только для автора (isEventAuthor)
    fun onShareClick()

    // Участие
    fun onParticipantToggle()  // Только для авторизованных
    fun onParticipantsCountClick()  // Только для авторизованных

    // Профили
    fun onAuthorClick()        // Блокируется для неавторизованных
    fun onCommentAuthorClick(commentId: Long)  // Блокируется для неавторизованных

    // Локация
    fun onRouteClick()         // Доступно всем

    // Фото
    fun onPhotoClick(photo: Photo)
    fun onPhotoReportClick(photo: Photo)  // Доступно всем (кроме автора - у него delete)

    // Комментарии
    fun onAddCommentClick()    // Только для авторизованных
    fun onCommentActionClick(commentId: Long, action: CommentAction)

    // Обновление
    fun refresh()
}
```

**Критерии завершения:**
- [ ] Интерфейс определён
- [ ] isAuthorized и isEventAuthor для условного отображения UI
- [ ] Все методы описаны с учётом авторизации

---

### 2.3. ViewModel Implementation

**Файл:** `app/src/main/java/com/swparks/ui/viewmodel/EventDetailViewModel.kt`

**Зависимости:**
- `SWRepository` - для `getEvent(id: Long)`
- `CountriesRepository` - для получения адреса
- `UserPreferencesRepository` - для проверки авторизации и получения currentUserId
- `Logger` - для логирования
- `UserNotifier` - для обработки ошибок

**Инициализация:**
- Получить `eventId` из SavedStateHandle
- Загрузить данные мероприятия через `repository.getEvent(id)`
- Загрузить адрес через `countriesRepository`
- Получить currentUserId из preferences для проверки isEventAuthor

**Логика isEventAuthor:**

```kotlin
private val _isEventAuthor = MutableStateFlow(false)
val isEventAuthor: StateFlow<Boolean> = _isEventAuthor

// Обновляется при загрузке данных:
_isEventAuthor.value = isAuthorized && event.author?.id == currentUserId
```

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

**Первая итерация - только логирование:**

Все методы в первой итерации только логируют действия:

```kotlin
override fun onParticipantToggle() {
    logger.d(TAG, "onParticipantToggle: eventId=$eventId, isAuthorized=${isAuthorized.value}")
}

override fun onAuthorClick() {
    val authorId = (uiState.value as? EventDetailUIState.Content)?.event?.author?.id
    logger.d(TAG, "onAuthorClick: authorId=$authorId, isAuthorized=${isAuthorized.value}")
    // Навигация не вызывается в первой итерации
}

override fun onEditClick() {
    logger.d(TAG, "onEditClick: eventId=$eventId, isEventAuthor=${isEventAuthor.value}")
    // Навигация не вызывается в первой итерации
}
// ... остальные методы аналогично
```

**Критерии завершения:**
- [ ] ViewModel загружает данные мероприятия
- [ ] Обработка состояний загрузки/ошибки
- [ ] Factory с получением eventId из SavedStateHandle
- [ ] isAuthorized и isEventAuthor корректно вычисляются
- [ ] Все методы логируют действия в консоль
- [ ] Навигация и API не вызываются (только логирование)

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
    // Callback-и для будущей навигации (пока не вызываются, только логируются)
    onEdit: (Long) -> Unit = {},
    onShare: (String) -> Unit = {},
    onParticipants: (Long) -> Unit = {},
    onAuthor: (Int) -> Unit = {},
    onGallery: (Long) -> Unit = {},
    onAddComment: (Long) -> Unit = {},
    onRoute: (String, String) -> Unit = {}
)
```

**Условное отображение по авторизации:**

```kotlin
val isAuthorized by viewModel.isAuthorized.collectAsState()
val isEventAuthor by viewModel.isEventAuthor.collectAsState()

// В LazyColumn:

// 1. Секция участников - ТОЛЬКО для авторизованных
if (isAuthorized) {
    ParticipantsSection(
        event = event,
        onToggle = viewModel::onParticipantToggle,
        onCountClick = viewModel::onParticipantsCountClick
    )
}

// 2. Кнопка "Добавить комментарий" - ТОЛЬКО для авторизованных
if (isAuthorized) {
    Button(onClick = viewModel::onAddCommentClick) {
        Text(stringResource(R.string.event_add_comment))
    }
}

// 3. Меню редактирования - ТОЛЬКО для автора мероприятия
if (isEventAuthor) {
    IconButton(onClick = viewModel::onEditClick) {
        Icon(Icons.Default.Edit, contentDescription = "Edit")
    }
}

// 4. Автор - виден всем, но клик работает только для авторизованных
UserRowView(
    user = event.author,
    onClick = {
        if (isAuthorized) {
            viewModel.onAuthorClick()
            // onAuthor(authorId) - в следующей итерации
        } else {
            viewModel.onAuthorClick()  // Логирует с isAuthorized=false
        }
    }
)
```

**Основные компоненты:**

1. **Scaffold с TopAppBar**
   - Кнопка "Назад" → `onBack()`
   - Название "Event" (локализованное)
   - Меню с действиями:
     - Редактировать (если `isEventAuthor`)
     - Поделиться (всегда)

2. **PullToRefreshBox** для обновления данных

3. **Секции (в LazyColumn):**
   - Заголовок мероприятия (title) - крупный шрифт
   - Дата проведения
   - Место проведения (адрес)
   - ParkLocationInfoView (карта + кнопка маршрута) - доступно всем
   - **Секция участников** - `if (isAuthorized)` → скрыта для неавторизованных
   - PhotoSectionView (если есть фото) - доступно всем
   - Описание (если есть)
   - Автор (UserRowView) - виден всем, клик логируется
   - Комментарии (SectionView + список CommentRowView) - видны всем
   - **Кнопка "Добавить комментарий"** - `if (isAuthorized)` → скрыта для неавторизованных

4. **LoadingOverlayView** для первичной загрузки

**Критерии завершения:**
- [ ] Экран отображает все секции
- [ ] Pull-to-refresh работает
- [ ] LoadingOverlayView показывается при первичной загрузке
- [ ] Секция участников скрыта для неавторизованных
- [ ] Кнопка "Добавить комментарий" скрыта для неавторизованных
- [ ] Меню редактирования показывается только автору
- [ ] Все клики логируются через ViewModel
- [ ] Поддержка темной темы

---

## Этап 4: Локализация

### 4.1. Строковые ресурсы

**Файл:** `app/src/main/res/values/strings.xml` (английский)
**Файл:** `app/src/main/res/values-ru/strings.xml` (русский)

**Необходимые строки:**

| Ключ                   | EN             | RU                   |
|------------------------|----------------|----------------------|
| `event_detail_title`   | Event          | Мероприятие          |
| `event_participants`   | Participants   | Участники            |
| `event_will_attend`    | I will attend  | Пойду                |
| `event_photos`         | Photos         | Фотографии           |
| `event_description`    | Description    | Описание             |
| `event_author`         | Organizer      | Организатор          |
| `event_comments`       | Comments       | Комментарии          |
| `event_add_comment`    | Add comment    | Добавить комментарий |
| `event_build_route`    | Build route    | Построить маршрут    |
| `event_edit`           | Edit event     | Редактировать        |
| `event_share`          | Share          | Поделиться           |
| `event_no_description` | No description | Нет описания         |

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

| Компонент            | Файл                          | Статус  |
|----------------------|-------------------------------|---------|
| `SectionView`        | `ui/ds/SectionView.kt`        | ✅ Готов |
| `UserRowView`        | `ui/ds/UserRowView.kt`        | ✅ Готов |
| `CommentRowView`     | `ui/ds/CommentRowView.kt`     | ✅ Готов |
| `LoadingOverlayView` | `ui/ds/LoadingOverlayView.kt` | ✅ Готов |
| `FormCardContainer`  | `ui/ds/FormCardContainer.kt`  | ✅ Готов |
| `FormRowView`        | `ui/ds/FormRowView.kt`        | ✅ Готов |
| `SWAsyncImage`       | `ui/ds/SWAsyncImage.kt`       | ✅ Готов |
| `ErrorContentView`   | `ui/ds/ErrorContentView.kt`   | ✅ Готов |
| `EmptyStateView`     | `ui/ds/EmptyStateView.kt`     | ✅ Готов |

---

## Примечания

1. **Карты:** Для снапшота карты можно использовать OpenStreetMap Static API (без API ключа) или Google Maps Static API (требует ключ).

2. **Первая итерация - только логирование:** Все действия пользователей (toggle, клики, навигация) только логируются в консоль через `logger.d()`. Навигация и API запросы добавляются в следующих итерациях.

3. **Авторизация:** Экран должен корректно работать для:
   - **Неавторизованных пользователей** - скрыты секция участников и кнопка добавления комментария
   - **Авторизованных пользователей** - полный функционал просмотра
   - **Авторов мероприятия** - дополнительное меню редактирования/удаления

4. **Фото:** Полноэкранный просмотр (EventGallery) будет реализован отдельной задачей. В первой итерации клик на фото только логируется.

5. **Календарь:** Добавление в календарь не включено в первую итерацию (iOS имеет эту функцию).

---

## Следующие итерации (не включены в первую)

1. **Итерация 2 - Навигация:**
   - Переход на профиль автора
   - Переход на экран участников
   - Переход на экран галереи
   - Переход на редактирование мероприятия

2. **Итерация 3 - API интеграция:**
   - Toggle "Пойду" с оптимистичным обновлением
   - Добавление комментария
   - Удаление/редактирование мероприятия (для автора)

3. **Итерация 4 - Расширенный функционал:**
   - Добавление в календарь
   - Пожаловаться на фото/комментарий
   - Pull-to-refresh с реальными данными
