package com.swparks.ui.model

import androidx.annotation.StringRes
import com.swparks.R

/**
 * Тип мероприятия (планируемое/прошедшее)
 */
enum class EventKind(@param:StringRes val description: Int) {
    FUTURE(R.string.future_events),
    PAST(R.string.past_events)
}