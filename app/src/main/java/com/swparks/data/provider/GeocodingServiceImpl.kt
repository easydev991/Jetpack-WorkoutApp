package com.swparks.data.provider

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.swparks.domain.model.GeocodingResult
import com.swparks.domain.provider.GeocodingService
import com.swparks.util.AppError
import com.swparks.util.AppErrorException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume

class GeocodingServiceImpl(
    private val context: Context,
    private val geocoderProvider: (Context) -> Geocoder = { ctx -> Geocoder(ctx) }
) : GeocodingService {

    private val geocoder: Geocoder by lazy {
        geocoderProvider(context)
    }

    override suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double
    ): Result<GeocodingResult> {
        return withContext(Dispatchers.IO) {
            try {
                val addresses = reverseGeocodeInternal(latitude, longitude)

                if (addresses.isNullOrEmpty()) {
                    return@withContext Result.failure(
                        AppErrorException(
                            AppError.GeocodingFailed(
                                message = "No placemark found",
                                kind = AppError.GeocodingFailureKind.ADDRESS_BUILD_FAIL
                            )
                        )
                    )
                }

                val address = addresses.first()
                val formattedAddress = buildAddress(address)

                if (formattedAddress.isBlank()) {
                    return@withContext Result.failure(
                        AppErrorException(
                            AppError.GeocodingFailed(
                                message = "Failed to build address",
                                kind = AppError.GeocodingFailureKind.ADDRESS_BUILD_FAIL
                            )
                        )
                    )
                }

                Result.success(
                    GeocodingResult(
                        address = formattedAddress,
                        locality = address.locality,
                        countryName = address.countryName
                    )
                )
            } catch (e: IOException) {
                Result.failure(
                    AppErrorException(
                        AppError.GeocodingFailed(
                            message = "IO error during geocoding",
                            kind = AppError.GeocodingFailureKind.IO_ERROR,
                            cause = e
                        )
                    )
                )
            }
        }
    }

    private suspend fun reverseGeocodeInternal(
        latitude: Double,
        longitude: Double
    ): List<Address>? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(
                    latitude,
                    longitude,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) {
                                continuation.resume(addresses)
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) {
                                continuation.resume(null)
                            }
                        }
                    }
                )
            }
        }

        @Suppress("DEPRECATION")
        return geocoder.getFromLocation(latitude, longitude, 1)
    }

    private fun buildAddress(address: Address): String {
        val parts = mutableListOf<String>()

        address.locality?.let { addIfNotDuplicate(parts, it) }
        address.subLocality?.let { addIfNotDuplicate(parts, it) }
        address.thoroughfare?.let { addIfNotDuplicate(parts, it) }
        address.subThoroughfare?.let { addIfNotDuplicate(parts, it) }
        address.adminArea?.let { addIfNotDuplicate(parts, it) }
        address.subAdminArea?.let { addIfNotDuplicate(parts, it) }
        address.countryName?.let { addIfNotDuplicate(parts, it) }

        return parts.joinToString(", ")
    }

    private fun addIfNotDuplicate(parts: MutableList<String>, value: String) {
        val normalized = value.trim()
        if (normalized.isNotEmpty() && !parts.any { it.contains(normalized, ignoreCase = true) }) {
            parts.add(normalized)
        }
    }
}
