package com.swparks.ui.viewmodel

import android.Manifest
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.swparks.JetpackWorkoutApplication
import com.swparks.data.model.NewParkDraft
import com.swparks.domain.usecase.ICreateParkLocationHandler
import com.swparks.util.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ParksRootViewModel(
    private val createParkLocationHandler: ICreateParkLocationHandler,
    private val logger: Logger
) : ViewModel(), IParksRootViewModel {

    companion object {
        private const val TAG = "ParksRootViewModel"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as JetpackWorkoutApplication
                val container = application.container
                ParksRootViewModel(
                    createParkLocationHandler = container.createParkLocationHandler,
                    logger = container.logger
                )
            }
        }
    }

    private val _uiState = MutableStateFlow(ParksRootUiState())
    override val uiState: StateFlow<ParksRootUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ParksRootEvent>()
    override val events: SharedFlow<ParksRootEvent> = _events.asSharedFlow()

    override var permissionLauncher: ((Map<String, Boolean>) -> Unit)? = null
    override var openSettingsLauncher: ((Intent) -> Unit)? = null

    override fun onPermissionGranted() {
        logger.d(TAG, "Разрешение получено")
        handlePermissionGranted()
    }

    override fun onPermissionDenied(shouldShowRationale: Boolean) {
        logger.d(TAG, "Разрешение отклонено, shouldShowRationale=$shouldShowRationale")
        if (shouldShowRationale) {
            _uiState.value = ParksRootUiState(
                showPermissionDialog = true,
                permissionDialogCause = PermissionDialogCause.DENIED
            )
        } else {
            requestPermission()
        }
    }

    override fun onPermissionResult(permissions: Map<String, Boolean>) {
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val isGranted = fineLocationGranted || coarseLocationGranted

        logger.d(
            TAG,
            "Результат запроса разрешений: fine=$fineLocationGranted, coarse=$coarseLocationGranted"
        )
        if (isGranted) {
            handlePermissionGranted()
        } else {
            _uiState.value = ParksRootUiState(
                showPermissionDialog = true,
                permissionDialogCause = PermissionDialogCause.FOREVER_DENIED
            )
        }
    }

    override fun onDismissDialog() {
        logger.d(TAG, "Диалог разрешения закрыт")
        _uiState.value = _uiState.value.copy(
            showPermissionDialog = false,
            permissionDialogCause = null
        )
    }

    override fun onConfirmDialog() {
        logger.d(TAG, "Подтверждение в диалоге, запрашиваем разрешение")
        _uiState.value = _uiState.value.copy(
            showPermissionDialog = false,
            permissionDialogCause = null
        )
        requestPermission()
    }

    override fun onOpenSettings(intent: Intent) {
        logger.d(TAG, "Открываем настройки приложения")
        _uiState.value = _uiState.value.copy(
            showPermissionDialog = false,
            permissionDialogCause = null
        )
        openSettingsLauncher?.invoke(intent)
        viewModelScope.launch {
            _events.emit(ParksRootEvent.OpenSettings)
        }
    }

    private fun requestPermission() {
        logger.d(TAG, "Запрос разрешений на местоположение")
        permissionLauncher?.invoke(
            mapOf(
                Manifest.permission.ACCESS_FINE_LOCATION to true,
                Manifest.permission.ACCESS_COARSE_LOCATION to true
            )
        )
    }

    private fun handlePermissionGranted() {
        logger.d(TAG, "Обработка полученного разрешения")
        _uiState.value = _uiState.value.copy(isGettingLocation = true)
        viewModelScope.launch {
            try {
                logger.d(TAG, "Вызов createParkLocationHandler")
                val result = createParkLocationHandler()
                logger.d(TAG, "Получен результат от createParkLocationHandler")
                result.fold(
                    onSuccess = { draft ->
                        logger.d(TAG, "Создание черновика площадки: $draft")
                        _events.emit(ParksRootEvent.NavigateToCreatePark(draft))
                    },
                    onFailure = {
                        logger.d(
                            TAG,
                            "Ошибка createParkLocationHandler, используем пустой черновик"
                        )
                        _events.emit(ParksRootEvent.NavigateToCreatePark(NewParkDraft.EMPTY))
                    }
                )
            } finally {
                _uiState.value = _uiState.value.copy(isGettingLocation = false)
            }
        }
    }
}
