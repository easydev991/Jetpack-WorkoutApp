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
    val cities: List<City> // все города из всех стран (плоский список)
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
        val countryName: String?
    )

    /**
     * Выбирает страну и возвращает результат.
     *
     * При смене страны автоматически выбирается первый город из новой страны,
     * так как сервер не принимает country_id без city_id.
     *
     * @param countryName Имя выбранной страны
     * @param currentCity Текущий выбранный город
     * @return SelectCountryResult с новой страной, новым городом и списком городов
     */
    fun selectCountry(countryName: String, currentCity: City?): SelectCountryResult {
        val newCountry = countries.find { it.name == countryName }
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
     * @param cityName Имя выбранного города
     * @param currentCountry Текущая выбранная страна
     * @return SelectCityResult с новым городом и именем страны (если нужно выбрать другую страну)
     */
    fun selectCity(cityName: String, currentCountry: Country?): SelectCityResult {
        val newCity = cities.find { it.name == cityName }
        var countryName: String? = null

        if (newCity != null) {
            val countryContainingCity = countries.find { it.cities.contains(newCity) }
            if (countryContainingCity != null && currentCountry != countryContainingCity) {
                countryName = countryContainingCity.name
            }
        }

        return SelectCityResult(
            newCity = newCity,
            countryName = countryName
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
