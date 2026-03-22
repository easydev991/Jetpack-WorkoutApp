package com.swparks.domain.usecase

import com.swparks.domain.model.LocationCoordinates
import com.swparks.domain.provider.LocationService
import com.swparks.util.AppError
import com.swparks.util.UserNotifier
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateParkLocationHandlerTest {

    private lateinit var locationService: LocationService
    private lateinit var userNotifier: UserNotifier
    private lateinit var handler: DefaultCreateParkLocationHandler

    @Before
    fun setup() {
        locationService = mockk(relaxed = true)
        userNotifier = mockk(relaxed = true)
        handler = DefaultCreateParkLocationHandler(locationService, userNotifier)
    }

    @Test
    fun invoke_whenLocationSucceeds_returnsSuccessResult() = runTest {
        val coordinates = LocationCoordinates(latitude = 55.7558, longitude = 37.6173)
        coEvery { locationService.getCurrentLocation() } returns Result.success(coordinates)

        val result = handler()

        assertTrue(result.isSuccess)
        val draft = result.getOrThrow()
        assertEquals(55.7558, draft.latitude, 0.0001)
        assertEquals(37.6173, draft.longitude, 0.0001)
        assertTrue(draft.lastLocationRequestDate != null)
    }

    @Test
    fun invoke_whenLocationSucceeds_draftHasEmptyAddressAndNullCityId() = runTest {
        val coordinates = LocationCoordinates(latitude = 55.7558, longitude = 37.6173)
        coEvery { locationService.getCurrentLocation() } returns Result.success(coordinates)

        val result = handler()

        val draft = result.getOrThrow()
        assertEquals("", draft.address)
        assertEquals(null, draft.cityId)
    }

    @Test
    fun invoke_whenLocationFails_returnsFailureResult() = runTest {
        coEvery { locationService.getCurrentLocation() } returns Result.failure(Exception("Location unavailable"))

        val result = handler()

        assertTrue(result.isFailure)
    }

    @Test
    fun invoke_whenLocationFails_doesNotShowInfoNotification() = runTest {
        coEvery { locationService.getCurrentLocation() } returns Result.failure(Exception("Location unavailable"))

        handler()

        verify(inverse = true) { userNotifier.showInfo(any()) }
    }

    @Test
    fun invoke_whenLocationFails_callsHandleErrorWithGenericAppError() = runTest {
        coEvery { locationService.getCurrentLocation() } returns Result.failure(Exception("Location unavailable"))

        handler()

        verify { userNotifier.handleError(match<AppError> { it.message.isNotEmpty() }) }
    }

    @Test
    fun invoke_whenLocationSucceeds_doesNotCallHandleError() = runTest {
        val coordinates = LocationCoordinates(latitude = 55.7558, longitude = 37.6173)
        coEvery { locationService.getCurrentLocation() } returns Result.success(coordinates)

        handler()

        verify(inverse = true) { userNotifier.handleError(any<AppError>()) }
    }
}
