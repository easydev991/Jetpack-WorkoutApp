package com.swparks.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

/**
 * Bottom navigation bar с верхнеуровневыми назначениями
 */
@Composable
fun BottomNavigationBar(
    appState: AppState,
    modifier: Modifier = Modifier,
) {
    val currentDestination = appState.currentDestination

    NavigationBar(
        modifier = modifier,
    ) {
        appState.topLevelDestinations.forEach { destination ->
            val isSelected = currentDestination?.route == destination.route

            NavigationBarItem(
                selected = isSelected,
                onClick = { appState.navigateToTopLevelDestination(destination) },
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
                    Text(text = stringResource(id = destination.iconTextId))
                },
            )
        }
    }
}
