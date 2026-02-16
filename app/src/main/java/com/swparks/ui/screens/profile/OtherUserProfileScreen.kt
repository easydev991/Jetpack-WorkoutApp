@file:Suppress("UnusedPrivateMember")

package com.swparks.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.swparks.R
import com.swparks.data.model.City
import com.swparks.data.model.Country
import com.swparks.data.model.User
import com.swparks.domain.model.FriendAction
import com.swparks.navigation.AppState
import com.swparks.navigation.Screen
import com.swparks.ui.ds.ButtonConfig
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.ds.SWButton
import com.swparks.ui.ds.SWButtonMode
import com.swparks.ui.ds.UserProfileCardView
import com.swparks.ui.ds.UserProfileData
import com.swparks.ui.model.BlacklistAction
import com.swparks.ui.model.TextEntryMode
import com.swparks.ui.screen.profile.AddedParksButton
import com.swparks.ui.screen.profile.FriendsButton
import com.swparks.ui.screen.profile.JournalsButton
import com.swparks.ui.screen.profile.UsedParksButton
import com.swparks.ui.screens.common.TextEntrySheetHost
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.IOtherUserProfileViewModel
import com.swparks.ui.viewmodel.OtherUserProfileUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherUserProfileScreen(
    viewModel: IOtherUserProfileViewModel,
    appState: AppState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onNavigateToOwnProfile: () -> Unit = {}
) {
    val viewedUser by viewModel.viewedUser.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val blacklist by viewModel.blacklist.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoadingCurrentUser by viewModel.isLoadingCurrentUser.collectAsState()

    // Проверки отношений между currentUser и viewedUser
    val viewedUserId = viewedUser?.id
    val currentUserId = currentUser?.id
    val isFriend = viewedUserId != null && friends.any { it.id == viewedUserId }
    val isInBlacklist = viewedUserId != null && blacklist.any { it.id == viewedUserId }

    // Защита от просмотра собственного профиля (если попали через deep link)
    if (viewedUserId != null && viewedUserId == currentUserId) {
        LaunchedEffect(viewedUserId) {
            // Навигация на собственный профиль вместо onBack (пустой back stack)
            onNavigateToOwnProfile()
        }
        return
    }

    var showBlacklistDialog by remember { mutableStateOf(false) }
    val blacklistAction = if (isInBlacklist) BlacklistAction.UNBLOCK else BlacklistAction.BLOCK

    // Состояние для TextEntrySheet (отправка сообщения)
    var showTextEntrySheet by remember { mutableStateOf(false) }
    var textEntryMode by remember { mutableStateOf<TextEntryMode?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            OtherUserProfileTopAppBar(
                isInBlacklist = isInBlacklist,
                onBlacklistClick = { showBlacklistDialog = true },
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoadingCurrentUser) {
                LoadingOverlayView(modifier = Modifier.fillMaxSize())
            } else {
                val pullRefreshState = rememberPullToRefreshState()

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refreshUser() },
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
                    when (val state = uiState) {
                        is OtherUserProfileUiState.Loading -> { /* Контент скрыт */
                        }

                        is OtherUserProfileUiState.UserNotFound -> UserNotFoundContent(onBack = onBack)
                        is OtherUserProfileUiState.BlockedByUser -> BlockedByUserContent(onBack = onBack)
                        is OtherUserProfileUiState.Success -> {
                            ProfileContent(
                                viewedUser = viewedUser,
                                country = state.country,
                                city = state.city,
                                isFriend = isFriend,
                                isInBlacklist = isInBlacklist,
                                isRefreshing = isRefreshing,
                                viewModel = viewModel,
                                appState = appState,
                                onSendMessageClick = { user ->
                                    textEntryMode =
                                        TextEntryMode.Message(user.id, user.fullName ?: user.name)
                                    showTextEntrySheet = true
                                }
                            )
                        }

                        is OtherUserProfileUiState.Error -> {
                            ErrorContent(
                                message = state.message,
                                canRetry = state.canRetry,
                                onRetry = { viewModel.loadUser() }
                            )
                        }
                    }
                }

                if (uiState is OtherUserProfileUiState.Loading && !isRefreshing) {
                    LoadingOverlayView(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }

    if (showBlacklistDialog) {
        BlacklistActionDialog(
            action = blacklistAction,
            onConfirm = {
                viewModel.performBlacklistAction(onBlocked = onBack)
                showBlacklistDialog = false
            },
            onDismiss = { showBlacklistDialog = false }
        )
    }

    // Sheet для отправки сообщения
    if (showTextEntrySheet && textEntryMode != null) {
        TextEntrySheetHost(
            show = true,
            mode = textEntryMode!!,
            onDismissed = {
                showTextEntrySheet = false
                textEntryMode = null
            },
            onSendSuccess = {
                showTextEntrySheet = false
                textEntryMode = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OtherUserProfileTopAppBar(
    isInBlacklist: Boolean,
    onBlacklistClick: () -> Unit,
    onBackClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = { Text(stringResource(R.string.profile)) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.close_button_content_description)
                )
            }
        },
        actions = {
            IconButton(onClick = onBlacklistClick) {
                Icon(
                    imageVector = if (isInBlacklist) Icons.Outlined.CheckCircle else Icons.Outlined.Block,
                    contentDescription = stringResource(if (isInBlacklist) R.string.unblock else R.string.block)
                )
            }
        }
    )
}

@Composable
internal fun BlacklistActionDialog(
    action: BlacklistAction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(action.description)) },
        text = { Text(stringResource(action.alertMessageDetailed)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (action == BlacklistAction.BLOCK) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            ) { Text(stringResource(action.description)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        }
    )
}

