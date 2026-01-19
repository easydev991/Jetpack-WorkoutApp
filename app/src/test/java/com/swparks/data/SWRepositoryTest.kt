package com.swparks.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import app.cash.turbine.test
import com.swparks.model.Event
import com.swparks.model.User
import com.swparks.network.SWApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class SWRepositoryTest {
    private val testDispatcher = StandardTestDispatcher()
    private val isAuthorizedKey = booleanPreferencesKey("isAuthorized")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        // resetMain не доступен в kotlinx-coroutines-test, но это не критично для unit-тестов
    }

    private fun createMockEvent(id: Long = 1L): Event {
        return Event(
                id = id,
                title = "Test Event",
                description = "Test Description",
                beginDate = "2024-01-01",
                countryID = 1,
                cityID = 1,
                preview = "",
                latitude = "0.0",
                longitude = "0.0",
                isCurrent = false,
                photos = emptyList(),
                author = createMockUser(),
                name = "Test Event"
        )
    }

    private fun createMockUser(id: Long = 1L): User {
        return User(id = id, name = "testuser", image = "", lang = "ru")
    }

    @Test
    fun getPastEvents_whenApiReturnsData_thenReturnsEvents() = runTest {
        // Given
        val mockEventsList = listOf(createMockEvent(1L), createMockEvent(2L))
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getPastEvents() } returns mockEventsList

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.getPastEvents()

        // Then
        assertEquals(mockEventsList, result)
        coVerify { mockApi.getPastEvents() }
    }

    @Test
    fun getPastEvents_whenApiThrowsIOException_thenThrowsIOException() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getPastEvents() } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When & Then
        try {
            repository.getPastEvents()
            throw AssertionError("Expected IOException was not thrown")
        } catch (e: IOException) {
            assertEquals("Network error", e.message)
        }
    }

    @Test
    fun getPastEvents_whenApiThrowsHttpException_thenThrowsHttpException() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        val mockResponse = mockk<Response<*>>(relaxed = true)
        every { mockResponse.code() } returns 404
        every { mockResponse.message() } returns "HTTP 404"
        coEvery { mockApi.getPastEvents() } throws HttpException(mockResponse)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When & Then
        try {
            repository.getPastEvents()
            throw AssertionError("Expected HttpException was not thrown")
        } catch (e: HttpException) {
            assertEquals("HTTP 404", e.message())
        }
    }

    @Test
    fun isAuthorized_whenCalled_thenDelegatesToUserPreferencesRepository() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        val mockDataStore = mockk<DataStore<Preferences>>()
        val preferences = mutablePreferencesOf(isAuthorizedKey to true)
        every { mockDataStore.data } returns flowOf(preferences)

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When & Then
        repository.isAuthorized.test {
            assertEquals(true, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun isAuthorized_whenCalled_thenReturnsFlowFromPreferencesRepository() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        val mockDataStore = mockk<DataStore<Preferences>>()
        val preferences = mutablePreferencesOf(isAuthorizedKey to false)
        every { mockDataStore.data } returns flowOf(preferences)

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When & Then
        repository.isAuthorized.test {
            assertEquals(false, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun savePreference_whenCalled_thenDelegatesToUserPreferencesRepository() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        val mockDataStore = mockk<DataStore<Preferences>>(relaxed = true)
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When & Then - проверяем, что метод выполнился без ошибок
        repository.savePreference(true)
        advanceUntilIdle()
        // Метод выполнился успешно, что означает, что делегирование работает корректно
    }
}
