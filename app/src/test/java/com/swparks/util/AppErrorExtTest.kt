package com.swparks.util

import android.content.Context
import com.swparks.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppErrorExtTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        every { context.getString(R.string.location_fetch_failed) } returns "Unable to determine location"
        every {
            context.getString(R.string.geocoding_address_build_fail)
        } returns "Unable to determine address, please enter it manually"
        every { context.getString(R.string.error_network_io) } returns "Network error. Check your internet connection."
    }

    @Test
    fun toUiText_whenLocationFailed_returnsLocationFetchFailedString() {
        val error = AppError.LocationFailed("Location request failed")

        val result = error.toUiText(context)

        assertEquals("Unable to determine location", result)
    }

    @Test
    fun toUiText_whenGeocodingFailedWithAddressBuildFail_returnsGeocodingAddressBuildFailString() {
        val error = AppError.GeocodingFailed(
            message = "Failed to build address",
            kind = AppError.GeocodingFailureKind.ADDRESS_BUILD_FAIL
        )

        val result = error.toUiText(context)

        assertEquals("Unable to determine address, please enter it manually", result)
    }

    @Test
    fun toUiText_whenGeocodingFailedWithIoError_returnsNetworkIoString() {
        val error = AppError.GeocodingFailed(
            message = "IO error during geocoding",
            kind = AppError.GeocodingFailureKind.IO_ERROR
        )

        val result = error.toUiText(context)

        assertEquals("Network error. Check your internet connection.", result)
    }

    @Test
    fun toUiText_whenGenericError_returnsMessageDirectly() {
        val error = AppError.Generic("Custom error message")

        val result = error.toUiText(context)

        assertEquals("Custom error message", result)
    }
}
