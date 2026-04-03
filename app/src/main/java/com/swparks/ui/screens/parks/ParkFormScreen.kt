package com.swparks.ui.screens.parks

import android.net.Uri
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.swparks.R
import com.swparks.data.model.Park
import com.swparks.data.model.ParkSize
import com.swparks.data.model.ParkType
import com.swparks.ui.ds.ButtonConfig
import com.swparks.ui.ds.FormCardContainer
import com.swparks.ui.ds.FormCardContainerParams
import com.swparks.ui.ds.ImagePreviewDialog
import com.swparks.ui.ds.KeyboardAwareBottomBar
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.ds.PickedImagesGrid
import com.swparks.ui.ds.PickedImagesGridAction
import com.swparks.ui.ds.PickedImagesGridConfig
import com.swparks.ui.ds.SWButton
import com.swparks.ui.ds.SWButtonMode
import com.swparks.ui.ds.SWButtonSize
import com.swparks.ui.ds.SWRadioButton
import com.swparks.ui.ds.SWTextField
import com.swparks.ui.ds.TextFieldConfig
import com.swparks.ui.ds.rememberPickedImagesController
import com.swparks.ui.model.ParkFormMode
import com.swparks.ui.state.ParkFormEvent
import com.swparks.ui.viewmodel.IParkFormViewModel
import com.swparks.ui.viewmodel.ParkFormAction
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.material.icons.Icons.AutoMirrored.Filled as AutoMirroredIcons

sealed class ParkFormNavigationAction {
    data object Back : ParkFormNavigationAction()

    data class BackWithSavedPark(
        val park: Park
    ) : ParkFormNavigationAction()
}

private sealed class ParkFormDialogAction {
    data object OnDismissConfirmDialog : ParkFormDialogAction()

    data object OnConfirmClose : ParkFormDialogAction()

    data object OnDismissPreview : ParkFormDialogAction()

    data class OnDeletePhoto(
        val uri: Uri
    ) : ParkFormDialogAction()
}

