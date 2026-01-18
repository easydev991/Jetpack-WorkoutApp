package com.swparks.ui.screens.events

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.swparks.R
import com.swparks.model.TabBarItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsNavHost() {
    val navController = rememberNavController()
    val eventsViewModel: EventsViewModel = viewModel(factory = EventsViewModel.Factory)
    NavHost(
        navController,
        startDestination = TabBarItem.Events.route
    ) {
        composable(TabBarItem.Events.route) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(text = stringResource(id = R.string.events))
                        },
                        windowInsets = WindowInsets(top = 0)
                    )
                }
            ) {
                EventsScreen(
                    modifier = Modifier.padding(it),
                    uiState = eventsViewModel.eventsUIState
                )
            }
        }
    }
}