package com.swparks.domain.usecase

import com.swparks.data.model.Country
import com.swparks.data.repository.CountriesRepositoryImpl

/**
 * Use case для получения страны по идентификатору.
 *
 * Делегирует вызов репозиторию стран и городов. Используется для загрузки информации о стране
 * (например, в детальной экране площадки).
 *
 * @param countriesRepository Репозиторий для работы со справочником стран и городов
 */
class GetCountryByIdUseCase(
    private val countriesRepository: CountriesRepositoryImpl
) {
    /**
     * Получить страну по идентификатору.
     *
     * @param countryId Идентификатор страны
     * @return Страна или null, если не найдена
     */
    suspend operator fun invoke(countryId: String): Country? = countriesRepository.getCountryById(countryId)
}
