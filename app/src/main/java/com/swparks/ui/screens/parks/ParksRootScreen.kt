package com.swparks.ui.screens.parks

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.swparks.R
import com.swparks.data.model.NewParkDraft
import com.swparks.data.model.Park
import com.swparks.navigation.AppState
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.ds.ParksListView
import com.swparks.ui.viewmodel.IParksRootViewModel
import com.swparks.ui.viewmodel.ParksRootEvent
import com.swparks.ui.viewmodel.PermissionDialogCause
import kotlinx.coroutines.flow.collectLatest

@Suppress("LongParameterList")
@Composable
fun ParksRootScreen(
    modifier: Modifier = Modifier,
    parks: List<Park>,
    onParkClick: (Park) -> Unit = {},
    onCreateParkClick: (NewParkDraft) -> Unit = {},
    onGettingLocationStateChange: (Boolean) -> Unit = {},
    appState: AppState,
    viewModel: IParksRootViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isGettingLocation) {
        onGettingLocationStateChange(uiState.isGettingLocation)
    }

    SetupPermissionLaunchers(viewModel)

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ParksRootEvent.NavigateToCreatePark -> onCreateParkClick(event.draft)
                is ParksRootEvent.OpenSettings -> {}
            }
        }
    }

    LocationPermissionDialog(
        visible = uiState.showPermissionDialog,
        permissionDialogCause = uiState.permissionDialogCause,
        onDismiss = viewModel::onDismissDialog,
        onConfirm = viewModel::onConfirmDialog,
        onOpenSettings = { intent -> viewModel.onOpenSettings(intent) }
    )

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            CreateParkFab(
                appState = appState,
                enabled = !uiState.isGettingLocation,
                onClick = {
                    val hasFineLocation = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    val hasCoarseLocation = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasFineLocation || hasCoarseLocation) {
                        viewModel.onPermissionGranted()
                    } else {
                        val activity = context as? android.app.Activity
                        val shouldShowRationale = activity != null &&
                            ActivityCompat.shouldShowRequestPermissionRationale(
                                activity,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        viewModel.onPermissionDenied(shouldShowRationale)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            ParksListView(
                modifier = Modifier.fillMaxSize(),
                parks = parks,
                onParkClick = onParkClick
            )

            if (uiState.isGettingLocation) {
                LoadingOverlayView()
            }
        }
    }
}

@Composable
private fun SetupPermissionLaunchers(viewModel: IParksRootViewModel) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.onPermissionResult(permissions)
    }

    val openSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    LaunchedEffect(Unit) {
        viewModel.permissionLauncher = { permissions ->
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        viewModel.openSettingsLauncher = { intent ->
            openSettingsLauncher.launch(intent)
        }
    }
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
    onFilterClick: () -> Unit = {},
    isFilterLoading: Boolean = false
) {
    CenterAlignedTopAppBar(
        title = {
            Text(text = stringResource(R.string.parks_title, parksCount.toString()))
        },
        actions = {
            IconButton(
                onClick = onFilterClick,
                enabled = !isFilterLoading
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
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    if (appState.isAuthorized) {
        FloatingActionButton(
            onClick = {
                if (!enabled) return@FloatingActionButton
                onClick()
            },
            containerColor = if (enabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Создать площадку"
            )
        }
    }
}
