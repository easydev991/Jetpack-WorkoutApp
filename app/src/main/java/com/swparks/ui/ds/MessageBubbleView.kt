package com.swparks.ui.ds

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.swparks.ui.theme.JetpackWorkoutAppTheme

/**
 * Тип сообщения (входящее/исходящее)
 *
 * @property topStartCorner Радиус верхнего левого угла
 * @property bottomEndCorner Радиус нижнего правого угла
 * @property boxPaddingValues Отступ сбоку, чтобы сообщение не занимало всю ширину экрана
 */
enum class MessageType(
    val topStartCorner: Dp,
    val bottomEndCorner: Dp,
    val boxPaddingValues: PaddingValues
) {
    INCOMING(
        0.dp,
        20.dp,
        PaddingValues(end = 40.dp)
    ),
    SENT(
        20.dp,
        0.dp,
        PaddingValues(start = 40.dp)
    )
}

/**
 * Вьюшка с сообщением в диалоге
 *
 * @param modifier Модификатор
 * @param messageType Тип сообщения - [MessageType]
 * @param messageBody Текст сообщения
 * @param dateString Дата/время отправки
 */
@Composable
fun MessageBubbleView(
    modifier: Modifier = Modifier,
    messageType: MessageType,
    messageBody: String,
    dateString: String
) {
    val bubbleColor = when (messageType) {
        MessageType.INCOMING -> MaterialTheme.colorScheme.surfaceVariant
        MessageType.SENT -> MaterialTheme.colorScheme.primary
    }
    val isIncoming = messageType == MessageType.INCOMING
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = if (isIncoming) Alignment.Start else Alignment.End,
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = if (isIncoming)
                Arrangement.Start else Arrangement.End
        ) {
            Box(
                contentAlignment = if (isIncoming) Alignment.TopStart else Alignment.TopEnd,
                modifier = Modifier
                    .padding(messageType.boxPaddingValues)
                    .clip(
                        RoundedCornerShape(
                            topStart = messageType.topStartCorner,
                            topEnd = 20.dp,
                            bottomStart = 20.dp,
                            bottomEnd = messageType.bottomEndCorner
                        )
                    )
                    .background(bubbleColor)
            ) {
                Text(
                    text = messageBody,
                    color = if (isIncoming) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(
                        vertical = 12.dp,
                        horizontal = 18.dp
                    )
                )
            }
        }
        Text(
            text = dateString,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Preview(
    showBackground = true,
    locale = "ru"
)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
fun MessageBubbleViewPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                MessageBubbleView(
                    messageType = MessageType.INCOMING,
                    messageBody = "orem ipsum dolor sit amet, consectetur adipiscing elit. " +
                            "Suspendisse ut semper quam. Phasellus non mauris sem. " +
                            "Donec sed fermentum eros. Donec pretium nec turpis a semper.",
                    dateString = "11:22"
                )
                MessageBubbleView(
                    messageType = MessageType.SENT,
                    messageBody = "Phasellus non mauris sem. Donec sed fermentum eros.",
                    dateString = "11:23"
                )
            }
        }
    }
}