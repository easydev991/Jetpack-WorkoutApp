package com.swparks.ui.ds

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.swparks.R
import com.swparks.data.model.Photo
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.utils.disabledAlpha

private const val ONE_COLUMN = 1
private const val TWO_COLUMNS = 2
private const val THREE_COLUMNS = 3

data class PhotoSectionConfig(
    val photos: List<Photo>,
    val enabled: Boolean = true,
    val onPhotoClick: (Photo) -> Unit
)

@Composable
fun PhotoSectionView(
    config: PhotoSectionConfig,
    modifier: Modifier = Modifier
) {
    if (config.photos.isEmpty()) return

    val columns = getColumnCount(config.photos.size)
    val spacing = dimensionResource(R.dimen.spacing_xsmall)

    SectionView(
        modifier = modifier,
        titleID = R.string.event_photos
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val itemSize = (maxWidth - spacing * (columns - 1)) / columns
            val rows = config.photos.chunked(columns)

            Column(
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        rowItems.forEach { photo ->
                            PhotoCell(
                                photo = photo,
                                size = itemSize,
                                enabled = config.enabled,
                                onPhotoClick = config.onPhotoClick
                            )
                        }

                        repeat(columns - rowItems.size) {
                            Spacer(modifier = Modifier.size(itemSize))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoCell(
    photo: Photo,
    size: Dp,
    enabled: Boolean,
    onPhotoClick: (Photo) -> Unit
) {
    Box(modifier = Modifier.size(size)) {
        SWAsyncImage(
            config = AsyncImageConfig(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = enabled) { onPhotoClick(photo) }
                    .disabledAlpha(!enabled),
                imageStringURL = photo.photo,
                size = size,
                contentScale = ContentScale.Crop,
                showBorder = false
            )
        )
    }
}

private fun getColumnCount(size: Int): Int = when (size) {
    ONE_COLUMN -> ONE_COLUMN
    TWO_COLUMNS -> TWO_COLUMNS
    else -> THREE_COLUMNS
}

@Preview(showBackground = true, locale = "ru")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
internal fun PhotoSectionViewOnePhotoPreview() {
    PhotoSectionPreviewContent(
        config = PhotoSectionConfig(
            photos = previewPhotos.take(1),
            onPhotoClick = {}
        )
    )
}

@Preview(showBackground = true, locale = "ru")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
internal fun PhotoSectionViewTwoPhotosPreview() {
    PhotoSectionPreviewContent(
        config = PhotoSectionConfig(
            photos = previewPhotos.take(2),
            onPhotoClick = {}
        )
    )
}

@Preview(showBackground = true, locale = "ru")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
internal fun PhotoSectionViewFourPhotosPreview() {
    PhotoSectionPreviewContent(
        config = PhotoSectionConfig(
            photos = previewPhotos,
            onPhotoClick = {}
        )
    )
}

@Composable
private fun PhotoSectionPreviewContent(config: PhotoSectionConfig) {
    JetpackWorkoutAppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
            ) {
                PhotoSectionView(config = config)
            }
        }
    }
}

private val previewPhotos = listOf(
    Photo(id = 1L, photo = "https://workout.su/files/trainings/photo1.jpg"),
    Photo(id = 2L, photo = "https://workout.su/files/trainings/photo2.jpg"),
    Photo(id = 3L, photo = "https://workout.su/files/trainings/photo3.jpg"),
    Photo(id = 4L, photo = "https://workout.su/files/trainings/photo4.jpg")
)
