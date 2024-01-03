package com.workout.jetpack_workout.ui.screens.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.workout.jetpack_workout.model.Event
import com.workout.jetpack_workout.ui.ds.EventRowView
import com.workout.jetpack_workout.ui.ds.LoadingOverlayView

@Composable
fun EventsScreen(
    uiState: EventsUIState,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is EventsUIState.Loading -> LoadingOverlayView()
        is EventsUIState.Success -> PastEventsScreen(
            uiState.events,
            modifier = modifier.fillMaxWidth()
        )

        is EventsUIState.Error -> ErrorScreen(
            retryAction = { TODO(reason = "Нужно обработать ошибку") }
        )
    }
}

@Composable
fun PastEventsScreen(
    events: List<Event>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start,
        modifier = modifier
    ) {
        items(
            events,
            key = { it.id }) {
            EventRowView(
                imageStringURL = it.preview,
                name = it.title,
                dateString = it.beginDate,
                address = "${it.countryID}, ${it.cityID}"
            )
        }
    }
}

@Composable
fun ErrorScreen(
    retryAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Error!",
            modifier = Modifier.padding(16.dp)
        )
        Button(onClick = retryAction) {
            Text(text = "Try again")
        }
    }
}