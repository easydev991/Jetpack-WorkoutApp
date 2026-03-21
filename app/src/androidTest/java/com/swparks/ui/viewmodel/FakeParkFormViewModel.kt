package com.swparks.ui.viewmodel

import android.net.Uri
import com.swparks.data.model.Park
import com.swparks.ui.model.ParkForm
import com.swparks.ui.model.ParkFormMode
import com.swparks.ui.state.ParkFormEvent
import com.swparks.ui.state.ParkFormUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

class FakeParkFormViewModel(
    initialState: ParkFormUiState = ParkFormUiState(
        mode = ParkFormMode.Create(
            initialAddress = "",
            initialLatitude = "",
            initialLongitude = "",
            initialCityId = null
        )
    )
) : IParkFormViewModel {

    private val _uiState = MutableStateFlow(initialState)
    override val uiState: StateFlow<ParkFormUiState> = _uiState

    private val _events = MutableSharedFlow<ParkFormEvent>()
    override val events: SharedFlow<ParkFormEvent> = _events.asSharedFlow()

    override fun onAddressChange(value: String) {
        _uiState.value = _uiState.value.copy(
            form = _uiState.value.form.copy(address = value)
        )
    }

    override fun onTypeChange(typeId: Int) {
        _uiState.value = _uiState.value.copy(
            form = _uiState.value.form.copy(typeId = typeId)
        )
    }

    override fun onSizeChange(sizeId: Int) {
        _uiState.value = _uiState.value.copy(
            form = _uiState.value.form.copy(sizeId = sizeId)
        )
    }

    override fun onAddPhotoClick() {
    }

    override fun onPhotoSelected(uris: List<Uri>) {
        _uiState.value = _uiState.value.copy(
            selectedPhotos = _uiState.value.selectedPhotos + uris
        )
    }

    override fun onPhotoRemove(uri: Uri) {
        _uiState.value = _uiState.value.copy(
            selectedPhotos = _uiState.value.selectedPhotos - uri
        )
    }

    override fun onSaveClick() {
    }

    override fun onAction(action: ParkFormAction) {
        when (action) {
            is ParkFormAction.AddressChange -> onAddressChange(action.value)
            is ParkFormAction.TypeChange -> onTypeChange(action.typeId)
            is ParkFormAction.SizeChange -> onSizeChange(action.sizeId)
        }
    }

    fun setLoading(loading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = loading)
    }

    fun setSaving(saving: Boolean) {
        _uiState.value = _uiState.value.copy(isSaving = saving)
    }

    fun updateForm(newForm: ParkForm) {
        _uiState.value = _uiState.value.copy(form = newForm)
    }

    fun setMode(mode: ParkFormMode) {
        _uiState.value = _uiState.value.copy(mode = mode)
    }
}
