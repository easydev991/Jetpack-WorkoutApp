package com.swparks.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.swparks.data.UserPreferencesRepository
import com.swparks.data.model.Comment
import com.swparks.data.model.Photo
import com.swparks.data.model.User
import com.swparks.data.model.removePhotoById
import com.swparks.data.repository.SWRepository
import com.swparks.domain.repository.CountriesRepository
import com.swparks.ui.ds.CommentAction
import com.swparks.ui.model.EditInfo
import com.swparks.ui.model.MapUriSet
import com.swparks.ui.model.TextEntryMode
import com.swparks.ui.model.TextEntryOption
import com.swparks.ui.state.EventDetailUIState
import com.swparks.util.AppError
import com.swparks.util.Complaint
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/**
 * События UI экрана детальной информации о мероприятии.
 *
 * Используются для одноразовых действий, которые должны быть обработаны в UI
 * (например, показ диалогов подтверждения, навигация).
 */
sealed class EventDetailEvent {
    /**
     * Показать диалог подтверждения удаления мероприятия.
     */
    data object ShowDeleteConfirmDialog : EventDetailEvent()

    /**
     * Показать диалог подтверждения удаления фото.
     *
     * @param photo Фотография для удаления
     */
    data class ShowDeletePhotoConfirmDialog(val photo: Photo) : EventDetailEvent()

    /**
     * Мероприятие успешно удалено, нужно закрыть экран.
     */
    data object EventDeleted : EventDetailEvent()

    /**
     * Фото успешно удалено.
     */
    data class PhotoDeleted(val photoId: Long) : EventDetailEvent()

    /**
     * Открыть системный календарь для добавления события.
     *
     * @param title Название мероприятия
     * @param beginDate Дата начала мероприятия
     * @param address Адрес мероприятия
     */
    data class OpenCalendar(
        val title: String,
        val beginDate: String,
        val address: String
    ) : EventDetailEvent()

    /**
     * Открыть карту по координатам мероприятия.
     */
    data object OpenMap : EventDetailEvent()

    /**
     * Построить маршрут до мероприятия.
     */
    data object BuildRoute : EventDetailEvent()

    /**
     * Навигация к списку участников мероприятия.
     *
     * @param eventId Идентификатор мероприятия
     * @param users Список участников
     */
    data class NavigateToParticipants(
        val eventId: Long,
        val users: List<User>
    ) : EventDetailEvent()

    /**
     * Отправить жалобу на комментарий через почтовый клиент.
     *
     * @param complaint Готовая модель жалобы на комментарий к мероприятию
     */
    data class SendCommentComplaint(
        val complaint: Complaint.EventComment
    ) : EventDetailEvent()

    /**
     * Открыть TextEntrySheet для создания/редактирования комментария к мероприятию.
     *
     * @param mode Режим экрана ввода текста
     */
    data class OpenCommentTextEntry(
        val mode: TextEntryMode
    ) : EventDetailEvent()

    /**
     * Показать диалог подтверждения удаления комментария.
     */
    data object ShowDeleteCommentConfirmDialog : EventDetailEvent()

    /**
     * Навигация к экрану детального просмотра фотографии.
     *
     * @param photo Фотография для просмотра
     * @param eventId Идентификатор мероприятия
     * @param eventTitle Название мероприятия
     * @param isEventAuthor Является ли пользователь автором мероприятия
     */
    data class NavigateToPhotoDetail(
        val photo: Photo,
        val eventId: Long,
        val eventTitle: String,
        val isEventAuthor: Boolean
    ) : EventDetailEvent()
}

/**
 * ViewModel для экрана детальной информации о мероприятии.
 *
 * Управляет загрузкой и отображением данных мероприятия,
 * обрабатывает пользовательские действия.
 *
 * @param swRepository Репозиторий для работы с сервером
 * @param countriesRepository Репозиторий для работы со странами и городами
 * @param userPreferencesRepository Репозиторий настроек пользователя
 * @param savedStateHandle Для получения eventId из аргументов навигации
 * @param userNotifier Интерфейс для обработки и отправки ошибок в UI-слой
 * @param logger Логгер для записи отладочной информации
 */
