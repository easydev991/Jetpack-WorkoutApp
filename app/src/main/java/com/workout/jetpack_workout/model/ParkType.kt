package com.workout.jetpack_workout.model

import androidx.annotation.StringRes
import com.workout.jetpack_workout.R

enum class ParkType(
    val rawValue: Int,
    @StringRes val description: Int
) {
    SOVIET(
        rawValue = 1,
        description = R.string.soviet_park
    ),
    MODERN(
        rawValue = 2,
        description = R.string.modern_park
    ),
    COLLARS(
        rawValue = 3,
        description = R.string.collars_park
    ),
    LEGENDARY(
        rawValue = 6,
        description = R.string.legendary_park
    )
}