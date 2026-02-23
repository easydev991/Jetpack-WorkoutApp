package com.swparks.domain.usecase

import com.swparks.data.repository.SWRepository
import com.swparks.ui.model.JournalAccess
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit тесты для EditJournalSettingsUseCase
 *
 * Проверяет корректность работы use case для редактирования настроек дневника,
 * включая прокидывание результатов из репозитория без изменений
 */
class EditJournalSettingsUseCaseTest {

    private lateinit var mockRepository: SWRepository
    private lateinit var editJournalSettingsUseCase: EditJournalSettingsUseCase

    private val testJournalId = 456L
    private val testTitle = "Мой дневник"
    private val testUserId = 123L
    private val testViewAccess = JournalAccess.FRIENDS
    private val testCommentAccess = JournalAccess.ALL

    @Before
    fun setup() {
        mockRepository = mockk()
        editJournalSettingsUseCase = EditJournalSettingsUseCase(mockRepository)
    }

    /**
     * Тест 1: Успешное редактирование настроек дневника
     */
    @Test
    fun testInvoke_success_returnsSuccess() = runTest {
        // Given
        coEvery {
            mockRepository.editJournalSettings(
                journalId = testJournalId,
                title = testTitle,
                userId = testUserId,
                viewAccess = testViewAccess,
                commentAccess = testCommentAccess
            )
        } returns Result.success(Unit)

        // When
        val result = editJournalSettingsUseCase(
            journalId = testJournalId,
            title = testTitle,
            userId = testUserId,
            viewAccess = testViewAccess,
            commentAccess = testCommentAccess
        )

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            mockRepository.editJournalSettings(
                journalId = testJournalId,
                title = testTitle,
                userId = testUserId,
                viewAccess = testViewAccess,
                commentAccess = testCommentAccess
            )
        }
    }

    /**
     * Тест 2: Ошибка при редактировании настроек дневника
     */
    @Test
    fun testInvoke_failure_returnsFailure() = runTest {
        // Given
        val exception = RuntimeException("Ошибка редактирования")
        coEvery {
            mockRepository.editJournalSettings(
                journalId = testJournalId,
                title = testTitle,
                userId = testUserId,
                viewAccess = testViewAccess,
                commentAccess = testCommentAccess
            )
        } returns Result.failure(exception)

        // When
        val result = editJournalSettingsUseCase(
            journalId = testJournalId,
            title = testTitle,
            userId = testUserId,
            viewAccess = testViewAccess,
            commentAccess = testCommentAccess
        )

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) {
            mockRepository.editJournalSettings(
                journalId = testJournalId,
                title = testTitle,
                userId = testUserId,
                viewAccess = testViewAccess,
                commentAccess = testCommentAccess
            )
        }
    }

    /**
     * Тест 3: Проверка передачи правильных параметров в репозиторий
     */
    @Test
    fun testInvoke_passesCorrectParameters() = runTest {
        // Given
        coEvery {
            mockRepository.editJournalSettings(
                journalId = testJournalId,
                title = testTitle,
                userId = testUserId,
                viewAccess = testViewAccess,
                commentAccess = testCommentAccess
            )
        } returns Result.success(Unit)

        // When
        editJournalSettingsUseCase(
            journalId = testJournalId,
            title = testTitle,
            userId = testUserId,
            viewAccess = testViewAccess,
            commentAccess = testCommentAccess
        )

        // Then - проверяем, что параметры переданы правильно
        coVerify(exactly = 1) {
            mockRepository.editJournalSettings(
                journalId = testJournalId,
                title = testTitle,
                userId = testUserId,
                viewAccess = testViewAccess,
                commentAccess = testCommentAccess
            )
        }
    }

    /**
     * Тест 4: Редактирование с null userId
     */
    @Test
    fun testInvoke_nullUserId() = runTest {
        // Given
        val nullUserId: Long? = null
        coEvery {
            mockRepository.editJournalSettings(
                journalId = testJournalId,
                title = testTitle,
                userId = nullUserId,
                viewAccess = testViewAccess,
                commentAccess = testCommentAccess
            )
        } returns Result.success(Unit)

        // When
        val result = editJournalSettingsUseCase(
            journalId = testJournalId,
            title = testTitle,
            userId = nullUserId,
            viewAccess = testViewAccess,
            commentAccess = testCommentAccess
        )

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            mockRepository.editJournalSettings(
                journalId = testJournalId,
                title = testTitle,
                userId = nullUserId,
                viewAccess = testViewAccess,
                commentAccess = testCommentAccess
            )
        }
    }

    /**
     * Тест 5: Проверка для разных уровней доступа
     */
    @Test
    fun testInvoke_differentAccessLevels() = runTest {
        // Given
        val viewAccess = JournalAccess.NOBODY
        val commentAccess = JournalAccess.NOBODY
        coEvery {
            mockRepository.editJournalSettings(
                journalId = testJournalId,
                title = testTitle,
                userId = testUserId,
                viewAccess = viewAccess,
                commentAccess = commentAccess
            )
        } returns Result.success(Unit)

        // When
        val result = editJournalSettingsUseCase(
            journalId = testJournalId,
            title = testTitle,
            userId = testUserId,
            viewAccess = viewAccess,
            commentAccess = commentAccess
        )

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            mockRepository.editJournalSettings(
                journalId = testJournalId,
                title = testTitle,
                userId = testUserId,
                viewAccess = viewAccess,
                commentAccess = commentAccess
            )
        }
    }

    /**
     * Тест 6: Проверка для разных title
     */
    @Test
    fun testInvoke_differentTitle() = runTest {
        // Given
        val differentTitle = "Новое название"
        coEvery {
            mockRepository.editJournalSettings(
                journalId = testJournalId,
                title = differentTitle,
                userId = testUserId,
                viewAccess = testViewAccess,
                commentAccess = testCommentAccess
            )
        } returns Result.success(Unit)

        // When
        val result = editJournalSettingsUseCase(
            journalId = testJournalId,
            title = differentTitle,
            userId = testUserId,
            viewAccess = testViewAccess,
            commentAccess = testCommentAccess
        )

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            mockRepository.editJournalSettings(
                journalId = testJournalId,
                title = differentTitle,
                userId = testUserId,
                viewAccess = testViewAccess,
                commentAccess = testCommentAccess
            )
        }
    }

    /**
     * Тест 7: Проверка для разных journalId
     */
    @Test
    fun testInvoke_differentJournalId() = runTest {
        // Given
        val differentJournalId = 789L
        coEvery {
            mockRepository.editJournalSettings(
                journalId = differentJournalId,
                title = testTitle,
                userId = testUserId,
                viewAccess = testViewAccess,
                commentAccess = testCommentAccess
            )
        } returns Result.success(Unit)

        // When
        val result = editJournalSettingsUseCase(
            journalId = differentJournalId,
            title = testTitle,
            userId = testUserId,
            viewAccess = testViewAccess,
            commentAccess = testCommentAccess
        )

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            mockRepository.editJournalSettings(
                journalId = differentJournalId,
                title = testTitle,
                userId = testUserId,
                viewAccess = testViewAccess,
                commentAccess = testCommentAccess
            )
        }
    }

    /**
     * Тест 8: Проверка для пустого названия
     */
    @Test
    fun testInvoke_emptyTitle() = runTest {
        // Given
        val emptyTitle = ""
        coEvery {
            mockRepository.editJournalSettings(
                journalId = testJournalId,
                title = emptyTitle,
                userId = testUserId,
                viewAccess = testViewAccess,
                commentAccess = testCommentAccess
            )
        } returns Result.success(Unit)

        // When
        val result = editJournalSettingsUseCase(
            journalId = testJournalId,
            title = emptyTitle,
            userId = testUserId,
            viewAccess = testViewAccess,
            commentAccess = testCommentAccess
        )

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            mockRepository.editJournalSettings(
                journalId = testJournalId,
                title = emptyTitle,
                userId = testUserId,
                viewAccess = testViewAccess,
                commentAccess = testCommentAccess
            )
        }
    }

    /**
     * Тест 9: Проверка для сети (IOException)
     */
    @Test
    fun testInvoke_networkError_returnsFailure() = runTest {
        // Given
        val exception = java.io.IOException("Ошибка сети")
        coEvery {
            mockRepository.editJournalSettings(
                journalId = testJournalId,
                title = testTitle,
                userId = testUserId,
                viewAccess = testViewAccess,
                commentAccess = testCommentAccess
            )
        } returns Result.failure(exception)

        // When
        val result = editJournalSettingsUseCase(
            journalId = testJournalId,
            title = testTitle,
            userId = testUserId,
            viewAccess = testViewAccess,
            commentAccess = testCommentAccess
        )

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) {
            mockRepository.editJournalSettings(
                journalId = testJournalId,
                title = testTitle,
                userId = testUserId,
                viewAccess = testViewAccess,
                commentAccess = testCommentAccess
            )
        }
    }

    /**
     * Тест 10: Проверка для ошибки сервера (ServerException)
     */
    @Test
    fun testInvoke_serverError_returnsFailure() = runTest {
        // Given
        val exception = Exception("Ошибка сервера")
        coEvery {
            mockRepository.editJournalSettings(
                journalId = testJournalId,
                title = testTitle,
                userId = testUserId,
                viewAccess = testViewAccess,
                commentAccess = testCommentAccess
            )
        } returns Result.failure(exception)

        // When
        val result = editJournalSettingsUseCase(
            journalId = testJournalId,
            title = testTitle,
            userId = testUserId,
            viewAccess = testViewAccess,
            commentAccess = testCommentAccess
        )

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) {
            mockRepository.editJournalSettings(
                journalId = testJournalId,
                title = testTitle,
                userId = testUserId,
                viewAccess = testViewAccess,
                commentAccess = testCommentAccess
            )
        }
    }
}
