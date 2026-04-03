package com.swparks.util

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat

object AppVersionProvider {
    fun getVersion(context: Context): String =
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        }.getOrDefault("unknown")

    fun getVersionCode(context: Context): Long =
        runCatching {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            PackageInfoCompat.getLongVersionCode(packageInfo)
        }.getOrDefault(-1L)
}
