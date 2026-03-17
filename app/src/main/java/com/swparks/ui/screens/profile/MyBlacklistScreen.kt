package com.swparks.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.swparks.R
import com.swparks.data.model.User
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.ds.UserRowData
import com.swparks.ui.ds.UserRowView
import com.swparks.ui.state.BlacklistAction
import com.swparks.ui.state.BlacklistUiState
import com.swparks.ui.viewmodel.IBlacklistViewModel
import com.swparks.ui.model.BlacklistAction as ApiBlacklistAction

/**
 * Экран черного списка текущего пользователя
 *
 * @param modifier Модификатор
 * @param viewModel ViewModel для управления состоянием экрана
 * @param onBackClick Callback для навигации назад
 * @param parentPaddingValues Паддинги для учета BottomNavigationBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBlacklistScreen(
    modifier: Modifier = Modifier,
    viewModel: IBlacklistViewModel,
    onBackClick: () -> Unit,
    parentPaddingValues: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsState()

    MyBlacklistScreenContent(
        modifier = modifier,
        uiState = uiState,
        parentPaddingValues = parentPaddingValues,
        onAction = { action ->
            when (action) {
                BlacklistAction.Back -> onBackClick()
                else -> viewModel.onAction(action)
            }
        }
    )
}

/**
 * Stateless контент экрана черного списка
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBlacklistScreenContent(
    modifier: Modifier = Modifier,
    uiState: BlacklistUiState,
    parentPaddingValues: PaddingValues,
    onAction: (BlacklistAction) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.black_list))
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(BlacklistAction.Back) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(parentPaddingValues)
                .padding(innerPadding)
        ) {
            when (uiState) {
                is BlacklistUiState.Loading -> {
                    LoadingOverlayView()
                }

                is BlacklistUiState.Error -> {
                    Text(
                        text = uiState.message,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is BlacklistUiState.Success -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        SuccessContent(
                            blacklist = uiState.blacklist,
                            onShowRemoveDialog = { onAction(BlacklistAction.ShowRemoveDialog(it)) },
                            enabled = !uiState.isLoading && !uiState.isRemoving
                        )
                        if (uiState.isLoading || uiState.isRemoving) {
                            LoadingOverlayView()
                        }
                    }
                }
            }
        }
    }

    RemoveDialogs(
        successState = uiState as? BlacklistUiState.Success,
        onAction = onAction
    )
}

/**
 * Контент с данными черного списка
 */
@Composable
private fun SuccessContent(
    modifier: Modifier = Modifier,
    blacklist: List<User>,
    onShowRemoveDialog: (User) -> Unit,
    enabled: Boolean = true
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = dimensionResource(R.dimen.spacing_regular),
            top = dimensionResource(R.dimen.spacing_small),
            end = dimensionResource(R.dimen.spacing_regular),
            bottom = dimensionResource(R.dimen.spacing_regular)
        ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
    ) {
        if (blacklist.isNotEmpty()) {
            items(count = blacklist.size) { index ->
                val user = blacklist[index]
                UserRowView(
                    data = UserRowData(
                        modifier = Modifier,
                        enabled = enabled,
                        imageStringURL = user.image,
                        name = user.name,
                        address = null,
                        onClick = { onShowRemoveDialog(user) }
                    )
                )
            }
        }

        if (blacklist.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dimensionResource(R.dimen.spacing_large)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.blacklist_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


@Composable
private fun RemoveDialogs(
    successState: BlacklistUiState.Success?,
    onAction: (BlacklistAction) -> Unit
) {
    if (successState?.showRemoveDialog == true && successState.itemToRemove != null) {
        AlertDialog(
            onDismissRequest = { onAction(BlacklistAction.CancelRemove) },
            text = {
                Text(stringResource(ApiBlacklistAction.UNBLOCK.alertMessage))
            },
            confirmButton = {
                TextButton(onClick = { onAction(BlacklistAction.Remove(successState.itemToRemove)) }) {
                    Text(stringResource(ApiBlacklistAction.UNBLOCK.description))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(BlacklistAction.CancelRemove) }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    if (successState?.showSuccessAlert == true) {
        AlertDialog(
            onDismissRequest = { onAction(BlacklistAction.DismissSuccessAlert) },
            title = {
                Text(stringResource(R.string.alert_reset_password_success_title))
            },
            text = {
                val userName = successState.unblockedUserName ?: ""
                Text(stringResource(R.string.user_unblocked_successfully, userName))
            },
            confirmButton = {
                TextButton(onClick = { onAction(BlacklistAction.DismissSuccessAlert) }) {
                    Text(stringResource(R.string.alert_ok))
                }
            }
        )
    }
}
