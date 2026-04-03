package com.swparks.ui.ds

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swparks.R
import com.swparks.ui.theme.JetpackWorkoutAppTheme

/**
 * Данные для отображения карточки пользователя.
 */
data class UserRowData(
    val modifier: Modifier = Modifier,
    val enabled: Boolean = true,
    val imageStringURL: String?,
    val name: String,
    val address: String?,
    val onClick: (() -> Unit)? = null
)

/**
 * Вьюшка с краткой информацией о пользователе
 *
 * @param data Данные для отображения и поведения карточки пользователя
 */
@Composable
fun UserRowView(data: UserRowData) {
    FormCardContainer(
        params =
            FormCardContainerParams(
                modifier = data.modifier,
                enabled = data.enabled,
                onClick = data.onClick
            )
    ) {
        FormRowContainer(
            config =
                FormRowConfig(
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
                    verticalPadding = dimensionResource(R.dimen.spacing_small),
                    content = {
                        SWAsyncImage(
                            config =
                                AsyncImageConfig(
                                    imageStringURL = data.imageStringURL,
                                    size = 42.dp,
                                    shape = CircleShape
                                )
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xxsmall))
                        ) {
                            Text(
                                text = data.name,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (!data.address.isNullOrBlank()) {
                                Text(
                                    text = data.address,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
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
fun UserRowViewPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            UserRowView(
                data =
                    UserRowData(
                        imageStringURL = null,
                        name = "yellowmouse215",
                        address = "Россия, Москва"
                    )
            )
        }
    }
}
