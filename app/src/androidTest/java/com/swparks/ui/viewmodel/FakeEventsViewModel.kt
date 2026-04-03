package com.swparks.ui.viewmodel

import com.swparks.data.model.Event
import com.swparks.data.model.User
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
    currentUser: MutableStateFlow<User?> =
        MutableStateFlow(
            if (isAuthorized.value) {
                User(id = 1L, name = "testuser", image = null)
            } else {
                null
            }
        ),
    selectedTab: MutableStateFlow<EventKind> = MutableStateFlow(EventKind.FUTURE),
    isRefreshing: MutableStateFlow<Boolean> = MutableStateFlow(false),
    fabClickResult: EventsEvent = EventsEvent.NavigateToCreateEvent
) : IEventsViewModel {
    private val eventsUiStateFlow = eventsUIState
    private val isAuthorizedFlow = isAuthorized
    private val currentUserFlow = currentUser
    private val selectedTabFlow = selectedTab
    private val isRefreshingFlow = isRefreshing
    private val fabClickResultEvent = fabClickResult

    override val eventsUIState: StateFlow<EventsUIState> = eventsUiStateFlow
    override val isAuthorized: StateFlow<Boolean> = isAuthorizedFlow
    override val currentUser: StateFlow<User?> = currentUserFlow
    override val selectedTab: StateFlow<EventKind> = selectedTabFlow
    override val isRefreshing: StateFlow<Boolean> = isRefreshingFlow

    private val eventsFlow = MutableSharedFlow<EventsEvent>(extraBufferCapacity = 1)
    override val events: Flow<EventsEvent> = eventsFlow.asSharedFlow()

    override fun onTabSelected(tab: EventKind) {
        selectedTabFlow.value = tab
    }

    override fun refresh() {}

    override fun onEventClick(event: Event) {}

    override fun onFabClick() {
        eventsFlow.tryEmit(fabClickResultEvent)
    }

    override fun addCreatedEvent(event: Event) {}

    override fun removeDeletedEvent(eventId: Long) {}

    fun setAddresses(addresses: Map<Pair<Int, Int>, String>) {
        val currentState = eventsUiStateFlow.value as? EventsUIState.Content ?: return
        eventsUiStateFlow.value = currentState.copy(addresses = addresses)
    }
}
