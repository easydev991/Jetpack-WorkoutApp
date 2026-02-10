package com.swparks.ui.model

/**
 * Информация о редактируемой записи
 */
data class EditInfo(
    val parentObjectId: Long,
    val entryId: Long,
    val oldEntry: String
)
