package com.swparks.ui.screens.photos

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomablePhotoViewMathTest {
    private val container = IntSize(1000, 1000)

    @Test
    fun calculateZoomOffset_whenCentroidIsCenterAndNoPan_thenReturnsScaledOffset() {
        val currentOffset = Offset(100f, 200f)
        val scaleRatio = 2f / 1f

        val result =
            calculateZoomOffset(
                currentOffset = currentOffset,
                currentScale = 1f,
                newScale = 2f,
                minScale = 1f,
                centroid = Offset(container.width / 2f, container.height / 2f),
                pan = Offset.Zero,
                containerSize = container
            )

        // Centroid at center: no shift, offset scales by ratio
        assertEquals(currentOffset.x * scaleRatio, result.x, 0.01f)
        assertEquals(currentOffset.y * scaleRatio, result.y, 0.01f)
    }

    @Test
    fun calculateZoomOffset_whenCentroidIsTopLeft_thenShiftsImageTowardsBottomRight() {
        val result =
            calculateZoomOffset(
                currentOffset = Offset.Zero,
                currentScale = 1f,
                newScale = 2f,
                minScale = 1f,
                centroid = Offset(0f, 0f),
                pan = Offset.Zero,
                containerSize = container
            )

        assertTrue("Expected positive x (shift right), got ${result.x}", result.x > 0f)
        assertTrue("Expected positive y (shift down), got ${result.y}", result.y > 0f)
    }

    @Test
    fun calculateZoomOffset_whenCentroidIsBottomRight_thenShiftsImageTowardsTopLeft() {
        val result =
            calculateZoomOffset(
                currentOffset = Offset.Zero,
                currentScale = 1f,
                newScale = 2f,
                minScale = 1f,
                centroid = Offset(container.width.toFloat(), container.height.toFloat()),
                pan = Offset.Zero,
                containerSize = container
            )

        assertTrue("Expected negative x (shift left), got ${result.x}", result.x < 0f)
        assertTrue("Expected negative y (shift up), got ${result.y}", result.y < 0f)
    }

    @Test
    fun clampOffset_whenScaleIsMin_returnsZero() {
        val result =
            clampOffset(
                rawOffset = Offset(500f, 500f),
                currentScale = 1f,
                minScale = 1f,
                containerSize = container
            )

        assertEquals(Offset.Zero, result)
    }

    @Test
    fun clampOffset_whenScaleExceedsMin_clampsToMaxBounds() {
        val scale = 3f
        val result =
            clampOffset(
                rawOffset = Offset(9999f, -9999f),
                currentScale = scale,
                minScale = 1f,
                containerSize = container
            )

        val maxX = (container.width * (scale - 1f)) / 2f
        val maxY = (container.height * (scale - 1f)) / 2f

        assertEquals(maxX, result.x, 0.01f)
        assertEquals(-maxY, result.y, 0.01f)
    }

    @Test
    fun calculateDoubleTapOffset_whenTapIsCenter_returnsZero() {
        val center = Offset(container.width / 2f, container.height / 2f)

        val result =
            calculateTargetOffset(
                containerSize = container,
                tapOffset = center,
                targetScale = 2.5f,
                minScale = 1f
            )

        assertEquals(Offset.Zero.x, result.x, 0.01f)
        assertEquals(Offset.Zero.y, result.y, 0.01f)
    }

    @Test
    fun calculateDoubleTapOffset_whenTapIsCorner_returnsCorrectOffset() {
        val tapOffset = Offset(0f, 0f)
        val targetScale = 2.5f

        val result =
            calculateTargetOffset(
                containerSize = container,
                tapOffset = tapOffset,
                targetScale = targetScale,
                minScale = 1f
            )

        // (center - tap) * (targetScale - 1) = (500, 500) * 1.5 = (750, 750)
        // Clamped to ±(1000 * 1.5) / 2 = ±750
        assertEquals(750f, result.x, 0.01f)
        assertEquals(750f, result.y, 0.01f)
    }
}
