package com.swparks.domain.usecase

import com.swparks.data.repository.CountriesRepositoryImpl

/**
 * Use case для поиска города по названию населённого пункта из геокодирования.
 *
 * Применяется после получения геокодирования в create-flow формы площадки,
 * когда пользователь нажал "Определить местоположение". Позволяет автоматически
 * заполнить поле города в форме.
 *
 * @param countriesRepository Репозиторий для работы со справочником стран и городов
 */
class FindCityByCoordinatesUseCase(
    private val countriesRepository: CountriesRepositoryImpl
) {
    suspend operator fun invoke(
        locality: String?,
        latitude: Double,
        longitude: Double
    ): Int? {
        if (locality == null) return null

        val normalizedLocality = normalizeLocality(locality)
        val allCities = countriesRepository.getAllCities()

        val matchingCity =
            allCities.find { city ->
                normalizeLocality(city.name) == normalizedLocality
            }

        return matchingCity?.id?.toIntOrNull()
    }

    private fun normalizeLocality(locality: String): String = locality.trim().lowercase()
}
