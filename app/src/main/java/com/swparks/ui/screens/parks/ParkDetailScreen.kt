@file:Suppress("LongMethod", "CyclomaticComplexMethod")

package com.swparks.ui.screens.parks

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.swparks.ui.screens.parks.sections.CommentItemAction
import com.swparks.ui.screens.parks.sections.CommentItemConfig
import com.swparks.ui.screens.parks.sections.ParkAddCommentButton
import com.swparks.ui.screens.parks.sections.ParkAuthorConfig
import com.swparks.ui.screens.parks.sections.ParkAuthorSection
import com.swparks.ui.screens.parks.sections.ParkCommentItem
import com.swparks.ui.screens.parks.sections.ParkHeaderAction
import com.swparks.ui.screens.parks.sections.ParkHeaderMapSection
import com.swparks.ui.screens.parks.sections.ParkParticipantsSection
import com.swparks.ui.screens.parks.sections.ParkPhotosSection
import com.swparks.ui.screens.photos.PhotoDetailSheetHost
import com.swparks.ui.state.ParkDetailUIState
import com.swparks.ui.state.PhotoDetailConfig
import com.swparks.ui.state.PhotoOwner
import com.swparks.ui.viewmodel.IParkDetailViewModel
import com.swparks.ui.viewmodel.ParkDetailEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkDetailScreen(
    viewModel: IParkDetailViewModel,
    source: String = "parks",
    onBack: () -> Unit,
    onParkDeleted: (Long) -> Unit,
    onNavigateToUserProfile: (Long) -> Unit,
    onNavigateToTrainees: (Long, String, List<User>) -> Unit,
    onNavigateToCreateEvent: (Long, String) -> Unit,
    parentPaddingValues: PaddingValues
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isAuthorized by viewModel.isAuthorized.collectAsState()
    val isParkAuthor by viewModel.isParkAuthor.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()

    var showDeleteParkDialog by remember { mutableStateOf(false) }
    var showDeletePhotoDialog by remember { mutableStateOf(false) }
    var showDeleteCommentDialog by remember { mutableStateOf(false) }
    var showTextEntrySheet by remember { mutableStateOf(false) }
    var textEntryMode by remember { mutableStateOf<TextEntryMode?>(null) }
    var showPhotoDetailSheet by remember { mutableStateOf(false) }
    var photoDetailConfig by remember { mutableStateOf<PhotoDetailConfig?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ParkDetailEvent.ShowDeleteConfirmDialog -> showDeleteParkDialog = true
                is ParkDetailEvent.ShowDeletePhotoConfirmDialog -> showDeletePhotoDialog = true
                ParkDetailEvent.ShowDeleteCommentConfirmDialog -> showDeleteCommentDialog = true
                is ParkDetailEvent.ParkDeleted -> onParkDeleted(event.parkId)
                is ParkDetailEvent.PhotoDeleted -> Unit
                is ParkDetailEvent.OpenMap -> {
                    viewModel.mapUriSet?.let { mapUriSet ->
                        val intent = Intent(Intent.ACTION_VIEW, mapUriSet.geoUri)
                        try {
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, mapUriSet.browserUri))
                        }
                    }
                }

                is ParkDetailEvent.BuildRoute -> {
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

                is ParkDetailEvent.NavigateToTrainees -> {
                    onNavigateToTrainees(event.parkId, source, event.users)
                }

                is ParkDetailEvent.NavigateToCreateEvent -> {
                    onNavigateToCreateEvent(event.parkId, event.parkName)
                }

                is ParkDetailEvent.SendCommentComplaint -> {
                    sendComplaint(
                        complaint = event.complaint,
                        context = context
                    )
                }

                is ParkDetailEvent.OpenCommentTextEntry -> {
                    textEntryMode = event.mode
                    showTextEntrySheet = true
                }

                is ParkDetailEvent.NavigateToPhotoDetail -> {
                    photoDetailConfig = PhotoDetailConfig(
                        photoId = event.photo.id,
                        parentId = event.parkId,
                        parentTitle = event.parkTitle,
                        isAuthor = event.isParkAuthor,
                        photoUrl = event.photo.photo,
                        ownerType = PhotoOwner.Park
                    )
                    showPhotoDetailSheet = true
                }

                is ParkDetailEvent.ParkUpdated -> Unit
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.park_title)) },
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
                    if (state is ParkDetailUIState.Content) {
                        ParkShareButton(
                            isRefreshing = isRefreshing,
                            onShareClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, state.park.shareLinkStringURL)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, null))
                            }
                        )
                    }
                    if (isAuthorized && isParkAuthor) {
                        ParkAuthorActionsButton(
                            isRefreshing = isRefreshing,
                            onEditClick = viewModel::onEditClick,
                            onDeleteClick = viewModel::onDeleteClick
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            ParkDetailUIState.InitialLoading -> LoadingOverlayView(
                modifier = Modifier
                    .padding(parentPaddingValues)
                    .padding(paddingValues)
            )

            is ParkDetailUIState.Error -> ErrorContentView(
                modifier = Modifier
                    .padding(parentPaddingValues)
                    .padding(paddingValues),
                message = state.message,
                retryAction = viewModel::refresh
            )

            is ParkDetailUIState.Content -> {
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
                            ParkHeaderMapSection(
                                park = state.park,
                                address = state.address,
                                isAuthorized = isAuthorized,
                                isRefreshing = isRefreshing,
                                onAction = { action ->
                                    when (action) {
                                        ParkHeaderAction.OpenMap -> viewModel.onOpenMapClick()
                                        ParkHeaderAction.Route -> viewModel.onRouteClick()
                                        ParkHeaderAction.CreateEvent -> viewModel.onCreateEventClick()
                                    }
                                }
                            )
                        }

                        item {
                            ParkParticipantsSection(
                                park = state.park,
                                isAuthorized = isAuthorized,
                                isRefreshing = isRefreshing,
                                onParticipantToggle = viewModel::onTrainHereToggle,
                                onClickParticipants = viewModel::onTraineesCountClick
                            )
                        }

                        val photos = state.park.photos.orEmpty()
                        if (photos.isNotEmpty()) {
                            item {
                                ParkPhotosSection(
                                    photos = photos,
                                    isRefreshing = isRefreshing,
                                    onPhotoClick = viewModel::onPhotoClick
                                )
                            }
                        }

                        item {
                            ParkAuthorSection(
                                park = state.park,
                                address = state.authorAddress,
                                config = ParkAuthorConfig(
                                    isAuthorized = isAuthorized,
                                    isRefreshing = isRefreshing,
                                    isParkAuthor = isParkAuthor
                                ),
                                onAuthorClick = onNavigateToUserProfile
                            )
                        }

                        itemsIndexed(
                            state.park.comments.orEmpty(),
                            key = { _, comment -> comment.id }
                        ) { index, comment ->
                            ParkCommentItem(
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
                            ParkAddCommentButton(
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

    if (showDeleteParkDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteParkDialog = false
                viewModel.onDeleteDismiss()
            },
            title = { Text(text = stringResource(R.string.delete_ground)) },
            confirmButton = {
                TextButton(
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = {
                        showDeleteParkDialog = false
                        viewModel.onDeleteConfirm()
                    }
                ) {
                    Text(text = stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteParkDialog = false
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
            show = true,
            mode = checkNotNull(textEntryMode),
            onDismissed = { showTextEntrySheet = false },
            onSendSuccess = {
                showTextEntrySheet = false
                viewModel.refresh()
            }
        )
    }

    val config = photoDetailConfig
    if (showPhotoDetailSheet && config != null) {
        PhotoDetailSheetHost(
            show = true,
            config = config,
            onDismissed = { deletedPhotoId ->
                showPhotoDetailSheet = false
                photoDetailConfig = null
                if (deletedPhotoId != null) {
                    viewModel.onPhotoDeleted(deletedPhotoId)
                }
            }
        )
    }
}

@Composable
private fun ParkShareButton(
    isRefreshing: Boolean,
    onShareClick: () -> Unit
) {
    IconButton(
        onClick = onShareClick,
        enabled = !isRefreshing
    ) {
        Icon(
            imageVector = Icons.Filled.Share,
            contentDescription = stringResource(R.string.park_share)
        )
    }
}

@Composable
private fun ParkAuthorActionsButton(
    isRefreshing: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    androidx.compose.foundation.layout.Box {
        IconButton(
            onClick = { showMenu = true },
            enabled = !isRefreshing
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.park_actions)
            )
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.edit)) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null
                    )
                },
                onClick = {
                    showMenu = false
                    onEditClick()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    showMenu = false
                    onDeleteClick()
                }
            )
        }
    }
}
