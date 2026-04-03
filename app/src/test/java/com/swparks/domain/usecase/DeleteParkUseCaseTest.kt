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

class DeleteParkUseCaseTest {
    private lateinit var mockRepository: SWRepository
    private lateinit var deleteParkUseCase: DeleteParkUseCase

    private val testParkId = 123L

    @Before
    fun setup() {
        mockRepository = mockk()
        deleteParkUseCase = DeleteParkUseCase(mockRepository)
    }

    @Test
    fun invoke_success_callsRemoveParkLocally() =
        runTest {
            coEvery {
                mockRepository.removeParkLocally(testParkId)
            } returns Result.success(Unit)

            val result = deleteParkUseCase(testParkId)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                mockRepository.removeParkLocally(testParkId)
            }
        }

    @Test
    fun invoke_failure_returnsFailure() =
        runTest {
            val exception = RuntimeException("Ошибка удаления")
            coEvery {
                mockRepository.removeParkLocally(testParkId)
            } returns Result.failure(exception)

            val result = deleteParkUseCase(testParkId)

            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
            coVerify(exactly = 1) {
                mockRepository.removeParkLocally(testParkId)
            }
        }

    @Test
    fun invoke_passesCorrectParkId() =
        runTest {
            val parkId = 456L
            coEvery {
                mockRepository.removeParkLocally(parkId)
            } returns Result.success(Unit)

            deleteParkUseCase(parkId)

            coVerify(exactly = 1) {
                mockRepository.removeParkLocally(parkId = parkId)
            }
        }
}
