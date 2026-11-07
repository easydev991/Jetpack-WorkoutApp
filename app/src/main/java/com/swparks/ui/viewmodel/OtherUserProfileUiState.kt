package com.swparks.ui.viewmodel

import com.swparks.domain.model.City
import com.swparks.domain.model.Country

sealed class OtherUserProfileUiState {
    data object Loading : OtherUserProfileUiState()

    data object UserNotFound : OtherUserProfileUiState()

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
