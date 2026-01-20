package com.swparks.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.swparks.data.ErrorResponse
import com.swparks.data.NetworkUtils
import com.swparks.data.APIError
import com.swparks.data.UserPreferencesRepository
import com.swparks.domain.exception.NetworkException
import com.swparks.domain.exception.ServerException
import com.swparks.model.ApiBlacklistOption
import com.swparks.model.ApiFriendAction
import com.swparks.model.ChangePasswordRequest
import com.swparks.model.DialogResponse
import com.swparks.model.EditJournalSettingsRequest
import com.swparks.model.Event
import com.swparks.model.EventForm
import com.swparks.model.EventType
import com.swparks.model.JournalAccess
import com.swparks.model.JournalEntryResponse
import com.swparks.model.JournalResponse
import com.swparks.model.LoginSuccess
import com.swparks.model.MainUserForm
import com.swparks.model.MarkAsReadRequest
import com.swparks.model.MessageResponse
import com.swparks.model.Park
import com.swparks.model.ParkForm
import com.swparks.model.SocialUpdates
import com.swparks.model.TextEntryOption
import com.swparks.model.User
import com.swparks.network.SWApi
import kotlinx.coroutines.flow.Flow
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

    // 3.1. Авторизация
    suspend fun register(request: com.swparks.model.RegistrationRequest): Result<LoginSuccess>
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
@Suppress("TooManyFunctions", "TooGenericExceptionCaught", "LargeClass", "MagicNumber", "CyclomaticComplexMethod")
class SWRepositoryImp(
    private val swApi: SWApi,
    private val dataStore: DataStore<Preferences>
) : SWRepository {
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
        Log.e("SWRepository", "Ошибка сети при $operation: ${e.message}")
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
        Log.e("SWRepository", "Ошибка сервера $statusCode при $operation")

        return try {
            val responseBody = e.response()?.errorBody()?.string()
            if (responseBody != null) {
                val errorResponse = json.decodeFromString<ErrorResponse>(responseBody)
                val errorMessage = errorResponse.realMessage ?: "Ошибка сервера: $statusCode"
                ServerException(message = errorMessage, cause = e)
            } else {
                val errorMessage = APIError.fromStatusCode(statusCode).errorMessage
                ServerException(message = errorMessage, cause = e)
            }
        } catch (se: SerializationException) {
            // Если не удалось десериализовать ответ сервера
            Log.e("SWRepository", "Не удалось десериализовать ответ об ошибке: ${se.message}")
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
        request: com.swparks.model.RegistrationRequest
    ): Result<LoginSuccess> =
        try {
            val response = swApi.register(request)
            // Сохраняем токен авторизации при успешной регистрации
            savePreference(true)
            Result.success(response)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "регистрации"))
        } catch (e: HttpException) {
            // Если вернулась ошибка авторизации (401), сохраняем статус авторизации как false
            if (e.code() == APIError.INVALID_CREDENTIALS.statusCode) {
                savePreference(false)
            }
            Result.failure(handleHttpException(e, "регистрации"))
        }

    override suspend fun login(token: String?): Result<LoginSuccess> =
        try {
            val response = swApi.login(token)
            // Сохраняем токен авторизации при успешном входе
            if (token != null) {
                savePreference(true)
            }
            Result.success(response)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "авторизации"))
        } catch (e: HttpException) {
            // Если вернулась ошибка авторизации (401), сохраняем статус авторизации как false
            if (e.code() == APIError.INVALID_CREDENTIALS.statusCode) {
                savePreference(false)
            }
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
            swApi.changePassword(ChangePasswordRequest(current, new))
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "смене пароля"))
        } catch (e: HttpException) {
            // Если вернулась ошибка авторизации (401), сохраняем статус авторизации как false
            if (e.code() == APIError.INVALID_CREDENTIALS.statusCode) {
                savePreference(false)
            }
            Result.failure(handleHttpException(e, "смене пароля"))
        }

    // Принудительный логаут (при ошибке 401)
    override suspend fun forceLogout() {
        savePreference(false)
        Log.i("SWRepository", "Принудительный логаут выполнен")
    }

    // 3.2. Профиль

    override suspend fun getUser(userId: Long): Result<User> =
        try {
            val user = swApi.getUser(userId)
            Result.success(user)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "загрузке пользователя"))
        } catch (e: HttpException) {
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
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
                gender = NetworkUtils.createOptionalPartWithName("gender", form.genderCode.toString()),
                countryId = NetworkUtils.createOptionalPartWithName(
                    "country_id",
                    form.countryId?.toString()
                ),
                cityId = NetworkUtils.createOptionalPartWithName("city_id", form.cityId?.toString()),
                image = NetworkUtils.createOptionalImagePart(image, "image")
            )
            Result.success(user)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "редактировании пользователя"))
        } catch (e: HttpException) {
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
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
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
            Result.failure(handleHttpException(e, "удалении пользователя"))
        }

    override suspend fun getSocialUpdates(userId: Long): Result<SocialUpdates> =
        try {
            val updates = swApi.getSocialUpdates(userId)
            val socialUpdates = SocialUpdates(
                user = updates.user,
                friends = updates.friends,
                friendRequests = updates.friendRequests,
                blacklist = updates.blacklist
            )
            Result.success(socialUpdates)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "загрузке социальных обновлений"))
        } catch (e: HttpException) {
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
            Result.failure(handleHttpException(e, "загрузке социальных обновлений"))
        }

    override suspend fun findUsers(name: String): Result<List<User>> =
        try {
            val users = swApi.findUsers(name)
            Result.success(users)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "поиске пользователей"))
        } catch (e: HttpException) {
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
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
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
            Result.failure(handleHttpException(e, "загрузке друзей"))
        }

    override suspend fun getFriendRequests(): Result<List<User>> =
        try {
            val requests = swApi.getFriendRequests()
            Result.success(requests)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "загрузке заявок в друзья"))
        } catch (e: HttpException) {
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
            Result.failure(handleHttpException(e, "загрузке заявок в друзья"))
        }

    override suspend fun respondToFriendRequest(userId: Long, accept: Boolean): Result<Unit> =
        try {
            if (accept) {
                swApi.acceptFriendRequest(userId)
            } else {
                swApi.declineFriendRequest(userId)
            }
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "обработке заявки в друзья"))
        } catch (e: HttpException) {
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
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
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
            Result.failure(handleHttpException(e, "действии с другом"))
        }

    override suspend fun blacklistAction(user: User, option: ApiBlacklistOption): Result<Unit> =
        try {
            when (option) {
                ApiBlacklistOption.ADD -> swApi.addToBlacklist(user.id)
                ApiBlacklistOption.REMOVE -> swApi.deleteFromBlacklist(user.id)
            }
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "действии с черным списком"))
        } catch (e: HttpException) {
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
            Result.failure(handleHttpException(e, "действии с черным списком"))
        }

    override suspend fun getBlacklist(): Result<List<User>> =
        try {
            val blacklist = swApi.getBlacklist()
            Result.success(blacklist)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "загрузке черного списка"))
        } catch (e: HttpException) {
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
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
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
            Result.failure(handleHttpException(e, "сохранении площадки"))
        }

    override suspend fun deletePark(parkId: Long): Result<Unit> =
        try {
            swApi.deletePark(parkId)
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "удалении площадки"))
        } catch (e: HttpException) {
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
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
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
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
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
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
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
            Result.failure(handleHttpException(e, "изменении участия в мероприятии"))
        }

    override suspend fun deleteEvent(eventId: Long): Result<Unit> =
        try {
            swApi.deleteEvent(eventId)
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "удалении мероприятия"))
        } catch (e: HttpException) {
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
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
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
            Result.failure(handleHttpException(e, "загрузке диалогов"))
        }

    override suspend fun getMessages(dialogId: Long): Result<List<MessageResponse>> =
        try {
            val messages = swApi.getMessages(dialogId)
            Result.success(messages)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "загрузке сообщений"))
        } catch (e: HttpException) {
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
            Result.failure(handleHttpException(e, "загрузке сообщений"))
        }

    override suspend fun sendMessage(message: String, userId: Long): Result<Unit> =
        try {
            swApi.sendMessageTo(userId, message)
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "отправке сообщения"))
        } catch (e: HttpException) {
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
            Result.failure(handleHttpException(e, "отправке сообщения"))
        }

    override suspend fun markAsRead(userId: Long): Result<Unit> =
        try {
            swApi.markAsRead(MarkAsReadRequest(userId))
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "отметке сообщений прочитанными"))
        } catch (e: HttpException) {
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
            Result.failure(handleHttpException(e, "отметке сообщений прочитанными"))
        }

    override suspend fun deleteDialog(dialogId: Long): Result<Unit> =
        try {
            swApi.deleteDialog(dialogId)
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "удалении диалога"))
        } catch (e: HttpException) {
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
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
            swApi.editJournalSettings(
                userId = userId ?: 1L, // Note: передавать реальный userId
                journalId = journalId,
                request = EditJournalSettingsRequest.create(
                    journalId = journalId,
                    title = title,
                    userId = userId?.toInt() ?: 0,
                    viewAccess = viewAccess,
                    commentAccess = commentAccess
                )
            )
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "редактировании настроек дневника"))
        } catch (e: HttpException) {
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
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
            swApi.createJournal(
                userId = userId ?: 1L, // Note: передавать реальный userId
                title = title
            )
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "создании дневника"))
        } catch (e: HttpException) {
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
            Result.failure(handleHttpException(e, "создании дневника"))
        }

    override suspend fun deleteJournal(journalId: Long, userId: Long?): Result<Unit> =
        try {
            swApi.deleteJournal(
                userId = userId ?: 1L, // Note: передавать реальный userId
                journalId = journalId
            )
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "удалении дневника"))
        } catch (e: HttpException) {
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
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
                // Если вернулась ошибка авторизации (401), принудительный логаут
                if (e.code() == 401) {
                    savePreference(false)
                }
                Result.failure(handleHttpException(e, "добавлении комментария"))
            }

            is TextEntryOption.Event -> try {
                swApi.addCommentToEvent(option.id, comment)
                Result.success(Unit)
            } catch (e: IOException) {
                Result.failure(handleIOException(e, "добавлении комментария"))
            } catch (e: HttpException) {
                // Если вернулась ошибка авторизации (401), принудительный логаут
                if (e.code() == 401) {
                    savePreference(false)
                }
                Result.failure(handleHttpException(e, "добавлении комментария"))
            }

            is TextEntryOption.Journal -> {
// Для дневников комментарии добавляются к записям
                val entryId =
                    option.entryId
                        ?: run {
                            Log.e("SWRepository", "entryId не указан для комментария к дневнику")
                            return Result.failure(
                                ServerException(
                                    "entryId обязателен для добавления комментария к дневнику"
                                )
                            )
                        }

                try {
                    swApi.addCommentToJournalEntry(
                        option.ownerId,
                        option.journalId,
                        entryId,
                        comment
                    )
                    Result.success(Unit)
                } catch (e: IOException) {
                    Result.failure(handleIOException(e, "добавлении комментария к дневнику"))
                } catch (e: HttpException) {
                    // Если вернулась ошибка авторизации (401), принудительный логаут
                    if (e.code() == 401) {
                        savePreference(false)
                    }
                    Result.failure(handleHttpException(e, "добавлении комментария к дневнику"))
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
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
            Result.failure(handleHttpException(e, "редактировании комментария"))
        }

        is TextEntryOption.Event -> try {
            swApi.editEventComment(option.id, commentId, newComment)
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, "редактировании комментария"))
        } catch (e: HttpException) {
            // Если вернулась ошибка авторизации (401), принудительный логаут
            if (e.code() == 401) {
                savePreference(false)
            }
            Result.failure(handleHttpException(e, "редактировании комментария"))
        }

        is TextEntryOption.Journal -> {
// Для дневников редактируются комментарии к записям
            val entryId =
                option.entryId
                    ?: run {
                        Log.e(
                            "SWRepository",
                            "entryId не указан для редактирования комментария к дневнику"
                        )
                        return Result.failure(
                            ServerException(
                                "entryId обязателен для редактирования комментария к дневнику"
                            )
                        )
                    }

            try {
                swApi.editJournalEntryComment(
                    option.ownerId,
                    option.journalId,
                    entryId,
                    commentId,
                    newComment
                )
                Result.success(Unit)
            } catch (e: IOException) {
                Result.failure(handleIOException(e, "редактировании комментария к дневнику"))
            } catch (e: HttpException) {
                // Если вернулась ошибка авторизации (401), принудительный логаут
                if (e.code() == 401) {
                    savePreference(false)
                }
                Result.failure(handleHttpException(e, "редактировании комментария к дневнику"))
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
                // Если вернулась ошибка авторизации (401), принудительный логаут
                if (e.code() == 401) {
                    savePreference(false)
                }
                Result.failure(handleHttpException(e, "удалении комментария"))
            }

            is TextEntryOption.Event -> try {
                swApi.deleteEventComment(option.id, commentId)
                Result.success(Unit)
            } catch (e: IOException) {
                Result.failure(handleIOException(e, "удалении комментария"))
            } catch (e: HttpException) {
                // Если вернулась ошибка авторизации (401), принудительный логаут
                if (e.code() == 401) {
                    savePreference(false)
                }
                Result.failure(handleHttpException(e, "удалении комментария"))
            }

            is TextEntryOption.Journal -> {
// Для дневников удаляются комментарии к записям
                val entryId =
                    option.entryId
                        ?: run {
                            Log.e(
                                "SWRepository",
                                "entryId не указан для удаления комментария к дневнику"
                            )
                            return Result.failure(
                                ServerException(
                                    "entryId обязателен для удаления комментария к дневнику"
                                )
                            )
                        }

                try {
                    swApi.deleteJournalEntryComment(
                        option.ownerId,
                        option.journalId,
                        entryId,
                        commentId
                    )
                    Result.success(Unit)
                } catch (e: IOException) {
                    Result.failure(handleIOException(e, "удалении комментария к дневнику"))
                } catch (e: HttpException) {
                    // Если вернулась ошибка авторизации (401), принудительный логаут
                    if (e.code() == 401) {
                        savePreference(false)
                    }
                    Result.failure(handleHttpException(e, "удалении комментария к дневнику"))
                }

            }
        }
}
