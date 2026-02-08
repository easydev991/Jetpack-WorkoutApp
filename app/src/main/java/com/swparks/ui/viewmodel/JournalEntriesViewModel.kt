package com.swparks.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.domain.usecase.ICanDeleteJournalEntryUseCase
import com.swparks.domain.usecase.IDeleteJournalEntryUseCase
import com.swparks.domain.usecase.IGetJournalEntriesUseCase
import com.swparks.domain.usecase.ISyncJournalEntriesUseCase
import com.swparks.ui.state.JournalEntriesUiState
import com.swparks.util.AppError
import com.swparks.util.ErrorReporter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel для экрана списка записей в дневнике.
 *
 * Управляет отображением списка записей с поддержкой
 * offline-first чтения и online-first синхронизации.
 *
 * @param userId Идентификатор пользователя
 * @param journalId Идентификатор дневника
 * @param getJournalEntriesUseCase Use case для получения потока записей
 * @param syncJournalEntriesUseCase Use case для синхронизации записей с сервером
 * @param deleteJournalEntryUseCase Use case для удаления записи
 * @param canDeleteJournalEntryUseCase Use case для проверки возможности удаления записи
 * @param errorReporter Обработчик ошибок для отправки ошибок в систему мониторинга
 */
class JournalEntriesViewModel(
    private val userId: Long,
    private val journalId: Long,
    private val getJournalEntriesUseCase: IGetJournalEntriesUseCase,
    private val syncJournalEntriesUseCase: ISyncJournalEntriesUseCase,
    private val deleteJournalEntryUseCase: IDeleteJournalEntryUseCase,
    private val canDeleteJournalEntryUseCase: ICanDeleteJournalEntryUseCase,
    private val errorReporter: ErrorReporter
) : ViewModel(), IJournalEntriesViewModel {

    private companion object {
        private const val TAG = "JournalEntriesViewModel"
    }

    // UI State (реализует интерфейс IJournalEntriesViewModel)
    private val _uiState =
        MutableStateFlow<JournalEntriesUiState>(JournalEntriesUiState.InitialLoading)
    override val uiState: StateFlow<JournalEntriesUiState> = _uiState.asStateFlow()

    // Индикатор обновления (pull-to-refresh) (реализует интерфейс IJournalEntriesViewModel)
    private val _isRefreshing = MutableStateFlow(false)
    override val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Индикатор удаления записи (реализует интерфейс IJournalEntriesViewModel)
    private val _isDeleting = MutableStateFlow(false)
    override val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    // Поток событий UI (реализует интерфейс IJournalEntriesViewModel)
    private val _events = MutableSharedFlow<JournalEntriesEvent>()
    override val events: SharedFlow<JournalEntriesEvent> = _events.asSharedFlow()

    init {
        Log.i(TAG, "Инициализация JournalEntriesViewModel: userId=$userId, journalId=$journalId")
        observeEntries()
        loadEntries()
    }

    /**
     * Подписаться на поток записей из Use Case (Single Source of Truth).
     * При получении данных обновляем UI State.
     */
    private fun observeEntries() {
        viewModelScope.launch {
            getJournalEntriesUseCase(userId, journalId).collect { entries ->
                Log.i(TAG, "Получены записи из Flow: ${entries.size}")
                val firstEntryId = entries.minByOrNull { it.id }?.id
                _uiState.value =
                    JournalEntriesUiState.Content(
                        entries = entries,
                        isRefreshing = _isRefreshing.value,
                        firstEntryId = firstEntryId
                    )
            }
        }
    }

    /**
     * Загрузить записи с сервера.
     * Устанавливает флаг обновления и триггерит синхронизацию.
     */
    @Suppress("TooGenericExceptionCaught")
    override fun loadEntries() {
        viewModelScope.launch {
            try {
                Log.i(TAG, "Синхронизация записей: userId=$userId, journalId=$journalId")
                _isRefreshing.value = true

                val result = syncJournalEntriesUseCase(userId, journalId)
                result.fold(
                    onSuccess = {
                        Log.i(TAG, "Синхронизация записей успешна")
                        _isRefreshing.value = false
                    },
                    onFailure = { error ->
                        val message = "Ошибка при синхронизации записей: ${error.message}"
                        Log.e(TAG, message)
                        errorReporter.handleError(AppError.Generic(message, error))
                        _isRefreshing.value = false
                        // Если это первая загрузка и список пустой - показываем ошибку
                        val currentState = _uiState.value
                        if (currentState is JournalEntriesUiState.Content && currentState.entries.isEmpty()) {
                            _uiState.value =
                                JournalEntriesUiState.Error("Ошибка загрузки записей")
                        }
                    }
                )
            } catch (e: Exception) {
                val message = "Исключение при загрузке записей: ${e.message}"
                Log.e(TAG, message)
                errorReporter.handleError(AppError.Generic(message, e))
                _isRefreshing.value = false
                val currentState = _uiState.value
                if (currentState is JournalEntriesUiState.Content && currentState.entries.isEmpty()) {
                    _uiState.value = JournalEntriesUiState.Error("Ошибка загрузки записей")
                }
            }
        }
    }

    /**
     * Повторить загрузку при ошибке.
     * Аналогично loadEntries, но используется для явного повтора пользователем.
     */
    override fun retry() {
        Log.i(TAG, "Повтор загрузки записей")
        loadEntries()
    }

    /**
     * Удалить запись из дневника.
     *
     * Удаляет запись через use case и отправляет событие для Snackbar
     * с результатом операции (успех или ошибка).
     *
     * @param entryId Идентификатор записи
     */
    @Suppress("TooGenericExceptionCaught")
    override fun deleteEntry(entryId: Long) {
        viewModelScope.launch {
            try {
                Log.i(TAG, "Удаление записи: entryId=$entryId")
                _isDeleting.value = true

                val result = deleteJournalEntryUseCase(userId, journalId, entryId)
                result
                    .onSuccess {
                        Log.i(TAG, "Запись успешно удалена")
                        _events.emit(JournalEntriesEvent.ShowSnackbar("Запись удалена"))
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Ошибка при удалении записи: ${error.message}")
                        errorReporter.handleError(
                            AppError.Generic(
                                error.message ?: "Ошибка удаления записи", error
                            )
                        )
                        _events.emit(
                            JournalEntriesEvent.ShowSnackbar(error.message ?: "Ошибка удаления")
                        )
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Исключение при удалении записи: ${e.message}")
                errorReporter.handleError(AppError.Generic("Ошибка удаления записи", e))
                _events.emit(JournalEntriesEvent.ShowSnackbar("Ошибка удаления"))
            } finally {
                _isDeleting.value = false
            }
        }
    }

    /**
     * Проверить, можно ли удалить запись.
     *
     * Первую запись (с минимальным id) нельзя удалить.
     *
     * @param entryId Идентификатор записи
     * @return true если удаление разрешено, false если это первая запись
     */
    override suspend fun canDeleteEntry(entryId: Long): Boolean {
        return canDeleteJournalEntryUseCase(entryId, journalId)
    }
}
