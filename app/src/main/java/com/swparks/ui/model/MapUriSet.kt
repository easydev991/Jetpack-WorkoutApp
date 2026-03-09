package com.swparks.ui.model

import android.net.Uri
import androidx.core.net.toUri

/**
 * Набор URI для работы с картой.
 *
 * Создаётся на основе координат и предоставляет готовые URI для разных сценариев:
 * - открытие точки на карте в нативном приложении
 * - открытие точки в браузере Google Maps
 * - запуск навигации к точке
 * - построение маршрута в браузере
 *
 * @property latitude Широта
 * @property longitude Долгота
 */
data class MapUriSet(
    val latitude: Double,
    val longitude: Double
) {
    /**
     * geo: URI для открытия в нативном приложении карты.
     * Формат: geo:lat,lng?q=lat,lng
     */
    val geoUri: Uri = "geo:$latitude,$longitude?q=$latitude,$longitude".toUri()

    /**
     * HTTPS URI для открытия в браузере (поиск точки).
     * Формат: https://maps.google.com/?q=lat,lng
     */
    val browserUri: Uri = "https://maps.google.com/?q=$latitude,$longitude".toUri()

    /**
     * google.navigation: URI для запуска навигации.
     * Формат: google.navigation:q=lat,lng
     */
    val navigationUri: Uri = "google.navigation:q=$latitude,$longitude".toUri()

    /**
     * HTTPS URI для построения маршрута в браузере.
     * Формат: https://maps.google.com/?daddr=lat,lng
     */
    val browserRouteUri: Uri = "https://maps.google.com/?daddr=$latitude,$longitude".toUri()
}
