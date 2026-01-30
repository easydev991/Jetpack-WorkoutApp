# План локального хранения данных пользователя

## Обзор

Необходимо создать локальное хранение данных пользователя для быстрого доступа из любой точки приложения. Данные должны сохраняться при перезапуске и обновляться с сервера.

## Текущая ситуация

### Android (Jetpack-WorkoutApp)

**Существующее:**
- **SWRepository** - работает с сервером, возвращает `Result<T>`
- **UserPreferencesRepository** - хранит только `isAuthorized` в DataStore
- **AppSettingsDataStore** - настройки приложения (тема, иконка)
- **User** - модель данных пользователя

**Проблемы:**
- Данные пользователя не кэшируются локально
- Каждый запуск - загрузка профиля с сервера
- Нет кэша для друзей, заявок в друзья, черного списка

### iOS (SwiftUI-WorkoutApp) - референс

**DefaultsService** использует `@AppStorage` (UserDefaults) с JSON - это костыль, который не нужно копировать.

## Архитектурное решение

### Технологии (Best Practice для Android)

**Room** - для структурированных данных (User, друзья, заявки, черный список)
- Типобезопасный ORM
- Flow для реактивного доступа
- Индексы для быстрого поиска
- Миграции схемы

**DataStore** - только для простых настроек
- Счетчик непрочитанных сообщений
- Флаги (нужно обновление и т.д.)

### Стратегия кэширования

**Стратегия 5 из architecture.mdc (Online-first с кэшем для офлайн):**

```kotlin
suspend fun getUser(userId: Long): Result<User> = try {
    // 1. Сначала с сервера
    val remoteUser = swApi.getUser(userId)
    userDao.insert(remoteUser.toEntity())  // Сохраняем в Room
    Result.success(remoteUser)
} catch (e: IOException) {
    // 2. Ошибка сети - берем из кэша
    val cachedUser = userDao.getById(userId)
    if (cachedUser != null) {
        Log.i("UsersRepository", "Профиль загружен из кэша")
        Result.success(cachedUser.toDomain())
    } else {
        Result.failure(NetworkException("Не удалось загрузить профиль. Проверьте интернет", e))
    }
}
```

---

## Этап 1: Data Layer (Room) ✅

Реализован слой данных с Room: создан `UserEntity` с флагами категоризации, `UserDao` с Flow методами, добавлен в `SWDatabase`, созданы конвертеры `UserEntity` ↔ `User`.

---

## Этап 2: Расширение SWRepository с кэшированием ✅

Реализовано кэширование в `SWRepositoryImpl`: добавлены Flow методы для локального доступа (getCurrentUserFlow, getFriendsFlow, getFriendRequestsFlow, getBlacklistFlow), реализовано онлайн-обновление с fallback на кэш при ошибках сети для getUser() и getSocialUpdates(), добавлен метод clearUserData() для очистки всех данных пользователя.

---

## Этап 3: Dependency Injection ✅

Настроен DI в AppContainer: добавлен userDao в DefaultAppContainer, внедрен в SWRepositoryImpl.

---

## Этап 4: Использование в ViewModels ✅

### 4.1. Обновить ProfileViewModel ✅

**Файл:** `app/src/main/java/com/swparks/viewmodel/ProfileViewModel.kt`

**Реализовано:**
- Добавлен `swRepository` в конструктор ViewModel
- Добавлено свойство `currentUser: StateFlow<User?>` для подписки на данные текущего пользователя из кэша
- Обновлен factory метод в `AppContainer` для передачи `swRepository`
- Обновлен метод `loadProfile()` для использования `currentUser` из StateFlow (убран параметр `user`)
- Обновлен `ProfileUiState.Success` - убрано поле `user`, данные берутся из `currentUser`
- Добавлен автоматический вызов `loadProfile()` в `init` блоке для загрузки при изменении `currentUser`
- Проект собирается успешно, проверки ktlint проходят

