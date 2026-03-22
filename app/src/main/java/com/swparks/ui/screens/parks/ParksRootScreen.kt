package com.swparks.ui.screens.parks

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.swparks.JetpackWorkoutApplication
import com.swparks.R
import com.swparks.data.model.NewParkDraft
import com.swparks.data.model.Park
import com.swparks.domain.usecase.ICreateParkLocationHandler
import com.swparks.navigation.AppState
import com.swparks.ui.ds.ParksListView
import kotlinx.coroutines.launch

private enum class PermissionDialogCause {
    DENIED,
    FOREVER_DENIED
}

@Composable
fun ParksRootScreen(
    modifier: Modifier = Modifier,
    parks: List<Park>,
    onParkClick: (Park) -> Unit = {},
    onCreateParkClick: (NewParkDraft) -> Unit = {},
    appState: AppState
) {
    val context = LocalContext.current
    val appContainer = remember {
        (context.applicationContext as JetpackWorkoutApplication).container
    }
    val createParkLocationHandler = appContainer.createParkLocationHandler
    val coroutineScope = rememberCoroutineScope()

    val permissionState = rememberLocationPermissionState(
        createParkLocationHandler = createParkLocationHandler,
        coroutineScope = coroutineScope
    )

    LaunchedEffect(permissionState) {
        permissionState.permissionGrantedEvents.collect { draft ->
            onCreateParkClick(draft)
        }
    }

    LocationPermissionDialog(
        visible = permissionState.showDialog,
        permissionDialogCause = permissionState.dialogCause,
        onDismiss = permissionState::dismissDialog,
        onConfirm = permissionState::requestPermission,
        onOpenSettings = { intent -> permissionState.openSettings(intent) }
    )

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            CreateParkFab(
                appState = appState,
                onClick = {
                    Log.i("ParksRootScreen", "Нажата кнопка создания площадки")
                    permissionState.onFabClicked()
                }
            )
        }
    ) { innerPadding ->
        ParksListView(
            modifier = Modifier.fillMaxSize(),
            parks = parks,
            onParkClick = onParkClick
        )
    }
}

private class LocationPermissionState(
    private val context: android.content.Context,
    private val createParkLocationHandler: ICreateParkLocationHandler,
    private val coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    var showDialog by mutableStateOf(false)
        private set
    var dialogCause by mutableStateOf<PermissionDialogCause?>(null)
        private set

    var permissionLauncher: ((Map<String, Boolean>) -> Unit)? = null
    var openSettingsLauncher: ((Intent) -> Unit)? = null

    private val _permissionGrantedEvents = kotlinx.coroutines.flow.MutableSharedFlow<NewParkDraft>(
        extraBufferCapacity = 1
    )
    val permissionGrantedEvents: kotlinx.coroutines.flow.SharedFlow<NewParkDraft> = _permissionGrantedEvents

    fun onFabClicked() {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFineLocation || hasCoarseLocation) {
            Log.i("ParksRootScreen", "Разрешение на геолокацию уже предоставлено")
            handlePermissionGranted()
        } else {
            Log.i("ParksRootScreen", "Запрос разрешения на геолокацию")
            val activity = context as android.app.Activity
            val shouldShowRationale = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            if (shouldShowRationale) {
                dialogCause = PermissionDialogCause.DENIED
                showDialog = true
            } else {
                requestPermission()
            }
        }
    }

    fun dismissDialog() {
        showDialog = false
    }

    fun requestPermission() {
        permissionLauncher?.invoke(
            mapOf(
                Manifest.permission.ACCESS_FINE_LOCATION to true,
                Manifest.permission.ACCESS_COARSE_LOCATION to true
            )
        )
    }

    fun openSettings(intent: Intent) {
        showDialog = false
        openSettingsLauncher?.invoke(intent)
    }

    fun handlePermissionResult(permissions: Map<String, Boolean>) {
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val isGranted = fineLocationGranted || coarseLocationGranted

        if (isGranted) {
            Log.i("ParksRootScreen", "Разрешение на геолокацию получено")
            handlePermissionGranted()
        } else {
            Log.i("ParksRootScreen", "Разрешение на геолокацию отклонено")
            val activity = context as android.app.Activity
            val shouldShowRationale = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            dialogCause = if (shouldShowRationale) {
                PermissionDialogCause.DENIED
            } else {
                PermissionDialogCause.FOREVER_DENIED
            }
            showDialog = true
        }
    }

    private fun handlePermissionGranted() {
        coroutineScope.launch {
            val result = createParkLocationHandler()
            result.onSuccess { draft ->
                Log.i("ParksRootScreen", "Draft создан: lat=${draft.latitude}, lon=${draft.longitude}")
                _permissionGrantedEvents.tryEmit(draft)
            }
        }
    }
}

@Composable
private fun rememberLocationPermissionState(
    createParkLocationHandler: ICreateParkLocationHandler,
    coroutineScope: kotlinx.coroutines.CoroutineScope
): LocationPermissionState {
    val context = LocalContext.current
    val state = remember(createParkLocationHandler, coroutineScope) {
        LocationPermissionState(context, createParkLocationHandler, coroutineScope)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        state.handlePermissionResult(permissions)
    }

    val openSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        Log.i("ParksRootScreen", "Возврат из настроек приложения")
    }

    state.permissionLauncher = { permissions ->
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    state.openSettingsLauncher = { intent ->
        openSettingsLauncher.launch(intent)
    }

    return state
}

@Composable
private fun LocationPermissionDialog(
    visible: Boolean,
    permissionDialogCause: PermissionDialogCause?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onOpenSettings: (Intent) -> Unit
) {
    val context = LocalContext.current

    if (visible && permissionDialogCause != null) {
        LocationPermissionAlertDialog(
            visible = true,
            onDismiss = onDismiss,
            onConfirm = onConfirm,
            onOpenSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                onOpenSettings(intent)
            },
            isDeniedForever = permissionDialogCause == PermissionDialogCause.FOREVER_DENIED
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParksTopAppBar(
    parksCount: Int,
    onFilterClick: () -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = {
            Text(text = stringResource(R.string.parks_title, parksCount.toString()))
        },
        actions = {
            IconButton(
                onClick = {
                    Log.d("ParksRootScreen", "Кнопка фильтрации нажата")
                    onFilterClick()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.FilterAlt,
                    contentDescription = stringResource(R.string.filter_parks)
                )
            }
        }
    )
}

@Composable
fun CreateParkFab(
    appState: AppState,
    onClick: () -> Unit = {}
) {
    if (appState.isAuthorized) {
        FloatingActionButton(
            onClick = {
                Log.i("ParksScreen", "Нажата кнопка создания площадки")
                onClick()
            }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Создать площадку"
            )
        }
    }
}
