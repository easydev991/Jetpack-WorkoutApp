package com.workout.jetpack_workout.ui.ds

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.workout.jetpack_workout.R

@Composable
fun WorkoutAsyncImage(
    modifier: Modifier = Modifier,
    imageStringURL: String?,
    size: Dp,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    AsyncImage(
        model = ImageRequest.Builder(context = LocalContext.current)
            .data(imageStringURL)
            .crossfade(300)
            .build(),
        placeholder = painterResource(id = R.drawable.defaultworkout),
        contentDescription = "Preview",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size)
            .clip(shape)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = shape
            )
    )
}