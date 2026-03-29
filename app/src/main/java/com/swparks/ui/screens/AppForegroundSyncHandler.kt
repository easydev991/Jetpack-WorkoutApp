package com.swparks.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.swparks.domain.usecase.SyncCountriesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun createAppForegroundSyncObserver(
    scope: CoroutineScope,
    onSyncCountries: suspend () -> Result<Unit>
): LifecycleEventObserver = LifecycleEventObserver { _, event ->
    if (event == Lifecycle.Event.ON_START) {
        scope.launch {
            runCatching { onSyncCountries() }
        }
    }
}

@Composable
fun AppForegroundSyncHandler(syncCountriesUseCase: SyncCountriesUseCase) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    DisposableEffect(lifecycleOwner, syncCountriesUseCase) {
        val observer = createAppForegroundSyncObserver(scope, syncCountriesUseCase::invoke)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
