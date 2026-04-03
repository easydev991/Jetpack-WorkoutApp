package com.swparks.ui.ds

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.offset

/**
 * Рисует круг на заднем плане
 *
 * @param color Цвет круга
 * @param padding Расстояние от контента до круга по бокам
 *
 * @return Комбинированный [Modifier], который сначала рисует круг на заднем плане,
 * а потом выравнивает контент по центру
 */
fun Modifier.circleBackground(
    color: Color,
    padding: Dp
): Modifier {
    val backgroundModifier =
        drawBehind {
            drawCircle(
                color,
                size.width / 2f,
                center =
                    Offset(
                        size.width / 2f,
                        size.height / 2f
                    )
            )
        }
    val layoutModifier =
        layout { measurable, constraints ->
            val adjustedConstraints = constraints.offset(-padding.roundToPx())
            val placeable = measurable.measure(adjustedConstraints)
            val currentHeight = placeable.height
            val currentWidth = placeable.width
            val newDiameter =
                maxOf(
                    currentHeight,
                    currentWidth
                ) + padding.roundToPx() * 2
            layout(
                newDiameter,
                newDiameter
            ) {
                placeable.placeRelative(
                    (newDiameter - currentWidth) / 2,
                    (newDiameter - currentHeight) / 2
                )
            }
        }

    return this then backgroundModifier then layoutModifier
}
