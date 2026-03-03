package com.swparks.ui.viewmodel

import com.swparks.data.model.Event
import com.swparks.ui.model.EventKind
import com.swparks.ui.screens.events.EventsUIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fake-реализация IEventsViewModel для UI тестов.
 *
 * Предоставляет простую реализацию интерфейса с возможностью установки состояния.
 * Используется в Compose UI тестах для проверки разных состояний экрана.
 */
class FakeEventsViewModel(
    override val eventsUIState: StateFlow<EventsUIState>,
    override val isAuthorized: StateFlow<Boolean>,
    override val selectedTab: StateFlow<EventKind> = MutableStateFlow(EventKind.FUTURE),
    override val isRefreshing: StateFlow<Boolean> = MutableStateFlow(false)
) : IEventsViewModel {

    /**
     * Функция-заглушка для переключения вкладки.
     * В тестах состояние устанавливается напрямую через конструктор.
     */
    override fun onTabSelected(tab: EventKind) {
        // Заглушка - не делает ничего в тестах
    }

    /**
     * Функция-заглушка для обновления (pull-to-refresh).
     * В тестах состояние устанавливается напрямую через конструктор.
     */
    override fun refresh() {
        // Заглушка - не делает ничего в тестах
    }

    /**
     * Функция-заглушка для обработки нажатия на мероприятие.
     * В тестах можно проверить, был ли вызван этот метод.
     */
    override fun onEventClick(event: Event) {
        // Заглушка - не делает ничего в тестах
    }

    /**
     * Функция-заглушка для обработки нажатия на FAB.
     * В тестах можно проверить, был ли вызван этот метод.
     */
    override fun onFabClick() {
        // Заглушка - не делает ничего в тестах
    }
}
