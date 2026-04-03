package com.swparks.domain.usecase

import com.swparks.data.model.City
import com.swparks.domain.repository.CountriesRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FindCityByCoordinatesUseCaseTest {
    private lateinit var countriesRepository: CountriesRepository
    private lateinit var useCase: FindCityByCoordinatesUseCase

    @Before
    fun setup() {
        countriesRepository = mockk(relaxed = true)
        useCase = FindCityByCoordinatesUseCase(countriesRepository)
    }

    @Test
    fun invoke_whenLocalityMatches_returnsCityId() =
        runTest {
            coEvery { countriesRepository.getAllCities() } returns
                listOf(
                    City(id = "1", name = "Moscow", lat = "55.7558", lon = "37.6173"),
                    City(id = "2", name = "London", lat = "51.5074", lon = "-0.1278")
                )

            val result =
                useCase.invoke(locality = "Moscow", latitude = 55.7558, longitude = 37.6173)

            assertEquals(1, result)
        }

    @Test
    fun invoke_whenLocalityMatches_caseInsensitive_returnsCityId() =
        runTest {
            coEvery { countriesRepository.getAllCities() } returns
                listOf(
                    City(id = "3", name = "Berlin", lat = "52.5200", lon = "13.4050")
                )

            val result =
                useCase.invoke(locality = "BERLIN", latitude = 52.5200, longitude = 13.4050)

            assertEquals(3, result)
        }

    @Test
    fun invoke_whenLocalityNull_returnsNull() =
        runTest {
            val result = useCase.invoke(locality = null, latitude = 55.7558, longitude = 37.6173)

            assertNull(result)
        }

    @Test
    fun invoke_whenCityIdIsNotInt_returnsNull() =
        runTest {
            coEvery { countriesRepository.getAllCities() } returns
                listOf(
                    City(id = "not_an_int", name = "Paris", lat = "48.8566", lon = "2.3522")
                )

            val result = useCase.invoke(locality = "Paris", latitude = 48.8566, longitude = 2.3522)

            assertNull(result)
        }

    @Test
    fun invoke_whenNoMatch_returnsNull() =
        runTest {
            coEvery { countriesRepository.getAllCities() } returns
                listOf(
                    City(id = "1", name = "Moscow", lat = "55.7558", lon = "37.6173")
                )

            val result = useCase.invoke(locality = "UnknownCity", latitude = 0.0, longitude = 0.0)

            assertNull(result)
        }

    @Test
    fun invoke_whenMultipleMatches_returnsFirstMatch() =
        runTest {
            coEvery { countriesRepository.getAllCities() } returns
                listOf(
                    City(id = "1", name = "Moscow", lat = "55.7558", lon = "37.6173"),
                    City(id = "5", name = "Moscow", lat = "55.7558", lon = "37.6173")
                )

            val result =
                useCase.invoke(locality = "Moscow", latitude = 55.7558, longitude = 37.6173)

            assertEquals(1, result)
        }

    @Test
    fun invoke_whenLocalityHasWhitespace_returnsTrimmedMatch() =
        runTest {
            coEvery { countriesRepository.getAllCities() } returns
                listOf(
                    City(id = "7", name = "Madrid", lat = "40.4168", lon = "-3.7038")
                )

            val result =
                useCase.invoke(locality = "  Madrid  ", latitude = 40.4168, longitude = -3.7038)

            assertEquals(7, result)
        }
}
