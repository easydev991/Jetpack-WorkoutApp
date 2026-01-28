package com.swparks.domain.usecase

import com.swparks.domain.repository.CountriesRepository
import com.swparks.model.City

/**
 * Интерфейс для use case получения города по идентификатору. Создан для удобства тестирования
 * ViewModels.
 */
interface IGetCityByIdUseCase {
    suspend operator fun invoke(cityId: String): City?
}

/**
 * Use case для получения города по идентификатору.
 *
 * Делегирует вызов репозиторию стран и городов. Используется для загрузки информации о городе
 * (например, в детальном экране площадки).
 *
 * @param countriesRepository Репозиторий для работы со справочником стран и городов
 */
class GetCityByIdUseCase(private val countriesRepository: CountriesRepository) :
    IGetCityByIdUseCase {
    /**
     * Получить город по идентификатору.
     *
     * @param cityId Идентификатор города
     * @return Город или null, если не найден
     */
    override suspend operator fun invoke(cityId: String): City? {
        return countriesRepository.getCityById(cityId)
    }
}
