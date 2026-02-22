package com.swparks.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.R
import com.swparks.domain.provider.ResourcesProvider
import com.swparks.domain.usecase.IDeleteJournalUseCase
import com.swparks.domain.usecase.IEditJournalSettingsUseCase
import com.swparks.domain.usecase.IGetJournalsUseCase
import com.swparks.domain.usecase.ISyncJournalsUseCase
import com.swparks.ui.model.JournalAccess
import com.swparks.ui.state.JournalsUiState
import com.swparks.util.AppError
import com.swparks.util.UserNotifier
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
 * @param deleteJournalUseCase Use case для удаления дневника
 * @param editJournalSettingsUseCase Use case для редактирования настроек дневника
 * @param userNotifier Обработчик ошибок для отправки ошибок в систему мониторинга
 * @param resources Провайдер строковых ресурсов
 */
@Suppress("LongParameterList")
class JournalsViewModel(
    private val userId: Long,
    private val getJournalsUseCase: IGetJournalsUseCase,
    private val syncJournalsUseCase: ISyncJournalsUseCase,
    private val deleteJournalUseCase: IDeleteJournalUseCase,
    private val editJournalSettingsUseCase: IEditJournalSettingsUseCase,
    private val userNotifier: UserNotifier,
    private val resources: ResourcesProvider
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

    // Индикатор удаления дневника (реализует интерфейс IJournalsViewModel)
    private val _isDeleting = MutableStateFlow(false)
    override val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    // Индикатор сохранения настроек дневника (реализует интерфейс IJournalSettingsViewModel)
    private val _isSavingSettings = MutableStateFlow(false)
    override val isSavingSettings: StateFlow<Boolean> = _isSavingSettings.asStateFlow()

    // Поток событий UI (реализует интерфейс IJournalsViewModel)
    private val _events = MutableSharedFlow<JournalsEvent>()
    override val events: SharedFlow<JournalsEvent> = _events.asSharedFlow()

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
                    JournalsUiState.Content(
                        journals = journals,
                        isRefreshing = _isRefreshing.value,
                        isSavingJournalSettings = (_uiState.value as? JournalsUiState.Content)
                            ?.isSavingJournalSettings ?: false
                    )
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
                                JournalsUiState.Error(resources.getString(R.string.error_loading_journals))
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Исключение при загрузке дневников: ${e.message}")
                _isRefreshing.value = false
                val currentState = _uiState.value
                if (currentState is JournalsUiState.Content && currentState.journals.isEmpty()) {
                    _uiState.value =
                        JournalsUiState.Error(resources.getString(R.string.error_loading_journals))
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

    /**
     * Удалить дневник.
     *
     * Удаляет дневник через use case и отправляет событие для Snackbar
     * с результатом операции (успех или ошибка).
     *
     * @param journalId Идентификатор дневника
     */
    @Suppress("TooGenericExceptionCaught")
    override fun deleteJournal(journalId: Long) {
        viewModelScope.launch {
            try {
                Log.i(TAG, "Удаление дневника: journalId=$journalId")
                _isDeleting.value = true

                val result = deleteJournalUseCase(userId, journalId)
                result
                    .onSuccess {
                        Log.i(TAG, "Дневник успешно удален")
                        userNotifier.showInfo(resources.getString(R.string.journal_deleted))
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Ошибка при удалении дневника: ${error.message}")
                        userNotifier.handleError(
                            AppError.Generic(
                                error.message ?: resources.getString(R.string.error_delete_journal),
                                error
                            )
                        )
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Исключение при удалении дневника: ${e.message}")
                userNotifier.handleError(
                    AppError.Generic(
                        resources.getString(R.string.error_delete_journal),
                        e
                    )
                )
            } finally {
                _isDeleting.value = false
            }
        }
    }

    /**
     * Редактировать настройки дневника.
     *
     * Обновляет настройки дневника через use case и перезагружает список дневников
     * с сервера, чтобы получить обновленные данные.
     *
     * После успешного обновления эмитится событие [JournalsEvent.JournalSettingsSaved].
     * При ошибке эмитится событие [JournalsEvent.ShowSnackbar].
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
            setSavingJournalSettings(true)

            try {
                Log.i(TAG, "Редактирование настроек дневника: journalId=$journalId, title=$title")

                val result = editJournalSettingsUseCase(
                    journalId = journalId,
                    title = title,
                    userId = userId,
                    viewAccess = viewAccess,
                    commentAccess = commentAccess
                )

                result.fold(
                    onSuccess = {
                        Log.i(TAG, "Настройки дневника успешно обновлены")
                        handleJournalSettingsSync(journalId)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Ошибка при редактировании настроек дневника: ${error.message}")
                        handleJournalSettingsError(error.message)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Исключение при редактировании настроек дневника: ${e.message}")
                handleJournalSettingsError(null)
            }
        }
    }

    /**
     * Установить флаг загрузки настроек дневника
     */
    private fun setSavingJournalSettings(isSaving: Boolean) {
        _isSavingSettings.value = isSaving
        val currentState = _uiState.value
        if (currentState is JournalsUiState.Content) {
            _uiState.value = currentState.copy(isSavingJournalSettings = isSaving)
        }
    }

    /**
     * Обработать синхронизацию после успешного редактирования настроек
     */
    private suspend fun handleJournalSettingsSync(journalId: Long) {
        syncJournalsUseCase(userId).fold(
            onSuccess = {
                Log.i(TAG, "Дневники перезагружены после обновления настроек")
                handleJournalSettingsSuccess(journalId)
            },
            onFailure = { error ->
                Log.e(TAG, "Ошибка при перезагрузке дневников: ${error.message}")
                handleJournalSettingsSuccess(journalId)
            }
        )
    }

    /**
     * Обработать успешное сохранение настроек дневника
     */
    private fun handleJournalSettingsSuccess(journalId: Long) {
        val updatedJournal =
            (_uiState.value as? JournalsUiState.Content)?.journals?.find { it.id == journalId }

        setSavingJournalSettings(false)

        updatedJournal?.let {
            viewModelScope.launch {
                _events.emit(JournalsEvent.JournalSettingsSaved(it))
            }
        }

        userNotifier.showInfo(resources.getString(R.string.journal_settings_saved))
    }

    /**
     * Обработать ошибку при редактировании настроек дневника
     */
    private fun handleJournalSettingsError(errorMessage: String?) {
        setSavingJournalSettings(false)
        userNotifier.handleError(
            AppError.Generic(
                errorMessage ?: resources.getString(R.string.error_save_journal_settings)
            )
        )
    }
}
