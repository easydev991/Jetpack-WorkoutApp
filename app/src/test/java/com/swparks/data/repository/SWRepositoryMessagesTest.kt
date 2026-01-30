package com.swparks.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.swparks.data.database.UserDao
import com.swparks.domain.exception.NetworkException
import com.swparks.model.DialogResponse
import com.swparks.model.MessageResponse
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
 * Unit тесты для методов сообщений в SWRepository
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SWRepositoryMessagesTest {
    private val testDispatcher = StandardTestDispatcher()
    private val mockUserDao = mockk<UserDao>(relaxed = true)

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

    private fun createMockDialog(id: Long = 1L): DialogResponse {
        return DialogResponse(
            id = id,
            anotherUserId = 2,
            name = "Test Dialog",
            image = null,
            lastMessageText = "Last message",
            lastMessageDate = "2024-01-01",
            count = null
        )
    }

    private fun createMockMessage(id: Long = 1L): MessageResponse {
        return MessageResponse(
            id = id,
            userId = 2,
            message = "Test message",
            name = null,
            created = "2024-01-01"
        )
    }

    @Test
    fun getDialogs_whenApiReturnsDialogs_thenReturnsDialogs() = runTest {
        // Given
        val mockDialogsList = listOf(
            createMockDialog(1L),
            createMockDialog(2L),
            createMockDialog(3L)
        )
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getDialogs() } returns mockDialogsList

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore, mockUserDao)

        // When
        val result = repository.getDialogs()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockDialogsList, result.getOrNull())
        coVerify { mockApi.getDialogs() }
    }

    @Test
    fun getDialogs_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getDialogs() } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore, mockUserDao)

        // When
        val result = repository.getDialogs()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun getMessages_whenApiReturnsMessages_thenReturnsMessages() = runTest {
        // Given
        val mockMessagesList = listOf(
            createMockMessage(1L),
            createMockMessage(2L)
        )
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getMessages(1L) } returns mockMessagesList

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore, mockUserDao)

        // When
        val result = repository.getMessages(1L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockMessagesList, result.getOrNull())
        coVerify { mockApi.getMessages(1L) }
    }

    @Test
    fun getMessages_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getMessages(any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore, mockUserDao)

        // When
        val result = repository.getMessages(1L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun sendMessage_whenApiReturnsSuccess_thenReturnsSuccess() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.sendMessageTo(2L, "Hello") } returns mockk(relaxed = true)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore, mockUserDao)

        // When
        val result = repository.sendMessage("Hello", 2L)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.sendMessageTo(2L, "Hello") }
    }

    @Test
    fun sendMessage_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.sendMessageTo(any(), any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore, mockUserDao)

        // When
        val result = repository.sendMessage("Hello", 2L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun markAsRead_whenApiReturnsSuccess_thenReturnsSuccess() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.markAsRead(any()) } returns mockk(relaxed = true)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore, mockUserDao)

        // When
        val result = repository.markAsRead(2L)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.markAsRead(2L) }
    }

    @Test
    fun markAsRead_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.markAsRead(any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore, mockUserDao)

        // When
        val result = repository.markAsRead(2L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun deleteDialog_whenApiReturnsSuccess_thenReturnsSuccess() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.deleteDialog(1L) } returns mockk(relaxed = true)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore, mockUserDao)

        // When
        val result = repository.deleteDialog(1L)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.deleteDialog(1L) }
    }

    @Test
    fun deleteDialog_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.deleteDialog(any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore, mockUserDao)

        // When
        val result = repository.deleteDialog(1L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }
}
