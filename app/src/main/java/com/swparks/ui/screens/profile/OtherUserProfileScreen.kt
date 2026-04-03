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
import androidx.compose.ui.platform.testTag
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
import com.swparks.navigation.navigateToUserAddedParks
import com.swparks.ui.ds.AddedParksButton
import com.swparks.ui.ds.ButtonConfig
import com.swparks.ui.ds.EmptyStateView
import com.swparks.ui.ds.FriendsButton
import com.swparks.ui.ds.JournalsButton
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.ds.SWButton
import com.swparks.ui.ds.SWButtonMode
import com.swparks.ui.ds.SWButtonSize
import com.swparks.ui.ds.UsedParksButton
import com.swparks.ui.ds.UserProfileCardView
import com.swparks.ui.ds.UserProfileData
import com.swparks.ui.model.BlacklistAction
import com.swparks.ui.model.TextEntryMode
import com.swparks.ui.screens.common.TextEntrySheetHost
import com.swparks.ui.testtags.ScreenshotTestTags
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.IOtherUserProfileViewModel
import com.swparks.ui.viewmodel.OtherUserProfileUiState

sealed class ProfileNavigationAction {
    object Back : ProfileNavigationAction()

    object NavigateToOwnProfile : ProfileNavigationAction()
}

data class ProfileContentState(
    val viewedUser: User?,
    val country: Country?,
    val city: City?,
    val isFriend: Boolean,
    val isInBlacklist: Boolean,
    val isRefreshing: Boolean,
    val isFriendActionLoading: Boolean
)

data class ProfileRelationsState(
    val isFriend: Boolean,
    val isInBlacklist: Boolean
)

private data class ProfileDialogState(
    val showBlacklistDialog: Boolean = false,
    val showRemoveFriendDialog: Boolean = false,
    val showTextEntrySheet: Boolean = false,
    val textEntryMode: TextEntryMode? = null
)

@Composable
private fun rememberProfileRelations(
    viewedUser: User?,
    currentUser: User?,
    friends: List<User>,
    blacklist: List<User>
): ProfileRelationsState {
    val viewedUserId = viewedUser?.id
    val currentUserId = currentUser?.id
    val isFriend = viewedUserId != null && friends.any { it.id == viewedUserId }
    val isInBlacklist = viewedUserId != null && blacklist.any { it.id == viewedUserId }

    return remember(viewedUserId, currentUserId, friends, blacklist) {
        ProfileRelationsState(isFriend, isInBlacklist)
    }
}

sealed class ProfileContentAction {
    data class SendMessage(
        val user: User
    ) : ProfileContentAction()

