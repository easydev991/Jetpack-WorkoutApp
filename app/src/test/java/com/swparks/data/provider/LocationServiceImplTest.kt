package com.swparks.data.provider

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LocationServiceImplTest {

    private lateinit var context: Context
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        fusedLocationClient = mockk()
        locationManager = mockk(relaxed = true)
        mockkStatic(ContextCompat::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
    }

    @Test
    fun getCurrentLocation_whenPermissionDenied_thenReturnsFailureWithoutFusedCall() = runTest {
        setPermissionsGranted(false)
        val service = createService()

        val result = service.getCurrentLocation()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
        verify(exactly = 0) {
            fusedLocationClient.getCurrentLocation(any<Int>(), any<CancellationToken>())
        }
    }

    @Test
    fun getCurrentLocation_whenFusedReturnsLocation_thenReturnsCoordinates() = runTest {
        setPermissionsGranted(true)
        every {
            fusedLocationClient.getCurrentLocation(any<Int>(), any<CancellationToken>())
        } returns successTask(location("fused", 55.751244, 37.618423))
        val service = createService()

        val result = service.getCurrentLocation()

        assertTrue(result.isSuccess)
        assertEquals(55.751244, result.getOrThrow().latitude, 0.0)
        assertEquals(37.618423, result.getOrThrow().longitude, 0.0)
    }

    @Test
    fun getCurrentLocation_whenFusedReturnsNull_thenUsesLastKnownLocationFallback() = runTest {
        setPermissionsGranted(true)
        every {
            fusedLocationClient.getCurrentLocation(any<Int>(), any<CancellationToken>())
        } returns successTask(null)
        every { context.getSystemService(Context.LOCATION_SERVICE) } returns locationManager
        every { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } returns true
        every { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } returns
            location("gps", 59.938632, 30.314130)
        val service = createService()

        val result = service.getCurrentLocation()

        assertTrue(result.isSuccess)
        assertEquals(59.938632, result.getOrThrow().latitude, 0.0)
        assertEquals(30.314130, result.getOrThrow().longitude, 0.0)
    }

    @Test
    fun getCurrentLocation_whenTimeout_thenUsesLastKnownLocationFallback() = runTest {
        setPermissionsGranted(true)
        every {
            fusedLocationClient.getCurrentLocation(any<Int>(), any<CancellationToken>())
        } returns pendingTask()
        every { context.getSystemService(Context.LOCATION_SERVICE) } returns locationManager
        every { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } returns true
        every { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } returns
            location("gps", 48.856613, 2.352222)
        val service = createService(timeoutMillis = 1L)

        val deferred = async { service.getCurrentLocation() }
        advanceUntilIdle()
        val result = deferred.await()

        assertTrue(result.isSuccess)
        assertEquals(48.856613, result.getOrThrow().latitude, 0.0)
        assertEquals(2.352222, result.getOrThrow().longitude, 0.0)
    }

    @Test
    fun getCurrentLocation_whenFusedFailsAndFallbackFails_thenReturnsFusedException() = runTest {
        setPermissionsGranted(true)
        val expected = IllegalStateException("fused failed")
        every {
            fusedLocationClient.getCurrentLocation(any<Int>(), any<CancellationToken>())
        } returns failureTask(expected)
        every { context.getSystemService(Context.LOCATION_SERVICE) } returns null
        val service = createService()

        val result = service.getCurrentLocation()

        assertTrue(result.isFailure)
        assertEquals(expected, result.exceptionOrNull())
    }

    private fun createService(timeoutMillis: Long = 10_000L): LocationServiceImpl {
        return LocationServiceImpl(
            context = context,
            fusedLocationClientProvider = { fusedLocationClient },
            locationTimeoutMillis = timeoutMillis
        )
    }

    private fun setPermissionsGranted(granted: Boolean) {
        val status = if (granted) {
            PackageManager.PERMISSION_GRANTED
        } else {
            PackageManager.PERMISSION_DENIED
        }
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns status
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        } returns status
    }

    private fun location(provider: String, latitude: Double, longitude: Double): Location {
        return Location(provider).apply {
            this.latitude = latitude
            this.longitude = longitude
        }
    }

    private fun successTask(location: Location?): Task<Location?> {
        val task = mockk<Task<Location?>>(relaxed = true)
        every { task.addOnSuccessListener(any()) } answers {
            arg<OnSuccessListener<Location?>>(0).onSuccess(location)
            task
        }
        every { task.addOnFailureListener(any()) } returns task
        return task
    }

    private fun failureTask(exception: Exception): Task<Location?> {
        val task = mockk<Task<Location?>>(relaxed = true)
        every { task.addOnSuccessListener(any()) } returns task
        every { task.addOnFailureListener(any()) } answers {
            arg<OnFailureListener>(0).onFailure(exception)
            task
        }
        return task
    }

    private fun pendingTask(): Task<Location?> {
        val task = mockk<Task<Location?>>(relaxed = true)
        every { task.addOnSuccessListener(any()) } returns task
        every { task.addOnFailureListener(any()) } returns task
        return task
    }
}
