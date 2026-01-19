package com.swparks.ui.ds

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember

/**
 * Обертка для добавления тени в светлой теме
 *
 * @param modifier Модификатор
 * @param enabled Возможность кликать по карточке
 * @param onClick Обработчик нажатия
 * @param content Контент
 */
@Composable
fun FormCardContainer(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable (ColumnScope.() -> Unit)
) {
    val isLight = MaterialTheme.colorScheme.isLight()
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = if (onClick != null) {
            modifier.clickable(
                enabled = enabled,
                indication = null,
                interactionSource = interactionSource,
                onClick = onClick
            )
        } else {
            modifier
        },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(
            if (isLight) 2.dp else 0.dp
        )
    ) {
        content()
    }
}