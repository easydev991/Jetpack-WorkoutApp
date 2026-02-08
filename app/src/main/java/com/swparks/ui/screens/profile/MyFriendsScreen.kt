package com.swparks.ui.screens.profile

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.swparks.model.User
import com.swparks.ui.ds.FriendRequestData
import com.swparks.ui.ds.FriendRequestRowView
import com.swparks.ui.ds.SectionView
import com.swparks.ui.ds.UserRowView
import com.swparks.viewmodel.FriendsListUiState
import com.swparks.viewmodel.FriendsListViewModel

/**
 * Экран списка друзей текущего пользователя
 *
 * @param modifier Модификатор
 * @param viewModel ViewModel для управления состоянием экрана
 * @param onBackClick Callback для навигации назад
 * @param parentPaddingValues Паддинги для учета BottomNavigationBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFriendsScreen(
    modifier: Modifier = Modifier,
    viewModel: FriendsListViewModel,
    onBackClick: () -> Unit,
    parentPaddingValues: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    MyFriendsScreenContent(
        modifier = modifier,
        uiState = uiState,
        onBackClick = onBackClick,
        parentPaddingValues = parentPaddingValues,
        onAcceptFriendRequest = { viewModel.onAcceptFriendRequest(it) },
        onDeclineFriendRequest = { viewModel.onDeclineFriendRequest(it) },
        onFriendClick = { viewModel.onFriendClick(it) },
        isProcessing = isProcessing
    )
}

/**
 * Stateless контент экрана списка друзей
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFriendsScreenContent(
    modifier: Modifier = Modifier,
    uiState: FriendsListUiState,
    onBackClick: () -> Unit,
    parentPaddingValues: PaddingValues,
    onAcceptFriendRequest: (Long) -> Unit,
    onDeclineFriendRequest: (Long) -> Unit,
    onFriendClick: (Long) -> Unit,
    isProcessing: Boolean = false
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
                is FriendsListUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is FriendsListUiState.Error -> {
                    Text(
                        text = uiState.message,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is FriendsListUiState.Success -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Данные с блокировкой кнопок при обработке запроса
                        SuccessContent(
                            friendRequests = uiState.friendRequests,
                            friends = uiState.friends,
                            onAcceptFriendRequest = onAcceptFriendRequest,
                            onDeclineFriendRequest = onDeclineFriendRequest,
                            onFriendClick = onFriendClick,
                            modifier = Modifier.fillMaxSize(),
                            enabled = !isProcessing
                        )
                        // Индикатор загрузки поверх при обработке запроса
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .matchParentSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Контент с данными друзей и заявок
 */
@Composable
private fun SuccessContent(
    friendRequests: List<User>,
    friends: List<User>,
    onAcceptFriendRequest: (Long) -> Unit,
    onDeclineFriendRequest: (Long) -> Unit,
    onFriendClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
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
        // Определяем, сколько секций будет отображаться
        val hasFriendRequests = friendRequests.isNotEmpty()
        val hasFriends = friends.isNotEmpty()
        val singleSection = hasFriendRequests.xor(hasFriends) // XOR - true если только одна секция

        // Секция заявок на добавление в друзья
        if (hasFriendRequests) {
            item {
                SectionView(
                    titleID = if (singleSection) null else R.string.requests,
                    titleBottomPadding = dimensionResource(R.dimen.spacing_xsmall)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
                    ) {
                        friendRequests.forEach { user ->
                            FriendRequestRowView(
                                data = FriendRequestData(
                                    modifier = Modifier,
                                    imageStringURL = user.image,
                                    name = user.name,
                                    address = null,
                                    onClickAccept = { onAcceptFriendRequest(user.id) },
                                    onClickDecline = { onDeclineFriendRequest(user.id) },
                                    enabled = enabled
                                )
                            )
                        }
                    }
                }
            }

            item {
                HorizontalDivider()
            }
        }

        // Секция друзей
        if (hasFriends) {
            item {
                SectionView(
                    titleID = if (singleSection) null else R.string.friends,
                    titleBottomPadding = dimensionResource(R.dimen.spacing_xsmall)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
                    ) {
                        friends.forEach { user ->
                            Box(
                                modifier = Modifier
                                    .clickable(enabled = enabled) {
                                        onFriendClick(user.id)
                                    }
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
            }
        }

        // Если данных нет
        if (friendRequests.isEmpty() && friends.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dimensionResource(R.dimen.spacing_large)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.loading),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
