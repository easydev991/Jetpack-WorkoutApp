package com.swparks.domain.model

import com.swparks.data.model.City
import com.swparks.data.model.Country
import org.junit.Test

/**
 * Тесты для EditProfileLocations.
 *
 * Аналог iOS: EditProfileLocationsTests.swift (11 тестов)
 */
class EditProfileLocationsTest {
    // MARK: - isEmpty tests

    @Test
    fun isEmpty_true_whenNoCountries() {
        val locations = makeLocations(countries = emptyList())

        assertTrue(locations.isEmpty)
    }

    @Test
    fun isEmpty_false_whenHasCountries() {
        val locations = makeLocations()

        assertFalse(locations.isEmpty)
    }

    // MARK: - selectCountry tests

    @Test
    fun selectCountry_keepsCurrentCity_whenCityInNewCountry() {
        val locations = makeLocations()
        val currentCity = makeCity(id = "1", name = "Москва", lat = "55.75", lon = "37.61")
        val countryName = "Россия"

        val result = locations.selectCountry(countryName, currentCity)

        assertStringsEqual(result.newCountry?.name, countryName)
        assertCitiesEqual(result.newCity, currentCity)
        assertTrue(result.newCities.isNotEmpty())
    }

    @Test
    fun selectCountry_selectsFirstCity_whenCityNotInNewCountry() {
        val locations = makeLocations()
        val currentCity = makeCity(id = "999", name = "Неизвестный город", lat = "0.0", lon = "0.0")
        val countryName = "Россия"

        val result = locations.selectCountry(countryName, currentCity)

        assertStringsEqual(result.newCountry?.name, countryName)
        // При смене страны выбирается первый город из новой страны
        assertStringsEqual(result.newCity?.name, "Москва")
        assertTrue(result.newCities.isNotEmpty())
    }

    @Test
    fun selectCountry_selectsFirstCity_whenCurrentCityIsNull() {
        val locations = makeLocations()
        val countryName = "Россия"

        val result = locations.selectCountry(countryName, null)

        assertStringsEqual(result.newCountry?.name, countryName)
        // При выборе страны без текущего города выбирается первый город
        assertStringsEqual(result.newCity?.name, "Москва")
        assertTrue(result.newCities.isNotEmpty())
    }

    @Test
    fun selectCountry_returnsNull_whenCountryNotFound() {
        val locations = makeLocations()
        val currentCity = makeCity(id = "1", name = "Москва", lat = "55.75", lon = "37.61")
        val countryName = "Несуществующая страна"

        val result = locations.selectCountry(countryName, currentCity)

        assertNull(result.newCountry)
        assertCitiesEqual(result.newCity, currentCity)
        assertTrue(result.newCities.isNotEmpty())
    }

    @Test
    fun selectCountry_updatesCitiesList() {
        val locations = makeLocations()
        val countryName = "Россия"

        val result = locations.selectCountry(countryName, null)

        assertIntsEqual(result.newCities.size, 2)
    }

    // MARK: - selectCity tests

    @Test
    fun selectCity_sameCountry_whenCityFromCurrentCountry() {
        val locations = makeLocations()
        val currentCountry = locations.countries.find { it.name == "Россия" }
        val cityName = "Москва"

        val result = locations.selectCity(cityName, currentCountry)

        assertStringsEqual(result.newCity?.name, cityName)
        assertNull(result.countryName)
    }

    @Test
    fun selectCity_differentCountry_whenCityFromAnotherCountry() {
        val locations = makeLocations()
        val currentCountry = locations.countries.find { it.name == "Россия" }
        val cityName = "Нью-Йорк"

        val result = locations.selectCity(cityName, currentCountry)

        assertStringsEqual(result.newCity?.name, cityName)
        assertStringsEqual(result.countryName, "США")
    }

    @Test
    fun selectCity_returnsNull_whenCityNotFound() {
        val locations = makeLocations()
        val currentCountry = locations.countries.find { it.name == "Россия" }
        val cityName = "Несуществующий город"

        val result = locations.selectCity(cityName, currentCountry)

        assertNull(result.newCity)
        assertNull(result.countryName)
    }

    @Test
    fun selectCity_returnsCountryName_whenCurrentCountryIsNull() {
        val locations = makeLocations()
        val cityName = "Москва"

        val result = locations.selectCity(cityName, null)

        assertStringsEqual(result.newCity?.name, cityName)
        assertStringsEqual(result.countryName, "Россия")
    }

    // MARK: - Helper methods

    private fun makeLocations(countries: List<Country>? = null): EditProfileLocations {
        val defaultCountries =
            listOf(
                makeCountry(
                    id = "1",
                    name = "Россия",
                    cities =
                        listOf(
                            makeCity(id = "1", name = "Москва", lat = "55.75", lon = "37.61"),
                            makeCity(
                                id = "2",
                                name = "Санкт-Петербург",
                                lat = "59.93",
                                lon = "30.33"
                            )
                        )
                ),
                makeCountry(
                    id = "2",
                    name = "США",
                    cities =
                        listOf(
                            makeCity(id = "3", name = "Нью-Йорк", lat = "40.71", lon = "-74.00"),
                            makeCity(
                                id = "4",
                                name = "Лос-Анджелес",
                                lat = "34.05",
                                lon = "-118.24"
                            )
                        )
                ),
                makeCountry(
                    id = "3",
                    name = "Франция",
                    cities =
                        listOf(
                            makeCity(id = "5", name = "Париж", lat = "48.85", lon = "2.35"),
                            makeCity(id = "6", name = "Лион", lat = "45.75", lon = "4.85")
                        )
                )
            )

        return EditProfileLocations.fromCountries(countries ?: defaultCountries)
    }

    private fun makeCountry(
        id: String,
        name: String,
        cities: List<City> = emptyList()
    ): Country = Country(id = id, name = name, cities = cities)

    private fun makeCity(
        id: String,
        name: String,
        lat: String,
        lon: String
    ): City = City(id = id, name = name, lat = lat, lon = lon)

    // MARK: - Helper Assertions

    private fun assertTrue(actual: Boolean) {
        if (!actual) {
            throw AssertionError("Ожидалось true, получено false")
        }
    }

    private fun assertFalse(actual: Boolean) {
        if (actual) {
            throw AssertionError("Ожидалось false, получено true")
        }
    }

    private fun assertNull(actual: Any?) {
        if (actual != null) {
            throw AssertionError("Ожидалось null, получено: $actual")
        }
    }

    private fun assertStringsEqual(
        actual: String?,
        expected: String?
    ) {
        if (actual != expected) {
            throw AssertionError("Ожидалась строка: $expected\nПолучена строка: $actual")
        }
    }

    private fun assertIntsEqual(
        actual: Int,
        expected: Int
    ) {
        if (actual != expected) {
            throw AssertionError("Ожидалось число: $expected\nПолучено число: $actual")
        }
    }

    private fun assertCitiesEqual(
        actual: City?,
        expected: City?
    ) {
        if (actual != expected) {
            throw AssertionError("Ожидался город: $expected\nПолучен город: $actual")
        }
    }
}
