package com.swparks.ui.viewmodel

import android.net.Uri
import com.swparks.ui.state.ParkFormEvent
import com.swparks.ui.state.ParkFormUiState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface IParkFormViewModel {
    val uiState: StateFlow<ParkFormUiState>

    val events: SharedFlow<ParkFormEvent>

    fun onAddressChange(value: String)

    fun onTypeChange(typeId: Int)

    fun onSizeChange(sizeId: Int)

    fun onAddPhotoClick()

    fun onPhotoSelected(uris: List<Uri>)

    fun onPhotoRemove(uri: Uri)

    fun onSaveClick()

    fun onAction(action: ParkFormAction)
}
