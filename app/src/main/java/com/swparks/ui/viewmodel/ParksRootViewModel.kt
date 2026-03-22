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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ParksRootViewModel(
    private val createParkLocationHandler: ICreateParkLocationHandler
) : ViewModel(), IParksRootViewModel {

    companion object {
        private const val TAG = "ParksRootViewModel"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as JetpackWorkoutApplication
                val container = application.container
                ParksRootViewModel(
                    createParkLocationHandler = container.createParkLocationHandler
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
        handlePermissionGranted()
    }

    override fun onPermissionDenied(shouldShowRationale: Boolean) {
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
        _uiState.value = _uiState.value.copy(
            showPermissionDialog = false,
            permissionDialogCause = null
        )
    }

    override fun onConfirmDialog() {
        _uiState.value = _uiState.value.copy(
            showPermissionDialog = false,
            permissionDialogCause = null
        )
        requestPermission()
    }

    override fun onOpenSettings(intent: Intent) {
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
        permissionLauncher?.invoke(
            mapOf(
                Manifest.permission.ACCESS_FINE_LOCATION to true,
                Manifest.permission.ACCESS_COARSE_LOCATION to true
            )
        )
    }

    private fun handlePermissionGranted() {
        viewModelScope.launch {
            val result = createParkLocationHandler()
            result.fold(
                onSuccess = { draft ->
                    _events.emit(ParksRootEvent.NavigateToCreatePark(draft))
                },
                onFailure = {
                    _events.emit(ParksRootEvent.NavigateToCreatePark(NewParkDraft.EMPTY))
                }
            )
        }
    }
}
