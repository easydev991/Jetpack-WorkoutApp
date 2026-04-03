package com.swparks.data.repository

import android.util.Log
import com.swparks.data.database.dao.JournalEntryDao
import com.swparks.data.database.entity.JournalEntryEntity
import com.swparks.data.model.JournalEntryResponse
import com.swparks.network.SWApi
import com.swparks.util.NoOpCrashReporter
import com.swparks.util.NoOpLogger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Unit тесты для JournalEntriesRepository
 */
class JournalEntriesRepositoryTest {
    private val mockApi = mockk<SWApi>()
    private val mockJournalEntryDao = mockk<JournalEntryDao>(relaxed = true)
    private val crashReporter = NoOpCrashReporter()
    private val logger = NoOpLogger()

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createMockJournalEntryResponse(id: Long = 1L): JournalEntryResponse =
        JournalEntryResponse(
            id = id,
            journalId = 1,
            authorId = 1,
            name = "Test Author",
            message = "Test entry message",
            createDate = "2024-01-01T10:00:00",
            modifyDate = "2024-01-01T10:00:00",
            image = "https://example.com/image.jpg"
        )

    private fun createMockJournalEntryEntity(id: Long = 1L): JournalEntryEntity =
        JournalEntryEntity(
            id = id,
            journalId = 1L,
            authorId = 1L,
            authorName = "Test Author",
            message = "Test entry message",
            createDate = "2024-01-01T10:00:00",
            modifyDate = 1704110400000L, // 2024-01-01T10:00:00 UTC
            authorImage = "https://example.com/image.jpg"
        )

    @Test
    fun observeJournalEntries_whenDaoReturnsEntries_thenReturnsDomainEntries() =
        runTest {
            // Given
            val mockEntities =
                listOf(
                    createMockJournalEntryEntity(1L),
                    createMockJournalEntryEntity(2L),
                    createMockJournalEntryEntity(3L)
                )
            every { mockJournalEntryDao.getJournalEntriesByJournalId(1L) } returns
                flowOf(
                    mockEntities
                )

            val repository =
                JournalEntriesRepositoryImpl(mockApi, mockJournalEntryDao, crashReporter, logger)

            // When
            val result = repository.observeJournalEntries(1L, 1L).toList()

            // Then
            assertEquals(1, result.size)
            assertEquals(3, result[0].size)
            assertEquals(1L, result[0][0].id)
            assertEquals(2L, result[0][1].id)
            assertEquals(3L, result[0][2].id)
        }

    @Test
    fun refreshJournalEntries_whenApiReturnsEntries_thenSavesToDatabase() =
        runTest {
            // Given
            val mockResponses =
                listOf(
                    createMockJournalEntryResponse(1L),
                    createMockJournalEntryResponse(2L),
                    createMockJournalEntryResponse(3L)
                )
            coEvery { mockApi.getJournalEntries(1L, 1L) } returns mockResponses

            val repository =
                JournalEntriesRepositoryImpl(mockApi, mockJournalEntryDao, crashReporter, logger)

            // When
            val result = repository.refreshJournalEntries(1L, 1L)

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockApi.getJournalEntries(1L, 1L) }
            coVerify { mockJournalEntryDao.deleteByJournalId(1L) }
            coVerify { mockJournalEntryDao.insertAll(any<List<JournalEntryEntity>>()) }
        }

    @Test
    fun refreshJournalEntries_whenApiThrowsException_thenReturnsFailure() =
        runTest {
            // Given
            val exception = IOException("Network error")
            coEvery { mockApi.getJournalEntries(any(), any()) } throws exception

            val repository =
                JournalEntriesRepositoryImpl(mockApi, mockJournalEntryDao, crashReporter, logger)

            // When
            val result = repository.refreshJournalEntries(1L, 1L)

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
            coVerify { mockApi.getJournalEntries(1L, 1L) }
        }

    @Test
    fun refreshJournalEntries_whenApiReturnsEmptyList_thenClearsDatabase() =
        runTest {
            // Given
            val emptyResponses = emptyList<JournalEntryResponse>()
            coEvery { mockApi.getJournalEntries(1L, 1L) } returns emptyResponses

            val repository =
                JournalEntriesRepositoryImpl(mockApi, mockJournalEntryDao, crashReporter, logger)

            // When
            val result = repository.refreshJournalEntries(1L, 1L)

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockApi.getJournalEntries(1L, 1L) }
            coVerify { mockJournalEntryDao.deleteByJournalId(1L) }
            coVerify { mockJournalEntryDao.insertAll(emptyList<JournalEntryEntity>()) }
        }
}
