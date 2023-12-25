package com.workout.jetpack_workout.ui.ds

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.workout.jetpack_workout.ui.theme.JetpackWorkoutAppTheme

@Composable
fun CircleBadgeView(
    modifier: Modifier = Modifier,
    value: Int
) {
    val badgeColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .drawWithContent {
                this.drawCircle(
                    badgeColor,
                    9.dp.toPx()
                )
                this.drawContent()
            }
    ) {
        Text(
            text = if (value > 9) "9+" else "$value",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.padding(5.dp)
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircleBadgeView(value = 4)
                CircleBadgeView(value = 10)
            }
        }
    }
}