package com.swparks.ui.viewmodel

import com.swparks.ui.screens.events.EventsUIState
import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс для EventsViewModel.
 *
 * Создан для возможности тестирования UI компонентов Compose без бизнес-логики.
 * Позволяет создавать mock/fake реализации для UI тестов.
 */
interface IEventsViewModel {
    /**
     * Состояние UI экрана.
     */
    val eventsUIState: StateFlow<EventsUIState>

    /**
     * Загружает список прошедших мероприятий.
     */
    fun getPastEvents()
}
