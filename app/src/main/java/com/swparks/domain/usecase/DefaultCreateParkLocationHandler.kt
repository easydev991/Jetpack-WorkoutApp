package com.swparks.domain.usecase

import com.swparks.data.model.NewParkDraft
import com.swparks.domain.provider.LocationService
import com.swparks.util.AppError
import com.swparks.util.UserNotifier

/**
 * Реализация обработчика create-flow после получения разрешения на геолокацию.
 */
class DefaultCreateParkLocationHandler(
    private val locationService: LocationService,
    private val userNotifier: UserNotifier
) : ICreateParkLocationHandler {

    override suspend fun invoke(): Result<NewParkDraft> {
        return locationService.getCurrentLocation().fold(
            onSuccess = { coordinates ->
                Result.success(NewParkDraft.EMPTY.withCoordinates(coordinates.latitude, coordinates.longitude))
            },
            onFailure = { error ->
                userNotifier.handleError(
                    AppError.Generic("Не удалось определить местоположение. Проверьте разрешения и попробуйте снова.")
                )
                Result.failure(error)
            }
        )
    }
}
