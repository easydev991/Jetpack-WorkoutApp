package com.swparks.ui.screens.photos

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swparks.JetpackWorkoutApplication
import com.swparks.ui.screens.more.sendComplaint
import com.swparks.ui.state.PhotoDetailAction
import com.swparks.ui.state.PhotoDetailConfig
import com.swparks.ui.state.PhotoDetailEvent
import com.swparks.ui.state.PhotoDetailUIState
import com.swparks.ui.viewmodel.PhotoDetailViewModel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal fun buildPhotoDetailViewModelKey(config: PhotoDetailConfig): String =
    "photo_${config.ownerType.name}_${config.parentId}_${config.photoId}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailSheetHost(
    show: Boolean,
    config: PhotoDetailConfig,
    onDismissed: (deletedPhotoId: Long?) -> Unit
) {
    var allowHide by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val appContainer = (context.applicationContext as JetpackWorkoutApplication).container

    val viewModel: PhotoDetailViewModel = viewModel(
        key = buildPhotoDetailViewModelKey(config),
        factory = PhotoDetailViewModel.factoryWithConfig(
            config = config,
            swRepository = appContainer.swRepository,
            userPreferencesRepository = appContainer.userPreferencesRepository,
            logger = appContainer.logger,
            userNotifier = appContainer.userNotifier
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val isAuthorized by viewModel.isAuthorized.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            when (newValue) {
                SheetValue.Expanded -> true
                SheetValue.PartiallyExpanded -> false
                SheetValue.Hidden -> allowHide
            }
        }
    )

    fun dismissSheet(onComplete: () -> Unit) {
        scope.launch {
            allowHide = true
            sheetState.hide()
            allowHide = false
            onComplete()
        }
    }

    HandleEvents(viewModel, ::dismissSheet, context, onDismissed) { showDeleteDialog = it }

    if (show) {
        ModalBottomSheet(
            onDismissRequest = {},
            sheetState = sheetState,
            dragHandle = null,
            properties = ModalBottomSheetProperties(
                shouldDismissOnBackPress = false,
                shouldDismissOnClickOutside = false
            ),
        ) {
            SheetContent(
                uiState = uiState,
                isAuthorized = isAuthorized,
                showDeleteDialog = showDeleteDialog,
                onShowDeleteDialogChange = { showDeleteDialog = it },
                onAction = viewModel::onAction
            )
        }
    }
}

@Composable
private fun HandleEvents(
    viewModel: PhotoDetailViewModel,
    dismissSheet: (() -> Unit) -> Unit,
    context: android.content.Context,
    onDismissed: (Long?) -> Unit,
    onShowDeleteDialog: (Boolean) -> Unit
) {
    LaunchedEffect(viewModel) {
        viewModel.events.receiveAsFlow().collect { event ->
            when (event) {
                PhotoDetailEvent.CloseScreen -> dismissSheet { onDismissed(null) }
                PhotoDetailEvent.ShowDeleteConfirmDialog -> onShowDeleteDialog(true)
                is PhotoDetailEvent.SendPhotoComplaint -> sendComplaint(event.complaint, context)
                is PhotoDetailEvent.PhotoDeleted -> dismissSheet { onDismissed(event.photoId) }
            }
        }
    }
}


@Composable
private fun SheetContent(
    uiState: PhotoDetailUIState,
    isAuthorized: Boolean,
    showDeleteDialog: Boolean,
    onShowDeleteDialogChange: (Boolean) -> Unit,
    onAction: (PhotoDetailAction) -> Unit
) {
    when (val state = uiState) {
        is PhotoDetailUIState.Content -> {
            PhotoDetailScreen(
                state = state,
                isAuthorized = isAuthorized,
                showDeleteDialog = showDeleteDialog,
                onAction = { action ->
                    when (action) {
                        PhotoDetailAction.DeleteDismiss,
                        PhotoDetailAction.DeleteConfirm -> onShowDeleteDialogChange(false)

                        else -> Unit
                    }
                    onAction(action)
                }
            )
        }

        is PhotoDetailUIState.Error -> Unit
    }
}
