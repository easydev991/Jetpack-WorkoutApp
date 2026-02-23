package com.swparks.ui.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.R
import com.swparks.data.UserPreferencesRepository
import com.swparks.data.model.toDomain
import com.swparks.data.repository.SWRepository
import com.swparks.domain.provider.ResourcesProvider
import com.swparks.domain.usecase.ICanDeleteJournalEntryUseCase
import com.swparks.domain.usecase.IDeleteJournalEntryUseCase
import com.swparks.domain.usecase.IEditJournalSettingsUseCase
import com.swparks.domain.usecase.IGetJournalEntriesUseCase
import com.swparks.domain.usecase.ISyncJournalEntriesUseCase
import com.swparks.ui.model.JournalAccess
import com.swparks.ui.model.canCreateEntry
import com.swparks.ui.state.JournalEntriesUiState
import com.swparks.util.AppError
import com.swparks.util.UserNotifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
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
    val editJournalSettingsUseCase: IEditJournalSettingsUseCase,
    val preferencesRepository: UserPreferencesRepository,
    val swRepository: SWRepository,
    val savedStateHandle: SavedStateHandle,
    val userNotifier: UserNotifier,
    val resources: ResourcesProvider,
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

    private companion object {
        const val TAG = "JournalEntriesVM"
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

    // Индикатор сохранения настроек дневника (реализует интерфейс IJournalSettingsViewModel)
    private val _isSavingSettings = MutableStateFlow(false)
    override val isSavingSettings: StateFlow<Boolean> = _isSavingSettings.asStateFlow()

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
        observeJournal()
        loadJournal()
        loadEntries()
    }

    /**
     * Загрузить информацию о дневнике с сервера и сохранить в кэш.
     * Необходимо для отображения кнопки настроек (требуется journal.ownerId).
     */
    @Suppress("TooGenericExceptionCaught")
    private fun loadJournal() {
        viewModelScope.launch {
            try {
                // Проверяем кэш перед загрузкой с сервера
                val cachedJournal = deps.swRepository.observeJournalById(journalId).first()
                if (cachedJournal != null) {
                    Log.i(TAG, "Дневник уже в кэше: journalId=$journalId, пропускаем загрузку")
                    return@launch
                }

                val result = deps.swRepository.getJournal(journalOwnerId, journalId)
                result.fold(
                    onSuccess = { journalResponse ->
                        val journal = journalResponse.toDomain()
                        deps.swRepository.saveJournalToCache(journal)
                        Log.i(TAG, "Дневник загружен и сохранён в кэш: journalId=$journalId")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Ошибка при загрузке дневника: ${error.message}")
                        // Не показываем ошибку пользователю - записи могут загрузиться успешно
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Исключение при загрузке дневника: ${e.message}")
            }
        }
    }

    /**
     * Подписаться на изменения дневника через Repository.
     * Обновляет UI State при изменении дневника в кэше.
     */
    private fun observeJournal() {
        deps.swRepository.observeJournalById(journalId)
            .onEach { journal ->
                _uiState.update { state ->
                    if (state is JournalEntriesUiState.Content) {
                        state.copy(journal = journal)
                    } else state
                }
            }
            .launchIn(viewModelScope)
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
                // Сохраняем текущий журнал при обновлении записей
                val currentJournal = (_uiState.value as? JournalEntriesUiState.Content)?.journal
                _uiState.value =
                    JournalEntriesUiState.Content(
                        entries = entries,
                        isRefreshing = _isRefreshing.value,
                        firstEntryId = firstEntryId,
                        canCreateEntry = canCreateEntry,
                        journal = currentJournal
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
            // Логирование для отладки FAB
            Log.d(TAG, "=== computeCanCreateEntry DEBUG ===")
            Log.d(TAG, "currentUserId=$currentUserIdParam, journalOwnerId=$journalOwnerId")
            Log.d(TAG, "commentAccess=$commentAccessType, friendsIds=$friendsIds")
            Log.d(TAG, "isFriend=${friendsIds.contains(journalOwnerId)}")
            Log.d(TAG, "==================")

            val result = commentAccessType.canCreateEntry(
                journalOwnerId = this@JournalEntriesViewModel.journalOwnerId, // владелец дневника
                mainUserId = currentUserIdParam,
                mainUserFriendsIds = friendsIds
            )
            Log.d(TAG, "canCreateEntry result: $result")
            result
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
                        deps.userNotifier.handleError(AppError.Generic(message, error))
                        _isRefreshing.value = false
                        // Если это первая загрузка и список пустой - показываем ошибку
                        val currentState = _uiState.value
                        if (currentState is JournalEntriesUiState.Content && currentState.entries.isEmpty()) {
                            _uiState.value =
                                JournalEntriesUiState.Error(deps.resources.getString(R.string.error_loading_entries))
                        }
                    }
                )
            } catch (e: Exception) {
                val message = "Исключение при загрузке записей: ${e.message}"
                deps.userNotifier.handleError(AppError.Generic(message, e))
                _isRefreshing.value = false
                val currentState = _uiState.value
                if (currentState is JournalEntriesUiState.Content && currentState.entries.isEmpty()) {
                    _uiState.value =
                        JournalEntriesUiState.Error(deps.resources.getString(R.string.error_loading_entries))
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
     * Удаляет запись через use case. Информационные сообщения и ошибки
     * отправляются через [UserNotifier].
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
                        deps.userNotifier.showInfo(deps.resources.getString(R.string.entry_deleted))
                    }
                    .onFailure { error ->
                        deps.userNotifier.handleError(
                            AppError.Generic(
                                error.message
                                    ?: deps.resources.getString(R.string.error_delete_entry), error
                            )
                        )
                    }
            } catch (e: Exception) {
                deps.userNotifier.handleError(
                    AppError.Generic(
                        deps.resources.getString(R.string.error_delete_entry),
                        e
                    )
                )
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
     * Проверить, можно ли отредактировать запись.
     *
     * Редактировать записи может владелец дневника или автор записи.
     *
     * @param entry Запись в дневнике
     * @return true если редактирование разрешено
     */
    override fun canEditEntry(entry: com.swparks.domain.model.JournalEntry): Boolean {
        val currentUserId = _currentUserId.value
        val commentAccessRaw = deps.savedStateHandle.get<String>("commentAccess")
        val commentAccessType =
            commentAccessRaw?.let { JournalAccess.valueOf(it) } ?: JournalAccess.NOBODY

        // Логирование для отладки FAB
        Log.d(TAG, "=== canEditEntry DEBUG ===")
        Log.d(
            TAG,
            "currentUserId=$currentUserId, journalOwnerId=$journalOwnerId, commentAccess=$commentAccessType"
        )
        Log.d(TAG, "entry.authorId=${entry.authorId}, entry.id=${entry.id}")
        Log.d(TAG, "==================")

        if (currentUserId == null) return false
        if (entry.authorId == null) return false
        val isOwner = journalOwnerId == currentUserId
        return isOwner || entry.authorId == currentUserId
    }

    /**
     * Проверить, можно ли удалить запись.
     *
     * Удалять записи может владелец дневника или автор записи.
     * Первая запись (с минимальным id) не может быть удалена.
     *
     * @param entry Запись в дневнике
     * @return true если удаление разрешено
     */
    override fun canDeleteEntry(entry: com.swparks.domain.model.JournalEntry): Boolean {
        val currentUserId = _currentUserId.value
        val commentAccessRaw = deps.savedStateHandle.get<String>("commentAccess")
        val commentAccessType =
            commentAccessRaw?.let { JournalAccess.valueOf(it) } ?: JournalAccess.NOBODY

        // Логирование для отладки FAB
        Log.d(TAG, "=== canDeleteEntry DEBUG ===")
        Log.d(
            TAG,
            "currentUserId=$currentUserId, journalOwnerId=$journalOwnerId, commentAccess=$commentAccessType"
        )
        Log.d(TAG, "entry.authorId=${entry.authorId}, entry.id=${entry.id}")
        Log.d(TAG, "==================")

        if (currentUserId == null) return false
        val isOwner = journalOwnerId == currentUserId
        return isOwner || entry.authorId == currentUserId
    }

    /**
     * Редактировать настройки дневника.
     *
     * После успешного обновления эмитится событие [JournalEntriesEvent.JournalSettingsSaved].
     * При ошибке эмитится событие [JournalEntriesEvent.ShowSnackbar].
     *
     * @param journalId Идентификатор дневника
     * @param title Новое название дневника
     * @param viewAccess Новый уровень доступа для просмотра
     * @param commentAccess Новый уровень доступа для комментариев
     */
    @Suppress("TooGenericExceptionCaught")
    override fun editJournalSettings(
        journalId: Long,
        title: String,
        viewAccess: JournalAccess,
        commentAccess: JournalAccess
    ) {
        viewModelScope.launch {
            try {
                _isSavingSettings.value = true

                Log.i(TAG, "Редактирование настроек дневника: journalId=$journalId, title=$title")

                val result = deps.editJournalSettingsUseCase(
                    journalId = journalId,
                    title = title,
                    userId = journalOwnerId,
                    viewAccess = viewAccess,
                    commentAccess = commentAccess
                )

                result.fold(
                    onSuccess = {
                        Log.i(TAG, "Настройки дневника успешно обновлены")
                        // После успешного обновления загружаем дневник заново для обновления кэша
                        loadJournalAfterSettingsUpdate(journalId)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Ошибка при редактировании настроек дневника: ${error.message}")
                        handleEditSettingsError(error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Исключение при редактировании настроек дневника: ${e.message}")
                handleEditSettingsError(e)
            } finally {
                _isSavingSettings.value = false
            }
        }
    }

    /**
     * Загрузить дневник после успешного обновления настроек.
     * Обновляет локальный кэш и эмитит событие об успехе.
     */
    private suspend fun loadJournalAfterSettingsUpdate(journalId: Long) {
        deps.swRepository.getJournal(journalOwnerId, journalId)
            .fold(
                onSuccess = { journalResponse ->
                    val journal = journalResponse.toDomain()
                    // Обновляем кэш через Repository
                    deps.swRepository.saveJournalToCache(journal)

                    // Эмитим событие об успехе
                    _events.emit(JournalEntriesEvent.JournalSettingsSaved(journal))
                    deps.userNotifier.showInfo(deps.resources.getString(R.string.settings_saved))
                },
                onFailure = { error ->
                    Log.e(TAG, "Ошибка загрузки дневника после обновления: ${error.message}")
                    // Даже если не удалось загрузить, считаем операцию успешной
                    // так как сервер подтвердил обновление
                    val currentJournal = (_uiState.value as? JournalEntriesUiState.Content)?.journal
                    if (currentJournal != null) {
                        _events.emit(JournalEntriesEvent.JournalSettingsSaved(currentJournal))
                    }
                    deps.userNotifier.showInfo(deps.resources.getString(R.string.settings_saved))
                }
            )
    }

    /**
     * Обработать ошибку редактирования настроек.
     */
    private fun handleEditSettingsError(error: Throwable) {
        val message = when {
            error.message?.contains("403") == true -> {
                deps.resources.getString(R.string.error_no_permission)
            }

            else -> {
                deps.resources.getString(R.string.error_saving_settings)
            }
        }
        deps.userNotifier.handleError(AppError.Generic(message, error))
    }
}
