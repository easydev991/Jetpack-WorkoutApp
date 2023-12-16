package com.workout.jetpack_workout.model

import androidx.annotation.StringRes
import com.workout.jetpack_workout.R

enum class Gender {
    MALE {
        override val model = Model(
            rawValue = 0,
            description = R.string.man
        )
    },
    FEMALE {
        override val model = Model(
            rawValue = 1,
            description = R.string.woman
        )
    };

    abstract val model: Model

    data class Model(
        val rawValue: Int,
        @StringRes
        val description: Int
    )
}