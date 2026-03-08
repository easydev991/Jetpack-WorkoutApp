package com.swparks.navigation

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow.Companion.Ellipsis
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color

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

    Log.d(
        TAG,
        "BottomNavigationBar: рекомпозиция, currentDestination=${currentDestination?.route}, " +
            "isAuthorized=$isAuthorized"
    )

    NavigationBar(
        modifier = modifier,
    ) {
        appState.topLevelDestinations.forEach { destination ->
            val isSelected = currentDestination?.route == destination.route

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    Log.d(
                        TAG,
                        ">>> НАЖАТА вкладка: ${destination.route} (текущая: ${currentDestination?.route})"
                    )
                    appState.navigateToTopLevelDestination(destination)
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                ),
                icon = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    Color.Transparent
                                }
                            )
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = if (isSelected) {
                                destination.selectedIcon
                            } else {
                                destination.unselectedIcon
                            },
                            contentDescription = stringResource(id = destination.iconTextId),
                        )
                    }
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
