package com.swparks.domain.repository

import com.swparks.model.City
import com.swparks.model.Country
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий для работы со справочником стран и городов
 *
 * Предоставляет доступ к справочнику стран и городов, загруженному из локального JSON-файла или
 * обновленному с сервера.
 *
 * Все идентификаторы имеют тип String, так как в JSON-файле они передаются как строки.
 */
interface CountriesRepository {
    /**
     * Получить список всех стран в виде Flow
     *
     * @return Flow со списком всех стран
     */
    fun getCountriesFlow(): Flow<List<Country>>

    /**
     * Получить страну по идентификатору
     *
     * @param countryId идентификатор страны
     * @return страна или null, если не найдена
     */
    suspend fun getCountryById(countryId: String): Country?

    /**
     * Получить город по идентификатору
     *
     * @param cityId идентификатор города
     * @return город или null, если не найден
     */
    suspend fun getCityById(cityId: String): City?

    /**
     * Получить список городов для конкретной страны
     *
     * @param countryId идентификатор страны
     * @return список городов страны
     */
    suspend fun getCitiesByCountry(countryId: String): List<City>

    /**
     * Обновить справочник стран и городов с сервера
     *
     * Метод для будущего использования. Позволяет обновить справочник данных с сервера workout.su.
     *
     * @return Result<Unit> с результатом операции
     */
    suspend fun updateCountriesFromServer(): Result<Unit>
}
