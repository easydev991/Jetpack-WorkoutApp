package com.swparks.ui.viewmodel

import com.swparks.data.model.Event
import com.swparks.ui.model.EventKind
import com.swparks.ui.state.EventsUIState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

class FakeEventsViewModel(
    eventsUIState: MutableStateFlow<EventsUIState>,
    isAuthorized: MutableStateFlow<Boolean>,
    selectedTab: MutableStateFlow<EventKind> = MutableStateFlow(EventKind.FUTURE),
    isRefreshing: MutableStateFlow<Boolean> = MutableStateFlow(false)
) : IEventsViewModel {

    private val _eventsUIState = eventsUIState
    private val _isAuthorized = isAuthorized
    private val _selectedTab = selectedTab
    private val _isRefreshing = isRefreshing

    override val eventsUIState: StateFlow<EventsUIState> = _eventsUIState
    override val isAuthorized: StateFlow<Boolean> = _isAuthorized
    override val selectedTab: StateFlow<EventKind> = _selectedTab
    override val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _eventsFlow = MutableSharedFlow<EventsEvent>()
    override val events: Flow<EventsEvent> = _eventsFlow.asSharedFlow()

    override fun onTabSelected(tab: EventKind) {
        _selectedTab.value = tab
    }

    override fun refresh() {}

    override fun onEventClick(event: Event) {}

    override fun onFabClick() {}

    override fun addCreatedEvent(event: Event) {}

    override fun removeDeletedEvent(eventId: Long) {}

    fun setAddresses(addresses: Map<Pair<Int, Int>, String>) {
        val currentState = _eventsUIState.value as? EventsUIState.Content ?: return
        _eventsUIState.value = currentState.copy(addresses = addresses)
    }
}
