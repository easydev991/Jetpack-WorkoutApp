package com.swparks.ui.state

import android.net.Uri
import com.swparks.ui.model.ParkForm
import com.swparks.ui.model.ParkFormMode

data class ParkFormUiState(
    val mode: ParkFormMode,
    val form: ParkForm = ParkForm(),
    val initialForm: ParkForm = ParkForm(),
    val selectedPhotos: List<Uri> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isGeocoding: Boolean = false
) {
    val hasChanges: Boolean
        get() = form != initialForm || selectedPhotos.isNotEmpty()

    val canSave: Boolean
        get() =
            when (mode) {
                is ParkFormMode.Create -> form.isReadyToCreate
                is ParkFormMode.Edit -> form.isReadyToUpdate(initialForm) || selectedPhotos.isNotEmpty()
            }

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

sealed interface ParkFormEvent {
    data class Saved(
        val park: com.swparks.data.model.Park
    ) : ParkFormEvent

    data object NavigateBack : ParkFormEvent

    data object ShowPhotoPicker : ParkFormEvent
}
