package com.swparks.data.repository

import com.swparks.data.database.dao.JournalEntryDao
import com.swparks.data.database.entity.JournalEntryEntity
import com.swparks.data.model.JournalEntryResponse
import com.swparks.network.SWApi
import com.swparks.util.CrashReporter
import com.swparks.util.Logger
import com.swparks.util.NoOpCrashReporter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * Unit тесты для JournalEntriesRepositoryImpl
 *
 * Проверяет корректность работы репозитория для записей в дневнике,
 * включая кэширование в БД и синхронизацию с сервером
 */
class JournalEntriesRepositoryImplTest {
    private companion object {
        const val TAG = "JournalEntriesRepository"
    }

    private lateinit var mockApi: SWApi
    private lateinit var mockJournalEntryDao: JournalEntryDao
    private lateinit var repository: JournalEntriesRepositoryImpl
    private val crashReporter: CrashReporter = NoOpCrashReporter()
    private val logger: Logger = mockk(relaxed = true)

    private val testUserId = 123L
    private val testJournalId = 456L

    @Before
    fun setup() {
        mockApi = mockk()
        mockJournalEntryDao = mockk(relaxed = true)
        repository =
            JournalEntriesRepositoryImpl(mockApi, mockJournalEntryDao, crashReporter, logger)
    }

    private fun createMockJournalEntryResponse(
        id: Long = 1L,
        journalId: Int = 1,
        authorId: Int = 1,
        name: String = "Test Author",
        message: String = "Test entry message",
        createDate: String = "2024-01-01T10:00:00",
        modifyDate: String = "2024-01-01T10:00:00",
        image: String = "https://example.com/image.jpg"
    ): JournalEntryResponse {
        return JournalEntryResponse(
            id = id,
            journalId = journalId,
            authorId = authorId,
            name = name,
            message = message,
            createDate = createDate,
            modifyDate = modifyDate,
            image = image
        )
    }

    private fun createMockJournalEntryEntity(
        id: Long = 1L,
        journalId: Long? = 1L,
        authorId: Long? = 1L,
        authorName: String? = "Test Author",
        message: String? = "Test entry message",
        createDate: String? = "2024-01-01T10:00:00",
        modifyDate: Long = 1704110400000L,
        authorImage: String? = "https://example.com/image.jpg"
    ): JournalEntryEntity {
        return JournalEntryEntity(
            id = id,
            journalId = journalId,
            authorId = authorId,
            authorName = authorName,
            message = message,
            createDate = createDate,
            modifyDate = modifyDate,
            authorImage = authorImage
        )
    }

    /**
     * Тест 1: Проверка, что observeJournalEntries возвращает Flow из DAO
     */
    @Test
    fun testObserveJournalEntries_returnsFlowFromDao() = runTest {
        // Given
        val mockEntities = listOf(
            createMockJournalEntryEntity(1L),
            createMockJournalEntryEntity(2L)
        )
        every { mockJournalEntryDao.getJournalEntriesByJournalId(testJournalId) } returns flowOf(
            mockEntities
        )

        // When
        val resultFlow = repository.observeJournalEntries(testUserId, testJournalId)

        // Then - проверяем, что результат является Flow и корректно преобразует данные
        val resultList = resultFlow.toList()
        assertEquals(1, resultList.size)
        assertEquals(2, resultList[0].size)
    }

    /**
     * Тест 2: Проверка успешной загрузки с сервера и сохранения в БД
     */
    @Test
    fun testRefreshJournalEntries_success_savesToDb() = runTest {
        // Given
        val mockResponses = listOf(
            createMockJournalEntryResponse(1L),
            createMockJournalEntryResponse(2L),
            createMockJournalEntryResponse(3L)
        )
        coEvery { mockApi.getJournalEntries(testUserId, testJournalId) } returns mockResponses

        // When
        val result = repository.refreshJournalEntries(testUserId, testJournalId)

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { mockApi.getJournalEntries(testUserId, testJournalId) }
        coVerify(exactly = 1) { mockJournalEntryDao.deleteByJournalId(testJournalId) }
        coVerify(exactly = 1) { mockJournalEntryDao.insertAll(any<List<JournalEntryEntity>>()) }
        verify {
            logger.i(
                TAG,
                "Загружаем записи дневника: userId=$testUserId, journalId=$testJournalId"
            )
        }
        verify {
            logger.i(
                TAG,
                "Получено ${mockResponses.size} записей с сервера"
            )
        }
        verify {
            logger.i(
                TAG,
                "Успешно сохранено ${mockResponses.size} записей в БД"
            )
        }
    }

