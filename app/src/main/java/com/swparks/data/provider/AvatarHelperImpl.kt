package com.swparks.data.provider

import android.content.Context
import android.net.Uri
import com.swparks.domain.provider.AvatarHelper
import com.swparks.util.ImageUtils
import com.swparks.util.UriUtils

/**
 * Реализация AvatarHelper на основе Android Context.
 *
 * @param context Application Context для доступа к ContentResolver
 */
class AvatarHelperImpl(
    private val context: Context
) : AvatarHelper {
    override fun isSupportedMimeType(uri: Uri): Boolean = ImageUtils.isSupportedMimeType(context, uri)

    override fun uriToByteArray(uri: Uri): Result<ByteArray> = UriUtils.uriToByteArray(context, uri)
}
