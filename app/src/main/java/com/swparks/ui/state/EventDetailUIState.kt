package com.swparks.ui.state

import com.swparks.data.model.Event

/**
 * Состояния UI экрана детальной информации о мероприятии.
 *
 * Используется в [com.swparks.ui.viewmodel.EventDetailViewModel] для управления
 * отображением экрана EventDetailScreen.
 */
sealed interface EventDetailUIState {
    /**
     * Первичная загрузка данных мероприятия.
     * Отображается загрузочный экран (LoadingOverlayView).
     */
    data object InitialLoading : EventDetailUIState

    /**
     * Данные мероприятия успешно загружены.
     *
     * @param event Загруженное мероприятие
     * @param address Адрес мероприятия (страна, город)
     * @param authorAddress Адрес автора мероприятия (страна, город)
     */
    data class Content(
        val event: Event,
        val address: String,
        val authorAddress: String
    ) : EventDetailUIState

    /**
     * Ошибка загрузки данных мероприятия.
     *
     * @param message Сообщение об ошибке (может быть null)
     */
    data class Error(
        val message: String?
    ) : EventDetailUIState
}
