package com.swparks.domain.provider

import com.swparks.domain.model.GeocodingResult

/**
 * Интерфейс сервиса для обратного геокодирования (reverse geocoding).
 *
 * Преобразует координаты в адрес и информацию о местоположении.
 */
interface GeocodingService {

    /**
     * Выполняет обратное геокодирование по координатам.
     *
     * @param latitude Широта
     * @param longitude Долгота
     * @return Result с [GeocodingResult] в случае успеха
     */
    suspend fun reverseGeocode(latitude: Double, longitude: Double): Result<GeocodingResult>
}
