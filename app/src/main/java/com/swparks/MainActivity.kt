package com.swparks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.swparks.data.preferences.AppSettingsDataStore
import com.swparks.navigation.rememberAppState
import com.swparks.ui.screens.RootScreen
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.viewmodel.MainActivityViewModel

/**
 * Главная Activity приложения.
 *
 * Проект использует ручной подход к внедрению зависимостей через factory методы.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Включаем edge-to-edge для Android 15+
        enableEdgeToEdge()

        // Создаём DataStore для настроек приложения
        val dataStore = AppSettingsDataStore(applicationContext)

        setContent {
            // Создаём ViewModel для MainActivity
            val viewModel: MainActivityViewModel =
                viewModel(
                    factory = MainActivityViewModel.factory(dataStore),
                )

            val theme by viewModel.theme.collectAsState()
            val useDynamicColors by viewModel.useDynamicColors.collectAsState()

            JetpackWorkoutAppTheme(
                appTheme = theme,
                dynamicColor = useDynamicColors
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val appState = rememberAppState(rememberNavController())
                    RootScreen(
                        appState = appState
                    )
                }
            }
        }
    }

    @Preview(showBackground = true, locale = "ru")
    @Composable
    fun AppPreview() {
        val appState = rememberAppState(rememberNavController())
        JetpackWorkoutAppTheme {
            RootScreen(
                appState = appState
            )
        }
    }
}
