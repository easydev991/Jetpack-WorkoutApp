package com.swparks.ui.screens.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.swparks.JetpackWorkoutApplication
import com.swparks.data.UserPreferencesRepository
import com.swparks.data.model.Event
import com.swparks.domain.usecase.IGetFutureEventsUseCase
import com.swparks.domain.usecase.IGetPastEventsFlowUseCase
import com.swparks.domain.usecase.ISyncPastEventsUseCase
import com.swparks.ui.model.EventKind
import com.swparks.ui.viewmodel.IEventsViewModel
import com.swparks.util.AppError
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface EventsUIState {
    data class Content(
        val events: List<Event>,
        val selectedTab: EventKind,
        val isLoading: Boolean = false
    ) : EventsUIState

    data class Error(val message: String?) : EventsUIState
    data object InitialLoading : EventsUIState
}

/**
 * ViewModel для экрана списка мероприятий.
 *
 * Управляет загрузкой и отображением мероприятий двух типов:
 * - FUTURE: предстоящие мероприятия (кэш в памяти)
 * - PAST: прошедшие мероприятия (кэш в БД)
 *
 * Обрабатывает сетевые ошибки и ошибки сервера через UserNotifier.
 * Использует Logger для отладочной информации.
 *
 * @param getFutureEventsUseCase Use case для загрузки будущих мероприятий
 * @param getPastEventsFlowUseCase Use case для получения потока прошедших мероприятий
 * @param syncPastEventsUseCase Use case для синхронизации прошедших мероприятий
 * @param userPreferencesRepository Репозиторий настроек пользователя
 * @param userNotifier Интерфейс для обработки и отправки ошибок в UI-слой
 * @param logger Логгер для записи отладочной информации
 */
