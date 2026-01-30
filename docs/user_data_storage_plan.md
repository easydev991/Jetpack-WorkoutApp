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

## Этап 1: Data Layer (Room)

### 1.1. Создать Room Entities

**Файл:** `app/src/main/java/com/swparks/data/database/UserEntity.kt`

```kotlin
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val image: String,
    val cityId: Int? = null,
    val countryId: Int? = null,
    val birthDate: String? = null,
    val email: String? = null,
    val fullName: String? = null,
    val genderCode: Int? = null,
    val friendRequestCount: String? = null,
    val friendsCount: Int? = null,
    val parksCount: String? = null,
    val journalCount: Int? = null,
    val lang: String,
    // Для списков друзей и черного списка - индекс для быстрого поиска
    val isFriend: Boolean = false,
    val isFriendRequest: Boolean = false,
    val isBlacklisted: Boolean = false,
    val isCurrentUser: Boolean = false
)
```

**Почему один Entity для User?**
- Упрощает схему
- Одни данные для всех контекстов (профиль, друзья, заявки, черный список)
- Флаги для категоризации (`isFriend`, `isFriendRequest`, `isBlacklisted`)

### 1.2. Создать UserDao

**Файл:** `app/src/main/java/com/swparks/data/database/UserDao.kt`

```kotlin
@Dao
interface UserDao {
    // Основной пользователь
    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    fun getCurrentUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserByIdFlow(userId: Long): Flow<UserEntity?>

    // Друзья
    @Query("SELECT * FROM users WHERE isFriend = 1 ORDER BY name ASC")
    fun getFriendsFlow(): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM users WHERE isFriend = 1")
    fun getFriendsCountFlow(): Flow<Int>

    // Заявки в друзья
    @Query("SELECT * FROM users WHERE isFriendRequest = 1 ORDER BY name ASC")
    fun getFriendRequestsFlow(): Flow<List<UserEntity>>

    // Черный список
    @Query("SELECT * FROM users WHERE isBlacklisted = 1 ORDER BY name ASC")
    fun getBlacklistFlow(): Flow<List<UserEntity>>

    // Вставка и обновление
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserEntity>)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteById(userId: Long)

    // Обновление флагов
    @Query("UPDATE users SET isFriend = 1 WHERE id = :userId")
    suspend fun markAsFriend(userId: Long)

    @Query("UPDATE users SET isFriend = 0 WHERE id = :userId")
    suspend fun removeFriend(userId: Long)

    @Query("UPDATE users SET isBlacklisted = 1 WHERE id = :userId")
    suspend fun addToBlacklist(userId: Long)

    @Query("UPDATE users SET isBlacklisted = 0 WHERE id = :userId")
    suspend fun removeFromBlacklist(userId: Long)

    // Очистка всех данных пользователя при logout
    @Query("DELETE FROM users")
    suspend fun clearAll()
}
```

### 1.3. Добавить UserDao в существующую Room Database

**Файл:** `app/src/main/java/com/swparks/data/database/SWDatabase.kt`

**Добавить:**
```kotlin
@Database(
    entities = [UserEntity::class, /* другие entities */],
    version = 1,  // Первая версия БД (не увеличивать до релиза)
    exportSchema = false
)
abstract class SWDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    // ... другие DAO
}
```

### 1.4. Создать конвертеры UserEntity ↔ User

**Файл:** `app/src/main/java/com/swparks/data/database/UserEntityMapper.kt`

```kotlin
fun User.toEntity(
    isCurrentUser: Boolean = false,
    isFriend: Boolean = false,
    isFriendRequest: Boolean = false,
    isBlacklisted: Boolean = false
): UserEntity = UserEntity(
    id = id,
    name = name,
    image = image,
    cityId = cityID,
    countryId = countryID,
    birthDate = birthDate,
    email = email,
    fullName = fullName,
    genderCode = genderCode,
    friendRequestCount = friendRequestCount,
    friendsCount = friendsCount,
    parksCount = parksCount,
    journalCount = journalCount,
    lang = lang,
    isCurrentUser = isCurrentUser,
    isFriend = isFriend,
    isFriendRequest = isFriendRequest,
    isBlacklisted = isBlacklisted
)

fun UserEntity.toDomain(): User = User(
    id = id,
    name = name,
    image = image,
    cityID = cityId,
    countryID = countryId,
    birthDate = birthDate,
    email = email,
    fullName = fullName,
    genderCode = genderCode,
    friendRequestCount = friendRequestCount,
    friendsCount = friendsCount,
    parksCount = parksCount,
    journalCount = journalCount,
    lang = lang
)
```

---

## Этап 2: Расширение SWRepository с кэшированием

### 2.1. Обновить интерфейс SWRepository

**Добавить Flow методы для локального доступа:**

```kotlin
interface SWRepository {
    // ... существующие методы с сервера ...

    // Flow методы для локального кэша
    fun getCurrentUserFlow(): Flow<User?>
    fun getFriendsFlow(): Flow<List<User>>
    fun getFriendRequestsFlow(): Flow<List<User>>
    fun getBlacklistFlow(): Flow<List<User>>
    fun getFriendsCountFlow(): Flow<Int>

    // Методы очистки данных пользователя
    suspend fun clearUserData()
}
```

### 2.2. Обновить реализацию SWRepositoryImpl

**Добавить кэширование в существующие методы:**

