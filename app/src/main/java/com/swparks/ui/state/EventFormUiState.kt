package com.swparks.ui.state

import android.net.Uri
import com.swparks.ui.model.EventForm
import com.swparks.ui.model.EventFormMode

data class EventFormUiState(
    val mode: EventFormMode = EventFormMode.RegularCreate,
    val form: EventForm = EventForm(),
    val initialForm: EventForm = EventForm(),
    val selectedPhotos: List<Uri> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false
) {
    val hasChanges: Boolean
        get() = form != initialForm || selectedPhotos.isNotEmpty()

    val canSave: Boolean
        get() =
            when (mode) {
                is EventFormMode.RegularCreate -> form.isReadyToCreate && hasChanges
                is EventFormMode.CreateForSelected -> form.isReadyToCreate && hasChanges
                is EventFormMode.EditExisting -> form.isReadyToUpdate(initialForm) || selectedPhotos.isNotEmpty()
            }

    val canSelectPark: Boolean
        get() = mode !is EventFormMode.CreateForSelected

    val photosCount: Int
        get() = form.photosCount

    val maxNewPhotos: Int
        get() = (PHOTOS_LIMIT - photosCount).coerceAtLeast(0)

    val remainingNewPhotos: Int
        get() = (maxNewPhotos - selectedPhotos.size).coerceAtLeast(0)

    companion object {
        const val PHOTOS_LIMIT = 15
    }
}

sealed interface EventFormEvent {
    data class Saved(
        val event: com.swparks.data.model.Event
    ) : EventFormEvent

    data object NavigateBack : EventFormEvent

    data class NavigateToSelectPark(
        val currentParkId: Long?
    ) : EventFormEvent

    data object ShowDatePicker : EventFormEvent

    data object ShowPhotoPicker : EventFormEvent
}
