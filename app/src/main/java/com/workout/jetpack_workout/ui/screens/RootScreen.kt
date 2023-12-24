package com.workout.jetpack_workout.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.workout.jetpack_workout.model.TabBarItem
import com.workout.jetpack_workout.ui.screens.events.EventsNavHost
import com.workout.jetpack_workout.ui.screens.messages.MessagesNavHost
import com.workout.jetpack_workout.ui.screens.more.MoreScreen
import com.workout.jetpack_workout.ui.screens.parks.ParksNavHost
import com.workout.jetpack_workout.ui.screens.profile.ProfileNavHost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootScreen() {
    val rootNavController = rememberNavController()
    Scaffold(
        bottomBar = {
            BottomNavigationView(navController = rootNavController)
        }
    ) { padding ->
        NavHost(
            rootNavController,
            startDestination = TabBarItem.Parks.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(TabBarItem.Parks.route) {
                ParksNavHost()
            }
            composable(TabBarItem.Events.route) {
                EventsNavHost()
            }
            composable(TabBarItem.Messages.route) {
                MessagesNavHost()
            }
            composable(TabBarItem.Profile.route) {
                ProfileNavHost()
            }
            composable(TabBarItem.More.route) {
                MoreScreen()
            }
        }
    }
}

@Composable
private fun BottomNavigationView(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val items = listOf(
        TabBarItem.Parks,
        TabBarItem.Events,
        TabBarItem.Messages,
        TabBarItem.Profile,
        TabBarItem.More
    )
    NavigationBar(tonalElevation = 0.dp) {
        items.forEach { item ->
            val isSelected = item.route == navBackStackEntry?.destination?.route
            NavigationBarItem(
                alwaysShowLabel = false,
                selected = isSelected,
                label = { item.Label() },
                icon = { item.Icon(isSelected = isSelected) },
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}