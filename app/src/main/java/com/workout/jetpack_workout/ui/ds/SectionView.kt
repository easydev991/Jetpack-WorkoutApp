package com.workout.jetpack_workout.ui.ds

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.workout.jetpack_workout.R
import com.workout.jetpack_workout.ui.theme.JetpackWorkoutAppTheme

@Composable
fun SectionView(
    modifier: Modifier = Modifier,
    @StringRes titleID: Int,
    addPaddingToTitle: Boolean = true,
    @StringRes footerID: Int? = null,
    content: @Composable() () -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(id = titleID).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = if (addPaddingToTitle) 12.dp else 0.dp,
                bottom = 2.dp
            )
        )
        content()
        if (footerID != null) {
            Text(
                text = stringResource(id = footerID),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SectionViewPreview() {
    JetpackWorkoutAppTheme {
        SectionView(
            titleID = R.string.about_app,
            addPaddingToTitle = false,
            footerID = R.string.more
        ) {
            ListRowView(leadingText = "Text")
        }
    }
}