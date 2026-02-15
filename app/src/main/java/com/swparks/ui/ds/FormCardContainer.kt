package com.swparks.ui.ds

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.dimensionResource
import com.swparks.R

/**
 * Обертка для добавления тени в светлой теме
 *
 * @param modifier Модификатор
 * @param enabled Возможность кликать по карточке
 * @param onClick Обработчик нажатия
 * @param onLongClick Обработчик долгого нажатия (без позиции)
 * @param onLongClickWithOffset Обработчик долгого нажатия с позицией нажатия
 * @param content Контент
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FormCardContainer(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onLongClickWithOffset: ((offsetX: Float, offsetY: Float) -> Unit)? = null,
    content: @Composable (ColumnScope.() -> Unit)
) {
    val isLight = MaterialTheme.colorScheme.isLight()
    val interactionSource = remember { MutableInteractionSource() }

    val cardModifier = when {
        onLongClickWithOffset != null && onClick != null -> {
            modifier.pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { offset ->
                            onLongClickWithOffset(offset.x, offset.y)
                        }
                    )
                }
            }
        }

        onLongClickWithOffset != null -> {
            modifier.pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onLongPress = { offset ->
                            onLongClickWithOffset(offset.x, offset.y)
                        }
                    )
                }
            }
        }

        onClick != null || onLongClick != null -> {
            modifier.combinedClickable(
                enabled = enabled,
                indication = null,
                interactionSource = interactionSource,
                onClick = onClick ?: {},
                onLongClick = onLongClick
            )
        }

        else -> modifier
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(dimensionResource(R.dimen.spacing_small)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(
            if (isLight)
                dimensionResource(R.dimen.elevation_small)
            else dimensionResource(R.dimen.elevation_none)
        )
    ) {
        content()
    }
}