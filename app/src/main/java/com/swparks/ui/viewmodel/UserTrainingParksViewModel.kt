package com.swparks.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.data.model.Park
import com.swparks.data.repository.SWRepository
import com.swparks.util.AppError
import com.swparks.util.ErrorReporter
import com.swparks.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI State для экрана площадок пользователя */
sealed class UserTrainingParksUiState {
    data object Loading : UserTrainingParksUiState()
    data class Success(val parks: List<Park>) : UserTrainingParksUiState()
    data class Error(val message: String) : UserTrainingParksUiState()
}

/**
 * ViewModel для экрана площадок пользователя
 *
 * Управляет списком площадок, на которых тренируется пользователь
 *
 * @param swRepository Репозиторий для работы с данными площадок
 * @param userId ID пользователя, для которого загружаются площадки
 * @param logger Логгер для записи сообщений
 * @param errorReporter Обработчик ошибок для отправки ошибок в UI
 */
@Suppress("TooGenericExceptionCaught", "MaxLineLength")
class UserTrainingParksViewModel(
    private val swRepository: SWRepository,
    private val userId: Long,
    private val logger: Logger,
    private val errorReporter: ErrorReporter,
) : ViewModel(), IUserTrainingParksViewModel {

    private companion object {
        private const val TAG = "UserTrainingParksViewModel"
    }

    // UI State
    private val _uiState =
        MutableStateFlow<UserTrainingParksUiState>(UserTrainingParksUiState.Loading)
    override val uiState: StateFlow<UserTrainingParksUiState> = _uiState.asStateFlow()

    // Состояние обновления данных (pull-to-refresh)
    private val _isRefreshing = MutableStateFlow(false)
    override val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadParks()
    }

    /**
     * Загружает список площадок пользователя
     */
    private fun loadParks() {
        viewModelScope.launch {
            swRepository.getParksForUser(userId)
                .onSuccess { parks ->
                    _uiState.update { UserTrainingParksUiState.Success(parks) }
                    logger.i(TAG, "Успешно загружено площадок: ${parks.size}")
                }
                .onFailure { error ->
                    val errorMessage = "Ошибка загрузки площадок пользователя: ${error.message}"
                    _uiState.update { UserTrainingParksUiState.Error(errorMessage) }
                    errorReporter.handleError(AppError.Generic(errorMessage, error))
                    logger.e(TAG, errorMessage)
                }
        }
    }

    /**
     * Обновляет список площадок для pull-to-refresh
     */
    override fun refreshParks() {
        viewModelScope.launch {
            try {
                _isRefreshing.update { true }
                logger.i(TAG, "Начало обновления площадок пользователя: $userId")

                swRepository.getParksForUser(userId)
                    .onSuccess { parks ->
                        _uiState.update { UserTrainingParksUiState.Success(parks) }
                        logger.i(TAG, "Успешно обновлено площадок: ${parks.size}")
                    }
                    .onFailure { error ->
                        val errorMessage =
                            "Ошибка обновления площадок пользователя: ${error.message}"
                        _uiState.update { UserTrainingParksUiState.Error(errorMessage) }
                        errorReporter.handleError(AppError.Generic(errorMessage, error))
                        logger.e(TAG, errorMessage)
                    }
            } catch (e: Exception) {
                val errorMessage = "Неожиданная ошибка при обновлении площадок: ${e.message}"
                _uiState.update { UserTrainingParksUiState.Error(errorMessage) }
                errorReporter.handleError(AppError.Generic(errorMessage, e))
                logger.e(TAG, errorMessage, e)
            } finally {
                _isRefreshing.update { false }
            }
        }
    }
}
