package com.workout.jetpack_workout.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.workout.jetpack_workout.R

sealed class TabBarItem(
    @StringRes
    val titleId: Int,
    val route: String,
    @DrawableRes
    private val unselectedIconID: Int,
    @DrawableRes
    private val selectedIconID: Int
) {
    @Composable
    fun Label() {
        Text(
            text = stringResource(id = titleId),
            fontSize = 10.sp,
            letterSpacing = 0.2.sp,
            softWrap = false,
            overflow = TextOverflow.Visible,
        )
    }

    @Composable
    fun Icon(isSelected: Boolean) {
        androidx.compose.material3.Icon(
            painter = painterResource(id = makeIconID(isSelected)),
            tint = makeTint(isSelected = isSelected),
            contentDescription = route
        )
    }

    private fun makeIconID(isSelected: Boolean): Int {
        return if (isSelected) selectedIconID else unselectedIconID
    }

    @Composable
    private fun makeTint(isSelected: Boolean): Color {
        return if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    object Parks: TabBarItem(
        titleId = R.string.parks,
        route = "parks",
        unselectedIconID = R.drawable.parksicon,
        selectedIconID = R.drawable.parksicon
    )

    object Events: TabBarItem(
        titleId = R.string.events,
        route = "events",
        unselectedIconID = R.drawable.outline_event,
        selectedIconID = R.drawable.filled_event
    )

    object Messages: TabBarItem(
        titleId = R.string.messages,
        route = "messages",
        unselectedIconID = R.drawable.outline_chat,
        selectedIconID = R.drawable.filled_chat
    )

    object Profile: TabBarItem(
        titleId = R.string.profile,
        route = "profile",
        unselectedIconID = R.drawable.outline_person,
        selectedIconID = R.drawable.filled_person
    )

    object More: TabBarItem(
        titleId = R.string.more,
        route = "more",
        unselectedIconID = R.drawable.outline_list,
        selectedIconID = R.drawable.filled_list
    )
}
