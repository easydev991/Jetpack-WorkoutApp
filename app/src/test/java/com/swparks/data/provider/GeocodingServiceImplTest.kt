package com.swparks.data.provider

import android.content.Context
import android.location.Address
import android.location.Geocoder
import com.swparks.util.AppError
import com.swparks.util.AppErrorException
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class GeocodingServiceImplTest {

    private val context: Context = mockk(relaxed = true)
    private val geocoder: Geocoder = mockk()

    @Test
    @Config(sdk = [33])
    fun reverseGeocode_whenApi33AndPlacemarkFound_thenReturnsFormattedResult() = runTest {
        val address = Address(Locale("ru", "RU")).apply {
            locality = "Москва"
            subLocality = "Москва"
            thoroughfare = "Тверская"
            subThoroughfare = "10"
            adminArea = "Москва"
            countryName = "Россия"
        }
        every {
            geocoder.getFromLocation(any(), any(), 1, any<Geocoder.GeocodeListener>())
        } answers {
            arg<Geocoder.GeocodeListener>(3).onGeocode(mutableListOf(address))
        }
        val service = createService()

        val result = service.reverseGeocode(55.751244, 37.618423)

        assertTrue(result.isSuccess)
        val data = result.getOrThrow()
        assertEquals("Москва, Тверская, 10, Россия", data.address)
        assertEquals("Москва", data.locality)
        assertEquals("Россия", data.countryName)
    }

    @Test
    @Config(sdk = [33])
    fun reverseGeocode_whenApi33AndListenerError_thenReturnsNoPlacemarkFailure() = runTest {
        every {
            geocoder.getFromLocation(any(), any(), 1, any<Geocoder.GeocodeListener>())
        } answers {
            arg<Geocoder.GeocodeListener>(3).onError("failed")
        }
        val service = createService()

        val result = service.reverseGeocode(55.751244, 37.618423)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is AppErrorException)
        val appError = (exception as AppErrorException).appError
        assertTrue(appError is AppError.GeocodingFailed)
        assertEquals(
            AppError.GeocodingFailureKind.ADDRESS_BUILD_FAIL,
            (appError as AppError.GeocodingFailed).kind
        )
    }

    @Test
    @Config(sdk = [32])
    fun reverseGeocode_whenApi32AndIOException_thenReturnsGeocodingFailed() = runTest {
        @Suppress("DEPRECATION")
        every { geocoder.getFromLocation(any(), any(), 1) } throws IOException("io")
        val service = createService()

        val result = service.reverseGeocode(55.751244, 37.618423)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is AppErrorException)
        val appError = (exception as AppErrorException).appError
        assertTrue(appError is AppError.GeocodingFailed)
        assertEquals(
            AppError.GeocodingFailureKind.IO_ERROR,
            (appError as AppError.GeocodingFailed).kind
        )
    }

    @Test
    @Config(sdk = [32])
    fun reverseGeocode_whenApi32AndAddressCannotBeBuilt_thenReturnsFailure() = runTest {
        @Suppress("DEPRECATION")
        every {
            geocoder.getFromLocation(any(), any(), 1)
        } returns listOf(Address(Locale("ru", "RU")))
        val service = createService()

        val result = service.reverseGeocode(55.751244, 37.618423)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is AppErrorException)
        val appError = (exception as AppErrorException).appError
        assertTrue(appError is AppError.GeocodingFailed)
        assertEquals(
            AppError.GeocodingFailureKind.ADDRESS_BUILD_FAIL,
            (appError as AppError.GeocodingFailed).kind
        )
    }

    private fun createService(): GeocodingServiceImpl {
        return GeocodingServiceImpl(
            context = context,
            geocoderProvider = { geocoder }
        )
    }
}
