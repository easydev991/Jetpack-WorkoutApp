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

    val cardModifier = when {
        params.onLongClickWithOffset != null && params.onClick != null -> {
            params.modifier.pointerInput(params.enabled) {
                if (params.enabled) {
                    detectTapGestures(
                        onTap = { params.onClick() },
                        onLongPress = { offset ->
                            params.onLongClickWithOffset(offset.x, offset.y)
                        }
                    )
                }
            }
        }

        params.onLongClickWithOffset != null -> {
            params.modifier.pointerInput(params.enabled) {
                if (params.enabled) {
                    detectTapGestures(
                        onLongPress = { offset ->
                            params.onLongClickWithOffset(offset.x, offset.y)
                        }
                    )
                }
            }
        }

        params.onClick != null || params.onLongClick != null -> {
            params.modifier.combinedClickable(
                enabled = params.enabled,
                indication = null,
                interactionSource = interactionSource,
                onClick = params.onClick ?: {},
                onLongClick = params.onLongClick
            )
        }

        else -> params.modifier
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