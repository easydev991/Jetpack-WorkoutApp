package com.swparks.domain.usecase

import com.swparks.domain.repository.JournalEntriesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit тесты для DeleteJournalEntryUseCase
 *
 * Проверяет корректность работы use case для удаления записей из дневника,
 * включая прокидывание результатов из репозитория без изменений
 */
class DeleteJournalEntryUseCaseTest {
    private lateinit var mockRepository: JournalEntriesRepository
    private lateinit var deleteJournalEntryUseCase: DeleteJournalEntryUseCase

    private val testUserId = 123L
    private val testJournalId = 456L
    private val testEntryId = 789L

    @Before
    fun setup() {
        mockRepository = mockk()
        deleteJournalEntryUseCase = DeleteJournalEntryUseCase(mockRepository)
    }

    /**
     * Тест 1: Успешное удаление записи
     */
    @Test
    fun testInvoke_success_returnsSuccess() =
        runTest {
            // Given
            coEvery {
                mockRepository.deleteJournalEntry(testUserId, testJournalId, testEntryId)
            } returns Result.success(Unit)

            // When
            val result = deleteJournalEntryUseCase(testUserId, testJournalId, testEntryId)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                mockRepository.deleteJournalEntry(testUserId, testJournalId, testEntryId)
            }
        }

    /**
     * Тест 2: Ошибка при удалении записи
     */
    @Test
    fun testInvoke_failure_returnsFailure() =
        runTest {
            // Given
            val exception = RuntimeException("Ошибка удаления")
            coEvery {
                mockRepository.deleteJournalEntry(testUserId, testJournalId, testEntryId)
            } returns Result.failure(exception)

            // When
            val result = deleteJournalEntryUseCase(testUserId, testJournalId, testEntryId)

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
            coVerify(exactly = 1) {
                mockRepository.deleteJournalEntry(testUserId, testJournalId, testEntryId)
            }
        }

    /**
     * Тест 3: Проверка передачи правильных параметров в репозиторий
     */
    @Test
    fun testInvoke_passesCorrectParameters() =
        runTest {
            // Given
            coEvery {
                mockRepository.deleteJournalEntry(testUserId, testJournalId, testEntryId)
            } returns Result.success(Unit)

            // When
            deleteJournalEntryUseCase(testUserId, testJournalId, testEntryId)

            // Then - проверяем, что параметры переданы правильно
            coVerify(exactly = 1) {
                mockRepository.deleteJournalEntry(
                    userId = testUserId,
                    journalId = testJournalId,
                    entryId = testEntryId
                )
            }
        }

    /**
     * Тест 4: Проверка для разных userId
     */
    @Test
    fun testInvoke_differentUserId() =
        runTest {
            // Given
            val differentUserId = 999L
            coEvery {
                mockRepository.deleteJournalEntry(differentUserId, testJournalId, testEntryId)
            } returns Result.success(Unit)

            // When
            val result = deleteJournalEntryUseCase(differentUserId, testJournalId, testEntryId)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                mockRepository.deleteJournalEntry(differentUserId, testJournalId, testEntryId)
            }
        }

    /**
     * Тест 5: Проверка для разных journalId
     */
    @Test
    fun testInvoke_differentJournalId() =
        runTest {
            // Given
            val differentJournalId = 111L
            coEvery {
                mockRepository.deleteJournalEntry(testUserId, differentJournalId, testEntryId)
            } returns Result.success(Unit)

            // When
            val result = deleteJournalEntryUseCase(testUserId, differentJournalId, testEntryId)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                mockRepository.deleteJournalEntry(testUserId, differentJournalId, testEntryId)
            }
        }

    /**
     * Тест 6: Проверка для разных entryId
     */
    @Test
    fun testInvoke_differentEntryId() =
        runTest {
            // Given
            val differentEntryId = 222L
            coEvery {
                mockRepository.deleteJournalEntry(testUserId, testJournalId, differentEntryId)
            } returns Result.success(Unit)

            // When
            val result = deleteJournalEntryUseCase(testUserId, testJournalId, differentEntryId)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                mockRepository.deleteJournalEntry(testUserId, testJournalId, differentEntryId)
            }
        }
}
