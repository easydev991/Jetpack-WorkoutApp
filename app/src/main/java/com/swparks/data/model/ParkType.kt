package com.swparks.data.model

import androidx.annotation.StringRes
import com.swparks.R

enum class ParkType(
    val rawValue: Int,
    @param:StringRes val description: Int
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
