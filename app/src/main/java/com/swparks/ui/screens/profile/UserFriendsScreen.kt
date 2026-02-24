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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.swparks.R
import com.swparks.data.model.User
import com.swparks.ui.ds.EmptyStateView
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.ds.UserRowView
import com.swparks.ui.utils.disabledIf
import com.swparks.ui.viewmodel.UserFriendsUiState
import com.swparks.ui.viewmodel.UserFriendsViewModel

/**
 * Экран списка друзей другого пользователя
 *
 * @param modifier Модификатор
 * @param viewModel ViewModel для управления состоянием экрана
 * @param onBackClick Callback для навигации назад
 * @param onUserClick Callback для навигации на профиль пользователя
 * @param parentPaddingValues Паддинги для учета BottomNavigationBar
 * @param currentUserId ID текущего авторизованного пользователя (для блокировки собственного профиля)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFriendsScreen(
    modifier: Modifier = Modifier,
    viewModel: UserFriendsViewModel,
    onBackClick: () -> Unit,
    onUserClick: (Long) -> Unit,
    parentPaddingValues: PaddingValues,
    currentUserId: Long? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    UserFriendsScreenContent(
        modifier = modifier,
        uiState = uiState,
        onBackClick = onBackClick,
        onUserClick = onUserClick,
        parentPaddingValues = parentPaddingValues,
        onRefresh = { viewModel.refresh() },
        currentUserId = currentUserId
    )
}

/**
 * Stateless контент экрана списка друзей пользователя
 *
 * @param modifier Модификатор
 * @param uiState Текущее состояние UI
 * @param onBackClick Callback для навигации назад
 * @param onUserClick Callback для навигации на профиль пользователя
 * @param parentPaddingValues Паддинги для учета BottomNavigationBar
 * @param onRefresh Callback для обновления данных
 * @param currentUserId ID текущего авторизованного пользователя (для блокировки собственного профиля)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFriendsScreenContent(
    modifier: Modifier = Modifier,
    uiState: UserFriendsUiState,
    onBackClick: () -> Unit,
    onUserClick: (Long) -> Unit,
    parentPaddingValues: PaddingValues,
    onRefresh: () -> Unit,
    currentUserId: Long? = null
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.friends))
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
                is UserFriendsUiState.Loading -> {
                    LoadingOverlayView()
                }

                is UserFriendsUiState.Error -> {
                    EmptyStateView(
                        text = stringResource(R.string.error_network_general),
                        buttonTitle = stringResource(R.string.try_again_button),
                        onButtonClick = onRefresh
                    )
                }

                is UserFriendsUiState.Success -> {
                    if (uiState.friends.isEmpty()) {
                        EmptyStateView(
                            text = stringResource(R.string.no_friends_yet)
                        )
                    } else {
                        FriendsList(
                            friends = uiState.friends,
                            onUserClick = onUserClick,
                            modifier = Modifier.fillMaxSize(),
                            currentUserId = currentUserId
                        )
                    }
                }
            }
        }
    }
}

/**
 * Список друзей
 *
 * @param friends Список друзей
 * @param onUserClick Callback при клике на пользователя
 * @param modifier Модификатор
 * @param currentUserId ID текущего авторизованного пользователя (для блокировки собственного профиля)
 */
@Composable
private fun FriendsList(
    friends: List<User>,
    onUserClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    currentUserId: Long? = null
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = dimensionResource(R.dimen.spacing_regular),
            top = dimensionResource(R.dimen.spacing_small),
            end = dimensionResource(R.dimen.spacing_regular),
            bottom = dimensionResource(R.dimen.spacing_regular)
        ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        items(friends.size) { index ->
            val user = friends[index]
            val isDisabled = user.id == currentUserId
            Box(
                modifier = Modifier.disabledIf(
                    disabled = isDisabled,
                    onClick = { onUserClick(user.id) }
                )
            ) {
                UserRowView(
                    modifier = Modifier,
                    imageStringURL = user.image,
                    name = user.name,
                    address = null
                )
            }
        }
    }
}
