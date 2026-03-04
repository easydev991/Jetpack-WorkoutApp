package com.swparks.ui.state

import com.swparks.data.model.Event
import com.swparks.ui.model.EventKind

sealed interface EventsUIState {
    data class Content(
        val events: List<Event>,
        val selectedTab: EventKind,
        val isLoading: Boolean = false,
        val addresses: Map<Pair<Int, Int>, String> = emptyMap()
    ) : EventsUIState

    data class Error(
        val message: String?,
        val addresses: Map<Pair<Int, Int>, String> = emptyMap()
    ) : EventsUIState

    data object InitialLoading : EventsUIState
}
