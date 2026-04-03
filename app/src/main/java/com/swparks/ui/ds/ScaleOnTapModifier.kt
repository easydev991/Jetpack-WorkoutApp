package com.swparks.ui.ds

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

private enum class ButtonState {
    Pressed,
    Idle
}

/**
 * Изменяет размер вьюшки при нажатии
 */
fun Modifier.scaleOnTap() =
    composed {
        var buttonState by remember { mutableStateOf(ButtonState.Idle) }
        val animatedTranslation by animateFloatAsState(
            targetValue = if (buttonState == ButtonState.Pressed) 0.98f else 1f,
            animationSpec = tween(durationMillis = 100, easing = LinearEasing),
            label = "scaleOnTapAnimation"
        )

        this
            .graphicsLayer {
                scaleX = animatedTranslation
                scaleY = animatedTranslation
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { }
            )
            .pointerInput(buttonState) {
                awaitPointerEventScope {
                    buttonState =
                        if (buttonState == ButtonState.Pressed) {
                            waitForUpOrCancellation()
                            ButtonState.Idle
                        } else {
                            awaitFirstDown(false)
                            ButtonState.Pressed
                        }
                }
            }
    }
