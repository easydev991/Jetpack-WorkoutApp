package com.swparks.ui.screens.parks

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.swparks.R
import com.swparks.data.model.NewParkDraft
import com.swparks.data.model.Park
import com.swparks.navigation.AppState
import com.swparks.ui.components.SearchCityButton
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.ds.ParksListView
import com.swparks.ui.model.ParksTab
import com.swparks.ui.state.MapEvent
import com.swparks.ui.state.UiCoordinates
import com.swparks.ui.viewmodel.IParksRootViewModel
import com.swparks.ui.viewmodel.ParksRootEvent
import com.swparks.ui.viewmodel.PermissionDialogCause
import com.swparks.ui.viewmodel.isSizeTypeFilterEdited
import com.swparks.ui.viewmodel.showNoParksFound
import kotlinx.coroutines.flow.collectLatest

private const val PARK_INFO_CARD_ANIMATION_DURATION_MS = 220

@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
@Composable
fun ParksRootScreen(
    modifier: Modifier = Modifier,
    parks: List<Park>,
    onParkClick: (Park) -> Unit = {},
    onCreateParkClick: (NewParkDraft) -> Unit = {},
    onGettingLocationStateChange: (Boolean) -> Unit = {},
    onNavigateToSelectCity: () -> Unit = {},
    appState: AppState,
    viewModel: IParksRootViewModel
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val uiState by viewModel.uiState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    var parkInfoCardHeightPx by remember { mutableIntStateOf(0) }
    val mapLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onMapEvent(MapEvent.OnLocationPermissionResult(granted))
        if (granted) {
            viewModel.onMapEvent(MapEvent.CenterOnUser)
        }
    }

    LaunchedEffect(uiState.isGettingLocation) {
        onGettingLocationStateChange(uiState.isGettingLocation)
    }

    LaunchedEffect(Unit) {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val granted = hasFineLocation || hasCoarseLocation
        if (uiState.mapState.locationPermissionGranted != granted) {
            viewModel.onMapEvent(MapEvent.OnLocationPermissionResult(granted))
        }
    }

    SetupPermissionLaunchers(viewModel)

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ParksRootEvent.NavigateToCreatePark -> onCreateParkClick(event.draft)
                is ParksRootEvent.OpenSettings -> {}
                is ParksRootEvent.ResolveLocationSettings -> {
                    viewModel.resolveLocationSettingsLauncher?.invoke(event.intentSender)
                }
            }
        }
    }

    LaunchedEffect(parks) {
        viewModel.updateParks(parks)
    }

    LocationPermissionDialog(
        visible = uiState.showPermissionDialog,
        permissionDialogCause = uiState.permissionDialogCause,
        onDismiss = viewModel::onDismissDialog,
        onConfirm = viewModel::onConfirmDialog,
        onOpenSettings = { intent -> viewModel.onOpenSettings(intent) }
    )

    val selectedTabIndex = selectedTab.ordinal
    val isParkInfoCardVisible =
        selectedTab == ParksTab.MAP && uiState.mapState.selectedParkId != null
    val fabCardGap = dimensionResource(R.dimen.spacing_regular)
    val createParkFabBottomOffsetTarget = if (isParkInfoCardVisible && parkInfoCardHeightPx > 0) {
        with(density) { parkInfoCardHeightPx.toDp() } + fabCardGap
    } else {
        0.dp
    }
    val createParkFabBottomOffset by animateDpAsState(
        targetValue = createParkFabBottomOffsetTarget,
        animationSpec = tween(durationMillis = PARK_INFO_CARD_ANIMATION_DURATION_MS),
        label = "create_park_fab_bottom_offset"
    )
    val selectedCityCenter = uiState.selectedCity?.let { city ->
        val latitude = city.lat.toDoubleOrNull()
        val longitude = city.lon.toDoubleOrNull()
        if (latitude != null && longitude != null) {
            UiCoordinates(latitude = latitude, longitude = longitude)
        } else {
            null
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            CreateParkFab(
                appState = appState,
                enabled = !uiState.isGettingLocation,
                modifier = Modifier.padding(bottom = createParkFabBottomOffset),
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
                        val activity = context as? Activity
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
            ) {
                ParksTabRow(
                    selectedTabIndex = selectedTabIndex,
                    isGettingLocation = uiState.isGettingLocation,
                    onTabSelected = viewModel::onTabSelected
                )
                SearchCityButton(
                    cityName = uiState.selectedCity?.name,
                    onClick = onNavigateToSelectCity,
                    onClearClick = if (uiState.selectedCity != null) {
                        viewModel::onClearCityFilter
                    } else {
                        null
                    }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    ParkMapView(
                        parks = uiState.filteredParks,
                        selectedParkId = uiState.mapState.selectedParkId,
                        selectedCityCenter = selectedCityCenter,
                        cameraPosition = uiState.mapState.cameraPosition,
                        onMapEvent = viewModel::onMapEvent,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(if (selectedTab == ParksTab.MAP && !uiState.showNoParksFound) 1f else 0f)
                    )

                    if (selectedTab == ParksTab.MAP) {
                        if (uiState.showNoParksFound) {
                            NoParksFoundView(
                                onSelectCity = onNavigateToSelectCity,
                                onOpenFilters = viewModel::onShowFilterDialog,
                                isSizeTypeFilterEdited = uiState.isSizeTypeFilterEdited
                            )
                        } else {
                            val selectedPark = uiState.mapState.selectedParkId?.let { parkId ->
                                uiState.filteredParks.find { it.id == parkId }
                            }
                            ParkSelectionAnimatedVisibility(
                                visible = selectedPark != null,
                                modifier = Modifier.align(Alignment.BottomCenter),
                                enter = fadeIn(
                                    animationSpec = tween(durationMillis = PARK_INFO_CARD_ANIMATION_DURATION_MS)
                                ) + slideInVertically(
                                    animationSpec = tween(durationMillis = PARK_INFO_CARD_ANIMATION_DURATION_MS),
                                    initialOffsetY = { fullHeight -> fullHeight / 2 }
                                ),
                                exit = fadeOut(
                                    animationSpec = tween(durationMillis = PARK_INFO_CARD_ANIMATION_DURATION_MS)
                                ) + slideOutVertically(
                                    animationSpec = tween(durationMillis = PARK_INFO_CARD_ANIMATION_DURATION_MS),
                                    targetOffsetY = { fullHeight -> fullHeight / 2 }
                                )
                            ) {
                                selectedPark?.let { park ->
                                    ParkInfoCard(
                                        park = park,
                                        onDetailsClick = onParkClick,
                                        onDismiss = { viewModel.onMapEvent(MapEvent.ClearSelection) },
                                        modifier = Modifier
                                            .onGloballyPositioned { coordinates ->
                                                parkInfoCardHeightPx = coordinates.size.height
                                            }
                                            .padding(16.dp)
                                    )
                                }
                            }

                            MyLocationFab(
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
                                        if (!uiState.mapState.locationPermissionGranted) {
                                            viewModel.onMapEvent(
                                                MapEvent.OnLocationPermissionResult(
                                                    true
                                                )
                                            )
                                        }
                                        viewModel.onMapEvent(MapEvent.CenterOnUser)
                                    } else {
                                        mapLocationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                },
                                isLoading = uiState.mapState.isLoadingLocation,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(dimensionResource(R.dimen.spacing_regular))
                            )
                        }
                    }

                    if (selectedTab == ParksTab.LIST) {
                        if (uiState.showNoParksFound) {
                            NoParksFoundView(
                                onSelectCity = onNavigateToSelectCity,
                                onOpenFilters = viewModel::onShowFilterDialog,
                                isSizeTypeFilterEdited = uiState.isSizeTypeFilterEdited
                            )
                        } else {
                            ParksListView(
                                modifier = Modifier.fillMaxSize(),
                                parks = uiState.filteredParks,
                                onParkClick = onParkClick
                            )
                        }
                    }
                }
            }

            if (uiState.isGettingLocation) {
                LoadingOverlayView()
            }
        }
    }
}

