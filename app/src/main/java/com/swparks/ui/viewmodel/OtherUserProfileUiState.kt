package com.swparks.ui.viewmodel

import com.swparks.data.model.City
import com.swparks.data.model.Country

sealed class OtherUserProfileUiState {
    data object Loading : OtherUserProfileUiState()

    data object UserNotFound : OtherUserProfileUiState()

    data object BlockedByUser : OtherUserProfileUiState()

    data class Success(
        val country: Country? = null,
        val city: City? = null
    ) : OtherUserProfileUiState()

    data class Error(
        val message: String,
        val canRetry: Boolean = true,
        val country: Country? = null,
        val city: City? = null
    ) : OtherUserProfileUiState()
}
