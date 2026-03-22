package com.swparks.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.swparks.data.database.dao.UserDao
import com.swparks.data.repository.SWRepository
import com.swparks.domain.provider.AvatarHelper
import com.swparks.domain.provider.GeocodingService
import com.swparks.domain.usecase.IFindCityByCoordinatesUseCase
import com.swparks.ui.model.ParkForm
import com.swparks.ui.model.ParkFormMode
import com.swparks.ui.state.ParkFormEvent
import com.swparks.ui.state.ParkFormUiState
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("TooGenericExceptionCaught")
class ParkFormViewModel(
    private val mode: ParkFormMode,
    private val swRepository: SWRepository,
    private val avatarHelper: AvatarHelper,
    private val logger: Logger,
    private val userNotifier: UserNotifier,
    private val geocodingService: GeocodingService,
    private val findCityByCoordinatesUseCase: IFindCityByCoordinatesUseCase,
    private val userDao: UserDao
) : ViewModel(), IParkFormViewModel {

    companion object {
        private const val TAG = "ParkFormViewModel"

        fun factory(
            mode: ParkFormMode,
            appContainer: com.swparks.data.AppContainer
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(ParkFormViewModel::class.java)) {
                    "Неизвестный класс ViewModel: ${modelClass.name}, " +
                        "ожидается: ${ParkFormViewModel::class.java.name}"
                }
                val viewModel = appContainer.parkFormViewModelFactory(mode)
                return checkNotNull(modelClass.cast(viewModel)) {
                    "Не удалось привести ${ParkFormViewModel::class.java.name} к ${modelClass.name}"
                }
            }
        }
    }

    private val _uiState = MutableStateFlow(createInitialState())
    override val uiState: StateFlow<ParkFormUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ParkFormEvent>()
    override val events: SharedFlow<ParkFormEvent> = _events.asSharedFlow()

    init {
        if (mode is ParkFormMode.Create) {
            val shouldGeocode = mode.initialAddress.isEmpty() ||
                mode.initialCityId == null ||
                mode.initialCityId == 0
            if (shouldGeocode) {
                val latitude = mode.initialLatitude.toDoubleOrNull() ?: 0.0
                val longitude = mode.initialLongitude.toDoubleOrNull() ?: 0.0
                if (latitude != 0.0 && longitude != 0.0) {
                    viewModelScope.launch {
                        performGeocoding(latitude, longitude)
                    }
                }
            }
        }
    }

    private suspend fun performGeocoding(latitude: Double, longitude: Double) {
        geocodingService.reverseGeocode(latitude, longitude).fold(
            onSuccess = { result ->
                val cityId = findCityByCoordinatesUseCase(
                    result.locality,
                    latitude,
                    longitude
                )
                _uiState.update { state ->
                    state.copy(
                        form = state.form.copy(
                            address = result.address,
                            cityId = cityId ?: state.form.cityId
                        )
                    )
                }
                logger.d(TAG, "Geocoding success: ${result.address}, cityId: $cityId")
            },
            onFailure = { error ->
                logger.w(TAG, "Geocoding failed: ${error.message}")
                val fallbackCityId = getCurrentUserCityId()
                if (fallbackCityId != null) {
                    _uiState.update { state ->
                        state.copy(form = state.form.copy(cityId = fallbackCityId))
                    }
                    logger.d(TAG, "Using fallback cityId: $fallbackCityId")
                }
            }
        )
    }

    private suspend fun getCurrentUserCityId(): Int? {
        return try {
            userDao.getCurrentUserFlow().first()?.cityId
        } catch (e: Exception) {
            logger.e(TAG, "Failed to get current user cityId: ${e.message}", e)
            null
        }
    }

    private fun createInitialState(): ParkFormUiState {
        return when (val m = mode) {
            is ParkFormMode.Create -> {
                val form = ParkForm.create(
                    address = m.initialAddress,
                    latitude = m.initialLatitude.toDoubleOrNull() ?: 0.0,
                    longitude = m.initialLongitude.toDoubleOrNull() ?: 0.0,
                    cityId = m.initialCityId
                )
                ParkFormUiState(
                    mode = mode,
                    form = form,
                    initialForm = form,
                    isLoading = false
                )
            }

            is ParkFormMode.Edit -> {
                val form = ParkForm.fromPark(m.park)
                ParkFormUiState(
                    mode = mode,
                    form = form,
                    initialForm = form,
                    isLoading = false
                )
            }
        }
    }

    override fun onAddressChange(value: String) {
        _uiState.update { it.copy(form = it.form.copy(address = value)) }
    }

    override fun onTypeChange(typeId: Int) {
        _uiState.update { it.copy(form = it.form.copy(typeId = typeId)) }
    }

    override fun onSizeChange(sizeId: Int) {
        _uiState.update { it.copy(form = it.form.copy(sizeId = sizeId)) }
    }

    override fun onAddPhotoClick() {
        viewModelScope.launch {
            _events.emit(ParkFormEvent.ShowPhotoPicker)
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
            val newUris = it.selectedPhotos + cappedUris
            it.copy(
                selectedPhotos = newUris,
                form = it.form.copy(selectedPhotos = newUris.map { uri -> uri.toString() })
            )
        }
        logger.d(TAG, "Выбрано фото: ${cappedUris.size}")
    }

    override fun onPhotoRemove(uri: Uri) {
        _uiState.update {
            val newUris = it.selectedPhotos - uri
            it.copy(
                selectedPhotos = newUris,
                form = it.form.copy(selectedPhotos = newUris.map { u -> u.toString() })
            )
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

        logger.i(TAG, "Начало сохранения площадки")

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val imageBytes = prepareImageBytes(currentState.selectedPhotos)

            savePark(currentState, imageBytes)
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

    private suspend fun savePark(
        currentState: ParkFormUiState,
        photos: List<ByteArray>
    ) {
        val parkId = when (currentState.mode) {
            is ParkFormMode.Create -> null
            is ParkFormMode.Edit -> currentState.mode.parkId
        }

        val result = swRepository.savePark(
            id = parkId,
            form = currentState.form,
            photos = photos.takeIf { it.isNotEmpty() }
        )

        result.fold(
            onSuccess = { savedPark ->
                logger.i(TAG, "Площадка успешно сохранена: ${savedPark.id}")
                _uiState.update {
                    it.copy(
                        initialForm = it.form.copy(selectedPhotos = emptyList()),
                        selectedPhotos = emptyList(),
                        isSaving = false
                    )
                }
                _events.emit(ParkFormEvent.Saved(savedPark))
            },
            onFailure = { error ->
                logger.e(TAG, "Ошибка сохранения площадки: ${error.message}", error)
                _uiState.update { it.copy(isSaving = false) }
                userNotifier.handleError(
                    AppError.Generic(
                        error.message ?: "Ошибка сохранения площадки",
                        error
                    )
                )
            }
        )
    }

    override fun onAction(action: ParkFormAction) {
        when (action) {
            is ParkFormAction.AddressChange -> onAddressChange(action.value)
            is ParkFormAction.TypeChange -> onTypeChange(action.typeId)
            is ParkFormAction.SizeChange -> onSizeChange(action.sizeId)
        }
    }
}