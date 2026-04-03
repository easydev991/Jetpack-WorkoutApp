package com.swparks.ui.state

import com.swparks.data.model.Photo
import com.swparks.util.Complaint

sealed class PhotoOwner {
    abstract val name: String

    data object Event : PhotoOwner() {
        override val name: String = "Event"
    }

    data object Park : PhotoOwner() {
        override val name: String = "Park"
    }

    companion object {
        fun fromName(name: String): PhotoOwner =
            when (name) {
                Park.name -> Park
                else -> Event
            }
    }
}

sealed class PhotoDetailUIState {
    data class Content(
        val photo: Photo,
        val parentTitle: String,
        val isAuthor: Boolean,
        val isLoading: Boolean = false
    ) : PhotoDetailUIState()

    data class Error(
        val message: String
    ) : PhotoDetailUIState()
}

data class PhotoDetailConfig(
    val photoId: Long,
    val parentId: Long,
    val parentTitle: String,
    val isAuthor: Boolean,
    val photoUrl: String,
    val ownerType: PhotoOwner
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

    data class SendPhotoComplaint(
        val complaint: Complaint
    ) : PhotoDetailEvent()

    data class PhotoDeleted(
        val photoId: Long
    ) : PhotoDetailEvent()
}