@Suppress("LongParameterList")
class EventsViewModel(
    private val getFutureEventsUseCase: IGetFutureEventsUseCase,
    private val getPastEventsFlowUseCase: IGetPastEventsFlowUseCase,
    private val syncPastEventsUseCase: ISyncPastEventsUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userNotifier: UserNotifier,
    private val logger: Logger,
) : ViewModel(), IEventsViewModel {
    companion object {
        private const val TAG = "EventsViewModel"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as JetpackWorkoutApplication
                val container = application.container
                EventsViewModel(
                    getFutureEventsUseCase = container.getFutureEventsUseCase,
                    getPastEventsFlowUseCase = container.getPastEventsFlowUseCase,
                    syncPastEventsUseCase = container.syncPastEventsUseCase,
                    userPreferencesRepository = container.userPreferencesRepository,
                    userNotifier = container.userNotifier,
                    logger = container.logger
                )
            }
        }
    }

    private var futureEventsCache: List<Event>? = null
    private var pastEventsCache: List<Event>? = null

    private var hasLoadedPastEvents = false

    private val _selectedTab = MutableStateFlow(EventKind.FUTURE)
    override val selectedTab: StateFlow<EventKind> = _selectedTab.asStateFlow()

    private val _eventsUIState =
        MutableStateFlow<EventsUIState>(EventsUIState.InitialLoading)
    override val eventsUIState: StateFlow<EventsUIState> = _eventsUIState.asStateFlow()

    private val _isAuthorized = MutableStateFlow(false)
    override val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    override val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        logger.d(TAG, "Инициализация EventsViewModel")
        observeAuthorization()
        loadFutureEventsInternal()
        observePastEvents()
    }

    private fun observeAuthorization() {
        viewModelScope.launch {
            userPreferencesRepository.isAuthorized.collect { isAuth ->
                _isAuthorized.value = isAuth
            }
        }
    }

    private fun loadFutureEventsInternal() {
        viewModelScope.launch {
            logger.d(TAG, "Загрузка будущих мероприятий")
            _eventsUIState.value =
                EventsUIState.Content(
                    events = emptyList(),
                    selectedTab = EventKind.FUTURE,
                    isLoading = true
                )
            try {
                val result = getFutureEventsUseCase()
                result.fold(
                    onSuccess = { events ->
                        futureEventsCache = events
                        logger.i(TAG, "Загружено ${events.size} будущих мероприятий")
                        _eventsUIState.value = EventsUIState.Content(
                            events = events,
                            selectedTab = EventKind.FUTURE,
                            isLoading = false
                        )
                    },
                    onFailure = { exception ->
                        logger.e(
                            TAG,
                            "Ошибка загрузки будущих мероприятий: ${exception.message}",
                            exception
                        )
                        _eventsUIState.value = EventsUIState.Error(message = exception.message)
                    }
                )
            } catch (e: IOException) {
                logger.e(TAG, "Сетевая ошибка при загрузке будущих мероприятий", e)
                userNotifier.handleError(
                    AppError.Network(
                        message = "Не удалось загрузить мероприятия. Проверьте подключение к интернету.",
                        throwable = e
                    )
                )
                _eventsUIState.value = EventsUIState.Error(message = e.message)
            } catch (e: HttpException) {
                val errorMessage = e.message()
                logger.e(TAG, "Ошибка сервера при загрузке будущих мероприятий: $errorMessage", e)
                userNotifier.handleError(
                    AppError.Server(
                        code = e.code(),
                        message = "Ошибка сервера при загрузке мероприятий: $errorMessage"
                    )
                )
                _eventsUIState.value = EventsUIState.Error(message = errorMessage)
            }
        }
    }

    private fun observePastEvents() {
        viewModelScope.launch {
            logger.d(TAG, "Подписка на Flow прошедших мероприятий")
            getPastEventsFlowUseCase().collect { pastEvents ->
                val sortedEvents = pastEvents.sortedByDescending { it.beginDate }
                pastEventsCache = sortedEvents
                logger.d(TAG, "Получены прошедшие мероприятия из Flow: ${sortedEvents.size} шт.")
                if (_selectedTab.value == EventKind.PAST) {
                    _eventsUIState.value = EventsUIState.Content(
                        events = sortedEvents,
                        selectedTab = EventKind.PAST,
                        isLoading = false
                    )
                }
            }
        }
    }

    override fun onTabSelected(tab: EventKind) {
        logger.d(TAG, "Переключение вкладки: $tab")
        _selectedTab.value = tab

        if (tab == EventKind.FUTURE) {
            val events = futureEventsCache ?: emptyList()
            _eventsUIState.value = EventsUIState.Content(
                events = events,
                selectedTab = EventKind.FUTURE,
                isLoading = false
            )
        } else {
            val events = pastEventsCache ?: emptyList()
            _eventsUIState.value = EventsUIState.Content(
                events = events,
                selectedTab = EventKind.PAST,
                isLoading = false
            )

            if (!hasLoadedPastEvents) {
                logger.d(TAG, "Первая загрузка PAST - запуск синхронизации")
                syncPastEventsInternal()
            }
        }
    }

    override fun refresh() {
        logger.d(TAG, "Обновление списка мероприятий")
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                when (_selectedTab.value) {
                    EventKind.FUTURE -> loadFutureEventsInternal()
                    EventKind.PAST -> syncPastEventsInternal()
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun syncPastEventsInternal() {
        if (hasLoadedPastEvents) {
            logger.d(TAG, "Прошедшие мероприятия уже загружены, пропуск синхронизации")
            return
        }
        viewModelScope.launch {
            try {
                logger.d(TAG, "Начало синхронизации прошедших мероприятий")
                val currentState = _eventsUIState.value as? EventsUIState.Content
                val currentTab = _selectedTab.value
                _eventsUIState.value = EventsUIState.Content(
                    events = currentState?.events ?: emptyList(),
                    selectedTab = currentTab,
                    isLoading = true
                )
                logger.d(TAG, "Вызов syncPastEventsUseCase()")
                val result = syncPastEventsUseCase()
                logger.d(TAG, "Результат syncPastEvents: isSuccess=${result.isSuccess}")
                result.fold(
                    onSuccess = {
                        hasLoadedPastEvents = true
                        logger.i(TAG, "Синхронизация прошедших мероприятий успешна")
                    },
                    onFailure = { exception ->
                        logger.e(TAG, "Ошибка синхронизации: ${exception.message}", exception)
                        userNotifier.handleError(
                            AppError.Network(
                                message = "Не удалось синхронизировать мероприятия",
                                throwable = exception
                            )
                        )
                        _eventsUIState.value = EventsUIState.Error(message = exception.message)
                    }
                )
            } catch (e: Exception) {
                logger.e(TAG, "Исключение при синхронизации прошедших мероприятий: ${e.message}", e)
                _eventsUIState.value = EventsUIState.Error(message = e.message)
            }
        }
    }

    override fun onEventClick(event: Event) {
        logger.d(TAG, "Нажато мероприятие: ${event.title} (id=${event.id})")
    }

    override fun onFabClick() {
        logger.d(TAG, "Нажатие FAB: создание мероприятия")
    }
}
