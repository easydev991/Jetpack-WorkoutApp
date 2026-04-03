package com.swparks.ui.viewmodel

import android.net.Uri
import com.swparks.ui.state.EventFormEvent
import com.swparks.ui.state.EventFormUiState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface IEventFormViewModel {
    val uiState: StateFlow<EventFormUiState>

    val events: SharedFlow<EventFormEvent>

    fun onTitleChange(value: String)

    fun onDescriptionChange(value: String)

    fun onDateChange(timestamp: Long)

    fun onTimeChange(
        hour: Int,
        minute: Int
    )

    fun onParkClick()

    fun onParkSelected(
        parkId: Long,
        parkName: String
    )

    fun onAddPhotoClick()

    fun onPhotoSelected(uris: List<Uri>)

    fun onPhotoRemove(uri: Uri)

    fun onSaveClick()

    fun onAction(action: EventFormAction)
}
