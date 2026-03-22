package com.swparks.ui.viewmodel

import android.Manifest
import android.content.Intent
import com.swparks.data.model.NewParkDraft
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

class FakeParksRootViewModel : IParksRootViewModel {

    private val _uiState = MutableStateFlow(ParksRootUiState())
    override val uiState: StateFlow<ParksRootUiState> = _uiState

    private val _events = MutableSharedFlow<ParksRootEvent>()
    override val events: SharedFlow<ParksRootEvent> = _events.asSharedFlow()

    override var permissionLauncher: ((Map<String, Boolean>) -> Unit)? = null
    override var openSettingsLauncher: ((Intent) -> Unit)? = null

    var nextDraft: NewParkDraft = NewParkDraft.EMPTY
    private val _capturedEvents = mutableListOf<ParksRootEvent>()
    val capturedEvents: List<ParksRootEvent> = _capturedEvents

    private var shouldBypassPermission = false
    private var shouldShowDialogOnPermissionDenied = false
    private var dialogCauseToShow: PermissionDialogCause? = null

    fun reset() {
        _uiState.value = ParksRootUiState()
        _capturedEvents.clear()
        nextDraft = NewParkDraft.EMPTY
        shouldBypassPermission = false
        shouldShowDialogOnPermissionDenied = false
        dialogCauseToShow = null
    }

    fun setBypassPermission(bypass: Boolean) {
        shouldBypassPermission = bypass
    }

    fun setShowDialogOnPermissionDenied(cause: PermissionDialogCause) {
        shouldShowDialogOnPermissionDenied = true
        dialogCauseToShow = cause
    }

    override fun onPermissionGranted() {
        _capturedEvents.add(ParksRootEvent.NavigateToCreatePark(nextDraft))
    }

    override fun onPermissionDenied(shouldShowRationale: Boolean) {
        if (shouldShowDialogOnPermissionDenied) {
            _uiState.value = ParksRootUiState(
                showPermissionDialog = true,
                permissionDialogCause = dialogCauseToShow
            )
        } else if (!shouldShowRationale) {
            // If no dialog should be shown and no rationale needed, simulate requesting permission
            permissionLauncher?.invoke(
                mapOf(
                    Manifest.permission.ACCESS_FINE_LOCATION to true,
                    Manifest.permission.ACCESS_COARSE_LOCATION to true
                )
            )
        }
    }

    override fun onPermissionResult(permissions: Map<String, Boolean>) {
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val isGranted = fineLocationGranted || coarseLocationGranted

        if (isGranted) {
            _capturedEvents.add(ParksRootEvent.NavigateToCreatePark(nextDraft))
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
        permissionLauncher?.invoke(
            mapOf(
                Manifest.permission.ACCESS_FINE_LOCATION to true,
                Manifest.permission.ACCESS_COARSE_LOCATION to true
            )
        )
    }

    override fun onOpenSettings(intent: Intent) {
        _uiState.value = _uiState.value.copy(
            showPermissionDialog = false,
            permissionDialogCause = null
        )
    }
}
