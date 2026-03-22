package com.swparks.ui.screens.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.window.DialogProperties
import com.swparks.R
import com.swparks.data.database.entity.DialogEntity
import com.swparks.navigation.AppState
import com.swparks.ui.ds.DialogRowData
import com.swparks.ui.ds.DialogRowView
import com.swparks.ui.ds.EmptyStateView
import com.swparks.ui.ds.IncognitoProfileView
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.state.DialogsUiState
import com.swparks.ui.viewmodel.IDialogsViewModel
import com.swparks.util.DateFormatter
import com.swparks.util.parseHtmlOrNull

sealed class MessagesNavigationAction {
    object ShowLoginSheet : MessagesNavigationAction()
    object ShowRegisterSheet : MessagesNavigationAction()
    object NavigateToFriends : MessagesNavigationAction()
    object NavigateToSearchUsers : MessagesNavigationAction()
    data class NavigateToChat(
        val dialogId: Long,
        val userId: Int,
        val userName: String,
        val userImage: String?
    ) : MessagesNavigationAction()
}

/**
 * Параметры для DialogsContent
 */
data class DialogsContentParams(
    val uiState: DialogsUiState,
    val isRefreshing: Boolean,
    val isUpdating: Boolean,
    val syncError: String?,
    val currentUser: com.swparks.data.model.User?,
    val onRefresh: () -> Unit,
    val onDismissSyncError: () -> Unit,
    val onDialogClick: (DialogEntity) -> Unit,
    val onMarkAsRead: (Long, Int) -> Unit,
    val onDeleteClick: (DialogEntity) -> Unit,
    val onNavigateToFriends: () -> Unit,
    val onNavigateToSearchUsers: () -> Unit
)

@Composable
fun MessagesRootScreen(
    modifier: Modifier = Modifier,
    viewModel: IDialogsViewModel,
    appState: AppState,
    onAction: (MessagesNavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val isLoadingDialogs by viewModel.isLoadingDialogs.collectAsState()
    val isUpdating by viewModel.isUpdating.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var dialogToDelete by remember { mutableStateOf<DialogEntity?>(null) }

    if (isLoadingDialogs) {
        LoadingOverlayView(modifier = modifier.fillMaxSize())
    } else if (!appState.isAuthorized) {
        IncognitoProfileView(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(R.dimen.spacing_regular),
                    end = dimensionResource(R.dimen.spacing_regular)
                ),
            onClickAuth = { onAction(MessagesNavigationAction.ShowLoginSheet) },
            onClickRegister = { onAction(MessagesNavigationAction.ShowRegisterSheet) }
        )
    } else {
        val params = createDialogsContentParams(
            uiState = uiState,
            isRefreshing = isRefreshing,
            isUpdating = isUpdating,
            syncError = syncError,
            currentUser = appState.currentUser,
            viewModel = viewModel,
            onAction = onAction,
            onDeleteClick = { dialog ->
                dialogToDelete = dialog
                showDeleteDialog = true
            }
        )
        DialogsContent(modifier = modifier, params = params)
    }

    if (showDeleteDialog) {
        DeleteDialogConfirmation(
            dialogToDelete = dialogToDelete,
            onConfirm = {
                dialogToDelete?.let { viewModel.deleteDialog(it.id) }
                showDeleteDialog = false
                dialogToDelete = null
            },
            onDismiss = {
                showDeleteDialog = false
                dialogToDelete = null
            }
        )
    }
}


internal fun createDialogsContentParams(
    uiState: DialogsUiState,
    isRefreshing: Boolean,
    isUpdating: Boolean,
    syncError: String?,
    currentUser: com.swparks.data.model.User?,
    viewModel: IDialogsViewModel,
    onAction: (MessagesNavigationAction) -> Unit,
    onDeleteClick: (DialogEntity) -> Unit
): DialogsContentParams {
    return DialogsContentParams(
        uiState = uiState,
        isRefreshing = isRefreshing,
        isUpdating = isUpdating,
        syncError = syncError,
        currentUser = currentUser,
        onRefresh = { viewModel.refresh() },
        onDismissSyncError = { viewModel.dismissSyncError() },
        onDialogClick = { dialog ->
            viewModel.onDialogClick(dialog.id, dialog.anotherUserId)
            onAction(
                MessagesNavigationAction.NavigateToChat(
                    dialogId = dialog.id,
                    userId = dialog.anotherUserId ?: 0,
                    userName = dialog.name ?: "",
                    userImage = dialog.image
                )
            )
        },
        onMarkAsRead = { dialogId, userId ->
            viewModel.markDialogAsRead(dialogId, userId)
        },
        onDeleteClick = onDeleteClick,
        onNavigateToFriends = { onAction(MessagesNavigationAction.NavigateToFriends) },
        onNavigateToSearchUsers = { onAction(MessagesNavigationAction.NavigateToSearchUsers) }
    )
}

