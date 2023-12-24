package com.workout.jetpack_workout.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val navBackStackEntry by rootNavController.currentBackStackEntryAsState()
    Scaffold(
        bottomBar = {
            NavigationBar(
                contentColor = Color(0x406ac173),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                TabBarItem.Items.list.forEach { item ->
                    val isSelected = item.route == navBackStackEntry?.destination?.route
                    NavigationBarItem(
                        alwaysShowLabel = false,
                        selected = isSelected,
                        label = {
                            Text(
                                text = stringResource(id = item.titleId),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 0.2.sp,
                                softWrap = false,
                                overflow = TextOverflow.Visible
                            )
                        },
                        icon = {
                            Icon(
                                painter = painterResource(
                                    id = if (isSelected) {
                                        item.selectedIconID
                                    } else {
                                        item.unselectedIconID
                                    }
                                ),
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                contentDescription = item.route
                            )
                        },
                        onClick = {
                            rootNavController.navigate(item.route) {
                                popUpTo(rootNavController.graph.findStartDestination().id) {
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
    ) { padding ->
        NavHost(
            rootNavController,
            startDestination = "parks",
            modifier = Modifier.padding(padding)
        ) {
            composable("parks") {
                ParksNavHost()
            }
            composable("events") {
                EventsNavHost()
            }
            composable("messages") {
                MessagesNavHost()
            }
            composable("profile") {
                ProfileNavHost()
            }
            composable("more") {
                MoreScreen()
            }
        }
    }
}