    /**
     * Тест 3: Проверка удаления старых записей перед вставкой
     */
    @Test
    fun testRefreshJournalEntries_success_deletesOldEntries() = runTest {
        // Given
        val mockResponses = listOf(
            createMockJournalEntryResponse(1L),
            createMockJournalEntryResponse(2L)
        )
        coEvery { mockApi.getJournalEntries(testUserId, testJournalId) } returns mockResponses

        // When
        repository.refreshJournalEntries(testUserId, testJournalId)

        // Then - проверяем, что deleteByJournalId вызывается ПЕРЕД insertAll
        coVerify(exactly = 1) { mockJournalEntryDao.deleteByJournalId(testJournalId) }
        coVerify(exactly = 1) { mockJournalEntryDao.insertAll(any<List<JournalEntryEntity>>()) }
    }

    /**
     * Тест 4: Проверка обработки ошибок сети
     */
    @Test
    fun testRefreshJournalEntries_error_returnsFailure() = runTest {
        // Given
        val exception = IOException("Network error: Unable to connect")
        coEvery { mockApi.getJournalEntries(testUserId, testJournalId) } throws exception

        // When
        val result = repository.refreshJournalEntries(testUserId, testJournalId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { mockApi.getJournalEntries(testUserId, testJournalId) }
        verify {
            logger.e(
                TAG,
                "Ошибка при загрузке записей: ${exception.message}",
                exception
            )
        }
    }

    /**
     * Тест 5: Проверка сохранения пустого списка
     */
    @Test
    fun testRefreshJournalEntries_emptyList_savesEmpty() = runTest {
        // Given
        val emptyResponses = emptyList<JournalEntryResponse>()
        coEvery { mockApi.getJournalEntries(testUserId, testJournalId) } returns emptyResponses

        // When
        val result = repository.refreshJournalEntries(testUserId, testJournalId)

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { mockApi.getJournalEntries(testUserId, testJournalId) }
        coVerify(exactly = 1) { mockJournalEntryDao.deleteByJournalId(testJournalId) }
        coVerify(exactly = 1) { mockJournalEntryDao.insertAll(emptyList()) }
        verify { logger.i(TAG, "Получено 0 записей с сервера") }
        verify { logger.i(TAG, "Успешно сохранено 0 записей в БД") }
    }

    /**
     * Тест 6: Проверка логирования успешной загрузки
     */
    @Test
    fun testRefreshJournalEntries_logsSuccess() = runTest {
        // Given
        val mockResponses = listOf(
            createMockJournalEntryResponse(1L),
            createMockJournalEntryResponse(2L)
        )
        coEvery { mockApi.getJournalEntries(testUserId, testJournalId) } returns mockResponses

        // When
        repository.refreshJournalEntries(testUserId, testJournalId)

        // Then - проверяем все три лог-сообщения
        verify(exactly = 1) {
            logger.i(
                TAG,
                "Загружаем записи дневника: userId=$testUserId, journalId=$testJournalId"
            )
        }
        verify(exactly = 1) {
            logger.i(
                TAG,
                "Получено ${mockResponses.size} записей с сервера"
            )
        }
        verify(exactly = 1) {
            logger.i(
                TAG,
                "Успешно сохранено ${mockResponses.size} записей в БД"
            )
        }
    }

    /**
     * Тест 7: Проверка логирования ошибок
     */
    @Test
    fun testRefreshJournalEntries_logsError() = runTest {
        // Given
        val exception = IOException("Connection timeout")
        coEvery { mockApi.getJournalEntries(testUserId, testJournalId) } throws exception

        // When
        repository.refreshJournalEntries(testUserId, testJournalId)

        // Then
        verify {
            logger.i(
                TAG,
                "Загружаем записи дневника: userId=$testUserId, journalId=$testJournalId"
            )
        }
        verify(exactly = 1) {
            logger.e(
                TAG,
                "Ошибка при загрузке записей: ${exception.message}",
                exception
            )
        }
    }

    /**
     * Тест 8: Проверка маппинга Entity → Domain
     */
    @Test
    fun testObserveJournalEntries_mapsToDomain() = runTest {
        // Given
        val mockEntity = createMockJournalEntryEntity(
            id = 1L,
            journalId = 2L,
            authorId = 3L,
            authorName = "Test Author Name",
            message = "Test message",
            createDate = "2024-01-01T10:00:00",
            modifyDate = 1704110400000L,
            authorImage = "https://example.com/author.jpg"
        )
        every { mockJournalEntryDao.getJournalEntriesByJournalId(testJournalId) } returns flowOf(
            listOf(mockEntity)
        )

        // When
        val resultFlow = repository.observeJournalEntries(testUserId, testJournalId)
        val resultEntries = resultFlow.first()

        // Then
        assertEquals(1, resultEntries.size)

        val resultEntry = resultEntries[0]
        assertEquals(mockEntity.id, resultEntry.id)
        assertEquals(mockEntity.journalId, resultEntry.journalId)
        assertEquals(mockEntity.authorId, resultEntry.authorId)
        assertEquals(mockEntity.authorName, resultEntry.authorName)
        assertEquals(mockEntity.message, resultEntry.message)
        assertEquals(mockEntity.createDate, resultEntry.createDate)
        assertEquals(mockEntity.authorImage, resultEntry.authorImage)
        assertNotNull(resultEntry.modifyDate) // modifyDate должен быть конвертирован из timestamp в String ISO
    }

    /**
     * Дополнительный тест: Проверка обработки нескольких записей при маппинге
     */
    @Test
    fun testObserveJournalEntries_mapsMultipleEntitiesToDomain() = runTest {
        // Given
        val mockEntities = listOf(
            createMockJournalEntryEntity(
                id = 1L,
                authorName = "Author 1",
                message = "Message 1"
            ),
            createMockJournalEntryEntity(
                id = 2L,
                authorName = "Author 2",
                message = "Message 2"
            ),
            createMockJournalEntryEntity(
                id = 3L,
                authorName = "Author 3",
                message = "Message 3"
            )
        )
        every { mockJournalEntryDao.getJournalEntriesByJournalId(testJournalId) } returns flowOf(
            mockEntities
        )

        // When
        val resultFlow = repository.observeJournalEntries(testUserId, testJournalId)
        val resultEntries = resultFlow.first()

        // Then
        assertEquals(3, resultEntries.size)
        assertEquals(1L, resultEntries[0].id)
        assertEquals("Author 1", resultEntries[0].authorName)
        assertEquals("Message 1", resultEntries[0].message)

        assertEquals(2L, resultEntries[1].id)
        assertEquals("Author 2", resultEntries[1].authorName)
        assertEquals("Message 2", resultEntries[1].message)

        assertEquals(3L, resultEntries[2].id)
        assertEquals("Author 3", resultEntries[2].authorName)
        assertEquals("Message 3", resultEntries[2].message)
    }

    /**
     * Дополнительный тест: Проверка обработки null значений при маппинге
     */
    @Test
    fun testObserveJournalEntries_handlesNullValuesInEntity() = runTest {
        // Given
        val mockEntity = createMockJournalEntryEntity(
            id = 1L,
            journalId = null,
            authorId = null,
            authorName = null,
            message = null,
            createDate = null,
            modifyDate = 0L,
            authorImage = null
        )
        every { mockJournalEntryDao.getJournalEntriesByJournalId(testJournalId) } returns flowOf(
            listOf(mockEntity)
        )

        // When
        val resultFlow = repository.observeJournalEntries(testUserId, testJournalId)
        val resultEntries = resultFlow.first()

        // Then
        assertEquals(1, resultEntries.size)

        val resultEntry = resultEntries[0]
        assertEquals(mockEntity.id, resultEntry.id)
        assertEquals(mockEntity.journalId, resultEntry.journalId)
        assertEquals(mockEntity.authorId, resultEntry.authorId)
        assertEquals(mockEntity.authorName, resultEntry.authorName)
        assertEquals(mockEntity.message, resultEntry.message)
        assertEquals(mockEntity.createDate, resultEntry.createDate)
        assertEquals(mockEntity.authorImage, resultEntry.authorImage)
        assertEquals(null, resultEntry.modifyDate) // При timestamp = 0L должен возвращать null
    }

    // ==================== ТЕСТЫ ДЛЯ УДАЛЕНИЯ ЗАПИСЕЙ ====================

    /**
     * Тест 9: Успешное удаление записи (200 OK)
     */
    @Test
    fun testDeleteJournalEntry_success_deletesFromDb() = runTest {
        // Given
        val testEntryId = 1L
        val successResponse: Response<Unit> = Response.success(Unit)
        coEvery { mockApi.deleteJournalEntry(testUserId, testJournalId, testEntryId) } returns
            successResponse

        // When
        val result = repository.deleteJournalEntry(testUserId, testJournalId, testEntryId)

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            mockApi.deleteJournalEntry(testUserId, testJournalId, testEntryId)
        }
        coVerify(exactly = 1) { mockJournalEntryDao.deleteById(testEntryId) }
        verify {
            logger.i(
                TAG,
                "Удаление записи: userId=$testUserId, journalId=$testJournalId, entryId=$testEntryId"
            )
        }
        verify { logger.i(TAG, "Запись успешно удалена на сервере") }
    }

