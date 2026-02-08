package com.swparks.ui.ds

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swparks.R
import com.swparks.ui.model.Gender
import com.swparks.ui.theme.JetpackWorkoutAppTheme

/**
 * Данные для отображения карточки профиля
 *
 * @param modifier Модификатор
 * @param imageStringURL Ссылка на фото профиля
 * @param userName Имя
 * @param gender Пол
 * @param age Возраст в годах
 * @param shortAddress Краткий адрес в формате "Россия, Москва"
 */
data class UserProfileData(
    val modifier: Modifier = Modifier,
    val imageStringURL: String?,
    val userName: String,
    val gender: String,
    val age: Int,
    val shortAddress: String
)

/**
 * Карточка профиля
 *
 * @param data Данные для отображения - [UserProfileData]
 */
@Composable
fun UserProfileCardView(data: UserProfileData) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = data.modifier.fillMaxWidth()
    ) {
        SWAsyncImage(
            config = AsyncImageConfig(
                imageStringURL = data.imageStringURL,
                size = 150.dp,
                contentScale = ContentScale.Crop
            )
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = data.userName,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xxsmall_plus)),
            ) {
                AdditionalInfoRow(
                    imageVector = Icons.Rounded.AccountCircle,
                    text = "${data.gender}, ${
                        pluralStringResource(
                            id = R.plurals.ageInYears,
                            count = data.age,
                            data.age
                        )
                    }"
                )
                AdditionalInfoRow(
                    imageVector = Icons.Rounded.LocationOn,
                    text = data.shortAddress
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
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_xsmall))
    ) {
        Image(
            imageVector = imageVector,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(id = R.dimen.icon_size_indicator))
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
                data = UserProfileData(
                    imageStringURL = null,
                    userName = "Very very very very very very long user name for two lines",
                    gender = stringResource(id = Gender.FEMALE.description),
                    age = 30,
                    shortAddress = "Россия, Москва"
                )
            )
        }
    }
}