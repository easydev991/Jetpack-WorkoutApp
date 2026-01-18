package com.swparks.ui.ds

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swparks.R
import com.swparks.ui.theme.JetpackWorkoutAppTheme

/**
 * Вьюшка для входящей заявки на добавление в список друзей
 *
 * @param modifier Модификатор
 * @param imageStringURL Ссылка на аватар
 * @param name Имя
 * @param address Адрес в формате "Россия, Москва"
 * @param onClickAccept Действие по нажатию на кнопку "Принять"
 * @param onClickDecline Действие по нажатию на кнопку "Отклонить"
 */
@Composable
fun FriendRequestRowView(
    modifier: Modifier = Modifier,
    imageStringURL: String?,
    name: String,
    address: String?,
    onClickAccept: () -> Unit,
    onClickDecline: () -> Unit
) {
    FormCardContainer(modifier = modifier) {
        FormRowContainer(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalPadding = 12.dp
        ) {
            SWAsyncImage(
                imageStringURL = imageStringURL,
                size = 42.dp,
                shape = CircleShape
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SWButton(
                        modifier = Modifier.weight(1f),
                        size = SWButtonSize.SMALL,
                        text = stringResource(id = R.string.accept_friend_request),
                        onClick = onClickAccept
                    )
                    SWButton(
                        modifier = Modifier.weight(1f),
                        size = SWButtonSize.SMALL,
                        mode = SWButtonMode.TINTED,
                        text = stringResource(id = R.string.decline_friend_request),
                        onClick = onClickDecline
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
fun FriendRequestRowViewPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            FriendRequestRowView(
                imageStringURL = null,
                name = "Silverfrog19",
                address = "Россия, Архангельск",
                onClickAccept = {},
                onClickDecline = {}
            )
        }
    }
}