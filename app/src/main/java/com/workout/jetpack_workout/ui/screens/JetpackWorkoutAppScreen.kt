package com.workout.jetpack_workout.ui.screens

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun JetpackWorkoutAppScreen() {
    val eventsViewModel: EventsViewModel = viewModel(factory = EventsViewModel.Factory)
    EventsScreen(uiState = eventsViewModel.eventsUIState)
}