```kotlin
class SWRepositoryImpl(
    private val userDao: UserDao,
    private val dataStore: DataStore<Preferences>,
    private val swApi: SWApi,
) : SWRepository {

    override suspend fun getUser(userId: Long): Result<User> = try {
        // 1. Загружаем с сервера
        val remoteUser = swApi.getUserProfile(userId)

        // 2. Сохраняем в кэш
        userDao.insert(remoteUser.toEntity(isCurrentUser = (userId == getCurrentUserId())))

        Result.success(remoteUser.toDomain())
    } catch (e: IOException) {
        // 3. Ошибка сети - берем из кэша
        val cachedUser = userDao.getUserByIdFlow(userId).first()
        if (cachedUser != null) {
            Log.i("SWRepository", "Профиль загружен из кэша")
            Result.success(cachedUser.toDomain())
        } else {
            Result.failure(NetworkException("Не удалось загрузить профиль. Проверьте интернет", e))
        }
    }

    override suspend fun getSocialUpdates(userId: Long): Result<SocialUpdates> = try {
        // Загружаем с сервера
        val remoteUpdates = swApi.getSocialUpdates(userId)

        // Сохраняем в кэш
        userDao.insert(remoteUpdates.user.toEntity(isCurrentUser = true))
        userDao.insertAll(
            remoteUpdates.friends.map { it.toEntity(isFriend = true) }
        )
        userDao.insertAll(
            remoteUpdates.friendRequests.map { it.toEntity(isFriendRequest = true) }
        )
        userDao.insertAll(
            remoteUpdates.blacklist.map { it.toEntity(isBlacklisted = true) }
        )

        Result.success(remoteUpdates.toDomain())
    } catch (e: IOException) {
        // Ошибка сети - возвращаем кэшированные данные
        Result.success(SocialUpdates(
            user = userDao.getUserByIdFlow(userId).first()?.toDomain(),
            friends = userDao.getFriendsFlow().first().map { it.toDomain() },
            friendRequests = userDao.getFriendRequestsFlow().first().map { it.toDomain() },
            blacklist = userDao.getBlacklistFlow().first().map { it.toDomain() }
        ))
    }

    override fun getCurrentUserFlow(): Flow<User?> = userDao.getCurrentUserFlow()
        .map { it?.toDomain() }

    override fun getFriendsFlow(): Flow<List<User>> = userDao.getFriendsFlow()
        .map { users -> users.map { it.toDomain() } }

    override fun getFriendRequestsFlow(): Flow<List<User>> = userDao.getFriendRequestsFlow()
        .map { users -> users.map { it.toDomain() } }

    override fun getBlacklistFlow(): Flow<List<User>> = userDao.getBlacklistFlow()
        .map { users -> users.map { it.toDomain() } }

    override suspend fun clearUserData() {
        // Удаляем все данные пользователя (профиль, друзья, заявки, черный список)
        userDao.clearAll()
        // TODO: Добавить очистку других таблиц при их реализации (дневники, сообщения)
        Log.i("SWRepository", "Все данные пользователя удалены")
    }
}
```

---

## Этап 3: Dependency Injection

### 3.1. Добавить userDao в AppContainer

**Файл:** `app/src/main/java/com/swparks/data/AppContainer.kt`

```kotlin
class DefaultAppContainer(
    private val context: Context,
) : AppContainer {
    val database: SWDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            SWDatabase::class.java,
            "sw_database"
        ).fallbackToDestructiveMigration()  // Для разработки: пересоздает БД при изменении схемы
         .build()
    }

    val userDao: UserDao by lazy { database.userDao() }

    val swRepository: SWRepository by lazy {
        SWRepositoryImpl(
            userDao = userDao,
            dataStore = createAppSettingsDataStore(context),
            swApi = createSWApi(context)
        )
    }
}
```

---

## Этап 4: Использование в ViewModels

### 4.1. Обновить ProfileViewModel

**Файл:** `app/src/main/java/com/swparks/viewmodel/ProfileViewModel.kt`

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

    fun loadProfile(user: User?) {
        if (user == null) {
            _uiState.update { ProfileUiState.Error("Пользователь не авторизован") }
            return
        }

        viewModelScope.launch {
            try {
                val country = user.countryID?.let { countryId ->
                    countriesRepository.getCountryById(countryId.toString())
                }

                val city = user.cityID?.let { cityId ->
                    countriesRepository.getCityById(cityId.toString())
                }

                _uiState.update { ProfileUiState.Success(user, country, city) }
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

### Этап 1: Room
- [ ] Создан `UserEntity` с флагами категоризации
- [ ] Создан `UserDao` с Flow методами
- [ ] `UserDao` добавлен в `SWDatabase`
- [ ] Созданы конвертеры `UserEntity` ↔ `User`

### Этап 2: SWRepository
- [ ] Добавлены Flow методы в интерфейс `SWRepository`
- [ ] Реализовано кэширование в `getUser()`
- [ ] Реализовано кэширование в `getSocialUpdates()`
- [ ] Реализован `clearUserCache()`

### Этап 3: DI
- [ ] `userDao` добавлен в `AppContainer`
- [ ] `userDao` передан в `SWRepositoryImpl`

### Этап 4: ViewModels
- [ ] `ProfileViewModel` обновлен для использования Flow
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
1. Создать `UserEntity` и `UserDao`
2. Добавить `userDao` в `SWDatabase`
3. Создать конвертеры
4. Обновить `SWRepository` с кэшированием
5. Обновить `ProfileViewModel`
6. Базовые тесты

### Вторая итерация
1. Обновить другие ViewModels (Friends, Messages и т.д.)
2. Реализовать очистку при logout
3. Расширить тестовое покрытие
