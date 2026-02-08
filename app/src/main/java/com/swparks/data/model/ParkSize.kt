package com.swparks.data.model

import androidx.annotation.StringRes
import com.swparks.R

enum class ParkSize(
    val rawValue: Int,
    @param:StringRes val description: Int
) {
    SMALL(
        rawValue = 1,
        description = R.string.small_park
    ),
    MEDIUM(
        rawValue = 2,
        description = R.string.medium_park
    ),
    LARGE(
        rawValue = 3,
        description = R.string.large_park
    )
}