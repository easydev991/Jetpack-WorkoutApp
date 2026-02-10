package com.swparks.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.data.UserPreferencesRepository
import com.swparks.data.repository.SWRepository
import com.swparks.domain.usecase.ICanDeleteJournalEntryUseCase
import com.swparks.domain.usecase.IDeleteJournalEntryUseCase
import com.swparks.domain.usecase.IGetJournalEntriesUseCase
import com.swparks.domain.usecase.ISyncJournalEntriesUseCase
import com.swparks.ui.model.JournalAccess
import com.swparks.ui.model.canCreateEntry
import com.swparks.ui.state.JournalEntriesUiState
import com.swparks.util.AppError
import com.swparks.util.ErrorReporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Зависимости для JournalEntriesViewModel.
 *
 * Используется для уменьшения количества параметров конструктора.
 */
data class JournalEntriesDeps(
    val getJournalEntriesUseCase: IGetJournalEntriesUseCase,
    val syncJournalEntriesUseCase: ISyncJournalEntriesUseCase,
    val deleteJournalEntryUseCase: IDeleteJournalEntryUseCase,
    val canDeleteJournalEntryUseCase: ICanDeleteJournalEntryUseCase,
    val preferencesRepository: UserPreferencesRepository,
    val swRepository: SWRepository,
    val savedStateHandle: SavedStateHandle,
    val errorReporter: ErrorReporter,
)

/**
 * ViewModel для экрана списка записей в дневнике.
 *
 * Управляет отображением списка записей с поддержкой
 * offline-first чтения и online-first синхронизации.
 *
 * @param journalOwnerId Идентификатор владельца дневника
 * @param journalId Идентификатор дневника
 * @param deps Зависимости для ViewModel (use cases, repositories, error reporter)
 */
class JournalEntriesViewModel(
    private val journalOwnerId: Long,
    private val journalId: Long,
    private val deps: JournalEntriesDeps
) : ViewModel(), IJournalEntriesViewModel {

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

    // Признак возможности создания записей (реализует интерфейс IJournalEntriesViewModel)
    private val _canCreateEntry = MutableStateFlow(false)
    override val canCreateEntry: StateFlow<Boolean> = _canCreateEntry.asStateFlow()

    // Текущий пользователь (для canEditEntry)
    private val _currentUserId = MutableStateFlow<Long?>(null)

    // Поток событий UI (реализует интерфейс IJournalEntriesViewModel)
    private val _events = MutableSharedFlow<JournalEntriesEvent>()
    override val events: SharedFlow<JournalEntriesEvent> = _events.asSharedFlow()

    init {
        observeCurrentUserId()
        // Важно: observeCanCreateEntry() вызывается первым для вычисления прав доступа до подписки на записи
        // Это предотвращает гонку состояния, когда observeEntries() создает Content до обновления canCreateEntry
        observeCanCreateEntry()
        observeEntries()
        loadEntries()
    }

    /**
     * Подписаться на изменения текущего пользователя.
     */
    private fun observeCurrentUserId() {
        viewModelScope.launch {
            deps.preferencesRepository.currentUserId.collect { userId ->
                _currentUserId.value = userId
            }
        }
    }

    /**
     * Подписаться на поток записей из Use Case (Single Source of Truth).
     * При получении данных обновляем UI State.
     */
    private fun observeEntries() {
        viewModelScope.launch {
            deps.getJournalEntriesUseCase(journalOwnerId, journalId).collect { entries ->
                val firstEntryId = entries.minByOrNull { it.id }?.id
                // Используем текущее вычисленное значение canCreateEntry
                val canCreateEntry = _canCreateEntry.value
                _uiState.value =
                    JournalEntriesUiState.Content(
                        entries = entries,
                        isRefreshing = _isRefreshing.value,
                        firstEntryId = firstEntryId,
                        canCreateEntry = canCreateEntry
                    )
            }
        }
    }

    /**
     * Подписаться на изменения параметров для возможности создания записей.
     * Вычисляет canCreateEntry на основе прав доступа, текущего пользователя и друзей.
     */
    private fun observeCanCreateEntry() {
        viewModelScope.launch {
            computeCanCreateEntry().collect { canCreate ->
                _canCreateEntry.value = canCreate

                // Обновляем текущее состояние Content с новым значением canCreateEntry
                val currentState = _uiState.value
                if (currentState is JournalEntriesUiState.Content) {
                    val newState = currentState.copy(canCreateEntry = canCreate)
                    _uiState.value = newState
                }
            }
        }
    }

    /**
     * Вычисляет возможность создания записей на основе прав доступа к дневнику.
     */
    private fun computeCanCreateEntry(): Flow<Boolean> {
        val commentAccessRaw = deps.savedStateHandle.get<String>("commentAccess")
        val commentAccessType = commentAccessRaw
            ?.let { JournalAccess.valueOf(it) }
            ?: JournalAccess.NOBODY // по умолчанию самый строгий режим

        return combine(
            deps.preferencesRepository.currentUserId,
            deps.swRepository.getFriendsFlow().map { friends -> friends.map { it.id } }
        ) { currentUserIdParam, friendsIds ->
            commentAccessType.canCreateEntry(
                journalOwnerId = this@JournalEntriesViewModel.journalOwnerId, // владелец дневника
                mainUserId = currentUserIdParam,
                mainUserFriendsIds = friendsIds
            )
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
                _isRefreshing.value = true

                val result = deps.syncJournalEntriesUseCase(journalOwnerId, journalId)
                result.fold(
                    onSuccess = {
                        _isRefreshing.value = false
                    },
                    onFailure = { error ->
                        val message = "Ошибка при синхронизации записей: ${error.message}"
                        deps.errorReporter.handleError(AppError.Generic(message, error))
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
                deps.errorReporter.handleError(AppError.Generic(message, e))
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
                _isDeleting.value = true

                val result = deps.deleteJournalEntryUseCase(journalOwnerId, journalId, entryId)
                result
                    .onSuccess {
                        _events.emit(JournalEntriesEvent.ShowSnackbar("Запись удалена"))
                    }
                    .onFailure { error ->
                        deps.errorReporter.handleError(
                            AppError.Generic(
                                error.message ?: "Ошибка удаления записи", error
                            )
                        )
                        _events.emit(
                            JournalEntriesEvent.ShowSnackbar(error.message ?: "Ошибка удаления")
                        )
                    }
            } catch (e: Exception) {
                deps.errorReporter.handleError(AppError.Generic("Ошибка удаления записи", e))
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
        return deps.canDeleteJournalEntryUseCase(entryId, journalId)
    }

    /**
     * Обновить список записей (public метод для refresh).
     * Используется для обновления списка после создания или редактирования записи.
     */
    override fun refresh() {
        loadEntries()
    }

    /**
     * Проверить, можно ли редактировать запись.
     *
     * Редактировать записи может только автор записи.
     *
     * @param entry Запись в дневнике
     * @return true если редактирование разрешено (автор записи)
     */
    override fun canEditEntry(entry: com.swparks.domain.model.JournalEntry): Boolean {
        val currentUserId = _currentUserId.value
        return currentUserId == entry.authorId
    }
}
