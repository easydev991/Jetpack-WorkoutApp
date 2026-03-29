package com.swparks.ui.screens.events

import android.net.Uri
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import com.swparks.data.model.Event
import com.swparks.ui.ds.ButtonConfig
import com.swparks.ui.ds.DateTimePickerConfig
import com.swparks.ui.ds.FormCardContainer
import com.swparks.ui.ds.FormCardContainerParams
import com.swparks.ui.ds.ImagePreviewDialog
import com.swparks.ui.ds.KeyboardAwareBottomBar
import com.swparks.ui.ds.ListRowData
import com.swparks.ui.ds.ListRowView
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.ds.PickedImagesGrid
import com.swparks.ui.ds.PickedImagesGridAction
import com.swparks.ui.ds.PickedImagesGridConfig
import com.swparks.ui.ds.SWButton
import com.swparks.ui.ds.SWButtonMode
import com.swparks.ui.ds.SWButtonSize
import com.swparks.ui.ds.SWDatePickerMode
import com.swparks.ui.ds.SWDateTimePicker
import com.swparks.ui.ds.SWTextEditor
import com.swparks.ui.ds.SWTextField
import com.swparks.ui.ds.TextFieldConfig
import com.swparks.ui.ds.rememberPickedImagesController
import com.swparks.ui.model.EventForm
import com.swparks.ui.state.EventFormEvent
import com.swparks.ui.viewmodel.EventFormAction
import com.swparks.ui.viewmodel.IEventFormViewModel
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import androidx.compose.material.icons.Icons.AutoMirrored.Filled as AutoMirroredIcons

sealed class EventFormNavigationAction {
    data object Back : EventFormNavigationAction()
    data class BackWithCreatedEvent(val event: Event) :
        EventFormNavigationAction()

    data class NavigateToSelectPark(val currentParkId: Long?) : EventFormNavigationAction()
}

