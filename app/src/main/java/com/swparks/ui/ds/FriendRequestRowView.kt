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
 * Данные для отображения вьюшки для входящей заявки на добавление в список друзей
 *
 * @param modifier Модификатор
 * @param imageStringURL Ссылка на аватар
 * @param name Имя
 * @param address Адрес в формате "Россия, Москва"
 * @param onClickAccept Действие по нажатию на кнопку "Принять"
 * @param onClickDecline Действие по нажатию на кнопку "Отклонить"
 */
data class FriendRequestData(
    val modifier: Modifier = Modifier,
    val imageStringURL: String?,
    val name: String,
    val address: String?,
    val onClickAccept: () -> Unit,
    val onClickDecline: () -> Unit
)

/**
 * Вьюшка для входящей заявки на добавление в список друзей
 *
 * @param data Данные для отображения - [FriendRequestData]
 */
@Composable
fun FriendRequestRowView(data: FriendRequestData) {
    FormCardContainer(modifier = data.modifier) {
        FormRowContainer(
            config = FormRowConfig(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalPadding = 12.dp,
                content = {
                    FriendRequestAvatar(imageStringURL = data.imageStringURL)
                    FriendRequestContent(
                        name = data.name,
                        address = data.address,
                        onClickAccept = data.onClickAccept,
                        onClickDecline = data.onClickDecline
                    )
                }
            )
        )
    }
}

@Composable
private fun FriendRequestAvatar(imageStringURL: String?) {
    SWAsyncImage(
        config = AsyncImageConfig(
            imageStringURL = imageStringURL,
            size = 42.dp,
            shape = CircleShape
        )
    )
}

@Composable
private fun FriendRequestContent(
    name: String,
    address: String?,
    onClickAccept: () -> Unit,
    onClickDecline: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FriendRequestHeader(
            name = name,
            address = address
        )
        FriendRequestButtons(
            onClickAccept = onClickAccept,
            onClickDecline = onClickDecline
        )
    }
}

@Composable
private fun FriendRequestHeader(
    name: String,
    address: String?
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
}

@Composable
private fun FriendRequestButtons(
    onClickAccept: () -> Unit,
    onClickDecline: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        SWButton(
            config = ButtonConfig(
                modifier = Modifier.weight(1f),
                size = SWButtonSize.SMALL,
                text = stringResource(id = R.string.accept_friend_request),
                onClick = onClickAccept
            )
        )
        SWButton(
            config = ButtonConfig(
                modifier = Modifier.weight(1f),
                size = SWButtonSize.SMALL,
                mode = SWButtonMode.TINTED,
                text = stringResource(id = R.string.decline_friend_request),
                onClick = onClickDecline
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
fun FriendRequestRowViewPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            FriendRequestRowView(
                data = FriendRequestData(
                    imageStringURL = null,
                    name = "Silverfrog19",
                    address = "Россия, Архангельск",
                    onClickAccept = {},
                    onClickDecline = {}
                )
            )
        }
    }
}