@file:Suppress("LongMethod", "CyclomaticComplexMethod")

package com.swparks.ui.screens.events

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.swparks.data.model.User
import com.swparks.ui.ds.ErrorContentView
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.model.TextEntryMode
import com.swparks.ui.screens.common.TextEntrySheetHost
import com.swparks.ui.screens.more.sendComplaint
import com.swparks.ui.state.EventDetailUIState
import com.swparks.ui.viewmodel.EventDetailEvent
import com.swparks.ui.viewmodel.IEventDetailViewModel
import com.swparks.util.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    viewModel: IEventDetailViewModel,
    onBack: () -> Unit,
    onNavigateToUserProfile: (Long) -> Unit,
    onNavigateToParticipants: (Long, List<User>) -> Unit,
    parentPaddingValues: PaddingValues
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isAuthorized by viewModel.isAuthorized.collectAsState()
    val isEventAuthor by viewModel.isEventAuthor.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()

    var showDeleteEventDialog by remember { mutableStateOf(false) }
    var showDeletePhotoDialog by remember { mutableStateOf(false) }
    var showDeleteCommentDialog by remember { mutableStateOf(false) }
    var showTextEntrySheet by remember { mutableStateOf(false) }
    var textEntryMode by remember { mutableStateOf<TextEntryMode?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                EventDetailEvent.ShowDeleteConfirmDialog -> showDeleteEventDialog = true
                is EventDetailEvent.ShowDeletePhotoConfirmDialog -> showDeletePhotoDialog = true
                EventDetailEvent.ShowDeleteCommentConfirmDialog -> showDeleteCommentDialog = true
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

                is EventDetailEvent.NavigateToParticipants -> {
                    onNavigateToParticipants(event.eventId, event.users)
                }

                is EventDetailEvent.SendCommentComplaint -> {
                    sendComplaint(
                        complaint = event.complaint,
                        context = context
                    )
                }

                is EventDetailEvent.OpenCommentTextEntry -> {
                    textEntryMode = event.mode
                    showTextEntrySheet = true
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                    val state = uiState
                    if (state is EventDetailUIState.Content) {
                        EventShareButton(
                            isRefreshing = isRefreshing,
                            onShareClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, state.event.shareLinkStringURL)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, null))
                            }
                        )
                    }
                    if (isAuthorized && isEventAuthor) {
                        EventAuthorActionsButton(
                            isRefreshing = isRefreshing,
                            onEditClick = {
                                android.util.Log.d("EventDetailScreen", "Edit event clicked")
                            },
                            onDeleteClick = viewModel::onDeleteClick
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            EventDetailUIState.InitialLoading -> LoadingOverlayView(
                modifier = Modifier
                    .padding(parentPaddingValues)
                    .padding(paddingValues)
            )

            is EventDetailUIState.Error -> ErrorContentView(
                modifier = Modifier
                    .padding(parentPaddingValues)
                    .padding(paddingValues),
                message = state.message,
                retryAction = viewModel::refresh
            )

            is EventDetailUIState.Content -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(parentPaddingValues)
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
                                onAction = { action ->
                                    when (action) {
                                        EventHeaderAction.OpenMap -> viewModel.onOpenMapClick()
                                        EventHeaderAction.Route -> viewModel.onRouteClick()
                                        EventHeaderAction.AddToCalendar -> viewModel.onAddToCalendarClick()
                                    }
                                }
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

                        if (state.event.photos.isNotEmpty()) {
                            item {
                                EventPhotosSection(
                                    photos = state.event.photos,
                                    isRefreshing = isRefreshing,
                                    onPhotoClick = viewModel::onPhotoClick
                                )
                            }
                        }

                        item {
                            EventAuthorSection(
                                event = state.event,
                                address = state.authorAddress,
                                config = EventAuthorConfig(
                                    isAuthorized = isAuthorized,
                                    isRefreshing = isRefreshing,
                                    isEventAuthor = isEventAuthor
                                ),
                                onAuthorClick = onNavigateToUserProfile
                            )
                        }

                        itemsIndexed(
                            state.event.comments.orEmpty(),
                            key = { _, comment -> comment.id }
                        ) { index, comment ->
                            EventCommentItem(
                                comment = comment,
                                config = CommentItemConfig(
                                    enabled = isAuthorized && !isRefreshing,
                                    currentUserId = currentUserId,
                                    showSectionHeader = index == 0
                                ),
                                modifier = Modifier.padding(
                                    horizontal = dimensionResource(R.dimen.spacing_regular)
                                ),
                                onAction = { action ->
                                    when (action) {
                                        is CommentItemAction.AuthorClick -> onNavigateToUserProfile(
                                            action.userId
                                        )

                                        is CommentItemAction.CommentAction -> viewModel.onCommentActionClick(
                                            action.commentId,
                                            action.action
                                        )
                                    }
                                }
                            )
                        }

                        item {
                            EventAddCommentButton(
                                isAuthorized = isAuthorized,
                                isRefreshing = isRefreshing,
                                onAddCommentClick = viewModel::onAddCommentClick
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
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
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
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
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

    if (showDeleteCommentDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteCommentDialog = false
                viewModel.onCommentDeleteDismiss()
            },
            title = { Text(text = stringResource(R.string.event_delete_comment_confirm_title)) },
            text = { Text(text = stringResource(R.string.event_delete_comment_confirm_message)) },
            confirmButton = {
                TextButton(
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = {
                        showDeleteCommentDialog = false
                        viewModel.onCommentDeleteConfirm()
                    }
                ) {
                    Text(text = stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteCommentDialog = false
                        viewModel.onCommentDeleteDismiss()
                    }
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showTextEntrySheet && textEntryMode != null) {
        TextEntrySheetHost(
            show = showTextEntrySheet,
            mode = checkNotNull(textEntryMode),
            onDismissed = { showTextEntrySheet = false },
            onSendSuccess = {
                showTextEntrySheet = false
                viewModel.refresh()
            }
        )
    }
}
