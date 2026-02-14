package com.swparks.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.swparks.data.APIError
import com.swparks.data.ErrorResponse
import com.swparks.data.NetworkUtils
import com.swparks.data.UserPreferencesRepository
import com.swparks.data.database.dao.DialogDao
import com.swparks.data.database.dao.JournalDao
import com.swparks.data.database.dao.UserDao
import com.swparks.data.database.entity.toDomain
import com.swparks.data.database.entity.toEntity
import com.swparks.data.model.ApiBlacklistOption
import com.swparks.data.model.ApiFriendAction
import com.swparks.data.model.DialogResponse
import com.swparks.data.model.Event
import com.swparks.data.model.JournalEntryResponse
import com.swparks.data.model.JournalResponse
import com.swparks.data.model.LoginSuccess
import com.swparks.data.model.MessageResponse
import com.swparks.data.model.Park
import com.swparks.data.model.SocialUpdates
import com.swparks.data.model.User
import com.swparks.domain.exception.NetworkException
import com.swparks.domain.exception.ServerException
import com.swparks.network.SWApi
import com.swparks.ui.model.EditJournalSettingsRequest
import com.swparks.ui.model.EventForm
import com.swparks.ui.model.EventType
import com.swparks.ui.model.JournalAccess
import com.swparks.ui.model.MainUserForm
import com.swparks.ui.model.ParkForm
import com.swparks.ui.model.RegistrationRequest
import com.swparks.ui.model.TextEntryOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/**
 * Интерфейс репозитория для работы с сервером workout.su
 *
 * Примечание: Интерфейс содержит много функций по назначению,
 * так как это основной API для работы со всеми функциями сервера.
 * Подавляет предупреждение [TooManyFunctions] от Detekt.
 */
@Suppress("TooManyFunctions")
interface SWRepository {
    // Существующие методы (для обратной совместимости)
    suspend fun getPastEvents(): List<Event>
    val isAuthorized: Flow<Boolean>
    suspend fun savePreference(isAuthorized: Boolean)

    // Flow методы для локального кэша
    fun getCurrentUserFlow(): Flow<User?>
    fun getFriendsFlow(): Flow<List<User>>
    fun getFriendRequestsFlow(): Flow<List<User>>
    fun getBlacklistFlow(): Flow<List<User>>
    fun getFriendsCountFlow(): Flow<Int>

    // Методы очистки данных пользователя
    suspend fun clearUserData()

    // 3.1. Авторизация
    suspend fun register(request: RegistrationRequest): Result<LoginSuccess>
    suspend fun login(token: String?): Result<LoginSuccess>
    suspend fun resetPassword(login: String): Result<Unit>
    suspend fun changePassword(current: String, new: String): Result<Unit>

    // Принудительный логаут (при ошибке 401)
    suspend fun forceLogout()

    // 3.2. Профиль
    suspend fun getUser(userId: Long): Result<User>
    suspend fun editUser(userId: Long, form: MainUserForm, image: ByteArray?): Result<User>
    suspend fun deleteUser(): Result<Unit>
    suspend fun getSocialUpdates(userId: Long): Result<SocialUpdates>
    suspend fun findUsers(name: String): Result<List<User>>

    // 3.3. Друзья
    suspend fun getFriendsForUser(userId: Long): Result<List<User>>
    suspend fun getFriendRequests(): Result<List<User>>
    suspend fun respondToFriendRequest(userId: Long, accept: Boolean): Result<Unit>
    suspend fun friendAction(userId: Long, action: ApiFriendAction): Result<Unit>
    suspend fun blacklistAction(user: User, option: ApiBlacklistOption): Result<Unit>
    suspend fun getBlacklist(): Result<List<User>>

    // 3.4. Площадки
    suspend fun getAllParks(): Result<List<Park>>
    suspend fun getPark(id: Long): Result<Park>
    suspend fun savePark(id: Long?, form: ParkForm, photos: List<ByteArray>?): Result<Park>
    suspend fun deletePark(parkId: Long): Result<Unit>
    suspend fun getParksForUser(userId: Long): Result<List<Park>>
    suspend fun changeTrainHereStatus(trainHere: Boolean, parkId: Long): Result<Unit>
    suspend fun getUpdatedParks(date: String): Result<List<Park>>

