package com.swparks.ui.viewmodel

import com.swparks.data.model.User
import kotlinx.coroutines.flow.StateFlow

interface IOtherUserProfileViewModel {
    val viewedUser: StateFlow<User?>
    val currentUser: StateFlow<User?>
    val friends: StateFlow<List<User>>
    val blacklist: StateFlow<List<User>>
    val uiState: StateFlow<OtherUserProfileUiState>
    val isRefreshing: StateFlow<Boolean>
    val isLoadingCurrentUser: StateFlow<Boolean>
    val isFriendActionLoading: StateFlow<Boolean>

    fun loadUser()

    fun refreshUser()

    fun performFriendAction()

    fun performBlacklistAction(onBlocked: () -> Unit)
}
