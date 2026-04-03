package com.swparks.ui.model

import androidx.annotation.StringRes
import com.swparks.R

enum class ParksTab(
    @param:StringRes val description: Int
) {
    MAP(R.string.parks_map),
    LIST(R.string.parks_list)
}
