package com.swparks.data.repository

import android.content.Context
import com.swparks.data.UserPreferencesRepository
import com.swparks.data.database.dao.EventDao
import com.swparks.data.database.dao.ParkDao
import com.swparks.data.database.dao.UserDao
import com.swparks.data.database.dao.UserTrainingParkDao
import com.swparks.data.database.entity.UserTrainingParkEntity
import com.swparks.data.database.entity.toDomain
import com.swparks.data.database.entity.toEntity
import com.swparks.data.database.entity.toEvent
import com.swparks.data.database.entity.toFullEntity
import com.swparks.data.database.entity.toPark
import com.swparks.data.database.entity.toPartialEntity
import com.swparks.data.model.Event
import com.swparks.data.model.Park
import com.swparks.domain.exception.NotFoundException
import com.swparks.network.SWApi
import com.swparks.ui.model.EventForm
import com.swparks.ui.model.EventType
import com.swparks.ui.model.ParkForm
import com.swparks.util.CrashReporter
import com.swparks.util.Logger
import com.swparks.util.readJSONFromAssets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

/**
 * Реализация репозитория для работы с площадками и мероприятиями.
 *
 * Заменяет монолитный репозиторий, оставляя только методы
 * для площадок, мероприятий и связанных кэшей.
 */
@Suppress(
    "TooManyFunctions",
    "TooGenericExceptionCaught",
    "MagicNumber",
    "CyclomaticComplexMethod",
    "NestedBlockDepth",
    "ReturnCount"
)
open class ParksEventsRepository(
    private val swApi: SWApi,
    private val preferencesRepository: UserPreferencesRepository,
    private val eventDao: EventDao,
    private val parkDao: ParkDao,
    private val userDao: UserDao,
    private val userTrainingParkDao: UserTrainingParkDao? = null,
    logger: Logger,
    crashReporter: CrashReporter
) : BaseRepository(logger, crashReporter) {
    companion object {
        private const val TAG = "ParksEventsRepo"
    }

    /**
     * In-memory StateFlow для будущих мероприятий
     */
    private val futureEventsFlow = MutableStateFlow<List<Event>>(emptyList())

    // ==================== Мероприятия (прошлые) ====================

    open suspend fun getPastEvents(): List<Event> = swApi.getPastEvents()

    // Flow методы для будущих мероприятий
    open fun getFutureEventsFlow(): Flow<List<Event>> = futureEventsFlow.asStateFlow()

    open suspend fun syncFutureEvents(): Result<Unit> =
        try {
            val events = swApi.getFutureEvents()
            futureEventsFlow.value = events
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "синхронизации будущих мероприятий"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "синхронизации будущих мероприятий"))
        }

    // Flow методы для прошедших мероприятий
    open fun getPastEventsFlow(): Flow<List<Event>> =
        eventDao.getAllPastEvents().map { entities ->
            entities.map { it.toEvent() }
        }

    open suspend fun syncPastEvents(): Result<Unit> =
        try {
            val events = swApi.getPastEvents()
            val entities = events.map { it.toPartialEntity() }
            eventDao.insertEventsPartial(entities)
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "синхронизации прошедших мероприятий"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "синхронизации прошедших мероприятий"))
        }

    // ==================== Площадки ====================

    open suspend fun getAllParks(): Result<List<Park>> =
        try {
            val parks = swApi.getAllParks()
            Result.success(parks)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "загрузке площадок"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "загрузке площадок"))
        }

    open suspend fun getPark(id: Long): Result<Park> =
        try {
            val park = swApi.getPark(id)
            cachePark(park)
            Result.success(park)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "загрузке площадки"))
        } catch (e: HttpException) {
            if (e.code() == 404) {
                Result.failure(NotFoundException.ParkNotFound(resourceId = id))
            } else {
                Result.failure(handleHttpException(e, TAG, "загрузке площадки"))
            }
        }

    open suspend fun savePark(
        id: Long?,
        form: ParkForm,
        photos: List<ByteArray>?
    ): Result<Park> =
        try {
            val photoParts =
                photos?.mapIndexed { index, bytes ->
                    val partName = "photo${index + 1}"
                    com.swparks.data.NetworkUtils
                        .createImagePart(bytes, partName)
                }

            val park =
                if (id != null) {
                    swApi.editPark(
                        parkId = id,
                        address =
                            com.swparks.data.NetworkUtils
                                .createPartWithName("address", form.address),
                        latitude =
                            com.swparks.data.NetworkUtils
                                .createPartWithName("latitude", form.latitude),
                        longitude =
                            com.swparks.data.NetworkUtils
                                .createPartWithName("longitude", form.longitude),
                        cityId =
                            com.swparks.data.NetworkUtils.createOptionalPartWithName(
                                "city_id",
                                form.cityId?.toString()
                            ),
                        typeId =
                            com.swparks.data.NetworkUtils
                                .createPartWithName("type_id", form.typeId.toString()),
                        sizeId =
                            com.swparks.data.NetworkUtils.createPartWithName(
                                "class_id",
                                form.sizeId.toString()
                            ),
                        photos = photoParts
                    )
                } else {
                    swApi.createPark(
                        address =
                            com.swparks.data.NetworkUtils
                                .createPartWithName("address", form.address),
                        latitude =
                            com.swparks.data.NetworkUtils
                                .createPartWithName("latitude", form.latitude),
                        longitude =
                            com.swparks.data.NetworkUtils
                                .createPartWithName("longitude", form.longitude),
                        cityId =
                            com.swparks.data.NetworkUtils.createOptionalPartWithName(
                                "city_id",
                                form.cityId?.toString()
                            ),
                        typeId =
                            com.swparks.data.NetworkUtils
                                .createPartWithName("type_id", form.typeId.toString()),
                        sizeId =
                            com.swparks.data.NetworkUtils.createPartWithName(
                                "class_id",
                                form.sizeId.toString()
                            ),
                        photos = photoParts
                    )
                }
            val currentUserId = preferencesRepository.getCurrentUserIdSync()
            if (currentUserId != null) {
                updateUserAddedParksCache(currentUserId, id, park)
            }
            cachePark(park)
            Result.success(park)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "сохранении площадки"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "сохранении площадки"))
        }

    open suspend fun deletePark(parkId: Long): Result<Unit> =
        try {
            val response = swApi.deletePark(parkId)
            if (response.isSuccessful) {
                val currentUserId = preferencesRepository.getCurrentUserIdSync()
                if (currentUserId != null) {
                    removeParkFromUser(currentUserId, parkId)
                }
                parkDao.deleteById(parkId)
                Result.success(Unit)
            } else {
                Result.failure(handleResponseError(response, TAG, "удалении площадки"))
            }
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "удалении площадки"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "удалении площадки"))
        }

    open suspend fun removeParkLocally(parkId: Long): Result<Unit> =
        try {
            val currentUserId = preferencesRepository.getCurrentUserIdSync()
            if (currentUserId != null) {
                removeParkFromUser(currentUserId, parkId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Ошибка локального удаления парка $parkId", e)
            Result.failure(e)
        }

    open suspend fun getParksForUser(userId: Long): Result<List<Park>> =
        try {
            val parks = swApi.getParksForUser(userId)
            cacheParksAndUserRelations(userId, parks)
            Result.success(parks)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "загрузке площадок пользователя"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "загрузке площадок пользователя"))
        }

    open suspend fun changeTrainHereStatus(
        trainHere: Boolean,
        parkId: Long
    ): Result<Unit> =
        try {
            val response =
                if (trainHere) {
                    swApi.postTrainHere(parkId)
                } else {
                    swApi.deleteTrainHere(parkId)
                }
            if (response.isSuccessful) {
                updateTrainHereCache(trainHere, parkId)
                Result.success(Unit)
            } else {
                Result.failure(handleResponseError(response, TAG, "изменении статуса тренировки"))
            }
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "изменении статуса тренировки"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "изменении статуса тренировки"))
        }

    open suspend fun getUpdatedParks(date: String): Result<List<Park>> =
        try {
            val parks = swApi.getUpdatedParks(date)
            Result.success(parks)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "загрузке обновленных площадок"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "загрузке обновленных площадок"))
        }

    open suspend fun deleteParkPhoto(
        parkId: Long,
        photoId: Long
    ): Result<Unit> =
        try {
            val response = swApi.deleteParkPhoto(parkId, photoId)
            if (response.isSuccessful) {
                updateDeletedPhotoCache(parkId, photoId)
                Result.success(Unit)
            } else {
                val errorMessage = parseErrorResponse(response, TAG, "удалении фото площадки")
                Result.failure(
                    com.swparks.domain.exception
                        .ServerException(message = errorMessage)
                )
            }
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "удалении фото площадки"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "удалении фото площадки"))
        }

    // Локальное хранение площадок (Room)

    open fun getParksFlow(): Flow<List<Park>> =
        parkDao.getAllParks().map { entities ->
            entities.map { it.toPark() }
        }

    open suspend fun importSeedParks(context: Context) {
        withContext(Dispatchers.IO) {
            if (parkDao.isEmpty()) {
                logger.i(TAG, "Импорт seed parks из assets в Room")
                val jsonString = readJSONFromAssets(context, "parks.json")
                val parks: List<Park> = json.decodeFromString(jsonString)
                val entities = parks.map { it.toEntity() }
                parkDao.insertAll(entities)
                logger.i(TAG, "Импортировано ${entities.size} parks в Room")
            } else {
                logger.i(TAG, "Parks уже импортированы, пропускаем seed")
            }
        }
    }

    open suspend fun upsertParks(parks: List<Park>) {
        withContext(Dispatchers.IO) {
            val entities = parks.map { it.toEntity() }
            parkDao.insertAll(entities)
            logger.d(TAG, "Upsert ${entities.size} parks в Room")
        }
    }

    // Cache API для ParkDetailScreen

    open suspend fun getParkFromCache(parkId: Long): Park? =
        withContext(Dispatchers.IO) {
            parkDao.getParkById(parkId)?.toPark()
        }

    open suspend fun cachePark(park: Park) {
        withContext(Dispatchers.IO) {
            parkDao.upsertPark(park.toEntity())
            logger.d(TAG, "Площадка ${park.id} закэширована в Room")
        }
    }

    // Cache API для past EventDetailScreen

    open suspend fun getEventFromCache(eventId: Long): Event? =
        withContext(Dispatchers.IO) {
            eventDao.getEventById(eventId)?.takeIf { it.isFull }?.toEvent()
        }

    open suspend fun saveEventFull(event: Event) {
        withContext(Dispatchers.IO) {
            eventDao.upsertEventFull(event.toFullEntity())
            logger.d(TAG, "Мероприятие ${event.id} закэшировано в Room как full snapshot")
        }
    }

    // Cache API для UserTrainingParksScreen

    open suspend fun getCachedParksForUser(userId: Long): List<Park>? =
        withContext(Dispatchers.IO) {
            if (userTrainingParkDao == null || !userTrainingParkDao.hasCachedParksForUser(userId)) {
                null
            } else {
                userTrainingParkDao.getParksForUserFromCache(userId).map { it.toPark() }
            }
        }

    open suspend fun hasCachedParksForUser(userId: Long): Boolean =
        withContext(Dispatchers.IO) {
            userTrainingParkDao?.hasCachedParksForUser(userId) ?: false
        }

    // ==================== Мероприятия ====================

    open suspend fun getEvents(type: EventType): Result<List<Event>> =
        try {
            val events =
                when (type) {
                    EventType.FUTURE -> swApi.getFutureEvents()
                    EventType.PAST -> swApi.getPastEvents()
                }
            Result.success(events)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "загрузке мероприятий"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "загрузке мероприятий"))
        }

    open suspend fun getEvent(id: Long): Result<Event> =
        try {
            val event = swApi.getEvent(id)
            Result.success(event)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "загрузке мероприятия"))
        } catch (e: HttpException) {
            if (e.code() == 404) {
                Result.failure(NotFoundException.EventNotFound(resourceId = id))
            } else {
                Result.failure(handleHttpException(e, TAG, "загрузке мероприятия"))
            }
        }

    open suspend fun saveEvent(
        id: Long?,
        form: EventForm,
        photos: List<ByteArray>?
    ): Result<Event> =
        try {
            val photoParts =
                photos?.mapIndexed { index, bytes ->
                    val partName = "photo${index + 1}"
                    com.swparks.data.NetworkUtils
                        .createImagePart(bytes, partName)
                }

            val event =
                if (id != null) {
                    swApi.editEvent(
                        eventId = id,
                        title =
                            com.swparks.data.NetworkUtils
                                .createPartWithName("title", form.title),
                        description =
                            com.swparks.data.NetworkUtils.createPartWithName(
                                "description",
                                form.description
                            ),
                        date =
                            com.swparks.data.NetworkUtils
                                .createPartWithName("date", form.date),
                        parkId =
                            com.swparks.data.NetworkUtils
                                .createPartWithName("area_id", form.parkId.toString()),
                        photos = photoParts
                    )
                } else {
                    swApi.createEvent(
                        title =
                            com.swparks.data.NetworkUtils
                                .createPartWithName("title", form.title),
                        description =
                            com.swparks.data.NetworkUtils.createPartWithName(
                                "description",
                                form.description
                            ),
                        date =
                            com.swparks.data.NetworkUtils
                                .createPartWithName("date", form.date),
                        parkId =
                            com.swparks.data.NetworkUtils
                                .createPartWithName("area_id", form.parkId.toString()),
                        photos = photoParts
                    )
                }
            Result.success(event)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "сохранении мероприятия"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "сохранении мероприятия"))
        }

    open suspend fun changeIsGoingToEvent(
        go: Boolean,
        eventId: Long
    ): Result<Unit> =
        try {
            val response =
                if (go) {
                    swApi.postGoToEvent(eventId)
                } else {
                    swApi.deleteGoToEvent(eventId)
                }
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(handleResponseError(response, TAG, "изменении участия в мероприятии"))
            }
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "изменении участия в мероприятии"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "изменении участия в мероприятии"))
        }

    open suspend fun deleteEvent(eventId: Long): Result<Unit> =
        try {
            val response = swApi.deleteEvent(eventId)
            if (response.isSuccessful) {
                futureEventsFlow.value = futureEventsFlow.value.filter { it.id != eventId }
                eventDao.deleteById(eventId)
                Result.success(Unit)
            } else {
                Result.failure(handleResponseError(response, TAG, "удалении мероприятия"))
            }
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "удалении мероприятия"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "удалении мероприятия"))
        }

    open suspend fun deleteEventPhoto(
        eventId: Long,
        photoId: Long
    ): Result<Unit> =
        try {
            val response = swApi.deleteEventPhoto(eventId, photoId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMessage = parseErrorResponse(response, TAG, "удалении фото события")
                Result.failure(
                    com.swparks.domain.exception
                        .ServerException(message = errorMessage)
                )
            }
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "удалении фото мероприятия"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "удалении фото мероприятия"))
        }

    open suspend fun removeEventLocally(eventId: Long): Result<Unit> =
        try {
            futureEventsFlow.value = futureEventsFlow.value.filter { it.id != eventId }
            eventDao.deleteById(eventId)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Ошибка локального удаления мероприятия $eventId", e)
            Result.failure(e)
        }

    // ==================== Private helpers ====================

    private suspend fun cacheParksAndUserRelations(
        userId: Long,
        parks: List<Park>
    ) {
        withContext(Dispatchers.IO) {
            if (userTrainingParkDao == null) {
                logger.d(TAG, "userTrainingParkDao не доступен, пропускаем кэширование связей")
                return@withContext
            }
            val parkEntities = parks.map { it.toEntity() }
            if (parkEntities.isNotEmpty()) {
                parkDao.insertAll(parkEntities)
                logger.d(TAG, "Upsert ${parkEntities.size} parks в общую таблицу parks")
            }

            val relations =
                parks.mapIndexed { index, park ->
                    UserTrainingParkEntity(
                        userId = userId,
                        parkId = park.id,
                        position = index
                    )
                }
            userTrainingParkDao.replaceForUser(userId, relations)
            logger.d(TAG, "Сохранены связи пользователя $userId с ${relations.size} площадками")
        }
    }

    private suspend fun updateTrainHereCache(
        trainHere: Boolean,
        parkId: Long
    ) {
        val cachedPark = parkDao.getParkById(parkId) ?: return
        val currentUserId = preferencesRepository.getCurrentUserIdSync()

        val updatedTrainingUsers = cachedPark.trainingUsers?.toMutableList()
        val updatedCount = cachedPark.trainingUsersCount

        if (trainHere) {
            if (currentUserId != null && updatedTrainingUsers != null && updatedCount != null) {
                val currentUserEntity = userDao.getUserByIdFlow(currentUserId).first()
                if (currentUserEntity != null) {
                    val currentUser = currentUserEntity.toDomain()
                    if (updatedTrainingUsers.none { it.id == currentUser.id }) {
                        updatedTrainingUsers.add(currentUser)
                    }
                }
            }
        } else {
            if (currentUserId != null && updatedTrainingUsers != null) {
                updatedTrainingUsers.removeAll { it.id == currentUserId }
            }
        }

        val newCount =
            updatedCount?.let { count ->
                if (trainHere) {
                    count + 1
                } else {
                    maxOf(count - 1, 0)
                }
            }

        parkDao.upsertPark(
            cachedPark.copy(
                trainHere = trainHere,
                trainingUsers = updatedTrainingUsers,
                trainingUsersCount = newCount
            )
        )

        if (currentUserId != null) {
            val currentUserEntity = userDao.getUserByIdFlow(currentUserId).first()
            if (currentUserEntity != null) {
                val currentParksCount = currentUserEntity.parksCount?.toIntOrNull() ?: 0
                val updatedParksCount =
                    if (trainHere) {
                        currentParksCount + 1
                    } else {
                        maxOf(currentParksCount - 1, 0)
                    }
                userDao.insert(currentUserEntity.copy(parksCount = updatedParksCount.toString()))
                logger.d(
                    TAG,
                    "Обновлён parksCount текущего пользователя: $currentParksCount -> $updatedParksCount"
                )
            }
        }

        if (currentUserId != null && userTrainingParkDao != null) {
            if (trainHere) {
                userTrainingParkDao.insertForUser(
                    listOf(UserTrainingParkEntity(userId = currentUserId, parkId = parkId))
                )
            } else {
                userTrainingParkDao.deleteRelation(currentUserId, parkId)
            }
            logger.d(TAG, "Синхронизирована связь trainHere для пользователя $currentUserId и площадки $parkId")
        }

        logger.d(TAG, "Обновлён кэш trainHere для площадки $parkId")
    }

    private suspend fun updateDeletedPhotoCache(
        parkId: Long,
        photoId: Long
    ) {
        val cachedPark = parkDao.getParkById(parkId) ?: return
        val photos = cachedPark.photos ?: return

        val photoToDelete = photos.find { it.id == photoId } ?: return
        val updatedPhotos = photos.filter { it.id != photoId }

        val newPreview =
            when {
                cachedPark.preview != photoToDelete.photo -> cachedPark.preview
                updatedPhotos.isNotEmpty() -> updatedPhotos.first().photo
                else -> ""
            }

        parkDao.upsertPark(
            cachedPark.copy(
                photos = updatedPhotos,
                preview = newPreview
            )
        )
        logger.d(TAG, "Обновлён кэш фото для площадки $parkId, удалено фото $photoId")
    }

    private suspend fun removeParkFromUser(
        userId: Long,
        parkId: Long
    ) {
        val user = userDao.getUserByIdFlow(userId).first() ?: return
        val currentParks = user.addedParks.orEmpty().toMutableList()
        currentParks.removeAll { it.id == parkId }
        userDao.insert(user.copy(addedParks = currentParks))
        logger.d(TAG, "Парк $parkId удалён из addedParks пользователя $userId")
    }

    private suspend fun updateUserAddedParksCache(
        currentUserId: Long,
        editedParkId: Long?,
        savedPark: Park
    ) {
        val user = userDao.getUserByIdFlow(currentUserId).first() ?: return
        val currentParks = user.addedParks.orEmpty().toMutableList()
        if (editedParkId == null) {
            if (currentParks.none { it.id == savedPark.id }) {
                currentParks.add(savedPark)
                userDao.insert(user.copy(addedParks = currentParks))
                logger.d(
                    TAG,
                    "Парк ${savedPark.id} добавлен в addedParks пользователя $currentUserId"
                )
            }
        } else {
            currentParks.removeAll { it.id == editedParkId }
            if (currentParks.none { it.id == savedPark.id }) {
                currentParks.add(savedPark)
            }
            userDao.insert(user.copy(addedParks = currentParks))
            logger.d(TAG, "Парк ${savedPark.id} обновлён в addedParks пользователя $currentUserId")
        }
    }
}
