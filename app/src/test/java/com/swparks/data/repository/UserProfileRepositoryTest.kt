package com.swparks.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.swparks.data.UserPreferencesRepository
import com.swparks.data.database.dao.UserDao
import com.swparks.data.model.User
import com.swparks.domain.exception.NetworkException
import com.swparks.network.SWApi
import com.swparks.ui.model.MainUserForm
import com.swparks.util.NoOpCrashReporter
import com.swparks.util.NoOpLogger
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
import retrofit2.Response
import java.io.IOException

/**
 * Unit тесты для методов профиля в UserProfileRepository
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileRepositoryTest {
    private val testDispatcher = StandardTestDispatcher()
    private val mockUserDao = mockk<UserDao>(relaxed = true)
    private val crashReporter = NoOpCrashReporter()
    private val logger = NoOpLogger()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createMockUser(id: Long = 1L): User =
        User(
            id = id,
            name = "testuser",
            image = "",
            cityID = 1,
            countryID = 1
        )

    private fun createRepository(
        mockApi: SWApi,
        mockDataStore: DataStore<Preferences> = mockk(relaxed = true)
    ): UserProfileRepository {
        every { mockDataStore.data } returns flowOf(emptyPreferences())
        val prefsRepo = UserPreferencesRepository(mockDataStore)
        return UserProfileRepository(mockApi, prefsRepo, mockUserDao, logger, crashReporter)
    }

    @Test
    fun getUser_whenApiReturnsUser_thenReturnsUser() =
        runTest {
            // Given
            val mockUser = createMockUser(123L)
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getUser(123L) } returns mockUser

            every { mockUserDao.getUserByIdFlow(any()) } returns flowOf(null)

            val repository = createRepository(mockApi)

            // When
            val result = repository.getUser(123L)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(mockUser, result.getOrNull())
            coVerify { mockApi.getUser(123L) }
        }

    @Test
    fun getUser_whenApiThrowsException_thenReturnsFailure() =
        runTest {
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

            val repository = createRepository(mockApi)
            val form =
                MainUserForm(
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
    fun deleteUser_whenApiReturnsSuccess_thenReturnsSuccess() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.deleteUser() } returns Response.success(Unit)

            val mockDataStore = mockk<DataStore<Preferences>>()
            every { mockDataStore.data } returns flowOf(emptyPreferences())
            coEvery { mockDataStore.updateData(any()) } coAnswers {
                firstArg<suspend (Preferences) -> Preferences>().invoke(emptyPreferences())
            }

            val repository = createRepository(mockApi, mockDataStore)

            // When
            val result = repository.deleteUser()

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockApi.deleteUser() }
            coVerify { mockDataStore.updateData(any()) }
        }

    @Test
    fun deleteUser_whenApiThrowsException_thenReturnsFailure() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.deleteUser() } throws IOException("Network error")

            val repository = createRepository(mockApi)

            // When
            val result = repository.deleteUser()

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NetworkException)
        }

    @Test
    fun getSocialUpdates_whenApiReturnsUpdates_thenReturnsSocialUpdates() =
        runTest {
            // Given

            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getUser(1L) } returns createMockUser(1L)

            coEvery { mockApi.getFriendsForUser(1L) } returns
                listOf(
                    createMockUser(2L),
                    createMockUser(3L)
                )

            coEvery { mockApi.getFriendRequests() } returns listOf(createMockUser(4L))

            coEvery { mockApi.getBlacklist() } returns listOf(createMockUser(5L))

            val repository = createRepository(mockApi)

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
    fun getSocialUpdates_whenApiThrowsException_thenReturnsFailure() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getUser(any()) } throws IOException("Network error")

            coEvery { mockApi.getFriendsForUser(any()) } throws IOException("Network error")

            coEvery { mockApi.getFriendRequests() } throws IOException("Network error")

            coEvery { mockApi.getBlacklist() } throws IOException("Network error")

            every { mockUserDao.getCurrentUserFlow() } returns flowOf(null)
            every { mockUserDao.getFriendsFlow() } returns flowOf(emptyList())
            every { mockUserDao.getFriendRequestsFlow() } returns flowOf(emptyList())
            every { mockUserDao.getBlacklistFlow() } returns flowOf(emptyList())

            val repository = createRepository(mockApi)

            // When
            val result = repository.getSocialUpdates(1L)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NetworkException)
        }

    @Test
    fun getSocialUpdates_clearsOldFriendsBeforeInsertingNew() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getUser(1L) } returns createMockUser(1L)
            coEvery { mockApi.getFriendsForUser(1L) } returns listOf(createMockUser(2L))
            coEvery { mockApi.getFriendRequests() } returns emptyList()
            coEvery { mockApi.getBlacklist() } returns emptyList()

            val repository = createRepository(mockApi)

            // When
            val result = repository.getSocialUpdates(1L)

            // Then
            assertTrue(result.isSuccess)

            // Проверяем, что старые флаги были очищены ПЕРЕД вставкой новых данных
            coVerify { mockUserDao.clearAllFriendFlags() }
            coVerify { mockUserDao.clearAllFriendRequestFlags() }
            coVerify { mockUserDao.clearAllBlacklistFlags() }
        }

    @Test
    fun getSocialUpdates_whenApiReturnsEmptyFriendsList_clearsOldFriends() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getUser(1L) } returns createMockUser(1L)
            // API возвращает ПУСТОЙ список друзей (пользователь удалил всех друзей)
            coEvery { mockApi.getFriendsForUser(1L) } returns emptyList()
            coEvery { mockApi.getFriendRequests() } returns emptyList()
            coEvery { mockApi.getBlacklist() } returns emptyList()

            val repository = createRepository(mockApi)

            // When
            val result = repository.getSocialUpdates(1L)

            // Then
            assertTrue(result.isSuccess)
            val socialUpdates = result.getOrNull()
            assertNotNull(socialUpdates)
            assertTrue(socialUpdates?.friends?.isEmpty() == true)

            // КРИТИЧЕСКИ ВАЖНО: старые друзья должны быть очищены из БД
            coVerify { mockUserDao.clearAllFriendFlags() }
        }

    @Test
    fun getSocialUpdates_whenApiReturnsEmptyFriendRequests_clearsOldRequests() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getUser(1L) } returns createMockUser(1L)
            coEvery { mockApi.getFriendsForUser(1L) } returns emptyList()
            // API возвращает ПУСТОЙ список заявок в друзья
            coEvery { mockApi.getFriendRequests() } returns emptyList()
            coEvery { mockApi.getBlacklist() } returns emptyList()

            val repository = createRepository(mockApi)

            // When
            val result = repository.getSocialUpdates(1L)

            // Then
            assertTrue(result.isSuccess)

            // Старые заявки должны быть очищены
            coVerify { mockUserDao.clearAllFriendRequestFlags() }
        }

    @Test
    fun getSocialUpdates_whenApiReturnsEmptyBlacklist_clearsOldBlacklist() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getUser(1L) } returns createMockUser(1L)
            coEvery { mockApi.getFriendsForUser(1L) } returns emptyList()
            coEvery { mockApi.getFriendRequests() } returns emptyList()
            // API возвращает ПУСТОЙ черный список
            coEvery { mockApi.getBlacklist() } returns emptyList()

            val repository = createRepository(mockApi)

            // When
            val result = repository.getSocialUpdates(1L)

            // Then
            assertTrue(result.isSuccess)

            // Старый черный список должен быть очищен
            coVerify { mockUserDao.clearAllBlacklistFlags() }
        }

    @Test
    fun findUsers_whenApiReturnsUsers_thenReturnsUsers() =
        runTest {
            // Given
            val mockUsers = listOf(createMockUser(2L))
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.findUsers("search") } returns mockUsers

            val repository = createRepository(mockApi)

            // When
            val result = repository.findUsers("search")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(mockUsers, result.getOrNull())
            coVerify { mockApi.findUsers("search") }
        }

    @Test
    fun findUsers_whenApiThrowsException_thenReturnsFailure() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.findUsers(any()) } throws IOException("Network error")

            val repository = createRepository(mockApi)

            // When
            val result = repository.findUsers("search")

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NetworkException)
        }
}
