package com.swparks.domain.provider

import android.content.IntentSender

sealed class LocationSettingsCheckResult {
    data object SettingsOk : LocationSettingsCheckResult()

    data class NeedsResolution(
        val intentSender: IntentSender
    ) : LocationSettingsCheckResult()

    data object SettingsDisabled : LocationSettingsCheckResult()
}
