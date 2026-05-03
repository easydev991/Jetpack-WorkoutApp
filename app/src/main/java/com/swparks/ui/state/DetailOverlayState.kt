package com.swparks.ui.state

import com.swparks.ui.model.TextEntryMode

/**
 * Состояние overlay-элементов (dialog/sheet) для экранов деталей парка и события.
 *
 * Используется как единый snapshot вместо нескольких отдельных Compose-флагов,
 * чтобы исключить несогласованные комбинации флагов.
 */
data class DetailOverlayState(
    val showDeleteDialog: Boolean = false,
    val showDeletePhotoDialog: Boolean = false,
    val showDeleteCommentDialog: Boolean = false,
    val showTextEntrySheet: Boolean = false,
    val textEntryMode: TextEntryMode? = null,
    val showPhotoDetailSheet: Boolean = false,
    val photoDetailConfig: PhotoDetailConfig? = null
)
