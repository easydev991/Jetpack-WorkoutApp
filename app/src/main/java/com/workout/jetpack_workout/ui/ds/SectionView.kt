package com.workout.jetpack_workout.ui.ds

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.workout.jetpack_workout.R
import com.workout.jetpack_workout.ui.theme.JetpackWorkoutAppTheme

@Composable
fun SectionView(
    modifier: Modifier = Modifier,
    @StringRes titleID: Int,
    addPaddingToTitle: Boolean = true,
    titleBottomPadding: Dp = 2.dp,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(titleBottomPadding)
    ) {
        Text(
            text = stringResource(id = titleID).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = if (addPaddingToTitle) 12.dp else 0.dp
            )
        )
        content()
    }
}

@Preview(
    showBackground = true,
    locale = "ru"
)
@Composable
fun SectionViewPreview() {
    JetpackWorkoutAppTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionView(
                titleID = R.string.about_app,
                addPaddingToTitle = false
            ) {
                ListRowView(leadingText = "Text")
            }
            Divider()
            SectionView(
                titleID = R.string.friends,
                titleBottomPadding = 4.dp
            ) {
                UserRowView(
                    imageStringURL = null,
                    name = "Alica",
                    address = "Россия, Арзамас"
                )
            }
            Divider()
            SectionView(
                titleID = R.string.requests,
                titleBottomPadding = 4.dp
            ) {
                FriendRequestRowView(
                    imageStringURL = null,
                    name = "Alica",
                    address = "Россия, Арзамас",
                    onClickAccept = {},
                    onClickDecline = {}
                )
            }
        }
    }
}