package com.swparks.ui.screens.settings

import com.swparks.R

/**
 * Режим работы экрана ItemListScreen.
 */
enum class ItemListMode {
    COUNTRY,
    CITY;

    val titleResId: Int
        get() =
            when (this) {
                COUNTRY -> R.string.select_country
                CITY -> R.string.select_city
            }

    val helpMessageResId: Int
        get() =
            when (this) {
                COUNTRY -> R.string.help_country_not_found
                CITY -> R.string.help_city_not_found
            }
}
