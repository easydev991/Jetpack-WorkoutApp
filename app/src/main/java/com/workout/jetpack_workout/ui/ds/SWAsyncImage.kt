package com.workout.jetpack_workout.ui.ds

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.workout.jetpack_workout.R
import com.workout.jetpack_workout.ui.theme.JetpackWorkoutAppTheme

@Composable
fun SWAsyncImage(
    modifier: Modifier = Modifier,
    imageStringURL: String?,
    size: Dp,
    contentScale: ContentScale = ContentScale.Fit,
    shape: Shape = RoundedCornerShape(12.dp),
    showBorder: Boolean = true
) {
    AsyncImage(
        model = ImageRequest.Builder(context = LocalContext.current)
            .data(imageStringURL)
            .crossfade(300)
            .build(),
        placeholder = painterResource(id = R.drawable.defaultworkout),
        error = painterResource(id = R.drawable.defaultworkout),
        contentDescription = "Preview",
        contentScale = contentScale,
        modifier = modifier
            .size(size)
            .clip(shape)
            .let {
                if (showBorder) {
                    return@let it.border(
                        width = 2.dp,
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
                    imageStringURL = null,
                    size = 42.dp
                )
                SWAsyncImage(
                    imageStringURL = null,
                    size = 42.dp,
                    shape = CircleShape
                )
                SWAsyncImage(
                    imageStringURL = null,
                    size = 74.dp,
                    showBorder = false
                )
                SWAsyncImage(
                    imageStringURL = null,
                    size = 150.dp
                )
            }
        }
    }
}