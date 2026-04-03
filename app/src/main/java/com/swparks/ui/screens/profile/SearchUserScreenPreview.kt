package com.swparks.ui.screens.profile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swparks.data.model.User
import com.swparks.ui.state.SearchUserUiState
import com.swparks.ui.theme.JetpackWorkoutAppTheme

@Preview(showBackground = true, locale = "ru", name = "Initial State")
@Composable
fun SearchUserScreenInitialPreview() {
    JetpackWorkoutAppTheme {
        SearchUserScreenContent(
            uiState = SearchUserUiState.Initial,
            searchQueryState =
                SearchQueryState(
                    query = "",
                    onQueryChange = {}
                ),
            config =
                SearchUserConfig(
                    parentPaddingValues = PaddingValues(0.dp),
                    currentUserId = null
                ),
            onAction = {}
        )
    }
}

@Preview(showBackground = true, locale = "ru", name = "Loading State")
@Composable
fun SearchUserScreenLoadingPreview() {
    JetpackWorkoutAppTheme {
        SearchUserScreenContent(
            uiState = SearchUserUiState.Loading,
            searchQueryState =
                SearchQueryState(
                    query = "test",
                    onQueryChange = {}
                ),
            config =
                SearchUserConfig(
                    parentPaddingValues = PaddingValues(0.dp),
                    currentUserId = null
                ),
            onAction = {}
        )
    }
}

@Preview(showBackground = true, locale = "ru", name = "Success State")
@Composable
fun SearchUserScreenSuccessPreview() {
    val users =
        listOf(
            User(
                id = 1,
                name = "ivan_petrov",
                fullName = "Ivan Petrov",
                image = null,
                email = "ivan@example.com"
            ),
            User(
                id = 2,
                name = "workout_master",
                fullName = "Workout Master",
                image = null,
                email = "master@example.com"
            ),
            User(
                id = 3,
                name = "fitness_queen",
                fullName = null,
                image = null,
                email = "queen@example.com"
            )
        )

    JetpackWorkoutAppTheme {
        SearchUserScreenContent(
            uiState = SearchUserUiState.Success(users),
            searchQueryState =
                SearchQueryState(
                    query = "test",
                    onQueryChange = {}
                ),
            config =
                SearchUserConfig(
                    parentPaddingValues = PaddingValues(0.dp),
                    currentUserId = null
                ),
            onAction = {}
        )
    }
}

@Preview(showBackground = true, locale = "ru", name = "Empty State")
@Composable
fun SearchUserScreenEmptyPreview() {
    JetpackWorkoutAppTheme {
        SearchUserScreenContent(
            uiState = SearchUserUiState.Empty,
            searchQueryState =
                SearchQueryState(
                    query = "notfound",
                    onQueryChange = {}
                ),
            config =
                SearchUserConfig(
                    parentPaddingValues = PaddingValues(0.dp),
                    currentUserId = null
                ),
            onAction = {}
        )
    }
}

@Preview(showBackground = true, locale = "ru", name = "Network Error State")
@Composable
fun SearchUserScreenNetworkErrorPreview() {
    JetpackWorkoutAppTheme {
        SearchUserScreenContent(
            uiState = SearchUserUiState.NetworkError,
            searchQueryState =
                SearchQueryState(
                    query = "test",
                    onQueryChange = {}
                ),
            config =
                SearchUserConfig(
                    parentPaddingValues = PaddingValues(0.dp),
                    currentUserId = null
                ),
            onAction = {}
        )
    }
}
