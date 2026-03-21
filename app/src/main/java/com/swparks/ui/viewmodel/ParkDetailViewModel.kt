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
import com.swparks.ui.state.ParkDetailUIState
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

@Suppress("TooManyFunctions", "TooGenericExceptionCaught", "InstanceOfCheckForException")
class ParkDetailViewModel(
    private val swRepository: SWRepository,
    private val countriesRepository: CountriesRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val savedStateHandle: SavedStateHandle,
    private val userNotifier: UserNotifier,
    private val logger: Logger,
) : ViewModel(), IParkDetailViewModel {

    companion object {
        private const val TAG = "ParkDetailViewModel"
        private const val PARK_ID_KEY = "parkId"
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
                ParkDetailViewModel(
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

    private enum class ManageParkAction(val logName: String) {
        DeleteParkClick("delete_park_click"),
        DeleteParkConfirm("delete_park_confirm"),
        DeletePhotoClick("delete_photo_click"),
        DeletePhotoConfirm("delete_photo_confirm")
    }

    private val _uiState = MutableStateFlow<ParkDetailUIState>(ParkDetailUIState.InitialLoading)
    override val uiState: StateFlow<ParkDetailUIState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    override val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isAuthorized = MutableStateFlow(false)
    override val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    private val _isParkAuthor = MutableStateFlow(false)
    override val isParkAuthor: StateFlow<Boolean> = _isParkAuthor.asStateFlow()

    private val _currentUserId = MutableStateFlow<Long?>(null)
    override val currentUserId: StateFlow<Long?> = _currentUserId.asStateFlow()

    override val mapUriSet: MapUriSet?
        get() {
            val currentState = _uiState.value
            if (currentState !is ParkDetailUIState.Content) {
                return null
            }

            val latitude = currentState.park.latitude.toDoubleOrNull()
            val longitude = currentState.park.longitude.toDoubleOrNull()

            return if (latitude != null && longitude != null) {
                MapUriSet(
                    latitude = latitude,
                    longitude = longitude
                )
            } else {
                null
            }
        }

    private val _events = MutableSharedFlow<ParkDetailEvent>()
    override val events: SharedFlow<ParkDetailEvent> = _events.asSharedFlow()

    private var pendingDeletePhoto: Photo? = null
    private var pendingDeleteCommentId: Long? = null

    init {
        logger.d(TAG, "Инициализация ParkDetailViewModel")
        observeAuthorization()
        loadPark()
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
                updateIsParkAuthor()
                logger.d(TAG, "Текущий userId: $userId")
            }
        }
    }

    private fun updateIsParkAuthor() {
        val currentState = _uiState.value
        if (currentState is ParkDetailUIState.Content) {
            val parkAuthorId = currentState.park.author?.id
            _isParkAuthor.value =
                _currentUserId.value != null && _currentUserId.value == parkAuthorId
            logger.d(
                TAG,
                "isParkAuthor: ${_isParkAuthor.value} " +
                    "(currentUserId=${_currentUserId.value}, authorId=$parkAuthorId)"
            )
        }
    }

    private fun getParkId(): Long? {
        return savedStateHandle.get<Long>(PARK_ID_KEY)
            ?: savedStateHandle.get<Int>(PARK_ID_KEY)?.toLong()
            ?: savedStateHandle.get<String>(PARK_ID_KEY)?.toLongOrNull()
    }

    private fun canManagePark(action: ManageParkAction): Boolean {
        val canManage = _isAuthorized.value && _isParkAuthor.value
        if (!canManage) {
            if (!_isAuthorized.value) {
                logger.w(TAG, "Отклонено действие '${action.logName}': пользователь не авторизован")
            } else {
                logger.w(
                    TAG,
                    "Отклонено действие '${action.logName}': пользователь не автор площадки"
                )
            }
        }
        return canManage
    }

    private fun loadPark() {
        val parkId = getParkId()

        if (parkId == null) {
            logger.e(TAG, "parkId отсутствует в SavedStateHandle")
            _uiState.value = ParkDetailUIState.Error("Неверный идентификатор площадки")
            return
        }

        logger.d(TAG, "Загрузка площадки id=$parkId")
        viewModelScope.launch {
            try {
                val result = swRepository.getPark(parkId)
                result.fold(
                    onSuccess = { park ->
                        logger.i(TAG, "Площадка загружена: ${park.name}")
                        val address = buildAddress(park.countryID, park.cityID, park.address)
                        val authorAddress = buildAuthorAddress(park.author)
                        _uiState.value = ParkDetailUIState.Content(
                            park = park,
                            address = address,
                            authorAddress = authorAddress
                        )
                        updateIsParkAuthor()
                    },
                    onFailure = { exception ->
                        logger.e(
                            TAG,
                            "Ошибка загрузки площадки: ${exception.message}",
                            exception
                        )
                        handleError(exception, "загрузке площадки")
                        _uiState.value = ParkDetailUIState.Error(exception.message)
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logger.e(TAG, "Исключение при загрузке площадки: ${e.message}", e)
                handleError(e, "загрузке площадки")
                _uiState.value = ParkDetailUIState.Error(e.message)
            }
        }
    }

    private suspend fun buildAddress(countryId: Int, cityId: Int, serverAddress: String): String {
        val address = buildAddressNullable(countryId, cityId, serverAddress)
        return address ?: "$countryId, $cityId"
    }

    private suspend fun buildAuthorAddress(user: User?): String {
        return buildAddressNullable(user?.countryID, user?.cityID, null) ?: ""
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
        logger.d(TAG, "Pull-to-refresh: обновление площадки")
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val parkId = getParkId()
                if (parkId == null) {
                    logger.e(TAG, "parkId отсутствует при refresh")
                    return@launch
                }
                refreshParkContent(parkId)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun refreshParkContent(parkId: Long) {
        val result = swRepository.getPark(parkId)
        result.fold(
            onSuccess = { park ->
                logger.i(TAG, "Площадка обновлена: ${park.name}")
                val address = buildAddress(park.countryID, park.cityID, park.address)
                val authorAddress = buildAuthorAddress(park.author)
                _uiState.value = ParkDetailUIState.Content(
                    park = park,
                    address = address,
                    authorAddress = authorAddress
                )
                updateIsParkAuthor()
            },
            onFailure = { exception ->
                logger.e(
                    TAG,
                    "Ошибка обновления площадки: ${exception.message}",
                    exception
                )
                handleError(exception, "обновлении площадки")
            }
        )
    }

    override fun onEditClick() {
        val currentState = _uiState.value
        if (currentState is ParkDetailUIState.Content) {
            logger.d(TAG, "Нажата кнопка редактирования площадки id=${currentState.park.id}")
            viewModelScope.launch {
                _events.emit(ParkDetailEvent.NavigateToEditPark(currentState.park))
            }
        }
    }

    override fun onDeleteClick() {
        val currentState = _uiState.value
        if (
            currentState is ParkDetailUIState.Content &&
            canManagePark(ManageParkAction.DeleteParkClick)
        ) {
            logger.d(TAG, "Нажата кнопка удаления площадки id=${currentState.park.id}")
            viewModelScope.launch {
                _events.emit(ParkDetailEvent.ShowDeleteConfirmDialog)
            }
        }
    }

    override fun onDeleteConfirm() {
        val currentState = _uiState.value
        if (currentState !is ParkDetailUIState.Content) return
        if (!canManagePark(ManageParkAction.DeleteParkConfirm)) return

        val parkId = currentState.park.id
        logger.d(TAG, "Подтверждение удаления площадки id=$parkId")

        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val result = swRepository.deletePark(parkId)
                result.fold(
                    onSuccess = {
                        logger.i(TAG, "Площадка id=$parkId успешно удалена")
                        viewModelScope.launch {
                            _events.emit(ParkDetailEvent.ParkDeleted(parkId))
                        }
                    },
                    onFailure = { exception ->
                        logger.e(
                            TAG,
                            "Ошибка удаления площадки: ${exception.message}",
                            exception
                        )
                        handleError(exception, "удалении площадки")
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logger.e(TAG, "Исключение при удалении площадки: ${e.message}", e)
                handleError(e, "удалении площадки")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    override fun onDeleteDismiss() {
        logger.d(TAG, "Отмена удаления площадки")
    }

    override fun onPhotoClick(photo: Photo) {
        logger.d(TAG, "Нажата фотография id=${photo.id}")
        val currentState = _uiState.value
        if (currentState is ParkDetailUIState.Content) {
            viewModelScope.launch {
                _events.emit(
                    ParkDetailEvent.NavigateToPhotoDetail(
                        photo = photo,
                        parkId = currentState.park.id,
                        parkTitle = currentState.park.name,
                        isParkAuthor = _isParkAuthor.value
                    )
                )
            }
        }
    }

    override fun onPhotoDeleteClick(photo: Photo) {
        if (!canManagePark(ManageParkAction.DeletePhotoClick)) return
        logger.d(TAG, "Нажата кнопка удаления фото id=${photo.id}")
        pendingDeletePhoto = photo
        viewModelScope.launch {
            _events.emit(ParkDetailEvent.ShowDeletePhotoConfirmDialog(photo))
        }
    }

    override fun onPhotoDeleteConfirm() {
        val photo = pendingDeletePhoto ?: return
        val currentState = _uiState.value
        if (currentState is ParkDetailUIState.Content) {
            if (!canManagePark(ManageParkAction.DeletePhotoConfirm)) {
                pendingDeletePhoto = null
                return
            }

            val parkId = currentState.park.id
            val photoId = photo.id
            logger.d(TAG, "Подтверждение удаления фото id=$photoId площадки id=$parkId")

            viewModelScope.launch {
                try {
                    val result = swRepository.deleteParkPhoto(parkId, photoId)
                    result.fold(
                        onSuccess = {
                            logger.i(TAG, "Фото id=$photoId успешно удалено")
                            val updatedPhotos =
                                currentState.park.photos.orEmpty().removePhotoById(photoId)
                            logger.d(
                                TAG,
                                "Перенумерация фото: было ${currentState.park.photos?.size ?: 0}, " +
                                    "стало ${updatedPhotos.size}"
                            )
                            val updatedPark = currentState.park.copy(photos = updatedPhotos)
                            _uiState.value = currentState.copy(park = updatedPark)
                            viewModelScope.launch {
                                _events.emit(ParkDetailEvent.PhotoDeleted(photoId))
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

    override fun onShareClick() {
        val currentState = _uiState.value
        if (currentState is ParkDetailUIState.Content) {
            logger.d(TAG, "Нажата кнопка 'Поделиться' для площадки id=${currentState.park.id}")
        }
    }

    override fun onTrainHereToggle() {
        val currentState = _uiState.value
        if (currentState !is ParkDetailUIState.Content) return

        val currentValue = currentState.park.trainHere ?: false
        val newValue = !currentValue
        val parkId = currentState.park.id

        logger.d(
            TAG,
            "Toggle 'Тренируюсь здесь' для площадки id=$parkId: $currentValue -> $newValue"
        )

        val optimisticPark = currentState.park.copy(trainHere = newValue)
        _uiState.value = currentState.copy(park = optimisticPark)
        _isRefreshing.value = true

        viewModelScope.launch {
            try {
                val result = swRepository.changeTrainHereStatus(newValue, parkId)
                _isRefreshing.value = false

                if (result.isSuccess) {
                    val currentUser = swRepository.getCurrentUserFlow().first()
                    val updatedUsers = if (newValue && currentUser != null) {
                        (currentState.park.trainingUsers.orEmpty() + currentUser)
                            .distinctBy { it.id }
                    } else {
                        currentState.park.trainingUsers.orEmpty()
                            .filterNot { it.id == _currentUserId.value }
                    }
                    val finalPark = optimisticPark.copy(
                        trainingUsers = updatedUsers,
                        trainingUsersCount = updatedUsers.size
                    )
                    _uiState.value = currentState.copy(park = finalPark)
                    logger.i(
                        TAG,
                        "Статус тренировки на площадке id=$parkId обновлен: trainHere=$newValue"
                    )
                } else {
                    _uiState.value = currentState
                    val error = result.exceptionOrNull() ?: Exception("Unknown error")
                    handleError(error, "изменении статуса тренировки")
                    logger.e(TAG, "Ошибка изменения статуса тренировки: ${error.message}")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _isRefreshing.value = false
                _uiState.value = currentState
                handleError(e, "изменении статуса тренировки")
                logger.e(TAG, "Исключение при изменении статуса тренировки: ${e.message}", e)
            }
        }
    }

    override fun onTraineesCountClick() {
        val currentState = _uiState.value
        if (currentState is ParkDetailUIState.Content) {
            logger.d(
                TAG,
                "Нажато количество тренирующихся (${currentState.park.trainingUsersCount}) " +
                    "для площадки id=${currentState.park.id}"
            )
            viewModelScope.launch {
                _events.emit(
                    ParkDetailEvent.NavigateToTrainees(
                        parkId = currentState.park.id,
                        users = currentState.park.trainingUsers.orEmpty()
                    )
                )
            }
        }
    }

    override fun onOpenMapClick() {
        val currentState = _uiState.value
        if (currentState is ParkDetailUIState.Content) {
            logger.d(
                TAG,
                "Нажата кнопка 'Открыть на карте' для площадки id=${currentState.park.id}"
            )
            viewModelScope.launch {
                _events.emit(ParkDetailEvent.OpenMap)
            }
        }
    }

    override fun onRouteClick() {
        val currentState = _uiState.value
        if (currentState is ParkDetailUIState.Content) {
            logger.d(
                TAG,
                "Нажата кнопка 'Построить маршрут' для площадки id=${currentState.park.id}"
            )
            viewModelScope.launch {
                _events.emit(ParkDetailEvent.BuildRoute)
            }
        }
    }

    override fun onCreateEventClick() {
        val currentState = _uiState.value
        if (currentState is ParkDetailUIState.Content) {
            logger.d(
                TAG,
                "Нажата кнопка 'Создать мероприятие' для площадки id=${currentState.park.id}"
            )
            viewModelScope.launch {
                _events.emit(
                    ParkDetailEvent.NavigateToCreateEvent(
                        parkId = currentState.park.id,
                        parkName = currentState.park.name
                    )
                )
            }
        }
    }

    override fun onAddCommentClick() {
        val currentState = _uiState.value
        if (currentState is ParkDetailUIState.Content) {
            logger.d(
                TAG,
                "Нажата кнопка 'Добавить комментарий' для площадки id=${currentState.park.id}"
            )
            viewModelScope.launch {
                _events.emit(
                    ParkDetailEvent.OpenCommentTextEntry(
                        mode = TextEntryMode.NewForPark(currentState.park.id)
                    )
                )
            }
        }
    }

    override fun onCommentActionClick(commentId: Long, action: CommentAction) {
        val currentState = _uiState.value
        if (currentState !is ParkDetailUIState.Content) return

        logger.d(TAG, "Нажато действие $action для комментария id=$commentId")

        val comment = currentState.park.comments.orEmpty().firstOrNull { it.id == commentId }
        if (comment == null) {
            logger.w(TAG, "Комментарий не найден: id=$commentId")
            return
        }

        when (action) {
            CommentAction.EDIT -> handleEditComment(comment, currentState.park.id)
            CommentAction.REPORT -> handleReportComment(comment, currentState.park.name)
            CommentAction.DELETE -> handleDeleteComment(comment, commentId)
        }
    }

    private fun handleEditComment(comment: Comment, parkId: Long) {
        viewModelScope.launch {
            _events.emit(
                ParkDetailEvent.OpenCommentTextEntry(
                    mode = TextEntryMode.EditPark(
                        editInfo = EditInfo(
                            parentObjectId = parkId,
                            entryId = comment.id,
                            oldEntry = comment.parsedBody.orEmpty()
                        )
                    )
                )
            )
        }
    }

    private fun handleReportComment(comment: Comment, parkName: String) {
        val complaintAuthor = comment.user?.name?.ifBlank { UNKNOWN_COMMENT_AUTHOR }
            ?: UNKNOWN_COMMENT_AUTHOR
        val complaintText = comment.parsedBody ?: comment.body.orEmpty()

        viewModelScope.launch {
            _events.emit(
                ParkDetailEvent.SendCommentComplaint(
                    complaint = Complaint.ParkComment(
                        parkTitle = parkName,
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
            _events.emit(ParkDetailEvent.ShowDeleteCommentConfirmDialog)
        }
    }

    override fun onCommentDeleteConfirm() {
        val currentState = _uiState.value
        if (currentState !is ParkDetailUIState.Content) return
        val commentId = pendingDeleteCommentId ?: return

        logger.d(TAG, "Подтверждение удаления комментария id=$commentId")
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val result = swRepository.deleteComment(
                    option = com.swparks.ui.model.TextEntryOption.Park(currentState.park.id),
                    commentId = commentId
                )
                result.fold(
                    onSuccess = {
                        logger.i(TAG, "Комментарий id=$commentId успешно удален")
                        refreshParkContent(currentState.park.id)
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
        if (currentState is ParkDetailUIState.Content) {
            logger.d(TAG, "Локальное удаление фото id=$photoId из UI с перенумерацией")
            val updatedPhotos = currentState.park.photos.orEmpty().removePhotoById(photoId)
            _uiState.value = currentState.copy(
                park = currentState.park.copy(photos = updatedPhotos)
            )
        }
    }

    override fun onParkUpdated(parkId: Long) {
        logger.d(TAG, "Обновление площадки после редактирования: id=$parkId")
        loadPark()
    }

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
