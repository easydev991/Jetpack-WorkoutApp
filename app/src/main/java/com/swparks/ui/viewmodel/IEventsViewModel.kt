package com.swparks.ui.viewmodel

import com.swparks.data.model.Event
import com.swparks.ui.model.EventKind
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
     * Состояние авторизации пользователя.
     */
    val isAuthorized: StateFlow<Boolean>

    /**
     * Текущая выбранная вкладка (FUTURE или PAST).
     */
    val selectedTab: StateFlow<EventKind>

    /**
     * Состояние pull-to-refresh (true во время обновления).
     */
    val isRefreshing: StateFlow<Boolean>

    /**
     * Переключение вкладки.
     *
     * @param tab Выбранная вкладка (FUTURE или PAST)
     */
    fun onTabSelected(tab: EventKind)

    /**
     * Обновление списка мероприятий (pull-to-refresh).
     */
    fun refresh()

    /**
     * Обработка нажатия на мероприятие.
     *
     * @param event Выбранное мероприятие
     */
    fun onEventClick(event: Event)

    /**
     * Обработка нажатия на FAB (создание мероприятия).
     */
    fun onFabClick()
}
