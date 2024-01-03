package com.workout.jetpack_workout.ui.ds

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.workout.jetpack_workout.R
import com.workout.jetpack_workout.model.Gender
import com.workout.jetpack_workout.ui.theme.JetpackWorkoutAppTheme

@Composable
fun UserProfileCardView(
    modifier: Modifier = Modifier,
    imageStringURL: String?,
    userName: String,
    gender: String,
    age: Int,
    shortAddress: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        SWAsyncImage(
            imageStringURL = imageStringURL,
            size = 150.dp
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = userName,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AdditionalInfoRow(
                    imageVector = Icons.Rounded.AccountCircle,
                    text = "$gender, ${
                        pluralStringResource(
                            id = R.plurals.ageInYears,
                            count = age,
                            age
                        )
                    }"
                )
                AdditionalInfoRow(
                    imageVector = Icons.Rounded.LocationOn,
                    text = shortAddress
                )
            }
        }
    }
}

@Composable
private fun AdditionalInfoRow(
    imageVector: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            imageVector = imageVector,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
            contentDescription = null,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
fun UserProfileCardViewPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            UserProfileCardView(
                imageStringURL = null,
                userName = "Very very very very very very long user name for two lines",
                gender = stringResource(id = Gender.FEMALE.model.description),
                age = 30,
                shortAddress = "Россия, Москва"
            )
        }
    }
}