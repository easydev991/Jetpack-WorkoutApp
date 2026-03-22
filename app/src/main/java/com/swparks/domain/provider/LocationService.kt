package com.swparks.domain.provider

import com.swparks.domain.model.LocationCoordinates

/**
 * Интерфейс сервиса для получения текущих координат устройства.
 */
interface LocationService {

    /**
     * Возвращает текущие координаты устройства.
     *
     * @return Result с [LocationCoordinates] в случае успеха
     */
    suspend fun getCurrentLocation(): Result<LocationCoordinates>
}
