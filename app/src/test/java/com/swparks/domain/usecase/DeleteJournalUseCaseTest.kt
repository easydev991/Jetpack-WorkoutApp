package com.swparks.domain.usecase

import com.swparks.data.repository.SWRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit тесты для DeleteJournalUseCase
 *
 * Проверяет корректность работы use case для удаления дневников,
 * включая прокидывание результатов из репозитория без изменений
 */
class DeleteJournalUseCaseTest {

    private lateinit var mockRepository: SWRepository
    private lateinit var deleteJournalUseCase: DeleteJournalUseCase

    private val testUserId = 123L
    private val testJournalId = 456L

    @Before
    fun setup() {
        mockRepository = mockk()
        deleteJournalUseCase = DeleteJournalUseCase(mockRepository)
    }

    /**
     * Тест 1: Успешное удаление дневника
     */
    @Test
    fun testInvoke_success_returnsSuccess() = runTest {
        // Given
        coEvery {
            mockRepository.deleteJournal(testJournalId, testUserId)
        } returns Result.success(Unit)

        // When
        val result = deleteJournalUseCase(testUserId, testJournalId)

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            mockRepository.deleteJournal(testJournalId, testUserId)
        }
    }

    /**
     * Тест 2: Ошибка при удалении дневника
     */
    @Test
    fun testInvoke_failure_returnsFailure() = runTest {
        // Given
        val exception = RuntimeException("Ошибка удаления")
        coEvery {
            mockRepository.deleteJournal(testJournalId, testUserId)
        } returns Result.failure(exception)

        // When
        val result = deleteJournalUseCase(testUserId, testJournalId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) {
            mockRepository.deleteJournal(testJournalId, testUserId)
        }
    }

    /**
     * Тест 3: Проверка передачи правильных параметров в репозиторий
     */
    @Test
    fun testInvoke_passesCorrectParameters() = runTest {
        // Given
        coEvery {
            mockRepository.deleteJournal(testJournalId, testUserId)
        } returns Result.success(Unit)

        // When
        deleteJournalUseCase(testUserId, testJournalId)

        // Then - проверяем, что параметры переданы правильно
        coVerify(exactly = 1) {
            mockRepository.deleteJournal(
                journalId = testJournalId,
                userId = testUserId
            )
        }
    }

    /**
     * Тест 4: Проверка для разных userId
     */
    @Test
    fun testInvoke_differentUserId() = runTest {
        // Given
        val differentUserId = 999L
        coEvery {
            mockRepository.deleteJournal(testJournalId, differentUserId)
        } returns Result.success(Unit)

        // When
        val result = deleteJournalUseCase(differentUserId, testJournalId)

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            mockRepository.deleteJournal(testJournalId, differentUserId)
        }
    }

    /**
     * Тест 5: Проверка для разных journalId
     */
    @Test
    fun testInvoke_differentJournalId() = runTest {
        // Given
        val differentJournalId = 111L
        coEvery {
            mockRepository.deleteJournal(differentJournalId, testUserId)
        } returns Result.success(Unit)

        // When
        val result = deleteJournalUseCase(testUserId, differentJournalId)

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            mockRepository.deleteJournal(differentJournalId, testUserId)
        }
    }
}
