package com.swparks.ui.state

import com.swparks.data.model.Photo

sealed class PhotoDetailUIState {
    data class Content(
        val photo: Photo,
        val eventTitle: String,
        val isEventAuthor: Boolean,
        val isLoading: Boolean = false
    ) : PhotoDetailUIState()

    data class Error(val message: String) : PhotoDetailUIState()
}

data class PhotoDetailConfig(
    val photoId: Long,
    val eventId: Long,
    val eventTitle: String,
    val isEventAuthor: Boolean,
    val photoUrl: String
)

sealed class PhotoDetailAction {
    data object Close : PhotoDetailAction()
    data object DeleteClick : PhotoDetailAction()
    data object DeleteConfirm : PhotoDetailAction()
    data object DeleteDismiss : PhotoDetailAction()
    data object Report : PhotoDetailAction()
}

sealed class PhotoDetailEvent {
    data object CloseScreen : PhotoDetailEvent()
    data object ShowDeleteConfirmDialog : PhotoDetailEvent()
    data class SendPhotoComplaint(val complaint: com.swparks.util.Complaint.EventPhoto) :
        PhotoDetailEvent()

    data class PhotoDeleted(val photoId: Long) : PhotoDetailEvent()
}
