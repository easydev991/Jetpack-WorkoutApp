package com.workout.jetpack_workout.ui.ds

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.workout.jetpack_workout.ui.theme.JetpackWorkoutAppTheme

@Composable
fun UserRowView(
    modifier: Modifier = Modifier,
    imageStringURL: String?,
    name: String,
    address: String?
) {
    FormCardContainer(modifier = modifier) {
        FormRowContainer(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalPadding = 12.dp
        ) {
            SWAsyncImage(
                imageStringURL = imageStringURL,
                size = 42.dp,
                shape = CircleShape
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = name,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (address != null) {
                    Text(
                        text = address,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
fun UserRowViewPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            UserRowView(
                imageStringURL = null,
                name = "Very very very very very very long user name for two lines",
                address = "Россия, Москва"
            )
        }
    }
}