**Реактивное обновление UI:**
- UI автоматически обновляется при изменении `currentUser` через Flow
- `ProfileRootScreen` использует `currentUser` из ViewModel вместо явной передачи параметра
- Убран параметр `user` из `ProfileRootScreen` - теперь он не требуется

```kotlin
class ProfileViewModel(
    private val countriesRepository: CountriesRepository,
    private val swRepository: SWRepository,
) : ViewModel() {

    // Подписываемся на текущего пользователя из кэша
    val currentUser: StateFlow<User?> = swRepository.getCurrentUserFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // UI State
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        // Автоматически загружаем данные при изменении currentUser
        loadProfile()
    }

    /**
     * Загружает данные профиля с информацией о стране и городе
     * Использует currentUser из StateFlow для получения данных пользователя
     */
    fun loadProfile() {
        viewModelScope.launch {
            val user = currentUser.value
            if (user == null) {
                _uiState.update { ProfileUiState.Error("Пользователь не авторизован") }
                return@launch
            }

            try {
                // Получаем страну пользователя
                val country =
                    user.countryID?.let { countryId ->
                        countriesRepository.getCountryById(countryId.toString())
                    }

                // Получаем город пользователя
                val city =
                    user.cityID?.let { cityId ->
                        countriesRepository.getCityById(cityId.toString())
                    }

                _uiState.update { ProfileUiState.Success(country, city) }
            } catch (e: Exception) {
                _uiState.update { ProfileUiState.Error("Ошибка загрузки профиля: ${e.message}") }
            }
        }
    }
}
```

### 4.2. Пример FriendsViewModel

```kotlin
class FriendsViewModel(
    private val swRepository: SWRepository,
    private val countriesRepository: CountriesRepository,
) : ViewModel() {

    sealed class FriendsUiState {
        data object Loading : FriendsUiState()
        data class Success(val friends: List<User>) : FriendsUiState()
        data class Error(val message: String) : FriendsUiState()
    }

    private val _uiState = MutableStateFlow<FriendsUiState>(FriendsUiState.Loading)
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    // Подписываемся на друзей из кэша
    init {
        viewModelScope.launch {
            swRepository.getFriendsFlow().collect { friends ->
                _uiState.update { FriendsUiState.Success(friends) }
            }
        }
    }
}
```

---

## Этап 5: Очистка данных при выходе

### 5.1. Логика logout

**Важно:** При выходе из учетной записи удаляются **ВСЕ данные пользователя**, КРОМЕ:
- Площадок (сейчас хранятся в JSON в bundle приложения, потом будут сохраняться в Room)
- Старых мероприятий (пока не реализованы)

### 5.2. Что удаляется при logout

**Данные пользователя:**
- Профиль текущего пользователя
- Список друзей
- Заявки в друзья
- Черный список
- Сообщения и диалоги
- Дневники и записи в дневниках
- Комментарии (к площадкам, мероприятиям, дневникам)

**Что НЕ удаляется:**
- Площадки (публичные данные, общие для всех пользователей)
- Старые мероприятия (публичные данные, пока не реализованы)

### 5.3. Реализация очистки

**Добавить метод в UserDao:**

```kotlin
@Query("DELETE FROM users")
suspend fun clearAll()
```

**Добавить метод в SWRepository:**

```kotlin
interface SWRepository {
    // ... существующие методы ...

    // Полная очистка данных пользователя при logout
    suspend fun clearUserData()
}
```

**Реализация в SWRepositoryImpl:**

```kotlin
override suspend fun clearUserData() {
    userDao.clearAll()
    // TODO: Добавить очистку других таблиц (дневники, сообщения) при их реализации
}
```

**Использование в ProfileRootScreen:**

```kotlin
LaunchedEffect(isLoggingOut) {
    if (isLoggingOut) {
        // 1. Удаляем все данные пользователя из БД и кэша
        appContainer?.swRepository?.clearUserData()

        // 2. Удаляем токен авторизации
        appContainer?.logoutUseCase?.invoke()

        // 3. Завершаем logout
        onLogoutComplete()
    }
}
```

### 5.4. Обоснование стратегии

