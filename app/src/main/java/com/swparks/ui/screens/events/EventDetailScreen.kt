@file:Suppress("LongMethod", "CyclomaticComplexMethod")

package com.swparks.ui.screens.events

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.swparks.R
import com.swparks.data.model.Comment
import com.swparks.ui.ds.ErrorContentView
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.state.EventDetailUIState
import com.swparks.ui.viewmodel.EventDetailEvent
import com.swparks.ui.viewmodel.IEventDetailViewModel
import com.swparks.util.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    modifier: Modifier = Modifier,
    viewModel: IEventDetailViewModel,
    onBack: () -> Unit,
    onNavigateToUserProfile: (Long) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isAuthorized by viewModel.isAuthorized.collectAsState()
    val isEventAuthor by viewModel.isEventAuthor.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()

    var showDeleteEventDialog by remember { mutableStateOf(false) }
    var showDeletePhotoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                EventDetailEvent.ShowDeleteConfirmDialog -> showDeleteEventDialog = true
                is EventDetailEvent.ShowDeletePhotoConfirmDialog -> showDeletePhotoDialog = true
                EventDetailEvent.EventDeleted -> onBack()
                is EventDetailEvent.PhotoDeleted -> Unit
                is EventDetailEvent.OpenCalendar -> {
                    val beginTime = DateFormatter.parseIsoDateToMillis(event.beginDate)
                    val intent = Intent(Intent.ACTION_INSERT).apply {
                        data = CalendarContract.Events.CONTENT_URI
                        putExtra(CalendarContract.Events.TITLE, event.title)
                        putExtra(CalendarContract.Events.EVENT_LOCATION, event.address)
                        if (beginTime != null) {
                            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime)
                        }
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        viewModel.onAddToCalendarFailed()
                    }
                }

                is EventDetailEvent.OpenMap -> {
                    viewModel.mapUriSet?.let { mapUriSet ->
                        val intent = Intent(Intent.ACTION_VIEW, mapUriSet.geoUri)
                        try {
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, mapUriSet.browserUri))
                        }
                    }
                }

                is EventDetailEvent.BuildRoute -> {
                    viewModel.mapUriSet?.let { mapUriSet ->
                        val navigationIntent = Intent(Intent.ACTION_VIEW, mapUriSet.navigationUri)
                        try {
                            context.startActivity(navigationIntent)
                        } catch (_: ActivityNotFoundException) {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, mapUriSet.browserRouteUri)
                            )
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.event_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (isAuthorized && isEventAuthor) {
                        IconButton(onClick = viewModel::onDeleteClick) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.delete)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            EventDetailUIState.InitialLoading -> LoadingOverlayView(
                modifier = Modifier.padding(paddingValues)
            )

            is EventDetailUIState.Error -> ErrorContentView(
                modifier = Modifier.padding(paddingValues),
                message = state.message,
                retryAction = viewModel::refresh
            )

            is EventDetailUIState.Content -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(
                            dimensionResource(R.dimen.spacing_regular)
                        )
                    ) {
                        item {
                            EventHeaderMapCalendarSection(
                                event = state.event,
                                address = state.address,
                                isRefreshing = isRefreshing,
                                callbacks = EventHeaderCallbacks(
                                    onOpenMapClick = viewModel::onOpenMapClick,
                                    onRouteClick = viewModel::onRouteClick,
                                    onAddToCalendarClick = viewModel::onAddToCalendarClick
                                )
                            )
                        }

                        item {
                            EventParticipantsSection(
                                event = state.event,
                                isAuthorized = isAuthorized,
                                isRefreshing = isRefreshing,
                                onParticipantToggle = viewModel::onParticipantToggle,
                                onClickParticipants = viewModel::onParticipantsCountClick
                            )
                        }

                        if (state.event.description.isNotBlank()) {
                            item {
                                EventDescriptionSection(description = state.event.description)
                            }
                        }

                        item {
                            EventAuthorSection(
                                event = state.event,
                                address = state.address,
                                isAuthorized = isAuthorized,
                                isRefreshing = isRefreshing,
                                onAuthorClick = onNavigateToUserProfile
                            )
                        }

                        items(state.event.comments.orEmpty(), key = Comment::id) { comment ->
                            EventCommentItem(
                                comment = comment,
                                isAuthorized = isAuthorized,
                                currentUserId = currentUserId,
                                onAuthorClick = onNavigateToUserProfile,
                                onActionClick = viewModel::onCommentActionClick
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteEventDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteEventDialog = false
                viewModel.onDeleteDismiss()
            },
            title = { Text(text = stringResource(R.string.event_delete_confirm_title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteEventDialog = false
                        viewModel.onDeleteConfirm()
                    }
                ) {
                    Text(text = stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteEventDialog = false
                        viewModel.onDeleteDismiss()
                    }
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDeletePhotoDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeletePhotoDialog = false
                viewModel.onPhotoDeleteDismiss()
            },
            title = { Text(text = stringResource(R.string.event_delete_photo_confirm_title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeletePhotoDialog = false
                        viewModel.onPhotoDeleteConfirm()
                    }
                ) {
                    Text(text = stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeletePhotoDialog = false
                        viewModel.onPhotoDeleteDismiss()
                    }
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }
}
