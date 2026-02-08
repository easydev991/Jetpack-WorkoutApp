package com.swparks.ui.screens.profile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swparks.model.User
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.viewmodel.FriendsListUiState

@Preview(showBackground = true, locale = "ru", name = "Success State")
@Composable
fun MyFriendsScreenSuccessPreview() {
    val friends = listOf(
        User(
            id = 1,
            name = "Ivan",
            fullName = "Ivan Ivanov",
            image = null,
            email = "ivan@example.com"
        ),
        User(
            id = 2,
            name = "Petr",
            fullName = null,
            image = null,
            email = "petr@example.com"
        )
    )
    val requests = listOf(
        User(
            id = 3,
            name = "Sid",
            fullName = "Sid Vicious",
            image = null,
            email = "sid@example.com"
        ),
        User(
            id = 4,
            name = "Sid1",
            fullName = "Sid Vicious",
            image = null,
            email = "sid@example.com"
        )
    )

    JetpackWorkoutAppTheme {
        MyFriendsScreenContent(
            uiState = FriendsListUiState.Success(
                friends = friends,
                friendRequests = requests
            ),
            onBackClick = {},
            parentPaddingValues = PaddingValues(0.dp),
            onAcceptFriendRequest = {},
            onDeclineFriendRequest = {},
            onFriendClick = {},
            isProcessing = false
        )
    }
}

@Preview(showBackground = true, locale = "ru", name = "Loading State")
@Composable
fun MyFriendsScreenLoadingPreview() {
    JetpackWorkoutAppTheme {
        MyFriendsScreenContent(
            uiState = FriendsListUiState.Loading,
            onBackClick = {},
            parentPaddingValues = PaddingValues(0.dp),
            onAcceptFriendRequest = {},
            onDeclineFriendRequest = {},
            onFriendClick = {},
            isProcessing = false
        )
    }
}

@Preview(showBackground = true, locale = "ru", name = "Error State")
@Composable
fun MyFriendsScreenErrorPreview() {
    JetpackWorkoutAppTheme {
        MyFriendsScreenContent(
            uiState = FriendsListUiState.Error("Не удалось загрузить список друзей"),
            onBackClick = {},
            parentPaddingValues = PaddingValues(0.dp),
            onAcceptFriendRequest = {},
            onDeclineFriendRequest = {},
            onFriendClick = {},
            isProcessing = false
        )
    }
}
