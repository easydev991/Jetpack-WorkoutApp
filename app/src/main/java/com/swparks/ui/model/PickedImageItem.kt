package com.swparks.ui.model

import android.net.Uri

sealed class PickedImageItem {
    data class Image(val uri: Uri) : PickedImageItem()

    data object AddButton : PickedImageItem()
}
