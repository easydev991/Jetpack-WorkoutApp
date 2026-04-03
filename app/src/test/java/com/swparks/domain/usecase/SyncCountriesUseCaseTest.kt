package com.swparks.domain.usecase

import com.swparks.data.UserPreferencesRepository
import com.swparks.domain.repository.CountriesRepository
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

class SyncCountriesUseCaseTest {
    private lateinit var clock: Clock
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var countriesRepository: CountriesRepository
    private lateinit var syncCountriesUseCase: SyncCountriesUseCase

    @Before
    fun setup() {
        clock = TestClock("2025-10-27T12:00:00Z")
        userPreferencesRepository = mockk(relaxed = true)
        countriesRepository = mockk(relaxed = true)
        syncCountriesUseCase =
            SyncCountriesUseCase(
                clock,
                userPreferencesRepository,
                countriesRepository,
                NoOpLogger()
            )
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
    fun invoke_whenNeedUpdateIsFalse_thenDoesNotCallUpdateCountriesFromServer() =
        runTest {
            every { userPreferencesRepository.lastCountriesUpdateDate } returns flowOf("2025-10-27T10:00:00Z")

            syncCountriesUseCase()

            coVerify(exactly = 0) { countriesRepository.updateCountriesFromServer() }
        }

    @Test
    fun invoke_whenNeedUpdateIsTrue_thenCallsUpdateCountriesFromServer() =
        runTest {
            every { userPreferencesRepository.lastCountriesUpdateDate } returns flowOf("2025-10-25T00:00:00Z")
            coEvery { countriesRepository.updateCountriesFromServer() } returns Result.success(Unit)

            syncCountriesUseCase()

            coVerify { countriesRepository.updateCountriesFromServer() }
        }

    @Test
    fun invoke_whenSuccessfulUpdate_thenSavesDate() =
        runTest {
            every { userPreferencesRepository.lastCountriesUpdateDate } returns flowOf("2025-10-25T00:00:00Z")
            coEvery { countriesRepository.updateCountriesFromServer() } returns Result.success(Unit)

            syncCountriesUseCase()

            coVerify { userPreferencesRepository.setLastCountriesUpdateDate(clock.nowIsoString()) }
        }

    @Test
    fun invoke_whenNetworkError_thenDoesNotSaveDate() =
        runTest {
            every { userPreferencesRepository.lastCountriesUpdateDate } returns flowOf("2025-10-25T00:00:00Z")
            coEvery { countriesRepository.updateCountriesFromServer() } returns
                Result.failure(
                    Exception(
                        "Network error"
                    )
                )

            syncCountriesUseCase()

            coVerify(exactly = 0) { userPreferencesRepository.setLastCountriesUpdateDate(any()) }
        }

    @Test
    fun invoke_whenDefaultDate_thenCallsUpdateCountriesFromServer() =
        runTest {
            every { userPreferencesRepository.lastCountriesUpdateDate } returns flowOf("2025-10-25T00:00:00Z")
            coEvery { countriesRepository.updateCountriesFromServer() } returns Result.success(Unit)

            syncCountriesUseCase()

            coVerify { countriesRepository.updateCountriesFromServer() }
        }
}
