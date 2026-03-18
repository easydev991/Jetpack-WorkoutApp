package com.swparks.ui.screens.messages

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
import com.swparks.data.model.User
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.screens.common.EmptyFriendsContent
import com.swparks.ui.screens.common.FriendsListSectionWithUserData
import com.swparks.ui.state.FriendsListUiState
import com.swparks.ui.viewmodel.IFriendsListViewModel

data class FriendsPickerParams(
    val friendsCount: Int,
    val friendRequestsCount: Int,
    val isEmpty: Boolean,
    val isLoading: Boolean,
    val errorMessage: String?,
    val disabledFriendIds: Set<Long>,
    val onFriendClick: (Long, String) -> Unit,
    val onBackClick: () -> Unit
) {
    fun isFriendDisabled(userId: Long): Boolean = userId in disabledFriendIds
}

fun createFriendsPickerParams(
    uiState: FriendsListUiState,
    currentUserId: Long?,
    onFriendClick: (Long, String) -> Unit,
    onBackClick: () -> Unit
): FriendsPickerParams {
    return when (uiState) {
        is FriendsListUiState.Loading -> FriendsPickerParams(
            friendsCount = 0,
            friendRequestsCount = 0,
            isEmpty = false,
            isLoading = true,
            errorMessage = null,
            disabledFriendIds = emptySet(),
            onFriendClick = onFriendClick,
            onBackClick = onBackClick
        )

        is FriendsListUiState.Error -> FriendsPickerParams(
            friendsCount = 0,
            friendRequestsCount = 0,
            isEmpty = false,
            isLoading = false,
            errorMessage = uiState.message,
            disabledFriendIds = emptySet(),
            onFriendClick = onFriendClick,
            onBackClick = onBackClick
        )

        is FriendsListUiState.Success -> {
            val friends = uiState.friends
            val disabledIds = currentUserId?.let { setOf(it) } ?: emptySet()
            FriendsPickerParams(
                friendsCount = friends.size,
                friendRequestsCount = 0,
                isEmpty = friends.isEmpty(),
                isLoading = false,
                errorMessage = null,
                disabledFriendIds = disabledIds,
                onFriendClick = onFriendClick,
                onBackClick = onBackClick
            )
        }
    }
}

data class FriendsPickerConfig(
    val parentPaddingValues: PaddingValues,
    val currentUserId: Long? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesFriendsPickerScreen(
    modifier: Modifier = Modifier,
    viewModel: IFriendsListViewModel,
    config: FriendsPickerConfig,
    onFriendClick: (Long, String) -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    MessagesFriendsPickerContent(
        modifier = modifier,
        uiState = uiState,
        config = config,
        onFriendClick = onFriendClick,
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesFriendsPickerContent(
    modifier: Modifier = Modifier,
    uiState: FriendsListUiState,
    config: FriendsPickerConfig,
    onFriendClick: (Long, String) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.select_friend))
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
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                is FriendsListUiState.Success -> {
                    FriendsOnlyContent(
                        friends = uiState.friends,
                        currentUserId = config.currentUserId,
                        enabled = !uiState.isProcessing,
                        onFriendClick = onFriendClick,
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

@Composable
private fun FriendsOnlyContent(
    friends: List<User>,
    currentUserId: Long?,
    enabled: Boolean,
    onFriendClick: (Long, String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (friends.isEmpty()) {
        EmptyFriendsContent(modifier = modifier)
    } else {
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
            item {
                FriendsListSectionWithUserData(
                    friends = friends,
                    currentUserId = currentUserId,
                    enabled = enabled,
                    onFriendClick = onFriendClick
                )
            }
        }
    }
}

