package com.swparks.domain.model

import com.swparks.data.model.City
import com.swparks.data.model.Country

/**
 * Страны и города для редактирования профиля.
 *
 * Note: На сервере нельзя указать только страну без города:
 * поле country_id будет проигнорировано при сохранении данных на сервере,
 * если не указать city_id
 */
data class EditProfileLocations(
    val countries: List<Country>,
    // все города из всех стран (плоский список)
    val cities: List<City>
) {
    val isEmpty: Boolean
        get() = countries.isEmpty() && cities.isEmpty()

    /**
     * Результат выбора страны.
     */
    data class SelectCountryResult(
        val newCountry: Country?,
        val newCity: City?,
        val newCities: List<City>
    )

    /**
     * Результат выбора города.
     */
    data class SelectCityResult(
        val newCity: City?,
        /**
         * Идентификатор страны, которой принадлежит выбранный город,
         * если она отличается от currentCountry. null если страна не меняется.
         */
        val countryId: String?
    )

    /**
     * Выбирает страну и возвращает результат.
     *
     * При смене страны автоматически выбирается первый город из новой страны,
     * так как сервер не принимает country_id без city_id.
     *
     * @param countryId идентификатор выбранной страны
     * @param currentCity Текущий выбранный город
     * @return SelectCountryResult с новой страной, новым городом и списком городов
     */
    fun selectCountry(
        countryId: String,
        currentCity: City?
    ): SelectCountryResult {
        val newCountry = countries.find { it.id == countryId }
        var newCity: City? = currentCity
        var newCities: List<City> = cities

        if (newCountry != null && !newCountry.cities.contains(currentCity)) {
            // При смене страны выбираем первый город из новой страны
            newCity = newCountry.cities.firstOrNull()
            newCities = newCountry.cities
        }

        return SelectCountryResult(
            newCountry = newCountry,
            newCity = newCity,
            newCities = newCities
        )
    }

    /**
     * Выбирает город и возвращает результат.
     *
     * @param cityId идентификатор выбранного города
     * @param currentCountry Текущая выбранная страна
     * @return SelectCityResult с новым городом и идентификатором страны,
     *         если выбранный город принадлежит другой стране
     */
    fun selectCity(
        cityId: String,
        currentCountry: Country?
    ): SelectCityResult {
        val newCity = cities.find { it.id == cityId }
        var countryId: String? = null

        if (newCity != null) {
            val countryContainingCity = countries.find { it.cities.contains(newCity) }
            if (countryContainingCity != null && currentCountry != countryContainingCity) {
                countryId = countryContainingCity.id
            }
        }

        return SelectCityResult(
            newCity = newCity,
            countryId = countryId
        )
    }

    companion object {
        /**
         * Создает EditProfileLocations из списка стран.
         */
        fun fromCountries(countries: List<Country>): EditProfileLocations =
            EditProfileLocations(
                countries = countries,
                cities = countries.flatMap { it.cities }
            )
    }
}
