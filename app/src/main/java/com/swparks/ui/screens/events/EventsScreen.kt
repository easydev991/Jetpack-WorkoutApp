package com.swparks.ui.screens.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swparks.R
import com.swparks.data.model.Event
import com.swparks.ui.ds.EmptyStateView
import com.swparks.ui.ds.ErrorContentView
import com.swparks.ui.ds.EventRowData
import com.swparks.ui.ds.EventRowView
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.model.EventKind
import com.swparks.ui.viewmodel.IEventsViewModel
import com.swparks.util.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    modifier: Modifier = Modifier,
    viewModel: IEventsViewModel = viewModel<EventsViewModel>(factory = EventsViewModel.Factory),
) {
    val uiState by viewModel.eventsUIState.collectAsState()
    val isAuthorized by viewModel.isAuthorized.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val fabDescription = stringResource(id = R.string.events_fab_description)

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dimensionResource(id = R.dimen.spacing_regular),
                        end = dimensionResource(id = R.dimen.spacing_regular),
                        top = dimensionResource(id = R.dimen.spacing_small)
                    )
            ) {
                EventKind.entries.forEachIndexed { index, eventKind ->
                    SegmentedButton(
                        selected = selectedTab == eventKind,
                        onClick = { viewModel.onTabSelected(eventKind) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = EventKind.entries.size
                        ),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = when (eventKind) {
                                EventKind.FUTURE -> stringResource(id = R.string.future_events)
                                EventKind.PAST -> stringResource(id = R.string.past_events)
                            }
                        )
                    }
                }
            }

            when (val state = uiState) {
                is EventsUIState.InitialLoading -> Box(modifier = Modifier.fillMaxSize()) {
                    LoadingOverlayView()
                }
                is EventsUIState.Content -> EventsListWithRefresh(
                    events = state.events,
                    selectedTab = state.selectedTab,
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    onEventClick = { event -> viewModel.onEventClick(event) }
                )

                is EventsUIState.Error -> ErrorContentView(
                    retryAction = { viewModel.refresh() },
                    message = state.message
                )
            }
        }

        if (isAuthorized) {
            ExtendedFloatingActionButton(
                onClick = { viewModel.onFabClick() },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(text = fabDescription) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(dimensionResource(id = R.dimen.spacing_regular))
                    .semantics {
                        this.contentDescription = fabDescription
                    }
            )
        }
    }
}

@Composable
private fun EventsListWithRefresh(
    events: List<Event>,
    selectedTab: EventKind,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onEventClick: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    text = when (selectedTab) {
                        EventKind.FUTURE -> stringResource(id = R.string.events_empty_future)
                        EventKind.PAST -> stringResource(id = R.string.events_empty_past)
                    }
                )
            }
        } else {
            EventsList(
                events = events,
                onEventClick = onEventClick
            )
        }
    }
}

@Composable
private fun EventsList(
    events: List<Event>,
    onEventClick: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
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
            key = { it.id }
        ) { event ->
            EventRowView(
                data = EventRowData(
                    imageStringURL = event.preview,
                    name = event.title,
                    dateString = DateFormatter.formatDate(
                        context = context,
                        dateString = event.beginDate
                    ),
                    address = "${event.countryID}, ${event.cityID}",
                    onClick = { onEventClick(event) }
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
