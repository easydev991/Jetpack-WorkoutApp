package com.swparks.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.swparks.R
import com.swparks.data.model.User
import com.swparks.ui.ds.FriendRequestData
import com.swparks.ui.ds.FriendRequestRowView
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.ds.SectionView
import com.swparks.ui.screens.common.EmptyFriendsContent
import com.swparks.ui.screens.common.FriendsListConfig
import com.swparks.ui.screens.common.FriendsListSection
import com.swparks.ui.state.FriendsListUiState
import com.swparks.ui.viewmodel.IFriendsListViewModel

data class FriendsScreenConfig(
    val parentPaddingValues: PaddingValues,
    val currentUserId: Long? = null
)

data class FriendsContentState(
    val friendRequests: List<User>,
    val friends: List<User>,
    val enabled: Boolean,
    val currentUserId: Long?
)

sealed class FriendAction {
    data class Accept(val userId: Long) : FriendAction()
    data class Decline(val userId: Long) : FriendAction()
    data class Click(val userId: Long) : FriendAction()
}

/**
 * Экран списка друзей текущего пользователя
 *
 * @param modifier Модификатор
 * @param viewModel ViewModel для управления состоянием экрана
 * @param config Конфигурация экрана с паддингами и ID текущего пользователя
 * @param onFriendClick Callback для навигации на профиль друга
 * @param onAction Callback для обработки действий с друзьями
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFriendsScreen(
    modifier: Modifier = Modifier,
    viewModel: IFriendsListViewModel,
    config: FriendsScreenConfig,
    onFriendClick: (Long) -> Unit,
    onAction: (FriendAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    MyFriendsScreenContent(
        modifier = modifier,
        uiState = uiState,
        config = config,
        onFriendClick = onFriendClick,
        onAction = onAction
    )
}

/**
 * Stateless контент экрана списка друзей
 *
 * @param modifier Модификатор
 * @param uiState Текущее состояние UI
 * @param config Конфигурация экрана с паддингами и ID текущего пользователя
 * @param onFriendClick Callback при клике на друга
 * @param onAction Callback для обработки действий с друзьями
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFriendsScreenContent(
    modifier: Modifier = Modifier,
    uiState: FriendsListUiState,
    config: FriendsScreenConfig,
    onFriendClick: (Long) -> Unit,
    onAction: (FriendAction) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.friends))
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(FriendAction.Click(-1)) }) {
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
                .padding(config.parentPaddingValues)
                .padding(innerPadding)
        ) {
            when (uiState) {
                is FriendsListUiState.Loading -> {
                    LoadingOverlayView()
                }

                is FriendsListUiState.Error -> {
                    Text(
                        text = uiState.message,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is FriendsListUiState.Success -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        SuccessContent(
                            state = FriendsContentState(
                                friendRequests = uiState.friendRequests,
                                friends = uiState.friends,
                                enabled = !uiState.isProcessing,
                                currentUserId = config.currentUserId
                            ),
                            onFriendClick = onFriendClick,
                            onAction = onAction,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (uiState.isProcessing) {
                            LoadingOverlayView()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Контент с данными друзей и заявок
 *
 * @param state Состояние с данными друзей и заявок
 * @param onFriendClick Callback при клике на друга
 * @param onAction Callback для обработки действий с друзьями
 * @param modifier Модификатор
 */
@Composable
private fun SuccessContent(
    state: FriendsContentState,
    onFriendClick: (Long) -> Unit,
    onAction: (FriendAction) -> Unit,
    modifier: Modifier = Modifier
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
        val hasFriendRequests = state.friendRequests.isNotEmpty()
        val hasFriends = state.friends.isNotEmpty()
        val singleSection = hasFriendRequests.xor(hasFriends)

        if (hasFriendRequests) {
            item {
                FriendRequestsSection(
                    friendRequests = state.friendRequests,
                    enabled = state.enabled,
                    singleSection = singleSection,
                    onAction = onAction
                )
            }

            if (hasFriends) {
                item { HorizontalDivider() }
            }
        }

        if (hasFriends) {
            item {
                FriendsListSection(
                    config = FriendsListConfig(
                        friends = state.friends,
                        currentUserId = state.currentUserId,
                        enabled = state.enabled,
                        showTitle = !singleSection,
                        onFriendClick = onFriendClick
                    )
                )
            }
        }

        if (state.friendRequests.isEmpty() && state.friends.isEmpty()) {
            item {
                EmptyFriendsContentLegacy(modifier = Modifier.fillMaxSize())
            }
        }
    }
}


@Composable
private fun FriendRequestsSection(
    friendRequests: List<User>,
    enabled: Boolean,
    singleSection: Boolean,
    onAction: (FriendAction) -> Unit
) {
    SectionView(
        titleID = if (singleSection) null else R.string.requests,
        titleBottomPadding = dimensionResource(R.dimen.spacing_xsmall)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))) {
            friendRequests.forEach { user ->
                FriendRequestRowView(
                    data = FriendRequestData(
                        modifier = Modifier,
                        imageStringURL = user.image,
                        name = user.name,
                        address = null,
                        onClickAccept = { onAction(FriendAction.Accept(user.id)) },
                        onClickDecline = { onAction(FriendAction.Decline(user.id)) },
                        enabled = enabled
                    )
                )
            }
        }
    }
}


@Composable
private fun EmptyFriendsContentLegacy(modifier: Modifier = Modifier) {
    EmptyFriendsContent(modifier = modifier)
}
