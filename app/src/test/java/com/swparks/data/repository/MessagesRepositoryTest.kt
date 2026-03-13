package com.swparks.data.repository

import android.util.Log
import com.swparks.data.database.dao.DialogDao
import com.swparks.data.database.entity.DialogEntity
import com.swparks.data.model.DialogResponse
import com.swparks.network.SWApi
import com.swparks.util.Logger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * Unit тесты для MessagesRepositoryImpl
 *
 * Проверяет корректность работы репозитория для диалогов,
 * включая кэширование в БД и синхронизацию с сервером
 */
class MessagesRepositoryTest {

    private lateinit var mockDialogDao: DialogDao
    private lateinit var mockApi: SWApi
    private lateinit var mockLogger: Logger
    private lateinit var repository: MessagesRepositoryImpl

    @Before
    fun setup() {
        mockDialogDao = mockk(relaxed = true)
        mockApi = mockk()
        mockLogger = mockk(relaxed = true)

        repository = MessagesRepositoryImpl(
            dialogsDao = mockDialogDao,
            swApi = mockApi,
            logger = mockLogger
        )

        // Мокаем статический класс Log для тестов
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createMockDialogResponse(
        id: Long = 1L,
        anotherUserId: Int? = 123,
        name: String? = "Тестовый пользователь",
        image: String? = "https://example.com/avatar.png",
        lastMessageText: String? = "Привет!",
        lastMessageDate: String? = "2024-01-15 12:30",
        count: Int? = 2
    ): DialogResponse {
        return DialogResponse(
            id = id,
            anotherUserId = anotherUserId,
            name = name,
            image = image,
            lastMessageText = lastMessageText,
            lastMessageDate = lastMessageDate,
            count = count
        )
    }

    private fun createMockDialogEntity(
        id: Long = 1L,
        anotherUserId: Int? = 123,
        name: String? = "Тестовый пользователь",
        image: String? = "https://example.com/avatar.png",
        lastMessageText: String? = "Привет!",
        lastMessageDate: String? = "2024-01-15 12:30",
        unreadCount: Int? = 2
    ): DialogEntity {
        return DialogEntity(
            id = id,
            anotherUserId = anotherUserId,
            name = name,
            image = image,
            lastMessageText = lastMessageText,
            lastMessageDate = lastMessageDate,
            unreadCount = unreadCount
        )
    }

    @Test
    fun dialogs_returnsFlowFromDao() = runTest {
        // Given
        val expectedDialogs = listOf(
            createMockDialogEntity(id = 1L),
            createMockDialogEntity(id = 2L)
        )
        coEvery { mockDialogDao.getDialogsFlow() } returns flowOf(expectedDialogs)

        // When - создаём репозиторий после настройки мока
        val testRepository = MessagesRepositoryImpl(
            dialogsDao = mockDialogDao,
            swApi = mockApi,
            logger = mockLogger
        )
        val result = testRepository.dialogs.first()

        // Then
        assertEquals(2, result.size)
        assertEquals(1L, result[0].id)
        assertEquals(2L, result[1].id)
    }

    @Test
    fun refreshDialogs_onSuccess_updatesDao() = runTest {
        // Given
        val remoteDialogs = listOf(
            createMockDialogResponse(id = 1L),
            createMockDialogResponse(id = 2L)
        )
        coEvery { mockApi.getDialogs() } returns remoteDialogs
        coEvery { mockDialogDao.deleteAll() } returns Unit
        coEvery { mockDialogDao.insertAll(any<List<DialogEntity>>()) } returns Unit

        // When
        val result = repository.refreshDialogs()

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockDialogDao.deleteAll() }
        coVerify { mockDialogDao.insertAll(any<List<DialogEntity>>()) }
    }

    @Test
    fun refreshDialogs_onFailure_returnsError() = runTest {
        // Given
        val exception = IOException("Network error")
        coEvery { mockApi.getDialogs() } throws exception

        // When
        val result = repository.refreshDialogs()

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun refreshDialogs_onSuccess_mapsResponseToEntity() = runTest {
        // Given
        val remoteDialogs = listOf(
            createMockDialogResponse(
                id = 1L,
                anotherUserId = 456,
                name = "Тест",
                count = 5
            )
        )
        coEvery { mockApi.getDialogs() } returns remoteDialogs
        coEvery { mockDialogDao.deleteAll() } returns Unit
        coEvery { mockDialogDao.insertAll(any<List<DialogEntity>>()) } returns Unit

        // When
        val result = repository.refreshDialogs()

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            mockDialogDao.insertAll(match { entities ->
                entities.size == 1 &&
                    entities[0].id == 1L &&
                    entities[0].anotherUserId == 456 &&
                    entities[0].name == "Тест" &&
                    entities[0].unreadCount == 5
            })
        }
    }

    @Test
    fun refreshDialogs_onEmptyList_clearsAndInsertsEmpty() = runTest {
        // Given
        coEvery { mockApi.getDialogs() } returns emptyList()
        coEvery { mockDialogDao.deleteAll() } returns Unit
        coEvery { mockDialogDao.insertAll(any<List<DialogEntity>>()) } returns Unit

        // When
        val result = repository.refreshDialogs()

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockDialogDao.deleteAll() }
        coVerify { mockDialogDao.insertAll(emptyList()) }
    }

    // ==================== HttpException Tests ====================

    @Test
    fun refreshDialogs_onHttpException_returnsFailure() = runTest {
        // Given
        val mockResponse = mockk<Response<*>>(relaxed = true)
        every { mockResponse.code() } returns 500
        every { mockResponse.message() } returns "Server Error"
        val httpException = HttpException(mockResponse)
        coEvery { mockApi.getDialogs() } throws httpException

        // When
        val result = repository.refreshDialogs()

        // Then
        assertTrue("Expected Result.failure but got $result", result.isFailure)
        // DAO не должен вызываться при ошибке
        coVerify(exactly = 0) { mockDialogDao.deleteAll() }
        coVerify(exactly = 0) { mockDialogDao.insertAll(any()) }
    }

    @Test
    fun refreshDialogs_onHttpException401_returnsFailure() = runTest {
        // Given
        val mockResponse = mockk<Response<*>>(relaxed = true)
        every { mockResponse.code() } returns 401
        every { mockResponse.message() } returns "Unauthorized"
        val httpException = HttpException(mockResponse)
        coEvery { mockApi.getDialogs() } throws httpException

        // When
        val result = repository.refreshDialogs()

        // Then
        assertTrue("Expected Result.failure but got $result", result.isFailure)
    }
}
