package com.swparks.ui.screens.photos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.swparks.R
import com.swparks.data.model.Photo
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.state.PhotoDetailAction
import com.swparks.ui.state.PhotoDetailUIState
import com.swparks.ui.theme.JetpackWorkoutAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(
    state: PhotoDetailUIState.Content,
    isAuthorized: Boolean,
    showDeleteDialog: Boolean,
    onAction: (PhotoDetailAction) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Black,
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = { onAction(PhotoDetailAction.Close) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        PhotoDetailTopAppBarActions(
                            isAuthor = state.isAuthor,
                            isAuthorized = isAuthorized,
                            onAction = onAction
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            ZoomablePhotoView(
                config = ZoomConfig(
                    imageUrl = state.photo.photo,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            )
        }

        if (state.isLoading) {
            LoadingOverlayView(modifier = Modifier.fillMaxSize())
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            onConfirm = { onAction(PhotoDetailAction.DeleteConfirm) },
            onDismiss = { onAction(PhotoDetailAction.DeleteDismiss) }
        )
    }
}


@Composable
private fun PhotoDetailTopAppBarActions(
    isAuthor: Boolean,
    isAuthorized: Boolean,
    onAction: (PhotoDetailAction) -> Unit
) {
    when {
        isAuthor -> {
            IconButton(onClick = { onAction(PhotoDetailAction.DeleteClick) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = Color.White
                )
            }
        }

        isAuthorized -> {
            IconButton(onClick = { onAction(PhotoDetailAction.Report) }) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = stringResource(R.string.report),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.event_delete_photo_confirm_title)) },
        text = { Text(text = stringResource(R.string.event_delete_photo_confirm_message)) },
        confirmButton = {
            TextButton(
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                onClick = onConfirm
            ) {
                Text(text = stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

private val previewPhoto = Photo(
    id = 1L,
    photo = "https://example.com/photo.jpg"
)

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Author - with delete button")
@Composable
internal fun PhotoDetailScreenAuthorPreview() {
    JetpackWorkoutAppTheme {
        Surface(color = Color.Black) {
            PhotoDetailScreen(
                state = PhotoDetailUIState.Content(
                    photo = previewPhoto,
                    parentTitle = "Тренировка",
                    isAuthor = true
                ),
                isAuthorized = true,
                showDeleteDialog = false,
                onAction = {}
            )
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF000000,
    name = "Authorized user - with report button"
)
@Composable
internal fun PhotoDetailScreenAuthorizedPreview() {
    JetpackWorkoutAppTheme {
        Surface(color = Color.Black) {
            PhotoDetailScreen(
                state = PhotoDetailUIState.Content(
                    photo = previewPhoto,
                    parentTitle = "Тренировка",
                    isAuthor = false
                ),
                isAuthorized = true,
                showDeleteDialog = false,
                onAction = {}
            )
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF000000,
    name = "Unauthorized - no action buttons"
)
@Composable
internal fun PhotoDetailScreenUnauthorizedPreview() {
    JetpackWorkoutAppTheme {
        Surface(color = Color.Black) {
            PhotoDetailScreen(
                state = PhotoDetailUIState.Content(
                    photo = previewPhoto,
                    parentTitle = "Тренировка",
                    isAuthor = false
                ),
                isAuthorized = false,
                showDeleteDialog = false,
                onAction = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "With delete dialog")
@Composable
internal fun PhotoDetailScreenWithDialogPreview() {
    JetpackWorkoutAppTheme {
        Surface(color = Color.Black) {
            PhotoDetailScreen(
                state = PhotoDetailUIState.Content(
                    photo = previewPhoto,
                    parentTitle = "Тренировка",
                    isAuthor = true
                ),
                isAuthorized = true,
                showDeleteDialog = true,
                onAction = {}
            )
        }
    }
}
