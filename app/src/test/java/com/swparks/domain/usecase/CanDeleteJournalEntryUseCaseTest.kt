package com.swparks.domain.usecase

import com.swparks.domain.repository.JournalEntriesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit тесты для CanDeleteJournalEntryUseCase
 *
 * Проверяет корректность проверки возможности удаления записи
 */
class CanDeleteJournalEntryUseCaseTest {
    private lateinit var mockRepository: JournalEntriesRepository
    private lateinit var useCase: CanDeleteJournalEntryUseCase

    private val testJournalId = 456L

    @Before
    fun setup() {
        mockRepository = mockk()
        useCase = CanDeleteJournalEntryUseCase(mockRepository)
    }

    /**
     * Тест 1: Первую запись (с минимальным id) нельзя удалить
     */
    @Test
    fun testInvoke_firstEntry_returnsFalse() =
        runTest {
            // Given
            val testEntryId = 1L
            coEvery { mockRepository.canDeleteEntry(testEntryId, testJournalId) } returns false

            // When
            val result = useCase(testEntryId, testJournalId)

            // Then
            assertEquals(false, result)
            coVerify(exactly = 1) { mockRepository.canDeleteEntry(testEntryId, testJournalId) }
        }

    /**
     * Тест 2: Не первую запись можно удалить
     */
    @Test
    fun testInvoke_notFirstEntry_returnsTrue() =
        runTest {
            // Given
            val testEntryId = 2L
            coEvery { mockRepository.canDeleteEntry(testEntryId, testJournalId) } returns true

            // When
            val result = useCase(testEntryId, testJournalId)

            // Then
            assertEquals(true, result)
            coVerify(exactly = 1) { mockRepository.canDeleteEntry(testEntryId, testJournalId) }
        }

    /**
     * Тест 3: Если записей нет в БД, то можно удалить любую запись
     */
    @Test
    fun testInvoke_noEntries_returnsTrue() =
        runTest {
            // Given
            val testEntryId = 1L
            coEvery { mockRepository.canDeleteEntry(testEntryId, testJournalId) } returns true

            // When
            val result = useCase(testEntryId, testJournalId)

            // Then
            assertEquals(true, result)
            coVerify(exactly = 1) { mockRepository.canDeleteEntry(testEntryId, testJournalId) }
        }
}