@Suppress("TooManyFunctions", "TooGenericExceptionCaught", "InstanceOfCheckForException")
class EventDetailViewModel(
    private val swRepository: SWRepository,
    private val countriesRepository: CountriesRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val savedStateHandle: SavedStateHandle,
    private val userNotifier: UserNotifier,
    private val logger: Logger,
) : ViewModel(), IEventDetailViewModel {

    companion object {
        private const val TAG = "EventDetailViewModel"
        private const val EVENT_ID_KEY = "eventId"
        private const val NO_CALENDAR_APP_MESSAGE = "Нет календарного приложения на устройстве."
        private const val UNKNOWN_COMMENT_AUTHOR = "неизвестен"

        fun factory(
            swRepository: SWRepository,
            countriesRepository: CountriesRepository,
            userPreferencesRepository: UserPreferencesRepository,
            userNotifier: UserNotifier,
            logger: Logger,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle()
                EventDetailViewModel(
                    swRepository = swRepository,
                    countriesRepository = countriesRepository,
                    userPreferencesRepository = userPreferencesRepository,
                    savedStateHandle = savedStateHandle,
                    userNotifier = userNotifier,
                    logger = logger
                )
            }
        }
    }

    private enum class ManageEventAction(val logName: String) {
        DeleteEventClick("delete_event_click"),
        DeleteEventConfirm("delete_event_confirm"),
        DeletePhotoClick("delete_photo_click"),
        DeletePhotoConfirm("delete_photo_confirm")
    }

    // UI State
    private val _uiState =
        MutableStateFlow<EventDetailUIState>(EventDetailUIState.InitialLoading)
    override val uiState: StateFlow<EventDetailUIState> = _uiState.asStateFlow()

    // Pull-to-refresh
    private val _isRefreshing = MutableStateFlow(false)
    override val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Авторизация
    private val _isAuthorized = MutableStateFlow(false)
    override val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    // Автор мероприятия
    private val _isEventAuthor = MutableStateFlow(false)
    override val isEventAuthor: StateFlow<Boolean> = _isEventAuthor.asStateFlow()

    // Текущий пользователь
    private val _currentUserId = MutableStateFlow<Long?>(null)
    override val currentUserId: StateFlow<Long?> = _currentUserId.asStateFlow()

    // URI для карты
    override val mapUriSet: MapUriSet?
        get() {
            val currentState = _uiState.value
            if (currentState !is EventDetailUIState.Content) {
                return null
            }

            val latitude = currentState.event.latitude.toDoubleOrNull()
            val longitude = currentState.event.longitude.toDoubleOrNull()

            return if (latitude != null && longitude != null) {
                MapUriSet(
                    latitude = latitude,
                    longitude = longitude
                )
            } else {
                null
            }
        }

    // События UI
    private val _events = MutableSharedFlow<EventDetailEvent>()
    override val events: SharedFlow<EventDetailEvent> = _events.asSharedFlow()

    // Фото для удаления (временно хранится до подтверждения)
    private var pendingDeletePhoto: Photo? = null
    private var pendingDeleteCommentId: Long? = null

    init {
        logger.d(TAG, "Инициализация EventDetailViewModel")
        observeAuthorization()
        loadEvent()
    }

    private fun observeAuthorization() {
        viewModelScope.launch {
            userPreferencesRepository.isAuthorized.collect { isAuth ->
                _isAuthorized.value = isAuth
                logger.d(TAG, "Состояние авторизации: $isAuth")
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.currentUserId.collect { userId ->
                _currentUserId.value = userId
                updateIsEventAuthor()
                logger.d(TAG, "Текущий userId: $userId")
            }
        }
    }

    private fun updateIsEventAuthor() {
        val currentState = _uiState.value
        if (currentState is EventDetailUIState.Content) {
            val eventAuthorId = currentState.event.author.id
            _isEventAuthor.value =
                _currentUserId.value != null && _currentUserId.value == eventAuthorId
            logger.d(
                TAG,
                "isEventAuthor: ${_isEventAuthor.value} " +
                    "(currentUserId=${_currentUserId.value}, authorId=$eventAuthorId)"
            )
        }
    }

    private fun getEventId(): Long? {
        return savedStateHandle.get<Long>(EVENT_ID_KEY)
            ?: savedStateHandle.get<Int>(EVENT_ID_KEY)?.toLong()
            ?: savedStateHandle.get<String>(EVENT_ID_KEY)?.toLongOrNull()
    }

    private fun canManageEvent(action: ManageEventAction): Boolean {
        val canManage = _isAuthorized.value && _isEventAuthor.value
        if (!canManage) {
            if (!_isAuthorized.value) {
                logger.w(TAG, "Отклонено действие '${action.logName}': пользователь не авторизован")
            } else {
                logger.w(
                    TAG,
                    "Отклонено действие '${action.logName}': пользователь не автор мероприятия"
                )
            }
        }
        return canManage
    }

    private fun loadEvent() {
        val eventId = getEventId()

        if (eventId == null) {
            logger.e(TAG, "eventId отсутствует в SavedStateHandle")
            _uiState.value = EventDetailUIState.Error("Неверный идентификатор мероприятия")
            return
        }

        logger.d(TAG, "Загрузка мероприятия id=$eventId")
        viewModelScope.launch {
            try {
                val result = swRepository.getEvent(eventId)
                result.fold(
                    onSuccess = { event ->
                        logger.i(TAG, "Мероприятие загружено: ${event.title}")
                        val address = buildAddress(event.countryID, event.cityID, event.address)
                        val authorAddress = buildAuthorAddress(event.author)
                        _uiState.value = EventDetailUIState.Content(
                            event = event,
                            address = address,
                            authorAddress = authorAddress
                        )
                        updateIsEventAuthor()
                    },
                    onFailure = { exception ->
                        logger.e(
                            TAG,
                            "Ошибка загрузки мероприятия: ${exception.message}",
                            exception
                        )
                        handleError(exception, "загрузке мероприятия")
                        _uiState.value = EventDetailUIState.Error(exception.message)
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logger.e(TAG, "Исключение при загрузке мероприятия: ${e.message}", e)
                handleError(e, "загрузке мероприятия")
                _uiState.value = EventDetailUIState.Error(e.message)
            }
        }
    }

    private suspend fun buildAddress(countryId: Int, cityId: Int, serverAddress: String?): String {
        val address = buildAddressNullable(countryId, cityId, serverAddress)
        return address ?: "$countryId, $cityId"
    }

    private suspend fun buildAuthorAddress(user: User): String {
        return buildAddressNullable(user.countryID, user.cityID, null) ?: ""
    }

    private suspend fun buildAddressNullable(
        countryId: Int?,
        cityId: Int?,
        serverAddress: String?
    ): String? {
        if (countryId == null && cityId == null && serverAddress.isNullOrBlank()) {
            return null
        }

        return try {
            val country = countryId?.let { countriesRepository.getCountryById(it.toString()) }
            val city = cityId?.let { countriesRepository.getCityById(it.toString()) }

            when {
                country != null && city != null -> "${country.name}, ${city.name}"
                country != null -> country.name
                city != null -> city.name
                !serverAddress.isNullOrBlank() -> serverAddress
                else -> null
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logger.e(TAG, "Ошибка загрузки адреса ($countryId, $cityId): ${e.message}")
            serverAddress?.takeIf { it.isNotBlank() }
        }
    }

    override fun refresh() {
        logger.d(TAG, "Pull-to-refresh: обновление мероприятия")
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val eventId = getEventId()
                if (eventId == null) {
                    logger.e(TAG, "eventId отсутствует при refresh")
                    return@launch
                }
                refreshEventContent(eventId)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun refreshEventContent(eventId: Long) {
        val result = swRepository.getEvent(eventId)
        result.fold(
            onSuccess = { event ->
                logger.i(TAG, "Мероприятие обновлено: ${event.title}")
                val address = buildAddress(event.countryID, event.cityID, event.address)
                val authorAddress = buildAuthorAddress(event.author)
                _uiState.value = EventDetailUIState.Content(
                    event = event,
                    address = address,
                    authorAddress = authorAddress
                )
                updateIsEventAuthor()
            },
            onFailure = { exception ->
                logger.e(
                    TAG,
                    "Ошибка обновления мероприятия: ${exception.message}",
                    exception
                )
                handleError(exception, "обновлении мероприятия")
            }
        )
    }

    // ==================== Действия редактирования и удаления ====================

    override fun onEditClick() {
        val currentState = _uiState.value
        if (currentState is EventDetailUIState.Content) {
            logger.d(TAG, "Нажата кнопка редактирования мероприятия id=${currentState.event.id}")
        }
    }

    override fun onDeleteClick() {
        val currentState = _uiState.value
        if (
            currentState is EventDetailUIState.Content &&
            canManageEvent(ManageEventAction.DeleteEventClick)
        ) {
            logger.d(TAG, "Нажата кнопка удаления мероприятия id=${currentState.event.id}")
            viewModelScope.launch {
                _events.emit(EventDetailEvent.ShowDeleteConfirmDialog)
            }
        }
    }

    override fun onDeleteConfirm() {
        val currentState = _uiState.value
        if (currentState !is EventDetailUIState.Content) return
        if (!canManageEvent(ManageEventAction.DeleteEventConfirm)) return

        val eventId = currentState.event.id
        logger.d(TAG, "Подтверждение удаления мероприятия id=$eventId")

        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val result = swRepository.deleteEvent(eventId)
                result.fold(
                    onSuccess = {
                        logger.i(TAG, "Мероприятие id=$eventId успешно удалено")
                        viewModelScope.launch {
                            _events.emit(EventDetailEvent.EventDeleted)
                        }
                    },
                    onFailure = { exception ->
                        logger.e(
                            TAG,
                            "Ошибка удаления мероприятия: ${exception.message}",
                            exception
                        )
                        handleError(exception, "удалении мероприятия")
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logger.e(TAG, "Исключение при удалении мероприятия: ${e.message}", e)
                handleError(e, "удалении мероприятия")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    override fun onDeleteDismiss() {
        logger.d(TAG, "Отмена удаления мероприятия")
    }

    // ==================== Действия с фото ====================

    override fun onPhotoClick(photo: Photo) {
        logger.d(TAG, "Нажата фотография id=${photo.id}")
        val currentState = _uiState.value
        if (currentState is EventDetailUIState.Content) {
            viewModelScope.launch {
                _events.emit(
                    EventDetailEvent.NavigateToPhotoDetail(
                        photo = photo,
                        eventId = currentState.event.id,
                        eventTitle = currentState.event.title,
                        isEventAuthor = _isEventAuthor.value
                    )
                )
            }
        }
    }

    override fun onPhotoDeleteClick(photo: Photo) {
        if (!canManageEvent(ManageEventAction.DeletePhotoClick)) return
        logger.d(TAG, "Нажата кнопка удаления фото id=${photo.id}")
        pendingDeletePhoto = photo
        viewModelScope.launch {
            _events.emit(EventDetailEvent.ShowDeletePhotoConfirmDialog(photo))
        }
    }

    override fun onPhotoDeleteConfirm() {
        val photo = pendingDeletePhoto ?: return
        val currentState = _uiState.value
        if (currentState is EventDetailUIState.Content) {
            if (!canManageEvent(ManageEventAction.DeletePhotoConfirm)) {
                pendingDeletePhoto = null
                return
            }

            val eventId = currentState.event.id
            val photoId = photo.id
            logger.d(TAG, "Подтверждение удаления фото id=$photoId мероприятия id=$eventId")

            viewModelScope.launch {
                try {
                    val result = swRepository.deleteEventPhoto(eventId, photoId)
                    result.fold(
                        onSuccess = {
                            logger.i(TAG, "Фото id=$photoId успешно удалено")
                            val updatedPhotos = currentState.event.photos.removePhotoById(photoId)
                            logger.d(
                                TAG,
                                "Перенумерация фото: было ${currentState.event.photos.size}, " +
                                    "стало ${updatedPhotos.size}"
                            )
                            val updatedEvent = currentState.event.copy(photos = updatedPhotos)
                            _uiState.value = currentState.copy(event = updatedEvent)
                            viewModelScope.launch {
                                _events.emit(EventDetailEvent.PhotoDeleted(photoId))
                            }
                        },
                        onFailure = { exception ->
                            logger.e(TAG, "Ошибка удаления фото: ${exception.message}", exception)
                            handleError(exception, "удалении фото")
                        }
                    )
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    logger.e(TAG, "Исключение при удалении фото: ${e.message}", e)
                    handleError(e, "удалении фото")
                } finally {
                    pendingDeletePhoto = null
                }
            }
        }
    }

    override fun onPhotoDeleteDismiss() {
        logger.d(TAG, "Отмена удаления фото")
        pendingDeletePhoto = null
    }

    // ==================== Прочие действия ====================

    override fun onShareClick() {
        val currentState = _uiState.value
        if (currentState is EventDetailUIState.Content) {
            logger.d(TAG, "Нажата кнопка 'Поделиться' для мероприятия id=${currentState.event.id}")
        }
    }

    override fun onParticipantToggle() {
        val currentState = _uiState.value
        if (currentState !is EventDetailUIState.Content) return

        val currentValue = currentState.event.trainHere ?: false
        val newValue = !currentValue
        val eventId = currentState.event.id

        logger.d(
            TAG,
            "Toggle 'Пойду' для мероприятия id=$eventId: $currentValue -> $newValue"
        )

        val optimisticEvent = currentState.event.copy(trainHere = newValue)
        _uiState.value = currentState.copy(event = optimisticEvent)
        _isRefreshing.value = true

        viewModelScope.launch {
            try {
                val result = swRepository.changeIsGoingToEvent(newValue, eventId)
                _isRefreshing.value = false

                if (result.isSuccess) {
                    val currentUser = swRepository.getCurrentUserFlow().first()
                    val updatedUsers = if (newValue && currentUser != null) {
                        (currentState.event.trainingUsers.orEmpty() + currentUser)
                            .distinctBy { it.id }
                    } else {
                        currentState.event.trainingUsers.orEmpty()
                            .filterNot { it.id == _currentUserId.value }
                    }
                    val finalEvent = optimisticEvent.copy(
                        trainingUsers = updatedUsers,
                        trainingUsersCount = updatedUsers.size
                    )
                    _uiState.value = currentState.copy(event = finalEvent)
                    logger.i(
                        TAG,
                        "Участие в мероприятии id=$eventId обновлено: trainHere=$newValue"
                    )
                } else {
                    _uiState.value = currentState
                    val error = result.exceptionOrNull() ?: Exception("Unknown error")
                    handleError(error, "изменении участия в мероприятии")
                    logger.e(TAG, "Ошибка изменения участия: ${error.message}")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _isRefreshing.value = false
                _uiState.value = currentState
                handleError(e, "изменении участия в мероприятии")
                logger.e(TAG, "Исключение при изменении участия: ${e.message}", e)
            }
        }
    }

    override fun onParticipantsCountClick() {
        val currentState = _uiState.value
        if (currentState is EventDetailUIState.Content) {
            logger.d(
                TAG,
                "Нажато количество участников (${currentState.event.trainingUsersCount}) " +
                    "для мероприятия id=${currentState.event.id}"
            )
            // Отправляем событие навигации
            viewModelScope.launch {
                _events.emit(
                    EventDetailEvent.NavigateToParticipants(
                        eventId = currentState.event.id,
                        users = currentState.event.trainingUsers.orEmpty()
                    )
                )
            }
        }
    }

    override fun onOpenMapClick() {
        val currentState = _uiState.value
        if (currentState is EventDetailUIState.Content) {
            val event = currentState.event
            logger.d(TAG, "Нажата кнопка 'Открыть на карте' для мероприятия id=${event.id}")
            viewModelScope.launch {
                _events.emit(EventDetailEvent.OpenMap)
            }
        }
    }

    override fun onRouteClick() {
        val currentState = _uiState.value
        if (currentState is EventDetailUIState.Content) {
            val event = currentState.event
            logger.d(TAG, "Нажата кнопка 'Построить маршрут' для мероприятия id=${event.id}")
            viewModelScope.launch {
                _events.emit(EventDetailEvent.BuildRoute)
            }
        }
    }

    override fun onAddToCalendarClick() {
        val currentState = _uiState.value
        if (currentState is EventDetailUIState.Content) {
            val event = currentState.event
            if (!event.isCurrent) {
                logger.w(
                    TAG,
                    "Отклонено добавление в календарь: мероприятие id=${event.id} уже завершено"
                )
                return
            }
            logger.d(TAG, "Нажата кнопка 'Добавить в календарь' для мероприятия id=${event.id}")
            viewModelScope.launch {
                _events.emit(
                    EventDetailEvent.OpenCalendar(
                        title = event.title,
                        beginDate = event.beginDate,
                        address = currentState.address
                    )
                )
            }
        }
    }

    override fun onAddToCalendarFailed() {
        logger.w(TAG, NO_CALENDAR_APP_MESSAGE)
        userNotifier.handleError(
            AppError.Generic(
                message = NO_CALENDAR_APP_MESSAGE,
                throwable = null
            )
        )
    }

    override fun onAddCommentClick() {
        val currentState = _uiState.value
        if (currentState is EventDetailUIState.Content) {
            logger.d(
                TAG,
                "Нажата кнопка 'Добавить комментарий' для мероприятия id=${currentState.event.id}"
            )
            viewModelScope.launch {
                _events.emit(
                    EventDetailEvent.OpenCommentTextEntry(
                        mode = TextEntryMode.NewForEvent(currentState.event.id)
                    )
                )
            }
        }
    }

    override fun onCommentActionClick(commentId: Long, action: CommentAction) {
        val currentState = _uiState.value
        if (currentState !is EventDetailUIState.Content) return

        logger.d(TAG, "Нажато действие $action для комментария id=$commentId")

        val comment = currentState.event.comments.orEmpty().firstOrNull { it.id == commentId }
        if (comment == null) {
            logger.w(TAG, "Комментарий не найден: id=$commentId")
            return
        }

        when (action) {
            CommentAction.EDIT -> handleEditComment(comment, currentState.event.id)
            CommentAction.REPORT -> handleReportComment(comment, currentState.event.title)
            CommentAction.DELETE -> handleDeleteComment(comment, commentId)
        }
    }

    private fun handleEditComment(comment: Comment, eventId: Long) {
        viewModelScope.launch {
            _events.emit(
                EventDetailEvent.OpenCommentTextEntry(
                    mode = TextEntryMode.EditEvent(
                        editInfo = EditInfo(
                            parentObjectId = eventId,
                            entryId = comment.id,
                            oldEntry = comment.parsedBody.orEmpty()
                        )
                    )
                )
            )
        }
    }

    private fun handleReportComment(comment: Comment, eventTitle: String) {
        val complaintAuthor = comment.user?.name?.ifBlank { UNKNOWN_COMMENT_AUTHOR }
            ?: UNKNOWN_COMMENT_AUTHOR
        val complaintText = comment.parsedBody ?: comment.body.orEmpty()

        viewModelScope.launch {
            _events.emit(
                EventDetailEvent.SendCommentComplaint(
                    complaint = Complaint.EventComment(
                        eventTitle = eventTitle,
                        author = complaintAuthor,
                        commentText = complaintText
                    )
                )
            )
        }
    }

    private fun handleDeleteComment(comment: Comment, commentId: Long) {
        val isOwnComment = comment.user?.id != null && comment.user.id == _currentUserId.value
        if (!isOwnComment) {
            logger.w(TAG, "Отклонено удаление комментария id=$commentId: не автор")
            return
        }

        pendingDeleteCommentId = commentId
        viewModelScope.launch {
            _events.emit(EventDetailEvent.ShowDeleteCommentConfirmDialog)
        }
    }

    override fun onCommentDeleteConfirm() {
        val currentState = _uiState.value
        if (currentState !is EventDetailUIState.Content) return
        val commentId = pendingDeleteCommentId ?: return

        logger.d(TAG, "Подтверждение удаления комментария id=$commentId")
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val result = swRepository.deleteComment(
                    option = TextEntryOption.Event(currentState.event.id),
                    commentId = commentId
                )
                result.fold(
                    onSuccess = {
                        logger.i(TAG, "Комментарий id=$commentId успешно удален")
                        refreshEventContent(currentState.event.id)
                    },
                    onFailure = { exception ->
                        logger.e(
                            TAG,
                            "Ошибка удаления комментария: ${exception.message}",
                            exception
                        )
                        handleError(exception, "удалении комментария")
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logger.e(TAG, "Исключение при удалении комментария: ${e.message}", e)
                handleError(e, "удалении комментария")
            } finally {
                pendingDeleteCommentId = null
                _isRefreshing.value = false
            }
        }
    }

    override fun onCommentDeleteDismiss() {
        logger.d(TAG, "Отмена удаления комментария")
        pendingDeleteCommentId = null
    }

    override fun onPhotoDeleted(photoId: Long) {
        val currentState = _uiState.value
        if (currentState is EventDetailUIState.Content) {
            logger.d(TAG, "Локальное удаление фото id=$photoId из UI с перенумерацией")
            val updatedPhotos = currentState.event.photos.removePhotoById(photoId)
            _uiState.value = currentState.copy(
                event = currentState.event.copy(photos = updatedPhotos)
            )
        }
    }

    // ==================== Обработка ошибок ====================

    private fun handleError(exception: Throwable, operation: String) {
        when (exception) {
            is IOException -> {
                userNotifier.handleError(
                    AppError.Network(
                        message = "Не удалось выполнить операцию. Проверьте подключение к интернету.",
                        throwable = exception
                    )
                )
            }

            is HttpException -> {
                userNotifier.handleError(
                    AppError.Server(
                        code = exception.code(),
                        message = "Ошибка сервера при $operation: ${exception.message()}"
                    )
                )
            }

            else -> {
                userNotifier.handleError(
                    AppError.Generic(
                        message = "Ошибка при $operation: ${exception.message}",
                        throwable = exception
                    )
                )
            }
        }
    }

}
