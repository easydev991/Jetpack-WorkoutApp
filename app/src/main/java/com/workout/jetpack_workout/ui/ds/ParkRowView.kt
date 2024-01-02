package com.workout.jetpack_workout.ui.ds

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.workout.jetpack_workout.R
import com.workout.jetpack_workout.ui.theme.JetpackWorkoutAppTheme

@Composable
fun ParkRowView(
    modifier: Modifier = Modifier,
    imageStringURL: String?,
    name: String,
    address: String? = null,
    peopleTrainCount: Int
) {
    FormCardContainer(modifier = modifier) {
        FormRowContainer(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalPadding = 12.dp
        ) {
            SWAsyncImage(
                imageStringURL = imageStringURL,
                size = 84.dp,
                showBorder = false
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!address.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.outline_assistant_navigation),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.size(16.dp),
                                contentDescription = null
                            )
                            Text(
                                text = address,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // TODO: поменять на material-symbols и вынести в отдельную вьюху
                        Image(
                            imageVector = Icons.Outlined.AccountCircle,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = pluralStringResource(
                                id = R.plurals.peopleTrainHere,
                                count = peopleTrainCount,
                                peopleTrainCount
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
fun ParkRowViewPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            ParkRowView(
                imageStringURL = null,
                name = "Very very very very very very long user name for two lines",
                address = "Россия, Москва",
                peopleTrainCount = 5
            )
        }
    }
}