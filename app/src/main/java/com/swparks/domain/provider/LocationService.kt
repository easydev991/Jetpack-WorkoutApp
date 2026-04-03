package com.swparks.domain.provider

import android.content.IntentSender
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

    /**
     * Проверяет настройки геолокации устройства.
     *
     * @return Result с [LocationSettingsCheckResult]
     */
    suspend fun checkLocationSettings(): Result<LocationSettingsCheckResult>
}

sealed class LocationSettingsCheckResult {
    data object SettingsOk : LocationSettingsCheckResult()

    data class NeedsResolution(
        val intentSender: IntentSender
    ) : LocationSettingsCheckResult()

    data object SettingsDisabled : LocationSettingsCheckResult()
}
