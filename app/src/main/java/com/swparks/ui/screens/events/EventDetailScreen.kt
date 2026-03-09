@file:Suppress("LongMethod", "CyclomaticComplexMethod")

package com.swparks.ui.screens.events

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.CalendarContract
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.swparks.R
import com.swparks.data.model.Comment
import com.swparks.ui.ds.ButtonConfig
import com.swparks.ui.ds.CommentRowData
import com.swparks.ui.ds.CommentRowView
import com.swparks.ui.ds.ErrorContentView
import com.swparks.ui.ds.FormRowView
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.ds.LocationInfoConfig
import com.swparks.ui.ds.LocationInfoView
import com.swparks.ui.ds.SWButton
import com.swparks.ui.ds.SWButtonMode
import com.swparks.ui.ds.SWButtonSize
import com.swparks.ui.ds.UserRowData
import com.swparks.ui.ds.UserRowView
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
                            dimensionResource(R.dimen.spacing_small)
                        )
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(dimensionResource(R.dimen.spacing_regular)),
                                verticalArrangement = Arrangement.spacedBy(
                                    dimensionResource(R.dimen.spacing_small)
                                )
                            ) {
                                Text(
                                    text = state.event.title,
                                    style = MaterialTheme.typography.titleLarge
                                )

                                LabeledValueRow(
                                    label = stringResource(R.string.`when`),
                                    value = DateFormatter.formatDate(
                                        context = context,
                                        dateString = state.event.beginDate
                                    )
                                )

                                LabeledValueRow(
                                    label = stringResource(R.string.where),
                                    value = state.address
                                )

                                val eventAddress = state.event.address
                                if (!eventAddress.isNullOrBlank()) {
                                    LabeledValueRow(
                                        label = stringResource(R.string.address),
                                        value = eventAddress
                                    )
                                }

                                LocationInfoView(
                                    config = LocationInfoConfig(
                                        latitude = state.event.latitude,
                                        longitude = state.event.longitude,
                                        address = state.address,
                                        onOpenMapClick = viewModel::onOpenMapClick,
                                        onRouteClick = viewModel::onRouteClick
                                    )
                                )
                                if (state.event.isCurrent) {
                                    SWButton(
                                        config = ButtonConfig(
                                            modifier = Modifier.fillMaxWidth(),
                                            size = SWButtonSize.LARGE,
                                            mode = SWButtonMode.FILLED,
                                            text = stringResource(R.string.event_add_to_calendar),
                                            enabled = !isRefreshing,
                                            onClick = viewModel::onAddToCalendarClick
                                        )
                                    )
                                }
                                if (isAuthorized) {
                                    state.event.trainingUsersCount?.let { count ->
                                        FormRowView(
                                            modifier = Modifier.fillMaxWidth(),
                                            leadingText = stringResource(R.string.participants),
                                            trailingText = pluralStringResource(
                                                id = R.plurals.peopleCount,
                                                count = count,
                                                count
                                            ),
                                            enabled = !isRefreshing,
                                            onClick = {
                                                Log.d(
                                                    "EventDetailScreen",
                                                    "Нажаты участники: count=$count"
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            UserRowView(
                                data = UserRowData(
                                    modifier = Modifier.padding(
                                        horizontal = dimensionResource(R.dimen.spacing_regular)
                                    ),
                                    enabled = isAuthorized && !isRefreshing,
                                    imageStringURL = state.event.author.image,
                                    name = state.event.author.name,
                                    address = state.address,
                                    onClick = {
                                        onNavigateToUserProfile(state.event.author.id)
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
                                        author?.id?.let(onNavigateToUserProfile)
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

/**
 * Строка с меткой слева и значением справа.
 * Используется для отображения информации вида "Метка: Значение".
 *
 * @param label Текст метки (отображается слева полужирным шрифтом)
 * @param value Текст значения (отображается справа, выравнивание по правому краю)
 * @param modifier Modifier для компонента
 */
@Composable
private fun LabeledValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            dimensionResource(R.dimen.spacing_xxsmall)
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.End
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Short text", showBackground = true)
@Composable
private fun LabeledValueRowShortPreview() {
    MaterialTheme {
        LabeledValueRow(
            label = "Когда",
            value = "15 марта 2026",
            modifier = Modifier.padding(dimensionResource(R.dimen.spacing_regular))
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Long text", showBackground = true)
@Composable
private fun LabeledValueRowLongPreview() {
    MaterialTheme {
        LabeledValueRow(
            label = "Адрес",
            value = "г. Москва, парк Горького, Центральная аллея, д. 1, около главного входа",
            modifier = Modifier.padding(dimensionResource(R.dimen.spacing_regular))
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Mixed texts", showBackground = true)
@Composable
private fun LabeledValueRowMixedPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.spacing_regular)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
        ) {
            LabeledValueRow(
                label = "Когда",
                value = "15 марта"
            )
            LabeledValueRow(
                label = "Где",
                value = "Парк"
            )
            LabeledValueRow(
                label = "Адрес",
                value = "г. Москва, парк Горького, Центральная аллея, д. 1"
            )
        }
    }
}
