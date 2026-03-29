package com.swparks.domain.usecase

import com.swparks.data.repository.SWRepository
import com.swparks.util.Logger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InitializeParksUseCaseTest {

    private lateinit var swRepository: SWRepository
    private lateinit var logger: Logger
    private lateinit var useCase: InitializeParksUseCase

    @Before
    fun setup() {
        swRepository = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        useCase = InitializeParksUseCase(mockk(relaxed = true), swRepository, logger)
    }

    @Test
    fun invoke_callsImportSeedParks() = runTest {
        coEvery { swRepository.importSeedParks(any()) } returns Unit

        val result = useCase()

        coVerify(exactly = 1) { swRepository.importSeedParks(any()) }
        assertTrue(result.isSuccess)
    }

    @Test
    fun invoke_whenRepositoryThrows_returnsFailure() = runTest {
        val error = RuntimeException("Import failed")
        coEvery { swRepository.importSeedParks(any()) } throws error

        val result = useCase()

        assertEquals(Result.failure<Unit>(error), result)
    }
}