    object ShowRemoveFriendDialog : ProfileContentAction()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherUserProfileScreen(
    viewModel: IOtherUserProfileViewModel,
    appState: AppState,
    source: String = "profile",
    modifier: Modifier = Modifier,
    onAction: (ProfileNavigationAction) -> Unit = {}
) {
    val viewedUser by viewModel.viewedUser.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val blacklist by viewModel.blacklist.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoadingCurrentUser by viewModel.isLoadingCurrentUser.collectAsState()
    val isFriendActionLoading by viewModel.isFriendActionLoading.collectAsState()

    val relations = rememberProfileRelations(viewedUser, currentUser, friends, blacklist)

    if (viewedUser?.id != null && viewedUser?.id == currentUser?.id) {
        LaunchedEffect(viewedUser?.id) {
            onAction(ProfileNavigationAction.NavigateToOwnProfile)
        }
        return
    }

    OtherUserProfileScaffold(
        modifier = modifier,
        params =
            OtherUserProfileScaffoldParams(
                isLoadingCurrentUser = isLoadingCurrentUser,
                isRefreshing = isRefreshing,
                isFriendActionLoading = isFriendActionLoading,
                uiState = uiState,
                viewedUser = viewedUser,
                isFriend = relations.isFriend,
                isInBlacklist = relations.isInBlacklist
            ),
        config =
            ProfileContentConfig(
                viewModel = viewModel,
                appState = appState,
                source = source
            ),
        relations = relations,
        onAction = onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OtherUserProfileScaffold(
    modifier: Modifier,
    params: OtherUserProfileScaffoldParams,
    config: ProfileContentConfig,
    relations: ProfileRelationsState,
    onAction: (ProfileNavigationAction) -> Unit
) {
    var dialogState by remember { mutableStateOf(ProfileDialogState()) }
    val blacklistAction =
        if (relations.isInBlacklist) BlacklistAction.UNBLOCK else BlacklistAction.BLOCK

    Scaffold(
        modifier = modifier,
        topBar = {
            OtherUserProfileTopAppBar(
                isInBlacklist = relations.isInBlacklist,
                onBlacklistClick = { dialogState = dialogState.copy(showBlacklistDialog = true) },
                onBackClick = { onAction(ProfileNavigationAction.Back) }
            )
        }
    ) { paddingValues ->
        OtherUserProfileScaffoldContent(
            paddingValues = paddingValues,
            params = params,
            config = config,
            handlers =
                ProfileActionHandlers(
                    onProfileAction = onAction,
                    onTextEntryAction = { mode ->
                        dialogState =
                            dialogState.copy(showTextEntrySheet = true, textEntryMode = mode)
                    },
                    onShowRemoveFriendDialog = {
                        dialogState = dialogState.copy(showRemoveFriendDialog = true)
                    }
                )
        )
    }

    OtherUserProfileDialogs(
        state = dialogState,
        blacklistAction = blacklistAction,
        handlers =
            ProfileDialogHandlers(
                onBlacklistConfirm = {
                    config.viewModel.performBlacklistAction(onBlocked = {
                        onAction(
                            ProfileNavigationAction.Back
                        )
                    })
                    dialogState = dialogState.copy(showBlacklistDialog = false)
                },
                onBlacklistDismiss = {
                    dialogState = dialogState.copy(showBlacklistDialog = false)
                },
                onRemoveFriendConfirm = {
                    config.viewModel.performFriendAction()
                    dialogState = dialogState.copy(showRemoveFriendDialog = false)
                },
                onRemoveFriendDismiss = {
                    dialogState = dialogState.copy(showRemoveFriendDialog = false)
                },
                onTextEntryDismiss = {
                    dialogState = dialogState.copy(showTextEntrySheet = false, textEntryMode = null)
                },
                onTextEntrySuccess = {
                    dialogState = dialogState.copy(showTextEntrySheet = false, textEntryMode = null)
                }
            )
    )
}

private data class OtherUserProfileScaffoldParams(
    val isLoadingCurrentUser: Boolean,
    val isRefreshing: Boolean,
    val isFriendActionLoading: Boolean,
    val uiState: OtherUserProfileUiState,
    val viewedUser: User?,
    val isFriend: Boolean,
    val isInBlacklist: Boolean
)

private data class ProfileNavigationParams(
    val viewedUser: User,
    val appState: AppState,
    val source: String,
    val buttonsEnabled: Boolean
)

private data class ProfileContentConfig(
    val viewModel: IOtherUserProfileViewModel,
    val appState: AppState,
    val source: String
)

private data class ProfileActionHandlers(
    val onProfileAction: (ProfileNavigationAction) -> Unit,
    val onTextEntryAction: (TextEntryMode) -> Unit,
    val onShowRemoveFriendDialog: () -> Unit
)

private sealed class TextEntryAction {
    data class Show(
        val mode: TextEntryMode
    ) : TextEntryAction()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OtherUserProfileScaffoldContent(
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    params: OtherUserProfileScaffoldParams,
    config: ProfileContentConfig,
    handlers: ProfileActionHandlers,
    modifier: Modifier = Modifier
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag(ScreenshotTestTags.OTHER_USER_PROFILE_SCREEN)
    ) {
        if (params.isLoadingCurrentUser) {
            LoadingOverlayView(modifier = Modifier.fillMaxSize())
        } else {
            ProfilePullToRefreshContent(
                params = params,
                config = config,
                handlers = handlers
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfilePullToRefreshContent(
    params: OtherUserProfileScaffoldParams,
    config: ProfileContentConfig,
    handlers: ProfileActionHandlers
) {
    val pullRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = params.isRefreshing,
        onRefresh = { config.viewModel.refreshUser() },
        state = pullRefreshState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullRefreshState,
                isRefreshing = params.isRefreshing,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = dimensionResource(R.dimen.spacing_regular))
            )
        }
    ) {
        ProfileUiStateContent(
            uiState = params.uiState,
            params = params,
            config = config,
            handlers = handlers
        )
    }

    if (params.uiState is OtherUserProfileUiState.Loading && !params.isRefreshing) {
        LoadingOverlayView(modifier = Modifier.fillMaxSize())
    }

    if (params.isFriendActionLoading) {
        LoadingOverlayView(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun ProfileUiStateContent(
    uiState: OtherUserProfileUiState,
    params: OtherUserProfileScaffoldParams,
    config: ProfileContentConfig,
    handlers: ProfileActionHandlers
) {
    when (val state = uiState) {
        is OtherUserProfileUiState.Loading -> { // Контент скрыт
        }

        is OtherUserProfileUiState.UserNotFound ->
            UserNotFoundContent(
                onBack = { handlers.onProfileAction(ProfileNavigationAction.Back) }
            )

        is OtherUserProfileUiState.BlockedByUser ->
            BlockedByUserContent(
                onBack = { handlers.onProfileAction(ProfileNavigationAction.Back) }
            )

        is OtherUserProfileUiState.Success -> {
            ProfileContent(
                state =
                    ProfileContentState(
                        viewedUser = params.viewedUser,
                        country = state.country,
                        city = state.city,
                        isFriend = params.isFriend,
                        isInBlacklist = params.isInBlacklist,
                        isRefreshing = params.isRefreshing,
                        isFriendActionLoading = params.isFriendActionLoading
                    ),
                viewModel = config.viewModel,
                appState = config.appState,
                source = config.source,
                onAction = { action ->
                    when (action) {
                        is ProfileContentAction.SendMessage -> {
                            handlers.onTextEntryAction(
                                TextEntryMode.Message(
                                    action.user.id,
                                    action.user.fullName?.takeIf { it.isNotBlank() }
                                        ?: action.user.name
                                )
                            )
                        }

                        is ProfileContentAction.ShowRemoveFriendDialog -> {
                            handlers.onShowRemoveFriendDialog()
                        }
                    }
                }
            )
        }

        is OtherUserProfileUiState.Error -> {
            ErrorContent(
                message = state.message,
                canRetry = state.canRetry,
                onRetry = { config.viewModel.loadUser() }
            )
        }
    }
}

private data class ProfileDialogHandlers(
    val onBlacklistConfirm: () -> Unit,
    val onBlacklistDismiss: () -> Unit,
    val onRemoveFriendConfirm: () -> Unit,
    val onRemoveFriendDismiss: () -> Unit,
    val onTextEntryDismiss: () -> Unit,
    val onTextEntrySuccess: () -> Unit
)

@Composable
private fun OtherUserProfileDialogs(
    state: ProfileDialogState,
    blacklistAction: BlacklistAction,
    handlers: ProfileDialogHandlers
) {
    if (state.showBlacklistDialog) {
        BlacklistActionDialog(
            action = blacklistAction,
            onConfirm = handlers.onBlacklistConfirm,
            onDismiss = handlers.onBlacklistDismiss
        )
    }

    if (state.showRemoveFriendDialog) {
        RemoveFriendDialog(
            onConfirm = handlers.onRemoveFriendConfirm,
            onDismiss = handlers.onRemoveFriendDismiss
        )
    }

    if (state.showTextEntrySheet && state.textEntryMode != null) {
        TextEntrySheetHost(
            show = true,
            mode = state.textEntryMode,
            onDismissed = handlers.onTextEntryDismiss,
            onSendSuccess = handlers.onTextEntrySuccess
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
                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor =
                            if (action == BlacklistAction.BLOCK) {
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
internal fun RemoveFriendDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.remove_friend_alert_title)) },
        text = { Text(stringResource(R.string.remove_friend_alert_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
            ) { Text(stringResource(R.string.remove_friend_alert_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        }
    )
}

@Composable
private fun ProfileContent(
    state: ProfileContentState,
    viewModel: IOtherUserProfileViewModel,
    appState: AppState,
    source: String,
    onAction: (ProfileContentAction) -> Unit
) {
    val viewedUser = state.viewedUser ?: return

    val buttonsEnabled = !state.isRefreshing && !state.isInBlacklist && !state.isFriendActionLoading

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.spacing_regular)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
    ) {
        ProfileHeaderSection(
            viewedUser = viewedUser,
            country = state.country,
            city = state.city
        )

        ProfileActionButtons(
            viewedUser = viewedUser,
            isFriend = state.isFriend,
            buttonsEnabled = buttonsEnabled,
            viewModel = viewModel,
            onAction = onAction
        )

        ProfileNavigationButtons(
            params =
                ProfileNavigationParams(
                    viewedUser = viewedUser,
                    appState = appState,
                    source = source,
                    buttonsEnabled = buttonsEnabled
                )
        )
    }
}

@Composable
private fun ProfileHeaderSection(
    viewedUser: User,
    country: Country?,
    city: City?
) {
    val shortAddress = listOfNotNull(country?.name, city?.name).joinToString(", ")
    UserProfileCardView(
        data =
            UserProfileData(
                imageStringURL = viewedUser.image,
                userName = viewedUser.fullName?.takeIf { it.isNotBlank() } ?: viewedUser.name,
                gender = viewedUser.genderOption?.let { stringResource(it.description) } ?: "",
                age = viewedUser.age,
                shortAddress = shortAddress
            )
    )
}

@Composable
private fun ProfileActionButtons(
    viewedUser: User,
    isFriend: Boolean,
    buttonsEnabled: Boolean,
    viewModel: IOtherUserProfileViewModel,
    onAction: (ProfileContentAction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))) {
        SendMessageButton(
            onClick = { onAction(ProfileContentAction.SendMessage(viewedUser)) },
            enabled = buttonsEnabled
        )

        val friendAction =
            if (isFriend) FriendAction.REMOVE_FRIEND else FriendAction.SEND_FRIEND_REQUEST
        FriendActionButton(
            action = friendAction,
            onClick = {
                if (friendAction == FriendAction.REMOVE_FRIEND) {
                    onAction(ProfileContentAction.ShowRemoveFriendDialog)
                } else {
                    viewModel.performFriendAction()
                }
            },
            enabled = buttonsEnabled
        )
    }
}

@Composable
private fun ProfileNavigationButtons(params: ProfileNavigationParams) {
    with(params) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
        ) {
            if (viewedUser.hasFriends) {
                FriendsButton(
                    friendsCount = viewedUser.friendsCount ?: 0,
                    friendRequestsCount = 0,
                    onClick = {
                        appState.navController.navigate(
                            Screen.UserFriends.createRoute(viewedUser.id, source)
                        )
                    },
                    enabled = buttonsEnabled
                )
            }

            if (viewedUser.hasUsedParks) {
                UsedParksButton(
                    parksCount = viewedUser.parksCount?.toIntOrNull() ?: 0,
                    onClick = {
                        appState.navController.navigate(
                            Screen.UserTrainingParks.createRoute(viewedUser.id, source)
                        )
                    },
                    enabled = buttonsEnabled
                )
            }

            if (viewedUser.hasAddedParks) {
                AddedParksButton(
                    addedParksCount = viewedUser.addedParks?.size ?: 0,
                    onClick = {
                        appState.navController.navigateToUserAddedParks(
                            userId = viewedUser.id,
                            source = source,
                            addedParks = viewedUser.addedParks.orEmpty()
                        )
                    },
                    enabled = buttonsEnabled
                )
            }

            if ((viewedUser.journalCount ?: 0) > 0) {
                JournalsButton(
                    journalsCount = viewedUser.journalCount ?: 0,
                    onClick = {
                        appState.navController.navigate(
                            Screen.JournalsList.createRoute(viewedUser.id, source)
                        )
                    },
                    enabled = buttonsEnabled
                )
            }
        }
    }
}

@Composable
internal fun SendMessageButton(
    onClick: () -> Unit,
    enabled: Boolean
) {
    SWButton(
        config =
            ButtonConfig(
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
internal fun FriendActionButton(
    action: FriendAction,
    onClick: () -> Unit,
    enabled: Boolean
) {
    val icon =
        when (action) {
            FriendAction.SEND_FRIEND_REQUEST -> Icons.Outlined.PersonAdd
            FriendAction.REMOVE_FRIEND -> Icons.Outlined.PersonRemove
        }
    SWButton(
        config =
            ButtonConfig(
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
    EmptyStateView(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.spacing_regular)),
        text = stringResource(R.string.user_not_found),
        buttonTitle = stringResource(R.string.go_back),
        onButtonClick = onBack
    )
}

@Composable
internal fun BlockedByUserContent(onBack: () -> Unit) {
    Column(
        modifier =
            Modifier
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
        SWButton(
            config =
                ButtonConfig(
                    size = SWButtonSize.SMALL,
                    text = stringResource(R.string.go_back),
                    onClick = onBack
                )
        )
    }
}

@Composable
internal fun ErrorContent(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit
) {
    Column(
        modifier =
            Modifier
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
            SWButton(
                config =
                    ButtonConfig(
                        size = SWButtonSize.SMALL,
                        text = stringResource(R.string.try_again_button),
                        onClick = onRetry
                    )
            )
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

@Preview(showBackground = true)
@Composable
private fun RemoveFriendDialogPreview() {
    JetpackWorkoutAppTheme {
        RemoveFriendDialog(onConfirm = {}, onDismiss = {})
    }
}
