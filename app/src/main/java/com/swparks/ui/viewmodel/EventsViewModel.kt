package com.swparks.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.swparks.JetpackWorkoutApplication
import com.swparks.data.UserPreferencesRepository
import com.swparks.data.model.Event
import com.swparks.data.model.User
import com.swparks.data.repository.SWRepository
import com.swparks.domain.repository.CountriesRepository
import com.swparks.domain.usecase.IGetFutureEventsFlowUseCase
import com.swparks.domain.usecase.IGetPastEventsFlowUseCase
import com.swparks.domain.usecase.ISyncFutureEventsUseCase
import com.swparks.domain.usecase.ISyncPastEventsUseCase
import com.swparks.ui.model.EventKind
import com.swparks.ui.state.EventsUIState
import com.swparks.util.AppError
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * События UI экрана списка мероприятий.
 *
 * Используются для одноразовых действий навигации.
 */
sealed class EventsEvent {
    /**
     * Навигация к экрану создания мероприятия.
     */
    data object NavigateToCreateEvent : EventsEvent()

    /**
     * Показать алерт с правилом создания мероприятия.
     *
     * Отображается когда пользователь без площадок тренировки
     * пытается создать мероприятие.
     */
    data object ShowEventCreationRule : EventsEvent()
}

/**
 * ViewModel для экрана списка мероприятий.
 *
 * Управляет загрузкой и отображением мероприятий двух типов:
 * - FUTURE: предстоящие мероприятия (Flow из Repository)
 * - PAST: прошедшие мероприятия (Flow из Room)
 *
 * Обрабатывает сетевые ошибки и ошибки сервера через UserNotifier.
 * Использует Logger для отладочной информации.
 *
 * @param getFutureEventsFlowUseCase Use case для получения потока будущих мероприятий
 * @param syncFutureEventsUseCase Use case для синхронизации будущих мероприятий
 * @param getPastEventsFlowUseCase Use case для получения потока прошедших мероприятий
 * @param syncPastEventsUseCase Use case для синхронизации прошедших мероприятий
 * @param userPreferencesRepository Репозиторий настроек пользователя
 * @param countriesRepository Репозиторий для работы со странами и городами
 * @param userNotifier Интерфейс для обработки и отправки ошибок в UI-слой
 * @param logger Логгер для записи отладочной информации
 */