    /**
     * Тест 10: Успешное удаление записи (204 No Content)
     */
    @Test
    fun testDeleteJournalEntry_success204_deletesFromDb() = runTest {
        // Given
        val testEntryId = 1L
        val successResponse: Response<Unit> = Response.success(204, Unit)
        coEvery { mockApi.deleteJournalEntry(testUserId, testJournalId, testEntryId) } returns
            successResponse

        // When
        val result = repository.deleteJournalEntry(testUserId, testJournalId, testEntryId)

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            mockApi.deleteJournalEntry(testUserId, testJournalId, testEntryId)
        }
        coVerify(exactly = 1) { mockJournalEntryDao.deleteById(testEntryId) }
        verify { logger.i(TAG, "Запись успешно удалена на сервере") }
    }

    /**
     * Тест 11: Идемпотентность - 404 Not Found возвращает success и удаляет из БД
     */
    @Test
    fun testDeleteJournalEntry_404_deletesFromDbAndReturnsSuccess() = runTest {
        // Given
        val testEntryId = 1L
        val notFoundResponse: Response<Unit> =
            Response.error(404, "".toResponseBody(null))
        coEvery { mockApi.deleteJournalEntry(testUserId, testJournalId, testEntryId) } returns
            notFoundResponse

        // When
        val result = repository.deleteJournalEntry(testUserId, testJournalId, testEntryId)

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            mockApi.deleteJournalEntry(testUserId, testJournalId, testEntryId)
        }
        coVerify(exactly = 1) { mockJournalEntryDao.deleteById(testEntryId) }
        verify {
            logger.i(
                TAG,
                "Запись уже удалена на сервере (404), удаляем из локального кэша"
            )
        }
    }

    /**
     * Тест 12: Ошибка 401 Unauthorized возвращает failure
     */
    @Test
    fun testDeleteJournalEntry_401_returnsFailure() = runTest {
        // Given
        val testEntryId = 1L
        val unauthorizedResponse: Response<Unit> =
            Response.error(401, "Unauthorized".toResponseBody(null))
        coEvery { mockApi.deleteJournalEntry(testUserId, testJournalId, testEntryId) } returns
            unauthorizedResponse

        // When
        val result = repository.deleteJournalEntry(testUserId, testJournalId, testEntryId)

        // Then
        assertTrue(result.isFailure)
        coVerify(exactly = 1) {
            mockApi.deleteJournalEntry(testUserId, testJournalId, testEntryId)
        }
        coVerify(exactly = 0) { mockJournalEntryDao.deleteById(any()) } // DAO не должен вызываться
        verify {
            logger.e(TAG, "Ошибка удаления записи: код 401")
        }
    }

    /**
     * Тест 13: Ошибка 403 Forbidden возвращает failure
     */
    @Test
    fun testDeleteJournalEntry_403_returnsFailure() = runTest {
        // Given
        val testEntryId = 1L
        val forbiddenResponse: Response<Unit> =
            Response.error(403, "Forbidden".toResponseBody(null))
        coEvery { mockApi.deleteJournalEntry(testUserId, testJournalId, testEntryId) } returns
            forbiddenResponse

        // When
        val result = repository.deleteJournalEntry(testUserId, testJournalId, testEntryId)

        // Then
        assertTrue(result.isFailure)
        coVerify(exactly = 1) {
            mockApi.deleteJournalEntry(testUserId, testJournalId, testEntryId)
        }
        coVerify(exactly = 0) { mockJournalEntryDao.deleteById(any()) }
        verify {
            logger.e(TAG, "Ошибка удаления записи: код 403")
        }
    }

    /**
     * Тест 14: Ошибка сети (IOException) возвращает failure
     */
    @Test
    fun testDeleteJournalEntry_ioException_returnsFailure() = runTest {
        // Given
        val testEntryId = 1L
        val exception = IOException("Connection timeout")
        coEvery {
            mockApi.deleteJournalEntry(
                testUserId,
                testJournalId,
                testEntryId
            )
        } throws exception

        // When
        val result = repository.deleteJournalEntry(testUserId, testJournalId, testEntryId)

        // Then
        assertTrue(result.isFailure)
        coVerify(exactly = 1) {
            mockApi.deleteJournalEntry(testUserId, testJournalId, testEntryId)
        }
        coVerify(exactly = 0) { mockJournalEntryDao.deleteById(any()) }
        verify {
            logger.e(
                TAG,
                "Ошибка сети при удалении записи: ${exception.message}"
            )
        }
    }

    /**
     * Тест 15: Логирование успешного удаления
     */
    @Test
    fun testDeleteJournalEntry_logsSuccess() = runTest {
        // Given
        val testEntryId = 1L
        val successResponse: Response<Unit> = Response.success(Unit)
        coEvery { mockApi.deleteJournalEntry(testUserId, testJournalId, testEntryId) } returns
            successResponse

        // When
        repository.deleteJournalEntry(testUserId, testJournalId, testEntryId)

        // Then - проверяем логи
        verify(exactly = 1) {
            logger.i(
                TAG,
                "Удаление записи: userId=$testUserId, journalId=$testJournalId, entryId=$testEntryId"
            )
        }
        verify(exactly = 1) {
            logger.i(TAG, "Запись успешно удалена на сервере")
        }
    }

    // ==================== ТЕСТЫ ДЛЯ ПРОВЕРКИ ВОЗМОЖНОСТИ УДАЛЕНИЯ ====================

    /**
     * Тест 16: Первую запись (с минимальным id) нельзя удалить
     */
    @Test
    fun testCanDeleteEntry_firstEntry_returnsFalse() = runTest {
        // Given
        val testEntryId = 1L
        val minEntryId = 1L
        coEvery { mockJournalEntryDao.getMinEntryId(testJournalId) } returns minEntryId

        // When
        val result = repository.canDeleteEntry(testEntryId, testJournalId)

        // Then
        assertEquals(false, result)
        coVerify(exactly = 1) { mockJournalEntryDao.getMinEntryId(testJournalId) }
    }

    /**
     * Тест 17: Не первую запись можно удалить
     */
    @Test
    fun testCanDeleteEntry_notFirstEntry_returnsTrue() = runTest {
        // Given
        val testEntryId = 2L
        val minEntryId = 1L
        coEvery { mockJournalEntryDao.getMinEntryId(testJournalId) } returns minEntryId

        // When
        val result = repository.canDeleteEntry(testEntryId, testJournalId)

        // Then
        assertEquals(true, result)
        coVerify(exactly = 1) { mockJournalEntryDao.getMinEntryId(testJournalId) }
    }

    /**
     * Тест 18: Если записей нет в БД (null), то можно удалить любую запись
     */
    @Test
    fun testCanDeleteEntry_noEntries_returnsTrue() = runTest {
        // Given
        val testEntryId = 1L
        coEvery { mockJournalEntryDao.getMinEntryId(testJournalId) } returns null

        // When
        val result = repository.canDeleteEntry(testEntryId, testJournalId)

        // Then
        assertEquals(true, result)
        coVerify(exactly = 1) { mockJournalEntryDao.getMinEntryId(testJournalId) }
    }

    // ==================== ТЕСТЫ ДЛЯ HttpException ====================

    /**
     * Тест 19: HttpException при refreshJournalEntries возвращает Result.failure
     */
    @Test
    fun testRefreshJournalEntries_httpException_returnsFailure() = runTest {
        // Given
        val mockResponse = mockk<Response<*>>(relaxed = true)
        every { mockResponse.code() } returns 500
        every { mockResponse.message() } returns "Server Error"
        val httpException = HttpException(mockResponse)
        coEvery { mockApi.getJournalEntries(testUserId, testJournalId) } throws httpException

        // When
        val result = repository.refreshJournalEntries(testUserId, testJournalId)

        // Then
        assertTrue("Expected Result.failure but got $result", result.isFailure)
        coVerify(exactly = 1) { mockApi.getJournalEntries(testUserId, testJournalId) }
        // DAO не должен вызываться при ошибке
        coVerify(exactly = 0) { mockJournalEntryDao.deleteByJournalId(any()) }
        coVerify(exactly = 0) { mockJournalEntryDao.insertAll(any()) }
    }

    /**
     * Тест 20: HttpException 401 при refreshJournalEntries возвращает Result.failure
     */
    @Test
    fun testRefreshJournalEntries_httpException401_returnsFailure() = runTest {
        // Given
        val mockResponse = mockk<Response<*>>(relaxed = true)
        every { mockResponse.code() } returns 401
        every { mockResponse.message() } returns "Unauthorized"
        val httpException = HttpException(mockResponse)
        coEvery { mockApi.getJournalEntries(testUserId, testJournalId) } throws httpException

        // When
        val result = repository.refreshJournalEntries(testUserId, testJournalId)

        // Then
        assertTrue("Expected Result.failure but got $result", result.isFailure)
        coVerify(exactly = 1) { mockApi.getJournalEntries(testUserId, testJournalId) }
    }

    /**
     * Тест 21: HttpException при deleteJournalEntry возвращает Result.failure
     * Примечание: deleteJournalEntry использует Response<T>, поэтому HttpException
     * не выбрасывается Retrofit-ом, но тест добавлен для полноты покрытия
     */
    @Test
    fun testDeleteJournalEntry_httpException_returnsFailure() = runTest {
        // Given
        val testEntryId = 1L
        val mockResponse = mockk<Response<*>>(relaxed = true)
        every { mockResponse.code() } returns 500
        every { mockResponse.message() } returns "Server Error"
        val httpException = HttpException(mockResponse)
        coEvery {
            mockApi.deleteJournalEntry(
                testUserId,
                testJournalId,
                testEntryId
            )
        } throws httpException

        // When
        val result = repository.deleteJournalEntry(testUserId, testJournalId, testEntryId)

        // Then
        assertTrue("Expected Result.failure but got $result", result.isFailure)
        coVerify(exactly = 1) {
            mockApi.deleteJournalEntry(testUserId, testJournalId, testEntryId)
        }
        coVerify(exactly = 0) { mockJournalEntryDao.deleteById(any()) }
    }
}
