package com.swparks.data.repository

import android.util.Log
import com.swparks.data.database.dao.UserDao
import com.swparks.data.model.ApiBlacklistOption
import com.swparks.data.model.ApiFriendAction
import com.swparks.data.model.User
import com.swparks.domain.exception.NetworkException
import com.swparks.network.SWApi
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/**
 * Unit тесты для методов друзей в FriendsRepository
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FriendsRepositoryTest {
    private val testDispatcher = StandardTestDispatcher()
    private val mockUserDao = mockk<UserDao>(relaxed = true)
    private val crashReporter = NoOpCrashReporter()
    private val logger = NoOpLogger()

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

    private fun createMockUser(id: Long = 1L): User =
        User(
            id = id,
            name = "testuser$id",
            image = "",
            cityID = 1,
            countryID = 1
        )

    @Test
    fun getFriendsForUser_whenApiReturnsFriends_thenReturnsFriends() =
        runTest {
            val mockFriends = listOf(createMockUser(2L), createMockUser(3L))
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getFriendsForUser(1L) } returns mockFriends

            val repository = FriendsRepository(mockApi, mockUserDao, logger, crashReporter)

            val result = repository.getFriendsForUser(1L)

            assertTrue(result.isSuccess)
            assertEquals(mockFriends, result.getOrNull())
            coVerify { mockApi.getFriendsForUser(1L) }
        }

    @Test
    fun getFriendsForUser_whenApiThrowsException_thenReturnsFailure() =
        runTest {
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getFriendsForUser(any()) } throws IOException("Network error")

            val repository = FriendsRepository(mockApi, mockUserDao, logger, crashReporter)

            val result = repository.getFriendsForUser(1L)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NetworkException)
        }

    @Test
    fun getFriendRequests_whenApiReturnsRequests_thenReturnsRequests() =
        runTest {
            val mockRequests = listOf(createMockUser(2L))
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getFriendRequests() } returns mockRequests

            val repository = FriendsRepository(mockApi, mockUserDao, logger, crashReporter)

            val result = repository.getFriendRequests()

            assertTrue(result.isSuccess)
            assertEquals(mockRequests, result.getOrNull())
            coVerify { mockApi.getFriendRequests() }
        }

    @Test
    fun getFriendRequests_whenApiThrowsException_thenReturnsFailure() =
        runTest {
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getFriendRequests() } throws IOException("Network error")

            val repository = FriendsRepository(mockApi, mockUserDao, logger, crashReporter)

            val result = repository.getFriendRequests()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NetworkException)
        }

    @Test
    fun respondToFriendRequest_whenAcceptTrue_thenUpdatesCache() =
        runTest {
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.acceptFriendRequest(2L) } returns Response.success(Unit)

            val repository = FriendsRepository(mockApi, mockUserDao, logger, crashReporter)

            val result = repository.respondToFriendRequest(2L, true)

            assertTrue(result.isSuccess)
            coVerify { mockApi.acceptFriendRequest(2L) }
            coVerify { mockUserDao.markAsFriend(2L) }
            coVerify { mockUserDao.removeFriendRequest(2L) }
            coVerify(exactly = 0) { mockApi.declineFriendRequest(any()) }
        }

    @Test
    fun respondToFriendRequest_whenAcceptFalse_thenUpdatesCache() =
        runTest {
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.declineFriendRequest(2L) } returns Response.success(Unit)

            val repository = FriendsRepository(mockApi, mockUserDao, logger, crashReporter)

            val result = repository.respondToFriendRequest(2L, false)

            assertTrue(result.isSuccess)
            coVerify { mockApi.declineFriendRequest(2L) }
            coVerify { mockUserDao.removeFriendRequest(2L) }
            coVerify(exactly = 0) { mockApi.acceptFriendRequest(any()) }
            coVerify(exactly = 0) { mockUserDao.markAsFriend(any()) }
        }

    @Test
    fun respondToFriendRequest_whenApiFails_thenDoesNotUpdateCache() =
        runTest {
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.acceptFriendRequest(any()) } throws IOException("Network error")

            val repository = FriendsRepository(mockApi, mockUserDao, logger, crashReporter)

            val result = repository.respondToFriendRequest(2L, true)

            assertTrue(result.isFailure)
            coVerify { mockApi.acceptFriendRequest(2L) }
            coVerify(exactly = 0) { mockUserDao.markAsFriend(any()) }
            coVerify(exactly = 0) { mockUserDao.removeFriendRequest(any()) }
        }

    @Test
    fun friendAction_whenActionAdd_thenCallsSendFriendRequest() =
        runTest {
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.sendFriendRequest(2L) } returns Response.success(Unit)

            val repository = FriendsRepository(mockApi, mockUserDao, logger, crashReporter)

            val result = repository.friendAction(2L, ApiFriendAction.ADD)

            assertTrue(result.isSuccess)
            coVerify { mockApi.sendFriendRequest(2L) }
            coVerify(exactly = 0) { mockApi.deleteFriend(any()) }
            coVerify(exactly = 0) { mockUserDao.markAsFriend(any()) }
        }

    @Test
    fun friendAction_whenActionRemove_thenCallsDeleteFriend() =
        runTest {
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.deleteFriend(2L) } returns Response.success(Unit)

            val repository = FriendsRepository(mockApi, mockUserDao, logger, crashReporter)

            val result = repository.friendAction(2L, ApiFriendAction.REMOVE)

            assertTrue(result.isSuccess)
            coVerify { mockApi.deleteFriend(2L) }
            coVerify(exactly = 0) { mockApi.sendFriendRequest(any()) }
            coVerify { mockUserDao.removeFriend(2L) }
            coVerify { mockUserDao.decrementFriendsCount() }
        }

    @Test
    fun friendAction_whenApiThrowsException_thenReturnsFailure() =
        runTest {
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.sendFriendRequest(any()) } throws IOException("Network error")

            val repository = FriendsRepository(mockApi, mockUserDao, logger, crashReporter)

            val result = repository.friendAction(2L, ApiFriendAction.ADD)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NetworkException)
            coVerify(exactly = 0) { mockUserDao.markAsFriend(any()) }
            coVerify(exactly = 0) { mockUserDao.decrementFriendsCount() }
        }

    @Test
    fun friendAction_whenRemoveApiFails_thenDoesNotUpdateCache() =
        runTest {
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.deleteFriend(any()) } throws IOException("Network error")

            val repository = FriendsRepository(mockApi, mockUserDao, logger, crashReporter)

            val result = repository.friendAction(2L, ApiFriendAction.REMOVE)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NetworkException)
            coVerify(exactly = 0) { mockUserDao.removeFriend(any()) }
            coVerify(exactly = 0) { mockUserDao.decrementFriendsCount() }
        }

    @Test
    fun blacklistAction_whenOptionAdd_thenCallsAddToBlacklist() =
        runTest {
            val mockUser = createMockUser(2L)
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.addToBlacklist(2L) } returns Response.success(Unit)

            val repository = FriendsRepository(mockApi, mockUserDao, logger, crashReporter)

            val result = repository.blacklistAction(mockUser, ApiBlacklistOption.ADD)

            assertTrue(result.isSuccess)
            coVerify { mockApi.addToBlacklist(2L) }
            coVerify(exactly = 0) { mockApi.deleteFromBlacklist(any()) }
        }

    @Test
    fun blacklistAction_whenOptionRemove_thenCallsDeleteFromBlacklist() =
        runTest {
            val mockUser = createMockUser(2L)
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.deleteFromBlacklist(2L) } returns Response.success(Unit)

            val repository = FriendsRepository(mockApi, mockUserDao, logger, crashReporter)

            val result = repository.blacklistAction(mockUser, ApiBlacklistOption.REMOVE)

            assertTrue(result.isSuccess)
            coVerify { mockApi.deleteFromBlacklist(2L) }
            coVerify(exactly = 0) { mockApi.addToBlacklist(any()) }
        }

    @Test
    fun blacklistAction_whenApiThrowsException_thenReturnsFailure() =
        runTest {
            val mockUser = createMockUser(2L)
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.addToBlacklist(any()) } throws IOException("Network error")

            val repository = FriendsRepository(mockApi, mockUserDao, logger, crashReporter)

            val result = repository.blacklistAction(mockUser, ApiBlacklistOption.ADD)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NetworkException)
        }

    @Test
    fun getBlacklist_whenApiReturnsBlacklist_thenReturnsBlacklist() =
        runTest {
            val mockBlacklist = listOf(createMockUser(5L))
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getBlacklist() } returns mockBlacklist

            val repository = FriendsRepository(mockApi, mockUserDao, logger, crashReporter)

            val result = repository.getBlacklist()

            assertTrue(result.isSuccess)
            assertEquals(mockBlacklist, result.getOrNull())
            coVerify { mockApi.getBlacklist() }
        }

    @Test
    fun getBlacklist_whenApiThrowsException_thenReturnsFailure() =
        runTest {
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getBlacklist() } throws IOException("Network error")

            val repository = FriendsRepository(mockApi, mockUserDao, logger, crashReporter)

            val result = repository.getBlacklist()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NetworkException)
        }
}
