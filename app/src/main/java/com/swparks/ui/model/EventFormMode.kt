package com.swparks.ui.model

import com.swparks.R
import com.swparks.data.model.Event

sealed class EventFormMode {
    data object RegularCreate : EventFormMode()

    data class EditExisting(
        val eventId: Long,
        val event: Event
    ) : EventFormMode()

    data class CreateForSelected(
        val parkId: Long,
        val parkName: String
    ) : EventFormMode()

    val isEdit: Boolean
        get() = this is EditExisting

    val navigationTitle: Int
        get() =
            when (this) {
                is RegularCreate -> R.string.event_form_title_create
                is EditExisting -> R.string.event_form_title_edit
                is CreateForSelected -> R.string.event_form_title_create
            }
}
