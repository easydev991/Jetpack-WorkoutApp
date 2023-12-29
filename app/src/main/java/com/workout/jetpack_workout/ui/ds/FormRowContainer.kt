package com.workout.jetpack_workout.ui.ds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FormRowContainer(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal,
    horizontalPadding: Dp = 12.dp,
    verticalPadding: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Row(
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = horizontalPadding,
                vertical = verticalPadding
            )
    ) {
        content()
    }
}