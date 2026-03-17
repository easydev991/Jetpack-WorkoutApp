package com.swparks.ui.common

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppViewModelNavigationReuseTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appViewModel_whenNavigateForwardAndBack_thenDoesNotRecreateViewModel() {
        var createdCount = 0

        composeTestRule.setContent {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = "screen_a"
            ) {
                composable("screen_a") {
                    val viewModel: TestViewModel = appViewModel {
                        createdCount++
                        TestViewModel()
                    }

                    ScreenA(
                        viewModel = viewModel,
                        onNavigateForward = { navController.navigate("screen_b") }
                    )
                }

                composable("screen_b") {
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Назад")
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Вперед").performClick()
        composeTestRule.onNodeWithText("Назад").performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, createdCount)
    }

    @Composable
    private fun ScreenA(
        viewModel: TestViewModel,
        onNavigateForward: () -> Unit
    ) {
        Text("VM:${viewModel.instanceId}")
        Button(onClick = onNavigateForward) {
            Text("Вперед")
        }
    }

    private class TestViewModel : ViewModel() {
        val instanceId: Int = nextId++

        private companion object {
            var nextId: Int = 1
        }
    }
}