**Почему удаляем все данные пользователя:**
- Безопасность: при выходе удаляются все персональные данные
- Приватность: сообщения и дневники не должны оставаться локально
- Чистота состояния: при повторном входе загружаем свежие данные с сервера

**Почему сохраняем площадки:**
- Публичные данные, общие для всех пользователей
- Хранятся в JSON в bundle приложения
- Не зависят от авторизации
- Будут сохранены в Room в будущем

**Почему сохраняем старые мероприятия:**
- Публичные данные, общие для всех пользователей
- Пока не реализованы
- Не зависят от авторизации

---

## Этап 6: Тестирование

### 6.1. Unit-тесты для UserDao

**Файл:** `app/src/test/java/com/swparks/data/database/UserDaoTest.kt`

Тесты:
- `getCurrentUserFlow_whenNoUser_thenEmitsNull`
- `getCurrentUserFlow_whenUserExists_thenEmitsUser`
- `getFriendsFlow_whenNoFriends_thenEmitsEmptyList`
- `insert_andGetById_thenReturnsUser`
- `markAsFriend_thenUpdatesIsFriendFlag`
- `clearAll_thenRemovesAllUsers`

### 6.2. Интеграционные тесты для SWRepository

**Файл:** `app/src/androidTest/java/com/swparks/data/repository/SWRepositoryUserCacheTest.kt`

Тесты:
- `getUser_whenNetworkError_thenReturnsCachedUser`
- `getSocialUpdates_whenNetworkError_thenReturnsCachedData`
- `getCurrentUserFlow_whenUserSaved_thenEmitsUser`

---

## Критерии завершения

### Этап 1: Room ✅

Реализован Room слой: созданы UserEntity, UserDao (с Flow методами), добавлен в SWDatabase, созданы конвертеры.

### Этап 2: SWRepository ✅

Реализовано кэширование: Flow методы, онлайн-обновление с fallback на кэш, clearUserData().

### Этап 3: DI ✅

Настроен DI: userDao добавлен в AppContainer, внедрен в SWRepositoryImpl.

### Этап 4: ViewModels ✅

- [x] `ProfileViewModel` обновлен для использования Flow
- [x] Реактивное обновление UI через `currentUser` из ViewModel
- [x] Убран параметр `user` из `ProfileRootScreen`
- [ ] Созданы примеры ViewModels для друзей/заявок

### Этап 5: Logout

- [ ] Добавлен метод `clearAll()` в `UserDao`
- [ ] Добавлен метод `clearUserData()` в `SWRepository`
- [ ] Реализована очистка всех данных пользователя при выходе
- [ ] Очистка сохраняет площадки и старые мероприятия

### Этап 6: Тестирование

- [ ] Написаны unit-тесты для `UserDao`
- [ ] Написаны интеграционные тесты для `SWRepository`

---

## Примечания

### Архитектурные решения

- **Room вместо JSON**: Структурированные данные, индексы, Flow
- **Единый UserEntity**: Один entity для разных контекстов (профиль, друзья, заявки)
- **Флаги категоризации**: `isFriend`, `isFriendRequest`, `isBlacklisted`
- **Flow для реактивности**: Автоматическое обновление UI при изменении данных
- **Online-first с кэшем**: Сначала сервер, потом кэш при ошибке сети

### Производительность

- **Индексы**: Room автоматически индексирует PrimaryKey
- **Оптимизированные запросы**: Запросы с фильтрами по флагам
- **Потоки данных**: Flow лениво загружает данные только при подписке

### Безопасность

- **SQL Injection**: Room защищает от SQL-инъекций
- **Типобезопасность**: Компилятор проверяет типы

---

## Приоритет задач

### Первая итерация

1. ~~Room слой~~ ✅
2. ~~SWRepository с кэшированием~~ ✅
3. ~~DI настройка~~ ✅
4. Обновить `ProfileViewModel`
5. Базовые тесты

### Вторая итерация

1. Обновить другие ViewModels (Friends, Messages и т.д.)
2. Реализовать очистку при logout
3. Расширить тестовое покрытие
