package com.swparks.data.repository

import android.util.Log
import com.swparks.data.database.dao.DialogDao
import com.swparks.data.model.MessageResponse
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
 * Unit тесты для методов сообщений в MessagesRepositoryImpl
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MessagesRepositoryImplTest {
    private val testDispatcher = StandardTestDispatcher()
    private val mockDialogDao = mockk<DialogDao>(relaxed = true)
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

    private fun createMockMessage(id: Long = 1L): MessageResponse =
        MessageResponse(
            id = id,
            userId = 2,
            message = "Test message",
            name = null,
            created = "2024-01-01"
        )

    @Test
    fun getMessages_whenApiThrowsException_thenReturnsFailure() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getMessages(any()) } throws IOException("Network error")

            val repository =
                MessagesRepositoryImpl(
                    dialogsDao = null,
                    swApi = mockApi,
                    logger = logger,
                    crashReporter = crashReporter
                )

            // When
            val result = repository.getMessages(1L)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NetworkException)
        }

    @Test
    fun getMessages_whenApiReturnsMessages_thenReturnsMessages() =
        runTest {
            // Given
            val mockMessagesList =
                listOf(
                    createMockMessage(1L),
                    createMockMessage(2L)
                )
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getMessages(1L) } returns mockMessagesList

            val repository =
                MessagesRepositoryImpl(
                    dialogsDao = null,
                    swApi = mockApi,
                    logger = logger,
                    crashReporter = crashReporter
                )

            // When
            val result = repository.getMessages(1L)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(mockMessagesList, result.getOrNull())
            coVerify { mockApi.getMessages(1L) }
        }

    @Test
    fun sendMessage_whenApiReturnsSuccess_thenReturnsSuccess() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.sendMessageTo(2L, "Hello") } returns Response.success(Unit)

            val repository =
                MessagesRepositoryImpl(
                    dialogsDao = null,
                    swApi = mockApi,
                    logger = logger,
                    crashReporter = crashReporter
                )

            // When
            val result = repository.sendMessage("Hello", 2L)

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockApi.sendMessageTo(2L, "Hello") }
        }

    @Test
    fun sendMessage_whenApiThrowsException_thenReturnsFailure() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.sendMessageTo(any(), any()) } throws IOException("Network error")

            val repository =
                MessagesRepositoryImpl(
                    dialogsDao = null,
                    swApi = mockApi,
                    logger = logger,
                    crashReporter = crashReporter
                )

            // When
            val result = repository.sendMessage("Hello", 2L)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NetworkException)
        }

    @Test
    fun markAsRead_whenApiReturnsSuccess_thenReturnsSuccess() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.markAsRead(any()) } returns Response.success(Unit)

            val repository =
                MessagesRepositoryImpl(
                    dialogsDao = null,
                    swApi = mockApi,
                    logger = logger,
                    crashReporter = crashReporter
                )

            // When
            val result = repository.markAsRead(2L)

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockApi.markAsRead(2L) }
        }

    @Test
    fun markAsRead_whenApiThrowsException_thenReturnsFailure() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.markAsRead(any()) } throws IOException("Network error")

            val repository =
                MessagesRepositoryImpl(
                    dialogsDao = null,
                    swApi = mockApi,
                    logger = logger,
                    crashReporter = crashReporter
                )

            // When
            val result = repository.markAsRead(2L)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NetworkException)
        }

    @Test
    fun deleteDialog_whenApiReturnsSuccess_thenReturnsSuccess() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.deleteDialog(1L) } returns Response.success(Unit)

            val repository =
                MessagesRepositoryImpl(
                    dialogsDao = null,
                    swApi = mockApi,
                    logger = logger,
                    crashReporter = crashReporter
                )

            // When
            val result = repository.deleteDialog(1L)

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockApi.deleteDialog(1L) }
        }

    @Test
    fun deleteDialog_whenApiThrowsException_thenReturnsFailure() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.deleteDialog(any()) } throws IOException("Network error")

            val repository =
                MessagesRepositoryImpl(
                    dialogsDao = null,
                    swApi = mockApi,
                    logger = logger,
                    crashReporter = crashReporter
                )

            // When
            val result = repository.deleteDialog(1L)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NetworkException)
        }

    @Test
    fun deleteDialog_whenApiThrowsIllegalStateException_thenReturnsFailure() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.deleteDialog(any()) } throws IllegalStateException("closed")

            val repository =
                MessagesRepositoryImpl(
                    dialogsDao = null,
                    swApi = mockApi,
                    logger = logger,
                    crashReporter = crashReporter
                )

            // When
            val result = repository.deleteDialog(1L)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NetworkException)
        }

    // ==================== Тесты для markDialogAsRead ====================

    @Test
    fun markDialogAsRead_whenApiReturnsSuccess_thenReturnsSuccess() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.markAsRead(2L) } returns Response.success(Unit)

            val repository =
                MessagesRepositoryImpl(
                    dialogsDao = mockDialogDao,
                    swApi = mockApi,
                    logger = logger,
                    crashReporter = crashReporter
                )

            // When
            val result = repository.markDialogAsRead(dialogId = 1L, userId = 2)

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockApi.markAsRead(2L) }
            coVerify { mockDialogDao.updateUnreadCount(1L) }
        }

    @Test
    fun markDialogAsRead_whenApiThrowsIOException_thenReturnsFailure() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.markAsRead(any()) } throws IOException("Network error")

            val repository =
                MessagesRepositoryImpl(
                    dialogsDao = mockDialogDao,
                    swApi = mockApi,
                    logger = logger,
                    crashReporter = crashReporter
                )

            // When
            val result = repository.markDialogAsRead(dialogId = 1L, userId = 2)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NetworkException)
        }

    @Test
    fun markDialogAsRead_whenApiThrowsIllegalStateException_thenReturnsFailure() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.markAsRead(any()) } throws IllegalStateException("closed")

            val repository =
                MessagesRepositoryImpl(
                    dialogsDao = mockDialogDao,
                    swApi = mockApi,
                    logger = logger,
                    crashReporter = crashReporter
                )

            // When
            val result = repository.markDialogAsRead(dialogId = 1L, userId = 2)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NetworkException)
        }
}
