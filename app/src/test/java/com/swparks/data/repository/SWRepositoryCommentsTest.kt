package com.swparks.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.swparks.data.database.dao.DialogDao
import com.swparks.data.database.dao.JournalDao
import com.swparks.data.database.dao.JournalEntryDao
import com.swparks.data.database.dao.UserDao
import com.swparks.domain.exception.NetworkException
import com.swparks.network.SWApi
import com.swparks.ui.model.TextEntryOption
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Unit тесты для методов комментариев в SWRepository
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SWRepositoryCommentsTest {
    private val testDispatcher = StandardTestDispatcher()
    private val mockUserDao = mockk<UserDao>(relaxed = true)
    private val mockJournalDao = mockk<JournalDao>(relaxed = true)
    private val mockJournalEntryDao = mockk<JournalEntryDao>(relaxed = true)
    private val mockDialogDao = mockk<DialogDao>(relaxed = true)

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
    fun addComment_whenOptionIsPark_thenCallsAddCommentToPark() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.addCommentToPark(
                parkId = any(),
                comment = any()
            )
        } returns mockk(relaxed = true)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao
        )
        val option = TextEntryOption.Park(id = 123L)

        // When
        val result = repository.addComment(option, "Test comment")

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.addCommentToPark(parkId = 123L, comment = "Test comment") }
    }

    @Test
    fun addComment_whenOptionIsEvent_thenCallsAddCommentToEvent() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.addCommentToEvent(eventId = any(), comment = any()) } returns mockk(
            relaxed = true
        )

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao
        )
        val option = TextEntryOption.Event(id = 456L)

        // When
        val result = repository.addComment(option, "Test comment")

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.addCommentToEvent(eventId = 456L, comment = "Test comment") }
    }

    @Test
    fun addComment_whenOptionIsJournal_thenCallsSaveJournalEntry() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.saveJournalEntry(
                userId = any(),
                journalId = any(),
                message = any()
            )
        } returns mockk(relaxed = true)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao
        )
        val option = TextEntryOption.Journal(ownerId = 1L, journalId = 789L)

        // When
        val result = repository.addComment(option, "Test comment")

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            mockApi.saveJournalEntry(
                userId = 1L,
                journalId = 789L,
                message = "Test comment"
            )
        }
    }

    @Test
    fun addComment_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.addCommentToPark(
                parkId = any(),
                comment = any()
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
            mockDialogDao
        )
        val option = TextEntryOption.Park(id = 123L)

        // When
        val result = repository.addComment(option, "Test comment")

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun editComment_whenOptionIsPark_thenCallsEditParkComment() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.editParkComment(
                parkId = any(),
                commentId = any(),
                comment = any()
            )
        } returns mockk(relaxed = true)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao
        )
        val option = TextEntryOption.Park(id = 123L)

        // When
        val result = repository.editComment(option, 999L, "Updated comment")

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            mockApi.editParkComment(
                parkId = 123L,
                commentId = 999L,
                comment = "Updated comment"
            )
        }
    }

    @Test
    fun editComment_whenOptionIsEvent_thenCallsEditEventComment() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.editEventComment(
                eventId = any(),
                commentId = any(),
                comment = any()
            )
        } returns mockk(relaxed = true)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao
        )
        val option = TextEntryOption.Event(id = 456L)

        // When
        val result = repository.editComment(option, 999L, "Updated comment")

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            mockApi.editEventComment(
                eventId = 456L,
                commentId = 999L,
                comment = "Updated comment"
            )
        }
    }

    @Test
    fun editComment_whenOptionIsJournal_thenCallsEditJournalEntry() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.editJournalEntry(
                userId = any(),
                journalId = any(),
                entryId = any(),
                newEntryText = any()
            )
        } returns mockk(relaxed = true)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        // Мок для существующей записи
        val existingEntry = com.swparks.data.database.entity.JournalEntryEntity(
            id = 999L,
            journalId = 789L,
            authorId = 1L,
            authorName = "Test User",
            message = "Old message",
            createDate = "2024-01-01T12:00:00",
            modifyDate = 1000000L,
            authorImage = null
        )
        coEvery { mockJournalEntryDao.getById(999L) } returns existingEntry

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao
        )
        val option = TextEntryOption.Journal(ownerId = 1L, journalId = 789L)

        // When
        val result = repository.editComment(option, 999L, "Updated comment")

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            mockApi.editJournalEntry(
                userId = 1L,
                journalId = 789L,
                entryId = 999L,
                newEntryText = "Updated comment"
            )
            mockJournalEntryDao.getById(999L)
            mockJournalEntryDao.insert(
                match<com.swparks.data.database.entity.JournalEntryEntity> {
                    it.id == 999L &&
                            it.message == "Updated comment" &&
                            it.modifyDate != 1000000L // modifyDate должен измениться
                }
            )
        }
    }

    @Test
    fun editComment_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.editParkComment(
                parkId = any(),
                commentId = any(),
                comment = any()
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
            mockDialogDao
        )
        val option = TextEntryOption.Park(id = 123L)

        // When
        val result = repository.editComment(option, 999L, "Updated comment")

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun deleteComment_whenOptionIsPark_thenCallsDeleteParkComment() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.deleteParkComment(parkId = any(), commentId = any()) } returns mockk(
            relaxed = true
        )

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao
        )
        val option = TextEntryOption.Park(id = 123L)

        // When
        val result = repository.deleteComment(option, 999L)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.deleteParkComment(parkId = 123L, commentId = 999L) }
    }

    @Test
    fun deleteComment_whenOptionIsEvent_thenCallsDeleteEventComment() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.deleteEventComment(eventId = any(), commentId = any()) } returns mockk(
            relaxed = true
        )

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao
        )
        val option = TextEntryOption.Event(id = 456L)

        // When
        val result = repository.deleteComment(option, 999L)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.deleteEventComment(eventId = 456L, commentId = 999L) }
    }

    @Test
    fun deleteComment_whenOptionIsJournal_thenCallsDeleteJournalEntry() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.deleteJournalEntry(
                userId = any(),
                journalId = any(),
                entryId = any()
            )
        } returns mockk(relaxed = true)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao
        )
        val option = TextEntryOption.Journal(ownerId = 1L, journalId = 789L)

        // When
        val result = repository.deleteComment(option, 999L)

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            mockApi.deleteJournalEntry(
                userId = 1L,
                journalId = 789L,
                entryId = 999L
            )
        }
    }

    @Test
    fun deleteComment_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.deleteParkComment(parkId = any(), commentId = any()) } throws IOException(
            "Network error"
        )

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao
        )
        val option = TextEntryOption.Park(id = 123L)

        // When
        val result = repository.deleteComment(option, 999L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }
}
