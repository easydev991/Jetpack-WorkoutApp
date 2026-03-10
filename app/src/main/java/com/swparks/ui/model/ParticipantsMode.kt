package com.swparks.ui.model

import com.swparks.R

sealed class ParticipantsMode {
    data object Event : ParticipantsMode()
    data object Park : ParticipantsMode()
}

fun ParticipantsMode.getTitleResId(): Int = when (this) {
    is ParticipantsMode.Event -> R.string.event_participants_title
    is ParticipantsMode.Park -> R.string.park_trainees_title
}
