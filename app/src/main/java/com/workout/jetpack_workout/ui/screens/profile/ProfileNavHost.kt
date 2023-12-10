package com.workout.jetpack_workout.ui.screens.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.workout.jetpack_workout.ui.theme.JetpackWorkoutAppTheme

@Composable
fun ProfileNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController,
        startDestination = "profile"
    ) {
        composable("profile") {
            ProfileRootScreen()
        }
    }
}

@Composable
fun ProfileRootScreen() {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Profile screen",
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileRootScreenPreview() {
    JetpackWorkoutAppTheme {
        ProfileRootScreen()
    }
}