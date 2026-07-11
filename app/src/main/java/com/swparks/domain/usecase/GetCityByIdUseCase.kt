package com.swparks.domain.usecase

import com.swparks.data.model.City
import com.swparks.domain.repository.CountriesRepository

/**
 * Use case для получения города по идентификатору.
 *
 * Делегирует вызов репозиторию стран и городов. Используется для загрузки информации о городе
 * (например, в детальном экране площадки).
 *
 * @param countriesRepository Репозиторий для работы со справочником стран и городов
 */
class GetCityByIdUseCase(
    private val countriesRepository: CountriesRepository
) {
    /**
     * Получить город по идентификатору.
     *
     * @param cityId Идентификатор города
     * @return Город или null, если не найден
     */
    suspend operator fun invoke(cityId: String): City? = countriesRepository.getCityById(cityId)
}
