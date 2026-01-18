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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swparks.ui.theme.JetpackWorkoutAppTheme

/**
 * Вьюшка с краткой информацией о пользователе
 *
 * @param modifier Модификатор
 * @param imageStringURL Ссылка на аватар пользователя
 * @param name Имя
 * @param address Адрес в формате "Россия, Москва"
 */
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
                if (!address.isNullOrBlank()) {
                    Text(
                        text = address,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
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
                imageStringURL = null,
                name = "yellowmouse215",
                address = "Россия, Москва"
            )
        }
    }
}