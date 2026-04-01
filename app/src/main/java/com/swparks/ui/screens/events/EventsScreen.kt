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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swparks.R
import com.swparks.data.model.Event
import com.swparks.ui.ds.EmptyStateView
import com.swparks.ui.ds.ErrorContentView
import com.swparks.ui.ds.EventRowData
import com.swparks.ui.ds.EventRowView
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.model.EventKind
import com.swparks.ui.state.EventsListAction
import com.swparks.ui.state.EventsListState
import com.swparks.ui.state.EventsUIState
import com.swparks.ui.testtags.ScreenshotTestTags
import com.swparks.ui.viewmodel.EventsEvent
import com.swparks.ui.viewmodel.EventsViewModel
import com.swparks.ui.viewmodel.IEventsViewModel
import com.swparks.util.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    modifier: Modifier = Modifier,
    viewModel: IEventsViewModel = viewModel<EventsViewModel>(factory = EventsViewModel.Factory),
    onNavigateToEventDetail: (Long) -> Unit = {},
    onNavigateToCreateEvent: () -> Unit = {},
    onNavigateToParks: () -> Unit = {},
) {
    val uiState by viewModel.eventsUIState.collectAsState()
    val isAuthorized by viewModel.isAuthorized.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val selectedTabIndex = if (selectedTab == EventKind.FUTURE) 0 else 1
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val fabDescription = stringResource(id = R.string.events_fab_description)
    var showEventCreationRuleDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                EventsEvent.NavigateToCreateEvent -> onNavigateToCreateEvent()
                EventsEvent.ShowEventCreationRule -> showEventCreationRuleDialog = true
            }
        }
    }

    if (showEventCreationRuleDialog) {
        AlertDialog(
            onDismissRequest = { showEventCreationRuleDialog = false },
            title = { Text(text = stringResource(R.string.event_creation_rule_title)) },
            text = { Text(text = stringResource(R.string.event_creation_rule_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showEventCreationRuleDialog = false
                        onNavigateToParks()
                    }
                ) {
                    Text(text = stringResource(R.string.event_creation_rule_open_parks))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEventCreationRuleDialog = false }) {
                    Text(text = stringResource(R.string.event_creation_rule_dismiss))
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            EventsTabRow(
                selectedTabIndex = selectedTabIndex,
                isRefreshing = isRefreshing,
                onTabSelected = { viewModel.onTabSelected(it) }
            )

            EventsStateContent(
                uiState = uiState,
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                onEventClick = { event ->
                    viewModel.onEventClick(event)
                    onNavigateToEventDetail(event.id)
                }
            )
        }

        if (isAuthorized && currentUser != null) {
            CreateEventFab(
                onClick = { viewModel.onFabClick() },
                description = fabDescription,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventsTabRow(
    selectedTabIndex: Int,
    isRefreshing: Boolean,
    onTabSelected: (EventKind) -> Unit,
    modifier: Modifier = Modifier
) {
    PrimaryTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(id = R.dimen.spacing_regular),
                end = dimensionResource(id = R.dimen.spacing_regular),
                top = dimensionResource(id = R.dimen.spacing_small)
            )
    ) {
        EventKind.entries.forEachIndexed { index, eventKind ->
            Tab(
                modifier = Modifier
                    .focusProperties { canFocus = false }
                    .testTag(
                        if (eventKind == EventKind.FUTURE) {
                            ScreenshotTestTags.EVENTS_TAB_FUTURE
                        } else {
                            ScreenshotTestTags.EVENTS_TAB_PAST
                        }
                    ),
                selected = selectedTabIndex == index,
                enabled = !isRefreshing,
                onClick = { onTabSelected(eventKind) },
                text = {
                    Text(
                        text = when (eventKind) {
                            EventKind.FUTURE -> stringResource(id = R.string.future_events)
                            EventKind.PAST -> stringResource(id = R.string.past_events)
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun EventsStateContent(
    uiState: EventsUIState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onEventClick: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is EventsUIState.InitialLoading -> Box(modifier = modifier.fillMaxSize()) {
            LoadingOverlayView()
        }

        is EventsUIState.Content -> {
            Box(modifier = modifier.fillMaxSize()) {
                EventsListWithRefresh(
                    state = EventsListState(
                        events = uiState.events,
                        addresses = uiState.addresses,
                        selectedTab = uiState.selectedTab,
                        isRefreshing = isRefreshing,
                        isLoading = uiState.isLoading
                    ),
                    onAction = { action ->
                        when (action) {
                            is EventsListAction.Refresh -> onRefresh()
                            is EventsListAction.EventClick -> onEventClick(action.event)
                        }
                    }
                )
                if (uiState.isLoading && !isRefreshing) {
                    LoadingOverlayView()
                }
            }
        }

        is EventsUIState.Error -> ErrorContentView(
            retryAction = onRefresh,
            message = uiState.message
        )
    }
}

@Composable
private fun CreateEventFab(
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.padding(dimensionResource(id = R.dimen.spacing_regular))
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = description
        )
    }
}

@Composable
private fun EventsListWithRefresh(
    state: EventsListState,
    onAction: (EventsListAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val content: @Composable () -> Unit = {
        if (state.events.isEmpty() && !state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    text = when (state.selectedTab) {
                        EventKind.FUTURE -> stringResource(id = R.string.events_empty_future)
                        EventKind.PAST -> stringResource(id = R.string.events_empty_past)
                    }
                )
            }
        } else if (state.events.isNotEmpty()) {
            EventsList(
                events = state.events,
                addresses = state.addresses,
                enabled = !state.isRefreshing && !state.isLoading,
                onEventClick = { event -> onAction(EventsListAction.EventClick(event)) }
            )
        }
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onAction(EventsListAction.Refresh) },
        modifier = modifier.fillMaxSize()
    ) {
        content()
    }
}

@Composable
private fun EventsList(
    events: List<Event>,
    addresses: Map<Pair<Int, Int>, String>,
    enabled: Boolean,
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
                    modifier = Modifier.testTag("${ScreenshotTestTags.EVENT_ROW_PREFIX}${event.id}"),
                    imageStringURL = event.preview,
                    name = event.title,
                    dateString = DateFormatter.formatDate(
                        context = context,
                        dateString = event.beginDate
                    ),
                    address = addresses[event.countryID to event.cityID]
                        ?: "${event.countryID}, ${event.cityID}",
                    enabled = enabled,
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
