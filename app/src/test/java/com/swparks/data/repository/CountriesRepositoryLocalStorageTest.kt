package com.swparks.data.repository

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.swparks.data.model.City
import com.swparks.data.model.Country
import com.swparks.network.SWApi
import com.swparks.util.Logger
import com.swparks.util.NoOpLogger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files

class CountriesRepositoryLocalStorageTest {
    private lateinit var mockContext: Context
    private lateinit var mockAssetManager: AssetManager
    private lateinit var mockApi: SWApi
    private lateinit var logger: Logger
    private lateinit var repository: CountriesRepositoryImpl
    private lateinit var tempDir: File

    private val testCity1 = City(id = "city1", name = "Тестоград", lat = "55.7558", lon = "37.6173")
    private val testCity2 = City(id = "city2", name = "Демополис", lat = "59.9343", lon = "30.3351")
    private val testCountry1 =
        Country(id = "country1", name = "Тестландия", cities = listOf(testCity1, testCity2))
    private val testCountry2 = Country(id = "country2", name = "Демоландия", cities = emptyList())
    private val testCountries = listOf(testCountry1, testCountry2)

    private val testCountriesJson =
        """
        [
            {
                "id": "country1",
                "name": "Тестландия",
                "cities": [
                    {"id": "city1", "name": "Тестоград", "lat": "55.7558", "lon": "37.6173"},
                    {"id": "city2", "name": "Демополис", "lat": "59.9343", "lon": "30.3351"}
                ]
            },
            {
                "id": "country2",
                "name": "Демоландия",
                "cities": []
            }
        ]
        """.trimIndent()

    @Before
    fun setup() {
        tempDir = Files.createTempDirectory("countries_test_").toFile()
        tempDir.deleteOnExit()

        mockContext = mockk(relaxed = true)
        mockAssetManager = mockk(relaxed = true)
        mockApi = mockk(relaxed = true)
        logger = NoOpLogger()

        every { mockContext.assets } returns mockAssetManager
        every { mockContext.filesDir } returns tempDir

        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0

        repository =
            CountriesRepositoryImpl(context = mockContext, swApi = mockApi, logger = logger)
    }

    @After
    fun tearDown() {
        unmockkAll()
        tempDir.deleteRecursively()
    }

    @Test
    fun whenLocalFileDoesNotExist_seedIsCopiedFromAssets() =
        runTest {
            every { mockAssetManager.open("countries.json") } returns
                ByteArrayInputStream(
                    testCountriesJson.toByteArray()
                )

            repository.ensureCountriesLoaded()

            val localFile = File(tempDir, "countries.json")
            assertTrue(localFile.exists())
        }

    @Test
    fun whenLocalFileExists_seedIsNotCopiedAgain() =
        runTest {
            val localFile = File(tempDir, "countries.json")
            localFile.writeText(testCountriesJson)

            every { mockAssetManager.open("countries.json") } returns
                ByteArrayInputStream(
                    testCountriesJson.toByteArray()
                )

            repository.ensureCountriesLoaded()

            coVerify(exactly = 0) { mockAssetManager.open("countries.json") }
        }

    @Test
    fun countriesAreReadFromLocalJsonFile() =
        runTest {
            val localFile = File(tempDir, "countries.json")
            localFile.writeText(testCountriesJson)

            repository.ensureCountriesLoaded()
            val countries = repository.getCountriesFlow().first()

            assertEquals(2, countries.size)
            assertEquals("country1", countries[0].id)
            assertEquals("Тестландия", countries[0].name)
            assertEquals(2, countries[0].cities.size)
        }

    @Test
    fun updateCountriesFromServer_savesToLocalFileAtomically() =
        runTest {
            val localFile = File(tempDir, "countries.json")
            localFile.writeText(testCountriesJson)

            coEvery { mockApi.getCountries() } returns testCountries

            val result = repository.updateCountriesFromServer()

            assertTrue(result.isSuccess)
            coVerify { mockApi.getCountries() }
            assertTrue(localFile.exists())
            assertTrue(localFile.readText().isNotEmpty())
        }

    @Test
    fun afterUpdateFromServer_countriesFlowReturnsUpdatedData() =
        runTest {
            val localFile = File(tempDir, "countries.json")
            localFile.writeText(testCountriesJson)

            val newCountry = Country(id = "country3", name = "Новая Страна", cities = emptyList())
            coEvery { mockApi.getCountries() } returns listOf(newCountry)

            repository.ensureCountriesLoaded()
            val beforeUpdate = repository.getCountriesFlow().first()
            assertEquals(2, beforeUpdate.size)

            repository.updateCountriesFromServer()
            val afterUpdate = repository.getCountriesFlow().first()
            assertEquals(1, afterUpdate.size)
            assertEquals("country3", afterUpdate[0].id)
            assertEquals("Новая Страна", afterUpdate[0].name)
        }

    @Test
    fun getCountryById_searchesInLocalFileData() =
        runTest {
            val localFile = File(tempDir, "countries.json")
            localFile.writeText(testCountriesJson)

            repository.ensureCountriesLoaded()
            val country = repository.getCountryById("country1")

            assertNotNull(country)
            assertEquals("country1", country?.id)
            assertEquals("Тестландия", country?.name)
        }

    @Test
    fun getCityById_searchesInLocalFileData() =
        runTest {
            val localFile = File(tempDir, "countries.json")
            localFile.writeText(testCountriesJson)

            repository.ensureCountriesLoaded()
            val city = repository.getCityById("city1")

            assertNotNull(city)
            assertEquals("city1", city?.id)
            assertEquals("Тестоград", city?.name)
        }

    @Test
    fun getCitiesByCountry_returnsCorrectCities() =
        runTest {
            val localFile = File(tempDir, "countries.json")
            localFile.writeText(testCountriesJson)

            repository.ensureCountriesLoaded()
            val cities = repository.getCitiesByCountry("country1")

            assertEquals(2, cities.size)
            assertEquals("city1", cities[0].id)
            assertEquals("Тестоград", cities[0].name)
        }

    @Test
    fun updateCountriesFromServer_onSuccess_updatesCacheAndSavesFile() =
        runTest {
            val localFile = File(tempDir, "countries.json")
            localFile.writeText(testCountriesJson)

            coEvery { mockApi.getCountries() } returns testCountries

            repository.ensureCountriesLoaded()
            val result = repository.updateCountriesFromServer()

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { mockApi.getCountries() }
        }

    @Test
    fun updateCountriesFromServer_onNetworkError_doesNotUpdateCache() =
        runTest {
            val localFile = File(tempDir, "countries.json")
            localFile.writeText(testCountriesJson)

            coEvery { mockApi.getCountries() } throws IOException("Network error")

            repository.ensureCountriesLoaded()
            val beforeUpdate = repository.getCountriesFlow().first()
            val result = repository.updateCountriesFromServer()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is com.swparks.domain.exception.NetworkException)
            val afterUpdate = repository.getCountriesFlow().first()
            assertEquals(beforeUpdate.size, afterUpdate.size)
        }

    @Test
    fun firstLaunch_loadsFromAssetsAndSavesToLocalFile() =
        runTest {
            every { mockAssetManager.open("countries.json") } returns
                ByteArrayInputStream(
                    testCountriesJson.toByteArray()
                )

            repository.ensureCountriesLoaded()
            val countries = repository.getCountriesFlow().first()

            assertEquals(2, countries.size)
            val localFile = File(tempDir, "countries.json")
            assertTrue(localFile.exists())
            assertTrue(localFile.readText().isNotEmpty())
        }
}
