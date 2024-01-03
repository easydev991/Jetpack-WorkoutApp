package com.workout.jetpack_workout.ui.screens.messages

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.workout.jetpack_workout.R
import com.workout.jetpack_workout.model.TabBarItem
import com.workout.jetpack_workout.ui.ds.IncognitoProfileView
import com.workout.jetpack_workout.ui.theme.JetpackWorkoutAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController,
        startDestination = TabBarItem.Messages.route
    ) {
        composable(TabBarItem.Messages.route) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(text = stringResource(id = R.string.messages))
                        },
                        windowInsets = WindowInsets(top = 0)
                    )
                }
            ) {
                MessagesRootScreen(modifier = Modifier.padding(it))
            }
        }
    }
}

@Composable
fun MessagesRootScreen(modifier: Modifier = Modifier) {
    IncognitoProfileView(
        modifier = modifier.padding(horizontal = 16.dp),
        onClickAuth = {
            TODO(reason = "Нужно подключить экран авторизации")
        }
    )
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun MessagesRootScreenPreview() {
    JetpackWorkoutAppTheme {
        MessagesRootScreen()
    }
}