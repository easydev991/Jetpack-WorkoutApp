package com.workout.jetpack_workout.ui.ds

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.workout.jetpack_workout.R
import com.workout.jetpack_workout.ui.theme.JetpackWorkoutAppTheme

@Composable
fun IncognitoProfileView(
    modifier: Modifier = Modifier,
    onClickAuth: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize()
    ) {
        Text(
            text = stringResource(id = R.string.auth_invitation),
            maxLines = 2,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 22.dp)
        )
        Button(
            modifier = Modifier
                .scaleOnTap()
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentPadding = PaddingValues(12.dp),
            shape = RoundedCornerShape(12.dp),
            onClick = onClickAuth
        ) {
            Text(text = stringResource(id = R.string.authorization))
        }
        Text(
            text = stringResource(id = R.string.registration_info),
            maxLines = 2,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 22.dp)
        )
    }
}

@Preview(showBackground = true)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
fun IncognitoProfileViewPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            IncognitoProfileView(
                modifier = Modifier.padding(horizontal = 16.dp),
                onClickAuth = {}
            )
        }
    }
}