@Composable
private fun DeleteDialogConfirmation(
    dialogToDelete: DialogEntity?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (dialogToDelete != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.delete_dialog_title)) },
            text = { Text(stringResource(R.string.delete_dialog_message)) },
            confirmButton = {
                TextButton(
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = onConfirm
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogsContent(
    modifier: Modifier = Modifier,
    params: DialogsContentParams
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val density = LocalDensity.current

    var contextMenuItem by remember { mutableStateOf<DialogEntity?>(null) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }

    Box(modifier = modifier.fillMaxSize()) {
        DialogsStateContent(
            uiState = params.uiState,
            isRefreshing = params.isRefreshing,
            isUpdating = params.isUpdating,
            currentUser = params.currentUser,
            onRefresh = params.onRefresh,
            onDialogClick = params.onDialogClick,
            onNavigateToFriends = params.onNavigateToFriends,
            onNavigateToSearchUsers = params.onNavigateToSearchUsers,
            onLongClick = { dialog, localOffset, itemPosition ->
                contextMenuItem = dialog
                menuOffset = with(density) {
                    DpOffset(
                        (itemPosition.x + localOffset.x).toDp(),
                        (itemPosition.y + localOffset.y).toDp()
                    )
                }
            }
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (params.uiState is DialogsUiState.Success && params.uiState.dialogs.isNotEmpty()) {
            val hasFriends = params.currentUser?.hasFriends ?: false
            NewDialogFab(
                enabled = !params.isRefreshing,
                hasFriends = hasFriends,
                onNavigateToFriends = params.onNavigateToFriends,
                onNavigateToSearchUsers = params.onNavigateToSearchUsers
            )
        }

        if (params.isUpdating) LoadingOverlayView()
    }

    DialogContextMenu(
        contextMenuItem = contextMenuItem,
        menuOffset = menuOffset,
        onDismiss = { contextMenuItem = null },
        onMarkAsRead = { dialog ->
            contextMenuItem = null
            dialog.anotherUserId?.let { userId ->
                params.onMarkAsRead(dialog.id, userId)
            }
        },
        onDeleteClick = { dialog ->
            contextMenuItem = null
            params.onDeleteClick(dialog)
        }
    )

    HandleSyncError(
        syncError = params.syncError,
        snackbarHostState = snackbarHostState,
        onDismissSyncError = params.onDismissSyncError
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogsStateContent(
    uiState: DialogsUiState,
    isRefreshing: Boolean,
    isUpdating: Boolean,
    currentUser: com.swparks.data.model.User?,
    onRefresh: () -> Unit,
    onDialogClick: (DialogEntity) -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToSearchUsers: () -> Unit,
    onLongClick: (DialogEntity, Offset, Offset) -> Unit
) {
    when (uiState) {
        is DialogsUiState.Loading -> {
            LoadingOverlayView()
        }

        is DialogsUiState.Success -> {
            val pullRefreshState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                state = pullRefreshState,
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullRefreshState,
                        isRefreshing = isRefreshing,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = dimensionResource(R.dimen.spacing_regular))
                    )
                }
            ) {
                if (uiState.dialogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateViewForDialogs(
                            currentUser = currentUser,
                            onNavigateToFriends = onNavigateToFriends,
                            onNavigateToSearchUsers = onNavigateToSearchUsers
                        )
                    }
                } else {
                    DialogsList(
                        dialogs = uiState.dialogs,
                        isRefreshing = isRefreshing || isUpdating,
                        onDialogClick = onDialogClick,
                        onLongClick = onLongClick
                    )
                }
            }
        }

        is DialogsUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    text = uiState.message,
                    buttonTitle = stringResource(R.string.try_again_button),
                    onButtonClick = onRefresh
                )
            }
        }
    }
}

