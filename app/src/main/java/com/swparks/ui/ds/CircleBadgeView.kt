package com.swparks.ui.ds

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.swparks.R
import com.swparks.ui.theme.JetpackWorkoutAppTheme

/**
 * Вьюшка с бейджиком, например для непрочитанных сообщений в чате
 *
 * @param modifier Модификатор
 * @param value Цифра в бейджике. Значения больше 9 будут заменяться на 9+
 */
@Composable
fun CircleBadgeView(
    modifier: Modifier = Modifier,
    value: Int
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val badgeColor = MaterialTheme.colorScheme.primary
        Text(
            text = if (value > 99) "99+" else "$value",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .circleBackground(badgeColor, dimensionResource(id = R.dimen.spacing_micro))
        )
    }
}

@Preview(showBackground = true)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
fun CircleBadgeViewPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircleBadgeView(value = 4)
                CircleBadgeView(value = 120)
            }
        }
    }
}