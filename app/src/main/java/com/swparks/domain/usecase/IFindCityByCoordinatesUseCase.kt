package com.swparks.domain.usecase

/**
 * Интерфейс для use case поиска города по координатам.
 * Создан для удобства тестирования ViewModels.
 */
interface IFindCityByCoordinatesUseCase {
    /**
     * Найти город по названию населённого пункта из геокодирования.
     *
     * @param locality Название населённого пункта (из GeocodingResult.locality)
     * @param latitude Широта
     * @param longitude Долгота
     * @return ID города или null, если город не найден / id не конвертируется в Int
     */
    suspend operator fun invoke(
        locality: String?,
        latitude: Double,
        longitude: Double
    ): Int?
}
