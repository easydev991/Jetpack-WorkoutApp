package com.workout.jetpack_workout.ui.ds

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Обертка для добавления тени в светлой теме
 *
 * @param modifier Модификатор
 * @param content Контент
 */
@Composable
fun FormCardContainer(
    modifier: Modifier = Modifier,
    content: @Composable() (ColumnScope.() -> Unit)
) {
    val isLight = MaterialTheme.colorScheme.isLight()
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(
            if (isLight) 2.dp else 0.dp
        )
    ) {
        content()
    }
}