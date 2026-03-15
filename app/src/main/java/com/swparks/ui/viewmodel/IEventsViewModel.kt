package com.swparks.ui.viewmodel

import com.swparks.data.model.Event
import com.swparks.ui.model.EventKind
import com.swparks.ui.state.EventsUIState
import kotlinx.coroutines.flow.Flow
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
     * Одноразовые события UI (навигация и т.д.).
     */
    val events: Flow<EventsEvent>

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

    /**
     * Добавление созданного мероприятия в список.
     *
     * Вызывается при возврате с EventFormScreen после успешного создания мероприятия.
     * Добавляет мероприятие в кэш и обновляет UI без дополнительного запроса к серверу.
     *
     * @param event Созданное мероприятие
     */
    fun addCreatedEvent(event: Event)

    /**
     * Удаление мероприятия из списка.
     *
     * Вызывается при возврате с EventDetailScreen после успешного удаления мероприятия.
     * Удаляет мероприятие из кэша и обновляет UI без дополнительного запроса к серверу.
     *
     * @param eventId Идентификатор удаленного мероприятия
     */
    fun removeDeletedEvent(eventId: Long)
}
