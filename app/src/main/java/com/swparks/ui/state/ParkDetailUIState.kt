package com.swparks.ui.state

import com.swparks.data.model.Park

/**
 * Состояния UI экрана детальной информации о площадке.
 *
 * Используется в [com.swparks.ui.viewmodel.ParkDetailViewModel] для управления
 * отображением экрана ParkDetailScreen.
 */
sealed interface ParkDetailUIState {
    /**
     * Первичная загрузка данных площадки.
     * Отображается загрузочный экран (LoadingOverlayView).
     */
    data object InitialLoading : ParkDetailUIState

    /**
     * Данные площадки успешно загружены.
     *
     * @param park Загруженная площадка
     * @param address Адрес площадки (страна, город)
     * @param authorAddress Адрес автора площадки (страна, город)
     */
    data class Content(
        val park: Park,
        val address: String,
        val authorAddress: String
    ) : ParkDetailUIState

    /**
     * Ошибка загрузки данных площадки.
     *
     * @param message Сообщение об ошибке (может быть null)
     */
    data class Error(
        val message: String?
    ) : ParkDetailUIState
}
