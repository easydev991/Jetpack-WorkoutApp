package com.swparks.ui.ds

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import java.util.Random

@Composable
fun CheckmarkRowView(
    modifier: Modifier = Modifier,
    text: String,
    isChecked: Boolean
) {
    FormRowContainer(
        config = FormRowConfig(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalPadding = 12.dp
        ) {
            Text(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(2f)
            )
            AnimatedVisibility(
                visible = isChecked,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Image(
                    imageVector = Icons.Default.Check,
                    colorFilter = ColorFilter.tint(
                        color = MaterialTheme.colorScheme.primary
                    ),
                    contentDescription = "Checkmark"
                )
            }
        }
    )
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
fun CheckmarkRowViewPreview() {
    val numbers = (1..300).toList()
    JetpackWorkoutAppTheme {
        Surface {
            FormCardContainer(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                LazyColumn {
                    items(numbers, key = { it }) {
                        val randomBoolean = Random().nextBoolean()
                        CheckmarkRowView(
                            text = if (randomBoolean) "Элемент с галкой, № $it" else "Без галки, № $it",
                            isChecked = randomBoolean
                        )
                        if (it != numbers.last()) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}