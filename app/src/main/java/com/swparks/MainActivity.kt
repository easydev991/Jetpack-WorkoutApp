package com.swparks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import com.swparks.ui.screens.RootScreen
import com.swparks.ui.theme.JetpackWorkoutAppTheme

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val systemBarStyle = SystemBarStyle.auto(
            Color.Transparent.toArgb(),
            Color.Transparent.toArgb()
        )
        enableEdgeToEdge(
            statusBarStyle = systemBarStyle,
            navigationBarStyle = systemBarStyle
        )
        setContent {
            JetpackWorkoutAppTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    RootScreen()
                }
            }
        }
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun AppPreview() {
    JetpackWorkoutAppTheme {
        RootScreen()
    }
}