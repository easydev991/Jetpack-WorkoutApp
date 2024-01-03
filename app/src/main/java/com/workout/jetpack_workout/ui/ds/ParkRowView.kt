package com.workout.jetpack_workout.ui.ds

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
                        AdditionalInfoRow(
                            imageID = R.drawable.outline_assistant_navigation_16,
                            text = address
                        )
                    }
                    AdditionalInfoRow(
                        imageID = R.drawable.outline_account_circle_16,
                        text = pluralStringResource(
                            id = R.plurals.peopleTrainHere,
                            count = peopleTrainCount,
                            peopleTrainCount
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AdditionalInfoRow(
    @DrawableRes imageID: Int,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Image(
            painter = painterResource(id = imageID),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            contentDescription = null
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true, locale = "ru")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
fun ParkRowViewPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            ParkRowView(
                imageStringURL = null,
                name = "N° 3 Легендарная / Средняя",
                address = "м. Партизанская, улица 2-я Советская",
                peopleTrainCount = 5
            )
        }
    }
}