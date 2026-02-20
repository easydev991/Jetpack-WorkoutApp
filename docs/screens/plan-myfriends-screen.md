# Экран списка друзей пользователя (MyFriendsScreen)

## Обзор

Экран для отображения списка друзей и входящих заявок на добавление в друзья для текущего авторизованного пользователя.

### Различие между экранами друзей

**Важно:** В приложении есть два разных экрана для работы с друзьями:

1. **`Screen.Friends`** (из вкладки "Сообщения") - экран выбора друга для начала чата
2. **`Screen.MyFriends`** (из профиля) - экран управления друзьями текущего пользователя (этот документ)

### Архитектурный подход: Offline-First с предзагрузкой

**Как это работает:**
1. `ProfileViewModel` при загрузке профиля вызывает `swRepository.getSocialUpdates(userId)`
2. `getSocialUpdates()` загружает параллельно: пользователя, друзей, заявки, черный список
3. Все данные сохраняются в кэш (UserDao) с флагами: `isFriend`, `isFriendRequest`, `isBlacklisted`
4. При открытии экрана `MyFriendsScreen` данные отображаются мгновенно из кэша

**Преимущества:** мгновенный UX, Offline-First, Single Source of Truth (UserDao), реактивный UI через Flow

---

## Реализованный функционал ✅

### Базовый экран

- [x] Навигация: маршрут `Screen.MyFriends` в `Destinations.kt`
- [x] Domain Layer: `FriendsListUiState` (Loading, Success, Error), `FriendsListViewModel`
- [x] UI Layer: `MyFriendsScreen` с Scaffold, TopAppBar, LazyColumn
- [x] Секции заявок и друзей с логикой скрытия пустых секций
- [x] Кнопка "Друзья" в профиле с навигацией

### Принятие/отклонение заявок

- [x] **UserDao**: методы `removeFriendRequest()` и `markAsFriend()`
- [x] **SWRepository.respondToFriendRequest**: синхронизация кэша с сервером
  - При принятии: API `acceptFriendRequest` + `markAsFriend` + `removeFriendRequest`
  - При отклонении: API `declineFriendRequest` + `removeFriendRequest`
- [x] **UI State**: состояние `isProcessing` для блокировки UI при выполнении запроса
- [x] **LoadingOverlayView**: индикатор загрузки поверх контента при обработке запроса

### Удаление друга

- [x] **RemoveFriendDialog**: диалог подтверждения удаления из друзей
- [x] **UserDao.decrementFriendsCount()**: уменьшение счетчика друзей текущего пользователя
- [x] Обновление `isFriend=false` в кэше при удалении друга

### UI доработки

- [x] Divider между секциями только при наличии обеих секций
- [x] Пустое состояние: сообщение "Друзей пока нет" / "No friends yet" вместо "Loading"
- [x] FriendsButton: скрытие текста про друзей при `friendsCount=0` (показывается только badge с заявками)
- [x] Кнопки профиля disabled при `isFriendActionLoading`

---

## Тестирование

### Автоматические тесты ✅

- **Unit-тесты**: `FriendsListViewModelTest`, `SWRepositoryFriendsTest`
- **UI тесты**: `MyFriendsScreenTest` (19 тестов)
- Все тесты проходят (100% успех)

### Ручное тестирование (требуется устройство)

- [ ] Переход: Профиль → "Друзья"
- [ ] Принятие заявки: индикатор загрузки, заявка исчезает, пользователь в "Друзья"
- [ ] Отклонение заявки: индикатор загрузки, заявка исчезает
- [ ] Удаление друга: диалог подтверждения, реактивное обновление UI
- [ ] Пустой список: отображение "Друзей пока нет"
- [ ] Работа без интернета: ошибка логируется, UI не крашится

---

## Файлы

| Файл | Назначение |
|------|------------|
| `MyFriendsScreen.kt` | UI экран списка друзей |
| `FriendsListViewModel.kt` | ViewModel для управления состоянием |
| `FriendsListUiState.kt` | Sealed class состояний UI |
| `IFriendsListViewModel.kt` | Интерфейс ViewModel |
| `MyFriendsScreenTest.kt` | UI инструментальные тесты |
| `FriendsListViewModelTest.kt` | Unit-тесты ViewModel |

---

## Будущие улучшения

- Навигация на профиль друга при клике
- Pull-to-refresh
- Поиск по списку друзей
- Анимации
