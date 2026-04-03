package com.swparks.ui.screens.profile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swparks.data.model.User
import com.swparks.ui.state.FriendsListUiState
import com.swparks.ui.theme.JetpackWorkoutAppTheme

@Preview(showBackground = true, locale = "ru", name = "Success State")
@Composable
fun MyFriendsScreenSuccessPreview() {
    val friends =
        listOf(
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
    val requests =
        listOf(
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
            uiState =
                FriendsListUiState.Success(
                    friends = friends,
                    friendRequests = requests,
                    isProcessing = false
                ),
            config =
                FriendsScreenConfig(
                    parentPaddingValues = PaddingValues(0.dp),
                    currentUserId = null
                ),
            onFriendClick = {},
            onAction = {}
        )
    }
}

@Preview(showBackground = true, locale = "ru", name = "Loading State")
@Composable
fun MyFriendsScreenLoadingPreview() {
    JetpackWorkoutAppTheme {
        MyFriendsScreenContent(
            uiState = FriendsListUiState.Loading,
            config =
                FriendsScreenConfig(
                    parentPaddingValues = PaddingValues(0.dp),
                    currentUserId = null
                ),
            onFriendClick = {},
            onAction = {}
        )
    }
}

@Preview(showBackground = true, locale = "ru", name = "Error State")
@Composable
fun MyFriendsScreenErrorPreview() {
    JetpackWorkoutAppTheme {
        MyFriendsScreenContent(
            uiState = FriendsListUiState.Error("Не удалось загрузить список друзей"),
            config =
                FriendsScreenConfig(
                    parentPaddingValues = PaddingValues(0.dp),
                    currentUserId = null
                ),
            onFriendClick = {},
            onAction = {}
        )
    }
}
