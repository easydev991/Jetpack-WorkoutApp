package com.workout.jetpack_workout.model

import androidx.annotation.StringRes
import com.workout.jetpack_workout.R

/**
 * Тип мероприятия (планируемое/прошедшее)
 */
enum class EventKind(@StringRes val description: Int) {
    FUTURE(R.string.future_events),
    PAST(R.string.past_events)
}