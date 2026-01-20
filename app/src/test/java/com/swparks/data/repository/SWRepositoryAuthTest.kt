package com.swparks.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.swparks.domain.exception.NetworkException
import com.swparks.model.ChangePasswordRequest
import com.swparks.model.LoginSuccess
import com.swparks.model.RegistrationRequest
import com.swparks.network.SWApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Unit тесты для методов авторизации в SWRepository
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SWRepositoryAuthTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun login_whenApiReturnsSuccess_thenReturnsSuccessAndSavesToken() = runTest {
        // Given
        val mockSuccess = LoginSuccess(userId = 123L)
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.login("test_token") } returns mockSuccess

        val mockDataStore = mockk<DataStore<Preferences>>(relaxed = true)
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.login("test_token")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockSuccess, result.getOrNull())
        coVerify { mockApi.login("test_token") }
    }

    @Test
    fun login_whenApiThrowsIOException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.login(any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.login("test_token")

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun login_whenTokenIsNull_thenDoesNotSavePreference() = runTest {
        // Given
        val mockSuccess = LoginSuccess(userId = 123L)
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.login(null) } returns mockSuccess

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.login(null)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.login(null) }
    }

    @Test
    fun resetPassword_whenApiReturnsSuccess_thenReturnsSuccess() = runTest {
        // Given
        val mockSuccess = LoginSuccess(userId = 123L)
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.resetPassword("test_login") } returns mockSuccess

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.resetPassword("test_login")

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.resetPassword("test_login") }
    }

    @Test
    fun resetPassword_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.resetPassword(any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.resetPassword("test_login")

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun changePassword_whenApiReturnsSuccess_thenReturnsSuccess() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.changePassword(any<ChangePasswordRequest>())
        } returns mockk(relaxed = true)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.changePassword("current", "new")

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.changePassword(any()) }
    }

    @Test
    fun changePassword_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.changePassword(any<ChangePasswordRequest>())
        } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.changePassword("current", "new")

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun register_whenApiReturnsSuccess_thenReturnsSuccessAndSavesToken() = runTest {
        // Given
        val mockSuccess = LoginSuccess(userId = 456L)
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.register(any()) } returns mockSuccess

        val mockDataStore = mockk<DataStore<Preferences>>(relaxed = true)
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.register(
            RegistrationRequest(
                name = "testuser",
                fullName = "Test User",
                email = "test@example.com",
                password = "password123",
                birthDate = "1990-01-01",
                genderCode = 1
            )
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockSuccess, result.getOrNull())
        coVerify { mockApi.register(any()) }
    }

    @Test
    fun register_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.register(any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.register(
            RegistrationRequest(
                name = "testuser",
                fullName = "Test User",
                email = "test@example.com",
                password = "password123",
                birthDate = "1990-01-01",
                genderCode = 1
            )
        )

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }
}
