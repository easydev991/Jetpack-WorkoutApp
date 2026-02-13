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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
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

@Composable
fun MessagesRootScreen(
    modifier: Modifier = Modifier,
    viewModel: IDialogsViewModel,
    appState: AppState,
    onShowLoginSheet: () -> Unit,
    onNavigateToFriends: () -> Unit = {},
    onNavigateToSearchUsers: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val isLoadingDialogs by viewModel.isLoadingDialogs.collectAsState()

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
            onClickAuth = onShowLoginSheet
        )
    } else {
        // Авторизованный пользователь
        DialogsContent(
            modifier = modifier,
            uiState = uiState,
            isRefreshing = isRefreshing,
            syncError = syncError,
            currentUser = appState.currentUser,
            onRefresh = { viewModel.refresh() },
            onDismissSyncError = { viewModel.dismissSyncError() },
            onDialogClick = { dialogId, userId ->
                viewModel.onDialogClick(dialogId, userId)
            },
            onNavigateToFriends = onNavigateToFriends,
            onNavigateToSearchUsers = onNavigateToSearchUsers
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogsContent(
    modifier: Modifier = Modifier,
    uiState: DialogsUiState,
    isRefreshing: Boolean,
    syncError: String?,
    currentUser: com.swparks.data.model.User?,
    onRefresh: () -> Unit,
    onDismissSyncError: () -> Unit,
    onDialogClick: (Long, Int?) -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToSearchUsers: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is DialogsUiState.Loading -> {
                // Показываем LoadingOverlayView при первой загрузке
                LoadingOverlayView()
            }

            is DialogsUiState.Success -> {
                // PullToRefreshBox для обновления
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
                        // EmptyStateView с условием по друзьям (центрирование + scroll для pull to refresh)
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
                        // LazyColumn с DialogRowView
                        DialogsList(
                            dialogs = uiState.dialogs,
                            isRefreshing = isRefreshing,
                            onDialogClick = onDialogClick
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
                        text = uiState.message,
                        buttonTitle = stringResource(R.string.try_again_button),
                        onButtonClick = onRefresh
                    )
                }
            }
        }

        // Snackbar для ошибки синхронизации
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Показываем ошибку синхронизации
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
    onDialogClick: (Long, Int?) -> Unit
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
                    bodyText = dialog.lastMessageText ?: "",
                    unreadCount = dialog.unreadCount,
                    enabled = !isRefreshing
                ),
                onClick = {
                    onDialogClick(dialog.id, dialog.anotherUserId)
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
