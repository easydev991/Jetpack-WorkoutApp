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
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files

/** Unit тесты для CountriesRepositoryImpl */
class CountriesRepositoryTest {

    private lateinit var mockContext: Context
    private lateinit var mockAssetManager: AssetManager
    private lateinit var mockApi: SWApi
    private lateinit var logger: Logger
    private lateinit var repository: CountriesRepositoryImpl
    private lateinit var tempDir: File

    // Тестовые данные
    private val testCity1 = City(id = "city1", name = "Тестград", lat = "55.7558", lon = "37.6173")
    private val testCity2 =
        City(id = "city2", name = "Демополис", lat = "59.9343", lon = "30.3351")
    private val testCity3 = City(id = "city3", name = "Примерск", lat = "50.4501", lon = "30.5234")

    private val testCountry1 =
        Country(id = "country1", name = "Тестландия", cities = listOf(testCity1, testCity2))

    private val testCountry2 =
        Country(id = "country2", name = "Демоландия", cities = listOf(testCity3))

    private val testCountries = listOf(testCountry1, testCountry2)

    private val testCountriesJson =
        """
        [
            {
                "id": "country1",
                "name": "Тестландия",
                "cities": [
                    {
                        "id": "city1",
                        "name": "Тестград",
                        "lat": "55.7558",
                        "lon": "37.6173"
                    },
                    {
                        "id": "city2",
                        "name": "Демополис",
                        "lat": "59.9343",
                        "lon": "30.3351"
                    }
                ]
            },
            {
                "id": "country2",
                "name": "Демоландия",
                "cities": [
                    {
                        "id": "city3",
                        "name": "Примерск",
                        "lat": "50.4501",
                        "lon": "30.5234"
                    }
                ]
            }
        ]
    """.trimIndent()

    @Before
    fun setup() {
        tempDir = Files.createTempDirectory("countries_test_").toFile()
        tempDir.deleteOnExit()

        mockContext = mockk(relaxed = true)
        mockAssetManager = mockk()
        mockApi = mockk(relaxed = true)
        logger = NoOpLogger()

        every { mockContext.assets } returns mockAssetManager
        every { mockContext.filesDir } returns tempDir

        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        repository =
            CountriesRepositoryImpl(context = mockContext, swApi = mockApi, logger = logger)
    }

    @After
    fun tearDown() {
        unmockkAll()
        tempDir.deleteRecursively()
    }

    @Test
    fun testGetCountriesFlow_returnsCachedCountries() = runTest {
        // Given
        val inputStream = ByteArrayInputStream(testCountriesJson.toByteArray())
        every { mockAssetManager.open("countries.json") } returns inputStream

        // When - загружаем данные (getCountriesFlow не вызывает загрузку автоматически)
        repository.getCountryById("country1")

        // Then - проверяем, что flow возвращает загруженные данные
        val countries = repository.getCountriesFlow().first()

        assertNotNull(countries)
        assertEquals(2, countries.size)
        assertEquals("country1", countries[0].id)
        assertEquals("Тестландия", countries[0].name)
        assertEquals("country2", countries[1].id)
        assertEquals("Демоландия", countries[1].name)

        verify { mockAssetManager.open("countries.json") }
    }

    @Test
    fun testGetCountryById_existingId_returnsCountry() = runTest {
        // Given
        val inputStream = ByteArrayInputStream(testCountriesJson.toByteArray())
        every { mockAssetManager.open("countries.json") } returns inputStream

        // When
        val country = repository.getCountryById("country1")

        // Then
        assertNotNull(country)
        assertEquals("country1", country?.id)
        assertEquals("Тестландия", country?.name)
        assertEquals(2, country?.cities?.size)
        verify { mockAssetManager.open("countries.json") }
    }

    @Test
    fun testGetCountryById_nonExistingId_returnsNull() = runTest {
        // Given
        val inputStream = ByteArrayInputStream(testCountriesJson.toByteArray())
        every { mockAssetManager.open("countries.json") } returns inputStream

        // When
        val country = repository.getCountryById("nonexistent")

        // Then
        assertEquals(null, country)
        verify { mockAssetManager.open("countries.json") }
    }

    @Test
    fun testGetCityById_existingId_returnsCity() = runTest {
        // Given
        val inputStream = ByteArrayInputStream(testCountriesJson.toByteArray())
        every { mockAssetManager.open("countries.json") } returns inputStream

        // When
        val city = repository.getCityById("city1")

        // Then
        assertNotNull(city)
        assertEquals("city1", city?.id)
        assertEquals("Тестград", city?.name)
        verify { mockAssetManager.open("countries.json") }
    }

    @Test
    fun testGetCityById_nonExistingId_returnsNull() = runTest {
        // Given
        val inputStream = ByteArrayInputStream(testCountriesJson.toByteArray())
        every { mockAssetManager.open("countries.json") } returns inputStream

        // When
        val city = repository.getCityById("nonexistent")

        // Then
        assertEquals(null, city)
        verify { mockAssetManager.open("countries.json") }
    }

    @Test
    fun testGetCitiesByCountry_returnsCorrectCities() = runTest {
        // Given
        val inputStream = ByteArrayInputStream(testCountriesJson.toByteArray())
        every { mockAssetManager.open("countries.json") } returns inputStream

        // When
        val cities = repository.getCitiesByCountry("country1")

        // Then
        assertNotNull(cities)
        assertEquals(2, cities.size)
        assertEquals("city1", cities[0].id)
        assertEquals("Тестград", cities[0].name)
        assertEquals("city2", cities[1].id)
        assertEquals("Демополис", cities[1].name)
        verify { mockAssetManager.open("countries.json") }
    }

