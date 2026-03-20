package com.swparks.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.swparks.domain.provider.AvatarHelper
import com.swparks.domain.usecase.ICreateEventUseCase
import com.swparks.domain.usecase.IEditEventUseCase
import com.swparks.ui.model.EventForm
import com.swparks.ui.model.EventFormMode
import com.swparks.ui.state.EventFormEvent
import com.swparks.ui.state.EventFormUiState
import com.swparks.util.AppError
import com.swparks.util.ImageUtils
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Suppress("TooGenericExceptionCaught")
class EventFormViewModel(
    private val mode: EventFormMode,
    private val createEventUseCase: ICreateEventUseCase,
    private val editEventUseCase: IEditEventUseCase,
    private val avatarHelper: AvatarHelper,
    private val logger: Logger,
    private val userNotifier: UserNotifier
) : ViewModel(), IEventFormViewModel {

    companion object {
        private const val TAG = "EventFormViewModel"
        private val serverDateTimeFormatter: DateTimeFormatter =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME

        fun factory(
            mode: EventFormMode,
            appContainer: com.swparks.data.AppContainer
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(EventFormViewModel::class.java)) {
                    "Неизвестный класс ViewModel: ${modelClass.name}, " +
                        "ожидается: ${EventFormViewModel::class.java.name}"
                }
                val viewModel = appContainer.eventFormViewModelFactory(mode)
                return checkNotNull(modelClass.cast(viewModel)) {
                    "Не удалось привести ${EventFormViewModel::class.java.name} к ${modelClass.name}"
                }
            }
        }
    }

    private val _uiState = MutableStateFlow(createInitialState())
    override val uiState: StateFlow<EventFormUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EventFormEvent>()
    override val events: SharedFlow<EventFormEvent> = _events.asSharedFlow()

    private fun createInitialState(): EventFormUiState {
        val now = LocalDateTime.now()
            .withSecond(0)
            .withNano(0)
            .format(serverDateTimeFormatter)

        return when (mode) {
            is EventFormMode.RegularCreate -> {
                val form = EventForm(date = now)
                EventFormUiState(
                    mode = mode,
                    form = form,
                    initialForm = form,
                    isLoading = false
                )
            }

            is EventFormMode.CreateForSelected -> {
                val form = EventForm(
                    date = now,
                    parkId = mode.parkId,
                    parkName = mode.parkName
                )
                EventFormUiState(
                    mode = mode,
                    form = form,
                    initialForm = form,
                    isLoading = false
                )
            }

            is EventFormMode.EditExisting -> {
                val event = mode.event
                val form = EventForm(
                    title = event.title,
                    description = event.description,
                    date = normalizeDateForServer(event.beginDate),
                    parkId = event.parkID ?: 0L,
                    parkName = "№${event.parkID ?: 0L}",
                    photosCount = event.photos.size
                )
                EventFormUiState(
                    mode = mode,
                    form = form,
                    initialForm = form,
                    isLoading = false
                )
            }
        }
    }

    private fun normalizeDateForServer(dateString: String): String {
        return try {
            val localDateTime = when {
                dateString.contains('Z') || dateString.contains('+') -> {
                    OffsetDateTime.parse(dateString)
                        .atZoneSameInstant(ZoneId.systemDefault())
                        .toLocalDateTime()
                }

                dateString.contains('T') -> {
                    LocalDateTime.parse(dateString, serverDateTimeFormatter)
                }

                else -> return dateString
            }
            localDateTime
                .withNano(0)
                .format(serverDateTimeFormatter)
        } catch (_: Exception) {
            val regex = Regex("""^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})""")
            val match = regex.find(dateString)
            match?.groupValues?.get(1) ?: dateString
        }
    }

    override fun onTitleChange(value: String) {
        _uiState.update { it.copy(form = it.form.copy(title = value)) }
    }

    override fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(form = it.form.copy(description = value)) }
    }

    override fun onDateChange(timestamp: Long) {
        val localDate = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val currentDateTime = parseFormDateTimeOrNow(_uiState.value.form.date)
        val formatted = LocalDateTime.of(localDate, currentDateTime.toLocalTime())
            .withNano(0)
            .format(serverDateTimeFormatter)

        _uiState.update { it.copy(form = it.form.copy(date = formatted)) }
        logger.d(TAG, "Дата изменена: $formatted")
    }

    override fun onTimeChange(hour: Int, minute: Int) {
        val currentDateTime = parseFormDateTimeOrNow(_uiState.value.form.date)
        val formatted = currentDateTime
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)
            .format(serverDateTimeFormatter)

        _uiState.update { it.copy(form = it.form.copy(date = formatted)) }
        logger.d(TAG, "Время изменено: $formatted")
    }

    private fun parseFormDateTimeOrNow(dateString: String): LocalDateTime {
        if (dateString.isBlank()) {
            return LocalDateTime.now().withSecond(0).withNano(0)
        }

        return try {
            when {
                dateString.contains('Z') || dateString.contains('+') -> {
                    OffsetDateTime.parse(dateString)
                        .atZoneSameInstant(ZoneId.systemDefault())
                        .toLocalDateTime()
                }

                dateString.contains('T') -> {
                    LocalDateTime.parse(dateString, serverDateTimeFormatter)
                }

                dateString.contains(' ') -> {
                    LocalDateTime.parse(
                        dateString,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    )
                }

                else -> {
                    LocalDate.parse(dateString)
                        .atTime(LocalTime.now().withSecond(0).withNano(0))
                }
            }.withNano(0)
        } catch (_: Exception) {
            LocalDateTime.now().withSecond(0).withNano(0)
        }
    }

    override fun onParkClick() {
        val currentParkId = _uiState.value.form.parkId.takeIf { it > 0 }
        viewModelScope.launch {
            _events.emit(EventFormEvent.NavigateToSelectPark(currentParkId))
        }
        logger.d(TAG, "Клик по выбору парка: $currentParkId")
    }

    override fun onParkSelected(parkId: Long, parkName: String) {
        _uiState.update {
            it.copy(
                form = it.form.copy(
                    parkId = parkId,
                    parkName = parkName
                )
            )
        }
        logger.d(TAG, "Выбран парк: id=$parkId, name=$parkName")
    }

    override fun onAddPhotoClick() {
        viewModelScope.launch {
            _events.emit(EventFormEvent.ShowPhotoPicker)
        }
        logger.d(TAG, "Клик по добавлению фото")
    }

    override fun onPhotoSelected(uris: List<Uri>) {
        if (uris.isEmpty()) {
            logger.d(TAG, "Выбор фото отменен")
            return
        }

        val validUris = uris.filter { avatarHelper.isSupportedMimeType(it) }
        if (validUris.size != uris.size) {
            logger.w(TAG, "Некоторые фото имеют неподдерживаемый формат")
        }

        val remaining = _uiState.value.remainingNewPhotos
        val cappedUris = validUris.take(remaining)
        if (validUris.size > remaining) {
            logger.w(TAG, "Превышен лимит фото: выбрано ${validUris.size}, доступно $remaining")
        }

        _uiState.update {
            it.copy(selectedPhotos = it.selectedPhotos + cappedUris)
        }
        logger.d(TAG, "Выбрано фото: ${cappedUris.size}")
    }

    override fun onPhotoRemove(uri: Uri) {
        _uiState.update {
            it.copy(selectedPhotos = it.selectedPhotos - uri)
        }
        logger.d(TAG, "Фото удалено")
    }

    override fun onSaveClick() {
        val currentState = _uiState.value

        if (!currentState.canSave || currentState.isSaving) {
            when {
                !currentState.canSave -> logger.w(
                    TAG,
                    "Попытка сохранить без изменений или невалидные данные"
                )

                currentState.isSaving -> logger.w(TAG, "Сохранение уже в процессе")
            }
            return
        }

        logger.i(TAG, "Начало сохранения мероприятия")

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val imageBytes = prepareImageBytes(currentState.selectedPhotos)

            saveEvent(currentState, imageBytes)
        }
    }

    private fun prepareImageBytes(uris: List<Uri>): List<ByteArray> {
        return uris.mapNotNull { uri ->
            avatarHelper.uriToByteArray(uri).fold(
                onSuccess = { bytes ->
                    val jpegBytes = ImageUtils.convertToJpeg(bytes)
                    val compressed = ImageUtils.compressIfNeeded(jpegBytes)
                    logger.d(
                        TAG,
                        "Фото подготовлено: ${bytes.size} -> ${jpegBytes.size} -> ${compressed.size} bytes"
                    )
                    compressed
                },
                onFailure = { error ->
                    logger.e(TAG, "Ошибка чтения фото: ${error.message}", error)
                    userNotifier.handleError(
                        AppError.Generic("Ошибка чтения фото", error)
                    )
                    null
                }
            )
        }
    }

    private suspend fun saveEvent(
        currentState: EventFormUiState,
        photos: List<ByteArray>
    ) {
        val result = when (val m = currentState.mode) {
            is EventFormMode.RegularCreate -> {
                createEventUseCase(
                    form = currentState.form,
                    photos = photos.takeIf { it.isNotEmpty() }
                )
            }

            is EventFormMode.CreateForSelected -> {
                createEventUseCase(
                    form = currentState.form,
                    photos = photos.takeIf { it.isNotEmpty() }
                )
            }

            is EventFormMode.EditExisting -> {
                editEventUseCase(
                    eventId = m.eventId,
                    form = currentState.form,
                    photos = photos.takeIf { it.isNotEmpty() }
                )
            }
        }

        result.fold(
            onSuccess = { savedEvent ->
                logger.i(TAG, "Мероприятие успешно сохранено: ${savedEvent.id}")
                _uiState.update {
                    it.copy(
                        initialForm = it.form,
                        selectedPhotos = emptyList(),
                        isSaving = false
                    )
                }
                _events.emit(EventFormEvent.Saved(savedEvent))
            },
            onFailure = { error ->
                logger.e(TAG, "Ошибка сохранения мероприятия: ${error.message}", error)
                _uiState.update { it.copy(isSaving = false) }
                userNotifier.handleError(
                    AppError.Generic(
                        error.message ?: "Ошибка сохранения мероприятия",
                        error
                    )
                )
            }
        )
    }


    override fun onAction(action: EventFormAction) {
        when (action) {
            is EventFormAction.TitleChange -> onTitleChange(action.value)
            is EventFormAction.DescriptionChange -> onDescriptionChange(action.value)
            is EventFormAction.ParkClick -> onParkClick()
            is EventFormAction.DateChange -> onDateChange(action.timestamp)
            is EventFormAction.TimeChange -> onTimeChange(action.hour, action.minute)
        }
    }
}

sealed class EventFormAction {
    data class TitleChange(val value: String) : EventFormAction()
    data class DescriptionChange(val value: String) : EventFormAction()
    data object ParkClick : EventFormAction()
    data class DateChange(val timestamp: Long) : EventFormAction()
    data class TimeChange(val hour: Int, val minute: Int) : EventFormAction()
}
