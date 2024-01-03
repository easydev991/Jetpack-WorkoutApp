package com.workout.jetpack_workout.ui.ds

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.workout.jetpack_workout.R
import com.workout.jetpack_workout.ui.theme.JetpackWorkoutAppTheme

@Composable
fun SWTextEditor(
    modifier: Modifier = Modifier,
    text: String,
    @StringRes labelID: Int,
    enabled: Boolean = true,
    onTextChange: (String) -> Unit
) {
    SWTextField(
        modifier = modifier.defaultMinSize(minHeight = 104.dp),
        text = text,
        labelID = labelID,
        onTextChange = onTextChange,
        enabled = enabled,
        singleLine = false
    )
}

@Preview(showBackground = true, locale = "ru")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
fun SWTextEditorPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SWTextEditor(
                    text = "",
                    labelID = R.string.event_details_placeholder,
                    onTextChange = {}
                )
                SWTextEditor(
                    text = "Мероприятие будет длится около трех часов, так что можно приходить в любое удобное время.",
                    labelID = R.string.event_details_placeholder,
                    onTextChange = {}
                )
            }
        }
    }
}