    @Test
    fun testGetCitiesByCountry_nonExistingCountry_returnsEmptyList() = runTest {
        // Given
        val inputStream = ByteArrayInputStream(testCountriesJson.toByteArray())
        every { mockAssetManager.open("countries.json") } returns inputStream

        // When
        val cities = repository.getCitiesByCountry("nonexistent")

        // Then
        assertEquals(emptyList<City>(), cities)
        verify { mockAssetManager.open("countries.json") }
    }

    @Test
    fun testUpdateCountriesFromServer_success() = runTest {
        // Given
        val inputStream = ByteArrayInputStream(testCountriesJson.toByteArray())
        every { mockAssetManager.open("countries.json") } returns inputStream
        coEvery { mockApi.getCountries() } returns testCountries

        // When
        val result = repository.updateCountriesFromServer()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())

        // Проверяем, что API был вызван
        coVerify(exactly = 1) { mockApi.getCountries() }
    }

    @Test
    fun testUpdateCountriesFromServer_networkError() = runTest {
        // Given
        val inputStream = ByteArrayInputStream(testCountriesJson.toByteArray())
        every { mockAssetManager.open("countries.json") } returns inputStream
        coEvery { mockApi.getCountries() } throws IOException("Network error")

        // When
        val result = repository.updateCountriesFromServer()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is com.swparks.domain.exception.NetworkException)
        assertEquals(
            "Не удалось обновить справочник. Проверьте интернет-соединение",
            result.exceptionOrNull()?.message
        )

        coVerify(exactly = 1) { mockApi.getCountries() }
    }

    @Test
    fun testUpdateCountriesFromServer_httpError() = runTest {
        // Given
        val inputStream = ByteArrayInputStream(testCountriesJson.toByteArray())
        every { mockAssetManager.open("countries.json") } returns inputStream

        val mockResponse = mockk<Response<*>>(relaxed = true)
        every { mockResponse.code() } returns 500
        every { mockResponse.message() } returns "Internal Server Error"
        coEvery { mockApi.getCountries() } throws HttpException(mockResponse)

        // When
        val result = repository.updateCountriesFromServer()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is com.swparks.domain.exception.ServerException)
        assertTrue(result.exceptionOrNull()?.message?.contains("500") == true)

        coVerify(exactly = 1) { mockApi.getCountries() }
    }

    @Test
    fun testCountriesFlow_afterUpdateFromServer_returnsUpdatedData() = runTest {
        // Given
        val inputStream = ByteArrayInputStream(testCountriesJson.toByteArray())
        every { mockAssetManager.open("countries.json") } returns inputStream

        val updatedCountry = Country(id = "country3", name = "Примерия", cities = emptyList())
        val updatedCountries = listOf(updatedCountry)

        coEvery { mockApi.getCountries() } returns updatedCountries

        // When - сначала загружаем из assets
        repository.getCountryById("country1")

        // Then - проверяем начальные данные
        val initialCountries = repository.getCountriesFlow().first()
        assertNotNull(initialCountries)
        assertEquals(2, initialCountries.size)

        // When - обновляем с сервера
        repository.updateCountriesFromServer()

        // Then - проверяем, что данные обновились
        val updatedCountriesResult = repository.getCountriesFlow().first()
        assertEquals(1, updatedCountriesResult.size)
        assertEquals("Примерия", updatedCountriesResult[0].name)
    }

    @Test
    fun testCountriesFlow_loadingFromAssetsError_doesNotCrash() = runTest {
        // Given - имитируем ошибку чтения из assets
        every { mockAssetManager.open("countries.json") } throws IOException("File not found")

        // When & Then - репозиторий должен работать без краша
        val countries = repository.getCountriesFlow().first()
        assertNotNull(countries)
        assertEquals(emptyList<Country>(), countries)

        // Проверяем, что методы поиска возвращают null при ошибке загрузки
        val country = repository.getCountryById("country1")
        assertEquals(null, country)

        val city = repository.getCityById("city1")
        assertEquals(null, city)

        val cities = repository.getCitiesByCountry("country1")
        assertNotNull(cities)
        assertEquals(emptyList<City>(), cities)
    }

    @Test
    fun testGetCountriesFlow_multipleCalls_loadsAssetsOnlyOnce() = runTest {
        // Given
        val inputStream = ByteArrayInputStream(testCountriesJson.toByteArray())
        every { mockAssetManager.open("countries.json") } returns inputStream

        // When - делаем несколько вызовов методов репозитория
        repository.getCountryById("country1")
        repository.getCountryById("country2")
        repository.getCitiesByCountry("country1")
        repository.getCitiesByCountry("country2")

        // Then - файл должен быть прочитан только один раз
        verify(exactly = 1) { mockAssetManager.open("countries.json") }

        // Проверяем, что все методы возвращают корректные данные
        val country1 = repository.getCountryById("country1")
        assertNotNull(country1)
        assertEquals("Тестландия", country1?.name)

        val country2 = repository.getCountryById("country2")
        assertNotNull(country2)
        assertEquals("Демоландия", country2?.name)

        val cities1 = repository.getCitiesByCountry("country1")
        assertEquals(2, cities1.size)

        val cities2 = repository.getCitiesByCountry("country2")
        assertEquals(1, cities2.size)
    }
}
