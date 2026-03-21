package com.swparks.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.swparks.data.UserPreferencesRepository
import com.swparks.data.model.Photo
import com.swparks.data.repository.SWRepository
import com.swparks.ui.state.PhotoDetailAction
import com.swparks.ui.state.PhotoDetailConfig
import com.swparks.ui.state.PhotoDetailEvent
import com.swparks.ui.state.PhotoDetailUIState
import com.swparks.ui.state.PhotoOwner
import com.swparks.util.AppError
import com.swparks.util.Complaint
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PhotoDetailViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val swRepository: SWRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val logger: Logger,
    private val userNotifier: UserNotifier,
) : ViewModel(), IPhotoDetailViewModel {

    companion object {
        private const val TAG = "PhotoDetailViewModel"
        private const val PHOTO_ID_KEY = "photoId"
        private const val PARENT_ID_KEY = "parentId"
        private const val PARENT_TITLE_KEY = "parentTitle"
        private const val IS_AUTHOR_KEY = "isAuthor"
        private const val PHOTO_URL_KEY = "photoUrl"
        private const val OWNER_TYPE_KEY = "ownerType"

        @Deprecated("Use factoryWithConfig instead", ReplaceWith("factoryWithConfig"))
        fun factory(
            swRepository: SWRepository,
            userPreferencesRepository: UserPreferencesRepository,
            logger: Logger,
            userNotifier: UserNotifier,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle()
                PhotoDetailViewModel(
                    savedStateHandle = savedStateHandle,
                    swRepository = swRepository,
                    userPreferencesRepository = userPreferencesRepository,
                    logger = logger,
                    userNotifier = userNotifier
                )
            }
        }

        fun factoryWithConfig(
            config: PhotoDetailConfig,
            swRepository: SWRepository,
            userPreferencesRepository: UserPreferencesRepository,
            logger: Logger,
            userNotifier: UserNotifier,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle()
                savedStateHandle[PHOTO_ID_KEY] = config.photoId
                savedStateHandle[PARENT_ID_KEY] = config.parentId
                savedStateHandle[PARENT_TITLE_KEY] = config.parentTitle
                savedStateHandle[IS_AUTHOR_KEY] = config.isAuthor
                savedStateHandle[PHOTO_URL_KEY] = config.photoUrl
                savedStateHandle[OWNER_TYPE_KEY] = config.ownerType.name
                PhotoDetailViewModel(
                    savedStateHandle = savedStateHandle,
                    swRepository = swRepository,
                    userPreferencesRepository = userPreferencesRepository,
                    logger = logger,
                    userNotifier = userNotifier
                )
            }
        }
    }

    private val _uiState = MutableStateFlow<PhotoDetailUIState>(
        PhotoDetailUIState.Error("Фото не найдено")
    )
    override val uiState: StateFlow<PhotoDetailUIState> = _uiState.asStateFlow()

    private val _events = Channel<PhotoDetailEvent>(Channel.BUFFERED)
    override val events: Channel<PhotoDetailEvent> = _events

    private val _isAuthorized = MutableStateFlow(false)
    override val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    init {
        logger.d(TAG, "Инициализация PhotoDetailViewModel")
        loadPhotoData()
        observeAuthorization()
    }

    private fun loadPhotoData() {
        val photoId = getPhotoId()
        val parentId = getParentId()
        val parentTitle = getParentTitle()
        val isAuthor = getIsAuthor()
        val photoUrl = getPhotoUrl()

        logger.d(
            TAG,
            "Загрузка данных фото: photoId=$photoId, parentId=$parentId, " +
                "parentTitle=$parentTitle, isAuthor=$isAuthor, photoUrl=$photoUrl"
        )

        if (!validatePhotoParams(photoId, parentId, parentTitle, photoUrl)) {
            logger.e(TAG, "Отсутствуют обязательные параметры для отображения фото")
            _uiState.value = PhotoDetailUIState.Error("Не удалось загрузить фотографию")
            return
        }

        val photo = Photo(id = photoId!!, photo = photoUrl!!)
        _uiState.value = PhotoDetailUIState.Content(
            photo = photo,
            parentTitle = parentTitle!!,
            isAuthor = isAuthor ?: false,
            isLoading = false
        )
        logger.i(TAG, "Фото загружено: id=$photoId")
    }

    private fun validatePhotoParams(
        photoId: Long?,
        parentId: Long?,
        parentTitle: String?,
        photoUrl: String?
    ): Boolean {
        return photoId != null && parentId != null && parentTitle != null && photoUrl != null
    }

    private fun observeAuthorization() {
        viewModelScope.launch {
            userPreferencesRepository.isAuthorized.collect { isAuth ->
                _isAuthorized.value = isAuth
                logger.d(TAG, "Состояние авторизации: $isAuth")
            }
        }
    }

    private fun getPhotoId(): Long? {
        return savedStateHandle.get<Long>(PHOTO_ID_KEY)
            ?: savedStateHandle.get<Int>(PHOTO_ID_KEY)?.toLong()
            ?: savedStateHandle.get<String>(PHOTO_ID_KEY)?.toLongOrNull()
    }

    private fun getParentId(): Long? {
        return savedStateHandle.get<Long>(PARENT_ID_KEY)
            ?: savedStateHandle.get<Int>(PARENT_ID_KEY)?.toLong()
            ?: savedStateHandle.get<String>(PARENT_ID_KEY)?.toLongOrNull()
    }

    private fun getParentTitle(): String? {
        return savedStateHandle.get<String>(PARENT_TITLE_KEY)
    }

    private fun getIsAuthor(): Boolean? {
        return savedStateHandle.get<Boolean>(IS_AUTHOR_KEY)
    }

    private fun getPhotoUrl(): String? {
        return savedStateHandle.get<String>(PHOTO_URL_KEY)
    }

    private fun getOwnerType(): PhotoOwner {
        val ownerTypeName = savedStateHandle.get<String>(OWNER_TYPE_KEY)
        return when (ownerTypeName) {
            PhotoOwner.Park.name -> PhotoOwner.Park
            else -> PhotoOwner.Event
        }
    }

    override fun onAction(action: PhotoDetailAction) {
        when (action) {
            PhotoDetailAction.Close -> handleClose()
            PhotoDetailAction.DeleteClick -> handleDeleteClick()
            PhotoDetailAction.DeleteConfirm -> handleDeleteConfirm()
            PhotoDetailAction.DeleteDismiss -> handleDeleteDismiss()
            PhotoDetailAction.Report -> handleReport()
        }
    }

    private fun handleClose() {
        logger.d(TAG, "Закрытие экрана фото")
        viewModelScope.launch {
            _events.send(PhotoDetailEvent.CloseScreen)
        }
    }

    private fun handleDeleteClick() {
        val currentState = _uiState.value
        if (currentState !is PhotoDetailUIState.Content) {
            logger.w(TAG, "Невозможно удалить фото: неверное состояние UI")
            return
        }

        if (!currentState.isAuthor) {
            logger.w(TAG, "Отклонено удаление фото: пользователь не автор")
            return
        }

        logger.d(TAG, "Запрос подтверждения удаления фото id=${currentState.photo.id}")
        viewModelScope.launch {
            _events.send(PhotoDetailEvent.ShowDeleteConfirmDialog)
        }
    }

    private fun handleDeleteConfirm() {
        val currentState = _uiState.value
        if (currentState !is PhotoDetailUIState.Content) {
            logger.w(TAG, "Невозможно удалить фото: неверное состояние UI")
            return
        }

        val parentId = getParentId()
        val canDelete = currentState.isAuthor && parentId != null
        if (!canDelete) {
            val reason = if (!currentState.isAuthor) {
                "пользователь не автор"
            } else {
                "parentId отсутствует"
            }
            logger.w(TAG, "Отклонено удаление фото: $reason")
            return
        }

        performDeletePhoto(currentState, parentId)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun performDeletePhoto(currentState: PhotoDetailUIState.Content, parentId: Long) {
        val photoId = currentState.photo.id
        val ownerType = getOwnerType()
        val entityName = if (ownerType == PhotoOwner.Park) "площадки" else "мероприятия"
        logger.d(TAG, "Подтверждение удаления фото id=$photoId $entityName id=$parentId")
        _uiState.value = currentState.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val result = when (ownerType) {
                    PhotoOwner.Park -> swRepository.deleteParkPhoto(parentId, photoId)
                    PhotoOwner.Event -> swRepository.deleteEventPhoto(parentId, photoId)
                }
                result.fold(
                    onSuccess = {
                        logger.i(TAG, "Фото id=$photoId успешно удалено")
                        val contentState = _uiState.value as? PhotoDetailUIState.Content
                        if (contentState != null) {
                            _uiState.value = contentState.copy(isLoading = false)
                        }
                        _events.send(PhotoDetailEvent.PhotoDeleted(photoId))
                    },
                    onFailure = { exception ->
                        logger.e(TAG, "Ошибка удаления фото: ${exception.message}", exception)
                        val contentState = _uiState.value as? PhotoDetailUIState.Content
                        if (contentState != null) {
                            _uiState.value = contentState.copy(isLoading = false)
                        }
                        userNotifier.handleError(
                            AppError.Generic(
                                message = exception.message ?: "Ошибка удаления фото",
                                throwable = exception
                            )
                        )
                    }
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(TAG, "Исключение при удалении фото: ${e.message}", e)
                val contentState = _uiState.value as? PhotoDetailUIState.Content
                if (contentState != null) {
                    _uiState.value = contentState.copy(isLoading = false)
                }
                userNotifier.handleError(
                    AppError.Generic(
                        message = e.message ?: "Ошибка удаления фото",
                        throwable = e
                    )
                )
            }
        }
    }

    private fun handleDeleteDismiss() {
        logger.d(TAG, "Отмена удаления фото")
    }

    private fun handleReport() {
        val currentState = _uiState.value
        if (currentState !is PhotoDetailUIState.Content) {
            logger.w(TAG, "Невозможно отправить жалобу: неверное состояние UI")
            return
        }

        if (!_isAuthorized.value || currentState.isAuthor) {
            val reason = if (!_isAuthorized.value) {
                "пользователь не авторизован"
            } else {
                "автор не может жаловаться на свое фото"
            }
            logger.w(TAG, "Отклонена жалоба: $reason")
            return
        }

        val parentTitle = currentState.parentTitle
        val ownerType = getOwnerType()
        val entityName = if (ownerType == PhotoOwner.Park) "площадки" else "мероприятия"
        logger.d(
            TAG,
            "Отправка жалобы на фото id=${currentState.photo.id} $entityName: $parentTitle"
        )

        val complaint: Complaint = when (ownerType) {
            PhotoOwner.Park -> Complaint.ParkPhoto(parkTitle = parentTitle)
            PhotoOwner.Event -> Complaint.EventPhoto(eventTitle = parentTitle)
        }
        viewModelScope.launch {
            _events.send(PhotoDetailEvent.SendPhotoComplaint(complaint))
        }
    }
}
