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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f

private data class ImageTransformParams(
    val imageUrl: String,
    val imageSize: Dp,
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float
)

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

private val zoomAnimationSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium
)

private class ZoomState(
    val scale: Animatable<Float, *>,
    val offsetX: Animatable<Float, *>,
    val offsetY: Animatable<Float, *>,
    val minScale: Float,
    val maxScale: Float
) {
    val isZoomed: Boolean get() = scale.value > minScale
}

private fun CoroutineScope.animateResetZoom(state: ZoomState) {
    launch { state.scale.animateTo(state.minScale, zoomAnimationSpec) }
    launch { state.offsetX.animateTo(0f, zoomAnimationSpec) }
    launch { state.offsetY.animateTo(0f, zoomAnimationSpec) }
}

private fun CoroutineScope.animateZoomToTarget(
    state: ZoomState,
    targetScale: Float,
    targetOffset: Offset
) {
    launch { state.scale.animateTo(targetScale, zoomAnimationSpec) }
    launch { state.offsetX.animateTo(targetOffset.x, zoomAnimationSpec) }
    launch { state.offsetY.animateTo(targetOffset.y, zoomAnimationSpec) }
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

    val zoomState = remember { ZoomState(scale, offsetX, offsetY, minScale, maxScale) }

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
                        handleDoubleTap(
                            scope = scope,
                            zoomState = zoomState,
                            doubleTapScale = doubleTapScale,
                            containerSize = containerSize,
                            tapOffset = tapOffset
                        )
                    }
                )
            }
            .transformable(state = transformableState),
        contentAlignment = Alignment.Center
    ) {
        TransformableImage(
            params = ImageTransformParams(
                imageUrl = imageUrl,
                imageSize = imageSize,
                scale = scale.value,
                offsetX = offsetX.value,
                offsetY = offsetY.value
            )
        )
    }
}

private fun handleDoubleTap(
    scope: CoroutineScope,
    zoomState: ZoomState,
    doubleTapScale: Float,
    containerSize: IntSize,
    tapOffset: Offset
) {
    scope.launch {
        if (zoomState.isZoomed) {
            scope.animateResetZoom(zoomState)
        } else {
            val targetScale = doubleTapScale.coerceIn(zoomState.minScale, zoomState.maxScale)
            val targetOffset = calculateTargetOffset(
                containerSize = containerSize,
                tapOffset = tapOffset,
                targetScale = targetScale,
                minScale = zoomState.minScale
            )
            scope.animateZoomToTarget(zoomState, targetScale, targetOffset)
        }
    }
}

private fun calculateTargetOffset(
    containerSize: IntSize,
    tapOffset: Offset,
    targetScale: Float,
    minScale: Float
): Offset {
    if (containerSize == IntSize.Zero) return Offset.Zero
    val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
    return clampOffset(
        rawOffset = (center - tapOffset) * (targetScale - 1f),
        currentScale = targetScale,
        minScale = minScale,
        containerSize = containerSize
    )
}

@Composable
private fun TransformableImage(
    params: ImageTransformParams,
    modifier: Modifier = Modifier
) {
    SWAsyncImage(
        config = AsyncImageConfig(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = params.scale,
                    scaleY = params.scale,
                    translationX = params.offsetX,
                    translationY = params.offsetY
                ),
            imageStringURL = params.imageUrl,
            size = params.imageSize,
            contentScale = ContentScale.Fit,
            showBorder = false
        )
    )
}
