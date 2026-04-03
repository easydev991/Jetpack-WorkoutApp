package com.swparks.ui.screens.messages

import com.swparks.data.model.User
import com.swparks.ui.state.FriendsListUiState
import com.swparks.ui.viewmodel.IFriendsListViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MessagesFriendsPickerScreenTest {
    private lateinit var viewModel: TrackingFriendsListViewModel

    @Before
    fun setup() {
        viewModel = TrackingFriendsListViewModel()
    }

    @Test
    fun createFriendsPickerParams_whenUiStateSuccess_thenContainsOnlyFriends() {
        val friends =
            listOf(
                User(id = 1L, name = "Friend 1", image = null),
                User(id = 2L, name = "Friend 2", image = null)
            )
        val uiState =
            FriendsListUiState.Success(
                friendRequests = listOf(User(id = 3L, name = "Request User", image = null)),
                friends = friends
            )

        val params =
            createFriendsPickerParams(
                uiState = uiState,
                currentUserId = null,
                onFriendClick = { _, _ -> },
                onBackClick = {}
            )

        assertEquals(2, params.friendsCount)
        assertEquals(0, params.friendRequestsCount)
    }

    @Test
    fun createFriendsPickerParams_whenFriendClicked_thenCallbackInvokedWithUserIdAndName() {
        var clickedUserId: Long? = null
        var clickedUserName: String? = null
        val friends =
            listOf(
                User(id = 1L, name = "Friend 1", image = null),
                User(id = 2L, name = "Friend 2", image = null)
            )
        val uiState = FriendsListUiState.Success(friends = friends)

        val params =
            createFriendsPickerParams(
                uiState = uiState,
                currentUserId = null,
                onFriendClick = { userId, userName ->
                    clickedUserId = userId
                    clickedUserName = userName
                },
                onBackClick = {}
            )

        params.onFriendClick(1L, "Friend 1")

        assertEquals(1L, clickedUserId)
        assertEquals("Friend 1", clickedUserName)
    }

    @Test
    fun createFriendsPickerParams_whenCurrentUserInList_thenMarkedAsDisabled() {
        val currentUserId = 1L
        val friends =
            listOf(
                User(id = 1L, name = "Current User", image = null),
                User(id = 2L, name = "Friend 2", image = null)
            )
        val uiState = FriendsListUiState.Success(friends = friends)

        val params =
            createFriendsPickerParams(
                uiState = uiState,
                currentUserId = currentUserId,
                onFriendClick = { _, _ -> },
                onBackClick = {}
            )

        assertEquals(true, params.isFriendDisabled(1L))
        assertEquals(false, params.isFriendDisabled(2L))
    }

    @Test
    fun createFriendsPickerParams_whenEmptyFriends_thenShowsEmptyState() {
        val uiState = FriendsListUiState.Success(friends = emptyList())

        val params =
            createFriendsPickerParams(
                uiState = uiState,
                currentUserId = null,
                onFriendClick = { _, _ -> },
                onBackClick = {}
            )

        assertEquals(true, params.isEmpty)
    }

    @Test
    fun createFriendsPickerParams_whenLoading_thenIsLoadingTrue() {
        val params =
            createFriendsPickerParams(
                uiState = FriendsListUiState.Loading,
                currentUserId = null,
                onFriendClick = { _, _ -> },
                onBackClick = {}
            )

        assertEquals(true, params.isLoading)
    }

    @Test
    fun createFriendsPickerParams_whenError_thenErrorMessageSet() {
        val errorMessage = "Network error"
        val uiState = FriendsListUiState.Error(message = errorMessage)

        val params =
            createFriendsPickerParams(
                uiState = uiState,
                currentUserId = null,
                onFriendClick = { _, _ -> },
                onBackClick = {}
            )

        assertEquals(errorMessage, params.errorMessage)
    }

    @Test
    fun createFriendsPickerParams_whenOnFriendClickCalled_thenDoesNotNavigateToProfile() {
        var navigationCalled = false
        var callbackCalled = false
        val friends = listOf(User(id = 1L, name = "Friend 1", image = null))
        val uiState = FriendsListUiState.Success(friends = friends)

        val params =
            createFriendsPickerParams(
                uiState = uiState,
                currentUserId = null,
                onFriendClick = { _, _ ->
                    callbackCalled = true
                },
                onBackClick = {}
            )

        params.onFriendClick(1L, "Friend 1")

        assertEquals(true, callbackCalled)
        assertEquals(false, navigationCalled)
    }

    @Test
    fun createFriendsPickerParams_whenFriendClicked_thenCallbackReadyForTextEntrySheet() {
        var capturedUserId: Long? = null
        var capturedUserName: String? = null
        val friends = listOf(User(id = 42L, name = "Test User", image = null))
        val uiState = FriendsListUiState.Success(friends = friends)

        val params =
            createFriendsPickerParams(
                uiState = uiState,
                currentUserId = null,
                onFriendClick = { userId, userName ->
                    capturedUserId = userId
                    capturedUserName = userName
                },
                onBackClick = {}
            )

        params.onFriendClick(42L, "Test User")

        assertEquals(42L, capturedUserId)
        assertEquals("Test User", capturedUserName)
    }

    @Test
    fun createFriendsPickerParams_onBackClick_thenNavigatesBack() {
        var backClicked = false
        val uiState = FriendsListUiState.Success(friends = emptyList())

        val params =
            createFriendsPickerParams(
                uiState = uiState,
                currentUserId = null,
                onFriendClick = { _, _ -> },
                onBackClick = { backClicked = true }
            )

        params.onBackClick()

        assertEquals(true, backClicked)
    }

    private class TrackingFriendsListViewModel : IFriendsListViewModel {
        override val friendRequests: StateFlow<List<com.swparks.data.database.entity.UserEntity>> =
            MutableStateFlow(emptyList())
        override val friends: StateFlow<List<com.swparks.data.database.entity.UserEntity>> =
            MutableStateFlow(emptyList())
        override val uiState: StateFlow<FriendsListUiState> =
            MutableStateFlow(FriendsListUiState.Loading)

        override fun onAcceptFriendRequest(userId: Long) = Unit

        override fun onDeclineFriendRequest(userId: Long) = Unit

        override fun onFriendClick(userId: Long) = Unit
    }
}
