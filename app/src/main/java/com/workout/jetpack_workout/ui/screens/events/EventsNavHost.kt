package com.workout.jetpack_workout.ui.screens.events

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.workout.jetpack_workout.ui.theme.JetpackWorkoutAppTheme

@Composable
fun EventsNavHost() {
    val navController = rememberNavController()
    val eventsViewModel: EventsViewModel = viewModel(factory = EventsViewModel.Factory)
    NavHost(
        navController,
        startDestination = "events"
    ) {
        composable("events") {
            EventsRootScreen(uiState = eventsViewModel.eventsUIState)
        }
    }
}

@Composable
fun EventsRootScreen(uiState: EventsUIState) {
    EventsScreen(uiState = uiState)
}

@Preview(showBackground = true)
@Composable
fun MessagesRootScreenPreview() {
    JetpackWorkoutAppTheme {
        EventsRootScreen(uiState = EventsUIState.Success(listOf()))
    }
}