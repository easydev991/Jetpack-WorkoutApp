package com.swparks.ui.model

import android.net.Uri

data class PickedImagesState(
    val images: List<Uri> = emptyList(),
    val selectionLimit: Int = 15
) {
    val canAddMore: Boolean
        get() = images.size < selectionLimit

    val remainingSlots: Int
        get() = (selectionLimit - images.size).coerceAtLeast(0)
}
