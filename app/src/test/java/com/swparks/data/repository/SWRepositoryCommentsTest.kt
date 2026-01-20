package com.swparks.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.swparks.domain.exception.NetworkException
import com.swparks.domain.exception.ServerException
import com.swparks.model.TextEntryOption
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

        val repository = SWRepositoryImp(mockApi, mockDataStore)
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

        val repository = SWRepositoryImp(mockApi, mockDataStore)
        val option = TextEntryOption.Event(id = 456L)

        // When
        val result = repository.addComment(option, "Test comment")

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.addCommentToEvent(eventId = 456L, comment = "Test comment") }
    }

    @Test
    fun addComment_whenOptionIsJournal_thenCallsAddCommentToJournalEntry() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.addCommentToJournalEntry(
                userId = any(),
                journalId = any(),
                entryId = any(),
                comment = any()
            )
        } returns mockk(relaxed = true)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)
        val option = TextEntryOption.Journal(ownerId = 1L, journalId = 789L, entryId = 123L)

        // When
        val result = repository.addComment(option, "Test comment")

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            mockApi.addCommentToJournalEntry(
                userId = 1L,
                journalId = 789L,
                entryId = 123L,
                comment = "Test comment"
            )
        }
    }

    @Test
    fun addComment_whenOptionIsJournalWithoutEntryId_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)
        val option = TextEntryOption.Journal(ownerId = 1L, journalId = 789L, entryId = null)

        // When
        val result = repository.addComment(option, "Test comment")

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ServerException)
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

        val repository = SWRepositoryImp(mockApi, mockDataStore)
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

        val repository = SWRepositoryImp(mockApi, mockDataStore)
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

        val repository = SWRepositoryImp(mockApi, mockDataStore)
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
    fun editComment_whenOptionIsJournal_thenCallsEditJournalEntryComment() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.editJournalEntryComment(
                userId = any(),
                journalId = any(),
                entryId = any(),
                commentId = any(),
                comment = any()
            )
        } returns mockk(relaxed = true)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)
        val option = TextEntryOption.Journal(ownerId = 1L, journalId = 789L, entryId = 123L)

        // When
        val result = repository.editComment(option, 999L, "Updated comment")

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            mockApi.editJournalEntryComment(
                userId = 1L,
                journalId = 789L,
                entryId = 123L,
                commentId = 999L,
                comment = "Updated comment"
            )
        }
    }

    @Test
    fun editComment_whenOptionIsJournalWithoutEntryId_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)
        val option = TextEntryOption.Journal(ownerId = 1L, journalId = 789L, entryId = null)

        // When
        val result = repository.editComment(option, 999L, "Updated comment")

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ServerException)
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

        val repository = SWRepositoryImp(mockApi, mockDataStore)
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

        val repository = SWRepositoryImp(mockApi, mockDataStore)
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

        val repository = SWRepositoryImp(mockApi, mockDataStore)
        val option = TextEntryOption.Event(id = 456L)

        // When
        val result = repository.deleteComment(option, 999L)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.deleteEventComment(eventId = 456L, commentId = 999L) }
    }

    @Test
    fun deleteComment_whenOptionIsJournal_thenCallsDeleteJournalEntryComment() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.deleteJournalEntryComment(
                userId = any(),
                journalId = any(),
                entryId = any(),
                commentId = any()
            )
        } returns mockk(relaxed = true)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)
        val option = TextEntryOption.Journal(ownerId = 1L, journalId = 789L, entryId = 123L)

        // When
        val result = repository.deleteComment(option, 999L)

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            mockApi.deleteJournalEntryComment(
                userId = 1L,
                journalId = 789L,
                entryId = 123L,
                commentId = 999L
            )
        }
    }

    @Test
    fun deleteComment_whenOptionIsJournalWithoutEntryId_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)
        val option = TextEntryOption.Journal(ownerId = 1L, journalId = 789L, entryId = null)

        // When
        val result = repository.deleteComment(option, 999L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ServerException)
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

        val repository = SWRepositoryImp(mockApi, mockDataStore)
        val option = TextEntryOption.Park(id = 123L)

        // When
        val result = repository.deleteComment(option, 999L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }
}
