package com.swparks.ui.screens.themeicon

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты расчёта сетки иконок [iconGridCells]: количество колонок от доступной
 * ширины, распределение остатка по ячейкам и усечение ячейки до целого пикселя,
 * чтобы FlowRow не переносил последнюю иконку ряда.
 */
class IconGridCellsTest {
    @Test
    fun iconGridCells_whenPhoneWidth_thenComputesFourColumns() {
        // 360dp экран − 32dp экранных отступов = 328dp: (328+8)/(64+8) = 4.55 → 4
        val cells = iconGridCells(maxWidth = 328.dp, spacing = 8.dp, minCellSize = 64.dp, density = 2.5f)

        assertEquals(4, cells.columns)
    }

    @Test
    fun iconGridCells_whenPhoneWidth_thenCellsDistributeLeftoverWidth() {
        // (328 − 8·3)/4 = 76dp на ячейку — ряд занимает всю ширину
        val cells = iconGridCells(maxWidth = 328.dp, spacing = 8.dp, minCellSize = 64.dp, density = 2.5f)

        assertEquals(76.dp, cells.cellSize)
    }

    @Test
    fun iconGridCells_whenFractionalCell_thenTruncatedToWholePixel() {
        // (385 − 8·4)/5 = 70.6dp = 211.8px @3 → toInt усекает до 211px
        val cells = iconGridCells(maxWidth = 385.dp, spacing = 8.dp, minCellSize = 64.dp, density = 3f)

        assertEquals(211f / 3f, cells.cellSize.value, 0.0001f)
    }

    @Test
    fun iconGridCells_whenFractionalCell_thenRowNeverOverflowsWidth() {
        // Инвариант против возврата бага «пустого столбца»: суммарная ширина
        // ячеек и отступов не превышает доступную ширину в пикселях
        val density = 3f
        val maxWidth = 385.dp
        val spacing = 8.dp

        val cells = iconGridCells(maxWidth, spacing, 64.dp, density)

        val columnsCount = cells.columns
        val rowWidthPx =
            cells.cellSize.value * density * columnsCount + spacing.value * density * (columnsCount - 1)
        assertTrue(
            "ряд $rowWidthPx px переполняет ширину ${maxWidth.value * density} px",
            rowWidthPx <= maxWidth.value * density
        )
    }

    @Test
    fun iconGridCells_whenNarrowerThanMinCell_thenSingleColumn() {
        // Узкое окно (split screen): (40+8)/(64+8) = 0.67 → 0, coerceAtLeast(1)
        val cells = iconGridCells(maxWidth = 40.dp, spacing = 8.dp, minCellSize = 64.dp, density = 2f)

        assertEquals(1, cells.columns)
        assertEquals(40.dp, cells.cellSize)
    }
}