@Composable
private fun ParkSelectionAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: androidx.compose.animation.EnterTransition,
    exit: androidx.compose.animation.ExitTransition,
    content: @Composable androidx.compose.animation.AnimatedVisibilityScope.() -> Unit
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
        exit = exit,
        content = content
    )
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

    val resolveLocationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onLocationSettingsResolutionResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(Unit) {
        viewModel.permissionLauncher = { _ ->
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
        viewModel.resolveLocationSettingsLauncher = { intentSender ->
            resolveLocationSettingsLauncher.launch(
                IntentSenderRequest.Builder(intentSender).build()
            )
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    if (appState.isAuthorized) {
        FloatingActionButton(
            modifier = modifier,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParksTabRow(
    selectedTabIndex: Int,
    isGettingLocation: Boolean,
    onTabSelected: (ParksTab) -> Unit,
    modifier: Modifier = Modifier
) {
    PrimaryTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(id = R.dimen.spacing_regular),
                end = dimensionResource(id = R.dimen.spacing_regular),
                top = dimensionResource(id = R.dimen.spacing_small)
            )
    ) {
        ParksTab.entries.forEachIndexed { index, parksTab ->
            Tab(
                selected = selectedTabIndex == index,
                enabled = !isGettingLocation,
                onClick = { onTabSelected(parksTab) },
                text = {
                    Text(
                        text = stringResource(id = parksTab.description)
                    )
                }
            )
        }
    }
}
