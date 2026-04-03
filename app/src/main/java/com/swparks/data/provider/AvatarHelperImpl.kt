package com.swparks.data.provider

import android.content.Context
import android.net.Uri
import android.util.Log
import com.swparks.domain.provider.AvatarHelper
import com.swparks.util.ImageUtils
import java.io.IOException

/**
 * Реализация AvatarHelper на основе Android Context.
 *
 * @param context Application Context для доступа к ContentResolver
 */
class AvatarHelperImpl(
    private val context: Context
) : AvatarHelper {
    private companion object {
        private const val TAG = "AvatarHelperImpl"
    }

    override fun isSupportedMimeType(uri: Uri): Boolean = ImageUtils.isSupportedMimeType(context, uri)

    override fun uriToByteArray(uri: Uri): Result<ByteArray> =
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                Result.success(inputStream.readBytes())
            } ?: Result.failure(IOException("Cannot open input stream for uri: $uri"))
        } catch (e: SecurityException) {
            Log.w(TAG, "Ошибка доступа при чтении uri: $uri", e)
            Result.failure(SecurityException("No permission to read uri: $uri"))
        } catch (e: IOException) {
            Log.w(TAG, "Ошибка ввода-вывода при чтении uri: $uri", e)
            Result.failure(e)
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception
        ) {
            Log.e(TAG, "Неожиданная ошибка при чтении uri: $uri", e)
            Result.failure(IOException("Unexpected error reading uri: ${e.message}"))
        }
}
