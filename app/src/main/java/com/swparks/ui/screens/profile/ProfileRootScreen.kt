package com.swparks.ui.screens.profile

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.swparks.R
import com.swparks.ui.ds.IncognitoProfileView
import com.swparks.ui.theme.JetpackWorkoutAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileRootScreen(modifier: Modifier = Modifier) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.profile))
                },
            )
        }
    ) { paddingValues ->
        IncognitoProfileView(
            modifier = modifier.padding(
                top = paddingValues.calculateTopPadding(),
                start = dimensionResource(R.dimen.spacing_regular),
                end = dimensionResource(R.dimen.spacing_regular)
            ),
            onClickAuth = {
                Log.d("ProfileRootScreen", "Кнопка авторизации нажата")
            }
        )
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun ProfileRootScreenPreview() {
    JetpackWorkoutAppTheme {
        ProfileRootScreen()
    }
}

