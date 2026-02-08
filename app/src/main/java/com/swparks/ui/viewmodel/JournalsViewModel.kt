package com.swparks.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.domain.usecase.IGetJournalsUseCase
import com.swparks.domain.usecase.ISyncJournalsUseCase
import com.swparks.ui.state.JournalsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel для экрана списка дневников.
 *
 * Управляет отображением списка дневников пользователя с поддержкой
 * offline-first чтения и online-first синхронизации.
 *
 * @param userId Идентификатор пользователя
 * @param getJournalsUseCase Use case для получения потока дневников
 * @param syncJournalsUseCase Use case для синхронизации дневников с сервером
 */
class JournalsViewModel(
    private val userId: Long,
    private val getJournalsUseCase: IGetJournalsUseCase,
    private val syncJournalsUseCase: ISyncJournalsUseCase
) : ViewModel(), IJournalsViewModel {

    private companion object {
        private const val TAG = "JournalsViewModel"
    }

    // UI State (реализует интерфейс IJournalsViewModel)
    private val _uiState = MutableStateFlow<JournalsUiState>(JournalsUiState.InitialLoading)
    override val uiState: StateFlow<JournalsUiState> = _uiState.asStateFlow()

    // Индикатор обновления (pull-to-refresh) (реализует интерфейс IJournalsViewModel)
    private val _isRefreshing = MutableStateFlow(false)
    override val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        Log.i(TAG, "Инициализация JournalsViewModel для пользователя: $userId")
        observeJournals()
        loadJournals()
    }

    /**
     * Подписаться на поток дневников из Use Case (Single Source of Truth).
     * При получении данных обновляем UI State.
     */
    private fun observeJournals() {
        viewModelScope.launch {
            getJournalsUseCase(userId).collect { journals ->
                Log.i(TAG, "Получены данные из Flow: ${journals.size} дневников")
                _uiState.value =
                    JournalsUiState.Content(journals = journals, isRefreshing = _isRefreshing.value)
            }
        }
    }

    /**
     * Загрузить дневники с сервера.
     * Устанавливает флаг обновления и триггерит синхронизацию.
     */
    @Suppress("TooGenericExceptionCaught")
    override fun loadJournals() {
        viewModelScope.launch {
            try {
                Log.i(TAG, "Запуск загрузки дневников для пользователя: $userId")
                _isRefreshing.value = true

                val result = syncJournalsUseCase(userId)
                result.fold(
                    onSuccess = {
                        Log.i(TAG, "Синхронизация дневников успешна")
                        _isRefreshing.value = false
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Ошибка при синхронизации дневников: ${error.message}")
                        _isRefreshing.value = false
                        // Если это первая загрузка и список пустой - показываем ошибку
                        val currentState = _uiState.value
                        if (currentState is JournalsUiState.Content && currentState.journals.isEmpty()) {
                            _uiState.value =
                                JournalsUiState.Error("Ошибка загрузки дневников")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Исключение при загрузке дневников: ${e.message}")
                _isRefreshing.value = false
                val currentState = _uiState.value
                if (currentState is JournalsUiState.Content && currentState.journals.isEmpty()) {
                    _uiState.value = JournalsUiState.Error("Ошибка загрузки дневников")
                }
            }
        }
    }

    /**
     * Повторить загрузку при ошибке.
     * Аналогично loadJournals, но используется для явного повтора пользователем.
     */
    override fun retry() {
        Log.i(TAG, "Повтор загрузки дневников")
        loadJournals()
    }
}