@Composable
private fun ProfileContent(
    viewedUser: User?,
    country: Country?,
    city: City?,
    isFriend: Boolean,
    isInBlacklist: Boolean,
    isRefreshing: Boolean,
    viewModel: IOtherUserProfileViewModel,
    appState: AppState,
    onSendMessageClick: (User) -> Unit
) {
    if (viewedUser == null) return

    // Кнопки недоступны если пользователь заблокирован или идет обновление
    val buttonsEnabled = !isRefreshing && !isInBlacklist

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dimensionResource(R.dimen.spacing_regular)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
    ) {
        val shortAddress = listOfNotNull(country?.name, city?.name).joinToString(", ")
        UserProfileCardView(
            data = UserProfileData(
                imageStringURL = viewedUser.image,
                userName = viewedUser.fullName ?: viewedUser.name,
                gender = viewedUser.genderOption?.let { stringResource(it.description) } ?: "",
                age = viewedUser.age,
                shortAddress = shortAddress
            )
        )

        SendMessageButton(
            onClick = { onSendMessageClick(viewedUser) },
            enabled = buttonsEnabled
        )

        val friendAction =
            if (isFriend) FriendAction.REMOVE_FRIEND else FriendAction.SEND_FRIEND_REQUEST
        FriendActionButton(
            action = friendAction,
            onClick = { viewModel.performFriendAction() },
            enabled = buttonsEnabled
        )

        // Для чужого профиля friendRequestsCount всегда 0
        if (viewedUser.hasFriends) {
            FriendsButton(
                friendsCount = viewedUser.friendsCount ?: 0,
                friendRequestsCount = 0,
                onClick = {
                    appState.navController.navigate(
                        Screen.UserFriends.createRoute(
                            viewedUser.id
                        )
                    )
                },
                enabled = !isRefreshing
            )
        }

        if (viewedUser.hasUsedParks) {
            UsedParksButton(
                parksCount = viewedUser.parksCount?.toIntOrNull() ?: 0,
                onClick = {
                    appState.navController.navigate(
                        Screen.UserTrainingParks.createRoute(
                            viewedUser.id
                        )
                    )
                },
                enabled = !isRefreshing
            )
        }

        if (viewedUser.hasAddedParks) {
            AddedParksButton(
                addedParksCount = viewedUser.addedParks?.size ?: 0,
                onClick = { appState.navController.navigate(Screen.UserParks.createRoute(viewedUser.id)) },
                enabled = !isRefreshing
            )
        }

        if ((viewedUser.journalCount ?: 0) > 0) {
            JournalsButton(
                journalsCount = viewedUser.journalCount ?: 0,
                onClick = {
                    appState.navController.navigate(
                        Screen.JournalsList.createRoute(
                            viewedUser.id
                        )
                    )
                },
                enabled = !isRefreshing
            )
        }
    }
}

@Composable
internal fun SendMessageButton(onClick: () -> Unit, enabled: Boolean) {
    SWButton(
        config = ButtonConfig(
            modifier = Modifier.fillMaxWidth(),
            mode = SWButtonMode.FILLED,
            imageVector = Icons.AutoMirrored.Filled.Send,
            text = stringResource(R.string.message),
            enabled = enabled,
            onClick = onClick
        )
    )
}

@Composable
private fun FriendActionButton(action: FriendAction, onClick: () -> Unit, enabled: Boolean) {
    val icon = when (action) {
        FriendAction.SEND_FRIEND_REQUEST -> Icons.Outlined.PersonAdd
        FriendAction.REMOVE_FRIEND -> Icons.Outlined.PersonRemove
    }
    SWButton(
        config = ButtonConfig(
            modifier = Modifier.fillMaxWidth(),
            mode = SWButtonMode.TINTED,
            imageVector = icon,
            text = stringResource(action.description),
            enabled = enabled,
            onClick = onClick
        )
    )
}

@Composable
internal fun UserNotFoundContent(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.spacing_regular)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.user_not_found),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_regular)))
        Button(onClick = onBack) { Text(stringResource(R.string.go_back)) }
    }
}

@Composable
internal fun BlockedByUserContent(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.spacing_regular)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.blocked_by_user),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_regular)))
        Button(onClick = onBack) { Text(stringResource(R.string.go_back)) }
    }
}

@Composable
internal fun ErrorContent(message: String, canRetry: Boolean, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.spacing_regular)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_regular)))
        if (canRetry) {
            Button(onClick = onRetry) { Text(stringResource(R.string.try_again_button)) }
        }
    }
}

// === Previews ===

@Preview(showBackground = true)
@Composable
private fun SendMessageButtonPreview() {
    JetpackWorkoutAppTheme {
        SendMessageButton(onClick = {}, enabled = true)
    }
}

@Preview(showBackground = true)
@Composable
private fun FriendActionButtonPreview() {
    JetpackWorkoutAppTheme {
        Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))) {
            FriendActionButton(
                action = FriendAction.SEND_FRIEND_REQUEST,
                onClick = {},
                enabled = true
            )
            FriendActionButton(action = FriendAction.REMOVE_FRIEND, onClick = {}, enabled = true)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UserNotFoundContentPreview() {
    JetpackWorkoutAppTheme {
        UserNotFoundContent(onBack = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun BlockedByUserContentPreview() {
    JetpackWorkoutAppTheme {
        BlockedByUserContent(onBack = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun BlacklistActionDialogPreview() {
    JetpackWorkoutAppTheme {
        Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))) {
            BlacklistActionDialog(action = BlacklistAction.BLOCK, onConfirm = {}, onDismiss = {})
            BlacklistActionDialog(action = BlacklistAction.UNBLOCK, onConfirm = {}, onDismiss = {})
        }
    }
}
