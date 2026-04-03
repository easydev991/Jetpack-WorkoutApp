package com.swparks.ui.ds

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.swparks.R
import com.swparks.ui.model.PickedImageItem

sealed class PickedImageCellAction {
    data object AddImage : PickedImageCellAction()

    data class DeleteImage(
        val index: Int
    ) : PickedImageCellAction()

    data class ViewFullscreen(
        val uri: Uri,
        val index: Int
    ) : PickedImageCellAction()
}

@Composable
fun PickedImageCell(
    item: PickedImageItem,
    index: Int,
    enabled: Boolean,
    onAction: (PickedImageCellAction) -> Unit,
    modifier: Modifier = Modifier
) {
    when (item) {
        is PickedImageItem.Image ->
            ImageCell(
                uri = item.uri,
                index = index,
                enabled = enabled,
                onAction = onAction,
                modifier = modifier
            )

        PickedImageItem.AddButton ->
            AddButtonCell(
                enabled = enabled,
                onClick = { onAction(PickedImageCellAction.AddImage) },
                modifier = modifier
            )
    }
}

@Composable
private fun ImageCell(
    uri: Uri,
    index: Int,
    enabled: Boolean,
    onAction: (PickedImageCellAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val cornerRadius = dimensionResource(R.dimen.corner_radius_small)

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(cornerRadius))
                .clickable(enabled = enabled) { menuExpanded = true }
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.defaultworkout),
            error = painterResource(R.drawable.defaultworkout),
            modifier = Modifier.fillMaxSize()
        )

        ImageContextMenu(
            expanded = menuExpanded,
            onDismiss = { menuExpanded = false },
            onViewFullscreen = {
                menuExpanded = false
                onAction(PickedImageCellAction.ViewFullscreen(uri, index))
            },
            onDelete = {
                menuExpanded = false
                onAction(PickedImageCellAction.DeleteImage(index))
            }
        )
    }
}

@Composable
private fun ImageContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onViewFullscreen: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.photos_view_fullscreen),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Visibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            },
            onClick = onViewFullscreen
        )
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            onClick = onDelete
        )
    }
}

@Composable
private fun AddButtonCell(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cornerRadius = dimensionResource(R.dimen.corner_radius_small)

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = stringResource(R.string.photos_add),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(AddButtonIconSize)
        )
    }
}

private val AddButtonIconSize = 32.dp
