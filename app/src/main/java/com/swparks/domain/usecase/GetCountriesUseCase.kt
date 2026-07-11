package com.swparks.domain.usecase

import com.swparks.data.model.Country
import com.swparks.data.repository.CountriesRepositoryImpl
import kotlinx.coroutines.flow.Flow

/**
 * Use case для получения списка всех стран.
 *
 * Делегирует вызов репозиторию стран и городов. Используется для загрузки списка стран в UI
 * (например, в профиле пользователя).
 *
 * @param countriesRepository Репозиторий для работы со справочником стран и городов
 */
class GetCountriesUseCase(
    private val countriesRepository: CountriesRepositoryImpl
) {
    /**
     * Получить список всех стран в виде Flow.
     *
     * @return Flow со списком всех стран
     */
    operator fun invoke(): Flow<List<Country>> = countriesRepository.getCountriesFlow()
}