@Composable
private fun DialogContextMenu(
    contextMenuItem: DialogEntity?,
    menuOffset: DpOffset,
    onDismiss: () -> Unit,
    onMarkAsRead: (DialogEntity) -> Unit,
    onDeleteClick: (DialogEntity) -> Unit
) {
    contextMenuItem?.let { dialog ->
        DropdownMenu(
            expanded = true,
            onDismissRequest = onDismiss,
            offset = menuOffset
        ) {
            if ((dialog.unreadCount ?: 0) > 0) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.mark_as_read)) },
                    onClick = { onMarkAsRead(dialog) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.MarkEmailRead,
                            contentDescription = null
                        )
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
                onClick = { onDeleteClick(dialog) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
        }
    }
}

@Composable
private fun HandleSyncError(
    syncError: String?,
    snackbarHostState: SnackbarHostState,
    onDismissSyncError: () -> Unit
) {
    LaunchedEffect(syncError) {
        syncError?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            onDismissSyncError()
        }
    }
}

/**
 * EmptyStateView для экрана диалогов с условной кнопкой
 */
@Composable
private fun EmptyStateViewForDialogs(
    currentUser: com.swparks.data.model.User?,
    onNavigateToFriends: () -> Unit,
    onNavigateToSearchUsers: () -> Unit
) {
    val hasFriends = currentUser?.hasFriends ?: false

    EmptyStateView(
        text = stringResource(R.string.dialogs_empty),
        buttonTitle = if (hasFriends) {
            stringResource(R.string.dialogs_open_friends)
        } else {
            stringResource(R.string.dialogs_find_user)
        },
        onButtonClick = if (hasFriends) {
            onNavigateToFriends
        } else {
            onNavigateToSearchUsers
        }
    )
}

@Composable
fun DialogsList(
    dialogs: List<DialogEntity>,
    isRefreshing: Boolean,
    onDialogClick: (DialogEntity) -> Unit,
    onLongClick: (DialogEntity, Offset, Offset) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        contentPadding = PaddingValues(
            start = dimensionResource(R.dimen.spacing_regular),
            end = dimensionResource(R.dimen.spacing_regular),
            top = dimensionResource(R.dimen.spacing_small),
            bottom = dimensionResource(R.dimen.spacing_regular)
        ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
    ) {
        items(dialogs) { dialog ->
            // Форматируем дату в читаемый вид
            val formattedDate = DateFormatter.formatDate(
                context = context,
                dateString = dialog.lastMessageDate,
                showTimeInThisYear = true
            )

            DialogRowView(
                data = DialogRowData(
                    imageStringURL = dialog.image,
                    // Fallback для пустого имени - показываем "Пользователь"
                    authorName = dialog.name?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.default_user_name),
                    dateString = formattedDate,
                    bodyText = dialog.lastMessageText.parseHtmlOrNull(compactMode = true) ?: "",
                    unreadCount = dialog.unreadCount,
                    enabled = !isRefreshing,
                    onLongClick = { localOffset, itemPosition ->
                        onLongClick(dialog, localOffset, itemPosition)
                    }
                ),
                onClick = {
                    onDialogClick(dialog)
                }
            )
        }
    }
}

@Composable
private fun BoxScope.NewDialogFab(
    enabled: Boolean,
    hasFriends: Boolean,
    onNavigateToFriends: () -> Unit,
    onNavigateToSearchUsers: () -> Unit
) {
    FloatingActionButton(
        onClick = {
            if (enabled) {
                if (hasFriends) {
                    onNavigateToFriends()
                } else {
                    onNavigateToSearchUsers()
                }
            }
        },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(
                end = dimensionResource(R.dimen.spacing_regular),
                bottom = dimensionResource(R.dimen.spacing_regular)
            )
            .testTag("NewDialogFAB")
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(R.string.dialogs_fab_description)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesTopAppBar() {
    CenterAlignedTopAppBar(
        title = {
            Text(text = stringResource(id = R.string.messages))
        }
    )
}