    // 3.5. Мероприятия
    suspend fun getEvents(type: EventType): Result<List<Event>>
    suspend fun getEvent(id: Long): Result<Event>
    suspend fun saveEvent(id: Long?, form: EventForm, photos: List<ByteArray>?): Result<Event>
    suspend fun changeIsGoingToEvent(go: Boolean, eventId: Long): Result<Unit>
    suspend fun deleteEvent(eventId: Long): Result<Unit>

    // 3.6. Сообщения
    suspend fun getDialogs(): Result<List<DialogResponse>>
    suspend fun getMessages(dialogId: Long): Result<List<MessageResponse>>
    suspend fun sendMessage(message: String, userId: Long): Result<Unit>
    suspend fun markAsRead(userId: Long): Result<Unit>
    suspend fun deleteDialog(dialogId: Long): Result<Unit>

    // 3.7. Дневники
    suspend fun getJournals(userId: Long): Result<List<JournalResponse>>
    suspend fun getJournal(userId: Long, journalId: Long): Result<JournalResponse>
    suspend fun getJournalEntries(userId: Long, journalId: Long): Result<List<JournalEntryResponse>>

    suspend fun editJournalSettings(
        journalId: Long,
        title: String,
        userId: Long?,
        viewAccess: JournalAccess,
        commentAccess: JournalAccess
    ): Result<Unit>

    /**
     * Получить поток дневника по ID для подписки на изменения.
     * Возвращает Flow с дневником из локального кэша.
     *
     * @param journalId Идентификатор дневника
     * @return Flow с дневником или null если не найден в кэше
     */
    fun observeJournalById(journalId: Long): Flow<com.swparks.domain.model.Journal?>

    /**
     * Сохранить дневник в локальный кэш.
     * Используется после успешного обновления настроек для синхронизации кэша.
     *
     * @param journal Дневник для сохранения
     */
    suspend fun saveJournalToCache(journal: com.swparks.domain.model.Journal)

    suspend fun createJournal(title: String, userId: Long?): Result<Unit>

    suspend fun deleteJournal(journalId: Long, userId: Long?): Result<Unit>

    // 3.8. Комментарии
    suspend fun addComment(option: TextEntryOption, comment: String): Result<Unit>
    suspend fun editComment(
        option: TextEntryOption,
        commentId: Long,
        newComment: String
    ): Result<Unit>

    suspend fun deleteComment(option: TextEntryOption, commentId: Long): Result<Unit>
}

/**
 * Реализация репозитория для работы с сервером workout.su
 *
 * Примечание: Класс содержит много функций по назначению,
 * так как это основная реализация для работы со всеми функциями сервера.
 * Подавляет предупреждение [TooManyFunctions] и [TooGenericExceptionCaught] от Detekt.
 *
 * Примечание по обработке исключений:
 * Все сетевые операции ловят общий Exception и оборачивают в Result.failure().
 * Это преднамеренный выбор для упрощения обработки ошибок в одном месте.
 */
