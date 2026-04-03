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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.dimensionResource
import com.swparks.R

private const val DISABLED_CARD_ALPHA = 0.5f

/**
 * Параметры для FormCardContainer
 */
data class FormCardContainerParams(
    val modifier: Modifier = Modifier,
    val enabled: Boolean = true,
    val onClick: (() -> Unit)? = null,
    val onLongClick: (() -> Unit)? = null,
    val onLongClickWithOffset: ((offsetX: Float, offsetY: Float) -> Unit)? = null
)

/**
 * Обертка для добавления тени в светлой теме
 *
 * @param params Параметры карточки
 * @param content Контент
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FormCardContainer(
    params: FormCardContainerParams,
    content: @Composable (ColumnScope.() -> Unit)
) {
    val isLight = MaterialTheme.colorScheme.isLight()
    val interactionSource = remember { MutableInteractionSource() }
    val onClick = if (params.enabled) params.onClick else null
    val onLongClick = if (params.enabled) params.onLongClick else null
    val onLongClickWithOffset = if (params.enabled) params.onLongClickWithOffset else null

    val cardModifier =
        when {
            onLongClickWithOffset != null && onClick != null -> {
                params.modifier.pointerInput(params.enabled) {
                    if (params.enabled) {
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
                params.modifier.pointerInput(params.enabled) {
                    if (params.enabled) {
                        detectTapGestures(
                            onLongPress = { offset ->
                                onLongClickWithOffset(offset.x, offset.y)
                            }
                        )
                    }
                }
            }

            onClick != null || onLongClick != null -> {
                params.modifier.combinedClickable(
                    enabled = params.enabled,
                    interactionSource = interactionSource,
                    onClick = onClick ?: {},
                    onLongClick = onLongClick
                )
            }

            else -> params.modifier
        }

    Card(
        modifier = cardModifier.alpha(if (params.enabled) 1f else DISABLED_CARD_ALPHA),
        shape = RoundedCornerShape(dimensionResource(R.dimen.spacing_small)),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
        elevation =
            CardDefaults.cardElevation(
                if (isLight) {
                    dimensionResource(R.dimen.elevation_small)
                } else {
                    dimensionResource(R.dimen.elevation_none)
                }
            )
    ) {
        content()
    }
}
