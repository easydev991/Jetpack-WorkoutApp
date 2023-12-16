package com.workout.jetpack_workout.model

import androidx.annotation.StringRes
import com.workout.jetpack_workout.R

enum class ParkSize {
    SMALL {
        override val model = Model(
            rawValue = 1,
            description = R.string.small_park
        )
    },
    MEDIUM {
        override val model = Model(
            rawValue = 2,
            description = R.string.medium_park
        )
    },
    LARGE {
        override val model = Model(
            rawValue = 3,
            description = R.string.large_park
        )
    };

    abstract val model: Model

    data class Model(
        val rawValue: Int,
        @StringRes
        val description: Int
    )
}