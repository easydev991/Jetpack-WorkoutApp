package com.swparks.ui.viewmodel

import android.net.Uri
import com.swparks.ui.model.EventForm
import com.swparks.ui.model.EventFormMode
import com.swparks.ui.state.EventFormEvent
import com.swparks.ui.state.EventFormUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

class FakeEventFormViewModel(
    initialState: EventFormUiState = EventFormUiState()
) : IEventFormViewModel {

    private val _uiState = MutableStateFlow(initialState)
    override val uiState: StateFlow<EventFormUiState> = _uiState

    private val _events = MutableSharedFlow<EventFormEvent>()
    override val events: SharedFlow<EventFormEvent> = _events.asSharedFlow()

    private var form: EventForm
        get() = _uiState.value.form
        set(value) {
            _uiState.value = _uiState.value.copy(form = value)
        }

    override fun onTitleChange(value: String) {
        form = form.copy(title = value)
    }

    override fun onDescriptionChange(value: String) {
        form = form.copy(description = value)
    }

    override fun onDateChange(timestamp: Long) {
        form = form.copy(date = timestamp.toString())
    }

    override fun onTimeChange(hour: Int, minute: Int) {
        form = form.copy(date = "${form.date}|$hour:$minute")
    }

    override fun onParkClick() {
    }

    override fun onParkSelected(parkId: Long, parkName: String) {
        form = form.copy(parkId = parkId, parkName = parkName)
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

    override fun onAction(action: EventFormAction) {
        when (action) {
            is EventFormAction.TitleChange -> onTitleChange(action.value)
            is EventFormAction.DescriptionChange -> onDescriptionChange(action.value)
            is EventFormAction.ParkClick -> onParkClick()
            is EventFormAction.DateChange -> onDateChange(action.timestamp)
            is EventFormAction.TimeChange -> onTimeChange(action.hour, action.minute)
        }
    }

    fun setLoading(loading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = loading)
    }

    fun setSaving(saving: Boolean) {
        _uiState.value = _uiState.value.copy(isSaving = saving)
    }

    fun updateForm(newForm: EventForm) {
        _uiState.value = _uiState.value.copy(form = newForm)
    }

    fun setMode(mode: EventFormMode) {
        _uiState.value = _uiState.value.copy(mode = mode)
    }
}
