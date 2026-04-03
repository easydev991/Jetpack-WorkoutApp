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
 * Unit тесты для CreateJournalUseCase
 *
 * Проверяет корректность работы use case для создания дневников,
 * включая прокидывание результатов из репозитория без изменений
 */
class CreateJournalUseCaseTest {
    private lateinit var mockRepository: SWRepository
    private lateinit var createJournalUseCase: CreateJournalUseCase

    private val testUserId = 123L
    private val testTitle = "Мой дневник"

    @Before
    fun setup() {
        mockRepository = mockk()
        createJournalUseCase = CreateJournalUseCase(mockRepository)
    }

    /**
     * Тест 1: Успешное создание дневника
     */
    @Test
    fun testInvoke_success_returnsSuccess() =
        runTest {
            // Given
            coEvery {
                mockRepository.createJournal(testTitle, testUserId)
            } returns Result.success(Unit)

            // When
            val result = createJournalUseCase(testUserId, testTitle)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                mockRepository.createJournal(testTitle, testUserId)
            }
        }

    /**
     * Тест 2: Ошибка при создании дневника
     */
    @Test
    fun testInvoke_failure_returnsFailure() =
        runTest {
            // Given
            val exception = RuntimeException("Ошибка создания")
            coEvery {
                mockRepository.createJournal(testTitle, testUserId)
            } returns Result.failure(exception)

            // When
            val result = createJournalUseCase(testUserId, testTitle)

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
            coVerify(exactly = 1) {
                mockRepository.createJournal(testTitle, testUserId)
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
                mockRepository.createJournal(testTitle, testUserId)
            } returns Result.success(Unit)

            // When
            createJournalUseCase(testUserId, testTitle)

            // Then - проверяем, что параметры переданы правильно
            coVerify(exactly = 1) {
                mockRepository.createJournal(
                    title = testTitle,
                    userId = testUserId
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
                mockRepository.createJournal(testTitle, differentUserId)
            } returns Result.success(Unit)

            // When
            val result = createJournalUseCase(differentUserId, testTitle)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                mockRepository.createJournal(testTitle, differentUserId)
            }
        }

    /**
     * Тест 5: Проверка для разных title
     */
    @Test
    fun testInvoke_differentTitle() =
        runTest {
            // Given
            val differentTitle = "Название 2"
            coEvery {
                mockRepository.createJournal(differentTitle, testUserId)
            } returns Result.success(Unit)

            // When
            val result = createJournalUseCase(testUserId, differentTitle)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                mockRepository.createJournal(differentTitle, testUserId)
            }
        }

    /**
     * Тест 6: Проверка для пустого названия
     */
    @Test
    fun testInvoke_emptyTitle() =
        runTest {
            // Given
            val emptyTitle = ""
            coEvery {
                mockRepository.createJournal(emptyTitle, testUserId)
            } returns Result.success(Unit)

            // When
            val result = createJournalUseCase(testUserId, emptyTitle)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                mockRepository.createJournal(emptyTitle, testUserId)
            }
        }
}
