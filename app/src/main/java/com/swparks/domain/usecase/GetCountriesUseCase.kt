package com.swparks.domain.usecase

import com.swparks.data.model.Country
import com.swparks.domain.repository.CountriesRepository
import kotlinx.coroutines.flow.Flow

/** Интерфейс для use case получения списка стран. Создан для удобства тестирования ViewModels. */
interface IGetCountriesUseCase {
    operator fun invoke(): Flow<List<Country>>
}

/**
 * Use case для получения списка всех стран.
 *
 * Делегирует вызов репозиторию стран и городов. Используется для загрузки списка стран в UI
 * (например, в профиле пользователя).
 *
 * @param countriesRepository Репозиторий для работы со справочником стран и городов
 */
class GetCountriesUseCase(private val countriesRepository: CountriesRepository) :
    IGetCountriesUseCase {
    /**
     * Получить список всех стран в виде Flow.
     *
     * @return Flow со списком всех стран
     */
    override operator fun invoke(): Flow<List<Country>> {
        return countriesRepository.getCountriesFlow()
    }
}
