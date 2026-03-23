package com.swparks.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.data.model.Park
import com.swparks.data.repository.SWRepository
import com.swparks.util.AppError
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class UserAddedParksUiState {
    data object Loading : UserAddedParksUiState()
    data class Success(val parks: List<Park>) : UserAddedParksUiState()
    data class Error(val message: String) : UserAddedParksUiState()
}

@Suppress("TooGenericExceptionCaught", "InstanceOfCheckForException")
class UserAddedParksViewModel(
    private val swRepository: SWRepository,
    private val userId: Long,
    private val seedParks: List<Park>?,
    private val requiresFetch: Boolean,
    private val logger: Logger,
    private val userNotifier: UserNotifier
) : ViewModel(), IUserAddedParksViewModel {

    private companion object {
        private const val TAG = "UserAddedParksViewModel"
    }

    private val _uiState = MutableStateFlow<UserAddedParksUiState>(UserAddedParksUiState.Loading)
    override val uiState: StateFlow<UserAddedParksUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    override val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        if (seedParks != null && !requiresFetch) {
            _uiState.update { UserAddedParksUiState.Success(seedParks) }
            logger.i(TAG, "Добавленные площадки получены из seed: ${seedParks.size}")
        } else {
            loadAddedParks()
        }
    }

    override fun refresh() {
        loadAddedParks(isRefresh = true)
    }

    override fun removePark(parkId: Long) {
        _uiState.update { state ->
            if (state is UserAddedParksUiState.Success) {
                state.copy(parks = state.parks.filter { it.id != parkId })
            } else state
        }
        logger.i(TAG, "Площадка $parkId удалена из локального списка")
    }

    private fun loadAddedParks(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _isRefreshing.update { true }
            } else {
                _uiState.update { UserAddedParksUiState.Loading }
            }

            try {
                swRepository.getUser(userId)
                    .onSuccess { user ->
                        val parks = user.addedParks.orEmpty()
                        _uiState.update { UserAddedParksUiState.Success(parks) }
                        logger.i(TAG, "Успешно загружено добавленных площадок: ${parks.size}")
                    }
                    .onFailure { error ->
                        val message = "Ошибка загрузки добавленных площадок: ${error.message}"
                        _uiState.update { UserAddedParksUiState.Error(message) }
                        userNotifier.handleError(AppError.Generic(message, error))
                        logger.e(TAG, message, error)
                    }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val message = "Неожиданная ошибка загрузки добавленных площадок: ${e.message}"
                _uiState.update { UserAddedParksUiState.Error(message) }
                userNotifier.handleError(AppError.Generic(message, e))
                logger.e(TAG, message, e)
            } finally {
                if (isRefresh) {
                    _isRefreshing.update { false }
                }
            }
        }
    }
}
