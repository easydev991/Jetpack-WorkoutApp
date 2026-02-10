package com.swparks.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.swparks.data.database.dao.JournalDao
import com.swparks.data.database.dao.JournalEntryDao
import com.swparks.data.database.dao.UserDao
import com.swparks.data.model.ApiBlacklistOption
import com.swparks.data.model.ApiFriendAction
import com.swparks.data.model.User
import com.swparks.domain.exception.NetworkException
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
 * Unit тесты для методов друзей в SWRepository
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SWRepositoryFriendsTest {
    private val testDispatcher = StandardTestDispatcher()
    private val mockUserDao = mockk<UserDao>(relaxed = true)
    private val mockJournalDao = mockk<JournalDao>(relaxed = true)
    private val mockJournalEntryDao = mockk<JournalEntryDao>(relaxed = true)

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

    private fun createMockUser(id: Long = 1L): User {
        return User(
            id = id,
            name = "testuser$id",
            image = "",
            cityID = 1,
            countryID = 1
        )
    }

    @Test
    fun getFriendsForUser_whenApiReturnsFriends_thenReturnsFriends() = runTest {
        // Given
        val mockFriends = listOf(createMockUser(2L), createMockUser(3L))
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getFriendsForUser(1L) } returns mockFriends

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao
        )

        // When
        val result = repository.getFriendsForUser(1L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockFriends, result.getOrNull())
        coVerify { mockApi.getFriendsForUser(1L) }
    }

    @Test
    fun getFriendsForUser_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getFriendsForUser(any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao
        )

        // When
        val result = repository.getFriendsForUser(1L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun getFriendRequests_whenApiReturnsRequests_thenReturnsRequests() = runTest {
        // Given
        val mockRequests = listOf(createMockUser(2L))
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getFriendRequests() } returns mockRequests

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao
        )

        // When
        val result = repository.getFriendRequests()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockRequests, result.getOrNull())
        coVerify { mockApi.getFriendRequests() }
    }

    @Test
    fun getFriendRequests_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getFriendRequests() } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao
        )

        // When
        val result = repository.getFriendRequests()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun respondToFriendRequest_whenAcceptTrue_thenUpdatesCache() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.acceptFriendRequest(2L) } returns mockk(relaxed = true)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao
        )

        // When
        val result = repository.respondToFriendRequest(2L, true)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.acceptFriendRequest(2L) }
        coVerify { mockUserDao.markAsFriend(2L) }
        coVerify { mockUserDao.removeFriendRequest(2L) }
        coVerify(exactly = 0) { mockApi.declineFriendRequest(any()) }
    }

    @Test
    fun respondToFriendRequest_whenAcceptFalse_thenUpdatesCache() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.declineFriendRequest(2L) } returns mockk(relaxed = true)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao
        )

        // When
        val result = repository.respondToFriendRequest(2L, false)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.declineFriendRequest(2L) }
        coVerify { mockUserDao.removeFriendRequest(2L) }
        coVerify(exactly = 0) { mockApi.acceptFriendRequest(any()) }
        coVerify(exactly = 0) { mockUserDao.markAsFriend(any()) }
    }

    @Test
    fun respondToFriendRequest_whenApiFails_thenDoesNotUpdateCache() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.acceptFriendRequest(any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao
        )

        // When
        val result = repository.respondToFriendRequest(2L, true)

        // Then
        assertTrue(result.isFailure)
        coVerify { mockApi.acceptFriendRequest(2L) }
        coVerify(exactly = 0) { mockUserDao.markAsFriend(any()) }
        coVerify(exactly = 0) { mockUserDao.removeFriendRequest(any()) }
    }

    @Test
    fun friendAction_whenActionAdd_thenCallsSendFriendRequest() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.sendFriendRequest(2L) } returns mockk(relaxed = true)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao
        )

        // When
        val result = repository.friendAction(2L, ApiFriendAction.ADD)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.sendFriendRequest(2L) }
        coVerify(exactly = 0) { mockApi.deleteFriend(any()) }
    }

    @Test
    fun friendAction_whenActionRemove_thenCallsDeleteFriend() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.deleteFriend(2L) } returns mockk(relaxed = true)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao
        )

        // When
        val result = repository.friendAction(2L, ApiFriendAction.REMOVE)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.deleteFriend(2L) }
        coVerify(exactly = 0) { mockApi.sendFriendRequest(any()) }
    }

    @Test
    fun friendAction_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.sendFriendRequest(any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao
        )

        // When
        val result = repository.friendAction(2L, ApiFriendAction.ADD)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun blacklistAction_whenOptionAdd_thenCallsAddToBlacklist() = runTest {
        // Given
        val mockUser = createMockUser(2L)
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.addToBlacklist(2L) } returns mockk(relaxed = true)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao
        )

        // When
        val result = repository.blacklistAction(mockUser, ApiBlacklistOption.ADD)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.addToBlacklist(2L) }
        coVerify(exactly = 0) { mockApi.deleteFromBlacklist(any()) }
    }

    @Test
    fun blacklistAction_whenOptionRemove_thenCallsDeleteFromBlacklist() = runTest {
        // Given
        val mockUser = createMockUser(2L)
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.deleteFromBlacklist(2L) } returns mockk(relaxed = true)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao
        )

        // When
        val result = repository.blacklistAction(mockUser, ApiBlacklistOption.REMOVE)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.deleteFromBlacklist(2L) }
        coVerify(exactly = 0) { mockApi.addToBlacklist(any()) }
    }

    @Test
    fun blacklistAction_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockUser = createMockUser(2L)
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.addToBlacklist(any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao
        )

        // When
        val result = repository.blacklistAction(mockUser, ApiBlacklistOption.ADD)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun getBlacklist_whenApiReturnsBlacklist_thenReturnsBlacklist() = runTest {
        // Given
        val mockBlacklist = listOf(createMockUser(5L))
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getBlacklist() } returns mockBlacklist

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao
        )

        // When
        val result = repository.getBlacklist()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockBlacklist, result.getOrNull())
        coVerify { mockApi.getBlacklist() }
    }

    @Test
    fun getBlacklist_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getBlacklist() } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao
        )

        // When
        val result = repository.getBlacklist()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }
}
