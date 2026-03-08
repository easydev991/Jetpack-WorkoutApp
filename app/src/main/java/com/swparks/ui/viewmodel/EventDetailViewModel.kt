package com.swparks.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.data.UserPreferencesRepository
import com.swparks.data.model.Photo
import com.swparks.data.repository.SWRepository
import com.swparks.domain.repository.CountriesRepository
import com.swparks.ui.ds.CommentAction
import com.swparks.ui.state.EventDetailUIState
import com.swparks.util.AppError
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
     * Открыть карту по координатам.
     *
     * @param latitude Широта
     * @param longitude Долгота
     */
    data class OpenMap(val latitude: String, val longitude: String) : EventDetailEvent()

    /**
     * Построить маршрут до мероприятия.
     *
     * @param latitude Широта
     * @param longitude Долгота
     */
    data class BuildRoute(val latitude: String, val longitude: String) : EventDetailEvent()
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
@Suppress("TooManyFunctions", "TooGenericExceptionCaught")
class EventDetailViewModel(
    private val swRepository: SWRepository,
    private val countriesRepository: CountriesRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val savedStateHandle: SavedStateHandle,
    private val userNotifier: UserNotifier,
    private val logger: Logger,
) : ViewModel(), IEventDetailViewModel {

    private companion object {
        const val TAG = "EventDetailViewModel"
        const val EVENT_ID_KEY = "eventId"
        const val NO_CALENDAR_APP_MESSAGE = "Нет календарного приложения на устройстве."
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

    // События UI
    private val _events = MutableSharedFlow<EventDetailEvent>()
    override val events: SharedFlow<EventDetailEvent> = _events.asSharedFlow()

    // Текущий пользователь
    private var currentUserId: Long? = null

    // Фото для удаления (временно хранится до подтверждения)
    private var pendingDeletePhoto: Photo? = null

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
                currentUserId = userId
                updateIsEventAuthor()
                logger.d(TAG, "Текущий userId: $userId")
            }
        }
    }

    private fun updateIsEventAuthor() {
        val currentState = _uiState.value
        if (currentState is EventDetailUIState.Content) {
            val eventAuthorId = currentState.event.author.id
            _isEventAuthor.value = currentUserId != null && currentUserId == eventAuthorId
            logger.d(
                TAG,
                "isEventAuthor: ${_isEventAuthor.value} (currentUserId=$currentUserId, authorId=$eventAuthorId)"
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
                        _uiState.value = EventDetailUIState.Content(
                            event = event,
                            address = address
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
                logger.e(TAG, "Исключение при загрузке мероприятия: ${e.message}", e)
                handleError(e, "загрузке мероприятия")
                _uiState.value = EventDetailUIState.Error(e.message)
            }
        }
    }

    private suspend fun buildAddress(countryId: Int, cityId: Int, serverAddress: String?): String {
        return try {
            val country = countriesRepository.getCountryById(countryId.toString())
            val city = countriesRepository.getCityById(cityId.toString())

            val address = when {
                country != null && city != null -> "${country.name}, ${city.name}"
                country != null -> country.name
                city != null -> city.name
                !serverAddress.isNullOrBlank() -> serverAddress
                else -> "$countryId, $cityId"
            }
            address
        } catch (e: Exception) {
            logger.e(TAG, "Ошибка загрузки адреса ($countryId, $cityId): ${e.message}")
            serverAddress?.takeIf { it.isNotBlank() } ?: "$countryId, $cityId"
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

                val result = swRepository.getEvent(eventId)
                result.fold(
                    onSuccess = { event ->
                        logger.i(TAG, "Мероприятие обновлено: ${event.title}")
                        val address = buildAddress(event.countryID, event.cityID, event.address)
                        _uiState.value = EventDetailUIState.Content(
                            event = event,
                            address = address
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
            } finally {
                _isRefreshing.value = false
            }
        }
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
                logger.e(TAG, "Исключение при удалении мероприятия: ${e.message}", e)
                handleError(e, "удалении мероприятия")
            }
        }
    }

    override fun onDeleteDismiss() {
        logger.d(TAG, "Отмена удаления мероприятия")
    }

    // ==================== Действия с фото ====================

    override fun onPhotoClick(photo: Photo) {
        logger.d(TAG, "Нажата фотография id=${photo.id}")
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
                            // Обновляем данные мероприятия
                            refresh()
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
        if (currentState is EventDetailUIState.Content) {
            logger.d(
                TAG,
                "Нажат toggle 'Пойду' для мероприятия id=${currentState.event.id}, " +
                    "текущее состояние: ${currentState.event.trainHere}"
            )
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
        }
    }

    override fun onAuthorClick(authorId: Long) {
        logger.d(TAG, "Нажат автор мероприятия authorId=$authorId")
    }

    override fun onCommentAuthorClick(authorId: Long) {
        logger.d(TAG, "Нажат автор комментария authorId=$authorId")
    }

    override fun onOpenMapClick() {
        val currentState = _uiState.value
        if (currentState is EventDetailUIState.Content) {
            val event = currentState.event
            logger.d(TAG, "Нажата кнопка 'Открыть на карте' для мероприятия id=${event.id}")
            viewModelScope.launch {
                _events.emit(
                    EventDetailEvent.OpenMap(
                        latitude = event.latitude,
                        longitude = event.longitude
                    )
                )
            }
        }
    }

    override fun onRouteClick() {
        val currentState = _uiState.value
        if (currentState is EventDetailUIState.Content) {
            val event = currentState.event
            logger.d(TAG, "Нажата кнопка 'Построить маршрут' для мероприятия id=${event.id}")
            viewModelScope.launch {
                _events.emit(
                    EventDetailEvent.BuildRoute(
                        latitude = event.latitude,
                        longitude = event.longitude
                    )
                )
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
        }
    }

    override fun onCommentActionClick(commentId: Long, action: CommentAction) {
        logger.d(TAG, "Нажато действие $action для комментария id=$commentId")
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
