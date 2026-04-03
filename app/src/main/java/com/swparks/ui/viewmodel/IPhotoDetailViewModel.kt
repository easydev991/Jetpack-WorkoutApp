package com.swparks.ui.viewmodel

import com.swparks.ui.state.PhotoDetailAction
import com.swparks.ui.state.PhotoDetailEvent
import com.swparks.ui.state.PhotoDetailUIState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow

interface IPhotoDetailViewModel {
    val uiState: StateFlow<PhotoDetailUIState>
    val events: Channel<PhotoDetailEvent>
    val isAuthorized: StateFlow<Boolean>

    fun onAction(action: PhotoDetailAction)
}
