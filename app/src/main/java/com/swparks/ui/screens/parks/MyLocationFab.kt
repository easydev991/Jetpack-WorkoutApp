package com.swparks.ui.screens.parks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swparks.R
import com.swparks.ui.testtags.ScreenshotTestTags
import com.swparks.ui.theme.JetpackWorkoutAppTheme

@Composable
fun MyLocationFab(
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.testTag(ScreenshotTestTags.MAP_MY_LOCATION_FAB),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Box(
            modifier = Modifier.size(dimensionResource(R.dimen.size_xsmall)),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(dimensionResource(R.dimen.size_xsmall)),
                    strokeWidth = dimensionResource(R.dimen.border_width_small)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = stringResource(R.string.my_location_content_description)
                )
            }
        }
    }
}

@Preview
@Composable
private fun MyLocationFabIdlePreview() {
    JetpackWorkoutAppTheme {
        Surface {
            MyLocationFab(
                onClick = {},
                isLoading = false,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview
@Composable
private fun MyLocationFabLoadingPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            MyLocationFab(
                onClick = {},
                isLoading = true,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
