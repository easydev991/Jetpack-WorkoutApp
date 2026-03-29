package com.swparks.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.swparks.data.database.dao.DialogDao
import com.swparks.data.database.dao.EventDao
import com.swparks.data.database.dao.JournalDao
import com.swparks.data.database.dao.JournalEntryDao
import com.swparks.data.database.dao.ParkDao
import com.swparks.data.database.dao.UserDao
import com.swparks.data.model.JournalEntryResponse
import com.swparks.data.model.JournalResponse
import com.swparks.domain.exception.NetworkException
import com.swparks.network.SWApi
import com.swparks.ui.model.JournalAccess
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/**
 * Unit тесты для методов дневников в SWRepository
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SWRepositoryJournalsTest {
    private val testDispatcher = StandardTestDispatcher()
    private val mockUserDao = mockk<UserDao>(relaxed = true)
    private val mockJournalDao = mockk<JournalDao>(relaxed = true)
    private val mockJournalEntryDao = mockk<JournalEntryDao>(relaxed = true)
    private val mockDialogDao = mockk<DialogDao>(relaxed = true)
    private val mockEventDao = mockk<EventDao>(relaxed = true)
    private val mockParkDao = mockk<ParkDao>(relaxed = true)
    private val crashReporter = NoOpCrashReporter()
    private val logger = NoOpLogger()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createMockJournal(id: Long = 1L): JournalResponse {
        return JournalResponse(
            id = id,
            title = "Test Journal",
            lastMessageImage = null,
            createDate = "2024-01-01",
            modifyDate = "2024-01-01",
            lastMessageDate = "2024-01-01",
            lastMessageText = "Last entry",
            count = null,
            ownerId = 1,
            viewAccess = 0,
            commentAccess = 0
        )
    }

    private fun createMockJournalEntry(id: Long = 1L): JournalEntryResponse {
        return JournalEntryResponse(
            id = id,
            journalId = 1,
            authorId = 1,
            name = null,
            message = "Test entry",
            createDate = "2024-01-01",
            modifyDate = "2024-01-01",
            image = null
        )
    }

    @Test
    fun getJournals_whenApiReturnsJournals_thenReturnsJournals() = runTest {
        // Given
        val mockJournalsList = listOf(
            createMockJournal(1L),
            createMockJournal(2L),
            createMockJournal(3L)
        )
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getJournals(1L) } returns mockJournalsList

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao,
            mockEventDao,
            mockParkDao,
            crashReporter,
            logger
        )

        // When
        val result = repository.getJournals(1L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockJournalsList, result.getOrNull())
        coVerify { mockApi.getJournals(1L) }
    }

    @Test
    fun getJournals_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getJournals(any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao,
            mockEventDao,
            mockParkDao,
            crashReporter,
            logger
        )

        // When
        val result = repository.getJournals(1L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun getJournal_whenApiReturnsJournal_thenReturnsJournal() = runTest {
        // Given
        val mockJournal = createMockJournal(123L)
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getJournal(1L, 123L) } returns mockJournal

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao,
            mockEventDao,
            mockParkDao,
            crashReporter,
            logger
        )

        // When
        val result = repository.getJournal(1L, 123L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockJournal, result.getOrNull())
        coVerify { mockApi.getJournal(1L, 123L) }
    }

    @Test
    fun getJournal_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getJournal(any(), any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao,
            mockEventDao,
            mockParkDao,
            crashReporter,
            logger
        )

        // When
        val result = repository.getJournal(1L, 123L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun getJournalEntries_whenApiReturnsEntries_thenReturnsEntries() = runTest {
        // Given
        val mockEntriesList = listOf(
            createMockJournalEntry(1L),
            createMockJournalEntry(2L),
            createMockJournalEntry(3L)
        )
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getJournalEntries(1L, 123L) } returns mockEntriesList

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao,
            mockEventDao,
            mockParkDao,
            crashReporter,
            logger
        )

        // When
        val result = repository.getJournalEntries(1L, 123L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockEntriesList, result.getOrNull())
        coVerify { mockApi.getJournalEntries(1L, 123L) }
    }

    @Test
    fun getJournalEntries_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getJournalEntries(any(), any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao,
            mockEventDao,
            mockParkDao,
            crashReporter,
            logger
        )

        // When
        val result = repository.getJournalEntries(1L, 123L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun editJournalSettings_whenApiReturnsSuccess_thenReturnsSuccess() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.editJournalSettings(
                userId = any(),
                journalId = any(),
                title = any(),
                viewAccess = any(),
                commentAccess = any()
            )
        } returns Response.success(Unit)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao,
            mockEventDao,
            mockParkDao,
            crashReporter,
            logger
        )

        // When
        val result = repository.editJournalSettings(
            journalId = 123L,
            title = "New Title",
            userId = 1L,
            viewAccess = JournalAccess.ALL,
            commentAccess = JournalAccess.FRIENDS
        )

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            mockApi.editJournalSettings(
                userId = any(),
                journalId = 123L,
                title = any(),
                viewAccess = any(),
                commentAccess = any()
            )
        }
    }

    @Test
    fun editJournalSettings_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.editJournalSettings(
                userId = any(),
                journalId = any(),
                title = any(),
                viewAccess = any(),
                commentAccess = any()
            )
        } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao,
            mockEventDao,
            mockParkDao,
            crashReporter,
            logger
        )

        // When
        val result = repository.editJournalSettings(
            journalId = 123L,
            title = "New Title",
            userId = 1L,
            viewAccess = JournalAccess.ALL,
            commentAccess = JournalAccess.FRIENDS
        )

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun createJournal_whenApiReturnsSuccess_thenReturnsSuccess() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.createJournal(
                userId = any(),
                title = any()
            )
        } returns Response.success(Unit)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao,
            mockEventDao,
            mockParkDao,
            crashReporter,
            logger
        )

        // When
        val result = repository.createJournal("New Journal", 1L)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.createJournal(userId = 1L, title = "New Journal") }
    }

    @Test
    fun createJournal_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.createJournal(
                userId = any(),
                title = any()
            )
        } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao,
            mockEventDao,
            mockParkDao,
            crashReporter,
            logger
        )

        // When
        val result = repository.createJournal("New Journal", 1L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun deleteJournal_whenApiReturnsSuccess_thenReturnsSuccess() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.deleteJournal(
                userId = any(),
                journalId = any()
            )
        } returns Response.success(Unit)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao,
            mockEventDao,
            mockParkDao,
            crashReporter,
            logger
        )

        // When
        val result = repository.deleteJournal(123L, 1L)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.deleteJournal(userId = 1L, journalId = 123L) }
    }

    @Test
    fun deleteJournal_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.deleteJournal(
                userId = any(),
                journalId = any()
            )
        } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao,
            mockEventDao,
            mockParkDao,
            crashReporter,
            logger
        )

        // When
        val result = repository.deleteJournal(123L, 1L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }
}
