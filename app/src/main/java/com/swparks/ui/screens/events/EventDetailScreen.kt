@file:Suppress("LongMethod", "CyclomaticComplexMethod")

package com.swparks.ui.screens.events

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.swparks.R
import com.swparks.data.model.Comment
import com.swparks.ui.ds.CommentRowData
import com.swparks.ui.ds.CommentRowView
import com.swparks.ui.ds.ErrorContentView
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.ds.UserRowData
import com.swparks.ui.ds.UserRowView
import com.swparks.ui.state.EventDetailUIState
import com.swparks.ui.viewmodel.EventDetailEvent
import com.swparks.ui.viewmodel.IEventDetailViewModel
import com.swparks.util.DateFormatter
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    modifier: Modifier = Modifier,
    viewModel: IEventDetailViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isAuthorized by viewModel.isAuthorized.collectAsState()
    val isEventAuthor by viewModel.isEventAuthor.collectAsState()

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
                    val beginTime = parseEventDateToMillis(event.beginDate)
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
                    val geoUri = "geo:${event.latitude},${event.longitude}?q=${event.latitude},${event.longitude}"
                        .toUri()
                    val intent = Intent(Intent.ACTION_VIEW, geoUri)
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        val browserUri =
                            "https://maps.google.com/?q=${event.latitude},${event.longitude}".toUri()
                        context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                    }
                }

                is EventDetailEvent.BuildRoute -> {
                    val navigationUri =
                        "google.navigation:q=${event.latitude},${event.longitude}".toUri()
                    val navigationIntent = Intent(Intent.ACTION_VIEW, navigationUri)
                    try {
                        context.startActivity(navigationIntent)
                    } catch (_: ActivityNotFoundException) {
                        val browserUri =
                            "https://maps.google.com/?daddr=${event.latitude},${event.longitude}".toUri()
                        context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.event_title)) }
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
                            dimensionResource(R.dimen.spacing_small)
                        )
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(dimensionResource(R.dimen.spacing_regular)),
                                verticalArrangement = Arrangement.spacedBy(
                                    dimensionResource(R.dimen.spacing_xsmall)
                                )
                            ) {
                                Text(text = state.event.title)
                                Text(
                                    text = DateFormatter.formatDate(
                                        context = context,
                                        dateString = state.event.beginDate
                                    )
                                )
                                Text(text = state.address)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(
                                        dimensionResource(R.dimen.spacing_xsmall)
                                    )
                                ) {
                                    Button(onClick = viewModel::onOpenMapClick) {
                                        Text(text = stringResource(R.string.event_open_map))
                                    }
                                    Button(onClick = viewModel::onRouteClick) {
                                        Text(text = stringResource(R.string.event_build_route))
                                    }
                                }
                                if (state.event.isCurrent) {
                                    Button(onClick = viewModel::onAddToCalendarClick) {
                                        Text(
                                            text = stringResource(R.string.event_add_to_calendar)
                                        )
                                    }
                                }
                                if (isAuthorized && isEventAuthor) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(
                                            dimensionResource(R.dimen.spacing_xsmall)
                                        )
                                    ) {
                                        Button(onClick = viewModel::onDeleteClick) {
                                            Text(text = stringResource(R.string.delete))
                                        }
                                    }
                                }
                                if (state.event.trainingUsersCount != null) {
                                    Text(
                                        text = pluralStringResource(
                                            id = R.plurals.peopleCount,
                                            count = state.event.trainingUsersCount,
                                            state.event.trainingUsersCount
                                        )
                                    )
                                }
                            }
                        }

                        item {
                            UserRowView(
                                data = UserRowData(
                                    modifier = Modifier.padding(
                                        horizontal = dimensionResource(R.dimen.spacing_regular)
                                    ),
                                    enabled = isAuthorized,
                                    imageStringURL = state.event.author.image,
                                    name = state.event.author.name,
                                    address = state.address,
                                    onClick = {
                                        viewModel.onAuthorClick(state.event.author.id)
                                    }
                                )
                            )
                        }

                        items(state.event.comments.orEmpty(), key = Comment::id) { comment ->
                            val author = comment.user
                            CommentRowView(
                                data = CommentRowData(
                                    modifier = Modifier.padding(
                                        horizontal = dimensionResource(R.dimen.spacing_regular)
                                    ),
                                    imageStringURL = author?.image,
                                    authorName = author?.name ?: "",
                                    dateString = DateFormatter.formatDate(
                                        context = context,
                                        dateString = comment.date
                                    ),
                                    bodyText = comment.parsedBody.orEmpty(),
                                    enabled = isAuthorized,
                                    byMainUser = false,
                                    onAuthorClick = {
                                        author?.id?.let(viewModel::onCommentAuthorClick)
                                    },
                                    onClickAction = { action ->
                                        viewModel.onCommentActionClick(comment.id, action)
                                    }
                                )
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

@Suppress("ReturnCount", "TooGenericExceptionCaught")
private fun parseEventDateToMillis(dateString: String): Long? {
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd"
    )

    patterns.forEach { pattern ->
        try {
            val parser = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }
            return parser.parse(dateString)?.time
        } catch (_: ParseException) {
            // Пробуем следующий формат.
        } catch (_: IllegalArgumentException) {
            // Некорректный шаблон или значение timezone.
        }
    }

    return null
}
