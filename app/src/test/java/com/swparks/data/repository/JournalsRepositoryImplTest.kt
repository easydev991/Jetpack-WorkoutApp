package com.swparks.data.repository

import android.util.Log
import com.swparks.data.database.dao.JournalDao
import com.swparks.data.database.entity.JournalEntity
import com.swparks.data.model.JournalResponse
import com.swparks.network.SWApi
import com.swparks.util.NoOpCrashReporter
import com.swparks.util.NoOpLogger
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
 * Unit тесты для JournalsRepositoryImpl
 *
 * Проверяет корректность работы репозитория для дневников,
 * включая кэширование в БД и синхронизацию с сервером
 */
class JournalsRepositoryImplTest {

    private lateinit var mockApi: SWApi
    private lateinit var mockJournalDao: JournalDao
    private lateinit var repository: JournalsRepositoryImpl
    private val crashReporter = NoOpCrashReporter()
    private val logger = NoOpLogger()

    private val testUserId = 123L

    @Before
    fun setup() {
        mockApi = mockk()
        mockJournalDao = mockk(relaxed = true)
        repository = JournalsRepositoryImpl(mockApi, mockJournalDao, crashReporter, logger)

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

    private fun createMockJournalResponse(
        id: Long = 1L,
        title: String? = "Test Journal",
        ownerId: Int? = 1,
        viewAccess: Int? = 0,
        commentAccess: Int? = 0,
        count: Int? = 0,
        lastMessageText: String? = null,
        lastMessageImage: String? = null,
        lastMessageDate: String? = null,
        createDate: String? = null,
        modifyDate: String? = null
    ): JournalResponse {
        return JournalResponse(
            id = id,
            title = title,
            lastMessageImage = lastMessageImage,
            createDate = createDate,
            modifyDate = modifyDate,
            lastMessageDate = lastMessageDate,
            lastMessageText = lastMessageText,
            count = count,
            ownerId = ownerId,
            viewAccess = viewAccess,
            commentAccess = commentAccess
        )
    }

    private fun createMockJournalEntity(
        id: Long = 1L,
        title: String? = "Test Journal",
        ownerId: Long? = 1L,
        viewAccess: Int? = 0,
        commentAccess: Int? = 0,
        entriesCount: Int? = 0,
        lastMessageText: String? = null,
        lastMessageImage: String? = null,
        lastMessageDate: String? = null,
        createDate: String? = null,
        modifyDate: Long = 0L
    ): JournalEntity {
        return JournalEntity(
            id = id,
            title = title,
            lastMessageImage = lastMessageImage,
            createDate = createDate,
            modifyDate = modifyDate,
            lastMessageDate = lastMessageDate,
            lastMessageText = lastMessageText,
            entriesCount = entriesCount,
            ownerId = ownerId,
            viewAccess = viewAccess,
            commentAccess = commentAccess
        )
    }

    @Test
    fun observeJournals_returnsFlowFromDao() = runTest {
        // Given
        val mockEntities = listOf(
            createMockJournalEntity(id = 1L),
            createMockJournalEntity(id = 2L)
        )
        every { mockJournalDao.getJournalsByUserId(testUserId) } returns flowOf(mockEntities)

        // When
        val resultFlow = repository.observeJournals(testUserId)
        val result = resultFlow.first()

        // Then
        assertEquals(2, result.size)
        assertEquals(1L, result[0].id)
        assertEquals(2L, result[1].id)
    }

    @Test
    fun refreshJournals_onSuccess_savesToDb() = runTest {
        // Given
        val mockResponses = listOf(
            createMockJournalResponse(id = 1L),
            createMockJournalResponse(id = 2L)
        )
        coEvery { mockApi.getJournals(testUserId) } returns mockResponses
        coEvery { mockJournalDao.deleteByUserId(testUserId) } returns Unit
        coEvery { mockJournalDao.insertAll(any<List<JournalEntity>>()) } returns Unit

        // When
        val result = repository.refreshJournals(testUserId)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.getJournals(testUserId) }
        coVerify { mockJournalDao.deleteByUserId(testUserId) }
        coVerify { mockJournalDao.insertAll(any<List<JournalEntity>>()) }
    }

    @Test
    fun refreshJournals_onIOException_returnsFailure() = runTest {
        // Given
        val exception = IOException("Network error")
        coEvery { mockApi.getJournals(testUserId) } throws exception

        // When
        val result = repository.refreshJournals(testUserId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun refreshJournals_onEmptyList_savesEmpty() = runTest {
        // Given
        coEvery { mockApi.getJournals(testUserId) } returns emptyList()
        coEvery { mockJournalDao.deleteByUserId(testUserId) } returns Unit
        coEvery { mockJournalDao.insertAll(any<List<JournalEntity>>()) } returns Unit

        // When
        val result = repository.refreshJournals(testUserId)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockJournalDao.deleteByUserId(testUserId) }
        coVerify { mockJournalDao.insertAll(emptyList()) }
    }

    // ==================== HttpException Tests ====================

    @Test
    fun refreshJournals_onHttpException_returnsFailure() = runTest {
        // Given
        val mockResponse = mockk<Response<*>>(relaxed = true)
        every { mockResponse.code() } returns 500
        every { mockResponse.message() } returns "Server Error"
        val httpException = HttpException(mockResponse)
        coEvery { mockApi.getJournals(testUserId) } throws httpException

        // When
        val result = repository.refreshJournals(testUserId)

        // Then
        assertTrue("Expected Result.failure but got $result", result.isFailure)
        // DAO не должен вызываться при ошибке
        coVerify(exactly = 0) { mockJournalDao.deleteByUserId(any()) }
        coVerify(exactly = 0) { mockJournalDao.insertAll(any()) }
    }

    @Test
    fun refreshJournals_onHttpException401_returnsFailure() = runTest {
        // Given
        val mockResponse = mockk<Response<*>>(relaxed = true)
        every { mockResponse.code() } returns 401
        every { mockResponse.message() } returns "Unauthorized"
        val httpException = HttpException(mockResponse)
        coEvery { mockApi.getJournals(testUserId) } throws httpException

        // When
        val result = repository.refreshJournals(testUserId)

        // Then
        assertTrue("Expected Result.failure but got $result", result.isFailure)
    }
}
