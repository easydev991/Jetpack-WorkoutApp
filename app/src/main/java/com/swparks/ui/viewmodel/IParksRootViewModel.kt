package com.swparks.ui.viewmodel

import android.content.Intent
import com.swparks.data.model.NewParkDraft
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface IParksRootViewModel {
    val uiState: StateFlow<ParksRootUiState>
    val events: SharedFlow<ParksRootEvent>

    var permissionLauncher: ((Map<String, Boolean>) -> Unit)?
    var openSettingsLauncher: ((Intent) -> Unit)?

    fun onPermissionGranted()
    fun onPermissionDenied(shouldShowRationale: Boolean)
    fun onPermissionResult(permissions: Map<String, Boolean>)
    fun onDismissDialog()
    fun onConfirmDialog()
    fun onOpenSettings(intent: Intent)
}

data class ParksRootUiState(
    val showPermissionDialog: Boolean = false,
    val permissionDialogCause: PermissionDialogCause? = null
)

sealed class ParksRootEvent {
    data class NavigateToCreatePark(val draft: NewParkDraft) : ParksRootEvent()
    data object OpenSettings : ParksRootEvent()
}

enum class PermissionDialogCause {
    DENIED,
    FOREVER_DENIED
}