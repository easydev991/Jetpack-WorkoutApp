package com.swparks.ui.screens.events

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swparks.R
import com.swparks.data.model.Event
import com.swparks.ui.ds.ErrorContentView
import com.swparks.ui.ds.EventRowData
import com.swparks.ui.ds.EventRowView
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.util.DateFormatter

@Composable
fun EventsScreen(
    viewModel: EventsViewModel = viewModel(factory = EventsViewModel.Factory),
    contentModifier: Modifier = Modifier
) {
    when (val uiState = viewModel.eventsUIState) {
        is EventsUIState.Loading -> LoadingOverlayView()
        is EventsUIState.Success -> PastEventsScreen(
            events = uiState.events,
            modifier = contentModifier
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
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = dimensionResource(id = R.dimen.spacing_regular),
            top = dimensionResource(id = R.dimen.spacing_small),
            end = dimensionResource(id = R.dimen.spacing_regular),
            bottom = dimensionResource(id = R.dimen.spacing_regular)
        ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_small)),
        horizontalAlignment = Alignment.Start,
    ) {
        items(
            events,
            key = { it.id }) { event ->
            EventRowView(
                data = EventRowData(
                    imageStringURL = event.preview,
                    name = event.title,
                    dateString = DateFormatter.formatDate(
                        context = context,
                        dateString = event.beginDate
                    ),
                    address = "${event.countryID}, ${event.cityID}",
                    onClick = {
                        Log.d("EventsScreen", "Нажато мероприятие: ${event.title}")
                    }
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsTopAppBar() {
    CenterAlignedTopAppBar(
        title = {
            Text(text = stringResource(id = R.string.events_title))
        },
    )
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
            modifier = Modifier.padding(dimensionResource(R.dimen.spacing_regular))
        )
        androidx.compose.material3.Button(onClick = retryAction) {
            Text(text = "Try again")
        }
    }
}