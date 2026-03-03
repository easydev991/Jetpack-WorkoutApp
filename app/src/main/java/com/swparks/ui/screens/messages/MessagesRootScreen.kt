package com.swparks.ui.screens.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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

/**
 * Callbacks для навигации в экране сообщений
 */
data class MessagesNavigationCallbacks(
    val onShowLoginSheet: () -> Unit,
    val onShowRegisterSheet: () -> Unit,
    val onNavigateToFriends: () -> Unit,
    val onNavigateToSearchUsers: () -> Unit,
    val onNavigateToChat: (dialogId: Long, userId: Int, userName: String, userImage: String?) -> Unit
)

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
    callbacks: MessagesNavigationCallbacks
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val isLoadingDialogs by viewModel.isLoadingDialogs.collectAsState()
    val isUpdating by viewModel.isUpdating.collectAsState()

    // Отладочное логирование
    LaunchedEffect(isLoadingDialogs, appState.isAuthorized, uiState) {
        android.util.Log.d(
            "MessagesRootScreen",
            "isLoadingDialogs=$isLoadingDialogs, isAuthorized=${appState.isAuthorized}, " +
                "uiState=${uiState.javaClass.simpleName}"
        )
    }

    // Состояние для диалога подтверждения удаления
    var showDeleteDialog by remember { mutableStateOf(false) }
    var dialogToDelete by remember { mutableStateOf<DialogEntity?>(null) }

    if (isLoadingDialogs) {
        // Загрузка диалогов после авторизации - показываем LoadingOverlayView
        LoadingOverlayView(modifier = modifier.fillMaxSize())
    } else if (!appState.isAuthorized) {
        // Экран для неавторизованного пользователя
        IncognitoProfileView(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(R.dimen.spacing_regular),
                    end = dimensionResource(R.dimen.spacing_regular)
                ),
            onClickAuth = callbacks.onShowLoginSheet,
            onClickRegister = callbacks.onShowRegisterSheet
        )
    } else {
        // Авторизованный пользователь
        DialogsContent(
            params = DialogsContentParams(
                uiState = uiState,
                isRefreshing = isRefreshing,
                isUpdating = isUpdating,
                syncError = syncError,
                currentUser = appState.currentUser,
                onRefresh = { viewModel.refresh() },
                onDismissSyncError = { viewModel.dismissSyncError() },
                onDialogClick = { dialog ->
                    viewModel.onDialogClick(dialog.id, dialog.anotherUserId)
                    callbacks.onNavigateToChat(
                        dialog.id,
                        dialog.anotherUserId ?: 0,
                        dialog.name ?: "",
                        dialog.image
                    )
                },
                onMarkAsRead = { dialogId, userId ->
                    viewModel.markDialogAsRead(dialogId, userId)
                },
                onDeleteClick = { dialog ->
                    dialogToDelete = dialog
                    showDeleteDialog = true
                },
                onNavigateToFriends = callbacks.onNavigateToFriends,
                onNavigateToSearchUsers = callbacks.onNavigateToSearchUsers
            )
        )
    }

    // Диалог подтверждения удаления
    if (showDeleteDialog && dialogToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                dialogToDelete = null
            },
            title = { Text(stringResource(R.string.delete_dialog_title)) },
            text = { Text(stringResource(R.string.delete_dialog_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        dialogToDelete?.let { viewModel.deleteDialog(it.id) }
                        showDeleteDialog = false
                        dialogToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        dialogToDelete = null
                    }
                ) {
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

    // Состояние контекстного меню - на уровне DialogsContent (вне PullToRefreshBox)
    var contextMenuItem by remember { mutableStateOf<DialogEntity?>(null) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }

    Box(modifier = modifier.fillMaxSize()) {
        when (params.uiState) {
            is DialogsUiState.Loading -> {
                // Показываем LoadingOverlayView при первой загрузке
                LoadingOverlayView()
            }

            is DialogsUiState.Success -> {
                // PullToRefreshBox для обновления
                val pullRefreshState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = params.isRefreshing,
                    onRefresh = params.onRefresh,
                    state = pullRefreshState,
                    modifier = Modifier.fillMaxSize(),
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullRefreshState,
                            isRefreshing = params.isRefreshing,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = dimensionResource(R.dimen.spacing_regular))
                        )
                    }
                ) {
                    if (params.uiState.dialogs.isEmpty()) {
                        // EmptyStateView с условием по друзьям (центрирование + scroll для pull to refresh)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyStateViewForDialogs(
                                currentUser = params.currentUser,
                                onNavigateToFriends = params.onNavigateToFriends,
                                onNavigateToSearchUsers = params.onNavigateToSearchUsers
                            )
                        }
                    } else {
                        // LazyColumn с DialogRowView
                        DialogsList(
                            dialogs = params.uiState.dialogs,
                            isRefreshing = params.isRefreshing || params.isUpdating,
                            onDialogClick = params.onDialogClick,
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
                    }
                }
            }

            is DialogsUiState.Error -> {
                // Показываем EmptyStateView с возможностью повтора (центрирование)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        text = params.uiState.message,
                        buttonTitle = stringResource(R.string.try_again_button),
                        onButtonClick = params.onRefresh
                    )
                }
            }
        }

        // Snackbar для ошибки синхронизации
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Индикатор загрузки при удалении или отметке диалога
        if (params.isUpdating) {
            LoadingOverlayView()
        }
    }

    // Контекстное меню рендерится ПОСЛЕ Box, на уровне DialogsContent (как в JetpackDays)
    contextMenuItem?.let { dialog ->
        DropdownMenu(
            expanded = true,
            onDismissRequest = { contextMenuItem = null },
            offset = menuOffset
        ) {
            // Mark as read - показываем только если есть непрочитанные сообщения
            if ((dialog.unreadCount ?: 0) > 0) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.mark_as_read)) },
                    onClick = {
                        contextMenuItem = null
                        dialog.anotherUserId?.let { userId ->
                            params.onMarkAsRead(dialog.id, userId)
                        }
                    },
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
                onClick = {
                    contextMenuItem = null
                    params.onDeleteClick(dialog)
                },
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

    // Показываем ошибку синхронизации
    LaunchedEffect(params.syncError) {
        params.syncError?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            params.onDismissSyncError()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesTopAppBar() {
    CenterAlignedTopAppBar(
        title = {
            Text(text = stringResource(id = R.string.messages))
        }
    )
}
