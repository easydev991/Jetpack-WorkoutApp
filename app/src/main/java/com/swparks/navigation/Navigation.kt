package com.swparks.navigation

import android.util.Log
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow.Companion.Ellipsis

private const val TAG = "BottomNavigation"

/**
 * Bottom navigation bar с верхнеуровневыми назначениями
 */
@Composable
fun BottomNavigationBar(
    appState: AppState,
    modifier: Modifier = Modifier,
) {
    val currentDestination = appState.currentTopLevelDestination
    val isAuthorized = appState.isAuthorized
    val bottomNavVisualEpoch = appState.bottomNavVisualEpoch

    Log.d(
        TAG,
        "BottomNavigationBar: рекомпозиция, currentDestination=${currentDestination?.route}, " +
            "isAuthorized=$isAuthorized, epoch=$bottomNavVisualEpoch"
    )

    key(currentDestination?.route, isAuthorized, bottomNavVisualEpoch) {
        NavigationBar(
            modifier = modifier,
        ) {
            appState.topLevelDestinations.forEach { destination ->
                val isSelected = currentDestination?.route == destination.route
                val interactionSource =
                    remember(destination.route, isAuthorized, bottomNavVisualEpoch) {
                        MutableInteractionSource()
                    }

                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        Log.d(
                            TAG,
                            ">>> НАЖАТА вкладка: ${destination.route} (текущая: ${currentDestination?.route})"
                        )
                        appState.navigateToTopLevelDestination(destination)
                    },
                    interactionSource = interactionSource,
                    icon = {
                        Icon(
                            imageVector = if (isSelected) {
                                destination.selectedIcon
                            } else {
                                destination.unselectedIcon
                            },
                            contentDescription = stringResource(id = destination.iconTextId),
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(id = destination.iconTextId),
                            maxLines = 1,
                            overflow = Ellipsis
                        )
                    },
                    alwaysShowLabel = false
                )
            }
        }
    }
}
