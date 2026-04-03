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

class DeleteEventUseCaseTest {
    private lateinit var mockRepository: SWRepository
    private lateinit var deleteEventUseCase: DeleteEventUseCase

    private val testEventId = 123L

    @Before
    fun setup() {
        mockRepository = mockk()
        deleteEventUseCase = DeleteEventUseCase(mockRepository)
    }

    @Test
    fun invoke_success_callsRemoveEventLocally() =
        runTest {
            coEvery {
                mockRepository.removeEventLocally(testEventId)
            } returns Result.success(Unit)

            val result = deleteEventUseCase(testEventId)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                mockRepository.removeEventLocally(testEventId)
            }
        }

    @Test
    fun invoke_failure_returnsFailure() =
        runTest {
            val exception = RuntimeException("Ошибка удаления")
            coEvery {
                mockRepository.removeEventLocally(testEventId)
            } returns Result.failure(exception)

            val result = deleteEventUseCase(testEventId)

            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
            coVerify(exactly = 1) {
                mockRepository.removeEventLocally(testEventId)
            }
        }

    @Test
    fun invoke_passesCorrectEventId() =
        runTest {
            val eventId = 456L
            coEvery {
                mockRepository.removeEventLocally(eventId)
            } returns Result.success(Unit)

            deleteEventUseCase(eventId)

            coVerify(exactly = 1) {
                mockRepository.removeEventLocally(eventId = eventId)
            }
        }
}
