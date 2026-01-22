package com.swparks.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.swparks.domain.exception.NetworkException
import com.swparks.model.MainUserForm
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Unit тесты для методов профиля в SWRepository
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SWRepositoryProfileTest {
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

    private fun createMockUser(id: Long = 1L): com.swparks.model.User {
        return com.swparks.model.User(
            id = id,
            name = "testuser",
            image = "",
            lang = "ru",
            cityID = 1,
            countryID = 1
        )
    }

    @Test
    fun getUser_whenApiReturnsUser_thenReturnsUser() = runTest {
        // Given
        val mockUser = createMockUser(123L)
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getUser(123L) } returns mockUser

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.getUser(123L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockUser, result.getOrNull())
        coVerify { mockApi.getUser(123L) }
    }

    @Test
    fun getUser_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getUser(any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.getUser(123L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun editUser_whenApiReturnsUser_thenReturnsUser() = runTest {
        // Given
        val mockUser = createMockUser(123L)
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.editUser(
                userId = any(),
                name = any(),
                fullName = any(),
                email = any(),
                birthDate = any(),
                gender = any(),
                countryId = any(),
                cityId = any(),
                image = any()
            )
        } returns mockUser

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)
        val form = MainUserForm(
            name = "testuser",
            fullname = "Test User",
            email = "test@example.com",
            password = "password",
            birthDate = "2000-01-01",
            genderCode = 1,
            countryId = 1,
            cityId = 1
        )

        // When
        val result = repository.editUser(123L, form, null)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockUser, result.getOrNull())
    }

    @Test
    fun editUser_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.editUser(
                userId = any(),
                name = any(),
                fullName = any(),
                email = any(),
                birthDate = any(),
                gender = any(),
                countryId = any(),
                cityId = any(),
                image = any()
            )
        } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)
        val form = MainUserForm(
            name = "testuser",
            fullname = "Test User",
            email = "test@example.com",
            password = "password",
            birthDate = "2000-01-01",
            genderCode = 1,
            countryId = 1,
            cityId = 1
        )

        // When
        val result = repository.editUser(123L, form, null)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun deleteUser_whenApiReturnsSuccess_thenReturnsSuccessAndClearsAuth() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.deleteUser() } returns mockk(relaxed = true)

        val mockDataStore = mockk<DataStore<Preferences>>(relaxed = true)
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.deleteUser()

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.deleteUser() }
    }

    @Test
    fun deleteUser_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.deleteUser() } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.deleteUser()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun getSocialUpdates_whenApiReturnsUpdates_thenReturnsSocialUpdates() = runTest {
        // Given

        val mockApi = mockk<SWApi>()
coEvery { mockApi.getUser(1L) } returns createMockUser(1L)

coEvery { mockApi.getFriendsForUser(1L) } returns listOf(createMockUser(2L), createMockUser(3L))

coEvery { mockApi.getFriendRequests() } returns listOf(createMockUser(4L))

coEvery { mockApi.getBlacklist() } returns listOf(createMockUser(5L))


        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.getSocialUpdates(1L)

        // Then
        assertTrue(result.isSuccess)
        val socialUpdates = result.getOrNull()
        assertNotNull(socialUpdates)
        assertEquals(2, socialUpdates?.friends?.size)
        assertEquals(1, socialUpdates?.friendRequests?.size)
        assertEquals(1, socialUpdates?.blacklist?.size)
coVerify { mockApi.getUser(1L) }

coVerify { mockApi.getFriendsForUser(1L) }

coVerify { mockApi.getFriendRequests() }

coVerify { mockApi.getBlacklist() }

    }

    @Test
    fun getSocialUpdates_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
coEvery { mockApi.getUser(any()) } throws IOException("Network error")

coEvery { mockApi.getFriendsForUser(any()) } throws IOException("Network error")

coEvery { mockApi.getFriendRequests() } throws IOException("Network error")

coEvery { mockApi.getBlacklist() } throws IOException("Network error")


val mockDataStore = mockk<DataStore<Preferences>>(relaxed = true)

        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.getSocialUpdates(1L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun findUsers_whenApiReturnsUsers_thenReturnsUsers() = runTest {
        // Given
        val mockUsers = listOf(createMockUser(2L))
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.findUsers("search") } returns mockUsers

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.findUsers("search")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockUsers, result.getOrNull())
        coVerify { mockApi.findUsers("search") }
    }

    @Test
    fun findUsers_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.findUsers(any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.findUsers("search")

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }
}
