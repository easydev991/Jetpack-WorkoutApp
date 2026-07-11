package com.swparks.domain.usecase

import com.swparks.data.model.City
import com.swparks.domain.repository.CountriesRepository

/**
 * Use case для получения списка городов для конкретной страны.
 *
 * Делегирует вызов репозиторию стран и городов. Используется для загрузки списка городов выбранной
 * страны (например, в профиле пользователя).
 *
 * @param countriesRepository Репозиторий для работы со справочником стран и городов
 */
class GetCitiesByCountryUseCase(
    private val countriesRepository: CountriesRepository
) {
    /**
     * Получить список городов для страны по идентификатору.
     *
     * @param countryId Идентификатор страны
     * @return Список городов страны
     */
    suspend operator fun invoke(countryId: String): List<City> = countriesRepository.getCitiesByCountry(countryId)
}
