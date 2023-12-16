package com.workout.jetpack_workout.model

import androidx.annotation.StringRes
import com.workout.jetpack_workout.R

enum class ParkType {
    SOVIET {
        override val model = Model(
            rawValue = 1,
            description = R.string.soviet_park
        )
    },
    MODERN {
        override val model = Model(
            rawValue = 2,
            description = R.string.modern_park
        )
    },
    COLLARS {
        override val model = Model(
            rawValue = 3,
            description = R.string.collars_park
        )
    },
    LEGENDARY {
        override val model = Model(
            rawValue = 6,
            description = R.string.legendary_park
        )
    };

    abstract val model: Model

    data class Model(
        val rawValue: Int,
        @StringRes
        val description: Int
    )
}