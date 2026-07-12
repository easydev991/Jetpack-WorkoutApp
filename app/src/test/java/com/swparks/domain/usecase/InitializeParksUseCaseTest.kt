package com.swparks.domain.usecase

import com.swparks.data.repository.ParksEventsRepository
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
    private lateinit var parksEventsRepository: ParksEventsRepository
    private lateinit var logger: Logger
    private lateinit var useCase: InitializeParksUseCase

    @Before
    fun setup() {
        parksEventsRepository = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        useCase = InitializeParksUseCase(mockk(relaxed = true), parksEventsRepository, logger)
    }

    @Test
    fun invoke_callsImportSeedParks() =
        runTest {
            coEvery { parksEventsRepository.importSeedParks(any()) } returns Unit

            val result = useCase()

            coVerify(exactly = 1) { parksEventsRepository.importSeedParks(any()) }
            assertTrue(result.isSuccess)
        }

    @Test
    fun invoke_whenRepositoryThrows_returnsFailure() =
        runTest {
            val error = RuntimeException("Import failed")
            coEvery { parksEventsRepository.importSeedParks(any()) } throws error

            val result = useCase()

            assertEquals(Result.failure<Unit>(error), result)
        }
}
