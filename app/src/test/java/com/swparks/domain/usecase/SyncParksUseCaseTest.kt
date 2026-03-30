package com.swparks.domain.usecase

import com.swparks.data.UserPreferencesRepository
import com.swparks.data.repository.SWRepository
import com.swparks.domain.util.Clock
import com.swparks.domain.util.TestClock
import com.swparks.domain.util.isUpdateNeeded
import com.swparks.util.NoOpLogger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncParksUseCaseTest {

    private lateinit var clock: Clock
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var swRepository: SWRepository
    private lateinit var syncParksUseCase: SyncParksUseCase

    @Before
    fun setup() {
        clock = TestClock("2025-10-27T12:00:00Z")
        userPreferencesRepository = mockk(relaxed = true)
        swRepository = mockk(relaxed = true)
        syncParksUseCase =
            SyncParksUseCase(clock, userPreferencesRepository, swRepository, NoOpLogger())
    }

    @Test
    fun isUpdateNeeded_whenLessThanOneDay_sinceLastUpdate_thenFalse() {
        val lastUpdate = "2025-10-27T10:00:00Z"
        assertFalse(lastUpdate.isUpdateNeeded(clock))
    }

    @Test
    fun isUpdateNeeded_whenMoreThanOneDay_sinceLastUpdate_thenTrue() {
        val lastUpdate = "2025-10-26T11:59:59Z"
        assertTrue(lastUpdate.isUpdateNeeded(clock))
    }

    @Test
    fun invoke_whenNeedUpdateIsFalse_thenDoesNotCallGetUpdatedParks() = runTest {
        every { userPreferencesRepository.lastParksUpdateDate } returns flowOf("2025-10-27T10:00:00Z")

        syncParksUseCase()

        coVerify(exactly = 0) { swRepository.getUpdatedParks(any()) }
    }

    @Test
    fun invoke_whenForceTrue_thenCallsGetUpdatedParks() = runTest {
        every { userPreferencesRepository.lastParksUpdateDate } returns flowOf("2025-10-27T10:00:00Z")
        coEvery { swRepository.getUpdatedParks(any()) } returns Result.success(emptyList())
        coEvery { swRepository.upsertParks(any()) } returns Unit

        syncParksUseCase(force = true)

        coVerify { swRepository.getUpdatedParks("2025-10-27") }
    }

    @Test
    fun invoke_whenSuccessfulUpdateWithNonEmptyList_thenSavesDate() = runTest {
        every { userPreferencesRepository.lastParksUpdateDate } returns flowOf("2025-10-25T00:00:00Z")
        coEvery { swRepository.getUpdatedParks(any()) } returns Result.success(emptyList())
        coEvery { swRepository.upsertParks(any()) } returns Unit

        syncParksUseCase()

        coVerify { userPreferencesRepository.setLastParksUpdateDate(clock.nowIsoString()) }
    }

    @Test
    fun invoke_whenSuccessfulUpdateWithEmptyList_thenSavesDate() = runTest {
        every { userPreferencesRepository.lastParksUpdateDate } returns flowOf("2025-10-25T00:00:00Z")
        coEvery { swRepository.getUpdatedParks(any()) } returns Result.success(emptyList())
        coEvery { swRepository.upsertParks(any()) } returns Unit

        syncParksUseCase()

        coVerify { userPreferencesRepository.setLastParksUpdateDate(clock.nowIsoString()) }
    }

    @Test
    fun invoke_whenNetworkError_thenDoesNotSaveDate() = runTest {
        every { userPreferencesRepository.lastParksUpdateDate } returns flowOf("2025-10-25T00:00:00Z")
        coEvery { swRepository.getUpdatedParks(any()) } returns Result.failure(Exception("Network error"))

        syncParksUseCase()

        coVerify(exactly = 0) { userPreferencesRepository.setLastParksUpdateDate(any()) }
    }

    @Test
    fun invoke_whenForceTrue_withDateTimeFormat_thenExtractsDateOnly() = runTest {
        every { userPreferencesRepository.lastParksUpdateDate } returns flowOf("2025-10-27T15:30:45Z")
        coEvery { swRepository.getUpdatedParks(any()) } returns Result.success(emptyList())
        coEvery { swRepository.upsertParks(any()) } returns Unit

        syncParksUseCase(force = true)

        coVerify { swRepository.getUpdatedParks("2025-10-27") }
    }

}
