package com.swparks.ui.screens.photos

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import com.swparks.ui.ds.AsyncImageConfig
import com.swparks.ui.ds.SWAsyncImage
import kotlinx.coroutines.launch

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f

private fun clampOffset(
    rawOffset: Offset,
    currentScale: Float,
    minScale: Float,
    containerSize: IntSize
): Offset {
    if (currentScale <= minScale || containerSize == IntSize.Zero) return Offset.Zero

    val maxX = (containerSize.width * (currentScale - 1f)) / 2f
    val maxY = (containerSize.height * (currentScale - 1f)) / 2f

    return Offset(
        x = rawOffset.x.coerceIn(-maxX, maxX),
        y = rawOffset.y.coerceIn(-maxY, maxY)
    )
}

@Composable
fun ZoomablePhotoView(
    imageUrl: String,
    modifier: Modifier = Modifier,
    minScale: Float = MIN_SCALE,
    maxScale: Float = MAX_SCALE,
    doubleTapScale: Float = DOUBLE_TAP_SCALE,
    imageSize: Dp = Dp.Unspecified
) {
    val scope = rememberCoroutineScope()

    val scale = remember { Animatable(minScale) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scope.launch {
            val currentScale = scale.value
            val newScale = (currentScale * zoomChange).coerceIn(minScale, maxScale)
            val scaleRatio = if (currentScale == 0f) 1f else newScale / currentScale

            val rawOffset = if (newScale > minScale) {
                Offset(offsetX.value, offsetY.value) * scaleRatio + panChange
            } else {
                Offset.Zero
            }

            val clamped = clampOffset(rawOffset, newScale, minScale, containerSize)

            scale.snapTo(newScale)
            offsetX.snapTo(clamped.x)
            offsetY.snapTo(clamped.y)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(minScale, maxScale, doubleTapScale, containerSize) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        scope.launch {
                            if (scale.value > minScale) {
                                launch {
                                    scale.animateTo(
                                        targetValue = minScale,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                                launch {
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                                launch {
                                    offsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                            } else {
                                val targetScale = doubleTapScale.coerceIn(minScale, maxScale)

                                val targetOffset = if (containerSize != IntSize.Zero) {
                                    val center = Offset(
                                        x = containerSize.width / 2f,
                                        y = containerSize.height / 2f
                                    )
                                    clampOffset(
                                        rawOffset = (center - tapOffset) * (targetScale - 1f),
                                        currentScale = targetScale,
                                        minScale = minScale,
                                        containerSize = containerSize
                                    )
                                } else {
                                    Offset.Zero
                                }

                                launch {
                                    scale.animateTo(
                                        targetValue = targetScale,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                                launch {
                                    offsetX.animateTo(
                                        targetValue = targetOffset.x,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                                launch {
                                    offsetY.animateTo(
                                        targetValue = targetOffset.y,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                            }
                        }
                    }
                )
            }
            .transformable(state = transformableState),
        contentAlignment = Alignment.Center
    ) {
        SWAsyncImage(
            config = AsyncImageConfig(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale.value,
                        scaleY = scale.value,
                        translationX = offsetX.value,
                        translationY = offsetY.value
                    ),
                imageStringURL = imageUrl,
                size = imageSize,
                contentScale = ContentScale.Fit,
                showBorder = false
            )
        )
    }
}