@Suppress(
    "TooManyFunctions",
    "TooGenericExceptionCaught",
    "LargeClass",
    "MagicNumber",
    "CyclomaticComplexMethod"
)
class SWRepositoryImp(
    private val swApi: SWApi,
    private val dataStore: DataStore<Preferences>,
    private val userDao: UserDao,
    private val journalDao: JournalDao,
    private val journalEntryDao: com.swparks.data.database.dao.JournalEntryDao,
    private val dialogDao: DialogDao
) : SWRepository {
    private companion object {
        const val TAG = "SWRepositoryImp"
    }

    private val preferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(dataStore)
    }

    /**
     * JSON-десериализатор для ErrorResponse
     */
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Обрабатывает IOException и возвращает NetworkException с сообщением для пользователя
     */
    private fun handleIOException(e: IOException, operation: String): NetworkException {
        Log.e(TAG, "Ошибка сети при $operation: ${e.message}")
        return NetworkException(
            message = "Не удалось выполнить операцию. Проверьте интернет-соединение",
            cause = e
        )
    }

    /**
     * Обрабатывает HttpException и пытается извлечь текст ошибки из тела ответа
     */
    private fun handleHttpException(e: HttpException, operation: String): Exception {
        val statusCode = e.code()
        Log.e(TAG, "Ошибка сервера $statusCode при $operation")

        return try {
            val responseBody = e.response()?.errorBody()?.string()
            if (responseBody != null) {
                Log.e(TAG, "Тело ответа сервера: $responseBody")
                val errorResponse = json.decodeFromString<ErrorResponse>(responseBody)
                Log.e(
                    TAG,
                    "Десериализованный ErrorResponse: message=${errorResponse.message}, errors=${errorResponse.errors}"
                )
                val errorMessage = errorResponse.realMessage ?: "Ошибка сервера: $statusCode"
                Log.e(TAG, "realMessage: ${errorResponse.realMessage}")
                Log.e(TAG, "errorMessage для ServerException: $errorMessage")
                ServerException(message = errorMessage, cause = e)
            } else {
                val errorMessage = APIError.fromStatusCode(statusCode).errorMessage
                ServerException(message = errorMessage, cause = e)
            }
        } catch (se: SerializationException) {
            // Если не удалось десериализовать ответ сервера
            Log.e(TAG, "Не удалось десериализовать ответ об ошибке: ${se.message}")
            ServerException(message = "Ошибка обработки ответа сервера", cause = se)
        }
    }

    // Существующие методы (для обратной совместимости)
    override suspend fun getPastEvents(): List<Event> = swApi.getPastEvents()

    override val isAuthorized: Flow<Boolean>
        get() = preferencesRepository.isAuthorized

    override suspend fun savePreference(isAuthorized: Boolean) {
        preferencesRepository.savePreference(isAuthorized)
    }

    // 3.1. Авторизация

    override suspend fun register(
        request: RegistrationRequest
    ): Result<LoginSuccess> =
        try {
            val response = swApi.register(request)
            // Сохраняем токен авторизации при успешной регистрации
            savePreference(true)
            Result.success(response)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "регистрации"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "регистрации"))
        }

    override suspend fun login(token: String?): Result<LoginSuccess> =
        try {
            val response = swApi.login()
            // Сохраняем флаг авторизации при успешном входе
            // Токен уже сохранен в SecureTokenRepository, здесь сохраняем только статус
            savePreference(true)
            Result.success(response)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "авторизации"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "авторизации"))
        }

    override suspend fun resetPassword(login: String): Result<Unit> =
        try {
            swApi.resetPassword(login)
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "восстановлении пароля"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "восстановлении пароля"))
        }

    override suspend fun changePassword(current: String, new: String): Result<Unit> =
        try {
            swApi.changePassword(current, new)
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "смене пароля"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "смене пароля"))
        }

    // Принудительный логаут (при ошибке 401)
    override suspend fun forceLogout() {
        savePreference(false)
        Log.i(TAG, "Принудительный логаут выполнен")
    }

    // 3.2. Профиль

    override suspend fun getUser(userId: Long): Result<User> =
        try {
            // 1. Загружаем с сервера
            val remoteUser = swApi.getUser(userId)

            // 2. Сохраняем в кэш
            val currentUserId = preferencesRepository.getCurrentUserIdSync()
            userDao.insert(remoteUser.toEntity(isCurrentUser = (userId == currentUserId)))

            Result.success(remoteUser)
        } catch (e: IOException) {
            // 3. Ошибка сети - берем из кэша
            val cachedUser = userDao.getUserByIdFlow(userId).first()
            if (cachedUser != null) {
                Log.i(TAG, "Профиль загружен из кэша")
                Result.success(cachedUser.toDomain())
            } else {
                Result.failure(handleIOException(e, "загрузке пользователя"))
            }
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "загрузке пользователя"))
        }

    override suspend fun editUser(
        userId: Long,
        form: MainUserForm,
        image: ByteArray?
    ): Result<User> =
        try {
            val user = swApi.editUser(
                userId = userId,
                name = NetworkUtils.createPartWithName("name", form.name),
                fullName = NetworkUtils.createPartWithName("fullname", form.fullname),
                email = NetworkUtils.createPartWithName("email", form.email),
                birthDate = NetworkUtils.createOptionalPartWithName("birth_date", form.birthDate),
                gender = NetworkUtils.createOptionalPartWithName(
                    "gender",
                    form.genderCode.toString()
                ),
                countryId = NetworkUtils.createOptionalPartWithName(
                    "country_id",
                    form.countryId?.toString()
                ),
                cityId = NetworkUtils.createOptionalPartWithName(
                    "city_id",
                    form.cityId?.toString()
                ),
                image = NetworkUtils.createOptionalImagePart(image, "image")
            )
            Result.success(user)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "редактировании пользователя"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "редактировании пользователя"))
        }

    override suspend fun deleteUser(): Result<Unit> =
        try {
            swApi.deleteUser()
            // Очищаем состояние авторизации при удалении аккаунта
            savePreference(false)
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "удалении пользователя"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "удалении пользователя"))
        }

    override suspend fun getSocialUpdates(userId: Long): Result<SocialUpdates> =
        kotlinx.coroutines.supervisorScope {
            try {
                // Параллельные запросы к серверу
                val userDeferred = async { swApi.getUser(userId) }
                val friendsDeferred = async { swApi.getFriendsForUser(userId) }
                val requestsDeferred = async { swApi.getFriendRequests() }
                val blacklistDeferred = async { swApi.getBlacklist() }

                // Ожидание всех результатов
                val user = userDeferred.await()
                val friends = friendsDeferred.await()
                val requests = requestsDeferred.await()
                val blacklist = blacklistDeferred.await()

                // Сохраняем в кэш
                userDao.insert(user.toEntity(isCurrentUser = true))
                userDao.insertAll(friends.map { it.toEntity(isFriend = true) })
                userDao.insertAll(requests.map { it.toEntity(isFriendRequest = true) })
                userDao.insertAll(blacklist.map { it.toEntity(isBlacklisted = true) })

                val socialUpdates = SocialUpdates(
                    user = user,
                    friends = friends,
                    friendRequests = requests,
                    blacklist = blacklist
                )
                Result.success(socialUpdates)
            } catch (e: IOException) {
                // Ошибка сети - возвращаем кэшированные данные
                val cachedUser = userDao.getCurrentUserFlow().first()?.toDomain()
                val cachedFriends = userDao.getFriendsFlow().first().map { it.toDomain() }
                val cachedRequests = userDao.getFriendRequestsFlow().first().map { it.toDomain() }
                val cachedBlacklist = userDao.getBlacklistFlow().first().map { it.toDomain() }

                if (cachedUser != null) {
                    Log.i(TAG, "Социальные обновления загружены из кэша")
                    Result.success(
                        SocialUpdates(
                            user = cachedUser,
                            friends = cachedFriends,
                            friendRequests = cachedRequests,
                            blacklist = cachedBlacklist
                        )
                    )
                } else {
                    Result.failure(handleIOException(e, "загрузке социальных обновлений"))
                }
            } catch (e: HttpException) {
                Result.failure(handleHttpException(e, "загрузке социальных обновлений"))
            }
        }

    override suspend fun findUsers(name: String): Result<List<User>> =
        try {
            val users = swApi.findUsers(name)
            Result.success(users)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "поиске пользователей"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "поиске пользователей"))
        }

    // 3.3. Друзья

    override suspend fun getFriendsForUser(userId: Long): Result<List<User>> =
        try {
            val friends = swApi.getFriendsForUser(userId)
            Result.success(friends)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "загрузке друзей"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "загрузке друзей"))
        }

    override suspend fun getFriendRequests(): Result<List<User>> =
        try {
            val requests = swApi.getFriendRequests()
            Result.success(requests)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "загрузке заявок в друзья"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "загрузке заявок в друзья"))
        }

    override suspend fun respondToFriendRequest(userId: Long, accept: Boolean): Result<Unit> =
        try {
            if (accept) {
                swApi.acceptFriendRequest(userId)
                // При принятии: помечаем как друга и увеличиваем счетчик
                userDao.markAsFriend(userId)
                userDao.incrementFriendsCount()
            } else {
                swApi.declineFriendRequest(userId)
            }
            // В обоих случаях: снимаем флаг заявки и уменьшаем счетчик заявок
            userDao.removeFriendRequest(userId)
            userDao.decrementFriendRequestCount()
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "обработке заявки в друзья"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "обработке заявки в друзья"))
        }

    override suspend fun friendAction(userId: Long, action: ApiFriendAction): Result<Unit> =
        try {
            when (action) {
                ApiFriendAction.ADD -> swApi.sendFriendRequest(userId)
                ApiFriendAction.REMOVE -> swApi.deleteFriend(userId)
            }
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "действии с другом"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "действии с другом"))
        }

    override suspend fun blacklistAction(user: User, option: ApiBlacklistOption): Result<Unit> =
        try {
            when (option) {
                ApiBlacklistOption.ADD -> swApi.addToBlacklist(user.id)
                ApiBlacklistOption.REMOVE -> swApi.deleteFromBlacklist(user.id)
            }
            // Обновляем локальную базу данных после успешного ответа от сервера
            when (option) {
                ApiBlacklistOption.ADD -> userDao.addToBlacklist(user.id)
                ApiBlacklistOption.REMOVE -> userDao.removeFromBlacklist(user.id)
            }
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "действии с черным списком"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "действии с черным списком"))
        }

    override suspend fun getBlacklist(): Result<List<User>> =
        try {
            val blacklist = swApi.getBlacklist()
            Result.success(blacklist)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "загрузке черного списка"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "загрузке черного списка"))
        }

    // 3.4. Площадки

    override suspend fun getAllParks(): Result<List<Park>> =
        try {
            val parks = swApi.getAllParks()
            Result.success(parks)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "загрузке площадок"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "загрузке площадок"))
        }

    override suspend fun getPark(id: Long): Result<Park> =
        try {
            val park = swApi.getPark(id)
            Result.success(park)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "загрузке площадки"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "загрузке площадки"))
        }

    override suspend fun savePark(
        id: Long?,
        form: ParkForm,
        photos: List<ByteArray>?
    ): Result<Park> =
        try {
            val photoParts = photos?.mapIndexed { index, bytes ->
                NetworkUtils.createImagePart(bytes, "photo_$index")
            }

            val park = if (id != null) {
                swApi.editPark(
                    parkId = id,
                    address = NetworkUtils.createPartWithName("address", form.address),
                    latitude = NetworkUtils.createPartWithName("latitude", form.latitude),
                    longitude = NetworkUtils.createPartWithName("longitude", form.longitude),
                    cityId = NetworkUtils.createOptionalPartWithName(
                        "city_id",
                        form.cityId?.toString()
                    ),
                    typeId = NetworkUtils.createPartWithName("type_id", form.typeId.toString()),
                    sizeId = NetworkUtils.createPartWithName("class_id", form.sizeId.toString()),
                    photos = photoParts
                )
            } else {
                swApi.createPark(
                    address = NetworkUtils.createPartWithName("address", form.address),
                    latitude = NetworkUtils.createPartWithName("latitude", form.latitude),
                    longitude = NetworkUtils.createPartWithName("longitude", form.longitude),
                    cityId = NetworkUtils.createOptionalPartWithName(
                        "city_id",
                        form.cityId?.toString()
                    ),
                    typeId = NetworkUtils.createPartWithName("type_id", form.typeId.toString()),
                    sizeId = NetworkUtils.createPartWithName("class_id", form.sizeId.toString()),
                    photos = photoParts
                )
            }
            Result.success(park)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "сохранении площадки"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "сохранении площадки"))
        }

    override suspend fun deletePark(parkId: Long): Result<Unit> =
        try {
            swApi.deletePark(parkId)
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "удалении площадки"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "удалении площадки"))
        }

    override suspend fun getParksForUser(userId: Long): Result<List<Park>> =
        try {
            val parks = swApi.getParksForUser(userId)
            Result.success(parks)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "загрузке площадок пользователя"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "загрузке площадок пользователя"))
        }

    override suspend fun changeTrainHereStatus(trainHere: Boolean, parkId: Long): Result<Unit> =
        try {
            if (trainHere) {
                swApi.postTrainHere(parkId)
            } else {
                swApi.deleteTrainHere(parkId)
            }
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "изменении статуса тренировки"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "изменении статуса тренировки"))
        }

    override suspend fun getUpdatedParks(date: String): Result<List<Park>> =
        try {
            val parks = swApi.getUpdatedParks(date)
            Result.success(parks)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "загрузке обновленных площадок"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "загрузке обновленных площадок"))
        }

    // 3.5. Мероприятия

    override suspend fun getEvents(type: EventType): Result<List<Event>> =
        try {
            val events = when (type) {
                EventType.FUTURE -> swApi.getFutureEvents()
                EventType.PAST -> swApi.getPastEvents()
            }
            Result.success(events)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "загрузке мероприятий"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "загрузке мероприятий"))
        }

    override suspend fun getEvent(id: Long): Result<Event> =
        try {
            val event = swApi.getEvent(id)
            Result.success(event)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "загрузке мероприятия"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "загрузке мероприятия"))
        }

    override suspend fun saveEvent(
        id: Long?,
        form: EventForm,
        photos: List<ByteArray>?
    ): Result<Event> =
        try {
            val photoParts = photos?.mapIndexed { index, bytes ->
                NetworkUtils.createImagePart(bytes, "photo_$index")
            }

            val event = if (id != null) {
                swApi.editEvent(
                    eventId = id,
                    title = NetworkUtils.createPartWithName("title", form.title),
                    description = NetworkUtils.createPartWithName("description", form.description),
                    date = NetworkUtils.createPartWithName("date", form.date),
                    parkId = NetworkUtils.createPartWithName("area_id", form.parkId.toString()),
                    photos = photoParts
                )
            } else {
                swApi.createEvent(
                    title = NetworkUtils.createPartWithName("title", form.title),
                    description = NetworkUtils.createPartWithName("description", form.description),
                    date = NetworkUtils.createPartWithName("date", form.date),
                    parkId = NetworkUtils.createPartWithName("area_id", form.parkId.toString()),
                    photos = photoParts
                )
            }
            Result.success(event)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "сохранении мероприятия"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "сохранении мероприятия"))
        }

    override suspend fun changeIsGoingToEvent(go: Boolean, eventId: Long): Result<Unit> =
        try {
            if (go) {
                swApi.postGoToEvent(eventId)
            } else {
                swApi.deleteGoToEvent(eventId)
            }
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "изменении участия в мероприятии"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "изменении участия в мероприятии"))
        }

    override suspend fun deleteEvent(eventId: Long): Result<Unit> =
        try {
            swApi.deleteEvent(eventId)
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "удалении мероприятия"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "удалении мероприятия"))
        }

    // 3.6. Сообщения

    override suspend fun getDialogs(): Result<List<DialogResponse>> =
        try {
            val dialogs = swApi.getDialogs()
            Result.success(dialogs)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "загрузке диалогов"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "загрузке диалогов"))
        }

    override suspend fun getMessages(dialogId: Long): Result<List<MessageResponse>> =
        try {
            val messages = swApi.getMessages(dialogId)
            Result.success(messages)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "загрузке сообщений"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "загрузке сообщений"))
        }

    override suspend fun sendMessage(message: String, userId: Long): Result<Unit> =
        try {
            swApi.sendMessageTo(userId, message)
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "отправке сообщения"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "отправке сообщения"))
        }

    override suspend fun markAsRead(userId: Long): Result<Unit> =
        try {
            swApi.markAsRead(userId)
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "отметке сообщений прочитанными"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "отметке сообщений прочитанными"))
        }

    override suspend fun deleteDialog(dialogId: Long): Result<Unit> =
        try {
            swApi.deleteDialog(dialogId)
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "удалении диалога"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "удалении диалога"))
        }

    // 3.7. Дневники

    override suspend fun getJournals(userId: Long): Result<List<JournalResponse>> =
        try {
            val journals = swApi.getJournals(userId)
            Result.success(journals)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "загрузке дневников"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "загрузке дневников"))
        }

    override suspend fun getJournal(userId: Long, journalId: Long): Result<JournalResponse> =
        try {
            val journal = swApi.getJournal(userId, journalId)
            Result.success(journal)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "загрузке дневника"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "загрузке дневника"))
        }

    override suspend fun editJournalSettings(
        journalId: Long,
        title: String,
        userId: Long?,
        viewAccess: JournalAccess,
        commentAccess: JournalAccess
    ): Result<Unit> =
        try {
            val request = EditJournalSettingsRequest.create(
                title = title,
                viewAccess = viewAccess,
                commentAccess = commentAccess
            )

            Log.i(
                TAG,
                "Запрос редактирования настроек дневника: journalId=$journalId, userId=$userId, title=$title, viewAccess=${request.viewAccess}, commentAccess=${request.commentAccess}"
            )

            val response = swApi.editJournalSettings(
                userId = userId ?: 1L,
                journalId = journalId,
                title = title,
                viewAccess = request.viewAccess,
                commentAccess = request.commentAccess
            )

            if (response.isSuccessful) {
                Log.i(TAG, "Настройки дневника успешно обновлены на сервере: journalId=$journalId")
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(
                    TAG,
                    "Ошибка сервера при редактировании настроек дневника: code=${response.code()}, body=$errorBody"
                )
                Result.failure(
                    ServerException(
                        message = errorBody ?: "Ошибка сервера: ${response.code()}",
                        cause = Exception("HTTP ${response.code()}")
                    )
                )
            }
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "редактировании настроек дневника"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "редактировании настроек дневника"))
        }

    override suspend fun getJournalEntries(
        userId: Long,
        journalId: Long
    ): Result<List<JournalEntryResponse>> =
        try {
            val entries = swApi.getJournalEntries(userId, journalId)
            Result.success(entries)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "загрузке записей дневника"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "загрузке записей дневника"))
        }

    override suspend fun createJournal(title: String, userId: Long?): Result<Unit> =
        try {
            val finalUserId = userId ?: 1L
            Log.i(TAG, "Создание дневника: userId=$finalUserId, title=$title")
            val response = swApi.createJournal(
                userId = finalUserId,
                title = title
            )
            Log.i(
                TAG,
                "Ответ сервера при создании дневника: код=${response.code()}, успешно=${response.isSuccessful}"
            )
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "создании дневника"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "создании дневника"))
        }

    override suspend fun deleteJournal(journalId: Long, userId: Long?): Result<Unit> =
        try {
            val response = swApi.deleteJournal(
                userId = userId ?: 1L, // Note: передавать реальный userId
                journalId = journalId
            )

            when {
                response.isSuccessful -> {
                    Log.i(TAG, "Дневник успешно удален на сервере")
                    journalDao.deleteById(journalId)
                    Result.success(Unit)
                }

                response.code() == 404 -> {
                    // Дневник уже удален на сервере — синхронизируем локальный кэш
                    Log.i(TAG, "Дневник уже удален на сервере (404), удаляем из локального кэша")
                    journalDao.deleteById(journalId)
                    Result.success(Unit)
                }

                else -> {
                    val statusCode = response.code()
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Ошибка при удалении дневника: код=$statusCode, тело=$errorBody")
                    Result.failure(
                        handleHttpException(
                            HttpException(response),
                            "удалении дневника"
                        )
                    )
                }
            }
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "удалении дневника"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "удалении дневника"))
        }

    // 3.8. Комментарии

    override suspend fun addComment(option: TextEntryOption, comment: String): Result<Unit> =
        when (option) {
            is TextEntryOption.Park -> try {
                swApi.addCommentToPark(option.id, comment)
                Result.success(Unit)
            } catch (e: IOException) {
                Result.failure(handleIOException(e, "добавлении комментария"))
            } catch (e: HttpException) {
                Result.failure(handleHttpException(e, "добавлении комментария"))
            }

            is TextEntryOption.Event -> try {
                swApi.addCommentToEvent(option.id, comment)
                Result.success(Unit)
            } catch (e: IOException) {
                Result.failure(handleIOException(e, "добавлении комментария"))
            } catch (e: HttpException) {
                Result.failure(handleHttpException(e, "добавлении комментария"))
            }

            is TextEntryOption.Journal -> {
// Для дневников создаются новые записи (сообщения)
                try {
                    swApi.saveJournalEntry(
                        option.ownerId,
                        option.journalId,
                        comment
                    )
                    Result.success(Unit)
                } catch (e: IOException) {
                    Result.failure(handleIOException(e, "создании записи в дневнике"))
                } catch (e: HttpException) {
                    Result.failure(handleHttpException(e, "создании записи в дневнике"))
                }

            }
        }

    override suspend fun editComment(
        option: TextEntryOption,
        commentId: Long,
        newComment: String
    ): Result<Unit> = when (option) {
        is TextEntryOption.Park -> try {
            swApi.editParkComment(option.id, commentId, newComment)
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "редактировании комментария"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "редактировании комментария"))
        }

        is TextEntryOption.Event -> try {
            swApi.editEventComment(option.id, commentId, newComment)
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "редактировании комментария"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, "редактировании комментария"))
        }

        is TextEntryOption.Journal -> {
// Для дневников редактируются сами записи (сообщения)
            try {
                swApi.editJournalEntry(
                    option.ownerId,
                    option.journalId,
                    commentId,
                    newComment
                )

                // Обновляем локальный кэш после успешного редактирования
                val existingEntry = journalEntryDao.getById(commentId)
                if (existingEntry != null) {
                    val updatedEntry = existingEntry.copy(
                        message = newComment,
                        modifyDate = System.currentTimeMillis()
                    )
                    journalEntryDao.insert(updatedEntry)
                }

                Result.success(Unit)
            } catch (e: IOException) {
                Result.failure(handleIOException(e, "редактировании записи в дневнике"))
            } catch (e: HttpException) {
                Result.failure(handleHttpException(e, "редактировании записи в дневнике"))
            }

        }
    }

    override suspend fun deleteComment(option: TextEntryOption, commentId: Long): Result<Unit> =
        when (option) {
            is TextEntryOption.Park -> try {
                swApi.deleteParkComment(option.id, commentId)
                Result.success(Unit)
            } catch (e: IOException) {
                Result.failure(handleIOException(e, "удалении комментария"))
            } catch (e: HttpException) {
                Result.failure(handleHttpException(e, "удалении комментария"))
            }

            is TextEntryOption.Event -> try {
                swApi.deleteEventComment(option.id, commentId)
                Result.success(Unit)
            } catch (e: IOException) {
                Result.failure(handleIOException(e, "удалении комментария"))
            } catch (e: HttpException) {
                Result.failure(handleHttpException(e, "удалении комментария"))
            }

            is TextEntryOption.Journal -> {
// Для дневников удаляются сами записи (сообщения)
                try {
                    swApi.deleteJournalEntry(
                        option.ownerId,
                        option.journalId,
                        commentId
                    )
                    Result.success(Unit)
                } catch (e: IOException) {
                    Result.failure(handleIOException(e, "удалении записи из дневника"))
                } catch (e: HttpException) {
                    Result.failure(handleHttpException(e, "удалении записи из дневника"))
                }

            }
        }

    // ==================== Flow методы для локального кэша ====================

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getCurrentUserFlow(): Flow<User?> =
        preferencesRepository.currentUserId
            .flatMapLatest { userId ->
                if (userId != null) {
                    Log.d(TAG, "Текущий пользователь изменился: $userId")
                    userDao.getUserByIdFlow(userId).map { entity -> entity?.toDomain() }
                } else {
                    Log.d(TAG, "Текущий пользователь отсутствует")
                    flowOf(null)
                }
            }
            .flowOn(Dispatchers.IO)

    override fun getFriendsFlow(): Flow<List<User>> = userDao.getFriendsFlow()
        .map { users -> users.map { it.toDomain() } }

    override fun getFriendRequestsFlow(): Flow<List<User>> = userDao.getFriendRequestsFlow()
        .map { users -> users.map { it.toDomain() } }

    override fun getBlacklistFlow(): Flow<List<User>> = userDao.getBlacklistFlow()
        .map { users -> users.map { it.toDomain() } }

    override fun getFriendsCountFlow(): Flow<Int> = userDao.getFriendsCountFlow()

    override suspend fun clearUserData() {
        // Удаляем все данные пользователя (профиль, друзья, заявки, черный список)
        userDao.clearAll()
        // Удаляем все диалоги пользователя
        dialogDao.deleteAll()
        Log.i(TAG, "Все данные пользователя удалены")
        // Очищаем ID текущего пользователя
        preferencesRepository.clearCurrentUserId()
    }

    override fun observeJournalById(journalId: Long): Flow<com.swparks.domain.model.Journal?> {
        return journalDao.observeById(journalId)
            .map { entity -> entity?.toDomain() }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun saveJournalToCache(journal: com.swparks.domain.model.Journal) {
        try {
            journalDao.insert(journal.toEntity())
            Log.i(TAG, "Дневник сохранен в кэш: journalId=${journal.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка сохранения дневника в кэш: ${e.message}")
            throw e
        }
    }
}
