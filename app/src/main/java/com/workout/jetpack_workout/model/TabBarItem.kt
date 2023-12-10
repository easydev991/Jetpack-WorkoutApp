package com.workout.jetpack_workout.model

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.workout.jetpack_workout.R

data class TabBarItem(
    @StringRes
    val titleId: Int,
    val route: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector
) {
    object Items {
        val list = listOf(
            TabBarItem(
                titleId = R.string.grounds,
                route = "grounds",
                unselectedIcon = Icons.Outlined.Place,
                selectedIcon = Icons.Filled.Place
            ),
            TabBarItem(
                titleId = R.string.events,
                route = "events",
                unselectedIcon = Icons.Outlined.List,
                selectedIcon = Icons.Filled.List
            ),
            TabBarItem(
                titleId = R.string.messages,
                route = "messages",
                unselectedIcon = Icons.Outlined.Email,
                selectedIcon = Icons.Filled.Email
            ),
            TabBarItem(
                titleId = R.string.profile,
                route = "profile",
                unselectedIcon = Icons.Outlined.Person,
                selectedIcon = Icons.Filled.Person
            ),
            TabBarItem(
                titleId = R.string.settings,
                route = "settings",
                unselectedIcon = Icons.Outlined.Settings,
                selectedIcon = Icons.Filled.Settings
            ),
        )
    }
}