@Suppress("LongParameterList")
class EventsViewModel(
    private val getFutureEventsFlowUseCase: IGetFutureEventsFlowUseCase,
    private val syncFutureEventsUseCase: ISyncFutureEventsUseCase,
    private val getPastEventsFlowUseCase: IGetPastEventsFlowUseCase,
    private val syncPastEventsUseCase: ISyncPastEventsUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val countriesRepository: CountriesRepository,
    private val userNotifier: UserNotifier,
    private val logger: Logger,
    private val swRepository: SWRepository,
    private val initialTab: EventKind = EventKind.FUTURE,
) : ViewModel(), IEventsViewModel {
    companion object {
        private const val TAG = "EventsViewModel"
        private const val SCREENSHOT_TEST_APPLICATION_CLASS_NAME =
            "com.swparks.screenshots.ScreenshotTestApplication"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as JetpackWorkoutApplication
                val container = application.container
                val initialTab = if (
                    application::class.java.name == SCREENSHOT_TEST_APPLICATION_CLASS_NAME
                ) {
                    EventKind.PAST
                } else {
                    EventKind.FUTURE
                }
                EventsViewModel(
                    getFutureEventsFlowUseCase = container.getFutureEventsFlowUseCase,
                    syncFutureEventsUseCase = container.syncFutureEventsUseCase,
                    getPastEventsFlowUseCase = container.getPastEventsFlowUseCase,
                    syncPastEventsUseCase = container.syncPastEventsUseCase,
                    userPreferencesRepository = container.userPreferencesRepository,
                    countriesRepository = container.countriesRepository,
                    userNotifier = container.userNotifier,
                    logger = container.logger,
                    swRepository = container.swRepository,
                    initialTab = initialTab
                )
            }
        }
    }

    private var futureEventsCache: List<Event>? = null
    private var pastEventsCache: List<Event>? = null
    private var addressesCache: Map<Pair<Int, Int>, String> = emptyMap()

    private var hasLoadedFutureEvents = false
    private var hasLoadedPastEvents = false

    private suspend fun loadAddress(
        countryId: Int,
        cityId: Int
    ): String {
        val cacheKey = countryId to cityId
        addressesCache[cacheKey]?.let { return it }

        return try {
            val country = countriesRepository.getCountryById(countryId.toString())
            val city = countriesRepository.getCityById(cityId.toString())

            val address = when {
                country != null && city != null -> "${country.name}, ${city.name}"
                country != null -> country.name
                city != null -> city.name
                else -> "$countryId, $cityId"
            }
            address
        } catch (e: IOException) {
            logger.e(TAG, "Ошибка загрузки адреса ($countryId, $cityId): ${e.message}")
            "$countryId, $cityId"
        }
    }

    private suspend fun loadAddresses(
        events: List<Event>
    ): Map<Pair<Int, Int>, String> {
        if (events.isEmpty()) return addressesCache

        val uniqueIds = events.map { it.countryID to it.cityID }.distinct()
        val newAddresses = mutableMapOf<Pair<Int, Int>, String>()

        uniqueIds.forEach { (countryId, cityId) ->
            if (!addressesCache.containsKey(countryId to cityId)) {
                val address = loadAddress(countryId, cityId)
                newAddresses[countryId to cityId] = address
            }
        }

        addressesCache = addressesCache + newAddresses
        return addressesCache
    }

    private val _selectedTab = MutableStateFlow(initialTab)
    override val selectedTab: StateFlow<EventKind> = _selectedTab.asStateFlow()

    private val _eventsUIState =
        MutableStateFlow<EventsUIState>(EventsUIState.InitialLoading)
    override val eventsUIState: StateFlow<EventsUIState> = _eventsUIState.asStateFlow()

    private val _isAuthorized = MutableStateFlow(false)
    override val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    override val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _events = Channel<EventsEvent>()
    override val events: Flow<EventsEvent> = _events.receiveAsFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        logger.d(TAG, "Инициализация EventsViewModel")
        observeAuthorization()
        observeCurrentUser()
        observeFutureEvents()
        syncFutureEventsInternal()
        observePastEvents()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            logger.d(TAG, "Подписка на Flow текущего пользователя")
            swRepository.getCurrentUserFlow().collect { user ->
                _currentUser.value = user
                logger.d(
                    TAG,
                    "Текущий пользователь обновлён: ${user?.name}, hasUsedParks=${user?.hasUsedParks}"
                )
            }
        }
    }

    private fun observeAuthorization() {
        viewModelScope.launch {
            userPreferencesRepository.isAuthorized.collect { isAuth ->
                _isAuthorized.value = isAuth
            }
        }
    }

    private fun observeFutureEvents() {
        viewModelScope.launch {
            logger.d(TAG, "Подписка на Flow будущих мероприятий")
            getFutureEventsFlowUseCase().collect { events ->
                val sortedEvents = events.sortedBy { it.beginDate }
                logger.d(TAG, "Получены будущие мероприятия из Flow: ${sortedEvents.size} шт.")
                futureEventsCache = sortedEvents
                if (sortedEvents.isEmpty() && !hasLoadedFutureEvents) {
                    return@collect
                }
                if (_selectedTab.value == EventKind.FUTURE) {
                    val addresses = loadAddresses(sortedEvents)
                    _eventsUIState.value = EventsUIState.Content(
                        events = sortedEvents,
                        selectedTab = EventKind.FUTURE,
                        isLoading = false,
                        addresses = addresses
                    )
                }
            }
        }
    }

    private fun syncFutureEventsInternal(force: Boolean = false) {
        if (!force && hasLoadedFutureEvents) {
            logger.d(TAG, "Будущие мероприятия уже загружены, пропуск синхронизации")
            return
        }
        viewModelScope.launch {
            try {
                logger.d(TAG, "Начало синхронизации будущих мероприятий")

                val result = syncFutureEventsUseCase()
                result.fold(
                    onSuccess = {
                        hasLoadedFutureEvents = true
                        logger.i(TAG, "Синхронизация будущих мероприятий успешна")
                        val events = futureEventsCache
                        if (_selectedTab.value == EventKind.FUTURE && events != null) {
                            _eventsUIState.value = EventsUIState.Content(
                                events = events,
                                selectedTab = EventKind.FUTURE,
                                isLoading = false,
                                addresses = addressesCache
                            )
                        }
                    },
                    onFailure = { exception ->
                        logger.e(TAG, "Ошибка синхронизации: ${exception.message}", exception)
                        userNotifier.handleError(
                            AppError.Network(
                                message = "Не удалось загрузить мероприятия",
                                throwable = exception
                            )
                        )
                        _eventsUIState.value = EventsUIState.Error(
                            message = exception.message,
                            addresses = addressesCache
                        )
                    }
                )
            } catch (e: IOException) {
                logger.e(TAG, "Исключение при синхронизации: ${e.message}", e)
                _eventsUIState.value = EventsUIState.Error(
                    message = e.message,
                    addresses = addressesCache
                )
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
                    val addresses = loadAddresses(sortedEvents)
                    _eventsUIState.value = EventsUIState.Content(
                        events = sortedEvents,
                        selectedTab = EventKind.PAST,
                        isLoading = false,
                        addresses = addresses
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
                isLoading = false,
                addresses = addressesCache
            )
        } else {
            val events = pastEventsCache ?: emptyList()

            if (!hasLoadedPastEvents) {
                logger.d(TAG, "Первая загрузка PAST - загрузка адресов перед показом")
                _eventsUIState.value = EventsUIState.Content(
                    events = emptyList(),
                    selectedTab = EventKind.PAST,
                    isLoading = true,
                    addresses = addressesCache
                )
                viewModelScope.launch {
                    val addresses = loadAddresses(events)
                    _eventsUIState.value = EventsUIState.Content(
                        events = events,
                        selectedTab = EventKind.PAST,
                        isLoading = false,
                        addresses = addresses
                    )
                    syncPastEventsInternal()
                }
            } else {
                _eventsUIState.value = EventsUIState.Content(
                    events = events,
                    selectedTab = EventKind.PAST,
                    isLoading = false,
                    addresses = addressesCache
                )
            }
        }
    }

    override fun refresh() {
        logger.d(TAG, "Обновление списка мероприятий")
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                when (_selectedTab.value) {
                    EventKind.FUTURE -> syncFutureEventsInternal(force = true)
                    EventKind.PAST -> syncPastEventsInternal(force = true)
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun syncPastEventsInternal(force: Boolean = false) {
        if (!force && hasLoadedPastEvents) {
            logger.d(TAG, "Прошедшие мероприятия уже загружены, пропуск синхронизации")
            return
        }
        viewModelScope.launch {
            try {
                logger.d(TAG, "Начало синхронизации прошедших мероприятий")
                val currentState = _eventsUIState.value as? EventsUIState.Content
                val currentTab = _selectedTab.value

                val shouldShowLoading = !force && currentState?.events?.isEmpty() != false
                if (shouldShowLoading) {
                    _eventsUIState.value = EventsUIState.Content(
                        events = currentState?.events ?: emptyList(),
                        selectedTab = currentTab,
                        isLoading = true,
                        addresses = addressesCache
                    )
                }

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
                        _eventsUIState.value = EventsUIState.Error(
                            message = exception.message,
                            addresses = addressesCache
                        )
                    }
                )
            } catch (e: IOException) {
                logger.e(TAG, "Исключение при синхронизации прошедших мероприятий: ${e.message}", e)
                _eventsUIState.value = EventsUIState.Error(
                    message = e.message,
                    addresses = addressesCache
                )
            }
        }
    }

    override fun onEventClick(event: Event) {
        logger.d(TAG, "Нажато мероприятие: ${event.title} (id=${event.id})")
    }

    override fun onFabClick() {
        logger.d(TAG, "Нажатие FAB: создание мероприятия")
        viewModelScope.launch {
            val user = _currentUser.value
            when {
                user == null -> {
                    logger.d(TAG, "Пользователь не авторизован, действие не выполняется")
                }

                !user.hasUsedParks -> {
                    logger.d(TAG, "У пользователя нет площадок тренировки, показываем алерт")
                    _events.send(EventsEvent.ShowEventCreationRule)
                }

                else -> {
                    logger.d(TAG, "У пользователя есть площадки, переход к созданию мероприятия")
                    _events.send(EventsEvent.NavigateToCreateEvent)
                }
            }
        }
    }

    override fun addCreatedEvent(event: Event) {
        logger.d(TAG, "Добавление созданного мероприятия в кэш: ${event.id}")

        val isFutureEvent = event.isCurrent
        if (isFutureEvent) {
            val currentCache = futureEventsCache ?: emptyList()
            futureEventsCache = (currentCache + event).sortedBy { it.beginDate }

            if (_selectedTab.value == EventKind.FUTURE) {
                val events = futureEventsCache ?: emptyList()
                viewModelScope.launch {
                    val addresses = loadAddresses(events)
                    _eventsUIState.value = EventsUIState.Content(
                        events = events,
                        selectedTab = EventKind.FUTURE,
                        isLoading = false,
                        addresses = addresses
                    )
                }
            }
        } else {
            val currentCache = pastEventsCache ?: emptyList()
            pastEventsCache = (currentCache + event).sortedByDescending { it.beginDate }

            if (_selectedTab.value == EventKind.PAST) {
                val events = pastEventsCache ?: emptyList()
                viewModelScope.launch {
                    val addresses = loadAddresses(events)
                    _eventsUIState.value = EventsUIState.Content(
                        events = events,
                        selectedTab = EventKind.PAST,
                        isLoading = false,
                        addresses = addresses
                    )
                }
            }
        }

        logger.i(
            TAG,
            "Мероприятие ${event.id} добавлено в кэш (${if (isFutureEvent) "FUTURE" else "PAST"})"
        )
    }

    override fun removeDeletedEvent(eventId: Long) {
        logger.d(TAG, "Удаление мероприятия из кэша: $eventId")

        val wasInFutureCache = futureEventsCache?.any { it.id == eventId } == true
        val wasInPastCache = pastEventsCache?.any { it.id == eventId } == true

        if (wasInFutureCache) {
            val currentCache = futureEventsCache ?: emptyList()
            futureEventsCache = currentCache.filter { it.id != eventId }

            if (_selectedTab.value == EventKind.FUTURE) {
                val events = futureEventsCache ?: emptyList()
                viewModelScope.launch {
                    val addresses = loadAddresses(events)
                    _eventsUIState.value = EventsUIState.Content(
                        events = events,
                        selectedTab = EventKind.FUTURE,
                        isLoading = false,
                        addresses = addresses
                    )
                }
            }
        } else if (wasInPastCache) {
            val currentCache = pastEventsCache ?: emptyList()
            pastEventsCache = currentCache.filter { it.id != eventId }

            if (_selectedTab.value == EventKind.PAST) {
                val events = pastEventsCache ?: emptyList()
                viewModelScope.launch {
                    val addresses = loadAddresses(events)
                    _eventsUIState.value = EventsUIState.Content(
                        events = events,
                        selectedTab = EventKind.PAST,
                        isLoading = false,
                        addresses = addresses
                    )
                }
            }
        }

        logger.i(TAG, "Мероприятие $eventId удалено из кэша")
    }

    fun onEventUpdated(event: Event) {
        logger.d(TAG, "Обновление мероприятия в кэше: ${event.id}")

        val futureWithoutEvent = (futureEventsCache ?: emptyList()).filter { it.id != event.id }
        val pastWithoutEvent = (pastEventsCache ?: emptyList()).filter { it.id != event.id }

        if (event.isCurrent) {
            futureEventsCache = (futureWithoutEvent + event).sortedBy { it.beginDate }
            pastEventsCache = pastWithoutEvent
        } else {
            futureEventsCache = futureWithoutEvent
            pastEventsCache = (pastWithoutEvent + event).sortedByDescending { it.beginDate }
        }

        if (_selectedTab.value == EventKind.FUTURE) {
            val events = futureEventsCache ?: emptyList()
            viewModelScope.launch {
                val addresses = loadAddresses(events)
                _eventsUIState.value = EventsUIState.Content(
                    events = events,
                    selectedTab = EventKind.FUTURE,
                    isLoading = false,
                    addresses = addresses
                )
            }
        } else {
            val events = pastEventsCache ?: emptyList()
            viewModelScope.launch {
                val addresses = loadAddresses(events)
                _eventsUIState.value = EventsUIState.Content(
                    events = events,
                    selectedTab = EventKind.PAST,
                    isLoading = false,
                    addresses = addresses
                )
            }
        }

        logger.i(
            TAG,
            "Мероприятие ${event.id} обновлено в кэше (${if (event.isCurrent) "FUTURE" else "PAST"})"
        )
    }
}
