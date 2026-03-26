package com.swparks.data.provider

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import com.swparks.domain.model.LocationCoordinates
import com.swparks.domain.provider.LocationService
import com.swparks.domain.provider.LocationSettingsCheckResult
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class LocationServiceImpl(
    private val context: Context,
    private val fusedLocationClientProvider: (Context) -> FusedLocationProviderClient = { ctx ->
        LocationServices.getFusedLocationProviderClient(ctx)
    },
    private val settingsClientProvider: (Context) -> SettingsClient = { ctx ->
        LocationServices.getSettingsClient(ctx)
    },
    private val locationTimeoutMillis: Long = LOCATION_TIMEOUT_MILLIS
) : LocationService {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        fusedLocationClientProvider(context)
    }

    private val settingsClient: SettingsClient by lazy {
        settingsClientProvider(context)
    }

    @RequiresPermission(
        anyOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ]
    )
    override suspend fun getCurrentLocation(): Result<LocationCoordinates> {
        if (!hasLocationPermission()) {
            return Result.failure(SecurityException("Location permission is not granted"))
        }

        val locationResult = withTimeoutOrNull(locationTimeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                val cancellationTokenSource = CancellationTokenSource()

                try {
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.token
                    ).addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            continuation.resumeIfActive(
                                Result.success(
                                    LocationCoordinates(
                                        latitude = location.latitude,
                                        longitude = location.longitude
                                    )
                                )
                            )
                        } else {
                            val fallbackResult = getLastKnownLocationFallback()
                            continuation.resumeIfActive(fallbackResult)
                        }
                    }.addOnFailureListener { exception: Exception ->
                        val fallbackResult = getLastKnownLocationFallback()
                        if (fallbackResult.isFailure) {
                            continuation.resumeIfActive(Result.failure(exception))
                        } else {
                            continuation.resumeIfActive(fallbackResult)
                        }
                    }
                } catch (securityException: SecurityException) {
                    continuation.resumeIfActive(Result.failure(securityException))
                }

                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                }
            }
        }

        return locationResult ?: getLastKnownLocationFallback()
    }

    @RequiresPermission(
        anyOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ]
    )
    private fun getLastKnownLocationFallback(): Result<LocationCoordinates> {
        if (!hasLocationPermission()) {
            return Result.failure(SecurityException("Location permission is not granted"))
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val provider = locationManager?.let { findEnabledProvider(it) }
        val location = try {
            provider?.let { locationManager.getLastKnownLocation(it) }
        } catch (_: SecurityException) {
            null
        }

        return when {
            locationManager == null -> Result.failure(IllegalStateException("LocationManager not available"))
            provider == null -> Result.failure(IllegalStateException("No location provider available"))
            location == null -> Result.failure(IllegalStateException("Last known location is null"))
            else -> Result.success(
                LocationCoordinates(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            )
        }
    }

    private fun findEnabledProvider(locationManager: LocationManager): String? {
        return when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> locationManager.allProviders.firstOrNull()
        }
    }

    override suspend fun checkLocationSettings(): Result<LocationSettingsCheckResult> {
        return withContext(Dispatchers.IO) {
            try {
                if (!hasLocationPermission()) {
                    return@withContext Result.success(LocationSettingsCheckResult.SettingsDisabled)
                }

                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    LOCATION_REQUEST_INTERVAL_MILLIS
                ).build()

                val locationSettingsRequest = LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest)
                    .setAlwaysShow(true)
                    .build()

                val task = settingsClient.checkLocationSettings(locationSettingsRequest)
                val result = Tasks.await(task, SETTINGS_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS)

                val states = result.locationSettingsStates
                if (states?.isGpsUsable == true || states?.isNetworkLocationUsable == true) {
                    Result.success(LocationSettingsCheckResult.SettingsOk)
                } else {
                    Result.success(LocationSettingsCheckResult.SettingsDisabled)
                }
            } catch (e: ResolvableApiException) {
                Result.success(LocationSettingsCheckResult.NeedsResolution(e.resolution.intentSender))
            } catch (@Suppress("SwallowedException") e: SecurityException) {
                Result.success(LocationSettingsCheckResult.SettingsDisabled)
            } catch (e: ExecutionException) {
                when (val cause = e.cause) {
                    is ResolvableApiException -> {
                        Result.success(
                            LocationSettingsCheckResult.NeedsResolution(cause.resolution.intentSender)
                        )
                    }

                    is SecurityException -> Result.success(LocationSettingsCheckResult.SettingsDisabled)
                    null -> Result.failure(e)
                    else -> Result.failure(cause)
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                Result.failure(e)
            } catch (e: CancellationException) {
                Result.failure(e)
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        val finePermissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarsePermissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return finePermissionGranted || coarsePermissionGranted
    }

    private fun <T> CancellableContinuation<T>.resumeIfActive(value: T) {
        if (isActive) {
            resume(value)
        }
    }

    private companion object {
        const val LOCATION_TIMEOUT_MILLIS = 10_000L
        const val LOCATION_REQUEST_INTERVAL_MILLIS = 10_000L
        const val SETTINGS_CHECK_TIMEOUT_SECONDS = 30L
    }
}
