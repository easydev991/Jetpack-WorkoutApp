package com.swparks.ui.screens.events

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swparks.R
import com.swparks.model.Event
import com.swparks.ui.ds.ErrorContentView
import com.swparks.ui.ds.EventRowData
import com.swparks.ui.ds.EventRowView
import com.swparks.ui.ds.LoadingOverlayView

@Composable
fun EventsScreen(
    viewModel: EventsViewModel = viewModel(factory = EventsViewModel.Factory)
) {
    when (val uiState = viewModel.eventsUIState) {
        is EventsUIState.Loading -> LoadingOverlayView()
        is EventsUIState.Success -> PastEventsScreen(
            events = uiState.events,
            modifier = Modifier.fillMaxWidth()
        )

        is EventsUIState.Error -> ErrorContentView(
            retryAction = { viewModel.getPastEvents() },
            message = uiState.message
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastEventsScreen(
    events: List<Event>,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.events_title))
                },
                windowInsets = WindowInsets(top = 0)
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier,
            contentPadding = PaddingValues(
                start = 16.dp,
                top = paddingValues.calculateTopPadding(),
                end = 16.dp,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            items(
                events,
                key = { it.id }) { event ->
                EventRowView(
                    data = EventRowData(
                        imageStringURL = event.preview,
                        name = event.title,
                        dateString = event.beginDate,
                        address = "${event.countryID}, ${event.cityID}",
                        onClick = {
                            Log.d("EventsScreen", "Нажато мероприятие: ${event.title}")
                        }
                    )
                )
            }
        }
    }
}

@Composable
fun ErrorScreen(
    retryAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
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

@Preview(showBackground = true)
@Composable
fun ErrorScreenPreview() {
    ErrorScreen(retryAction = { /* Empty lambda for preview */ })
}