private data class EventFormContentParams(
    val form: EventForm,
    val canSelectPark: Boolean,
    val isEnabled: Boolean,
    val selectedPhotos: List<Uri>,
    val maxNewPhotos: Int,
    val onAction: (EventFormAction) -> Unit,
    val onAddPhotoClick: () -> Unit,
    val onPhotoRemove: (Uri) -> Unit,
    val onPhotoPreview: (Uri) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormScreen(
    modifier: Modifier = Modifier,
    viewModel: IEventFormViewModel,
    onAction: (EventFormNavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var showConfirmDialog by remember { mutableStateOf(false) }
    var previewUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerController = rememberPickedImagesController(
        currentImageCount = uiState.selectedPhotos.size,
        selectionLimit = uiState.maxNewPhotos,
        onImagesSelected = viewModel::onPhotoSelected
    )

    HandleEventFormEvents(
        viewModel = viewModel,
        onAction = onAction,
        onShowPhotoPicker = { photoPickerController.launch() }
    )

    Scaffold(
        topBar = {
            EventFormTopBar(
                titleRes = uiState.mode.navigationTitle,
                onBackClick = {
                    if (uiState.hasChanges) {
                        showConfirmDialog = true
                    } else {
                        onAction(EventFormNavigationAction.Back)
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
            EventFormContent(
                paddingValues = paddingValues,
                scrollState = scrollState,
                params = EventFormContentParams(
                    form = uiState.form,
                    canSelectPark = uiState.canSelectPark,
                    isEnabled = !uiState.isSaving,
                    selectedPhotos = uiState.selectedPhotos,
                    maxNewPhotos = uiState.maxNewPhotos,
                    onAction = viewModel::onAction,
                    onAddPhotoClick = viewModel::onAddPhotoClick,
                    onPhotoRemove = viewModel::onPhotoRemove,
                    onPhotoPreview = { uri -> previewUri = uri }
                )
            )

            if (uiState.isSaving) {
                LoadingOverlayView()
            }
        }
    }

    if (showConfirmDialog) {
        ConfirmCloseDialog(
            onDismiss = { showConfirmDialog = false },
            onConfirm = {
                showConfirmDialog = false
                onAction(EventFormNavigationAction.Back)
            }
        )
    }

    previewUri?.let { uri ->
        ImagePreviewDialog(
            uri = uri,
            onDismiss = { previewUri = null },
            onDelete = {
                viewModel.onPhotoRemove(uri)
                previewUri = null
            }
        )
    }
}

@Composable
private fun HandleEventFormEvents(
    viewModel: IEventFormViewModel,
    onAction: (EventFormNavigationAction) -> Unit,
    onShowPhotoPicker: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is EventFormEvent.Saved -> {
                    onAction(EventFormNavigationAction.BackWithCreatedEvent(event.event))
                }

                is EventFormEvent.NavigateBack -> onAction(EventFormNavigationAction.Back)
                is EventFormEvent.NavigateToSelectPark -> onAction(
                    EventFormNavigationAction.NavigateToSelectPark(event.currentParkId)
                )

                is EventFormEvent.ShowDatePicker -> {}
                is EventFormEvent.ShowPhotoPicker -> onShowPhotoPicker()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventFormTopBar(
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
private fun EventFormContent(
    paddingValues: PaddingValues,
    scrollState: ScrollState,
    params: EventFormContentParams
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(scrollState)
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_regular),
                vertical = dimensionResource(R.dimen.spacing_regular)
            ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
    ) {
        Column {
            TitleField(
                value = params.form.title,
                enabled = params.isEnabled,
                onValueChange = { params.onAction(EventFormAction.TitleChange(it)) }
            )

            DescriptionField(
                value = params.form.description,
                enabled = params.isEnabled,
                onValueChange = { params.onAction(EventFormAction.DescriptionChange(it)) }
            )
        }

        ParkSelector(
            parkName = params.form.parkName,
            canSelectPark = params.canSelectPark,
            enabled = params.isEnabled,
            onClick = { params.onAction(EventFormAction.ParkClick) }
        )

        DatePickerSection(
            date = params.form.date,
            enabled = params.isEnabled,
            onDateChange = { params.onAction(EventFormAction.DateChange(it)) },
            onTimeChange = { h, m -> params.onAction(EventFormAction.TimeChange(h, m)) }
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
private fun TitleField(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    SWTextField(
        config = TextFieldConfig(
            text = value,
            labelID = R.string.event_form_name_label,
            enabled = enabled,
            singleLine = true,
            onTextChange = onValueChange
        )
    )
}

@Composable
private fun DescriptionField(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    SWTextEditor(
        text = value,
        labelID = R.string.event_form_description_label,
        enabled = enabled,
        onTextChange = onValueChange
    )
}

@Composable
private fun ParkSelector(
    parkName: String,
    canSelectPark: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    FormCardContainer(
        params = FormCardContainerParams(
            enabled = enabled && canSelectPark,
            onClick = onClick.takeIf { canSelectPark }
        )
    ) {
        ListRowView(
            data = ListRowData(
                modifier = Modifier.padding(dimensionResource(R.dimen.spacing_small)),
                leadingText = stringResource(R.string.event_form_park_label),
                trailingText = parkName.ifBlank { stringResource(R.string.event_form_park_select) },
                showChevron = canSelectPark,
                enabled = enabled
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSection(
    date: String,
    enabled: Boolean,
    onDateChange: (Long) -> Unit,
    onTimeChange: (Int, Int) -> Unit
) {
    val initialDateTime = remember(date) {
        parseEventDateTime(date) ?: LocalDateTime.now()
    }
    val initialTimestamp = remember(initialDateTime) {
        initialDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    val currentYear = LocalDate.now().year
    val maxYear = currentYear + 1

    FormCardContainer(
        params = FormCardContainerParams(enabled = enabled)
    ) {
        SWDateTimePicker(
            config = DateTimePickerConfig(
                modifier = Modifier.padding(dimensionResource(R.dimen.spacing_small)),
                mode = SWDatePickerMode.EVENT,
                initialSelectedDateMillis = initialTimestamp,
                initialHour = initialDateTime.hour,
                initialMinute = initialDateTime.minute,
                yearRange = currentYear..maxYear,
                enabled = enabled,
                onClickSaveDate = onDateChange,
                onClickSaveTime = onTimeChange
            )
        )
    }
}

private fun parseEventDateTime(value: String): LocalDateTime? {
    if (value.isBlank()) {
        return null
    }

    return try {
        when {
            value.contains('Z') || value.contains('+') -> {
                OffsetDateTime.parse(value)
                    .atZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDateTime()
            }

            value.contains('T') -> {
                LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            }

            value.contains(' ') -> {
                LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            }

            else -> {
                LocalDate.parse(value).atStartOfDay()
            }
        }
    } catch (_: DateTimeParseException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}

@Composable
private fun SaveButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    SWButton(
        config = ButtonConfig(
            mode = SWButtonMode.FILLED,
            size = SWButtonSize.LARGE,
            text = stringResource(R.string.event_form_save),
            enabled = enabled,
            onClick = onClick
        )
    )
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
