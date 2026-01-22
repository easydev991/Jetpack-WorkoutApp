package com.swparks.ui.ds

import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.swparks.R
import com.swparks.ui.theme.JetpackWorkoutAppTheme

/**
 * Конфигурация для асинхронной загрузки картинки
 *
 * @property modifier Модификатор
 * @property imageStringURL Ссылка на картинку
 * @property size Размер картинки
 * @property contentScale Как уместить картинку (fit/crop...)
 * @property shape Форма вьюшки
 * @property showBorder Нужно ли показывать зеленую рамку
 */
data class AsyncImageConfig(
    val modifier: Modifier = Modifier,
    val imageStringURL: String?,
    val size: Dp,
    val contentScale: ContentScale = ContentScale.Fit,
    val shape: Shape? = null,
    val showBorder: Boolean = true
)

/**
 * Вьюшка для асинхронной загрузки картинки, использует [coil]
 *
 * @param config Конфигурация для отображения - [AsyncImageConfig]
 */
@Composable
fun SWAsyncImage(config: AsyncImageConfig) {
    val context = LocalContext.current
    val defaultShape = RoundedCornerShape(dimensionResource(id = R.dimen.spacing_xsmall))
    val shape = config.shape ?: defaultShape
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(config.imageStringURL)
            .crossfade(context.resources.getInteger(R.integer.crossfade_duration_ms))
            .build(),
        placeholder = painterResource(id = R.drawable.defaultworkout),
        error = painterResource(id = R.drawable.defaultworkout),
        contentDescription = "Preview",
        contentScale = config.contentScale,
        modifier = config.modifier
            .size(config.size)
            .clip(shape)
            .let {
                if (config.showBorder) {
                    return@let it.border(
                        width = dimensionResource(id = R.dimen.border_width),
                        color = MaterialTheme.colorScheme.primary,
                        shape = shape
                    )
                }
                it
            }
    )
}

@Preview(showBackground = true)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
fun SWAsyncImagePreview() {
    JetpackWorkoutAppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SWAsyncImage(
                    config = AsyncImageConfig(
                        imageStringURL = null,
                        size = 42.dp
                    )
                )
                SWAsyncImage(
                    config = AsyncImageConfig(
                        imageStringURL = null,
                        size = 42.dp,
                        shape = CircleShape
                    )
                )
                SWAsyncImage(
                    config = AsyncImageConfig(
                        imageStringURL = null,
                        size = 74.dp,
                        showBorder = false
                    )
                )
                SWAsyncImage(
                    config = AsyncImageConfig(
                        imageStringURL = null,
                        size = 150.dp
                    )
                )
            }
        }
    }
}