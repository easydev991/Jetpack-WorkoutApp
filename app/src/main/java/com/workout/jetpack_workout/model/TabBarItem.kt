package com.workout.jetpack_workout.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.workout.jetpack_workout.R

data class TabBarItem(
    @StringRes
    val titleId: Int,
    val route: String,
    @DrawableRes
    val unselectedIconID: Int,
    @DrawableRes
    val selectedIconID: Int
) {
    object Items {
        val list = listOf(
            TabBarItem(
                titleId = R.string.parks,
                route = "parks",
                unselectedIconID = R.drawable.outline_map,
                selectedIconID = R.drawable.filled_map
            ),
            TabBarItem(
                titleId = R.string.events,
                route = "events",
                unselectedIconID = R.drawable.outline_event,
                selectedIconID = R.drawable.filled_event
            ),
            TabBarItem(
                titleId = R.string.messages,
                route = "messages",
                unselectedIconID = R.drawable.outline_chat,
                selectedIconID = R.drawable.filled_chat
            ),
            TabBarItem(
                titleId = R.string.profile,
                route = "profile",
                unselectedIconID = R.drawable.outline_person,
                selectedIconID = R.drawable.filled_person
            ),
            TabBarItem(
                titleId = R.string.settings,
                route = "settings",
                unselectedIconID = R.drawable.outline_settings,
                selectedIconID = R.drawable.filled_settings
            ),
        )
    }
}