private data class ParkFormContentParams(
    val form: com.swparks.ui.model.ParkForm,
    val isEnabled: Boolean,
    val selectedPhotos: List<Uri>,
    val maxNewPhotos: Int,
    val onAction: (ParkFormAction) -> Unit,
    val onAddPhotoClick: () -> Unit,
    val onPhotoRemove: (Uri) -> Unit,
    val onPhotoPreview: (Uri) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkFormScreen(
    modifier: Modifier = Modifier,
    viewModel: IParkFormViewModel,
    onAction: (ParkFormNavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var showConfirmDialog by remember { mutableStateOf(false) }
    var previewUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerController =
        rememberPickedImagesController(
            currentImageCount = uiState.selectedPhotos.size,
            selectionLimit = uiState.maxNewPhotos,
            onImagesSelected = viewModel::onPhotoSelected
        )

    HandleParkFormEvents(
        viewModel = viewModel,
        onAction = onAction,
        onShowPhotoPicker = { photoPickerController.launch() }
    )

    Scaffold(
        topBar = {
            ParkFormTopBar(
                titleRes = uiState.mode.navigationTitle,
                onBackClick = {
                    if (uiState.hasChanges) {
                        showConfirmDialog = true
                    } else {
                        onAction(ParkFormNavigationAction.Back)
                    }
                }
            )
        },
        bottomBar = {
            KeyboardAwareBottomBar {
                SaveButton(
                    enabled = uiState.canSave && !uiState.isSaving,
                    onClick = viewModel::onSaveClick
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            ParkFormContent(
                paddingValues = paddingValues,
                scrollState = scrollState,
                params =
                    ParkFormContentParams(
                        form = uiState.form,
                        isEnabled = !uiState.isSaving,
                        selectedPhotos = uiState.selectedPhotos,
                        maxNewPhotos = uiState.maxNewPhotos,
                        onAction = viewModel::onAction,
                        onAddPhotoClick = viewModel::onAddPhotoClick,
                        onPhotoRemove = viewModel::onPhotoRemove,
                        onPhotoPreview = { uri -> previewUri = uri }
                    )
            )

            if (uiState.isSaving || uiState.isGeocoding) {
                LoadingOverlayView()
            }
        }
    }

    ParkFormDialogs(
        showConfirmDialog = showConfirmDialog,
        previewUri = previewUri,
        onAction = { action ->
            when (action) {
                is ParkFormDialogAction.OnDismissConfirmDialog -> showConfirmDialog = false
                is ParkFormDialogAction.OnConfirmClose -> {
                    showConfirmDialog = false
                    onAction(ParkFormNavigationAction.Back)
                }

                is ParkFormDialogAction.OnDismissPreview -> previewUri = null
                is ParkFormDialogAction.OnDeletePhoto -> {
                    viewModel.onPhotoRemove(action.uri)
                    previewUri = null
                }
            }
        }
    )
}

private val ParkFormMode.navigationTitle: Int
    get() =
        when (this) {
            is ParkFormMode.Create -> R.string.new_park_title
            is ParkFormMode.Edit -> R.string.park_title
        }

@Composable
private fun HandleParkFormEvents(
    viewModel: IParkFormViewModel,
    onAction: (ParkFormNavigationAction) -> Unit,
    onShowPhotoPicker: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ParkFormEvent.Saved -> {
                    onAction(ParkFormNavigationAction.BackWithSavedPark(event.park))
                }

                is ParkFormEvent.NavigateBack -> onAction(ParkFormNavigationAction.Back)
                is ParkFormEvent.ShowPhotoPicker -> onShowPhotoPicker()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParkFormTopBar(
    titleRes: Int,
    onBackClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = { Text(text = stringResource(titleRes)) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = AutoMirroredIcons.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        }
    )
}

@Composable
private fun ParkFormContent(
    paddingValues: PaddingValues,
    scrollState: ScrollState,
    params: ParkFormContentParams
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(
                    horizontal = dimensionResource(R.dimen.spacing_regular),
                    vertical = dimensionResource(R.dimen.spacing_regular)
                ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
    ) {
        AddressField(
            value = params.form.address,
            enabled = params.isEnabled,
            onValueChange = { params.onAction(ParkFormAction.AddressChange(it)) }
        )

        ParkTypeSelectorSection(
            selectedTypeId = params.form.typeId,
            enabled = params.isEnabled,
            onTypeChange = { params.onAction(ParkFormAction.TypeChange(it)) }
        )

        ParkSizeSelectorSection(
            selectedSizeId = params.form.sizeId,
            enabled = params.isEnabled,
            onSizeChange = { params.onAction(ParkFormAction.SizeChange(it)) }
        )

        PickedImagesGrid(
            images = params.selectedPhotos,
            selectionLimit = params.maxNewPhotos,
            onAction = { action ->
                when (action) {
                    is PickedImagesGridAction.AddImage -> params.onAddPhotoClick()
                    is PickedImagesGridAction.RemoveImage ->
                        params.selectedPhotos.getOrNull(action.index)?.let(params.onPhotoRemove)

                    is PickedImagesGridAction.ViewImage -> params.onPhotoPreview(action.uri)
                }
            },
            config = PickedImagesGridConfig(enabled = params.isEnabled)
        )
    }
}

@Composable
private fun AddressField(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    SWTextField(
        config =
            TextFieldConfig(
                text = value,
                labelID = R.string.park_address,
                enabled = enabled,
                singleLine = true,
                onTextChange = onValueChange
            )
    )
}

@Composable
private fun ParkTypeSelectorSection(
    selectedTypeId: Int,
    enabled: Boolean,
    onTypeChange: (Int) -> Unit
) {
    FormCardContainer(
        params = FormCardContainerParams(enabled = enabled)
    ) {
        ParkTypeRadioButtons(
            selectedTypeId = selectedTypeId,
            enabled = enabled,
            onTypeChange = onTypeChange
        )
    }
}

@Composable
private fun ParkTypeRadioButtons(
    selectedTypeId: Int,
    enabled: Boolean,
    onTypeChange: (Int) -> Unit
) {
    Column(Modifier.padding(dimensionResource(R.dimen.spacing_small))) {
        Text(
            text = stringResource(R.string.park_type),
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            modifier = Modifier.padding(bottom = dimensionResource(R.dimen.spacing_xsmall))
        )
        Column(modifier = Modifier.selectableGroup()) {
            ParkType.entries.forEach { parkType ->
                SWRadioButton(
                    text = stringResource(parkType.description),
                    selected = selectedTypeId == parkType.rawValue,
                    onClick = { onTypeChange(parkType.rawValue) },
                    onClickable = enabled
                )
            }
        }
    }
}

@Composable
private fun ParkSizeSelectorSection(
    selectedSizeId: Int,
    enabled: Boolean,
    onSizeChange: (Int) -> Unit
) {
    FormCardContainer(
        params = FormCardContainerParams(enabled = enabled)
    ) {
        ParkSizeRadioButtons(
            selectedSizeId = selectedSizeId,
            enabled = enabled,
            onSizeChange = onSizeChange
        )
    }
}

@Composable
private fun ParkSizeRadioButtons(
    selectedSizeId: Int,
    enabled: Boolean,
    onSizeChange: (Int) -> Unit
) {
    Column(Modifier.padding(dimensionResource(R.dimen.spacing_small))) {
        Text(
            text = stringResource(R.string.park_size),
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            modifier = Modifier.padding(bottom = dimensionResource(R.dimen.spacing_xsmall))
        )
        Column(modifier = Modifier.selectableGroup()) {
            ParkSize.entries.forEach { parkSize ->
                SWRadioButton(
                    text = stringResource(parkSize.description),
                    selected = selectedSizeId == parkSize.rawValue,
                    onClick = { onSizeChange(parkSize.rawValue) },
                    onClickable = enabled
                )
            }
        }
    }
}

@Composable
private fun SaveButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    SWButton(
        config =
            ButtonConfig(
                mode = SWButtonMode.FILLED,
                size = SWButtonSize.LARGE,
                text = stringResource(R.string.save),
                enabled = enabled,
                onClick = onClick
            )
    )
}

@Composable
private fun ParkFormDialogs(
    showConfirmDialog: Boolean,
    previewUri: Uri?,
    onAction: (ParkFormDialogAction) -> Unit
) {
    if (showConfirmDialog) {
        ConfirmCloseDialog(
            onDismiss = { onAction(ParkFormDialogAction.OnDismissConfirmDialog) },
            onConfirm = { onAction(ParkFormDialogAction.OnConfirmClose) }
        )
    }

    previewUri?.let { uri ->
        ImagePreviewDialog(
            uri = uri,
            onDismiss = { onAction(ParkFormDialogAction.OnDismissPreview) },
            onDelete = { onAction(ParkFormDialogAction.OnDeletePhoto(uri)) }
        )
    }
}

@Composable
private fun ConfirmCloseDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.event_form_confirm_close_title))
        },
        text = {
            Text(text = stringResource(R.string.event_form_confirm_close_message))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.close),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}
