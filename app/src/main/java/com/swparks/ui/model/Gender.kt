package com.swparks.ui.model

import androidx.annotation.StringRes
import com.swparks.R

enum class Gender(
    val rawValue: Int,
    @param:StringRes val description: Int,
    @param:StringRes val sex: Int
) {
    MALE(
        rawValue = 0,
        description = R.string.man,
        sex = R.string.male
    ),
    FEMALE(
        rawValue = 1,
        description = R.string.woman,
        sex = R.string.female
    )
}