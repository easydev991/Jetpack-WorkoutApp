package com.swparks.ui.ds

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.swparks.R
import com.swparks.ui.model.PickedImageItem
import com.swparks.ui.model.PickedImagesState
import com.swparks.ui.theme.JetpackWorkoutAppTheme

private const val GRID_COLUMNS_COUNT = 3
private const val PREVIEW_SELECTION_LIMIT = 15

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PickedImagesGrid(
    images: List<Uri>,
    selectionLimit: Int,
    onAddClick: () -> Unit,
    onRemoveClick: (index: Int) -> Unit,
    onImageClick: (uri: Uri, index: Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showTitle: Boolean = true
) {
    val state = PickedImagesState(images = images, selectionLimit = selectionLimit)
    val items = buildItemsList(state)
    val spacing = dimensionResource(R.dimen.spacing_small)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        if (showTitle) {
            Text(
                text = if (images.isEmpty()) {
                    stringResource(R.string.photos_title)
                } else {
                    pluralStringResource(R.plurals.photoSectionHeader, images.size, images.size)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SubtitleText(state = state)

        if (items.isNotEmpty()) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val density = LocalDensity.current
                val spacingPx = with(density) { spacing.roundToPx() }
                val maxWidthPx = with(density) { maxWidth.roundToPx() }
                val itemWidthPx =
                    (maxWidthPx - spacingPx * (GRID_COLUMNS_COUNT - 1)) / GRID_COLUMNS_COUNT
                val itemWidthDp = with(density) { itemWidthPx.toDp() }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.Start),
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    items.forEachIndexed { index, item ->
                        PickedImageCell(
                            item = item,
                            index = if (item is PickedImageItem.Image) index else -1,
                            enabled = enabled,
                            onAction = { action ->
                                when (action) {
                                    is PickedImageCellAction.AddImage -> onAddClick()
                                    is PickedImageCellAction.DeleteImage -> onRemoveClick(action.index)
                                    is PickedImageCellAction.ViewFullscreen -> onImageClick(
                                        action.uri,
                                        action.index
                                    )
                                }
                            },
                            modifier = Modifier
                                .width(itemWidthDp)
                                .aspectRatio(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtitleText(state: PickedImagesState) {
    val subtitle = when {
        state.remainingSlots == 0 -> stringResource(R.string.photos_max_reached)
        state.images.isEmpty() -> stringResource(
            R.string.photos_add_subtitle_empty,
            state.selectionLimit
        )

        else -> stringResource(R.string.photos_add_subtitle_more, state.remainingSlots)
    }

    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun buildItemsList(state: PickedImagesState): List<PickedImageItem> {
    val items = mutableListOf<PickedImageItem>()
    items.addAll(state.images.map { PickedImageItem.Image(it) })
    if (state.canAddMore) {
        items.add(PickedImageItem.AddButton)
    }
    return items
}

@Preview(showBackground = true, locale = "ru")
@Composable
private fun PickedImagesGridEmptyPreview() {
    JetpackWorkoutAppTheme {
        PickedImagesGrid(
            images = emptyList(),
            selectionLimit = PREVIEW_SELECTION_LIMIT,
            onAddClick = {},
            onRemoveClick = {},
            onImageClick = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
private fun PickedImagesGridWithImagesPreview() {
    JetpackWorkoutAppTheme {
        PaddedPreview {
            PickedImagesGrid(
                images = listOf(
                    "content://media/1".toUri(),
                    "content://media/2".toUri(),
                    "content://media/3".toUri()
                ),
                selectionLimit = PREVIEW_SELECTION_LIMIT,
                onAddClick = {},
                onRemoveClick = {},
                onImageClick = { _, _ -> }
            )
        }
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
private fun PickedImagesGridFullPreview() {
    JetpackWorkoutAppTheme {
        PaddedPreview {
            PickedImagesGrid(
                images = List(PREVIEW_SELECTION_LIMIT) { index -> "content://media/$index".toUri() },
                selectionLimit = PREVIEW_SELECTION_LIMIT,
                onAddClick = {},
                onRemoveClick = {},
                onImageClick = { _, _ -> }
            )
        }
    }
}

@Composable
private fun PaddedPreview(content: @Composable () -> Unit) {
    Box(modifier = Modifier.padding(16.dp)) {
        content()
    }
}
