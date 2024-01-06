package com.workout.jetpack_workout.model

import androidx.annotation.StringRes
import com.workout.jetpack_workout.R

enum class Gender(
    val rawValue: Int,
    @StringRes val description: Int
) {
    MALE(
        rawValue = 0,
        description = R.string.man
    ),
    FEMALE(
        rawValue = 1,
        description = R.string.woman
    )
}